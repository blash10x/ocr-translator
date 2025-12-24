package blash10x.ocrtranslator.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: myungsik.sung@gmail.com
 */
public class GeminiWebApiService extends AbstractProcessService {
  private final Map<String, String> cache = new HashMap<>();
  private final ResultCollector resultCollector;

  public GeminiWebApiService() {
    super("gemini-webapi");

    String pipeName = configLoader.getProperty("translation.gemini-webapi.output.pipe-name");
    String command = configLoader.getProperty("translation.gemini-webapi.command");

    resultCollector = new ResultCollector(pipeName);
    start(command, resultCollector);
  }

  public String translate(String textToTranslate) {
    return cache.computeIfAbsent(textToTranslate, key -> _translate(textToTranslate));
  }

  private String _translate(String textToTranslate) {
    try {
      writeToProcess(textToTranslate + "\nEOF\n"); // translation command:

      synchronized (resultCollector) {
        resultCollector.wait();
      }

      JsonNode jsonNode = resultCollector.getResult();
      return jsonNode.get("result").asText();
    } catch (Exception e) {
      return e.toString();
    }
  }
}
