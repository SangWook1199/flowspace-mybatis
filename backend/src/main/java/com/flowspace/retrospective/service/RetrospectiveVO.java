package com.flowspace.retrospective.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.flowspace.entity.enums.SprintStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회고 + 스프린트 정보 (sp_retrospective_list·sp_retrospective 결과)
// 목록에서 회고가 없는 스프린트는 retrospectiveId·pageId가 null
@Getter
@Setter
@NoArgsConstructor
public class RetrospectiveVO {

    private Long retrospectiveId;

    // 회고 페이지
    private Long pageId;

    private LocalDateTime createdAt;

    // ↓ 스프린트 정보
    private Long sprintId;

    private Long workspaceId;

    private String sprintName;

    private SprintStatus sprintStatus;

    private LocalDate startDate;

    private LocalDate endDate;

    private String goal;

    // ↓ 회고 생성 전용 (sp_create_retrospective 인자, pageId는 OUT으로 설정)
    private Long createdBy;

    private String pageTitle;

    private String pageIcon;

    private String tableTitle;
}
