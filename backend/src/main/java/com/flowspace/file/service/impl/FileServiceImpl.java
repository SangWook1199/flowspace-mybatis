package com.flowspace.file.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.file.dto.response.CoverResponse;
import com.flowspace.file.service.FileService;
import com.flowspace.file.service.FileVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FileServiceImpl implements FileService {

    // 파일 SQL
    private final FileMapper fileMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 업로드 금지 확장자 (/uploads에서 HTML·SVG·스크립트가 실행되는 것을 방지)
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of("html", "htm", "xhtml", "xht", "shtml", "svg", "svgz",
        "xml", "xsl", "xslt", "rdf", "atom", "rss", "xul", "mml", "mathml", "js", "mjs", "jsp", "php", "asp", "aspx", "swf");

    // 작업 관련 문서 허용 확장자 (오피스·한글·PDF·텍스트·압축·이미지)
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("doc", "docx", "xls", "xlsx", "xlsm", "ppt", "pptx",
        "hwp", "hwpx", "pdf", "txt", "csv", "md", "rtf", "odt", "ods", "odp", "zip", "7z", "png", "jpg", "jpeg", "gif",
        "webp", "bmp");

    // 이미지 허용 확장자 (스크립트를 담을 수 있는 SVG 제외)
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp", "avif");

    // 파일 업로드 (파일 블록처럼 종류를 가리지 않는 첨부)
    @Override
    public FileVO upload(MultipartFile multipartFile, Long workspaceId, String email) {
        return store(multipartFile, workspaceId, email, false);
    }

    // 문서 업로드 (작업 관련 문서, 허용 확장자만)
    @Override
    public FileVO uploadDocument(MultipartFile multipartFile, Long workspaceId, String email) {

        if (multipartFile.isEmpty()) {
            throw new FlowSpaceException(ErrorCode.EMPTY_FILE);
        }

        if (!DOCUMENT_EXTENSIONS.contains(extensionOf(multipartFile.getOriginalFilename()))) {
            throw new FlowSpaceException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        return store(multipartFile, workspaceId, email, false);
    }

    // 이미지 업로드 (커버·프로필·이미지 블록, 확장자·Content-Type·시작 바이트 모두 확인)
    @Override
    public FileVO uploadImage(MultipartFile multipartFile, Long workspaceId, String email) {
        return store(multipartFile, workspaceId, email, true);
    }

    // 파일 저장 공통 처리 (확장자 정리, 금지 확장자·비이미지 거절)
    private FileVO store(MultipartFile multipartFile, Long workspaceId, String email, boolean imageOnly) {

        Long userId = fileMapper.selectUserIdByEmail(email);

        if (userId == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        String originalName = multipartFile.getOriginalFilename();
        String extension = extensionOf(originalName);

        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new FlowSpaceException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        if (imageOnly) {
            validateImage(multipartFile, extension);
        }

        try {

            String storedName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

            Path directory = Paths.get(uploadDir);
            Files.createDirectories(directory);

            Path target = directory.resolve(storedName);
            multipartFile.transferTo(target);

            FileVO file = newFile(workspaceId, userId, originalName, storedName, multipartFile.getContentType(),
                multipartFile.getSize(), "/uploads/" + storedName);

            fileMapper.insertFile(file);

            return file;

        } catch (IOException e) {
            throw new FlowSpaceException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // 저장할 파일 정보 생성 (가로·세로는 미사용)
    private FileVO newFile(Long workspaceId, Long userId, String originalName, String storedName, String mimeType,
        Long size, String fileUrl) {

        FileVO file = new FileVO();
        file.setWorkspaceId(workspaceId);
        file.setUploadedBy(userId);
        file.setOriginalName(originalName);
        file.setStoredName(storedName);
        file.setMimeType(mimeType);
        file.setSize(size);
        file.setFileUrl(fileUrl);

        return file;
    }

    // 마지막 확장자를 소문자로 반환 (영숫자 10자 이하가 아니면 확장자 없음)
    private String extensionOf(String originalName) {

        if (originalName == null || !originalName.contains(".")) {
            return "";
        }

        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);

        return extension.matches("[a-z0-9]{1,10}") ? extension : "";
    }

    // 이미지 검사: 허용 확장자 + image/* (SVG 제외) + 실제 이미지 시작 바이트
    private void validateImage(MultipartFile multipartFile, String extension) {

        String contentType = multipartFile.getContentType() == null ? ""
            : multipartFile.getContentType().toLowerCase(Locale.ROOT);

        if (!contentType.startsWith("image/") || contentType.contains("svg") || !IMAGE_EXTENSIONS.contains(extension)) {
            throw new FlowSpaceException(ErrorCode.INVALID_IMAGE_FILE);
        }

        byte[] head = new byte[12];
        int length;

        try (InputStream in = multipartFile.getInputStream()) {
            length = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new FlowSpaceException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        if (!hasImageSignature(head, length)) {
            throw new FlowSpaceException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    // PNG·JPEG·GIF·WEBP·BMP·AVIF 시작 바이트 확인
    private boolean hasImageSignature(byte[] h, int length) {

        boolean png = length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
        boolean jpeg = length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
        boolean gif = length >= 4 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8';
        boolean webp = length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W'
            && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
        boolean bmp = length >= 2 && h[0] == 'B' && h[1] == 'M';
        boolean avif = length >= 12 && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p';

        return png || jpeg || gif || webp || bmp || avif;
    }

    // 파일 삭제 (없는 번호는 무시)
    @Override
    public void delete(Long fileId) {

        FileVO file = fileMapper.selectFileById(fileId);

        if (file == null) {
            return;
        }

        if (!file.getFileUrl().startsWith("/covers/")) {
            try {
                Path path = Paths.get(uploadDir).resolve(file.getStoredName());
                Files.deleteIfExists(path);
            } catch (IOException e) {
                throw new FlowSpaceException(ErrorCode.FILE_DELETE_FAILED);
            }
        }

        fileMapper.deleteFile(fileId);
    }

    // 파일 복사 (페이지 복제용, 원본 파일이 없으면 null)
    @Override
    public FileVO copy(Long sourceFileId, Long workspaceId, Long userId) {

        FileVO source = fileMapper.selectFileById(sourceFileId);

        if (source == null) {
            return null;
        }

        if (source.getFileUrl().startsWith("/covers/")) {

            FileVO cover = newFile(workspaceId, userId, source.getOriginalName(), source.getStoredName(),
                source.getMimeType(), source.getSize(), source.getFileUrl());
            cover.setWidth(source.getWidth());
            cover.setHeight(source.getHeight());

            fileMapper.insertFile(cover);

            return cover;
        }

        try {

            Path directory = Paths.get(uploadDir);
            Path sourcePath = directory.resolve(source.getStoredName());

            if (!Files.exists(sourcePath)) {
                return null;
            }

            String storedName = source.getStoredName();
            String extension = storedName.contains(".") ? storedName.substring(storedName.lastIndexOf(".")) : "";
            String copiedName = UUID.randomUUID() + extension;

            Files.copy(sourcePath, directory.resolve(copiedName), StandardCopyOption.REPLACE_EXISTING);

            FileVO copied = newFile(workspaceId, userId, source.getOriginalName(), copiedName, source.getMimeType(),
                source.getSize(), "/uploads/" + copiedName);
            copied.setWidth(source.getWidth());
            copied.setHeight(source.getHeight());

            fileMapper.insertFile(copied);

            return copied;

        } catch (IOException e) {
            throw new FlowSpaceException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // 기본 커버 파일 정보 생성
    @Override
    public FileVO createDefaultCover(String coverName, Long workspaceId, Long userId) {

        if (coverName == null || coverName.isBlank() || coverName.contains("/") || coverName.contains("\\")
            || coverName.contains("..")) {
            throw new FlowSpaceException(ErrorCode.FILE_NOT_FOUND);
        }

        String fileName = coverName + ".png";
        ClassPathResource resource = new ClassPathResource("static/covers/" + fileName);

        for (String ext : List.of("png", "jpg", "jpeg", "webp")) {
            ClassPathResource candidate = new ClassPathResource("static/covers/" + coverName + "." + ext);
            if (candidate.exists()) {
                fileName = coverName + "." + ext;
                resource = candidate;
                break;
            }
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

        String mimeType = switch (extension.toLowerCase()) {
        case "png" -> "image/png";
        case "webp" -> "image/webp";
        default -> "image/jpeg";
        };

        if (!resource.exists()) {
            throw new FlowSpaceException(ErrorCode.FILE_NOT_FOUND);
        }

        FileVO file = newFile(workspaceId, userId, fileName, fileName, mimeType, 0L, "/covers/" + fileName);

        fileMapper.insertFile(file);

        return file;
    }

    // 기본 커버 갤러리 조회
    @Transactional(readOnly = true)
    @Override
    public List<CoverResponse> getCoverGallery() {

        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:static/covers/*");

            return Arrays.stream(resources).map(Resource::getFilename)
                .filter(fileName -> fileName != null && fileName.lastIndexOf('.') > 0).sorted().map(fileName -> {
                    String name = fileName.substring(0, fileName.lastIndexOf('.'));

                    return new CoverResponse(name, "/covers/" + fileName);
                }).toList();

        } catch (IOException e) {
            throw new FlowSpaceException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    // 프로필 이미지 저장
    @Override
    public FileVO uploadProfileImage(byte[] imageBytes, Long workspaceId, Long userId) {

        try {

            String storedName = UUID.randomUUID() + ".jpg";

            Path directory = Paths.get(uploadDir);
            Files.createDirectories(directory);

            Path target = directory.resolve(storedName);
            Files.write(target, imageBytes);

            FileVO file = newFile(workspaceId, userId, "profile.jpg", storedName, "image/jpeg",
                (long) imageBytes.length, "/uploads/" + storedName);

            fileMapper.insertFile(file);

            return file;

        } catch (IOException e) {
            throw new FlowSpaceException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}
