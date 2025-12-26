// src/main/java/blash10x/ocrtranslator/service/TranslationService.java
package blash10x.ocrtranslator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * 외부 번역 API를 호출하여 텍스트를 번역하는 서비스입니다.
 * <p/>
 * Author: myungsik.sung@gmail.com
 */
public class TranslationNsmtService extends AbstractHttpClientService implements TranslationService {
  private static final String PREFIX = "translation.nsmt.";
  private final String targetUrl;
  private final String subFormData;
  private final String resultKey;

  private final ObjectMapper mapper = new ObjectMapper();

  public TranslationNsmtService() {
    targetUrl = configLoader.getProperty(PREFIX + "target-url");

    StringBuilder formData = new StringBuilder();
    configLoader.startsWith(PREFIX + "form-data").forEach((key, value) ->
        formData.append("&")
            .append(key.toString().substring(27))
            .append("=").append(value.toString()));
    subFormData = formData.toString();
    resultKey = configLoader.getProperty(PREFIX + "response.resultKey");
  }

  @Override
  public String translate(String textToTranslate) {
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

      // "translatedText" 필드 추출
      JsonNode rootNode = mapper.readTree(response.body()); // JSON 파싱
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