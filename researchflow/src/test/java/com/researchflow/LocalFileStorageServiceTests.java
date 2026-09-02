package com.researchflow;

import com.researchflow.config.FileStorageProperties;
import com.researchflow.enums.DocumentFileType;
import com.researchflow.storage.LocalFileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageServiceTests {

    @TempDir
    Path tempDirectory;

    @Test
    void storesLoadsAndDeletesInsideConfiguredRoot() throws Exception {
        LocalFileStorageService service = new LocalFileStorageService(
                new FileStorageProperties(tempDirectory.toString(), 1024)
        );
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "research notes".getBytes()
        );

        var stored = service.store(42L, file, DocumentFileType.TXT);

        assertThat(stored.storagePath()).startsWith("42/").endsWith(".txt");
        assertThat(service.load(stored.storagePath()).getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo("research notes");
        Path absolutePath = service.absolutePath(stored.storagePath());
        assertThat(absolutePath).startsWith(tempDirectory.toAbsolutePath());

        service.deleteIfExists(stored.storagePath());
        assertThat(Files.exists(absolutePath)).isFalse();
    }
}
