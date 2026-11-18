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
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;
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

    // 회고 SQL (같은 패키지라 import가 없어요)
    private final RetrospectiveMapper retrospectiveMapper;
    private final WorkspaceService workspaceService;
    private final ActivityService activityService;

    // 회고 페이지 내용(블록)은 아직 JPA인 페이지 서비스가 보여줘요
    private final PageService pageService;

    // 회고 단건 조회
    @Override
    public RetrospectiveResponse getRetrospective(Long retrospectiveId, String email) {

        RetrospectiveVO retrospective = retrospectiveMapper.selectRetrospectiveById(retrospectiveId);

        if (retrospective == null) {
            throw new FlowSpaceException(ErrorCode.RETROSPECTIVE_NOT_FOUND);
        }

        Long workspaceId = retrospective.getWorkspaceId();

        // 워크스페이스 멤버인지는 워크스페이스 서비스의 규칙을 그대로 써요
        workspaceService.getWorkspace(workspaceId, email);

        List<TaskSnapshotVO> snapshots = retrospectiveMapper.selectTaskSnapshotList(workspaceId, retrospectiveId);

        // 담당자·하위 작업은 스냅샷마다 따로 읽지 않고 한 번에 읽어서 스냅샷 번호별로 나눠 담아요.
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

        // 스프린트마다 한 행이에요. 회고가 없는 스프린트는 회고 칸이 비어 있어요.
        List<RetrospectiveVO> rows = retrospectiveMapper.selectRetrospectiveList(workspaceId);

        if (rows.isEmpty()) {
            return List.of();
        }

        // 스프린트마다 따로 읽지 않고, 워크스페이스의 스냅샷·담당자를 한 번에 읽어요(회고 번호 자리에 null).
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
            // 스프린트가 없는 것과, 스프린트는 있지만 회고가 아직 없는 것을 나눠서 알려줘요
            if (retrospectiveMapper.selectSprintCount(sprintId) == 0) {
                throw new FlowSpaceException(ErrorCode.SPRINT_NOT_FOUND);
            }

            throw new FlowSpaceException(ErrorCode.RETROSPECTIVE_NOT_FOUND);
        }

        return getRetrospective(retrospectiveId, email);
    }

    // 회고 만들기: 페이지·KPT 표·스냅샷은 프로시저 한 번으로 만들고, 활동 기록은 자바에서 남겨요
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

        // 프로시저가 OUT 인자로 돌려준 회고 페이지 번호가 pageId에 들어 있어요
        activityService.log(workspaceId, userId, ActivityType.PAGE_CREATED, ActivityTargetType.PAGE,
            retrospective.getPageId());
    }

    // 회고 페이지들의 미완료 TODO 블록 수 (Action Item), 페이지 번호별로
    private Map<Long, Integer> countActionItemsByPage(List<Long> pageIds) {

        Map<Long, Integer> result = new HashMap<>();

        // 목록이 비면 IN ()이 되어 SQL 오류가 나서 여기서 끝내요
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

    // TODO 블록이 체크됐는지 (형식이 올바르지 않은 content는 미완료로 계산해요)
    private boolean isChecked(String content) {

        try {
            JsonNode node = content == null ? null : OBJECT_MAPPER.readTree(content);
            return node != null && node.path("checked").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    // 회고 스냅샷에는 상태의 이름만 남아 있어서 이름으로 "완료"를 알아봐요.
    // 기본 상태 이름이 DONE에서 "완료"로 바뀌었고, 예전에 만든 회고에는 DONE이 그대로 남아 있어서 둘 다 봐요.
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

        // 참여자: 담당자를 한 번씩만 (처음 나온 순서대로)
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
