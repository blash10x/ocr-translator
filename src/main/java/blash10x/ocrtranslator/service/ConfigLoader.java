package blash10x.ocrtranslator.service;

import blash10x.ocrtranslator.App;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Author: myungsik.sung@gmail.com
 */
public class ConfigLoader {
  private static final Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");

  @Getter
  private final Properties properties = new Properties();

  public static ConfigLoader getConfigLoader() {
    return new ConfigLoader();
  }

  private ConfigLoader() {
    try (InputStream input = App.class.getResourceAsStream("config.properties")) {
      properties.load(input);
    } catch (IOException e) {
      System.err.println("Could not load config.properties: " + e.getMessage());
    }
  }

  public String getProperty(String key) {
    String value = properties.getProperty(key);
    Matcher matcher = pattern.matcher(value);
    StringBuilder builder = new StringBuilder();
    while (matcher.find()) {
      String placeholderKey = matcher.group(1);
      String placeholderValue = properties.getProperty(placeholderKey);
      if (placeholderValue == null) {
        // Handle cases where the placeholder cannot be resolved (e.g., throw an error or use a default)
        System.err.println("Warning: Could not resolve placeholder: " + placeholderKey);
        placeholderValue = matcher.group(0); // Keep original placeholder if not found
      }
      matcher.appendReplacement(builder, Matcher.quoteReplacement(placeholderValue));
    }
    if (!builder.isEmpty()) {
      matcher.appendTail(builder);
      value = builder.toString();
      properties.put(key, value);
    }
    return pattern.matcher(value).find() ? getProperty(key) :value;
  }

  public Map<Object, Object> startsWith(String key) {
    return properties.entrySet().stream()
        .filter(entry -> entry.getKey().toString().startsWith(key))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }
}
