package com.flowspace.task.service;

import com.flowspace.entity.enums.TaskStatusCategory;
import com.flowspace.entity.enums.WorkspaceColor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 워크스페이스 작업 상태(칸반 열) (sp_task_status_list·sp_task_status 결과, 등록 공용)
// task_statuses + workspace_task_statuses
@Getter
@Setter
@NoArgsConstructor
public class TaskStatusVO {

    private Long workspaceId;

    private Long statusId;

    private String name;

    // TODO, IN_PROGRESS, DONE
    private TaskStatusCategory category;

    private WorkspaceColor color;

    // 칸반 열 순서
    private Integer position;

    // 공용 기본 상태 여부
    private Boolean isDefault;

    // 열에 둘 수 있는 작업 수 제한 (null이면 제한 없음)
    private Integer wipLimit;
}
