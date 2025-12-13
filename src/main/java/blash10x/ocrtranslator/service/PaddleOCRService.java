package blash10x.ocrtranslator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

/**
 * Author: myungsik.sung@gmail.com
 */
public class PaddleOCRService {
  private final ObjectMapper mapper = new ObjectMapper();
  private final String command;
  private final String imagePath;
  private final String outputImagePath;
  private final String outputJsonPath;

  public PaddleOCRService(Properties properties) {
    command = properties.getProperty("paddleocr.command");
    imagePath = properties.getProperty("paddleocr.image.path");
    outputImagePath = properties.getProperty("paddleocr.output.image.path");
    outputJsonPath = properties.getProperty("paddleocr.output.json.path");
  }

  public String doOCR(ImageView imageView) {
    try {
      Process process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", command});
      CompletableFuture<Process> future = process.onExit();
      future.get(); // wait

      splitImage(imageView);
      return collectTexts();
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  private void splitImage(ImageView imageView) {
    File file = new File(outputImagePath);
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
    File jsonFile = new File(outputJsonPath);
    JsonNode rootNode = mapper.readTree(jsonFile);
    JsonNode recTextsNode = rootNode.get("rec_texts");
    for (JsonNode arrayNode : recTextsNode )  {
      sb.append(arrayNode.toPrettyString().replace("\"", "")).append("\n");
    }
    return sb.toString().strip();
  }
}
