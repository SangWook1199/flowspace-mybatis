package com.flowspace.retrospective.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowspace.activity.service.ActivityService;
import com.flowspace.page.dto.response.PageDetailResponse;
import com.flowspace.common.enums.ActivityTargetType;
import com.flowspace.common.enums.ActivityType;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.retrospective.dto.response.ParticipantItem;
import com.flowspace.retrospective.dto.response.RetrospectiveListItem;
import com.flowspace.retrospective.dto.response.RetrospectiveResponse;
import com.flowspace.retrospective.dto.response.RetrospectiveSummary;
import com.flowspace.retrospective.dto.response.StatusSnapshotItem;
import com.flowspace.retrospective.dto.response.TaskSnapshotItem;
import com.flowspace.retrospective.service.RetrospectiveService;
import com.flowspace.retrospective.service.RetrospectiveVO;
import com.flowspace.retrospective.service.SubTaskSnapshotVO;
import com.flowspace.retrospective.service.TaskSnapshotAssigneeVO;
import com.flowspace.retrospective.service.TaskSnapshotVO;
import com.flowspace.retrospective.service.TodoBlockVO;
import com.flowspace.page.service.PageService;
import com.flowspace.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RetrospectiveServiceImpl implements RetrospectiveService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 회고 SQL
    private final RetrospectiveMapper retrospectiveMapper;
    private final WorkspaceService workspaceService;
    private final ActivityService activityService;

    // 회고 페이지 내용 조회용
    private final PageService pageService;

    // 회고 단건 조회
    @Override
    public RetrospectiveResponse getRetrospective(Long retrospectiveId, String email) {

        RetrospectiveVO retrospective = retrospectiveMapper.selectRetrospectiveById(retrospectiveId);

        if (retrospective == null) {
            throw new FlowSpaceException(ErrorCode.RETROSPECTIVE_NOT_FOUND);
        }

        Long workspaceId = retrospective.getWorkspaceId();

        workspaceService.getWorkspace(workspaceId, email);

        List<TaskSnapshotVO> snapshots = retrospectiveMapper.selectTaskSnapshotList(workspaceId, retrospectiveId);

        Map<Long, List<TaskSnapshotAssigneeVO>> assigneesBySnapshot = retrospectiveMapper
            .selectTaskSnapshotAssigneeList(workspaceId, retrospectiveId).stream()
            .collect(Collectors.groupingBy(TaskSnapshotAssigneeVO::getSnapshotId));

        Map<Long, List<SubTaskSnapshotVO>> subtasksBySnapshot = retrospectiveMapper
            .selectSubTaskSnapshotList(retrospectiveId).stream()
            .collect(Collectors.groupingBy(SubTaskSnapshotVO::getSnapshotId));

        RetrospectiveSummary summary = createSummary(snapshots, assigneesBySnapshot);

        PageDetailResponse page = pageService.getPageDetail(retrospective.getPageId(), email);

        List<TaskSnapshotItem> taskItems = snapshots.stream()
            .map(snapshot -> TaskSnapshotItem.from(snapshot,
                assigneesBySnapshot.getOrDefault(snapshot.getSnapshotId(), List.of()),
                subtasksBySnapshot.getOrDefault(snapshot.getSnapshotId(), List.of())))
            .toList();

        List<StatusSnapshotItem> statuses = retrospectiveMapper.selectStatusSnapshotList(retrospectiveId).stream()
            .map(StatusSnapshotItem::from).toList();

        return RetrospectiveResponse.from(retrospective, summary, statuses, taskItems, page);
    }

    // 워크스페이스 회고 목록 조회 (회고가 아직 없는 스프린트도 함께 반환)
    @Override
    public List<RetrospectiveListItem> getRetrospectives(Long workspaceId, String email) {

        workspaceService.getWorkspace(workspaceId, email);

        List<RetrospectiveVO> rows = retrospectiveMapper.selectRetrospectiveList(workspaceId);

        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, List<TaskSnapshotVO>> snapshotsByRetrospective = retrospectiveMapper
            .selectTaskSnapshotList(workspaceId, null).stream()
            .collect(Collectors.groupingBy(TaskSnapshotVO::getRetrospectiveId));

        Map<Long, List<TaskSnapshotAssigneeVO>> assigneesBySnapshot = retrospectiveMapper
            .selectTaskSnapshotAssigneeList(workspaceId, null).stream()
            .collect(Collectors.groupingBy(TaskSnapshotAssigneeVO::getSnapshotId));

        Map<Long, Integer> actionItemsByPage = countActionItemsByPage(
            rows.stream().map(RetrospectiveVO::getPageId).filter(Objects::nonNull).toList());

        return rows.stream().map(row -> {

            if (row.getRetrospectiveId() == null) {
                return RetrospectiveListItem.withoutRetrospective(row);
            }

            List<TaskSnapshotVO> snapshots = snapshotsByRetrospective.getOrDefault(row.getRetrospectiveId(),
                List.of());

            return RetrospectiveListItem.of(row, createSummary(snapshots, assigneesBySnapshot),
                actionItemsByPage.getOrDefault(row.getPageId(), 0));
        }).toList();
    }

    // 스프린트 회고 조회
    @Override
    public RetrospectiveResponse getBySprint(Long sprintId, String email) {

        Long retrospectiveId = retrospectiveMapper.selectRetrospectiveIdBySprint(sprintId);

        if (retrospectiveId == null) {
            if (retrospectiveMapper.selectSprintCount(sprintId) == 0) {
                throw new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND);
            }

            throw new FlowSpaceException(ErrorCode.RETROSPECTIVE_NOT_FOUND);
        }

        return getRetrospective(retrospectiveId, email);
    }

    // 회고 생성: 페이지·KPT 표·스냅샷은 프로시저, 활동 기록은 Java
    @Transactional
    @Override
    public void createRetrospective(Long sprintId, Long workspaceId, String sprintName, Long userId) {

        if (retrospectiveMapper.selectRetrospectiveIdBySprint(sprintId) != null) {
            throw new FlowSpaceException(ErrorCode.RETROSPECTIVE_ALREADY_EXISTS);
        }

        RetrospectiveVO retrospective = new RetrospectiveVO();
        retrospective.setSprintId(sprintId);
        retrospective.setWorkspaceId(workspaceId);
        retrospective.setCreatedBy(userId);
        retrospective.setPageTitle(sprintName + " 회고");
        retrospective.setPageIcon("🚀");
        retrospective.setTableTitle("회고");

        retrospectiveMapper.insertRetrospective(retrospective);

        activityService.log(workspaceId, userId, ActivityType.PAGE_CREATED, ActivityTargetType.PAGE,
            retrospective.getPageId());
    }

    // 회고 페이지들의 미완료 TODO 블록 수 (Action Item), 페이지 번호별로
    private Map<Long, Integer> countActionItemsByPage(List<Long> pageIds) {

        Map<Long, Integer> result = new HashMap<>();

        if (pageIds.isEmpty()) {
            return result;
        }

        for (TodoBlockVO block : retrospectiveMapper.selectTodoBlockList(pageIds)) {

            if (!isChecked(block.getContent())) {
                result.merge(block.getPageId(), 1, Integer::sum);
            }
        }

        return result;
    }

    // TODO 블록 체크 여부 (형식 오류는 미완료)
    private boolean isChecked(String content) {

        try {
            JsonNode node = content == null ? null : OBJECT_MAPPER.readTree(content);
            return node != null && node.path("checked").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    // 완료 상태 이름 확인 (스냅샷에는 상태 이름만 저장, 예전 회고의 DONE과 현재 "완료" 모두 인정)
    private static boolean isDoneStatusName(String name) {
        return "DONE".equals(name) || "완료".equals(name);
    }

    // 회고 요약 생성
    private RetrospectiveSummary createSummary(List<TaskSnapshotVO> snapshots,
        Map<Long, List<TaskSnapshotAssigneeVO>> assigneesBySnapshot) {

        int total = snapshots.size();

        int completed = (int) snapshots.stream().filter(s -> isDoneStatusName(s.getStatusName())).count();

        int incomplete = total - completed;

        int completionRate = total == 0 ? 0 : Math.round((completed * 100f) / total);

        Map<Long, ParticipantItem> participantMap = new LinkedHashMap<>();

        for (TaskSnapshotVO snapshot : snapshots) {

            for (TaskSnapshotAssigneeVO assignee : assigneesBySnapshot.getOrDefault(snapshot.getSnapshotId(),
                List.of())) {

                if (assignee.getOriginalUserId() == null) {
                    continue;
                }

                participantMap.putIfAbsent(assignee.getOriginalUserId(),
                    new ParticipantItem(assignee.getOriginalUserId(), assignee.getNickname(),
                        assignee.getProfileFileId(), assignee.getProfileImageUrl()));
            }
        }

        return new RetrospectiveSummary(completionRate, completed, incomplete, total,
            new ArrayList<>(participantMap.values()));
    }
}
