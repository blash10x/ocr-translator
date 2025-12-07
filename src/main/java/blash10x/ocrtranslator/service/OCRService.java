// src/main/java/blash10x/ocrtranslator/service/OCRService.java
package blash10x.ocrtranslator.service;

import java.util.Properties;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * 스크린샷 이미지에서 텍스트를 추출하는 OCR 서비스입니다.
 * <p/>
 * Author: myungsik.sung@gmail.com
 */
public class OCRService {
  private final ITesseract tesseract;

  public OCRService(Properties properties) {
    String tessdataPath = properties.getProperty("ocr.tesseract.tessdata.path");
    String language = properties.getProperty("ocr.tesseract.language");

    tesseract = new Tesseract();
    tesseract.setDatapath(tessdataPath); // tessdata 경로 설정
    tesseract.setLanguage(language);

    properties.entrySet().stream()
        .filter(entry -> entry.getKey().toString().startsWith("ocr.tesseract.variable"))
        .forEach(entry -> {
          String key = entry.getKey().toString().substring(23);
          String value = entry.getValue().toString();
          tesseract.setVariable(key, value);
        });
  }

  /**
   * JavaFX Image에서 텍스트를 추출합니다.
   * @param image OCR을 수행할 JavaFX Image
   * @return 추출된 텍스트, 오류 발생 시 빈 문자열
   */
  public String doOCR(Image image) {
    if (image == null) {
      return "";
    }

    // JavaFX Image를 BufferedImage로 변환
    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

    // OCR 수행
    try {
      return tesseract.doOCR(bufferedImage);
    } catch (TesseractException e) {
      System.err.println("OCR 처리 중 오류 발생: " + e.getMessage());
      return "";
    }
  }

  /**
   * BufferedImage를 임시 파일로 저장하는 유틸리티 메서드 (디버깅용)
   * @param img 저장할 BufferedImage
   * @param formatName 파일 형식 (예: "png")
   * @param filePath 저장될 파일 경로
   * @throws IOException 파일 쓰기 오류 발생 시
   */
  private void saveBufferedImage(BufferedImage img, String formatName, String filePath) throws IOException {
    File outputfile = new File(filePath);
    ImageIO.write(img, formatName, outputfile);
  }
}