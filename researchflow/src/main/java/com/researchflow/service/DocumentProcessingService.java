package com.researchflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.dto.DocumentChunkDTO;
import com.researchflow.entity.Document;
import com.researchflow.entity.DocumentChunk;
import com.researchflow.enums.DocumentStatus;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.DocumentChunkMapper;
import com.researchflow.mapper.DocumentMapper;
import com.researchflow.message.DocumentMessagePublisher;
import com.researchflow.message.DocumentVectorMessage;
import com.researchflow.vo.DocumentChunkPageVO;
import com.researchflow.vo.DocumentChunkVO;
import com.researchflow.vo.DocumentVectorDataVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final ProjectPermissionService projectPermissionService;
    private final DocumentMessagePublisher messagePublisher;

    @Transactional
    public void markProcessing(Long documentId) {
        Document document = requireDocument(documentId);
        document.setParseStatus(DocumentStatus.PROCESSING);
        document.setParseError(null);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        log.info("Document parsing started: documentId={}, projectId={}", documentId, document.getProjectId());
    }

    @Transactional
    public void saveParsed(Long documentId, List<DocumentChunkDTO> chunks) {
        Document document = requireDocument(documentId);
        if (chunks == null || chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_PARSE_RESULT_INVALID);
        }
        documentChunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId));
        LocalDateTime now = LocalDateTime.now();
        for (DocumentChunkDTO chunkDTO : chunks) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(documentId);
            chunk.setProjectId(document.getProjectId());
            chunk.setPageNumber(chunkDTO.pageNumber());
            chunk.setChunkIndex(chunkDTO.chunkIndex());
            chunk.setContent(chunkDTO.content());
            chunk.setCharCount(chunkDTO.content().length());
            chunk.setCreatedAt(now);
            documentChunkMapper.insert(chunk);
        }
        document.setParseStatus(DocumentStatus.SUCCESS);
        document.setParseError(null);
        document.setParsedAt(now);
        document.setVectorStatus(DocumentStatus.WAITING);
        document.setVectorError(null);
        document.setVectorizedAt(null);
        document.setUpdatedAt(now);
        documentMapper.updateById(document);
        try {
            messagePublisher.publishVectorize(new DocumentVectorMessage(documentId, document.getProjectId()));
        } catch (RuntimeException e) {
            document.setVectorStatus(DocumentStatus.FAILED);
            document.setVectorError(ErrorCode.DOCUMENT_VECTOR_PUBLISH_FAILED.getMessage());
            documentMapper.updateById(document);
            log.error("Failed to publish vectorization task: documentId={}", documentId, e);
        }
        log.info("Document parsing completed: documentId={}, projectId={}, chunks={}",
                documentId, document.getProjectId(), chunks.size());
    }

    @Transactional
    public void retryVectorize(Long documentId) {
        Document document = requireDocument(documentId);
        projectPermissionService.requireAccess(document.getProjectId());
        if (document.getParseStatus() != DocumentStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_PARSED);
        }
        document.setVectorStatus(DocumentStatus.WAITING);
        document.setVectorError(null);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        try {
            messagePublisher.publishVectorize(new DocumentVectorMessage(documentId, document.getProjectId()));
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_VECTOR_PUBLISH_FAILED);
        }
        log.info("Document vectorization queued: documentId={}, projectId={}", documentId, document.getProjectId());
    }

    @Transactional
    public void markVectorProcessing(Long documentId) {
        Document document = requireDocument(documentId);
        document.setVectorStatus(DocumentStatus.PROCESSING);
        document.setVectorError(null);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
    }

    @Transactional
    public void markVectorSuccess(Long documentId) {
        Document document = requireDocument(documentId);
        LocalDateTime now = LocalDateTime.now();
        document.setVectorStatus(DocumentStatus.SUCCESS);
        document.setVectorError(null);
        document.setVectorizedAt(now);
        document.setUpdatedAt(now);
        documentMapper.updateById(document);
        log.info("Document vectorization completed: documentId={}, projectId={}", documentId, document.getProjectId());
    }

    @Transactional
    public void markVectorFailed(Long documentId, String error) {
        Document document = requireDocument(documentId);
        document.setVectorStatus(DocumentStatus.FAILED);
        document.setVectorError(error);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        log.warn("Document vectorization failed: documentId={}, reason={}", documentId, error);
    }

    public DocumentVectorDataVO getVectorData(Long documentId) {
        Document document = requireDocument(documentId);
        if (document.getParseStatus() != DocumentStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_PARSED);
        }
        List<DocumentChunkVO> chunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)
                        .orderByAsc(DocumentChunk::getChunkIndex)
        ).stream().map(this::toVO).toList();
        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_PARSE_RESULT_INVALID);
        }
        return new DocumentVectorDataVO(documentId, document.getProjectId(), document.getOriginalName(), chunks);
    }

    @Transactional
    public void markFailed(Long documentId, String error) {
        Document document = requireDocument(documentId);
        document.setParseStatus(DocumentStatus.FAILED);
        document.setParseError(error);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        log.warn("Document parsing failed: documentId={}, projectId={}, reason={}",
                documentId, document.getProjectId(), error);
    }

    public DocumentChunkPageVO listChunks(Long documentId, long page, long size) {
        Document document = requireDocument(documentId);
        projectPermissionService.requireAccess(document.getProjectId());
        long total = documentChunkMapper.countByDocumentId(documentId);
        long offset = (page - 1) * size;
        List<DocumentChunkVO> records = documentChunkMapper
                .selectPageByDocumentId(documentId, offset, size)
                .stream().map(this::toVO).toList();
        return new DocumentChunkPageVO(total, page, size, records);
    }

    @Transactional
    public void deleteChunks(Long documentId) {
        documentChunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId));
    }

    private Document requireDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        return document;
    }

    private DocumentChunkVO toVO(DocumentChunk chunk) {
        DocumentChunkVO vo = new DocumentChunkVO();
        vo.setId(chunk.getId());
        vo.setPageNumber(chunk.getPageNumber());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setContent(chunk.getContent());
        vo.setCharCount(chunk.getCharCount());
        return vo;
    }
}
