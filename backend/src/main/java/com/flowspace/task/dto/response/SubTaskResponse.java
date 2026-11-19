package com.flowspace.task.dto.response;

import com.flowspace.task.service.SubTaskVO;

// @formatter:off

public record SubTaskResponse(

    Long subtaskId,

    Long taskId,

    Long assigneeId,    // 하위 작업 담당자(작업 담당자 중 한 명, 없으면 null)

    String content,

    Boolean isCompleted,

    Integer position

) {

    // SubTaskVO 기반 응답 생성
    public static SubTaskResponse from(SubTaskVO subTask) {
        return new SubTaskResponse(
            subTask.getSubtaskId(),
            subTask.getTaskId(),
            subTask.getAssigneeId(),
            subTask.getContent(),
            subTask.getIsCompleted(),
            subTask.getPosition()
        );
    }
}

// @formatter:on