package com.flowspace.retrospective.dto.response;

import com.flowspace.retrospective.service.StatusSnapshotVO;

// @formatter:off

public record StatusSnapshotItem(

    Long snapshotStatusId,
    String name,
    String color,
    Integer position

) {

    public static StatusSnapshotItem from(StatusSnapshotVO status) {
        return new StatusSnapshotItem(
            status.getSnapshotStatusId(),
            status.getName(),
            status.getColor(),
            status.getPosition()
        );
    }
}

// @formatter:on
