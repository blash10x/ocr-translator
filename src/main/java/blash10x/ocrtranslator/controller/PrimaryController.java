package blash10x.ocrtranslator.controller;

import static blash10x.ocrtranslator.util.ConsoleColors.CYAN;
import static blash10x.ocrtranslator.util.ConsoleColors.CYAN_BRIGHT;
import static blash10x.ocrtranslator.util.ConsoleColors.GREEN_BRIGHT;

import blash10x.ocrtranslator.App;
import blash10x.ocrtranslator.service.GeminiWebApiService;
import blash10x.ocrtranslator.service.OCRResult;
import blash10x.ocrtranslator.service.OCRService;
import blash10x.ocrtranslator.service.PaddleOCRService;
import blash10x.ocrtranslator.service.TranslationWebApiService;
import blash10x.ocrtranslator.service.TranslationService;
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
  private final TranslationService translationN2mtService;
  private final TranslationService geminiWebApiService;

  @FXML
  private ImageView primaryImageView;
  @FXML
  private TextArea ocrTextArea; // OCR 결과
  @FXML
  private TextArea textArea2; // 번역 결과
  @FXML
  private TextArea textArea3; // 번역 결과

  private SecondaryController secondaryController;
  private Stage secondaryStage;

  public PrimaryController() {
    ocrService = new PaddleOCRService();
    translationN2mtService = new TranslationWebApiService("n2mt");
    geminiWebApiService = new GeminiWebApiService();
  }

  @FXML
  public void initialize() {
    // TextArea를 Clipping 가능하도록 설정 (CSS로 구현)
    // 실제 Clipping 기능은 사용자 인터페이스 로직에 따라 구현해야 합니다.
    // 여기서는 TextAreas에 텍스트를 표시하는 기능만 포함합니다.
    // CSS 예시: .clippable-textarea { -fx-background-color: lightgray; -fx-border-color: darkgray; }
    ocrTextArea.getStyleClass().add("clippable-textarea");
    textArea2.getStyleClass().add("clippable-textarea");
    textArea3.getStyleClass().add("clippable-textarea");
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
    secondaryController.setPrimaryController(this);

    //Stage secondaryStage = new Stage();
    secondaryStage = new Stage();
    secondaryStage.setAlwaysOnTop(true);

    secondaryStage.initModality(Modality.NONE);
    secondaryStage.setTitle("Capture Window");

    Scene secondaryScene = new Scene(secondaryRoot, 600, 400); // Scene 생성 시 초기 너비와 높이를 지정합니다.
    secondaryStage.setScene(secondaryScene);

    secondaryController.setStage(secondaryStage);

    secondaryStage.show();
  }

  @FXML
  private void translate() {
    String textResult = ocrTextArea.getText();
    System.out.println("[번역 원문]:\n" + CYAN(textResult));

    App.EXECUTOR_SERVICE.submit(() -> {
      String translatedText = translationN2mtService.translate(textResult);
      System.out.printf("[번역 결과(%s)]:%n%s%n",
          translationN2mtService.getName(), GREEN_BRIGHT(translatedText));

      Platform.runLater(() -> textArea2.setText(translatedText));
    });
    App.EXECUTOR_SERVICE.submit(() -> {
      String translatedText = geminiWebApiService.translate(textResult);
      System.out.printf("[번역 결과(%s)]:%n%s%n",
          geminiWebApiService.getName(), GREEN_BRIGHT(translatedText));

      Platform.runLater(() -> textArea3.setText(translatedText));
    });
  }

  /**
   * @param image Captured JavaFX Image
   */
  public void displayCapturedImage(Image image, File imagePath) {
    primaryImageView.setImage(image);
    ocrTextArea.setText("처리 중입니다...");
    textArea2.setText("...");
    textArea3.setText("...");

    App.EXECUTOR_SERVICE.submit(() -> {
      OCRResult ocrResult = ocrService.doOCR(image, imagePath);
      System.out.println("[OCR 결과]:\n" + CYAN_BRIGHT(ocrResult.text()));

      Platform.runLater(() -> {
        primaryImageView.setImage(ocrResult.boxedImage());
        ocrTextArea.setText(ocrResult.text());
        translate();
      });
    });
  }
}