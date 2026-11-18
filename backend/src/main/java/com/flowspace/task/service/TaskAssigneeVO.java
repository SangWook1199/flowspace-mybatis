package com.flowspace.task.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 작업 담당자 한 명을 담는 객체예요(프로시저 sp_task_assignee_list의 결과).
// task_assignees, users, files 세 테이블을 조인한 결과라 어느 한 테이블의 모양과도 같지 않아요.
@Getter
@Setter
@NoArgsConstructor
public class TaskAssigneeVO {

    // 어느 작업의 담당자인지 (작업별로 나눠 담을 때 써요)
    private Long taskId;

    private Long userId;

    private String nickname;

    private Long profileFileId;

    private String profileImageUrl;
}
