package com.researchflow.vo;

public record ExportedFile(byte[] content, String fileName, String contentType) {
}
