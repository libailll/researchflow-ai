package com.researchflow.message;

public record DocumentParseMessage(
        Long documentId,
        Long projectId,
        String filePath
) {
}
