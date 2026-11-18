package com.flowspace.retrospective.dto.response;

import com.flowspace.retrospective.service.SubTaskSnapshotVO;

// @formatter:off

public record SubTaskSnapshotItem(

    Long subTaskSnapshotId,
    String content,
    Boolean isCompleted,
    String assigneeName,
    Long assigneeProfileFileId,
    String assigneeProfileImageUrl,
    Integer position

) {

    // 담당자 사진 주소는 프로시저가 files와 조인해서 넣어 줘요
    public static SubTaskSnapshotItem from(SubTaskSnapshotVO subtask) {
        return new SubTaskSnapshotItem(
            subtask.getSubTaskSnapshotId(),
            subtask.getContent(),
            subtask.getIsCompleted(),
            subtask.getAssigneeName(),
            subtask.getAssigneeProfileFileId(),
            subtask.getAssigneeProfileImageUrl(),
            subtask.getPosition()
        );
    }
}

// @formatter:on
