package com.flowspace.sprint.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 스프린트 + 진행 상황 (sp_sprint_progress_list·sp_sprint_progress 결과)
// 작업 수·완료 작업 수는 tasks 집계 값
@Getter
@Setter
@NoArgsConstructor
public class SprintProgressVO extends SprintVO {

    // 스프린트에 속한 작업 수
    private int taskCount;

    // 그중 완료(DONE) 상태인 작업 수
    private int completedTaskCount;
}
