package com.flowspace.task.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.flowspace.common.enums.TaskPriority;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 작업 + 상태 이름 (sp_task_list·sp_task 결과, 등록 공용)
// 워크스페이스·스프린트·상태는 번호로 보관
@Getter
@Setter
@NoArgsConstructor
public class TaskVO {

    private Long taskId;

    private Integer taskNumber;

    private Long workspaceId;

    private Long sprintId;

    private Long statusId;

    // 작업을 만든 사람의 번호
    private Long createdBy;

    // task_statuses 조인 컬럼
    private String statusName;

    private BigDecimal position;

    private String title;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private TaskPriority priority;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
}
