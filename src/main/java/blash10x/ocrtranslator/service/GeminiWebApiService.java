package blash10x.ocrtranslator.service;

import blash10x.ocrtranslator.util.JsonNodes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
      return resultCollector.getResult().result;
    } catch (Exception e) {
      return e.toString();
    }
  }

  @RequiredArgsConstructor
  @Getter
  public static class ResultCollector implements Consumer<String> {
    private final String pipeToken;
    private Result result;

    @Override
    public void accept(String str) {
      if (str.contains(pipeToken)) {
        synchronized (this) {
          String jsonStr = str.substring(pipeToken.length());
          result = JsonNodes.toValue(jsonStr, Result.class);
          notify();
        }
      }
    }
  }

  private record Result(String source, String result, String model) {}
}
