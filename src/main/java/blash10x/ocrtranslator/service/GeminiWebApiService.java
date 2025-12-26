package blash10x.ocrtranslator.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Author: myungsik.sung@gmail.com
 */
public class GeminiWebApiService extends AbstractProcessService implements TranslationService {
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
  public String translate(String textToTranslate) {
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
