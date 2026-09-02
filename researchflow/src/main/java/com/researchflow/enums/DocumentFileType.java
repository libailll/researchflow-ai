package com.researchflow.enums;

import java.util.Locale;
import java.util.Optional;

public enum DocumentFileType {
    PDF("pdf"),
    DOCX("docx"),
    TXT("txt"),
    MARKDOWN("md");

    private final String extension;

    DocumentFileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public static Optional<DocumentFileType> fromFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return Optional.empty();
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        if ("markdown".equals(extension)) {
            extension = "md";
        }
        for (DocumentFileType type : values()) {
            if (type.extension.equals(extension)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
