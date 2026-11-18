package com.flowspace.sprint.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.entity.enums.SprintStatus;

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

    // 워크스페이스에서 해당 상태인 스프린트 수
    // 인자가 두 개 이상이면 @Param 이름으로 XML의 #{workspaceId}, #{status}와 연결해요.
    int selectSprintCountByStatus(@Param("workspaceId") Long workspaceId, @Param("status") SprintStatus status);

    // 스프린트 등록. 실행 후 DB가 만든 번호가 sprint의 sprintId에 들어가요.
    int insertSprint(SprintVO sprint);

    // 스프린트 수정. 바뀐 행 수를 돌려줘요.
    int updateSprint(SprintVO sprint);
}
