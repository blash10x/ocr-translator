package blash10x.ocrtranslator.service;

import blash10x.ocrtranslator.util.JsonNodes;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Author: myungsik.sung@gmail.com
 */
@RequiredArgsConstructor
@Getter
public class ResultCollector implements Consumer<String> {
  private final String pipeToken;
  private JsonNode result;

  @Override
  public void accept(String str) {
    if (str.contains(pipeToken)) {
      synchronized (this) {
        String jsonStr = str.substring(pipeToken.length());
        result = JsonNodes.toJsonNode(jsonStr);
        notify();
      }
    }
  }
}
