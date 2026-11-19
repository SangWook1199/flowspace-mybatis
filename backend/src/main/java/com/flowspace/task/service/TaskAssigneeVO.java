package com.flowspace.task.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 작업 담당자 (sp_task_assignee_list 결과, task_assignees·users·files 조인)
@Getter
@Setter
@NoArgsConstructor
public class TaskAssigneeVO {

    // 담당 작업 번호 (작업별 묶음용)
    private Long taskId;

    private Long userId;

    private String nickname;

    private Long profileFileId;

    private String profileImageUrl;
}
