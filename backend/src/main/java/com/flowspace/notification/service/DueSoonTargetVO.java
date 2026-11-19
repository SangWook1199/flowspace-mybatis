package com.flowspace.notification.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 마감 임박 알림 대상 (담당자, 작업) (sp_due_soon_target_list 결과)
@Getter
@Setter
@NoArgsConstructor
public class DueSoonTargetVO {

    // 받을 담당자
    private Long userId;

    private Long taskId;

    private Long workspaceId;

    // 작업 화면 링크용 스프린트 번호 (null이면 백로그)
    private Long sprintId;

    private String title;
}
