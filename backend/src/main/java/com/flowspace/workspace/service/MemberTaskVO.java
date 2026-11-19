package com.flowspace.workspace.service;

import java.time.LocalDate;

import com.flowspace.entity.enums.TaskPriority;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 프로필 카드의 멤버 담당 작업 (sp_member_open_task_list 결과)
@Getter
@Setter
@NoArgsConstructor
public class MemberTaskVO {

    private Long taskId;

    // 작업 화면 링크용 스프린트 번호 (null이면 백로그)
    private Long sprintId;

    private String title;

    private LocalDate endDate;

    private TaskPriority priority;

    private String statusName;
}
