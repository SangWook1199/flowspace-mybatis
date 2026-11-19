package com.flowspace.task.dto.response;

import com.flowspace.task.service.TaskStatusVO;
import com.flowspace.common.enums.TaskStatusCategory;
import com.flowspace.common.enums.WorkspaceColor;

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