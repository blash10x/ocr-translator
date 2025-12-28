package blash10x.ocrtranslator.service;

import blash10x.ocrtranslator.util.JsonNodes;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Author: myungsik.sung@gmail.com
 */
public abstract class AbstractHttpClientService {
  protected final HttpClient client = createInsecureHttpClient();

  private HttpClient createInsecureHttpClient() {
    try {
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
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException(e);
    }
  }

  protected JsonNode post(URI uri, String contentType, BodyPublisher bodyPublisher) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("Content-Type", contentType)
        .header("Accept", "application/json")
        .POST(bodyPublisher)
        .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return JsonNodes.toJsonNode(response.body()); // JSON 파싱
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
