package com.researchflow.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai.service")
public record AiServiceProperties(
        @NotBlank String baseUrl,
        @NotBlank String internalToken
) {
}
