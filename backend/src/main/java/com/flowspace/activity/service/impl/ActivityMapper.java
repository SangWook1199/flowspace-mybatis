package com.flowspace.activity.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.activity.service.ActivityVO;

// 활동 기록 Mapper (Activity_SQL.xml)
@Mapper
public interface ActivityMapper {

    // 활동 기록 등록
    int insertActivity(ActivityVO activity);

    // 활동 기록 목록 (sp_activity_list)
    // types: 쉼표로 연결한 종류 문자열, null이면 전체
    List<ActivityVO> selectActivityList(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId,
        @Param("types") String types, @Param("offset") int offset, @Param("limit") int limit);

    // 같은 조건의 활동 기록 수 (sp_activity_count, 전체 페이지 수 계산용)
    // types: 쉼표로 연결한 종류 문자열, null이면 전체
    long selectActivityCount(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId,
        @Param("types") String types);

    // 사용자 존재 여부
    int selectUserCount(Long userId);
}
