package com.flowspace.calendar.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.calendar.service.CalendarTaskVO;
import com.flowspace.calendar.service.EventVO;
import com.flowspace.task.service.TaskAssigneeVO;

// 캘린더·일정 Mapper (Calendar_SQL.xml)
@Mapper
public interface CalendarMapper {

    // 이메일로 사용자 번호 조회
    Long selectUserIdByEmail(String email);

    // ---------- 일정 ----------

    // 일정 등록 (생성된 번호는 eventId에 설정)
    int insertEvent(EventVO event);

    // 일정 한 건과 만든 사람 이름 (프로시저 sp_event, 없으면 null)
    EventVO selectEventById(Long eventId);

    // 일정 수정
    int updateEvent(EventVO event);

    // 일정 삭제
    int deleteEvent(Long eventId);

    // 제목으로 일정 검색 (keyword는 %, _ 이스케이프 후 전달)
    List<EventVO> selectEventSearchList(@Param("workspaceId") Long workspaceId, @Param("keyword") String keyword);

    // ---------- 캘린더 ----------

    // 스프린트가 속한 워크스페이스 번호 (없는 스프린트면 null)
    Long selectSprintWorkspaceId(Long sprintId);

    // 기본으로 보여 줄 스프린트: 진행 중 → 계획 중 순서로, 시작일이 빠른 것 (없으면 null)
    Long selectDefaultSprintId(Long workspaceId);

    // 그 달에 시작하는 작업 (프로시저 sp_calendar_task_list, sprintId가 null이면 백로그)
    List<CalendarTaskVO> selectCalendarTaskList(@Param("workspaceId") Long workspaceId,
        @Param("sprintId") Long sprintId, @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    // 여러 작업의 담당자 (taskIds가 비어 있으면 호출하지 않음)
    List<TaskAssigneeVO> selectCalendarAssigneeList(@Param("taskIds") List<Long> taskIds);

    // 그 달에 시작하는 일정
    List<EventVO> selectCalendarEventList(@Param("workspaceId") Long workspaceId,
        @Param("startDatetime") LocalDateTime startDatetime, @Param("endDatetime") LocalDateTime endDatetime);
}
