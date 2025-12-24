// src/main/java/blash10x/ocrtranslator/service/OCRService.java
package blash10x.ocrtranslator.service;

import java.io.File;
import javafx.scene.image.Image;

/**
 * Author: myungsik.sung@gmail.com
 */
public interface OCRService {

  OCRResult doOCR(Image image, File imagePath);

  void close();
}