package com.flowspace.sprint.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.entity.enums.SprintStatus;

import com.flowspace.sprint.service.SprintProgressVO;
import com.flowspace.sprint.service.SprintVO;

// 스프린트 Mapper (Sprint_SQL.xml)
@Mapper
public interface SprintMapper {

    // 스프린트 한 건 (없으면 null)
    SprintVO selectSprintById(Long sprintId);

    // 워크스페이스의 스프린트 목록과 진행 상황 (프로시저 sp_sprint_progress_list)
    List<SprintProgressVO> selectSprintProgressList(Long workspaceId);

    // 스프린트 한 건과 진행 상황 (프로시저 sp_sprint_progress, 없으면 null)
    SprintProgressVO selectSprintProgressById(Long sprintId);

    // 워크스페이스의 해당 상태 스프린트 수
    // 인자가 둘 이상이면 @Param 이름으로 XML 파라미터와 연결
    int selectSprintCountByStatus(@Param("workspaceId") Long workspaceId, @Param("status") SprintStatus status);

    // 스프린트 등록 (생성된 번호는 sprintId에 설정)
    int insertSprint(SprintVO sprint);

    // 스프린트 수정 (변경 행 수 반환)
    int updateSprint(SprintVO sprint);

    // 스프린트 시작 (sp_start_sprint, 진행 중이던 스프린트는 계획으로 변경)
    void updateSprintStart(Long sprintId);

    // 스프린트 완료 (sp_complete_sprint, 미완료 작업은 백로그로 이동)
    void updateSprintComplete(Long sprintId);

    // 스프린트 삭제 (sp_delete_sprint, 작업은 백로그로 이동)
    void deleteSprint(Long sprintId);
}
