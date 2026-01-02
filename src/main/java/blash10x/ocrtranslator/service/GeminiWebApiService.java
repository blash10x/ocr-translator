package blash10x.ocrtranslator.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Author: myungsik.sung@gmail.com
 */
public class GeminiWebApiService extends AbstractProcessService implements TranslationService {
  private final Map<String, String> cache = new HashMap<>();
  private final String promptTemplate;
  @Getter
  private final String command;
  @Getter
  private final String pipeName;

  private ResultCollector resultCollector;

  public GeminiWebApiService() {
    super("gemini-webapi");

    promptTemplate = configLoader.getProperty("translation.gemini-webapi.prompt-template");
    command = configLoader.getProperty("translation.gemini-webapi.command");
    pipeName = configLoader.getProperty("translation.gemini-webapi.output.pipe-name");
  }

  @Override
  public void initialize() {
    resultCollector = start();
  }

  @Override
  public String getName() {
    return processName;
  }

  @Override
  public String translate(String textToTranslate) {
    return cache.computeIfAbsent(textToTranslate, key -> _translate(textToTranslate));
  }

  private String _translate(String textToTranslate) {
    try {
      String prompt = String.format(promptTemplate, textToTranslate);
      System.out.printf("[%s:prompt]:%n%s%n", getName(), prompt);

      writeToProcess(prompt + "\nEOF\n"); // translation command:

      synchronized (this) {
        wait();
      }

      JsonNode jsonNode = resultCollector.getResult();
      return jsonNode.get("target").asText();
    } catch (Exception e) {
      return e.toString();
    }
  }
}
