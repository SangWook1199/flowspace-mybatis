package com.flowspace.task.service;

import com.flowspace.entity.enums.TaskStatusCategory;
import com.flowspace.entity.enums.WorkspaceColor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 워크스페이스의 작업 상태(칸반 열) 하나를 담는 객체예요(프로시저 sp_task_status_list, sp_task_status의 결과).
// 상태 정보(task_statuses)와 워크스페이스별 설정(workspace_task_statuses)을 합친 모양이에요. 등록할 때도 써요.
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

    // 모든 워크스페이스가 같이 쓰는 기본 상태인지
    private Boolean isDefault;

    // 열에 둘 수 있는 작업 수 제한 (null이면 제한 없음)
    private Integer wipLimit;
}
