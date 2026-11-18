package com.flowspace.activity.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.activity.service.ActivityVO;
import com.flowspace.entity.enums.ActivityType;

// 활동 기록 DB 접근. 메서드 이름이 Activity_SQL.xml의 id와 같아야 해요.
@Mapper
public interface ActivityMapper {

    // 활동 기록 등록
    int insertActivity(ActivityVO activity);

    // 활동 기록 목록 (프로시저 sp_activity_list)
    // types는 쉼표로 이은 종류 문자열이에요 (null이면 모두)
    List<ActivityVO> selectActivityList(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId,
        @Param("types") String types, @Param("offset") int offset, @Param("limit") int limit);

    // 같은 조건의 활동 기록 수 (전체 페이지 수를 알려줄 때 써요)
    // types는 목록 그대로 넘겨요 (null이면 모두)
    long selectActivityCount(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId,
        @Param("types") List<ActivityType> types);

    // 사용자가 있는지 (한 사람의 활동을 볼 때 확인해요)
    int selectUserCount(Long userId);
}
