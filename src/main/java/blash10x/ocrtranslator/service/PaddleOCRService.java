package blash10x.ocrtranslator.service;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

/**
 * Author: myungsik.sung@gmail.com
 */
public class PaddleOCRService extends AbstractProcessService {
  private final ObjectMapper mapper = new ObjectMapper();
  private final Path watchPath;
  private final File outputImageFile;
  private final File outputJsonFile;
  private final String resultKey;

  public PaddleOCRService() {
    super("PaddleOCRService");
    ConfigLoader configLoader = ConfigLoader.getConfigLoader();

    String outputDir = configLoader.getProperty("paddleocr.output.dir");
    String outputImageFilename = configLoader.getProperty("paddleocr.output.image.filename");
    String outputJsonFilename = configLoader.getProperty("paddleocr.output.json.filename");
    String command = configLoader.getProperty("paddleocr.command");
    start(command, ProcessBuilder.Redirect.INHERIT);

    resultKey = configLoader.getProperty("paddleocr.output.json.resultKey");
    watchPath = Paths.get(outputDir);
    outputImageFile = new File(outputDir, outputImageFilename);
    outputJsonFile = new File(outputDir, outputJsonFilename);
  }

  public OCRResult doOCR(File imagePath) {
    OutputStream os = process.getOutputStream(); // 프로세스의 입력 스트림 가져오기
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
    try {
      writer.write(imagePath + "\n");
      writer.flush(); // 버퍼 비우기 (중요: 데드락 발생 가능성)
    } catch (IOException e) {
      return new OCRResult(e.toString(), null);
    }

    String resultText = null;
    Image boxedImage = null;
    try {
      // 감시할 이벤트 종류 등록
      WatchService watcher = FileSystems.getDefault().newWatchService();
      watchPath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
      System.out.println("-- 디렉터리 감시 시작: " + watchPath);
      while (true) {
        WatchKey key;
        try {
          key = watcher.take(); // 이벤트가 큐에 들어올 때까지 대기
        } catch (InterruptedException e) {
          resultText = e.toString();
          break;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();
          Path fileName = (Path) event.context();

          System.out.println("-- 이벤트 종류: " + kind.name() + ", 파일명: " + fileName);

          // 여기에 파일 변경 시 수행할 작업 추가
          if (outputImageFile.getName().equals(fileName.toString())) {
            boxedImage = splitImage(outputImageFile);
          }
          if (outputJsonFile.getName().equals(fileName.toString())) {
            resultText = collectTexts(outputJsonFile);
            break;
          }
        }

        // 다음 이벤트를 받기 위해 키를 재설정
        boolean valid = key.reset();
        if (!valid || resultText != null) {
          break; // 디렉터리를 더 이상 감시할 수 없을 때 루프 종료
        }
      }
    } catch (Exception e) {
      resultText = e.toString();
    }
    return new OCRResult(resultText, boxedImage);
  }

  private WritableImage splitImage(File outputImageFile) {
    Image outputImage = new Image(outputImageFile.toURI().toString());
    return new WritableImage(
        outputImage.getPixelReader(),
        0, 0,
        (int) (outputImage.getWidth() / 2),
        (int) outputImage.getHeight()
    );
  }

  private String collectTexts(File outputJsonFile) throws IOException {
    StringBuilder sb = new StringBuilder();
    JsonNode rootNode = mapper.readTree(outputJsonFile);
    JsonNode resultNodes = rootNode.get(resultKey);
    for (JsonNode textNode : resultNodes) {
      sb.append(textNode.toPrettyString().replace("\"", "")).append("\n");
    }
    return sb.toString().strip();
  }
}
