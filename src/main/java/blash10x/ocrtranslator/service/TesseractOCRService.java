// src/main/java/blash10x/ocrtranslator/service/OCRService.java
package blash10x.ocrtranslator.service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITessAPI.TessOcrEngineMode;
import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.ITessAPI.TessPageSegMode;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * 스크린샷 이미지에서 텍스트를 추출하는 OCR 서비스입니다.
 * <p/>
 * Author: myungsik.sung@gmail.com
 */
public class TesseractOCRService {
  private final ITesseract tesseract;

  static {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
  }

  public TesseractOCRService(Properties properties) {
    String tessdataPath = properties.getProperty("ocr.tesseract.tessdata.path");
    String language = properties.getProperty("ocr.tesseract.language");

    tesseract = new Tesseract();
    tesseract.setDatapath(tessdataPath); // tessdata 경로 설정
    tesseract.setLanguage(language);
    tesseract.setOcrEngineMode(TessOcrEngineMode.OEM_LSTM_ONLY);
    tesseract.setPageSegMode(TessPageSegMode.PSM_SINGLE_BLOCK);

    properties.entrySet().stream()
        .filter(entry -> entry.getKey().toString().startsWith("ocr.tesseract.variable"))
        .forEach(entry -> {
          String key = entry.getKey().toString().substring(23);
          String value = entry.getValue().toString();
          tesseract.setVariable(key, value);
        });
  }

  public String doOCR(Image image, ImageView imageView) {
    List<Word> ocrWords = getWords(image);
    BufferedImage originalBfImage = convertFxImageToBufferedImage(image);
    BufferedImage modifiedBfImage = drawBoxes(ocrWords, originalBfImage);
    Image modifiedFxImage = convertBufferedImageToFxImage(modifiedBfImage);
    imageView.setImage(modifiedFxImage);
    String ocrResult = ocrWords.stream().map(Word::getText).collect(Collectors.joining());
    System.out.println("tesseract: " + ocrResult);
    return ocrResult;
  }

  private List<Word> getWords(Image image) {
    if (image == null) {
      return Collections.emptyList();
    }

    // JavaFX Image를 BufferedImage로 변환
    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

    // OCR 수행
    return tesseract.getWords(bufferedImage, TessPageIteratorLevel.RIL_WORD);
  }

  private BufferedImage drawBoxes(List<Word> words, BufferedImage originalBfImage) {
    Mat imageMat = convertBufferedImageToMat(originalBfImage);
    System.out.println("--- OCR Results with Locations ---");
    for (Word word : words) {
      // 단어 텍스트
      String wordText = word.getText();
      // 단어의 바운딩 박스 (x, y, width, height)
      java.awt.Rectangle rect = word.getBoundingBox();
      // 신뢰도 (confidence)
      float confidence = word.getConfidence();

      System.out.printf(
          "Word: '%s' | Location: [x:%d, y:%d, w:%d, h:%d] | Confidence: %.2f\n",
          wordText,
          rect.x,
          rect.y,
          rect.width,
          rect.height,
          confidence
      );

      // 2. 사각형의 좌표 정의
      Point pt1 = new Point(rect.x, rect.y); // 왼쪽 상단 (x, y)
      Point pt2 = new Point(rect.x + rect.width, rect.y + rect.height); // 오른쪽 하단 (x, y)

      // 3. 색상 정의 (OpenCV는 BGR 순서 사용: Blue, Green, Red)
      Scalar color = new Scalar(0, 255, 0); // 빨간색 (Blue=0, Green=255, Red=0)

      // 4. 선 두께 (픽셀)
      int thickness = 1;

      // 5. 사각형 그리기
      Imgproc.rectangle(
          imageMat, // 사각형을 그릴 Mat
          pt1,                // 왼쪽 상단 좌표
          pt2,                // 오른쪽 하단 좌표
          color,              // 색상
          thickness           // 두께 (-1이면 채워진 사각형)
      );
    }

    return convertMatToBufferedImage(imageMat);
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

  // Helper: JavaFX Image를 BufferedImage로 변환
  private BufferedImage convertFxImageToBufferedImage(Image fxImage) {
    return SwingFXUtils.fromFXImage(fxImage, null);
  }

  // Helper: BufferedImage를 Mat으로 변환 (모든 BufferedImage 타입을 처리하도록 개선)
  private Mat convertBufferedImageToMat(BufferedImage bImage) {
    // 입력된 BufferedImage의 타입을 확인하고 필요시 TYPE_3BYTE_BGR로 강제 변환
    // 이렇게 하면 DataBufferByte를 항상 사용할 수 있게 됩니다.
    if (bImage.getType() != BufferedImage.TYPE_3BYTE_BGR) {
      BufferedImage convertedImg = new BufferedImage(bImage.getWidth(), bImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
      convertedImg.getGraphics().drawImage(bImage, 0, 0, null);
      bImage = convertedImg;
    }

    byte[] data = ((DataBufferByte) bImage.getRaster().getDataBuffer()).getData();
    Mat mat = new Mat(bImage.getHeight(), bImage.getWidth(), CvType.CV_8UC3); // 3채널 (BGR)
    mat.put(0, 0, data);
    return mat;
  }

  // Helper: Mat을 BufferedImage로 변환
  private BufferedImage convertMatToBufferedImage(Mat mat) {
    int type = BufferedImage.TYPE_BYTE_GRAY;
    if (mat.channels() > 1) {
      type = BufferedImage.TYPE_3BYTE_BGR;
    }
    int bufferSize = mat.channels() * mat.cols() * mat.rows();
    byte[] b = new byte[bufferSize];
    mat.get(0, 0, b); // Mat의 데이터를 byte 배열로 가져옴

    // 수정: Mat -> BufferedImage 변환 시, BufferedImage의 타입과 채널 수를 정확히 맞춰줍니다.
    // 예를 들어, `CvType.CV_8UC3` (3채널)은 `BufferedImage.TYPE_3BYTE_BGR`에 해당합니다.
    // `CvType.CV_8UC1` (1채널)은 `BufferedImage.TYPE_BYTE_GRAY`에 해당합니다.
    BufferedImage bImage;
    if (mat.channels() == 1) {
      bImage = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_BYTE_GRAY);
    } else if (mat.channels() == 3) {
      bImage = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_3BYTE_BGR);
    } else if (mat.channels() == 4) {
      bImage = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_4BYTE_ABGR);
    } else {
      // 다른 채널 수는 처리하기 어려우므로 예외 처리하거나 기본 타입으로 설정
      throw new IllegalArgumentException("Unsupported number of channels: " + mat.channels());
    }
    System.arraycopy(b, 0, ((DataBufferByte) bImage.getRaster().getDataBuffer()).getData(), 0, bufferSize);
    return bImage;
  }

  // Helper: BufferedImage를 JavaFX Image로 변환
  private Image convertBufferedImageToFxImage(BufferedImage bImage) {
    return SwingFXUtils.toFXImage(bImage, null);
  }
}