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

    /** Not needed: every method is static. Kept public so existing code still compiles. */
    public YamlWriter() {}

    /**
     * One value to write, with its comment and an optional section header.
     */
    public static class YamlEntry {
        private final String path;
        private final Object value;
        private final String comment;
        private final String headerComment;

        /**
         * Creates an entry.
         *
         * @param path the full dotted path, such as {@code "game.max-players"}
         * @param value plain YAML data: String, Number, Boolean, List or Map
         * @param comment the comment written after the value, or {@code null}
         * @param headerComment the comment written above the entry's top-level section, or
         *     {@code null}; see {@link YamlWriter#writeYaml(File, List, String)}
         */
        public YamlEntry(String path, Object value, String comment, String headerComment) {
            this.path = path;
            this.value = value;
            this.comment = comment;
            this.headerComment = headerComment;
        }

        /**
         * Returns the full dotted path.
         *
         * @return the path
         */
        public String getPath() {
            return path;
        }

        /**
         * Returns the value to write.
         *
         * @return the value
         */
        public Object getValue() {
            return value;
        }

        /**
         * Returns the comment written after the value.
         *
         * @return the comment, or {@code null}
         */
        public String getComment() {
            return comment;
        }

        /**
         * Returns the comment written above the entry's top-level section.
         *
         * @return the header, or {@code null}
         */
        public String getHeaderComment() {
            return headerComment;
        }
    }

    /**
     * Writes entries to a file, replacing it and creating missing folders. Same as
     * {@code writeYaml(file, entries, null)}.
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
     * <p>An entry's header comment is written above its top-level section; when several
     * entries in one section have headers, the first is used. A header on an entry without a
     * section is added to the file header. The file is replaced, and missing folders are
     * created. The format is described in {@link YamlConfigWriter}.
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
