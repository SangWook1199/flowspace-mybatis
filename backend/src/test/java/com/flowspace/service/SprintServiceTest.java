package com.flowspace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.flowspace.activity.service.ActivityService;
import com.flowspace.entity.User;
import com.flowspace.entity.Workspace;
import com.flowspace.entity.WorkspaceMember;
import com.flowspace.entity.enums.ActivityTargetType;
import com.flowspace.entity.enums.ActivityType;
import com.flowspace.entity.enums.SprintStatus;
import com.flowspace.entity.enums.WorkspaceRole;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.notification.service.NotificationService;
import com.flowspace.repository.TaskRepository;
import com.flowspace.repository.UserRepository;
import com.flowspace.repository.WorkspaceMemberRepository;
import com.flowspace.repository.WorkspaceRepository;
import com.flowspace.retrospective.service.RetrospectiveService;
import com.flowspace.sprint.dto.request.SprintStatusUpdateRequest;
import com.flowspace.sprint.service.SprintProgressVO;
import com.flowspace.sprint.service.SprintVO;
import com.flowspace.sprint.service.impl.SprintMapper;
import com.flowspace.sprint.service.impl.SprintServiceImpl;

// 스프린트 완료 → 회고를 먼저 만들고 완료 프로시저, 회고를 못 만들면 완료하지 않음, 삭제는 관리자 이상만 가능한지 확인해요.
@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private ActivityService activityService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RetrospectiveService retrospectiveService;
    @Mock
    private SprintMapper sprintMapper;

    @InjectMocks
    private SprintServiceImpl sprintService;

    private User user;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1L).email("a@a.com").nickname("a").build();
        workspace = Workspace.builder().workspaceId(5L).name("w").initials("W").build();

        when(userRepository.findByEmail("a@a.com")).thenReturn(Optional.of(user));
    }

    // 스프린트 한 건 (MyBatis 조회 결과)
    private void sprintRow(SprintStatus status) {
        SprintVO row = new SprintVO();
        row.setSprintId(10L);
        row.setWorkspaceId(5L);
        row.setName("스프린트 1");
        row.setStatus(status);

        when(sprintMapper.selectSprintById(10L)).thenReturn(row);
        when(workspaceRepository.findById(5L)).thenReturn(Optional.of(workspace));

        // 상태 변경이 끝난 뒤 응답을 만들 때 쓰는 조회 결과 (실패하는 테스트에서는 쓰이지 않아요)
        SprintProgressVO progress = new SprintProgressVO();
        progress.setSprintId(10L);
        progress.setWorkspaceId(5L);
        progress.setName("스프린트 1");
        progress.setStatus(status);

        lenient().when(sprintMapper.selectSprintProgressById(10L)).thenReturn(progress);
    }

    private void memberWithRole(WorkspaceRole role) {
        when(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user))
            .thenReturn(Optional.of(WorkspaceMember.builder().workspace(workspace).user(user).role(role).build()));
    }

    @Test
    @DisplayName("스프린트를 완료하면 회고를 먼저 만들고, 그다음 완료 프로시저를 호출한다")
    void completingSprintCreatesRetrospective() {
        memberWithRole(WorkspaceRole.MEMBER);
        sprintRow(SprintStatus.ACTIVE);

        sprintService.updateStatus(10L, new SprintStatusUpdateRequest(SprintStatus.COMPLETED), "a@a.com");

        // 회고(스냅샷)를 먼저 만들고, 그다음 프로시저가 끝나지 않은 작업을 옮겨요
        // 스프린트 이름은 회고 페이지 제목("스프린트 1 회고")에 쓰여요
        InOrder order = inOrder(retrospectiveService, sprintMapper);
        order.verify(retrospectiveService).createRetrospective(10L, 5L, "스프린트 1", 1L);
        order.verify(sprintMapper).updateSprintComplete(10L);

        verify(activityService).log(eq(5L), eq(1L), eq(ActivityType.SPRINT_COMPLETED),
            eq(ActivityTargetType.SPRINT), eq(10L));
    }

    @Test
    @DisplayName("완료된 스프린트는 다시 진행 중으로 되돌릴 수 없다")
    void completedSprintCannotBeReactivated() {
        memberWithRole(WorkspaceRole.MEMBER);
        sprintRow(SprintStatus.COMPLETED);

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> sprintService.updateStatus(10L, new SprintStatusUpdateRequest(SprintStatus.ACTIVE), "a@a.com"));

        assertEquals(ErrorCode.INVALID_SPRINT_STATUS_CHANGE, e.getErrorCode());
        verify(sprintMapper, never()).updateSprintStart(any());
    }

    @Test
    @DisplayName("계획된 스프린트를 시작하면 시작 프로시저를 호출한다")
    void startingSprintCallsProcedure() {
        memberWithRole(WorkspaceRole.MEMBER);
        sprintRow(SprintStatus.PLANNING);

        sprintService.updateStatus(10L, new SprintStatusUpdateRequest(SprintStatus.ACTIVE), "a@a.com");

        // 진행 중이던 다른 스프린트를 계획됨으로 돌리는 일은 프로시저 안에서 해요
        verify(sprintMapper).updateSprintStart(10L);
    }

    @Test
    @DisplayName("시작하지 않은 스프린트는 바로 완료할 수 없다")
    void planningSprintCannotBeCompletedDirectly() {
        memberWithRole(WorkspaceRole.MEMBER);
        sprintRow(SprintStatus.PLANNING);

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> sprintService.updateStatus(10L, new SprintStatusUpdateRequest(SprintStatus.COMPLETED), "a@a.com"));

        assertEquals(ErrorCode.INVALID_SPRINT_STATUS_CHANGE, e.getErrorCode());
        verify(retrospectiveService, never()).createRetrospective(any(), any(), any(), any());
        verify(sprintMapper, never()).updateSprintComplete(any());
    }

    @Test
    @DisplayName("회고를 만들지 못하면(이미 있으면) 스프린트를 완료하지 않는다")
    void alreadyHasRetrospective() {
        memberWithRole(WorkspaceRole.MEMBER);
        sprintRow(SprintStatus.ACTIVE);

        // 회고가 이미 있는지는 회고 서비스가 확인해요 → 여기서는 거절했다고 가정해요
        doThrow(new FlowSpaceException(ErrorCode.RETROSPECTIVE_ALREADY_EXISTS)).when(retrospectiveService)
            .createRetrospective(10L, 5L, "스프린트 1", 1L);

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> sprintService.updateStatus(10L, new SprintStatusUpdateRequest(SprintStatus.COMPLETED), "a@a.com"));

        assertEquals(ErrorCode.RETROSPECTIVE_ALREADY_EXISTS, e.getErrorCode());
        verify(sprintMapper, never()).updateSprintComplete(any());
    }

    @Test
    @DisplayName("일반 멤버는 스프린트를 삭제할 수 없다")
    void memberCannotDeleteSprint() {
        memberWithRole(WorkspaceRole.MEMBER);
        sprintRow(SprintStatus.ACTIVE);

        FlowSpaceException e = assertThrows(FlowSpaceException.class, () -> sprintService.deleteSprint(10L, "a@a.com"));

        assertEquals(ErrorCode.ACCESS_DENIED, e.getErrorCode());
        verify(sprintMapper, never()).deleteSprint(any());
    }

    @Test
    @DisplayName("관리자는 스프린트를 삭제할 수 있다")
    void adminCanDeleteSprint() {
        memberWithRole(WorkspaceRole.ADMIN);
        sprintRow(SprintStatus.ACTIVE);

        sprintService.deleteSprint(10L, "a@a.com");

        verify(sprintMapper).deleteSprint(10L);
    }
}
