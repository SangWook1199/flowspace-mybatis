package com.flowspace.retrospective.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회고 생성 시점의 작업 상태 스냅샷 (retrospective_status_snapshots 한 행)
@Getter
@Setter
@NoArgsConstructor
public class StatusSnapshotVO {

    private Long snapshotStatusId;

    private Long retrospectiveId;

    private String name;

    private String color;

    // 칸반 순서
    private Integer position;
}
