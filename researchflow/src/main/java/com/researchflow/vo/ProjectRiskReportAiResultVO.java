package com.researchflow.vo;

import java.util.List;

public record ProjectRiskReportAiResultVO(
        String content,
        String model,
        List<SemanticSearchResultVO> sources
) {
}
