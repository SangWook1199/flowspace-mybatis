package com.flowspace.page.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.page.service.PageVO;

// 페이지 DB 접근. 메서드 이름이 Page_SQL.xml의 id와 같아야 해요.
@Mapper
public interface PageMapper {

    // 이메일로 사용자 번호 (만든 사람을 남길 때 써요, 없으면 null)
    Long selectUserIdByEmail(String email);

    // ---------- 조회 ----------

    // 페이지 목록 (프로시저 sp_page_list, deleted가 true면 휴지통)
    List<PageVO> selectPageList(@Param("workspaceId") Long workspaceId, @Param("deleted") boolean deleted);

    // 페이지 한 개 (프로시저 sp_page, 휴지통에 있어도 나와요, 없으면 null)
    PageVO selectPageById(Long pageId);

    // 본인과 모든 하위 페이지 (프로시저 sp_page_subtree)
    List<PageVO> selectPageSubtree(Long pageId);

    // 여러 페이지의 워크스페이스·상위 페이지·휴지통 여부 (순서 바꾸기 확인용)
    List<PageVO> selectPageListByIds(@Param("pageIds") List<Long> pageIds);

    // 상위 페이지 번호 (최상위면 null)
    Long selectParentPageId(Long pageId);

    // 같은 상위 페이지 아래에 있는 페이지 수 (parentPageId가 null이면 최상위)
    int selectSiblingCount(@Param("workspaceId") Long workspaceId, @Param("parentPageId") Long parentPageId);

    // ---------- 등록·수정 ----------

    // 페이지 등록. 실행 후 DB가 만든 번호가 page의 pageId에 들어가요.
    int insertPage(PageVO page);

    // 제목·아이콘·상위 페이지 수정
    int updatePage(PageVO page);

    // 순서 수정
    int updatePagePosition(@Param("pageId") Long pageId, @Param("position") int position);

    // 상위 페이지와 순서 수정 (복원할 때 최상위로 올려요)
    int updatePageParent(@Param("pageId") Long pageId, @Param("parentPageId") Long parentPageId,
        @Param("position") int position);

    // 커버 수정 (fileId가 null이면 커버 없음)
    int updatePageCover(@Param("pageId") Long pageId, @Param("fileId") Long fileId);

    // ---------- 휴지통 ----------

    // 여러 페이지를 같은 시각으로 휴지통에 보내요
    int updatePagesTrashed(@Param("pageIds") List<Long> pageIds, @Param("deletedAt") LocalDateTime deletedAt);

    // 여러 페이지를 복원해요
    int updatePagesRestored(@Param("pageIds") List<Long> pageIds);

    // 휴지통에 가는 페이지를 가리키는 데이터베이스 행 삭제 (같이 휴지통에 가는 페이지 안의 데이터베이스는 빼요)
    int deleteOutsideDatabaseRows(@Param("pageIds") List<Long> pageIds, @Param("subtreeIds") List<Long> subtreeIds);
}
