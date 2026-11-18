package com.flowspace.file.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.flowspace.file.dto.response.CoverResponse;

// 파일 저장·복사·삭제 기능이에요. 실제 동작은 service.impl의 FileServiceImpl에 있어요.
// 프로필 사진, 페이지 커버, 블록 이미지, 작업 관련 문서가 이 서비스를 불러요.
// 엔티티 대신 번호(워크스페이스, 사람, 파일)를 받고, 저장한 파일 정보는 FileVO로 돌려줘요.
public interface FileService {

    // 파일 업로드 (파일 블록처럼 종류를 가리지 않는 첨부)
    FileVO upload(MultipartFile multipartFile, Long workspaceId, String email);

    // 문서 업로드 (작업 관련 문서). 허용 목록에 있는 확장자만 받아요.
    FileVO uploadDocument(MultipartFile multipartFile, Long workspaceId, String email);

    // 이미지 업로드 (커버·프로필·이미지 블록). 확장자·Content-Type·파일 첫 바이트가 모두 이미지여야 해요.
    FileVO uploadImage(MultipartFile multipartFile, Long workspaceId, String email);

    // 프로필 이미지 저장 (소셜 로그인 프로필 사진처럼 바이트로 받은 이미지)
    FileVO uploadProfileImage(byte[] imageBytes, Long workspaceId, Long userId);

    // 파일 복사 (페이지 복제용, 원본 파일이 없으면 null)
    FileVO copy(Long sourceFileId, Long workspaceId, Long userId);

    // 기본 커버 파일 정보 생성 (실제 파일은 static/covers에 있어요)
    FileVO createDefaultCover(String coverName, Long workspaceId, Long userId);

    // 파일 삭제 (서버 파일과 DB 행). 없는 번호면 아무것도 하지 않아요.
    void delete(Long fileId);

    // 기본 커버 갤러리 조회
    List<CoverResponse> getCoverGallery();
}
