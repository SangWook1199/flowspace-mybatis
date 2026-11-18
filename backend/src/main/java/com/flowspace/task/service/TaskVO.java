package com.flowspace.task.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.flowspace.entity.enums.TaskPriority;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 작업 한 건과 상태 이름을 담는 객체예요(프로시저 sp_task_list의 결과).
// 워크스페이스·스프린트·상태는 객체가 아니라 번호로 가져요.
@Getter
@Setter
@NoArgsConstructor
public class TaskVO {

    private Long taskId;

    private Integer taskNumber;

    private Long workspaceId;

    private Long sprintId;

    private Long statusId;

    // task_statuses 테이블에서 조인해 온 값이에요
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
