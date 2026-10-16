package me.mortaldev.abstractyaml;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import me.mortaldev.abstractyaml.config2.ConfigSource;
import me.mortaldev.abstractyaml.config2.YamlConfigWriter;

/**
 * Utility class for writing YAML configuration files with comments.
 * Supports both inline comments and header comments for sections.
 *
 * <p>Rendering is shared with config2 through {@link YamlConfigWriter}.
 *
 * @deprecated Part of the legacy config system. Use {@code me.mortaldev.abstractyaml.config2}, where
 *     {@code Config.toYaml()} and {@code Config.save(File)} write files.
 */
@Deprecated
public class YamlWriter {

    /**
     * Represents a YAML entry with its value and optional comment.
     */
    public static class YamlEntry {
        private final String path;
        private final Object value;
        private final String comment;
        private final String headerComment;

        public YamlEntry(String path, Object value, String comment, String headerComment) {
            this.path = path;
            this.value = value;
            this.comment = comment;
            this.headerComment = headerComment;
        }

        public String getPath() {
            return path;
        }

        public Object getValue() {
            return value;
        }

        public String getComment() {
            return comment;
        }

        public String getHeaderComment() {
            return headerComment;
        }
    }

    /**
     * Writes YAML entries to a file with proper formatting and comments.
     *
     * @param file the file to write to
     * @param entries the entries to write
     * @throws IOException if writing fails
     */
    public static void writeYaml(File file, List<YamlEntry> entries) throws IOException {
        writeYaml(file, entries, null);
    }

    /**
     * Writes YAML entries to a file with proper formatting, comments, and an optional file header.
     *
     * <p>An entry's header comment is written above its top-level section. A header on an entry
     * without a section is added to the file header.
     *
     * @param file the file to write to
     * @param entries the entries to write
     * @param fileHeader optional file header (multi-line string, each line will be prefixed with #)
     * @throws IOException if writing fails
     */
    public static void writeYaml(File file, List<YamlEntry> entries, String fileHeader) throws IOException {
        Map<String, String> sectionHeaders = new HashMap<>();
        List<YamlConfigWriter.Entry> rendered = new ArrayList<>();
        for (YamlEntry entry : entries) {
            rendered.add(new YamlConfigWriter.Entry(entry.getPath(), entry.getValue(), entry.getComment()));
            String header = entry.getHeaderComment();
            if (header == null) {
                continue;
            }
            int dot = entry.getPath().indexOf('.');
            if (dot >= 0) {
                sectionHeaders.putIfAbsent(entry.getPath().substring(0, dot), header);
            } else {
                fileHeader = fileHeader == null || fileHeader.isEmpty() ? header : fileHeader + "\n\n" + header;
            }
        }

        try {
            ConfigSource.file(file).write(YamlConfigWriter.render(fileHeader, sectionHeaders::get, rendered));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
