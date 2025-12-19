package blash10x.ocrtranslator.service;

import javafx.scene.image.Image;

/**
 * Author: myungsik.sung@gmail.com
 */
public record OCRResult(String text, Image boxedImage) {
}