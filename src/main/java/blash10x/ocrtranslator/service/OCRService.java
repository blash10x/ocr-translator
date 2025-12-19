// src/main/java/blash10x/ocrtranslator/service/OCRService.java
package blash10x.ocrtranslator.service;

import java.io.File;

/**
 * 스크린샷 이미지에서 텍스트를 추출하는 OCR 서비스입니다.
 * <p/>
 * Author: myungsik.sung@gmail.com
 */
public class OCRService {
  private final PaddleOCRService paddleOCRService;

  public OCRService() {
    paddleOCRService = new PaddleOCRService();
  }

  public OCRResult doOCR(File imagePath) {
    return paddleOCRService.doOCR(imagePath);
  }
}