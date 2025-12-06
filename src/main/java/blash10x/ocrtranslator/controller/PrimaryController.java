// src/main/java/blash10x/ocrtranslator/controller/PrimaryController.java
package blash10x.ocrtranslator.controller;

import blash10x.ocrtranslator.App;
import blash10x.ocrtranslator.service.OCRService;
import blash10x.ocrtranslator.service.TranslationService;
import javafx.application.Platform; // Platform import 추가
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class PrimaryController {

  @FXML
  private ImageView primaryImageView;
  @FXML
  private TextArea textArea1; // OCR 결과
  @FXML
  private TextArea textArea2; // 번역 결과

  private SecondaryController secondaryController;
  private OCRService ocrService;
  private TranslationService translationService;

  // 서비스 주입용 세터
  public void setOcrService(OCRService ocrService) {
    this.ocrService = ocrService;
  }

  public void setTranslationService(TranslationService translationService) {
    this.translationService = translationService;
  }

  @FXML
  public void initialize() {
    // TextArea를 Clipping 가능하도록 설정 (CSS로 구현)
    // 실제 Clipping 기능은 사용자 인터페이스 로직에 따라 구현해야 합니다.
    // 여기서는 TextAreas에 텍스트를 표시하는 기능만 포함합니다.
    // CSS 예시: .clippable-textarea { -fx-background-color: lightgray; -fx-border-color: darkgray; }
    textArea1.getStyleClass().add("clippable-textarea");
    textArea2.getStyleClass().add("clippable-textarea");
  }

  @FXML
  private void openCaptureWindow() throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("secondary-view.fxml"));
    Pane secondaryRoot = fxmlLoader.load();
    secondaryController = fxmlLoader.getController();
    secondaryController.setPrimaryController(this); // SecondaryController에 PrimaryController 전달

    Stage secondaryStage = new Stage();
    secondaryStage.initModality(Modality.NONE);
    secondaryStage.setTitle("Capture Window");

    // Scene 생성 시 초기 너비와 높이를 지정합니다.
    Scene secondaryScene = new Scene(secondaryRoot, 600, 400); // 이전 단계에서 요청하신 초기 크기 지정
    secondaryStage.setScene(secondaryScene);

    // SecondaryController에 Stage 설정
    secondaryController.setStage(secondaryStage);

    secondaryStage.show();
  }

  /**
   * Secondary Stage에서 캡처된 이미지를 받아 ImageView에 표시하고 OCR 및 번역을 수행합니다.
   * @param image 캡처된 JavaFX Image
   */
  public void displayCapturedImage(Image image) {
    primaryImageView.setImage(image);

    // UI 업데이트는 JavaFX Application Thread에서 실행
    Platform.runLater(() -> {
      // 1. OCR 수행 (별도 스레드에서 실행하여 UI 스레드 블로킹 방지)
      new Thread(() -> {
        String ocrResult = ocrService.doOCR(image);
        Platform.runLater(() -> {
          textArea1.setText(ocrResult); // 첫 번째 TextArea에 OCR 결과 표시
          System.out.println("OCR 결과:\n" + ocrResult);

          // 2. 번역 수행 (OCR 결과가 나온 후 별도 스레드에서 실행)
          new Thread(() -> {
            String translatedText = translationService.translate(ocrResult);
            Platform.runLater(() -> {
              textArea2.setText(translatedText); // 두 번째 TextArea에 번역 결과 표시
              System.out.println("번역 결과:\n" + translatedText);
            });
          }).start();
        });
      }).start();
    });
  }
}