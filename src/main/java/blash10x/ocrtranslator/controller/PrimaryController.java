// src/main/java/blash10x/ocrtranslator/controller/PrimaryController.java
package blash10x.ocrtranslator.controller;

import static blash10x.ocrtranslator.util.ConsoleColors.BLUE;
import static blash10x.ocrtranslator.util.ConsoleColors.CYAN;
import static blash10x.ocrtranslator.util.ConsoleColors.RESET;

import blash10x.ocrtranslator.App;
import blash10x.ocrtranslator.service.GeminiWebApiService;
import blash10x.ocrtranslator.service.OCRResult;
import blash10x.ocrtranslator.service.OCRService;
import blash10x.ocrtranslator.service.TranslationN2mtService;
import java.io.File;
import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Author: myungsik.sung@gmail.com
 */
public class PrimaryController {
  private final OCRService ocrService;
  private final TranslationN2mtService translationN2mtService;
  private final GeminiWebApiService geminiWebApiService;

  @FXML
  private ImageView primaryImageView;
  @FXML
  private TextArea textArea1; // OCR 결과
  @FXML
  private TextArea textArea2; // 번역 결과
  @FXML
  private TextArea textArea3; // 번역 결과

  private SecondaryController secondaryController;
  private Stage secondaryStage;

  public PrimaryController() {
    ocrService = new OCRService();
    translationN2mtService = new TranslationN2mtService();
    geminiWebApiService = new GeminiWebApiService();
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

  public void close() {
    ocrService.close();
    geminiWebApiService.close();
    Platform.exit();
  }

  @FXML
  private void openCaptureWindow() throws IOException {
    if (secondaryStage != null) {
      if (secondaryStage.isShowing()) {
        secondaryController.handleCapture();
      } else {
        secondaryStage.show();
      }
      return;
    }

    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("secondary-view.fxml"));
    Pane secondaryRoot = fxmlLoader.load();
    secondaryController = fxmlLoader.getController();
    secondaryController.setPrimaryController(this); // SecondaryController에 PrimaryController 전달

    //Stage secondaryStage = new Stage();
    secondaryStage = new Stage();
    secondaryStage.setAlwaysOnTop(true);

    secondaryStage.initModality(Modality.NONE);
    secondaryStage.setTitle("Capture Window");

    // Scene 생성 시 초기 너비와 높이를 지정합니다.
    Scene secondaryScene = new Scene(secondaryRoot, 600, 400);
    secondaryStage.setScene(secondaryScene);

    // SecondaryController에 Stage 설정
    secondaryController.setStage(secondaryStage);

    secondaryStage.show();
  }

  @FXML
  private void translate() {
    String textResult = textArea1.getText();
    System.out.println("[OCR]:\n" + CYAN + textResult + RESET);

    // 2. 번역 수행 (OCR 결과가 나온 후 별도 스레드에서 실행)
    Platform.runLater(() -> {
      new Thread(() -> {
        String translatedText = translationN2mtService.translate(textResult);
        textArea2.setText(translatedText); // 두 번째 TextArea에 번역 결과 표시
        System.out.println("[번역 결과(n2mt)]:\n" + BLUE + translatedText + RESET);
      }).start();
    });
    Platform.runLater(() -> {
      new Thread(() -> {
        String translatedText = geminiWebApiService.translate(textResult);
        textArea3.setText(translatedText); // 세 번째 TextArea에 번역 결과 표시
        System.out.println("[번역 결과(gemini)]:\n" + BLUE + translatedText + RESET);
      }).start();
    });
  }

  /**
   * Secondary Stage에서 캡처된 이미지를 받아 ImageView에 표시하고 OCR 및 번역을 수행합니다.
   * @param image 캡처된 JavaFX Image
   */
  public void displayCapturedImage(Image image, File imagePath) {
    primaryImageView.setImage(image);
    textArea1.setText("처리 중입니다...");
    textArea2.setText("...");
    textArea3.setText("...");

    // UI 업데이트는 JavaFX Application Thread에서 실행
    Platform.runLater(() -> {
      // 1. OCR 수행 (별도 스레드에서 실행하여 UI 스레드 블로킹 방지)
      new Thread(() -> {
        OCRResult ocrResult = ocrService.doOCR(imagePath);
        primaryImageView.setImage(ocrResult.boxedImage());

        String textResult = ocrResult.text();
        textArea1.setText(textResult); // 첫 번째 TextArea에 OCR 결과 표시
        System.out.println("[OCR 결과]:\n" + CYAN + textResult + RESET);

        translate();
      }).start();
    });
  }
}