package com.flowspace.retrospective.service;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회고 생성 시점의 작업 스냅샷 (sp_task_snapshot_list 결과)
@Getter
@Setter
@NoArgsConstructor
public class TaskSnapshotVO {

    private Long snapshotId;

    private Long retrospectiveId;

    private Long snapshotStatusId;

    // 상태 스냅샷 이름 (완료 수 계산용)
    private String statusName;

    private String title;

    private String priority;

    private BigDecimal position;
}
