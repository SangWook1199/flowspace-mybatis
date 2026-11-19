package com.flowspace.workspace.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 멤버 담당 작업의 진행 중·완료 수 (sp_member_task_count 결과, 항상 1행)
@Getter
@Setter
@NoArgsConstructor
public class MemberTaskCountVO {

    private long openTaskCount;

    private long doneTaskCount;
}
