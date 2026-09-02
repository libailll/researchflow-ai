package com.researchflow.vo;

import com.researchflow.enums.ProjectRiskLevel;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ProjectRiskReportVO {
    private Long id;
    private Long projectId;
    private Long creatorId;
    private String creatorName;
    private String title;
    private ProjectRiskLevel riskLevel;
    private Integer riskScore;
    private String content;
    private Map<String, Object> analysisSnapshot;
    private List<SemanticSearchResultVO> sources;
    private String model;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
