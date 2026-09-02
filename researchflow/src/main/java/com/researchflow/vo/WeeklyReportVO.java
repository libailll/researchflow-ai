package com.researchflow.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WeeklyReportVO {
    private Long id;
    private Long projectId;
    private Long creatorId;
    private String creatorName;
    private String title;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String content;
    private List<SemanticSearchResultVO> sources;
    private String model;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
