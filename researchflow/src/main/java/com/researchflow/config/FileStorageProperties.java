package com.researchflow.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage.local")
public record FileStorageProperties(
        @NotBlank String root,
        @Positive long maxFileSize
) {
}
