package com.researchflow;

import com.researchflow.common.ErrorCode;
import com.researchflow.config.FileStorageProperties;
import com.researchflow.context.UserContext;
import com.researchflow.entity.Document;
import com.researchflow.entity.Project;
import com.researchflow.entity.User;
import com.researchflow.enums.DocumentFileType;
import com.researchflow.enums.DocumentStatus;
import com.researchflow.exception.BusinessException;
import com.researchflow.mapper.DocumentMapper;
import com.researchflow.mapper.UserMapper;
import com.researchflow.message.DocumentMessagePublisher;
import com.researchflow.message.DocumentParseMessage;
import com.researchflow.service.ProjectPermissionService;
import com.researchflow.service.DocumentProcessingService;
import com.researchflow.service.impl.DocumentServiceImpl;
import com.researchflow.storage.LocalFileStorageService;
import com.researchflow.storage.StoredFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTests {

    @Mock private DocumentMapper documentMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProjectPermissionService projectPermissionService;
    @Mock private LocalFileStorageService fileStorageService;
    @Mock private DocumentMessagePublisher messagePublisher;
    @Mock private DocumentProcessingService documentProcessingService;

    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentServiceImpl(
                documentMapper,
                userMapper,
                projectPermissionService,
                fileStorageService,
                new FileStorageProperties("data/documents", 1024 * 1024),
                messagePublisher,
                documentProcessingService
        );
        UserContext.setUserId(1L);
    }

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void memberCanUploadPdfAndPublishParseMessage() {
        Project project = project(100L, 1L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "paper.pdf", "application/pdf", "%PDF-1.7 test".getBytes()
        );
        when(projectPermissionService.requireAccess(100L)).thenReturn(project);
        when(fileStorageService.store(100L, file, DocumentFileType.PDF))
                .thenReturn(new StoredFile("stored.pdf", "100/stored.pdf"));
        when(fileStorageService.absolutePath("100/stored.pdf"))
                .thenReturn(Path.of("C:/data/documents/100/stored.pdf"));
        when(documentMapper.insert(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(500L);
            return 1;
        });
        User uploader = new User();
        uploader.setId(1L);
        uploader.setNickname("上传者");
        when(userMapper.selectById(1L)).thenReturn(uploader);

        var result = documentService.upload(100L, file);

        assertThat(result.getId()).isEqualTo(500L);
        assertThat(result.getOriginalName()).isEqualTo("paper.pdf");
        assertThat(result.getFileType()).isEqualTo(DocumentFileType.PDF);
        assertThat(result.getParseStatus()).isEqualTo(DocumentStatus.WAITING);
        assertThat(result.getVectorStatus()).isEqualTo(DocumentStatus.WAITING);
        assertThat(result.getUploaderName()).isEqualTo("上传者");

        ArgumentCaptor<DocumentParseMessage> messageCaptor =
                ArgumentCaptor.forClass(DocumentParseMessage.class);
        verify(messagePublisher).publishParse(messageCaptor.capture());
        assertThat(messageCaptor.getValue().documentId()).isEqualTo(500L);
        assertThat(messageCaptor.getValue().projectId()).isEqualTo(100L);
        assertThat(messageCaptor.getValue().filePath()).endsWith("stored.pdf");
    }

    @Test
    void fakePdfIsRejectedBeforeStorage() {
        when(projectPermissionService.requireAccess(100L)).thenReturn(project(100L, 1L));
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", "not a pdf".getBytes()
        );

        assertThatThrownBy(() -> documentService.upload(100L, file))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.DOCUMENT_TYPE_NOT_SUPPORTED.getCode());
        verify(fileStorageService, never()).store(any(), any(), any());
    }

    @Test
    void ordinaryMemberCannotDeleteAnotherUsersDocument() {
        UserContext.setUserId(3L);
        Document document = document(500L, 100L, 2L);
        Project project = project(100L, 1L);
        when(documentMapper.selectById(500L)).thenReturn(document);
        when(projectPermissionService.requireAccess(100L)).thenReturn(project);
        when(projectPermissionService.canManage(project, 3L)).thenReturn(false);

        assertThatThrownBy(() -> documentService.delete(500L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.DOCUMENT_ACCESS_DENIED.getCode());
        verify(documentMapper, never()).deleteById(500L);
    }

    @Test
    void uploaderCanDeleteOwnDocumentAndFile() {
        Document document = document(500L, 100L, 1L);
        when(documentMapper.selectById(500L)).thenReturn(document);
        when(projectPermissionService.requireAccess(100L)).thenReturn(project(100L, 2L));

        documentService.delete(500L);

        verify(documentMapper).deleteById(500L);
        verify(fileStorageService).deleteIfExists("100/stored.pdf");
    }

    private Project project(Long id, Long ownerId) {
        Project project = new Project();
        project.setId(id);
        project.setOwnerId(ownerId);
        return project;
    }

    private Document document(Long id, Long projectId, Long uploaderId) {
        Document document = new Document();
        document.setId(id);
        document.setProjectId(projectId);
        document.setUploaderId(uploaderId);
        document.setStoragePath("100/stored.pdf");
        return document;
    }
}
