package blash10x.ocrtranslator.service;

import blash10x.ocrtranslator.util.JsonNodes;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: myungsik.sung@gmail.com
 */
public class TranslationWebApiService extends AbstractHttpClientService implements TranslationService {
  private final Map<String, String> cache = new HashMap<>();
  private final String targetUrl;
  private final String subFormData;
  private final String resultKey;

  public TranslationWebApiService(String name) {
    String prefix = String.format("translation.%s.", name);
    targetUrl = configLoader.getProperty(prefix + "target-url");

    StringBuilder formData = new StringBuilder();
    configLoader.startsWith(prefix + "form-data").forEach((key, value) ->
        formData.append("&")
            .append(key.toString().substring(27))
            .append("=").append(value.toString()));
    subFormData = formData.toString();
    resultKey = configLoader.getProperty(prefix + "response.resultKey");
  }

  @Override
  public String translate(String textToTranslate) {
    return cache.computeIfAbsent(textToTranslate, key -> _translate(textToTranslate));
  }

  public String _translate(String textToTranslate) {
    try {
      // Form-Data 인코딩
      String formData = "text=" + URLEncoder.encode(textToTranslate, StandardCharsets.UTF_8) + subFormData;
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(targetUrl))
          .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
          .header("Accept", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(formData))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      JsonNode rootNode = JsonNodes.toJsonNode(response.body()); // JSON 파싱
      if (rootNode.has(resultKey)) {
        return rootNode.get(resultKey).asText();
      } else {
        return "Error: No translatedText in response";
      }
    } catch (Exception e) {
      return "Translation Error: " + e.getMessage();
    }
  }
}