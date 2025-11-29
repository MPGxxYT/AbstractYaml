package me.mortaldev.config2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Writes configuration to YAML files with proper formatting and comments.
 */
class YamlConfigWriter {

  /**
   * Writes a config to a YAML file.
   */
  static void write(Config config, File file) {
    try {
      // Ensure parent directory exists
      if (!file.getParentFile().exists()) {
        file.getParentFile().mkdirs();
      }

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
        // Write file header if present
        String header = config.schema().fileHeader();
        if (header != null && !header.isEmpty()) {
          writeHeader(writer, header);
          writer.newLine();
        }

        // Group values by section
        Map<String, List<ConfigValue<?>>> sections = groupBySection(config);

        // Write each section
        boolean first = true;
        for (Map.Entry<String, List<ConfigValue<?>>> entry : sections.entrySet()) {
          if (!first) {
            writer.newLine(); // Blank line between sections
          }
          first = false;

          String sectionKey = entry.getKey();
          List<ConfigValue<?>> values = entry.getValue();

          // Write section header if it's a nested section
          if (!sectionKey.isEmpty()) {
            writer.write(sectionKey + ":");
            writer.newLine();
          }

          // Write values
          for (ConfigValue<?> value : values) {
            writeValue(writer, value, sectionKey);
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to write config to " + file, e);
    }
  }

  /**
   * Writes the file header comment.
   */
  private static void writeHeader(BufferedWriter writer, String header) throws IOException {
    String[] lines = header.split("\n");
    for (String line : lines) {
      if (line.trim().isEmpty()) {
        writer.write("#");
      } else if (line.trim().startsWith("#")) {
        writer.write(line);
      } else {
        writer.write("# " + line);
      }
      writer.newLine();
    }
  }

  /**
   * Groups config values by their top-level section.
   */
  private static Map<String, List<ConfigValue<?>>> groupBySection(Config config) {
    Map<String, List<ConfigValue<?>>> sections = new LinkedHashMap<>();

    for (ConfigValue<?> value : config.allValues().values()) {
      String section = getTopLevelSection(value.path());
      sections.computeIfAbsent(section, k -> new ArrayList<>()).add(value);
    }

    return sections;
  }

  /**
   * Gets the top-level section from a path.
   * For example, "keraunos.cooldown" returns "keraunos", "simple" returns "".
   */
  private static String getTopLevelSection(String path) {
    int dotIndex = path.indexOf('.');
    if (dotIndex == -1) {
      return "";
    }
    return path.substring(0, dotIndex);
  }

  /**
   * Gets the relative path after removing the section prefix.
   */
  private static String getRelativePath(String path, String section) {
    if (section.isEmpty()) {
      return path;
    }
    if (path.startsWith(section + ".")) {
      return path.substring(section.length() + 1);
    }
    return path;
  }

  /**
   * Writes a single config value.
   */
  private static void writeValue(BufferedWriter writer, ConfigValue<?> value, String section)
      throws IOException {
    String relativePath = getRelativePath(value.path(), section);
    String[] parts = relativePath.split("\\.");
    int baseIndent = section.isEmpty() ? 0 : 1;

    // Write nested structure
    for (int i = 0; i < parts.length - 1; i++) {
      String indent = getIndent(baseIndent + i);
      writer.write(indent + parts[i] + ":");
      writer.newLine();
    }

    // Write the key-value pair with optional comment
    String indent = getIndent(baseIndent + parts.length - 1);
    String key = parts[parts.length - 1];
    String valueStr = formatValue(value.value());

    writer.write(indent + key + ": " + valueStr);

    // Add inline comment if present
    if (value.comment() != null && !value.comment().isEmpty()) {
      int currentLength = indent.length() + key.length() + 2 + valueStr.length();
      int targetColumn = 40; // Target column for comments
      int padding = Math.max(2, targetColumn - currentLength);
      writer.write(" ".repeat(padding) + "# " + value.comment());
    }

    writer.newLine();
  }

  /**
   * Formats a value for YAML output.
   */
  private static String formatValue(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof String str) {
      // Quote strings to avoid YAML parsing issues
      return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
    if (value instanceof Boolean || value instanceof Number) {
      return value.toString();
    }
    if (value instanceof List<?> list) {
      return formatList(list);
    }
    return value.toString();
  }

  /**
   * Formats a list for YAML output.
   */
  private static String formatList(List<?> list) {
    if (list.isEmpty()) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < list.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(formatValue(list.get(i)));
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Gets the indentation string for a given level.
   */
  private static String getIndent(int level) {
    return "  ".repeat(level);
  }
}
