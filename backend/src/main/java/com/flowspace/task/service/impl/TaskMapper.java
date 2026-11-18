package com.flowspace.task.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.task.service.SubTaskVO;
import com.flowspace.task.service.TaskAssigneeVO;
import com.flowspace.task.service.TaskVO;

// 작업 DB 접근. 메서드 이름이 Task_SQL.xml의 id와 같아야 해요.
@Mapper
public interface TaskMapper {

    // 칸반 작업 목록과 상태 이름 (프로시저 sp_task_list, sprintId가 null이면 백로그)
    List<TaskVO> selectTaskList(@Param("workspaceId") Long workspaceId, @Param("sprintId") Long sprintId);

    // 칸반 작업들의 담당자 (프로시저 sp_task_assignee_list)
    List<TaskAssigneeVO> selectTaskAssigneeList(@Param("workspaceId") Long workspaceId,
        @Param("sprintId") Long sprintId);

    // 여러 작업의 하위 작업 (taskIds가 비어 있으면 안 돼요)
    List<SubTaskVO> selectSubTaskList(@Param("taskIds") List<Long> taskIds);

    // 작업 한 건과 상태 이름 (프로시저 sp_task, 없으면 null)
    TaskVO selectTaskById(Long taskId);

    // 작업 한 건의 담당자 (프로시저 sp_task_assignee)
    List<TaskAssigneeVO> selectTaskAssigneeListByTaskId(Long taskId);

    // 워크스페이스에서 해당 상태인 작업 수 (새 작업을 그 상태의 맨 뒤에 놓을 때 써요)
    int selectTaskCountByStatus(@Param("workspaceId") Long workspaceId, @Param("statusId") Long statusId);

    // 워크스페이스에서 지금까지 쓴 가장 큰 작업 번호 (작업이 없으면 0)
    int selectMaxTaskNumber(Long workspaceId);

    // 작업 등록. 실행 후 DB가 만든 번호가 task의 taskId에 들어가요.
    int insertTask(TaskVO task);

    // 작업 담당자 등록
    int insertTaskAssignee(@Param("taskId") Long taskId, @Param("userId") Long userId);
}
