package com.researchflow.controller;

import com.researchflow.common.ErrorCode;
import com.researchflow.common.Result;
import com.researchflow.config.AiServiceProperties;
import com.researchflow.dto.DocumentFailedDTO;
import com.researchflow.dto.DocumentParsedDTO;
import com.researchflow.exception.BusinessException;
import com.researchflow.service.DocumentProcessingService;
import com.researchflow.vo.DocumentVectorDataVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/ai/documents")
@RequiredArgsConstructor
public class InternalDocumentController {

    private final DocumentProcessingService processingService;
    private final AiServiceProperties aiServiceProperties;

    @PostMapping("/{documentId}/processing")
    public Result<Void> processing(
            @Positive @PathVariable Long documentId,
            @RequestHeader("X-Internal-Token") String token
    ) {
        verifyToken(token);
        processingService.markProcessing(documentId);
        return Result.success();
    }

    @PostMapping("/{documentId}/parsed")
    public Result<Void> parsed(
            @Positive @PathVariable Long documentId,
            @RequestHeader("X-Internal-Token") String token,
            @Valid @RequestBody DocumentParsedDTO request
    ) {
        verifyToken(token);
        processingService.saveParsed(documentId, request.chunks());
        return Result.success();
    }

    @PostMapping("/{documentId}/failed")
    public Result<Void> failed(
            @Positive @PathVariable Long documentId,
            @RequestHeader("X-Internal-Token") String token,
            @Valid @RequestBody DocumentFailedDTO request
    ) {
        verifyToken(token);
        processingService.markFailed(documentId, request.error());
        return Result.success();
    }

    @GetMapping("/{documentId}/vector-data")
    public Result<DocumentVectorDataVO> vectorData(
            @Positive @PathVariable Long documentId,
            @RequestHeader("X-Internal-Token") String token
    ) {
        verifyToken(token);
        return Result.success(processingService.getVectorData(documentId));
    }

    @PostMapping("/{documentId}/vector/processing")
    public Result<Void> vectorProcessing(
            @Positive @PathVariable Long documentId,
            @RequestHeader("X-Internal-Token") String token
    ) {
        verifyToken(token);
        processingService.markVectorProcessing(documentId);
        return Result.success();
    }

    @PostMapping("/{documentId}/vector/success")
    public Result<Void> vectorSuccess(
            @Positive @PathVariable Long documentId,
            @RequestHeader("X-Internal-Token") String token
    ) {
        verifyToken(token);
        processingService.markVectorSuccess(documentId);
        return Result.success();
    }

    @PostMapping("/{documentId}/vector/failed")
    public Result<Void> vectorFailed(
            @Positive @PathVariable Long documentId,
            @RequestHeader("X-Internal-Token") String token,
            @Valid @RequestBody DocumentFailedDTO request
    ) {
        verifyToken(token);
        processingService.markVectorFailed(documentId, request.error());
        return Result.success();
    }

    private void verifyToken(String token) {
        boolean valid = MessageDigest.isEqual(
                aiServiceProperties.internalToken().getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8)
        );
        if (!valid) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVICE_UNAUTHORIZED);
        }
    }
}
