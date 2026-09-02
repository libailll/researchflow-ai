package com.researchflow.vo;

import java.util.List;

public record WeeklyReportAiResultVO(
        String content,
        String model,
        List<SemanticSearchResultVO> sources
) {
}
