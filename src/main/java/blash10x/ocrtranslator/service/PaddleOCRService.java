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
  private final String outputDir;
  private final String outputImageFilename;
  private final String outputJsonFilename;

  public PaddleOCRService() {
    ConfigLoader configLoader = ConfigLoader.getConfigLoader();

    outputDir = configLoader.getProperty("paddleocr.output.dir");
    outputImageFilename = configLoader.getProperty("paddleocr.output.image.filename");
    outputJsonFilename = configLoader.getProperty("paddleocr.output.json.filename");
    String command = configLoader.getProperty("paddleocr.command");

    try {
      Runtime.getRuntime().exec(new String[]{"cmd", "/c", command});
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    watchPath = Paths.get(outputDir);
  }

  public String doOCR(ImageView imageView) {
    try {
      // 감시할 이벤트 종류 등록
      WatchService watcher = FileSystems.getDefault().newWatchService();
      watchPath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
      System.out.println("-------------- 디렉터리 감시 시작: " + watchPath);
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
          splitImage(imageView);
          return collectTexts();
        }

        // 다음 이벤트를 받기 위해 키를 재설정
        boolean valid = key.reset();
        if (!valid) {
          break; // 디렉터리를 더 이상 감시할 수 없을 때 루프 종료
        }
      }
      splitImage(imageView);
      return collectTexts();
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  private void splitImage(ImageView imageView) {
    File file = new File(outputDir, outputImageFilename);
    Image outputImage = new Image(file.toURI().toString());
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
    File jsonFile = new File(outputDir, outputJsonFilename);
    JsonNode rootNode = mapper.readTree(jsonFile);
    JsonNode recTextsNode = rootNode.get("rec_texts");
    for (JsonNode arrayNode : recTextsNode )  {
      sb.append(arrayNode.toPrettyString().replace("\"", "")).append("\n");
    }
    return sb.toString().strip();
  }
}
