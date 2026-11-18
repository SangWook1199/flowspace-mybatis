package com.flowspace.retrospective.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.flowspace.entity.enums.SprintStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회고 한 건과 그 스프린트 정보예요 (sp_retrospective_list, sp_retrospective 결과).
// 목록에서는 회고가 아직 없는 스프린트도 한 행으로 나와서, 그때는 retrospectiveId·pageId가 null이에요.
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

    // ↓ 회고를 만들 때만 써요 (sp_create_retrospective의 인자, pageId는 프로시저가 채워 줘요)
    private Long createdBy;

    private String pageTitle;

    private String pageIcon;

    private String tableTitle;
}
