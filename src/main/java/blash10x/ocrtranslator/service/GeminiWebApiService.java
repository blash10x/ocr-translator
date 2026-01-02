package blash10x.ocrtranslator.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: myungsik.sung@gmail.com
 */
public class GeminiWebApiService extends AbstractProcessService implements TranslationService {
  private final Map<String, String> cache = new HashMap<>();
  private final String promptTemplate;
  private final String command;
  private final ResultCollector resultCollector;

  public GeminiWebApiService() {
    super("gemini-webapi");

    promptTemplate = configLoader.getProperty("translation.gemini-webapi.prompt-template");
    command = configLoader.getProperty("translation.gemini-webapi.command");

    String pipeName = configLoader.getProperty("translation.gemini-webapi.output.pipe-name");
    resultCollector = new ResultCollector(pipeName);
  }

  @Override
  public void initialize() {
    start(command, resultCollector);
  }

  @Override
  public String getName() {
    return getProcessName();
  }

  @Override
  public String translate(String textToTranslate) {
    return cache.computeIfAbsent(textToTranslate, key -> _translate(textToTranslate));
  }

  public String _translate(String textToTranslate) {
    try {
      String prompt = String.format(promptTemplate, textToTranslate);
      System.out.printf("[%s:prompt]:%n%s%n", getName(), prompt);

      writeToProcess(prompt + "\nEOF\n"); // translation command:

      synchronized (resultCollector) {
        resultCollector.wait();
      }

      JsonNode jsonNode = resultCollector.getResult();
      return jsonNode.get("target").asText();
    } catch (Exception e) {
      return e.toString();
    }
  }
}
