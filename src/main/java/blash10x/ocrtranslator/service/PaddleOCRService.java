package blash10x.ocrtranslator.service;

import blash10x.ocrtranslator.util.Images;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.util.stream.Collectors;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Author: myungsik.sung@gmail.com
 */
public class PaddleOCRService extends AbstractProcessService implements OCRService {
  private final String resultTextKey;
  private final String resultBoxKey;
  private final String command;
  private final ResultCollector resultCollector;

  public PaddleOCRService() {
    super("paddleocr");

    resultTextKey = configLoader.getProperty("ocr.paddleocr.output.json.result-texts");
    resultBoxKey = configLoader.getProperty("ocr.paddleocr.output.json.result-boxes");
    command = configLoader.getProperty("ocr.paddleocr.command");

    String pipeName = configLoader.getProperty("ocr.paddleocr.output.pipe-name");
    resultCollector = new ResultCollector(pipeName);
  }

  @Override
  public void initialize() {
    start(command, resultCollector);
  }

  @Override
  public OCRResult doOCR(Image image, File imagePath) {
    try {
      writeToProcess(imagePath + "\n"); // command to OCR:

      synchronized (resultCollector) {
        resultCollector.wait();
      }

      String resultText= collectTexts(resultCollector.getResult(), resultTextKey);
      Image boxedImage = drawBoxes(image, resultCollector.getResult());
      return new OCRResult(resultText, boxedImage);
    } catch (Exception e) {
      return new OCRResult(e.toString(), image);
    }
  }

  private String collectTexts(JsonNode jsonNode, String fieldName) {
    JsonNode resultNodes = jsonNode.get(fieldName);
    return resultNodes.valueStream()
        .map(JsonNode::textValue)
        .collect(Collectors.joining("\n"));
  }

  private WritableImage drawBoxes(Image image, JsonNode result) {
    Mat imageMat = Images.writableImageToMat((WritableImage)image, null);

    JsonNode boxNodes = result.get(resultBoxKey);
    for (JsonNode position : boxNodes) {
      // 1. 사각형의 좌표 정의
      Point pt1 = new Point(position.get(0).intValue(), position.get(1).intValue()); // 왼쪽 상단 (x, y)
      Point pt2 = new Point(position.get(2).intValue(), position.get(3).intValue()); // 오른쪽 하단 (x, y)

      // 2. 색상 정의 (OpenCV는 BGR 순서 사용: Blue, Green, Red)
      Scalar color = new Scalar(0, 255, 0, 255); // Green (Blue=0, Green=255, Red=0, Alpha=투명도)

      // 4. 선 두께 (픽셀)
      int thickness = 1;

      // 5. 사각형 그리기
      Imgproc.rectangle(
          imageMat, // 사각형을 그릴 Mat
          pt1,      // 왼쪽 상단 좌표
          pt2,      // 오른쪽 하단 좌표
          color,    // 색상
          thickness // 두께 (-1이면 채워진 사각형)
      );
    }

    return Images.matToWritableImage(imageMat, (WritableImage)image);
  }
}