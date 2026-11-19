package com.flowspace.task.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 하위 작업 (subtasks 테이블 한 행)
@Getter
@Setter
@NoArgsConstructor
public class SubTaskVO {

    private Long subtaskId;

    private Long taskId;

    private Long assigneeId;

    private String content;

    // Boolean 타입 사용 (is_completed → setIsCompleted 매핑)
    private Boolean isCompleted;

    private Integer position;
}
