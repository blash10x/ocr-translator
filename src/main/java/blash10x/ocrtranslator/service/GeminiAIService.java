package blash10x.ocrtranslator.service;

import blash10x.ocrtranslator.util.JsonNodes;
import com.fasterxml.jackson.databind.JsonNode;
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
 * Author: myungsik.sung@gmail.com
 */
public class GeminiAIService implements TranslationService {
  private final Map<String, String> cache = new HashMap<>();
  private final Client client;
  private final GenerateContentConfig generateContentConfig;
  private final String model;
  private final String promptTemplate;

  public GeminiAIService() {
    String apiKey =  configLoader.getProperty("translation.gemini-ai.api-key");
    client = Client.builder()
        .apiKey(apiKey)
        //.vertexAI(true)
        .build();
    model =  configLoader.getProperty("translation.gemini-ai.model-name");
    promptTemplate = configLoader.getProperty("translation.gemini-webapi.prompt-template");
    generateContentConfig = createGenerateContentConfig();
  }

  private GenerateContentConfig createGenerateContentConfig() {
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

    return GenerateContentConfig.builder()
        .responseMimeType("application/json")
        .temperature(0.5f)
        .topP(0.8f)
        .safetySettings(safetySettings)
        .build();
  }

  @Override
  public String translate(String textToTranslate) {
    return cache.computeIfAbsent(textToTranslate, key -> _translate(textToTranslate));
  }

  public String _translate(String textToTranslate) {
    String prompt = String.format(promptTemplate, textToTranslate);
    System.out.println("[GeminiAI:prompt]:\n" + prompt);

    GenerateContentResponse response =
        client.models.generateContent(model, prompt, generateContentConfig);

    String responseText = response.text();
    System.out.println("[GeminiAI:response]:\n" + responseText);

    JsonNode jsonNode = JsonNodes.toJsonNode(responseText);
    return jsonNode.get("target").textValue();
  }
}