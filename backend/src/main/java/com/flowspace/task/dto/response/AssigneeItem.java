package com.flowspace.task.dto.response;

import com.flowspace.entity.TaskAssignee;
import com.flowspace.task.service.TaskAssigneeVO;

// @formatter:off

public record AssigneeItem(

    Long userId,
    String name,
    Long profileFileId,
    String profileImageUrl

) {

    public static AssigneeItem from(TaskAssignee assignee) {
        return new AssigneeItem(
            assignee.getUser().getUserId(),
            assignee.getUser().getNickname(),
            assignee.getUser().getProfileFile() == null
                ? null
                : assignee.getUser().getProfileFile().getFileId(),
            assignee.getUser().getProfileFile() == null
                ? null
                : assignee.getUser().getProfileFile().getFileUrl()
        );
    }

    // MyBatis 조회 결과(TaskAssigneeVO)로 만들어요 (조인으로 이름·이미지가 이미 들어 있어요)
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