package com.researchflow.storage;

import com.researchflow.common.ErrorCode;
import com.researchflow.config.FileStorageProperties;
import com.researchflow.enums.DocumentFileType;
import com.researchflow.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService {

    private final Path root;

    public LocalFileStorageService(FileStorageProperties properties) {
        this.root = Path.of(properties.root()).toAbsolutePath().normalize();
    }

    public StoredFile store(Long projectId, MultipartFile file, DocumentFileType type) {
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "document." + type.getExtension() : file.getOriginalFilename()
        );
        if (originalName.contains("..")) {
            throw new BusinessException(ErrorCode.DOCUMENT_TYPE_NOT_SUPPORTED);
        }

        String storedName = UUID.randomUUID() + "." + type.getExtension();
        Path projectDirectory = safeResolve(String.valueOf(projectId));
        Path target = projectDirectory.resolve(storedName).normalize();
        ensureInsideRoot(target);

        try {
            Files.createDirectories(projectDirectory);
            Files.copy(file.getInputStream(), target);
            String relativePath = root.relativize(target).toString().replace('\\', '/');
            return new StoredFile(storedName, relativePath);
        } catch (IOException e) {
            log.error("Failed to store document: projectId={}, originalName={}", projectId, originalName, e);
            throw new BusinessException(ErrorCode.DOCUMENT_STORAGE_FAILED);
        }
    }

    public Resource load(String storagePath) {
        Path file = safeResolve(storagePath);
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
    }

    public void deleteIfExists(String storagePath) {
        Path file = safeResolve(storagePath);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete document file: path={}", storagePath, e);
        }
    }

    public Path absolutePath(String storagePath) {
        return safeResolve(storagePath);
    }

    private Path safeResolve(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        ensureInsideRoot(resolved);
        return resolved;
    }

    private void ensureInsideRoot(Path path) {
        if (!path.startsWith(root)) {
            throw new BusinessException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }
    }
}
