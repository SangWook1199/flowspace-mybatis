package com.flowspace.sprint.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.flowspace.entity.enums.SprintStatus;
import com.flowspace.entity.enums.WorkspaceColor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 스프린트 조회 결과를 담는 객체예요(MyBatis가 sprints 테이블의 행을 여기에 채워 줘요).
// 엔티티와 달리 연관 객체를 들고 있지 않고, 워크스페이스·작성자는 번호(ID)만 가져요.
@Getter
@Setter
@NoArgsConstructor
public class SprintVO {

    private Long sprintId;

    private Long workspaceId;

    private Long createdBy;

    private String name;

    private String goal;

    private String description;

    private WorkspaceColor color;

    private LocalDate startDate;

    private LocalDate endDate;

    private SprintStatus status;

    private LocalDateTime createdAt;
}
