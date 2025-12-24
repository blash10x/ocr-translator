package blash10x.ocrtranslator.service;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
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
public class PaddleOCRService extends AbstractProcessService implements OCRService {
  private final ResultCollector resultCollector;
  private final Path watchPath;
  private final File outputImageFile;
  private final String resultKey;

  public PaddleOCRService() {
    super("paddleocr");

    String outputDir = configLoader.getProperty("paddleocr.output.dir");
    String outputImageFilename = configLoader.getProperty("paddleocr.output.image.filename");
    String pipeName = configLoader.getProperty("paddleocr.output.pipe-name");
    String command = configLoader.getProperty("paddleocr.command");

    resultKey = configLoader.getProperty("paddleocr.output.json.resultKey");
    watchPath = Paths.get(outputDir);
    outputImageFile = new File(outputDir, outputImageFilename);

    resultCollector = new ResultCollector(pipeName);
    start(command, resultCollector);
  }

  @Override
  public OCRResult doOCR(Image image, File imagePath) {
    writeToProcess(imagePath + "\n");

    String resultText;
    Image boxedImage = null;
    try {
      WatchService watcher = FileSystems.getDefault().newWatchService();
      watchPath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
      System.out.println("-- 디렉터리 감시 시작: " + watchPath);

      synchronized (resultCollector) {
        resultCollector.wait();
      }

      resultText = collectTexts(resultCollector.getResult(), resultKey);

      while (true) {
        WatchKey key;
        try {
          key = watcher.take();
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
            break;
          }
        }

        // 다음 이벤트를 받기 위해 키를 재설정
        boolean valid = key.reset();
        if (!valid || boxedImage != null) {
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

  private String collectTexts(JsonNode jsonNode, String fieldName) {
    StringBuilder sb = new StringBuilder();
    JsonNode resultNodes = jsonNode.get("res").get(fieldName);
    for (JsonNode textNode : resultNodes) {
      sb.append(textNode.asText().replace("\"", "")).append("\n");
    }
    return sb.toString().strip();
  }
}
