package com.researchflow.vo;

public record SemanticSearchResultVO(
        Long documentId,
        String documentName,
        Integer pageNumber,
        Integer chunkIndex,
        Double score,
        String content
) {
}
