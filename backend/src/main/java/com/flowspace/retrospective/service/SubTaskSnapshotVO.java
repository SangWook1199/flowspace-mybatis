package com.flowspace.retrospective.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 작업 스냅샷의 하위 작업 하나예요 (sp_subtask_snapshot_list 결과).
@Getter
@Setter
@NoArgsConstructor
public class SubTaskSnapshotVO {

    private Long subTaskSnapshotId;

    private Long snapshotId;

    private String content;

    private Boolean isCompleted;

    private String assigneeName;

    private Long assigneeProfileFileId;

    private String assigneeProfileImageUrl;

    private Integer position;
}
