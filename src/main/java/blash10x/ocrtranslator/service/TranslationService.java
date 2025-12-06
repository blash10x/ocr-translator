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
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 외부 번역 API를 호출하여 텍스트를 번역하는 서비스입니다.
 */
public class TranslationService {

  private static final String TARGET_URL = "https://papago.naver.com/apis/n2mt/translate";

  public String translate(String textToTranslate) {
    try {
      // Localhost의 self-signed 인증서 무시를 위한 설정 (개발용)
      HttpClient client = createInsecureHttpClient();

      // Form-Data 인코딩
      String formData = "text=" + URLEncoder.encode(textToTranslate, StandardCharsets.UTF_8)
          + "&deviceId=1b224e02-c3c4-42f1-b892-e3d14acda04d"
          + "&locale=ko"
          + "&dict=true"
          + "&dictDisplay=30"
          + "&honorific=false"
          + "&source=ja&target=ko"
          + "&usageAgreed=false"
          ;

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(TARGET_URL))
          .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
          .header("Accept", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(formData))
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
      e.printStackTrace();
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