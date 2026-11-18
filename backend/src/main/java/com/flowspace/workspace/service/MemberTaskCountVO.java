package com.flowspace.workspace.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 멤버가 맡은 작업의 진행 중·완료 수예요(프로시저 sp_member_task_count의 결과, 항상 1행).
@Getter
@Setter
@NoArgsConstructor
public class MemberTaskCountVO {

    private long openTaskCount;

    private long doneTaskCount;
}
