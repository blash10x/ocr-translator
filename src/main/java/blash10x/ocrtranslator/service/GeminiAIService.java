// src/main/java/blash10x/ocrtranslator/service/TranslationService.java
package blash10x.ocrtranslator.service;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HarmBlockThreshold;
import com.google.genai.types.HarmCategory;
import com.google.genai.types.SafetySetting;
import java.util.HashMap;
import java.util.Map;

/**
 * 외부 번역 API를 호출하여 텍스트를 번역하는 서비스입니다.
 * <p/>
 * Author: myungsik.sung@gmail.com
 */
public class GeminiAIService {
  private final Map<String, String> cache = new HashMap<>();
  private final String model;
  private final Client client;
  private final GenerateContentConfig generateContentConfig;

  public GeminiAIService() {
    ConfigLoader configLoader = ConfigLoader.getConfigLoader();

    model =  configLoader.getProperty("translation.gemini-ai.model-name");

    String apiKey =  configLoader.getProperty("translation.gemini-ai.api-key");
    client = Client.builder()
        .apiKey(apiKey)
        //.vertexAI(true)
        .build();

    ImmutableList<SafetySetting> safetySettings =
        ImmutableList.of(
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                .threshold(HarmBlockThreshold.Known.BLOCK_ONLY_HIGH)
                .build(),
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH)
                .threshold(HarmBlockThreshold.Known.BLOCK_ONLY_HIGH)
                .build(),
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT)
                .threshold(HarmBlockThreshold.Known.BLOCK_NONE)
                .build());

    generateContentConfig = GenerateContentConfig.builder()
        .temperature(0.5f)
        .topP(0.8f)
        .safetySettings(safetySettings)
        .build();
  }

  public String translate(String textToTranslate) {
    return cache.computeIfAbsent(textToTranslate, key -> _translate(textToTranslate));
  }

  private String _translate(String textToTranslate) {
    String text = "일본어:\n" + textToTranslate + "\n\n한국어로 번역.\n"
        + "원문의 줄바꿈을 번역문의 줄바꿈에도 동일하게 적용.\n"
        + "부가 설명없이 번역 결과만 응답.";
    GenerateContentResponse response =
        client.models.generateContent(model, text, generateContentConfig);

    return response.text();
  }
}