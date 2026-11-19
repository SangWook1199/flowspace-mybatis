package com.flowspace.task.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.task.service.SubTaskVO;
import com.flowspace.task.service.TaskAssigneeVO;
import com.flowspace.task.service.TaskStatusVO;
import com.flowspace.task.service.TaskVO;

// 작업 Mapper (Task_SQL.xml)
@Mapper
public interface TaskMapper {

    // 칸반 작업 목록과 상태 이름 (프로시저 sp_task_list, sprintId가 null이면 백로그)
    List<TaskVO> selectTaskList(@Param("workspaceId") Long workspaceId, @Param("sprintId") Long sprintId);

    // 칸반 작업들의 담당자 (프로시저 sp_task_assignee_list)
    List<TaskAssigneeVO> selectTaskAssigneeList(@Param("workspaceId") Long workspaceId,
        @Param("sprintId") Long sprintId);

    // 여러 작업의 하위 작업 (taskIds가 비어 있으면 호출하지 않음)
    List<SubTaskVO> selectSubTaskList(@Param("taskIds") List<Long> taskIds);

    // 작업 한 건과 상태 이름 (프로시저 sp_task, 없으면 null)
    TaskVO selectTaskById(Long taskId);

    // 작업 한 건의 담당자 (프로시저 sp_task_assignee)
    List<TaskAssigneeVO> selectTaskAssigneeListByTaskId(Long taskId);

    // 워크스페이스의 해당 상태 작업 수 (새 작업 위치 계산용)
    int selectTaskCountByStatus(@Param("workspaceId") Long workspaceId, @Param("statusId") Long statusId);

    // 워크스페이스에서 지금까지 쓴 가장 큰 작업 번호 (작업이 없으면 0)
    int selectMaxTaskNumber(Long workspaceId);

    // 작업 등록 (생성된 번호는 taskId에 설정)
    int insertTask(TaskVO task);

    // 작업 담당자 등록
    int insertTaskAssignee(@Param("taskId") Long taskId, @Param("userId") Long userId);

    // 작업 수정 (순서·완료 시각 포함)
    int updateTask(TaskVO task);

    // 작업 담당자의 사용자 번호 목록
    List<Long> selectTaskAssigneeUserIds(Long taskId);

    // 작업 담당자 모두 삭제
    int deleteTaskAssignees(Long taskId);

    // 작업 담당자가 아닌 하위 작업 담당자 해제 (assigneeIds가 비어 있으면 전체 해제)
    int updateSubTaskAssigneeClear(@Param("taskId") Long taskId, @Param("assigneeIds") List<Long> assigneeIds);

    // 작업 삭제 (sp_delete_task, 같은 칸의 순서 재정렬 포함)
    void deleteTask(Long taskId);

    // 작업 담당자인지 확인 (1이면 담당자, 0이면 아님)
    int selectTaskAssigneeCount(@Param("taskId") Long taskId, @Param("userId") Long userId);

    // 하위 작업 한 건 (없으면 null)
    SubTaskVO selectSubTaskById(Long subtaskId);

    // 작업의 하위 작업 수 (새 하위 작업 위치 계산용)
    int selectSubTaskCount(Long taskId);

    // 하위 작업 등록 (생성된 번호는 subtaskId에 설정)
    int insertSubTask(SubTaskVO subTask);

    // 하위 작업 수정 (내용·담당자·완료 여부)
    int updateSubTask(SubTaskVO subTask);

    // 하위 작업 순서 변경
    int updateSubTaskPosition(@Param("subtaskId") Long subtaskId, @Param("position") Integer position);

    // 하위 작업 삭제
    int deleteSubTask(Long subtaskId);

    // 워크스페이스의 작업 상태 목록 (프로시저 sp_task_status_list)
    List<TaskStatusVO> selectTaskStatusList(Long workspaceId);

    // 워크스페이스의 작업 상태 한 개 (프로시저 sp_task_status, 그 워크스페이스의 상태가 아니면 null)
    TaskStatusVO selectTaskStatusById(@Param("workspaceId") Long workspaceId, @Param("statusId") Long statusId);

    // 워크스페이스의 작업 상태 수
    int selectTaskStatusCount(Long workspaceId);

    // 상태 등록 (생성된 번호는 statusId에 설정)
    int insertTaskStatus(TaskStatusVO status);

    // 워크스페이스에 상태 연결 (순서·WIP 제한)
    int insertWorkspaceTaskStatus(TaskStatusVO status);

    // 상태 정보 수정 (이름·분류·색)
    int updateTaskStatusInfo(TaskStatusVO status);

    // 워크스페이스별 WIP 제한 수정
    int updateWorkspaceTaskStatusWipLimit(TaskStatusVO status);

    // 칸반 열 순서 변경 (변경 행 수 반환, 0이면 다른 워크스페이스의 상태)
    int updateTaskStatusPosition(@Param("workspaceId") Long workspaceId, @Param("statusId") Long statusId,
        @Param("position") Integer position);

    // 기본 상태를 새 상태로 바꿔 끼우기 (프로시저 sp_replace_default_task_status)
    void updateDefaultTaskStatus(@Param("workspaceId") Long workspaceId, @Param("oldStatusId") Long oldStatusId,
        @Param("newStatusId") Long newStatusId, @Param("wipLimit") Integer wipLimit);

    // 상태 삭제와 작업 이동 (프로시저 sp_delete_task_status)
    void deleteTaskStatus(@Param("workspaceId") Long workspaceId, @Param("statusId") Long statusId,
        @Param("targetStatusId") Long targetStatusId);

    // 제목으로 작업 검색 (sp_task_search, 번호·제목·상태만 조회)
    List<TaskVO> selectTaskSearchList(@Param("workspaceId") Long workspaceId, @Param("keyword") String keyword);

    // 여러 작업의 담당자 (taskIds가 비어 있으면 호출하지 않음)
    List<TaskAssigneeVO> selectTaskAssigneeListByTaskIds(@Param("taskIds") List<Long> taskIds);
}
