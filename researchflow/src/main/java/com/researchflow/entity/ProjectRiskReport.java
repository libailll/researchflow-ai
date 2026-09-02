package com.researchflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.researchflow.enums.ProjectRiskLevel;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_project_risk_report")
public class ProjectRiskReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long creatorId;
    private String title;
    private ProjectRiskLevel riskLevel;
    private Integer riskScore;
    private String content;
    private String analysisSnapshot;
    private String sources;
    private String model;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
