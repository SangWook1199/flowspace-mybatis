package com.flowspace.retrospective.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 작업 스냅샷의 담당자 한 명이에요 (sp_task_snapshot_assignee_list 결과). 닉네임은 회고를 만들 때의 이름이에요.
@Getter
@Setter
@NoArgsConstructor
public class TaskSnapshotAssigneeVO {

    private Long snapshotId;

    // 원래 사용자 번호 (탈퇴해서 지워졌으면 null)
    private Long originalUserId;

    private String nickname;

    private Long profileFileId;

    private String profileImageUrl;
}
