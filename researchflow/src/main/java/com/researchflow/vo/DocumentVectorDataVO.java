package com.researchflow.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DocumentVectorDataVO {
    private Long documentId;
    private Long projectId;
    private String documentName;
    private List<DocumentChunkVO> chunks;
}
