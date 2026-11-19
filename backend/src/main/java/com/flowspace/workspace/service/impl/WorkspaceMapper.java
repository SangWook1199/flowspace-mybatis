package com.flowspace.workspace.service.impl;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.entity.enums.WorkspaceRole;
import com.flowspace.entity.enums.InviteStatus;
import com.flowspace.workspace.service.InviteVO;
import com.flowspace.workspace.service.MemberTaskCountVO;
import com.flowspace.workspace.service.MemberTaskVO;
import com.flowspace.workspace.service.WorkspaceMemberVO;
import com.flowspace.workspace.service.WorkspaceVO;

// 워크스페이스 Mapper (Workspace_SQL.xml)
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

    // 워크스페이스 등록 (생성된 번호는 workspaceId에 설정)
    int insertWorkspace(WorkspaceVO workspace);

    // 새 워크스페이스에 소유자와 기본 상태 연결 (프로시저 sp_init_workspace)
    void insertWorkspaceInit(@Param("workspaceId") Long workspaceId, @Param("ownerId") Long ownerId);

    // 워크스페이스 수정 (이름·이니셜·색·아이콘)
    int updateWorkspace(WorkspaceVO workspace);

    // 워크스페이스 삭제 (프로시저 sp_delete_workspace)
    void deleteWorkspace(Long workspaceId);

    // 이 워크스페이스에 이 이메일로 보낸, 아직 답하지 않은 초대 수
    int selectPendingInviteCount(@Param("workspaceId") Long workspaceId, @Param("email") String email);

    // 초대 등록 (생성된 번호는 inviteId에 설정)
    int insertInvite(InviteVO invite);

    // 내가 받은, 아직 답하지 않은 초대 (프로시저 sp_my_invite_list)
    List<InviteVO> selectMyInviteList(String email);

    // 초대 한 건 (없으면 null)
    InviteVO selectInviteById(Long inviteId);

    // 초대에 답하기 (상태와 답한 시각)
    int updateInviteStatus(@Param("inviteId") Long inviteId, @Param("status") InviteStatus status);

    // 멤버 추가
    int insertWorkspaceMember(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId,
        @Param("role") WorkspaceRole role);

    // 멤버 역할 변경
    int updateMemberRole(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId,
        @Param("role") WorkspaceRole role);

    // 워크스페이스 소유자 변경
    int updateWorkspaceOwner(@Param("workspaceId") Long workspaceId, @Param("ownerId") Long ownerId);

    // 멤버 빼기 (내보내기·나가기)
    int deleteWorkspaceMember(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId);

    // 사용자가 마지막으로 본 워크스페이스 지정
    int updateUserLastWorkspace(@Param("userId") Long userId, @Param("workspaceId") Long workspaceId);

    // 마지막으로 본 워크스페이스가 이곳이면 비우기 (나갈 때)
    int clearUserLastWorkspace(@Param("userId") Long userId, @Param("workspaceId") Long workspaceId);
}
