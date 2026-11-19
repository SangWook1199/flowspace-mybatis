package com.flowspace.task.dto.response;

import com.flowspace.task.service.TaskAssigneeVO;

// @formatter:off

public record AssigneeItem(

    Long userId,
    String name,
    Long profileFileId,
    String profileImageUrl

) {

    // TaskAssigneeVO 기반 생성
    public static AssigneeItem from(TaskAssigneeVO assignee) {
        return new AssigneeItem(
            assignee.getUserId(),
            assignee.getNickname(),
            assignee.getProfileFileId(),
            assignee.getProfileImageUrl()
        );
    }
}

// @formatter:on