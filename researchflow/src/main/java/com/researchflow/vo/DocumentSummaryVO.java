package com.researchflow.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentSummaryVO {
    private Long id;
    private Long documentId;
    private Long projectId;
    private Long creatorId;
    private String creatorName;
    private String documentName;
    private String title;
    private String content;
    private List<DocumentSummarySourceVO> sources;
    private String model;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
