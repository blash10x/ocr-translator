package blash10x.ocrtranslator.service;

/**
 * Author: myungsik.sung@gmail.com
 */
public interface TranslationService {
  ConfigLoader configLoader = ConfigLoader.getConfigLoader();

  default String getName() {
    return "Translation";
  };

  String translate(String textToTranslate);

  default void close() {
  }
}
