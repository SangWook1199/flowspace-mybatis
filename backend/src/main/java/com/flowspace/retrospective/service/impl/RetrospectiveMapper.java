package com.flowspace.retrospective.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.retrospective.service.RetrospectiveVO;
import com.flowspace.retrospective.service.StatusSnapshotVO;
import com.flowspace.retrospective.service.SubTaskSnapshotVO;
import com.flowspace.retrospective.service.TaskSnapshotAssigneeVO;
import com.flowspace.retrospective.service.TaskSnapshotVO;
import com.flowspace.retrospective.service.TodoBlockVO;

// 회고 Mapper (Retrospective_SQL.xml)
@Mapper
public interface RetrospectiveMapper {

    // ---------- 회고 ----------

    // 워크스페이스의 스프린트와 회고 (프로시저 sp_retrospective_list)
    List<RetrospectiveVO> selectRetrospectiveList(Long workspaceId);

    // 회고 한 건과 스프린트 정보 (프로시저 sp_retrospective, 없으면 null)
    RetrospectiveVO selectRetrospectiveById(Long retrospectiveId);

    // 스프린트의 회고 번호 (회고가 없으면 null)
    Long selectRetrospectiveIdBySprint(Long sprintId);

    // 스프린트가 있는지 (0이면 없음)
    int selectSprintCount(Long sprintId);

    // 회고 생성 (sp_create_retrospective, 생성된 페이지 번호는 pageId에 설정)
    void insertRetrospective(RetrospectiveVO retrospective);

    // ---------- 스냅샷 ----------

    // 상태 스냅샷 (칸반 순서)
    List<StatusSnapshotVO> selectStatusSnapshotList(Long retrospectiveId);

    // 작업 스냅샷 (프로시저 sp_task_snapshot_list, retrospectiveId가 null이면 워크스페이스 전체)
    List<TaskSnapshotVO> selectTaskSnapshotList(@Param("workspaceId") Long workspaceId,
        @Param("retrospectiveId") Long retrospectiveId);

    // 작업 스냅샷 담당자 (sp_task_snapshot_assignee_list, 범위는 위와 동일)
    List<TaskSnapshotAssigneeVO> selectTaskSnapshotAssigneeList(@Param("workspaceId") Long workspaceId,
        @Param("retrospectiveId") Long retrospectiveId);

    // 하위 작업 스냅샷 (프로시저 sp_subtask_snapshot_list)
    List<SubTaskSnapshotVO> selectSubTaskSnapshotList(Long retrospectiveId);

    // ---------- Action Item ----------

    // 회고 페이지들의 TODO 블록 (pageIds가 비어 있으면 호출하지 않음)
    List<TodoBlockVO> selectTodoBlockList(@Param("pageIds") List<Long> pageIds);
}
