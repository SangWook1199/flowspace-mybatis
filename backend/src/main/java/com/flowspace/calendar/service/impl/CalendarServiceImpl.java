package com.flowspace.calendar.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowspace.calendar.dto.response.CalendarItemResponse;
import com.flowspace.calendar.service.CalendarService;
import com.flowspace.calendar.service.CalendarTaskVO;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.task.service.TaskAssigneeVO;
import com.flowspace.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarServiceImpl implements CalendarService {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    // 캘린더·일정 SQL (같은 패키지라 import가 없어요)
    private final CalendarMapper calendarMapper;
    private final WorkspaceService workspaceService;

    // 캘린더 조회
    @Override
    public List<CalendarItemResponse> getCalendar(Long workspaceId, Integer year, Integer month, Long sprintId,
        String email) {

        // 워크스페이스가 있는지, 보는 사람이 멤버인지는 워크스페이스 서비스의 규칙을 그대로 써요
        workspaceService.getWorkspace(workspaceId, email);

        // 달·연도가 이상하면 500 대신 400으로 알려요.
        if (year == null || month == null || month < 1 || month > 12 || year < MIN_YEAR || year > MAX_YEAR) {
            throw new FlowSpaceException(ErrorCode.INVALID_CALENDAR_RANGE);
        }

        YearMonth yearMonth = YearMonth.of(year, month);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // null이면 백로그 작업을 보여줘요
        Long targetSprintId = resolveSprintId(workspaceId, sprintId);

        List<CalendarItemResponse> result = new ArrayList<>();

        List<CalendarTaskVO> tasks =
            calendarMapper.selectCalendarTaskList(workspaceId, targetSprintId, startDate, endDate);

        // 작업이 없으면 IN ()이 되어 SQL 오류가 나서 담당자 조회를 건너뛰어요
        if (!tasks.isEmpty()) {

            List<Long> taskIds = tasks.stream().map(CalendarTaskVO::getTaskId).toList();

            // 담당자는 작업마다 따로 읽지 않고 한 번에 읽어서 작업 번호별로 나눠 담아요.
            Map<Long, List<TaskAssigneeVO>> assigneesByTask = calendarMapper.selectCalendarAssigneeList(taskIds)
                .stream().collect(Collectors.groupingBy(TaskAssigneeVO::getTaskId));

            tasks.stream()
                .map(task -> CalendarItemResponse.from(task, assigneesByTask.getOrDefault(task.getTaskId(), List.of())))
                .forEach(result::add);
        }

        calendarMapper.selectCalendarEventList(workspaceId, startDateTime, endDateTime).stream()
            .map(CalendarItemResponse::from).forEach(result::add);

        result.sort(Comparator.comparing(item -> item.startDatetime()));

        return result;
    }

    // 기본 스프린트 결정 (없으면 null)
    private Long resolveSprintId(Long workspaceId, Long sprintId) {

        if (sprintId != null) {
            // 없는 스프린트이거나 다른 워크스페이스의 스프린트로는 조회할 수 없어요.
            if (!workspaceId.equals(calendarMapper.selectSprintWorkspaceId(sprintId))) {
                throw new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND);
            }

            return sprintId;
        }

        // 진행 중인 스프린트, 없으면 계획 중인 스프린트 (둘 다 없으면 null)
        return calendarMapper.selectDefaultSprintId(workspaceId);
    }
}
