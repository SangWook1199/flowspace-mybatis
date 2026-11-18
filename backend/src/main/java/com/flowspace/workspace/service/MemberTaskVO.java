package com.flowspace.workspace.service;

import java.time.LocalDate;

import com.flowspace.entity.enums.TaskPriority;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 프로필 카드에 보여 줄, 멤버가 맡은 작업 한 줄이에요(프로시저 sp_member_open_task_list의 결과).
@Getter
@Setter
@NoArgsConstructor
public class MemberTaskVO {

    private Long taskId;

    // 작업 화면 링크를 만들 때 써요 (null이면 백로그)
    private Long sprintId;

    private String title;

    private LocalDate endDate;

    private TaskPriority priority;

    private String statusName;
}
