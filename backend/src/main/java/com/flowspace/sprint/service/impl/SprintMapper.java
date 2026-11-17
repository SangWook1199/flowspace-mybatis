package com.flowspace.sprint.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.flowspace.sprint.service.SprintProgressVO;
import com.flowspace.sprint.service.SprintVO;

// 스프린트 DB 접근. 메서드 이름이 Sprint_SQL.xml의 id와 같아야 해요.
@Mapper
public interface SprintMapper {

    // 스프린트 한 건 (없으면 null)
    SprintVO selectSprintById(Long sprintId);

    // 워크스페이스의 스프린트 목록과 진행 상황 (프로시저 sp_sprint_progress_list)
    List<SprintProgressVO> selectSprintProgressList(Long workspaceId);

    // 스프린트 한 건과 진행 상황 (프로시저 sp_sprint_progress, 없으면 null)
    SprintProgressVO selectSprintProgressById(Long sprintId);
}
