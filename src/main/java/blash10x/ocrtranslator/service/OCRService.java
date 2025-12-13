// src/main/java/blash10x/ocrtranslator/service/OCRService.java
package blash10x.ocrtranslator.service;

import java.util.Properties;
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
  private final TesseractOCRService tesseractOCRService;

  public OCRService(Properties properties) {
    paddleOCRService = new PaddleOCRService(properties);
    tesseractOCRService = new TesseractOCRService(properties);
  }

  public String doOCR(Image image, ImageView imageView, TextArea textArea) {
    tesseractOCRService.doOCR(image, imageView, textArea);
    return paddleOCRService.ocr(imageView, textArea);
  }
}