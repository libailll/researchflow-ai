package com.researchflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ai_weekly_report")
public class WeeklyReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long creatorId;
    private String title;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String content;
    private String sources;
    private String model;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
