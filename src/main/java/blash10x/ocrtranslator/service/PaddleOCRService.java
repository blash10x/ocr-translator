package blash10x.ocrtranslator.service;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

/**
 * Author: myungsik.sung@gmail.com
 */
public class PaddleOCRService {
  private final Path watchPath;
  private final ObjectMapper mapper = new ObjectMapper();
  private final Process process;
  private final File outputImageFile;
  private final File outputJsonFile;

  public PaddleOCRService() {
    ConfigLoader configLoader = ConfigLoader.getConfigLoader();

    String outputDir = configLoader.getProperty("paddleocr.output.dir");
    String outputImageFilename = configLoader.getProperty("paddleocr.output.image.filename");
    String outputJsonFilename = configLoader.getProperty("paddleocr.output.json.filename");
    String command = configLoader.getProperty("paddleocr.command");

    outputImageFile = new File(outputDir, outputImageFilename);
    outputJsonFile = new File(outputDir, outputJsonFilename);
    watchPath = Paths.get(outputDir);

    try {
      ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);

      // 표준 입출력을 파이프로 설정 (기본값)
      builder.redirectInput(ProcessBuilder.Redirect.PIPE);
      builder.redirectOutput(ProcessBuilder.Redirect.INHERIT); // 부모 프로세스(콘솔)로 출력 상속
      builder.redirectErrorStream(true); // 에러 스트림을 표준 출력으로 병합

      process = builder.start();
      System.out.println("PaddleOCR has started: " + command);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        process.children().forEach(ProcessHandle::destroy);
        process.destroy();
      }));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String doOCR(ImageView imageView) {
    try {
      // 감시할 이벤트 종류 등록
      WatchService watcher = FileSystems.getDefault().newWatchService();
      watchPath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
      System.out.println("---- 디렉터리 감시 시작: " + watchPath);
      while (true) {
        WatchKey key;
        try {
          key = watcher.take(); // 이벤트가 큐에 들어올 때까지 대기
        } catch (InterruptedException e) {
          return e.getMessage();
        }

        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();
          Path fileName = (Path) event.context();

          System.out.println("---- 이벤트 종류: " + kind.name() + ", 파일명: " + fileName);

          // 여기에 파일 변경 시 수행할 작업 추가
          if (outputImageFile.getName().equals(fileName.toString())) {
            splitImage(imageView);
          }
          if (outputJsonFile.getName().equals(fileName.toString())) {
            return collectTexts();
          }
        }

        // 다음 이벤트를 받기 위해 키를 재설정
        boolean valid = key.reset();
        if (!valid) {
          break; // 디렉터리를 더 이상 감시할 수 없을 때 루프 종료
        }
      }
      return ""; // unreachable code
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  private void splitImage(ImageView imageView) {
    Image outputImage = new Image(outputImageFile.toURI().toString());
    WritableImage writableImage = new WritableImage(
        outputImage.getPixelReader(),
        0, 0,
        (int) (outputImage.getWidth() / 2),
        (int) outputImage.getHeight()
    );
    imageView.setImage(writableImage);
  }

  private String collectTexts() throws IOException {
    StringBuilder sb = new StringBuilder();
    JsonNode rootNode = mapper.readTree(outputJsonFile);
    JsonNode recTextsNode = rootNode.get("rec_texts");
    for (JsonNode arrayNode : recTextsNode )  {
      sb.append(arrayNode.toPrettyString().replace("\"", "")).append("\n");
    }
    return sb.toString().strip();
  }
}
