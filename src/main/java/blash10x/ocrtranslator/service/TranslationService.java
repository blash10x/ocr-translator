package blash10x.ocrtranslator.service;

/**
 * Author: myungsik.sung@gmail.com
 */
public interface TranslationService {
  ConfigLoader configLoader = ConfigLoader.getConfigLoader();

  default void close() {
  }

  String translate(String textToTranslate);
}
