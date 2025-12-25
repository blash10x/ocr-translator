package blash10x.ocrtranslator.service;

import java.net.http.HttpClient;
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

  // 개발 환경용: SSL 인증서 검증 무시 HttpClient
  HttpClient createInsecureHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
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
