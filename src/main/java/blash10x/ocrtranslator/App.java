// src/main/java/blash10x/ocrtranslator/App.java
package blash10x.ocrtranslator;

import blash10x.ocrtranslator.controller.PrimaryController;
import blash10x.ocrtranslator.service.OCRService;
import blash10x.ocrtranslator.service.TranslationService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

  private OCRService ocrService;
  private TranslationService translationService;

  @Override
  public void init() throws Exception {
    super.init();
    // 서비스 초기화 (애플리케이션 시작 시 한 번만)
    ocrService = new OCRService();
    translationService = new TranslationService();
  }

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("primary-view.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 800, 600);
    PrimaryController primaryController = fxmlLoader.getController();

    // 컨트롤러에 서비스 주입
    primaryController.setOcrService(ocrService);
    primaryController.setTranslationService(translationService);

    stage.setTitle("Primary OCR Translator");
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch();
  }
}