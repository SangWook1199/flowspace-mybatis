package com.flowspace.search.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.search.service.SearchResultVO;

// 검색 Mapper (Search_SQL.xml)
@Mapper
public interface SearchMapper {

    // 제목이 맞는 페이지 (휴지통 제외, 최신순)
    List<SearchResultVO> selectPageListByTitle(@Param("workspaceId") Long workspaceId, @Param("like") String like,
        @Param("limit") int limit);

    // 본문 검색 후보 블록 (프로시저 sp_search_block_list, 최대 100개)
    List<SearchResultVO> selectBlockList(@Param("workspaceId") Long workspaceId, @Param("like") String like);

    // 작업 (프로시저 sp_search_task_list, 상태 이름 포함)
    List<SearchResultVO> selectTaskList(@Param("workspaceId") Long workspaceId, @Param("like") String like,
        @Param("limit") int limit);

    // 스프린트 (이름·목표)
    List<SearchResultVO> selectSprintList(@Param("workspaceId") Long workspaceId, @Param("like") String like,
        @Param("limit") int limit);

    // 댓글 (프로시저 sp_search_comment_list, 작업·페이지 이름과 작성자 포함)
    List<SearchResultVO> selectCommentList(@Param("workspaceId") Long workspaceId, @Param("like") String like,
        @Param("limit") int limit);

    // 일정 (제목·설명, 시작 시각 역순)
    List<SearchResultVO> selectEventList(@Param("workspaceId") Long workspaceId, @Param("like") String like,
        @Param("limit") int limit);
}
