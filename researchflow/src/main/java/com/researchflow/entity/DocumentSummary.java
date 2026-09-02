package com.researchflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_document_summary")
public class DocumentSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private Long projectId;
    private Long creatorId;
    private String title;
    private String content;
    private String sources;
    private String model;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
