package com.flowspace.sprint.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 스프린트와 진행 상황을 함께 담는 객체예요(프로시저 sp_sprint_progress_list, sp_sprint_progress의 결과).
// sprints 테이블 한 행이 아니라 "조회 결과"에 맞춘 모양이에요: 작업 수와 완료 작업 수는 tasks를 집계한 값이에요.
@Getter
@Setter
@NoArgsConstructor
public class SprintProgressVO extends SprintVO {

    // 스프린트에 속한 작업 수
    private int taskCount;

    // 그중 완료(DONE) 상태인 작업 수
    private int completedTaskCount;
}
