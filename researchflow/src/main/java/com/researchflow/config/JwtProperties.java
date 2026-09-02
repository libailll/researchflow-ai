package com.researchflow.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank(message = "JWT 密钥不能为空")
        @Size(min = 32, message = "JWT 密钥长度不能少于 32 个字符")
        String secretKey,

        @Positive(message = "JWT 有效期必须大于 0")
        long ttl
) {
}
