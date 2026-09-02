package com.researchflow.vo;

import lombok.Data;

@Data
public class DocumentChunkVO {
    private Long id;
    private Integer pageNumber;
    private Integer chunkIndex;
    private String content;
    private Integer charCount;
}
