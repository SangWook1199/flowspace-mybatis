package com.flowspace.sprint.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.flowspace.entity.enums.SprintStatus;
import com.flowspace.entity.enums.WorkspaceColor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 스프린트 (sprints 테이블 한 행, 등록·수정·조회 공용)
// 워크스페이스·작성자는 번호로 보관
@Getter
@Setter
@NoArgsConstructor
public class SprintVO {

    private Long sprintId;

    private Long workspaceId;

    private Long createdBy;

    private String name;

    private String goal;

    private String description;

    private WorkspaceColor color;

    private LocalDate startDate;

    private LocalDate endDate;

    private SprintStatus status;

    private LocalDateTime createdAt;
}
