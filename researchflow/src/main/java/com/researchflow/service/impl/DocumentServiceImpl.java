package com.researchflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.config.FileStorageProperties;
import com.researchflow.context.UserContext;
import com.researchflow.entity.Document;
import com.researchflow.entity.Project;
import com.researchflow.entity.User;
import com.researchflow.enums.DocumentFileType;
import com.researchflow.enums.DocumentStatus;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.DocumentMapper;
import com.researchflow.mapper.UserMapper;
import com.researchflow.message.DocumentMessagePublisher;
import com.researchflow.message.DocumentParseMessage;
import com.researchflow.message.DocumentVectorMessage;
import com.researchflow.service.DocumentService;
import com.researchflow.service.DocumentProcessingService;
import com.researchflow.service.ProjectPermissionService;
import com.researchflow.storage.LocalFileStorageService;
import com.researchflow.storage.StoredFile;
import com.researchflow.vo.DocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final UserMapper userMapper;
    private final ProjectPermissionService projectPermissionService;
    private final LocalFileStorageService fileStorageService;
    private final FileStorageProperties storageProperties;
    private final DocumentMessagePublisher messagePublisher;
    private final DocumentProcessingService processingService;

    @Override
    @Transactional
    public DocumentVO upload(Long projectId, MultipartFile file) {
        projectPermissionService.requireAccess(projectId);
        DocumentFileType fileType = validateFile(file);
        StoredFile storedFile = fileStorageService.store(projectId, file, fileType);

        Document document = new Document();
        document.setProjectId(projectId);
        document.setUploaderId(UserContext.getUserId());
        document.setFileName(storedFile.fileName());
        document.setOriginalName(normalizeOriginalName(file.getOriginalFilename(), fileType));
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setStoragePath(storedFile.storagePath());
        document.setParseStatus(DocumentStatus.WAITING);
        document.setVectorStatus(DocumentStatus.WAITING);
        LocalDateTime now = LocalDateTime.now();
        document.setCreatedAt(now);
        document.setUpdatedAt(now);

        try {
            documentMapper.insert(document);
        } catch (RuntimeException e) {
            fileStorageService.deleteIfExists(storedFile.storagePath());
            throw e;
        }

        try {
            messagePublisher.publishParse(new DocumentParseMessage(
                    document.getId(),
                    projectId,
                    fileStorageService.absolutePath(storedFile.storagePath()).toString()
            ));
        } catch (RuntimeException e) {
            fileStorageService.deleteIfExists(storedFile.storagePath());
            log.error("Failed to publish document parse task: documentId={}, projectId={}",
                    document.getId(), projectId, e);
            throw new BusinessException(ErrorCode.DOCUMENT_MESSAGE_PUBLISH_FAILED);
        }

        User uploader = userMapper.selectById(document.getUploaderId());
        log.info("Document uploaded: documentId={}, projectId={}, uploaderId={}, type={}, size={}",
                document.getId(), projectId, document.getUploaderId(), fileType, file.getSize());
        return toVO(document, uploader);
    }

    @Override
    public List<DocumentVO> list(Long projectId) {
        projectPermissionService.requireAccess(projectId);
        List<Document> documents = documentMapper.selectList(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getProjectId, projectId)
                        .orderByDesc(Document::getCreatedAt)
        );
        if (documents.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, User> uploaders = userMapper.selectByIds(
                        documents.stream().map(Document::getUploaderId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return documents.stream()
                .map(document -> toVO(document, uploaders.get(document.getUploaderId())))
                .toList();
    }

    @Override
    public DocumentVO get(Long documentId) {
        Document document = requireDocument(documentId);
        projectPermissionService.requireAccess(document.getProjectId());
        return toVO(document, userMapper.selectById(document.getUploaderId()));
    }

    @Override
    public DocumentDownload download(Long documentId) {
        Document document = requireDocument(documentId);
        projectPermissionService.requireAccess(document.getProjectId());
        Resource resource = fileStorageService.load(document.getStoragePath());
        return new DocumentDownload(
                resource,
                document.getOriginalName(),
                contentType(document.getFileType()),
                document.getFileSize()
        );
    }

    @Override
    @Transactional
    public void delete(Long documentId) {
        Document document = requireDocument(documentId);
        Project project = projectPermissionService.requireAccess(document.getProjectId());
        Long operatorId = UserContext.getUserId();
        if (!operatorId.equals(document.getUploaderId())
                && !projectPermissionService.canManage(project, operatorId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }
        documentMapper.deleteById(documentId);
        processingService.deleteChunks(documentId);
        fileStorageService.deleteIfExists(document.getStoragePath());
        try {
            messagePublisher.publishVectorDelete(new DocumentVectorMessage(documentId, document.getProjectId()));
        } catch (RuntimeException e) {
            log.warn("Failed to publish vector deletion task: documentId={}", documentId, e);
        }
        log.info("Document deleted: documentId={}, projectId={}, operatorId={}",
                documentId, document.getProjectId(), operatorId);
    }

    private Document requireDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        return document;
    }

    private DocumentFileType validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_EMPTY_FILE);
        }
        if (file.getSize() > storageProperties.maxFileSize()) {
            throw new BusinessException(ErrorCode.DOCUMENT_FILE_TOO_LARGE);
        }
        DocumentFileType type = DocumentFileType.fromFileName(file.getOriginalFilename())
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_TYPE_NOT_SUPPORTED));
        validateSignature(file, type);
        return type;
    }

    private void validateSignature(MultipartFile file, DocumentFileType type) {
        if (type != DocumentFileType.PDF && type != DocumentFileType.DOCX) {
            return;
        }
        try (InputStream input = file.getInputStream()) {
            byte[] signature = input.readNBytes(4);
            boolean valid = type == DocumentFileType.PDF
                    ? signature.length == 4 && signature[0] == '%' && signature[1] == 'P'
                    && signature[2] == 'D' && signature[3] == 'F'
                    : signature.length >= 2 && signature[0] == 'P' && signature[1] == 'K';
            if (!valid) {
                throw new BusinessException(ErrorCode.DOCUMENT_TYPE_NOT_SUPPORTED);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_STORAGE_FAILED);
        }
    }

    private String normalizeOriginalName(String originalName, DocumentFileType type) {
        if (originalName == null || originalName.isBlank()) {
            return "document." + type.getExtension();
        }
        String normalized = originalName.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private String contentType(DocumentFileType type) {
        return switch (type) {
            case PDF -> "application/pdf";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case TXT -> "text/plain";
            case MARKDOWN -> "text/markdown";
        };
    }

    private DocumentVO toVO(Document document, User uploader) {
        DocumentVO vo = new DocumentVO();
        vo.setId(document.getId());
        vo.setProjectId(document.getProjectId());
        vo.setUploaderId(document.getUploaderId());
        vo.setUploaderName(uploader == null ? null : uploader.getNickname());
        vo.setOriginalName(document.getOriginalName());
        vo.setFileType(document.getFileType());
        vo.setFileSize(document.getFileSize());
        vo.setParseStatus(document.getParseStatus());
        vo.setVectorStatus(document.getVectorStatus());
        vo.setParseError(document.getParseError());
        vo.setParsedAt(document.getParsedAt());
        vo.setVectorError(document.getVectorError());
        vo.setVectorizedAt(document.getVectorizedAt());
        vo.setCreatedAt(document.getCreatedAt());
        vo.setUpdatedAt(document.getUpdatedAt());
        return vo;
    }
}
