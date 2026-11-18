package com.flowspace.retrospective.service;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회고를 만들 때 복사해 둔 작업 하나예요 (sp_task_snapshot_list 결과).
@Getter
@Setter
@NoArgsConstructor
public class TaskSnapshotVO {

    private Long snapshotId;

    private Long retrospectiveId;

    private Long snapshotStatusId;

    // 상태 스냅샷의 이름 (완료 수를 셀 때 써요)
    private String statusName;

    private String title;

    private String priority;

    private BigDecimal position;
}
