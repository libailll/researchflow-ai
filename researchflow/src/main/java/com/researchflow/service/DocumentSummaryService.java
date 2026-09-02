package com.researchflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.context.UserContext;
import com.researchflow.dto.DocumentSummaryGenerateDTO;
import com.researchflow.dto.DocumentSummaryUpdateDTO;
import com.researchflow.entity.Document;
import com.researchflow.entity.DocumentChunk;
import com.researchflow.entity.DocumentSummary;
import com.researchflow.entity.Project;
import com.researchflow.entity.User;
import com.researchflow.enums.DocumentStatus;
import com.researchflow.enums.NotificationType;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.DocumentChunkMapper;
import com.researchflow.mapper.DocumentMapper;
import com.researchflow.mapper.DocumentSummaryMapper;
import com.researchflow.mapper.UserMapper;
import com.researchflow.vo.DocumentSummaryAiResultVO;
import com.researchflow.vo.DocumentSummarySourceVO;
import com.researchflow.vo.DocumentSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSummaryService {

    private final DocumentSummaryMapper summaryMapper;
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;
    private final UserMapper userMapper;
    private final ProjectPermissionService projectPermissionService;
    private final AiServiceClient aiServiceClient;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public DocumentSummaryVO generate(Long documentId, DocumentSummaryGenerateDTO request) {
        Document document = requireReadyDocument(documentId);
        projectPermissionService.requireAccess(document.getProjectId());
        List<DocumentChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId)
                .orderByAsc(DocumentChunk::getChunkIndex));
        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_PARSE_RESULT_INVALID);
        }

        List<Map<String, Object>> chunkData = chunks.stream().map(chunk -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("pageNumber", chunk.getPageNumber());
            item.put("chunkIndex", chunk.getChunkIndex());
            item.put("content", chunk.getContent());
            return item;
        }).toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentId", documentId);
        payload.put("projectId", document.getProjectId());
        payload.put("userId", UserContext.getUserId());
        payload.put("documentName", document.getOriginalName());
        payload.put("fileType", document.getFileType());
        payload.put("chunks", chunkData);

        DocumentSummaryAiResultVO generated = aiServiceClient.generateDocumentSummary(payload);
        if (generated.content() == null || generated.content().isBlank()) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
        Long userId = UserContext.getUserId();
        LocalDateTime now = LocalDateTime.now();
        DocumentSummary summary = new DocumentSummary();
        summary.setDocumentId(documentId);
        summary.setProjectId(document.getProjectId());
        summary.setCreatorId(userId);
        summary.setTitle(normalizeTitle(request.title(), document.getOriginalName()));
        summary.setContent(generated.content().trim());
        summary.setSources(writeSources(generated.sources()));
        summary.setModel(generated.model());
        summary.setCreatedAt(now);
        summary.setUpdatedAt(now);
        summaryMapper.insert(summary);
        notificationService.aiResultReady(userId, document.getProjectId(), NotificationType.DOCUMENT_SUMMARY_READY,
                "文档总结已生成", "“" + document.getOriginalName() + "”的 AI 总结已经生成并保存。",
                "DOCUMENT_SUMMARY", summary.getId(), "/documents?documentId=" + documentId);
        log.info("AI document summary generated: summaryId={}, documentId={}, projectId={}, userId={}",
                summary.getId(), documentId, document.getProjectId(), userId);
        return toVO(summary, document, displayName(userMapper.selectById(userId)));
    }

    public List<DocumentSummaryVO> list(Long documentId) {
        Document document = requireDocument(documentId);
        projectPermissionService.requireAccess(document.getProjectId());
        List<DocumentSummary> summaries = summaryMapper.selectList(
                new LambdaQueryWrapper<DocumentSummary>()
                        .eq(DocumentSummary::getDocumentId, documentId)
                        .orderByDesc(DocumentSummary::getCreatedAt)
        );
        List<Long> creatorIds = summaries.stream().map(DocumentSummary::getCreatorId).distinct().toList();
        Map<Long, User> creators = creatorIds.isEmpty() ? Map.of() : userMapper.selectByIds(creatorIds)
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return summaries.stream()
                .map(summary -> toVO(summary, document, displayName(creators.get(summary.getCreatorId()))))
                .toList();
    }

    public DocumentSummaryVO detail(Long summaryId) {
        SummaryContext context = requireAccessible(summaryId);
        return toVO(context.summary(), context.document(),
                displayName(userMapper.selectById(context.summary().getCreatorId())));
    }

    @Transactional
    public DocumentSummaryVO update(Long summaryId, DocumentSummaryUpdateDTO request) {
        SummaryContext context = requireEditable(summaryId);
        DocumentSummary summary = context.summary();
        summary.setTitle(request.title().trim());
        summary.setContent(request.content().trim());
        summary.setUpdatedAt(LocalDateTime.now());
        summaryMapper.updateById(summary);
        log.info("AI document summary updated: summaryId={}, userId={}", summaryId, UserContext.getUserId());
        return toVO(summary, context.document(), displayName(userMapper.selectById(summary.getCreatorId())));
    }

    @Transactional
    public void delete(Long summaryId) {
        SummaryContext context = requireEditable(summaryId);
        summaryMapper.deleteById(summaryId);
        log.info("AI document summary deleted: summaryId={}, documentId={}, userId={}",
                summaryId, context.document().getId(), UserContext.getUserId());
    }

    private SummaryContext requireAccessible(Long summaryId) {
        DocumentSummary summary = summaryMapper.selectById(summaryId);
        if (summary == null) throw new BusinessException(ErrorCode.DOCUMENT_SUMMARY_NOT_FOUND);
        Document document = requireDocument(summary.getDocumentId());
        projectPermissionService.requireAccess(document.getProjectId());
        return new SummaryContext(summary, document);
    }

    private SummaryContext requireEditable(Long summaryId) {
        SummaryContext context = requireAccessible(summaryId);
        Long userId = UserContext.getUserId();
        Project project = projectPermissionService.getProjectOrThrow(context.document().getProjectId());
        if (!context.summary().getCreatorId().equals(userId) && !projectPermissionService.canManage(project, userId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_SUMMARY_ACCESS_DENIED);
        }
        return context;
    }

    private Document requireReadyDocument(Long documentId) {
        Document document = requireDocument(documentId);
        if (document.getParseStatus() != DocumentStatus.SUCCESS
                || document.getVectorStatus() != DocumentStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.DOCUMENT_SUMMARY_NOT_READY);
        }
        return document;
    }

    private Document requireDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        return document;
    }

    private String normalizeTitle(String title, String documentName) {
        return title == null || title.isBlank() ? documentName + " · AI 总结" : title.trim();
    }

    private String displayName(User user) {
        if (user == null) return null;
        return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
    }

    private String writeSources(List<DocumentSummarySourceVO> sources) {
        if (sources == null || sources.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize document summary sources", e);
            return null;
        }
    }

    private List<DocumentSummarySourceVO> readSources(String sources) {
        if (sources == null || sources.isBlank()) return List.of();
        try {
            return objectMapper.readValue(sources, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            log.warn("Could not deserialize document summary sources", e);
            return List.of();
        }
    }

    private DocumentSummaryVO toVO(DocumentSummary summary, Document document, String creatorName) {
        DocumentSummaryVO vo = new DocumentSummaryVO();
        vo.setId(summary.getId());
        vo.setDocumentId(summary.getDocumentId());
        vo.setProjectId(summary.getProjectId());
        vo.setCreatorId(summary.getCreatorId());
        vo.setCreatorName(creatorName);
        vo.setDocumentName(document.getOriginalName());
        vo.setTitle(summary.getTitle());
        vo.setContent(summary.getContent());
        vo.setSources(readSources(summary.getSources()));
        vo.setModel(summary.getModel());
        vo.setCreatedAt(summary.getCreatedAt());
        vo.setUpdatedAt(summary.getUpdatedAt());
        return vo;
    }

    private record SummaryContext(DocumentSummary summary, Document document) {
    }
}
