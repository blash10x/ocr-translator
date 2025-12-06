// src/main/java/blash10x/ocrtranslator/service/TranslationService.java
package blash10x.ocrtranslator.service;

import blash10x.ocrtranslator.App;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 외부 번역 API를 호출하여 텍스트를 번역하는 서비스입니다.
 * <p/>
 * Author: myungsik.sung@gmail.com
 */
public class TranslationService {
  private final Properties properties = new Properties();
  private final String targetUrl;

  public TranslationService() {
    try (InputStream input = App.class.getResourceAsStream("translation-service.properties")) {
      properties.load(input);
    } catch (IOException e) {
      System.err.println("Could not load translation service.properties: " + e.getMessage());
    }
    targetUrl = properties.getProperty("target-url");
  }

  public String translate(String textToTranslate) {
    try {
      // Localhost의 self-signed 인증서 무시를 위한 설정 (개발용)
      HttpClient client = createInsecureHttpClient();

      // Form-Data 인코딩
      StringBuilder formData = new StringBuilder();
      formData.append("text=").append(URLEncoder.encode(textToTranslate, StandardCharsets.UTF_8));

      properties.entrySet().stream()
          .filter(entry -> entry.getKey().toString().startsWith("form-data"))
          .forEach(entry -> formData.append("&")
              .append(entry.getKey().toString().substring(10))
              .append("=").append(entry.getValue().toString()));

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(targetUrl))
          .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
          .header("Accept", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(formData.toString()))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      // JSON 파싱
      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(response.body());

      // "translatedText" 필드 추출
      if (rootNode.has("translatedText")) {
        return rootNode.get("translatedText").asText();
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