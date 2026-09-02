package com.researchflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.researchflow.common.ErrorCode;
import com.researchflow.context.UserContext;
import com.researchflow.dto.AiConversationCreateDTO;
import com.researchflow.dto.AiConversationUpdateDTO;
import com.researchflow.entity.AiConversation;
import com.researchflow.entity.AiMessage;
import com.researchflow.enums.AiMessageRole;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.AiConversationMapper;
import com.researchflow.mapper.AiMessageMapper;
import com.researchflow.vo.AiConversationDetailVO;
import com.researchflow.vo.AiConversationVO;
import com.researchflow.vo.AiPersistedMessageVO;
import com.researchflow.vo.AiStreamResult;
import com.researchflow.vo.SemanticSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationService {

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final ProjectPermissionService projectPermissionService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiConversationVO create(Long projectId, AiConversationCreateDTO request) {
        projectPermissionService.requireAccess(projectId);
        LocalDateTime now = LocalDateTime.now();
        AiConversation conversation = new AiConversation();
        conversation.setProjectId(projectId);
        conversation.setUserId(UserContext.getUserId());
        conversation.setTitle(normalizeTitle(request.title()));
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.insert(conversation);
        log.info("AI conversation created: conversationId={}, projectId={}, userId={}",
                conversation.getId(), projectId, conversation.getUserId());
        return toConversationVO(conversation, null);
    }

    public List<AiConversationVO> list(Long projectId) {
        projectPermissionService.requireAccess(projectId);
        Long userId = UserContext.getUserId();
        return conversationMapper.selectList(new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getProjectId, projectId)
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getUpdatedAt)
                        .last("LIMIT 50"))
                .stream()
                .map(conversation -> toConversationVO(conversation, findLastMessage(conversation.getId())))
                .toList();
    }

    public AiConversationDetailVO detail(Long conversationId) {
        AiConversation conversation = requireOwned(conversationId, UserContext.getUserId(), null);
        projectPermissionService.requireAccess(conversation.getProjectId());
        List<AiPersistedMessageVO> messages = messageMapper.selectList(
                        new LambdaQueryWrapper<AiMessage>()
                                .eq(AiMessage::getConversationId, conversationId)
                                .orderByAsc(AiMessage::getCreatedAt)
                                .orderByAsc(AiMessage::getId))
                .stream().map(this::toMessageVO).toList();
        String lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1).getContent();
        return new AiConversationDetailVO(toConversationVO(conversation, lastMessage), messages);
    }

    @Transactional
    public AiConversationVO rename(Long conversationId, AiConversationUpdateDTO request) {
        AiConversation conversation = requireOwned(conversationId, UserContext.getUserId(), null);
        projectPermissionService.requireAccess(conversation.getProjectId());
        conversation.setTitle(request.title().trim());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return toConversationVO(conversation, findLastMessage(conversationId));
    }

    @Transactional
    public void delete(Long conversationId) {
        AiConversation conversation = requireOwned(conversationId, UserContext.getUserId(), null);
        projectPermissionService.requireAccess(conversation.getProjectId());
        messageMapper.delete(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
        log.info("AI conversation deleted: conversationId={}, userId={}", conversationId, UserContext.getUserId());
    }

    @Transactional
    public void clearMessages(Long conversationId) {
        AiConversation conversation = requireOwned(conversationId, UserContext.getUserId(), null);
        projectPermissionService.requireAccess(conversation.getProjectId());
        messageMapper.delete(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId));
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        log.info("AI conversation messages cleared: conversationId={}, userId={}",
                conversationId, UserContext.getUserId());
    }

    public void requireConversation(Long conversationId, Long projectId, Long userId) {
        requireOwned(conversationId, userId, projectId);
    }

    @Transactional
    public void saveExchange(
            Long conversationId,
            Long projectId,
            Long userId,
            String question,
            AiStreamResult result
    ) {
        if (conversationId == null || !result.completed() || result.content() == null || result.content().isBlank()) {
            return;
        }
        AiConversation conversation = requireOwned(conversationId, userId, projectId);
        LocalDateTime now = LocalDateTime.now();
        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setRole(AiMessageRole.USER);
        userMessage.setContent(question);
        userMessage.setCreatedAt(now);
        messageMapper.insert(userMessage);

        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole(AiMessageRole.ASSISTANT);
        assistantMessage.setContent(result.content());
        assistantMessage.setReasoning(result.reasoning());
        assistantMessage.setSources(writeSources(result.sources()));
        assistantMessage.setModel(result.model());
        assistantMessage.setCreatedAt(now.plusNanos(1));
        messageMapper.insert(assistantMessage);

        conversation.setUpdatedAt(now);
        conversationMapper.updateById(conversation);
        log.info("AI exchange persisted: conversationId={}, projectId={}, userId={}",
                conversationId, projectId, userId);
    }

    private AiConversation requireOwned(Long conversationId, Long userId, Long projectId) {
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.AI_CONVERSATION_NOT_FOUND);
        }
        if (!conversation.getUserId().equals(userId)
                || (projectId != null && !conversation.getProjectId().equals(projectId))) {
            throw new BusinessException(ErrorCode.AI_CONVERSATION_ACCESS_DENIED);
        }
        return conversation;
    }

    private String findLastMessage(Long conversationId) {
        AiMessage message = messageMapper.selectOne(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .orderByDesc(AiMessage::getCreatedAt)
                .orderByDesc(AiMessage::getId)
                .last("LIMIT 1"));
        return message == null ? null : message.getContent();
    }

    private String normalizeTitle(String title) {
        return title == null || title.isBlank() ? "新对话" : title.trim();
    }

    private String writeSources(List<SemanticSearchResultVO> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize AI sources", e);
            return null;
        }
    }

    private List<SemanticSearchResultVO> readSources(String sources) {
        if (sources == null || sources.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(sources, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            log.warn("Could not deserialize persisted AI sources", e);
            return List.of();
        }
    }

    private AiConversationVO toConversationVO(AiConversation conversation, String lastMessage) {
        AiConversationVO vo = new AiConversationVO();
        vo.setId(conversation.getId());
        vo.setProjectId(conversation.getProjectId());
        vo.setTitle(conversation.getTitle());
        vo.setLastMessage(preview(lastMessage));
        vo.setCreatedAt(conversation.getCreatedAt());
        vo.setUpdatedAt(conversation.getUpdatedAt());
        return vo;
    }

    private String preview(String content) {
        if (content == null) {
            return null;
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        return compact.length() <= 160 ? compact : compact.substring(0, 160) + "…";
    }

    private AiPersistedMessageVO toMessageVO(AiMessage message) {
        AiPersistedMessageVO vo = new AiPersistedMessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setReasoning(message.getReasoning());
        vo.setSources(readSources(message.getSources()));
        vo.setModel(message.getModel());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }
}
