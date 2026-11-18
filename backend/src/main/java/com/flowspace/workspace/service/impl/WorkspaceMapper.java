package com.flowspace.workspace.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.entity.enums.WorkspaceRole;
import com.flowspace.workspace.service.MemberTaskCountVO;
import com.flowspace.workspace.service.MemberTaskVO;
import com.flowspace.workspace.service.WorkspaceMemberVO;
import com.flowspace.workspace.service.WorkspaceVO;

// 워크스페이스 DB 접근. 메서드 이름이 Workspace_SQL.xml의 id와 같아야 해요.
@Mapper
public interface WorkspaceMapper {

    // 워크스페이스 한 건 (없으면 null)
    WorkspaceVO selectWorkspaceById(Long workspaceId);

    // 그 워크스페이스에서 사용자의 역할 (멤버가 아니면 null)
    WorkspaceRole selectMemberRole(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId);

    // 내가 속한 워크스페이스와 내 역할 (프로시저 sp_my_workspace_list)
    List<WorkspaceVO> selectMyWorkspaceList(Long userId);

    // 멤버 목록 (프로시저 sp_workspace_member_list)
    List<WorkspaceMemberVO> selectWorkspaceMemberList(Long workspaceId);

    // 멤버 한 명 (프로시저 sp_workspace_member, 멤버가 아니면 null)
    WorkspaceMemberVO selectWorkspaceMember(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId);

    // 멤버가 맡은, 끝나지 않은 작업 (프로시저 sp_member_open_task_list)
    List<MemberTaskVO> selectMemberOpenTaskList(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId,
        @Param("limit") int limit);

    // 멤버가 맡은 작업의 진행 중·완료 수 (프로시저 sp_member_task_count)
    MemberTaskCountVO selectMemberTaskCount(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId);

    // 내가 속한 워크스페이스 수
    int selectMyWorkspaceCount(Long userId);

    // 워크스페이스 등록. 실행 후 DB가 만든 번호가 workspace의 workspaceId에 들어가요.
    int insertWorkspace(WorkspaceVO workspace);

    // 새 워크스페이스에 소유자와 기본 상태 연결 (프로시저 sp_init_workspace)
    void insertWorkspaceInit(@Param("workspaceId") Long workspaceId, @Param("ownerId") Long ownerId);

    // 워크스페이스 수정 (이름·이니셜·색·아이콘)
    int updateWorkspace(WorkspaceVO workspace);

    // 워크스페이스 삭제 (프로시저 sp_delete_workspace)
    void deleteWorkspace(Long workspaceId);
}
