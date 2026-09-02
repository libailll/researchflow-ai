package com.researchflow.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DocumentChunkPageVO {
    private long total;
    private long page;
    private long size;
    private List<DocumentChunkVO> records;
}
