package com.flowspace.calendar.service;

import java.time.LocalDate;

import com.flowspace.entity.enums.WorkspaceColor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 캘린더에 보여 줄 작업 한 건이에요 (sp_calendar_task_list 결과).
// 칸반용 TaskVO보다 컬럼이 적고, 작업 상태의 색이 들어 있어요.
@Getter
@Setter
@NoArgsConstructor
public class CalendarTaskVO {

    private Long taskId;

    // 백로그 작업이면 null
    private Long sprintId;

    private Long statusId;

    // 작업 상태의 색 (캘린더 칸의 색이 돼요)
    private WorkspaceColor statusColor;

    private String title;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;
}
