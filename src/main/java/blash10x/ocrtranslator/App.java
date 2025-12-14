// src/main/java/blash10x/ocrtranslator/App.java
package blash10x.ocrtranslator;

import blash10x.ocrtranslator.controller.PrimaryController;
import blash10x.ocrtranslator.service.OCRService;
import blash10x.ocrtranslator.service.TranslationN2mtService;
import blash10x.ocrtranslator.service.TranslationNsmtService;
import java.io.InputStream;
import java.util.Properties;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Author: myungsik.sung@gmail.com
 */
public class App extends Application {
  private OCRService ocrService;
  private TranslationNsmtService translationNstmService;
  private TranslationN2mtService translationN2mtService;

  @Override
  public void init() throws Exception {
    super.init();

    // 서비스 초기화 (애플리케이션 시작 시 한 번만)
    Properties properties = new Properties();
    try (InputStream input = App.class.getResourceAsStream("config.properties")) {
      properties.load(input);
    } catch (IOException e) {
      System.err.println("Could not load config.properties: " + e.getMessage());
    }
    ocrService = new OCRService(properties);
    translationNstmService = new TranslationNsmtService(properties);
    translationN2mtService = new TranslationN2mtService(properties);
  }

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("primary-view.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 620, 830);
    PrimaryController primaryController = fxmlLoader.getController();

    primaryController.setOcrService(ocrService);
    primaryController.setTranslationNsmtService(translationNstmService);
    primaryController.setTranslationN2mtService(translationN2mtService);

    stage.setTitle("Primary OCR Translator");
    stage.setScene(scene);
    stage.show();

    stage.setOnCloseRequest(event -> Platform.exit());
  }

  public static void main(String[] args) {
    launch();
  }
}