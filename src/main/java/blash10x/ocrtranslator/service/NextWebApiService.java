package blash10x.ocrtranslator.service;

import blash10x.ocrtranslator.util.JsonNodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: myungsik.sung@gmail.com
 */
public class NextWebApiService extends AbstractHttpClientService implements TranslationService {
  private final Map<String, String> cache = new HashMap<>();
  private final String name;
  private final URI targetUrl;
  private final String apiKey;
  private final String targetLang;
  private final String model;

  public NextWebApiService(String name) {
    this.name = name;

    String prefix = String.format("translation.%s.", name);
    targetUrl = URI.create(configLoader.getProperty(prefix + "target-url"));
    apiKey = configLoader.getProperty(prefix + "api-key");
    targetLang = configLoader.getProperty(prefix + "target-lang");
    model = configLoader.getProperty(prefix + "model");
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String translate(String textToTranslate) {
    return cache.computeIfAbsent(textToTranslate, key -> _translate(textToTranslate));
  }

  private String _translate(String textToTranslate) {
    try (HttpClient client = HttpClient.newHttpClient()) {
      JsonNode requestBody = buildRequestBody(textToTranslate);

      String jsonPayload = JsonNodes.toString(requestBody);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(targetUrl)
          .header("Content-Type", "application/json")
          .header("API-KEY", apiKey)
          .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      JsonNode responseBody = JsonNodes.toJsonNode(response.body());
      return extractTranslations(responseBody);
    } catch (Exception e) {
      e.printStackTrace();
      return "Error: No translatedText in response";
    }
  }

  private JsonNode buildRequestBody(String textToTranslate) {
    ObjectNode rootNode = JsonNodes.createEmptyObjectNode();
    ArrayNode textArray = rootNode.putArray("text");

    String[] texts = textToTranslate.split("\n");
    Arrays.stream(texts).forEach(textArray::add);
    rootNode.put("target_lang", targetLang);
    rootNode.put("model", model);
    return rootNode;
  }

  private String extractTranslations(JsonNode responseBody) {
    JsonNode translationsNode = responseBody.path("translations");
    StringBuilder sb = new StringBuilder();
    for (JsonNode node : translationsNode) {
      String translatedText = node.path("text").asText();
      sb.append(translatedText).append("\n");
    }
    return sb.toString().strip();
  }
}