package com.researchflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "report.export")
public record ReportExportProperties(String fontPath) {
}
