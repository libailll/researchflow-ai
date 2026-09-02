package com.researchflow.controller;

import com.researchflow.common.Result;
import com.researchflow.dto.AiChatDTO;
import com.researchflow.dto.SemanticSearchDTO;
import com.researchflow.service.AiChatService;
import com.researchflow.vo.AiChatVO;
import com.researchflow.vo.SemanticSearchResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/ai")
@RequiredArgsConstructor
@Tag(name = "AI 助手", description = "项目 AI 普通与流式对话")
@SecurityRequirement(name = "bearerAuth")
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    @Operation(summary = "AI 普通对话")
    public Result<AiChatVO> chat(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @Valid @RequestBody AiChatDTO request
    ) {
        return Result.success(aiChatService.chat(projectId, request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI SSE 流式对话")
    public ResponseEntity<StreamingResponseBody> stream(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @Valid @RequestBody AiChatDTO request
    ) {
        AiChatService.StreamRequest streamRequest = aiChatService.stream(projectId, request);
        StreamingResponseBody body = streamRequest::write;
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .cacheControl(CacheControl.noStore())
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    @PostMapping("/search")
    @Operation(summary = "检索项目文档相关片段")
    public Result<List<SemanticSearchResultVO>> search(
            @Positive(message = "项目ID必须大于 0") @PathVariable Long projectId,
            @Valid @RequestBody SemanticSearchDTO request
    ) {
        return Result.success(aiChatService.search(projectId, request));
    }
}
