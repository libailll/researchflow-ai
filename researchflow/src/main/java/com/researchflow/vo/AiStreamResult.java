package com.researchflow.vo;

import java.util.List;

public record AiStreamResult(
        String content,
        String reasoning,
        List<SemanticSearchResultVO> sources,
        String model,
        boolean completed
) {
}
