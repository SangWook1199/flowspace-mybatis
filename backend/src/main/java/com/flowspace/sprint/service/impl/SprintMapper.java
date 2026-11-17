package com.flowspace.sprint.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.flowspace.sprint.service.SprintVO;

// 스프린트 DB 접근. 메서드 이름이 Sprint_SQL.xml의 id와 같아야 해요.
@Mapper
public interface SprintMapper {

    // 스프린트 한 건 (없으면 null)
    SprintVO selectSprintById(Long sprintId);

    // 워크스페이스의 스프린트 목록 (시작일이 늦은 순)
    List<SprintVO> selectSprintListByWorkspaceId(Long workspaceId);

    // 스프린트에 속한 작업 수
    int selectTaskCountBySprintId(Long sprintId);

    // 스프린트에 속한 작업 중 완료(DONE) 상태인 작업 수
    int selectDoneTaskCountBySprintId(Long sprintId);
}
