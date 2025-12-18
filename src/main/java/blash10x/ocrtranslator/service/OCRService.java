// src/main/java/blash10x/ocrtranslator/service/OCRService.java
package blash10x.ocrtranslator.service;

import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * 스크린샷 이미지에서 텍스트를 추출하는 OCR 서비스입니다.
 * <p/>
 * Author: myungsik.sung@gmail.com
 */
public class OCRService {
  private final PaddleOCRService paddleOCRService;
//  private final TesseractOCRService tesseractOCRService;

  public OCRService() {
    paddleOCRService = new PaddleOCRService();
//    tesseractOCRService = new TesseractOCRService();
  }

  public String doOCR(Image image, ImageView imageView, TextArea textArea) {
//    String resultText = tesseractOCRService.doOCR(image, imageView);
//    System.out.println("tesseract: " + resultText);
//    textArea.setText(resultText);
    String resultText = paddleOCRService.doOCR(imageView);
//    System.out.println("paddleOCR: " + resultText);
    textArea.setText(resultText);
    return resultText;
  }

  public void close() {
    paddleOCRService.close();
  }
}