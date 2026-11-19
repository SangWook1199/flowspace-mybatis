package com.flowspace.ai.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.calendar.service.EventVO;
import com.flowspace.sprint.service.SprintVO;
import com.flowspace.task.service.SubTaskVO;
import com.flowspace.task.service.TaskStatusVO;
import com.flowspace.task.service.TaskVO;
import com.flowspace.user.service.UserVO;
import com.flowspace.workspace.service.WorkspaceMemberVO;

// AI 기능 Mapper (Ai_SQL.xml, 다른 도메인 프로시저만 호출)
@Mapper
public interface AiMapper {

    // 이메일로 사용자
    UserVO selectUserByEmail(String email);

    // 워크스페이스 멤버 목록
    List<WorkspaceMemberVO> selectWorkspaceMemberList(Long workspaceId);

    // 워크스페이스 멤버의 사용자 번호
    List<Long> selectWorkspaceUserIds(Long workspaceId);

    // 워크스페이스 작업 상태 목록 (위치 순)
    List<TaskStatusVO> selectTaskStatusList(Long workspaceId);

    // 워크스페이스의 최대 작업 번호
    int selectMaxTaskNumber(Long workspaceId);

    // 워크스페이스에서 해당 상태인 작업 수
    int selectTaskCountByStatus(@Param("workspaceId") Long workspaceId, @Param("statusId") Long statusId);

    // 스프린트 등록
    int insertSprint(SprintVO sprint);

    // 작업 등록
    int insertTask(TaskVO task);

    // 작업 담당자 등록
    int insertTaskAssignee(@Param("taskId") Long taskId, @Param("userId") Long userId);

    // 하위 작업 등록
    int insertSubTask(SubTaskVO subTask);

    // 일정 등록
    int insertEvent(EventVO event);
}
