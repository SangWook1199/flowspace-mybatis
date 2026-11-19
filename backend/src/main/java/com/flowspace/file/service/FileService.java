package com.flowspace.file.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.flowspace.file.dto.response.CoverResponse;

// 파일 저장·복사·삭제 서비스 (프로필, 페이지 커버, 블록 이미지, 작업 관련 문서)
// 엔티티 대신 번호를 받고 FileVO 반환
public interface FileService {

    // 파일 업로드 (파일 블록처럼 종류를 가리지 않는 첨부)
    FileVO upload(MultipartFile multipartFile, Long workspaceId, String email);

    // 문서 업로드 (작업 관련 문서, 허용 확장자만)
    FileVO uploadDocument(MultipartFile multipartFile, Long workspaceId, String email);

    // 이미지 업로드 (커버·프로필·이미지 블록, 확장자·Content-Type·시작 바이트 모두 확인)
    FileVO uploadImage(MultipartFile multipartFile, Long workspaceId, String email);

    // 프로필 이미지 저장 (소셜 프로필 사진 등 바이트 데이터)
    FileVO uploadProfileImage(byte[] imageBytes, Long workspaceId, Long userId);

    // 파일 복사 (페이지 복제용, 원본 파일이 없으면 null)
    FileVO copy(Long sourceFileId, Long workspaceId, Long userId);

    // 기본 커버 파일 정보 생성 (실제 파일은 static/covers)
    FileVO createDefaultCover(String coverName, Long workspaceId, Long userId);

    // 파일 삭제 (서버 파일 + DB 행, 없는 번호는 무시)
    void delete(Long fileId);

    // 기본 커버 갤러리 조회
    List<CoverResponse> getCoverGallery();
}
