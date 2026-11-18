package com.flowspace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.file.service.FileVO;
import com.flowspace.file.service.impl.FileMapper;
import com.flowspace.file.service.impl.FileServiceImpl;

// 업로드 검사: 위험한 확장자 차단, 이미지는 확장자·Content-Type·첫 바이트가 모두 이미지여야 통과해요.
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private static final byte[] PNG_HEAD = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };

    private static final Long WORKSPACE_ID = 5L;

    @Mock
    private FileMapper fileMapper;

    @InjectMocks
    private FileServiceImpl fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());

        lenient().when(fileMapper.selectUserIdByEmail("a@a.com")).thenReturn(1L);
    }

    private long storedFileCount() throws Exception {
        try (Stream<Path> files = Files.list(tempDir)) {
            return files.count();
        }
    }

    @Test
    @DisplayName("HTML 같은 위험한 확장자는 일반 첨부로도 받지 않는다")
    void blockedExtensionIsRejected() throws Exception {
        MockMultipartFile html = new MockMultipartFile("file", "evil.html", "text/html",
            "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> fileService.upload(html, WORKSPACE_ID, "a@a.com"));

        assertEquals(ErrorCode.UNSUPPORTED_FILE_TYPE, e.getErrorCode());
        assertEquals(0, storedFileCount());
        verify(fileMapper, never()).insertFile(any());
    }

    @Test
    @DisplayName("일반 첨부는 위험하지 않은 확장자면 저장된다")
    void normalAttachmentIsStored() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[] { 1, 2, 3 });

        FileVO saved = fileService.upload(pdf, WORKSPACE_ID, "a@a.com");

        assertTrue(saved.getFileUrl().startsWith("/uploads/"));
        assertTrue(saved.getFileUrl().endsWith(".pdf"));
        assertEquals(WORKSPACE_ID, saved.getWorkspaceId());
        assertEquals(1L, saved.getUploadedBy());
        assertEquals(1, storedFileCount());
        verify(fileMapper).insertFile(saved);
    }

    @Test
    @DisplayName("SVG는 이미지로 올릴 수 없다")
    void svgIsRejectedAsImage() throws Exception {
        MockMultipartFile svg = new MockMultipartFile("file", "logo.svg", "image/svg+xml",
            "<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes(StandardCharsets.UTF_8));

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> fileService.uploadImage(svg, WORKSPACE_ID, "a@a.com"));

        assertEquals(ErrorCode.UNSUPPORTED_FILE_TYPE, e.getErrorCode());
        assertEquals(0, storedFileCount());
    }

    @Test
    @DisplayName("확장자만 png이고 내용이 이미지가 아니면 거절한다")
    void fakeImageIsRejected() throws Exception {
        MockMultipartFile fake = new MockMultipartFile("file", "photo.png", "image/png",
            "<html>not an image</html>".getBytes(StandardCharsets.UTF_8));

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> fileService.uploadImage(fake, WORKSPACE_ID, "a@a.com"));

        assertEquals(ErrorCode.INVALID_IMAGE_FILE, e.getErrorCode());
        assertEquals(0, storedFileCount());
    }

    @Test
    @DisplayName("Content-Type이 이미지가 아니면 거절한다")
    void nonImageContentTypeIsRejected() throws Exception {
        MockMultipartFile wrongType = new MockMultipartFile("file", "photo.png", "text/plain", PNG_HEAD);

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> fileService.uploadImage(wrongType, WORKSPACE_ID, "a@a.com"));

        assertEquals(ErrorCode.INVALID_IMAGE_FILE, e.getErrorCode());
    }

    @Test
    @DisplayName("허용하지 않은 이미지 확장자는 거절한다")
    void unknownImageExtensionIsRejected() throws Exception {
        MockMultipartFile tiff = new MockMultipartFile("file", "scan.tiff", "image/tiff", PNG_HEAD);

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> fileService.uploadImage(tiff, WORKSPACE_ID, "a@a.com"));

        assertEquals(ErrorCode.INVALID_IMAGE_FILE, e.getErrorCode());
    }

    @Test
    @DisplayName("진짜 PNG는 저장된다")
    void realPngIsStored() throws Exception {
        MockMultipartFile png = new MockMultipartFile("file", "photo.PNG", "image/png", PNG_HEAD);

        FileVO saved = fileService.uploadImage(png, WORKSPACE_ID, "a@a.com");

        assertTrue(saved.getFileUrl().startsWith("/uploads/"));
        assertTrue(saved.getFileUrl().endsWith(".png"));
        assertEquals(1, storedFileCount());
    }
}
