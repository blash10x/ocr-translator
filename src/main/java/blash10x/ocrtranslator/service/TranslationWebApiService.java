package blash10x.ocrtranslator.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: myungsik.sung@gmail.com
 */
public class TranslationWebApiService extends AbstractHttpClientService implements TranslationService {
  private final Map<String, String> cache = new HashMap<>();
  private final String name;
  private final URI targetUrl;
  private final String subFormData;
  private final String resultKey;

  public TranslationWebApiService(String name) {
    this.name = name;

    String prefix = String.format("translation.%s.", name);
    targetUrl = URI.create(configLoader.getProperty(prefix + "target-url"));

    StringBuilder formData = new StringBuilder();
    configLoader.startsWith(prefix + "form-data").forEach((key, value) ->
        formData.append("&")
            .append(key.toString().substring(27))
            .append("=").append(value.toString()));
    subFormData = formData.toString();
    resultKey = configLoader.getProperty(prefix + "response.resultKey");
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
    String formData = "text=" + URLEncoder.encode(textToTranslate, StandardCharsets.UTF_8) + subFormData;
    JsonNode rootNode = post(targetUrl,
        "application/x-www-form-urlencoded; charset=UTF-8",
        HttpRequest.BodyPublishers.ofString(formData));
    return rootNode.has(resultKey)
        ? rootNode.get(resultKey).textValue()
        : "Error: No translatedText in response";
  }
}