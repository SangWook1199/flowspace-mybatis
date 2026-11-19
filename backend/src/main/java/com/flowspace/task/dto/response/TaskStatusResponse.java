package com.flowspace.task.dto.response;

import com.flowspace.entity.WorkspaceTaskStatus;
import com.flowspace.task.service.TaskStatusVO;
import com.flowspace.entity.enums.TaskStatusCategory;
import com.flowspace.entity.enums.WorkspaceColor;

// @formatter:off

public record TaskStatusResponse(

    Long statusId,
    String name,
    TaskStatusCategory category,
    WorkspaceColor color,
    Integer position,

    Boolean isDefault,
    Integer wipLimit

) {

    public static TaskStatusResponse from(WorkspaceTaskStatus mapping) {
        return new TaskStatusResponse(
            mapping.getTaskStatus().getStatusId(),
            mapping.getTaskStatus().getName(),
            mapping.getTaskStatus().getCategory(),
            mapping.getTaskStatus().getColor(),
            mapping.getPosition(),
            mapping.getIsDefault(),
            mapping.getWipLimit()
        );
    }

    // TaskStatusVO 기반 응답 생성
    public static TaskStatusResponse from(TaskStatusVO status) {
        return new TaskStatusResponse(
            status.getStatusId(),
            status.getName(),
            status.getCategory(),
            status.getColor(),
            status.getPosition(),
            status.getIsDefault(),
            status.getWipLimit()
        );
    }
}

// @formatter:on