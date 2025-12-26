package blash10x.ocrtranslator.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: myungsik.sung@gmail.com
 */
public class GeminiWebApiService extends AbstractProcessService implements TranslationService {
  private final Map<String, String> cache = new HashMap<>();
  private final ResultCollector resultCollector;
  private final String promptTemplate;

  public GeminiWebApiService() {
    super("gemini-webapi");

    String pipeName = configLoader.getProperty("translation.gemini-webapi.output.pipe-name");
    String command = configLoader.getProperty("translation.gemini-webapi.command");

    resultCollector = new ResultCollector(pipeName);
    start(command, resultCollector);

    promptTemplate = configLoader.getProperty("translation.gemini-webapi.prompt-template");
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
      System.out.println("[Gemini-WebAPI:prompt]:\n" + prompt);

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
