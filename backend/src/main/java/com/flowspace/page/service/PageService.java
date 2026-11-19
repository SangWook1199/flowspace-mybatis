package com.flowspace.page.service;

import java.util.List;

import com.flowspace.page.dto.request.PageCoverUpdateRequest;
import com.flowspace.page.dto.request.PageCreateRequest;
import com.flowspace.page.dto.request.PageDuplicateRequest;
import com.flowspace.page.dto.request.PageReorderRequest;
import com.flowspace.page.dto.request.PageUpdateRequest;
import com.flowspace.page.dto.response.PageDetailResponse;
import com.flowspace.page.dto.response.PageResponse;
import org.springframework.web.multipart.MultipartFile;

// 페이지 서비스
public interface PageService {

    // 페이지 생성
    PageResponse createPage(Long workspaceId, PageCreateRequest request, String email);

    // 페이지 목록 조회
    List<PageResponse> getPages(Long workspaceId, String email);

    // 페이지 단건 조회
    PageResponse getPage(Long pageId, String email);

    // 페이지 수정
    PageResponse updatePage(Long pageId, PageUpdateRequest request, String email);

    // 페이지 삭제
    void deletePage(Long pageId, String email);

    // 휴지통 목록 조회
    List<PageResponse> getTrash(Long workspaceId, String email);

    // 휴지통 페이지 복원
    PageResponse restorePage(Long pageId, String email);

    // 휴지통 페이지 영구 삭제
    void deletePagePermanently(Long pageId, String email);

    // 휴지통 비우기 (회고 페이지는 남겨둠)
    void emptyTrash(Long workspaceId, String email);

    // 휴지통에 들어간 지 retentionDays일이 지난 페이지를 영구 삭제 (스케줄러가 호출), 반환값: 삭제를 시도한 대상 수
    int purgeExpiredTrash(int retentionDays);

    // 페이지 순서 변경
    void reorderPages(Long workspaceId, PageReorderRequest request, String email);

    // 페이지 복제 (하위 페이지, 블록, 데이터베이스까지 함께 복사)
    PageResponse duplicatePage(Long pageId, PageDuplicateRequest request, String email);

    // 페이지 정보와 블록 전체 조회
    PageDetailResponse getPageDetail(Long pageId, String email);

    // 페이지 커버 업로드
    PageResponse uploadCover(Long pageId, MultipartFile file, String email);

    // 페이지 커버 삭제
    PageResponse deleteCover(Long pageId, String email);

    // 기본 커버 적용
    PageResponse applyDefaultCover(Long pageId, PageCoverUpdateRequest request, String email);
}
