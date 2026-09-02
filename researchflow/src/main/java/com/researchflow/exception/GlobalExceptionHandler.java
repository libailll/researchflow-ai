package com.researchflow.exception;

import com.researchflow.common.ErrorCode;
import com.researchflow.common.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
        log.warn("Request validation failed: {}", message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
        log.warn("Request binding failed: {}", message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(
            ConstraintViolationException e
    ) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(ErrorCode.PARAM_ERROR.getMessage());
        log.warn("Constraint validation failed: {}", message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e
    ) {
        log.warn("Request body is unreadable: {}", e.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        log.warn("Request parameter type mismatch: parameter={}, value={}",
                e.getName(), e.getValue());
        return Result.error(ErrorCode.PARAM_ERROR);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e
    ) {
        log.warn("Uploaded file exceeds configured size limit");
        return Result.error(ErrorCode.DOCUMENT_FILE_TOO_LARGE);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("Unhandled system exception", e);
        return Result.error(ErrorCode.SYSTEM_ERROR);
    }
}
