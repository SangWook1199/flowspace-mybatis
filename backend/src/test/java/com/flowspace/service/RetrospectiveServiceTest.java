package com.flowspace.service;

import com.flowspace.page.service.PageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.flowspace.activity.service.ActivityService;
import com.flowspace.common.enums.ActivityTargetType;
import com.flowspace.common.enums.ActivityType;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.retrospective.service.RetrospectiveVO;
import com.flowspace.retrospective.service.impl.RetrospectiveMapper;
import com.flowspace.retrospective.service.impl.RetrospectiveServiceImpl;
import com.flowspace.workspace.service.WorkspaceService;

// 회고 생성: 프로시저 인자, OUT 페이지 번호로 활동 기록, 중복 생성 거절
@ExtendWith(MockitoExtension.class)
class RetrospectiveServiceTest {

    @Mock
    private RetrospectiveMapper retrospectiveMapper;
    @Mock
    private WorkspaceService workspaceService;
    @Mock
    private ActivityService activityService;
    @Mock
    private PageService pageService;

    @InjectMocks
    private RetrospectiveServiceImpl retrospectiveService;

    @Test
    @DisplayName("회고를 만들면 프로시저에 제목을 넘기고, 돌려받은 페이지 번호로 활동 기록을 남긴다")
    void createRetrospective() {
        when(retrospectiveMapper.selectRetrospectiveIdBySprint(10L)).thenReturn(null);

        doAnswer(invocation -> {
            RetrospectiveVO vo = invocation.getArgument(0);
            vo.setPageId(77L);
            return null;
        }).when(retrospectiveMapper).insertRetrospective(any());

        retrospectiveService.createRetrospective(10L, 5L, "스프린트 1", 1L);

        ArgumentCaptor<RetrospectiveVO> captor = ArgumentCaptor.forClass(RetrospectiveVO.class);
        verify(retrospectiveMapper).insertRetrospective(captor.capture());
        assertEquals("스프린트 1 회고", captor.getValue().getPageTitle());
        assertEquals(10L, captor.getValue().getSprintId());
        assertEquals(1L, captor.getValue().getCreatedBy());

        verify(activityService).log(5L, 1L, ActivityType.PAGE_CREATED, ActivityTargetType.PAGE, 77L);
    }

    @Test
    @DisplayName("이미 회고가 있는 스프린트에는 다시 만들지 않는다")
    void alreadyHasRetrospective() {
        when(retrospectiveMapper.selectRetrospectiveIdBySprint(10L)).thenReturn(3L);

        FlowSpaceException e = assertThrows(FlowSpaceException.class,
            () -> retrospectiveService.createRetrospective(10L, 5L, "스프린트 1", 1L));

        assertEquals(ErrorCode.RETROSPECTIVE_ALREADY_EXISTS, e.getErrorCode());
        verify(retrospectiveMapper, never()).insertRetrospective(any());
    }
}
