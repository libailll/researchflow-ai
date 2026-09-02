package com.researchflow.service;

import com.researchflow.vo.DocumentVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {
    DocumentVO upload(Long projectId, MultipartFile file);
    List<DocumentVO> list(Long projectId);
    DocumentVO get(Long documentId);
    DocumentDownload download(Long documentId);
    void delete(Long documentId);

    record DocumentDownload(Resource resource, String originalName, String contentType, long fileSize) {
    }
}
