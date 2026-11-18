package com.flowspace.retrospective.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.flowspace.retrospective.service.SubTaskSnapshotVO;
import com.flowspace.retrospective.service.TaskSnapshotAssigneeVO;
import com.flowspace.retrospective.service.TaskSnapshotVO;
import com.flowspace.task.dto.response.AssigneeItem;

// @formatter:off

public record TaskSnapshotItem(

    Long snapshotId,
    Long snapshotStatusId,
    String title,
    List<AssigneeItem> assignees,
    List<SubTaskSnapshotItem> subtasks,
    String priority,
    BigDecimal position

) {

    public static TaskSnapshotItem from(
        TaskSnapshotVO snapshot,
        List<TaskSnapshotAssigneeVO> assignees,
        List<SubTaskSnapshotVO> subtasks
    ) {
        return new TaskSnapshotItem(
            snapshot.getSnapshotId(),
            snapshot.getSnapshotStatusId(),
            snapshot.getTitle(),
            assignees.stream()
                .map(assignee -> new AssigneeItem(
                    assignee.getOriginalUserId(),
                    assignee.getNickname(),
                    assignee.getProfileFileId(),
                    assignee.getProfileImageUrl()
                ))
                .toList(),
            subtasks.stream()
                .map(SubTaskSnapshotItem::from)
                .toList(),
            snapshot.getPriority(),
            snapshot.getPosition()
        );
    }
}

// @formatter:on
