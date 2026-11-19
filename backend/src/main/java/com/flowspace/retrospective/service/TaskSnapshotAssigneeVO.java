package com.flowspace.retrospective.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 작업 스냅샷 담당자 (sp_task_snapshot_assignee_list 결과, 닉네임은 회고 생성 시점 값)
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
