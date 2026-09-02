package com.researchflow.vo;

public record DocumentSummarySourceVO(
        Integer pageNumber,
        Integer chunkIndex,
        String excerpt
) {
}
