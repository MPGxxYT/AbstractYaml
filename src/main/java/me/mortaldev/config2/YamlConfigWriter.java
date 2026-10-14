package me.mortaldev.config2;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Renders configuration as YAML text with proper formatting and comments.
 *
 * <p>Both config systems write through this class: config2 via {@link Config#toYaml()}, and the
 * legacy {@code me.mortaldev.YamlWriter} via {@link #render(String, Function, List)}.
 */
public final class YamlConfigWriter {
  private static final int COMMENT_COLUMN = 40;
  private static final Pattern PLAIN_KEY = Pattern.compile("[A-Za-z0-9_-]+");

  private YamlConfigWriter() {}

  /**
   * One value to write: a dotted path, plain YAML data (String, Number, Boolean, List, Map),
   * and an optional inline comment.
   */
  public record Entry(String path, Object value, String comment) {}

  /**
   * Renders a config as YAML text. Values appear in schema declaration order, nested under their
   * sections, with section headers and value comments.
   */
  static String render(Config config) {
    List<Entry> entries = new ArrayList<>();
    for (ConfigValue<?> value : config.allValues().values()) {
      entries.add(new Entry(value.path(), value.value(), value.comment()));
    }
    ConfigSchema schema = config.schema();
    return render(schema.fileHeader(), schema::sectionHeader, entries);
  }

  /**
   * Renders entries as YAML text, in the given order, nested by their dotted paths.
   *
   * @param fileHeader comment written at the top of the file, or {@code null}
   * @param sectionHeaders returns the comment to write above a section (by full section path),
   *     or {@code null} for none
   * @param entries the values to write
   * @throws IllegalArgumentException if a value is not plain YAML data
   * @throws IllegalStateException if one path is used as both a value and a section
   */
  public static String render(
      String fileHeader, Function<String, String> sectionHeaders, List<Entry> entries) {
    StringBuilder out = new StringBuilder();

    if (fileHeader != null && !fileHeader.isEmpty()) {
      writeComment(out, 0, fileHeader);
      out.append('\n');
    }

    Map<String, Object> tree = new LinkedHashMap<>();
    for (Entry entry : entries) {
      insert(tree, entry);
    }
    writeSection(out, tree, 0, "", sectionHeaders);
    return out.toString();
  }

  /**
   * Places an entry in the tree; inner nodes are sections (maps), leaves are entries.
   */
  @SuppressWarnings("unchecked")
  private static void insert(Map<String, Object> tree, Entry entry) {
    String[] parts = entry.path().split("\\.");
    Map<String, Object> node = tree;
    for (int i = 0; i < parts.length - 1; i++) {
      Object child = node.computeIfAbsent(parts[i], k -> new LinkedHashMap<String, Object>());
      if (!(child instanceof Map)) {
        throw new IllegalStateException(
            "Path " + entry.path() + " is nested under a value, not a section");
      }
      node = (Map<String, Object>) child;
    }
    if (node.putIfAbsent(parts[parts.length - 1], entry) != null) {
      throw new IllegalStateException("Path " + entry.path() + " is used twice");
    }
  }

  @SuppressWarnings("unchecked")
  private static void writeSection(
      StringBuilder out,
      Map<String, Object> node,
      int level,
      String prefix,
      Function<String, String> sectionHeaders) {
    boolean first = true;
    boolean previousWasSection = false;
    for (Map.Entry<String, Object> entry : node.entrySet()) {
      String key = entry.getKey();
      String path = prefix.isEmpty() ? key : prefix + "." + key;
      boolean isSection = entry.getValue() instanceof Map;

      // Blank line around top-level sections
      if (level == 0 && !first && (isSection || previousWasSection)) {
        out.append('\n');
      }
      first = false;
      previousWasSection = isSection;

      if (isSection) {
        String sectionHeader = sectionHeaders.apply(path);
        if (sectionHeader != null && !sectionHeader.isEmpty()) {
          writeComment(out, level, sectionHeader);
        }
        out.append(getIndent(level)).append(key(key)).append(":\n");
        writeSection(out, (Map<String, Object>) entry.getValue(), level + 1, path, sectionHeaders);
      } else {
        Entry value = (Entry) entry.getValue();
        try {
          writeEntry(out, level, key, value.value(), value.comment());
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException(
              "Cannot write " + value.path() + ": " + e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Writes comment lines, prefixing "# " unless a line already starts with "#".
   */
  private static void writeComment(StringBuilder out, int level, String text) {
    for (String line : text.split("\n")) {
      out.append(getIndent(level));
      if (line.trim().isEmpty()) {
        out.append("#");
      } else if (line.trim().startsWith("#")) {
        out.append(line.trim());
      } else {
        out.append("# ").append(line);
      }
      out.append('\n');
    }
  }

  /**
   * Writes {@code key: value}, using block style for non-empty maps and for lists of strings or
   * maps, and inline style for everything else.
   */
  private static void writeEntry(
      StringBuilder out, int level, String key, Object value, String comment) {
    String indent = getIndent(level);
    if (value instanceof Map<?, ?> map && !map.isEmpty()) {
      out.append(indent).append(key(key)).append(':');
      appendComment(out, comment, 2);
      out.append('\n');
      for (Map.Entry<?, ?> e : map.entrySet()) {
        writeEntry(out, level + 1, String.valueOf(e.getKey()), e.getValue(), null);
      }
    } else if (value instanceof List<?> list && useBlockList(list)) {
      out.append(indent).append(key(key)).append(':');
      appendComment(out, comment, 2);
      out.append('\n');
      for (Object item : list) {
        writeListItem(out, level + 1, item);
      }
    } else {
      String line = indent + key(key) + ": " + formatValue(value);
      out.append(line);
      appendComment(out, comment, Math.max(2, COMMENT_COLUMN - line.length()));
      out.append('\n');
    }
  }

  /**
   * Writes one block-list item. A map item puts its first entry on the dash line.
   */
  private static void writeListItem(StringBuilder out, int level, Object item) {
    if (item instanceof Map<?, ?> map && !map.isEmpty()) {
      StringBuilder entries = new StringBuilder();
      for (Map.Entry<?, ?> e : map.entrySet()) {
        writeEntry(entries, level + 1, String.valueOf(e.getKey()), e.getValue(), null);
      }
      // Replace the first entry's indent with "- " at this level
      out.append(getIndent(level)).append("- ").append(entries, getIndent(level + 1).length(),
          entries.length());
    } else {
      out.append(getIndent(level)).append("- ").append(formatValue(item)).append('\n');
    }
  }

  private static void appendComment(StringBuilder out, String comment, int padding) {
    if (comment != null && !comment.isEmpty()) {
      out.append(" ".repeat(padding)).append("# ").append(comment);
    }
  }

  /**
   * Lists of strings or maps use one item per line; number and boolean lists stay inline.
   */
  private static boolean useBlockList(List<?> list) {
    for (Object item : list) {
      if (item instanceof String || item instanceof Map || item instanceof List) {
        return true;
      }
    }
    return false;
  }

  /**
   * Formats a value in YAML flow style.
   */
  private static String formatValue(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof String str) {
      return quote(str);
    }
    if (value instanceof Boolean || value instanceof Number) {
      return value.toString();
    }
    if (value instanceof List<?> list) {
      StringJoiner joiner = new StringJoiner(", ", "[", "]");
      for (Object item : list) {
        joiner.add(formatValue(item));
      }
      return joiner.toString();
    }
    if (value instanceof Map<?, ?> map) {
      StringJoiner joiner = new StringJoiner(", ", "{", "}");
      for (Map.Entry<?, ?> e : map.entrySet()) {
        joiner.add(key(String.valueOf(e.getKey())) + ": " + formatValue(e.getValue()));
      }
      return joiner.toString();
    }
    throw new IllegalArgumentException(
        "unsupported YAML type " + value.getClass().getName());
  }

  /** Quotes strings to avoid YAML parsing issues. */
  private static String quote(String str) {
    return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
  }

  private static String key(String key) {
    return PLAIN_KEY.matcher(key).matches() ? key : quote(key);
  }

  /**
   * Gets the indentation string for a given level.
   */
  private static String getIndent(int level) {
    return "  ".repeat(level);
  }
}
