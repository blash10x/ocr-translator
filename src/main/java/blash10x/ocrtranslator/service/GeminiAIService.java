// src/main/java/blash10x/ocrtranslator/service/TranslationService.java
package blash10x.ocrtranslator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * 외부 번역 API를 호출하여 텍스트를 번역하는 서비스입니다.
 * <p/>
 * Author: myungsik.sung@gmail.com
 */
public class GeminiAIService extends AbstractTranslationService {
  private static final String PREFIX = "translation.gemini-ai.";
  private final String targetUrl;
  private final String apiKey;

  private final Map<String, String> cache = new HashMap<>();

  private final HttpClient client;
  private final ObjectMapper mapper = new ObjectMapper();

  private final JsonNode baseNode;
  private final ObjectNode partNode;

  public GeminiAIService() {
    ConfigLoader configLoader = ConfigLoader.getConfigLoader();

    targetUrl = configLoader.getProperty(PREFIX + "target-url");
    apiKey =  configLoader.getProperty(PREFIX + "api-key");

    try {
      client = createInsecureHttpClient();
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException(e);
    }

    baseNode = createBaseNode();
    partNode = (ObjectNode) baseNode.findValue("parts").get(0);
  }

  private JsonNode createBaseNode() {
    ObjectNode objectNode = mapper.createObjectNode();

    ArrayNode contentsNode = mapper.createArrayNode();
    objectNode.set("contents",contentsNode);
    ObjectNode contentNode = mapper.createObjectNode();
    contentsNode.add(contentNode);

    ArrayNode partsNode = mapper.createArrayNode();
    contentNode.set("parts" ,partsNode);

    ObjectNode partNode = mapper.createObjectNode();
    partsNode.add(partNode);

    partNode.put("text","How does AI work?");
    return objectNode;
  }

  public String translate(String textToTranslate) {
    return cache.computeIfAbsent(textToTranslate, key -> _translate(textToTranslate));
  }

  private String _translate(String textToTranslate) {
    String text = """
        일본어 '
        """ + textToTranslate + """
        '을 한국어로 번역
        원문 줄바꿈을 번역문 줄바꿈에 적용하여 추천 번역만 간결하게 응답.
        """;
    partNode.put("text", text);

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(targetUrl))
          .header("Content-Type", "application/json")
          .header("x-goog-api-key", apiKey)
          .POST(HttpRequest.BodyPublishers.ofString(baseNode.toString()))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      JsonNode rootNode = mapper.readTree(response.body()); // JSON 파싱
      JsonNode textNode = rootNode.findValue("text");
      return textNode != null ? textNode.asText() : "Not found text node";
    } catch (Exception e) {
      return "Translation Error: " + e.getMessage();
    }
  }
}