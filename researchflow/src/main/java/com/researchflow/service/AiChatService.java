package com.researchflow.service;

import com.researchflow.context.UserContext;
import com.researchflow.dto.AiChatDTO;
import com.researchflow.dto.SemanticSearchDTO;
import com.researchflow.vo.AiChatVO;
import com.researchflow.vo.AiStreamResult;
import com.researchflow.vo.SemanticSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ProjectPermissionService projectPermissionService;
    private final AiServiceClient aiServiceClient;
    private final AiConversationService conversationService;

    public AiChatVO chat(Long projectId, AiChatDTO request) {
        projectPermissionService.requireAccess(projectId);
        Long userId = UserContext.getUserId();
        validateConversation(request.conversationId(), projectId, userId);
        log.info("AI chat requested: projectId={}, userId={}", projectId, userId);
        AiChatVO response = aiServiceClient.chat(projectId, userId, request);
        conversationService.saveExchange(
                request.conversationId(), projectId, userId, request.message(),
                new AiStreamResult(response.answer(), null, List.of(), response.model(), true)
        );
        return response;
    }

    public StreamRequest stream(Long projectId, AiChatDTO request) {
        projectPermissionService.requireAccess(projectId);
        Long userId = UserContext.getUserId();
        validateConversation(request.conversationId(), projectId, userId);
        log.info("AI stream requested: projectId={}, userId={}, conversationId={}",
                projectId, userId, request.conversationId());
        return outputStream -> {
            AiStreamResult result = aiServiceClient.stream(projectId, userId, request, outputStream);
            conversationService.saveExchange(
                    request.conversationId(), projectId, userId, request.message(), result
            );
        };
    }

    public List<SemanticSearchResultVO> search(Long projectId, SemanticSearchDTO request) {
        projectPermissionService.requireAccess(projectId);
        log.info("Semantic search requested: projectId={}, userId={}, topK={}",
                projectId, UserContext.getUserId(), request.safeTopK());
        return aiServiceClient.search(projectId, request);
    }

    private void validateConversation(Long conversationId, Long projectId, Long userId) {
        if (conversationId != null) {
            conversationService.requireConversation(conversationId, projectId, userId);
        }
    }

    @FunctionalInterface
    public interface StreamRequest {
        void write(OutputStream outputStream);
    }
}
