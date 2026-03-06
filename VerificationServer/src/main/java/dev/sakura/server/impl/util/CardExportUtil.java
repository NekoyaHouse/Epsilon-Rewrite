package dev.sakura.server.impl.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class CardExportUtil {
    private CardExportUtil() {
    }

    public static Path writeKeys(String group, long days, List<String> keys) throws Exception {
        Path dir = Paths.get("卡密");
        Files.createDirectories(dir);
        String safeGroup = sanitizeFilePart(group == null ? "" : group.trim());
        String safeDays = sanitizeFilePart(Long.toString(Math.max(0L, days)) + "天");
        String base = (safeGroup.isEmpty() ? "default" : safeGroup) + "-" + safeDays + ".txt";
        Path file = createUniqueFile(dir.resolve(base));
        Files.write(file, keys == null ? List.of() : keys, StandardCharsets.UTF_8);
        return file.toAbsolutePath();
    }

    private static Path createUniqueFile(Path target) throws Exception {
        String name = target.getFileName().toString();
        String stem = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            stem = name.substring(0, dot);
            ext = name.substring(dot);
        }

        for (int i = 0; i < 200; i++) {
            Path candidate = i == 0 ? target : target.getParent().resolve(stem + "_" + i + ext);
            try {
                return Files.createFile(candidate);
            } catch (FileAlreadyExistsException ignored) {
            }
        }
        throw new IllegalStateException("Failed to create output file");
    }

    private static String sanitizeFilePart(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '/' || c == ':' || c == '*' || c == '?' || c == '"' || c == '<' || c == '>' || c == '|' || c == '\n' || c == '\r' || c == '\t') {
                out.append('_');
            } else {
                out.append(c);
            }
        }
        String v = out.toString().trim();
        return v.length() > 80 ? v.substring(0, 80) : v;
    }
}

