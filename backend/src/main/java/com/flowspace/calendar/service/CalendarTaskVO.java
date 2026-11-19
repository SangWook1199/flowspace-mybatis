package com.flowspace.calendar.service;

import java.time.LocalDate;

import com.flowspace.common.enums.WorkspaceColor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 캘린더 작업 (sp_calendar_task_list 결과, 작업 상태 색 포함)
@Getter
@Setter
@NoArgsConstructor
public class CalendarTaskVO {

    private Long taskId;

    // 백로그 작업이면 null
    private Long sprintId;

    private Long statusId;

    // 작업 상태 색 (캘린더 표시 색)
    private WorkspaceColor statusColor;

    private String title;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;
}
