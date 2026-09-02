package com.researchflow.vo;

import java.util.List;

public record DocumentSummaryAiResultVO(
        String content,
        String model,
        List<DocumentSummarySourceVO> sources
) {
}
