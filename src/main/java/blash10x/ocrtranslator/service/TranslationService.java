package blash10x.ocrtranslator.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: myungsik.sung@gmail.com
 */
public interface TranslationService {
  ConfigLoader configLoader = ConfigLoader.getConfigLoader();
  Map<String, String> cache = new HashMap<>();

  default String getTranslatedText(String textToTranslate) {
    return cache.computeIfAbsent(textToTranslate, key -> translate(textToTranslate));
  }

  default void close() {
  }

  String translate(String textToTranslate);
}
