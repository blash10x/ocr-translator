package blash10x.ocrtranslator.util;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Author: myungsik.sung@gmail.com
 */
public class Images {
  static {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
  }

  public static Mat writableImageToMat(WritableImage image, Mat mat) {
    int width = (int) image.getWidth();
    int height = (int) image.getHeight();

    // BGRA 포맷으로 Mat 초기화 (JavaFX의 기본 픽셀 포맷은 보통 BGRA 계열)
    if (mat == null) {
      mat = new Mat(height, width, CvType.CV_8UC4);
    }

    byte[] buffer = new byte[width * height * 4];
    PixelReader reader = image.getPixelReader();

    // 픽셀 데이터를 BGRA 바이트 배열로 읽기
    reader.getPixels(0, 0, width, height,
        javafx.scene.image.PixelFormat.getByteBgraInstance(),
        buffer, 0, width * 4);

    // Mat에 데이터 넣기
    mat.put(0, 0, buffer);
    return mat;
  }

  public static WritableImage matToWritableImage(Mat mat, WritableImage image) {
    int width = mat.cols();
    int height = mat.rows();
    int channels = mat.channels();

    byte[] buffer = new byte[width * height * channels];
    mat.get(0, 0, buffer); // Mat 데이터를 배열로 복사

    if (image == null) {
      image = new WritableImage(width, height);
    }
    PixelWriter writer = image.getPixelWriter();

    // 채널 수에 따른 포맷 결정
    if (channels == 3) {
      // BGR (OpenCV 기본)
      writer.setPixels(0, 0, width, height,
          javafx.scene.image.PixelFormat.getByteRgbInstance(),
          buffer, 0, width * 3);
    } else if (channels == 4) {
      // BGRA
      writer.setPixels(0, 0, width, height,
          javafx.scene.image.PixelFormat.getByteBgraInstance(),
          buffer, 0, width * 4);
    } else if (channels == 1) {
      // 그레이스케일 처리 (별도의 루프나 변환 필요)
      // 여기서는 생략하나, 보통 Mat을 3채널로 변환 후 처리하는 것이 간편합니다.
    }

    return image;
  }
}
