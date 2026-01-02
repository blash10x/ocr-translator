package blash10x.ocrtranslator.service;

import blash10x.ocrtranslator.util.JsonNodes;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.util.function.Consumer;
import lombok.Getter;

/**
 * Author: myungsik.sung@gmail.com
 */
@Getter
public class ResultCollector implements Consumer<String> {
  private final AbstractProcessService processService;
  private final String token;
  private JsonNode result;

  public ResultCollector(AbstractProcessService processService) {
    this.processService = processService;
    this.token = processService.getPipeName() + File.pathSeparatorChar;
  }

  @Override
  public void accept(String str) {
    if (str.contains(token)) {
      synchronized (processService) {
        int beginIndex = str.indexOf("{");
        int endIndex = str.lastIndexOf("}");
        String jsonStr = str.substring(beginIndex, endIndex + 1);
        result = JsonNodes.toJsonNode(jsonStr);
        processService.notify();
      }
    }
  }
}
