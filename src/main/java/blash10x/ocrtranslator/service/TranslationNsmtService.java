// src/main/java/blash10x/ocrtranslator/service/TranslationService.java
package blash10x.ocrtranslator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 외부 번역 API를 호출하여 텍스트를 번역하는 서비스입니다.
 * <p/>
 * Author: myungsik.sung@gmail.com
 */
public class TranslationNsmtService {
  private static final String PREFIX = "translation.nsmt.";
  private final String targetUrl;
  private final String subFormData;
  private final String resultKey;

  private final Map<String, String> cache = new HashMap<>();
  private final HttpClient client;
  private final ObjectMapper mapper = new ObjectMapper();

  public TranslationNsmtService(Properties properties) {
    targetUrl = properties.getProperty(PREFIX + "target-url");

    StringBuilder formData = new StringBuilder();
    properties.entrySet().stream()
        .filter(entry -> entry.getKey().toString().startsWith(PREFIX + "form-data"))
        .forEach(entry -> formData.append("&")
            .append(entry.getKey().toString().substring(27))
            .append("=").append(entry.getValue().toString()));
    subFormData = formData.toString();
    resultKey = properties.getProperty("translation.response.resultKey");

    try {
      client = createInsecureHttpClient();
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException(e);
    }
  }

  public String translate(String textToTranslate) {
    return cache.computeIfAbsent(textToTranslate, key -> _translate(textToTranslate));
  }

  private String _translate(String textToTranslate) {
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

  // 개발 환경용: SSL 인증서 검증 무시 HttpClient
  private HttpClient createInsecureHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
    TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
          public X509Certificate[] getAcceptedIssuers() { return null; }
          public void checkClientTrusted(X509Certificate[] certs, String authType) {}
          public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }
    };

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

    return HttpClient.newBuilder()
        .sslContext(sslContext)
        .build();
  }
}