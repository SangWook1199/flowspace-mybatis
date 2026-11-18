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
}
