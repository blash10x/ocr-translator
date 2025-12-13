package blash10x.ocrtranslator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Author: myungsik.sung@gmail.com
 */
public class PaddleOCRService {
  private final ObjectMapper mapper = new ObjectMapper();
  private final String command;
  private final String imagePath;
  private final String outputJsonPath;

  public PaddleOCRService(Properties properties) {
    command = properties.getProperty("paddleocr.command");
    imagePath = properties.getProperty("paddleocr.image.path");
    outputJsonPath = properties.getProperty("paddleocr.output.json.path");
  }

  public String ocr() {
    StringBuilder sb = new StringBuilder();
    try {
      Process process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", command});
      CompletableFuture<Process> future = process.onExit();
      process = future.get();

      File jsonFile = new File(outputJsonPath);
      JsonNode rootNode = mapper.readTree(jsonFile);
      JsonNode recTextsNode = rootNode.get("rec_texts");
      for (JsonNode arrayNode : recTextsNode )  {
        sb.append(arrayNode.toPrettyString().replace("\"", ""));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  public String execCommand(String command) {
    try {
      Process process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", command});
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      StringBuilder sb = new StringBuilder();
      sb.append(command);
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
        sb.append(line).append("\n");
      }
      return sb + "\n";
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
