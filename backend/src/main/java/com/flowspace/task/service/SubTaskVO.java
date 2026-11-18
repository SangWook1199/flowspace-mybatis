package com.flowspace.task.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// subtasks 테이블의 한 행을 담는 객체예요.
@Getter
@Setter
@NoArgsConstructor
public class SubTaskVO {

    private Long subtaskId;

    private Long taskId;

    private Long assigneeId;

    private String content;

    // boolean이 아니라 Boolean이어야 MyBatis가 is_completed를 setIsCompleted로 채워요
    private Boolean isCompleted;

    private Integer position;
}
