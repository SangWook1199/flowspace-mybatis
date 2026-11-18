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

    // 파일 SQL (같은 패키지라 import가 없어요)
    private final FileMapper fileMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 올릴 수 없는 확장자예요. 업로드한 파일은 /uploads 주소에서 그대로 열리는데, 브라우저가 HTML·SVG·스크립트를 열면
    // 그 안의 코드가 이 서비스의 주소에서 실행될 수 있어서 처음부터 받지 않아요.
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of("html", "htm", "xhtml", "xht", "shtml", "svg", "svgz",
        "xml", "xsl", "xslt", "rdf", "atom", "rss", "xul", "mml", "mathml", "js", "mjs", "jsp", "php", "asp", "aspx", "swf");

    // 작업의 "관련 문서"로 올릴 수 있는 확장자예요(오피스·한글·PDF·텍스트·압축·이미지). 여기에 없는 확장자는 받지 않아요.
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("doc", "docx", "xls", "xlsx", "xlsm", "ppt", "pptx",
        "hwp", "hwpx", "pdf", "txt", "csv", "md", "rtf", "odt", "ods", "odp", "zip", "7z", "png", "jpg", "jpeg", "gif",
        "webp", "bmp");

    // 이미지로 올릴 수 있는 확장자예요(SVG는 스크립트를 담을 수 있어서 제외해요).
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp", "avif");

    // 파일 업로드 (파일 블록처럼 종류를 가리지 않는 첨부)
    @Override
    public FileVO upload(MultipartFile multipartFile, Long workspaceId, String email) {
        return store(multipartFile, workspaceId, email, false);
    }

    // 문서 업로드 (작업 관련 문서). 허용 목록에 있는 확장자만 받아요.
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

    // 이미지 업로드 (커버·프로필·이미지 블록). 확장자·Content-Type·파일 첫 바이트가 모두 이미지여야 해요.
    @Override
    public FileVO uploadImage(MultipartFile multipartFile, Long workspaceId, String email) {
        return store(multipartFile, workspaceId, email, true);
    }

    // 실제 저장 처리: 확장자를 소문자 영숫자로 정리해서 쓰고, 막은 확장자·이미지가 아닌 파일은 거절해요.
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

    // 저장할 파일 정보를 만들어요 (가로·세로 크기는 지금 쓰지 않아서 비워 둬요)
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

    // 파일 이름의 마지막 확장자를 소문자로 돌려줘요. 영숫자 10자 이하가 아니면(경로 문자 등) 확장자 없음으로 봐요.
    private String extensionOf(String originalName) {

        if (originalName == null || !originalName.contains(".")) {
            return "";
        }

        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);

        return extension.matches("[a-z0-9]{1,10}") ? extension : "";
    }

    // 이미지 파일인지 확인해요: 허용한 확장자 + image/* (SVG 제외) + 파일 맨 앞 바이트가 실제 이미지 형식.
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

    // PNG·JPEG·GIF·WEBP·BMP·AVIF 중 하나의 시작 바이트와 맞는지 봐요.
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

    // 파일 삭제 (없는 번호면 아무것도 하지 않아요)
    @Override
    public void delete(Long fileId) {

        FileVO file = fileMapper.selectFileById(fileId);

        if (file == null) {
            return;
        }

        // 기본 커버는 실제 파일 없이 정적 리소스를 가리켜서, 서버 폴더에서 지울 파일이 없어요.
        if (!file.getFileUrl().startsWith("/covers/")) {
            try {
                Path path = Paths.get(uploadDir).resolve(file.getStoredName());
                Files.deleteIfExists(path);
            } catch (IOException e) {
                throw new FlowSpaceException(ErrorCode.FILE_DELETE_FAILED);
            }
        }

        // 이 파일을 쓰던 곳(프로필·커버·이미지 블록)은 DB가 NULL로 바꾸고, 관련 문서는 함께 지워요(외래키 설정).
        fileMapper.deleteFile(fileId);
    }

    // 파일 복사 (페이지 복제용, 원본 파일이 없으면 null)
    @Override
    public FileVO copy(Long sourceFileId, Long workspaceId, Long userId) {

        FileVO source = fileMapper.selectFileById(sourceFileId);

        if (source == null) {
            return null;
        }

        // 기본 커버는 실제 파일 없이 정적 리소스를 가리키므로 행만 새로 만들어요.
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

        // 커버 이름에 경로 문자가 섞여 있으면 static/covers 밖을 가리킬 수 있어서 막아요.
        if (coverName == null || coverName.isBlank() || coverName.contains("/") || coverName.contains("\\")
            || coverName.contains("..")) {
            throw new FlowSpaceException(ErrorCode.FILE_NOT_FOUND);
        }

        // 갤러리에는 실제 파일 확장자(png 등)로 올라가 있어서, 있는 확장자를 찾아서 써요.
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
            // jar로 실행하면 static/covers가 파일 폴더가 아니라 jar 안에 있어서 getFile()이 실패해요 — 클래스패스 패턴으로 읽어요.
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
