# DB 프로시저

모든 SQL을 프로시저로 만들어요. Mapper XML(`src/main/resources/sqlmap/mappers/*.xml`)에는 `{call ...}`만 있어서, SQL을 고칠 때 백엔드를 다시 빌드하지 않아도 돼요.

| 쓰는 곳 | 기준 |
|---|---|
| 프로시저 | 조회·등록·수정·삭제 등 모든 SQL |
| Mapper XML | 프로시저 호출 (`{call sp_x(...)}`) |
| 자바 ServiceImpl | 권한 확인, 업무 규칙, 알림 (프로시저는 확인이 끝난 뒤의 데이터 처리만 해요) |

## 폴더 구조

```
database/
├── apply_procedures.sql    모든 프로시저를 DB에 적용하는 목록
└── procedures/
    ├── sprint.sql            스프린트 (Sprint_SQL.xml)
    ├── task.sql              작업(칸반) (Task_SQL.xml)
    ├── task_attachment.sql   작업 관련 문서 (TaskAttachment_SQL.xml)
    ├── workspace.sql         워크스페이스 (Workspace_SQL.xml)
    ├── user.sql              사용자·로그인 (User_SQL.xml)
    ├── notification.sql      알림 (Notification_SQL.xml)
    ├── activity.sql          활동 기록 (Activity_SQL.xml)
    ├── calendar.sql          캘린더·일정 (Calendar_SQL.xml)
    ├── comment.sql           댓글 (Comment_SQL.xml)
    ├── file.sql              파일 (File_SQL.xml)
    ├── retrospective.sql     회고 (Retrospective_SQL.xml)
    ├── page.sql              페이지 (Page_SQL.xml)
    ├── page_copy.sql         페이지 복제 (PageCopy_SQL.xml)
    ├── block.sql             블록 (Block_SQL.xml)
    ├── block_database.sql    데이터베이스 블록 (BlockDatabase_SQL.xml)
    ├── search.sql            통합 검색 (Search_SQL.xml)
    ├── presence.sql          접속 상태 (Presence_SQL.xml)
    └── cleanup.sql           만료 데이터 정리 (Cleanup_SQL.xml)
```

## 규칙

- 모든 SQL은 프로시저로 만들고, Mapper XML에는 `{call 프로시저(...)}`만 적어요. SQL을 고칠 때는 프로시저만 다시 적용하면 되고 백엔드를 다시 빌드하지 않아도 돼요.
- Mapper XML 하나에 프로시저 파일 하나 (`Page_SQL.xml` ↔ `procedures/page.sql`), 파일 안의 순서는 Mapper 메서드 순서
- 여러 Mapper가 부르는 프로시저는 테이블이 속한 도메인 파일에 하나만 둬요. 그 파일의 Mapper가 부르지 않으면 파일 끝 "다른 Mapper 공용" 구역에 둬요 (예: `sp_user_id_by_email`, `sp_page_lock`).
- 이름은 `sp_` + 대상 + 동작. 여러 건이면 `_list`, 개수는 `_count`, 단순 등록·수정·삭제는 `_insert`·`_update`·`_delete`로 끝나요.
- 새로 만든 번호는 `OUT` 인자로 돌려줘요 (`SET p_x_id = LAST_INSERT_ID()`, XML은 `#{xId, mode=OUT, jdbcType=BIGINT}`).
- 번호 목록은 JSON 배열 문자열로 넘겨요. XML은 `#{ids, typeHandler=com.flowspace.common.mybatis.IdListTypeHandler}`, 프로시저는 `JSON` 인자로 받아 `IN (SELECT id FROM JSON_TABLE(p_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids)`로 써요. 번호 쌍(원본 → 새 번호)은 `IdMapTypeHandler`예요.
- 종류(enum) 목록은 쉼표 문자열로 넘기고 `FIND_IN_SET`으로 받아요 (`sp_activity_list`, `sp_activity_count`).
- 인자에 null을 넘길 수 있게 `application.yaml`에 `jdbc-type-for-null: "NULL"`을 설정했어요.
- 프로시저마다 `DROP PROCEDURE IF EXISTS`로 시작해요. 그래서 파일을 몇 번 적용해도 최신 내용으로 다시 만들어져요.
- 데이터를 읽기만 하면 `READS SQL DATA`, 바꾸면 `MODIFIES SQL DATA`를 적어요.
- 새 프로시저는 Mapper에 맞는 파일에 추가하고, 아래 표에도 추가해요. 새 Mapper를 만들면 `apply_procedures.sql`에 파일 한 줄을 추가해요.

## 적용

```
docker compose up -d mysql
docker compose exec mysql mysql -uroot -p flow_space -e "source /database/apply_procedures.sql"
docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/page.sql"
docker compose exec mysql mysql -uroot -p flow_space -e "SHOW PROCEDURE STATUS WHERE Db = 'flow_space'"
```

빈 DB로 처음 켤 때는 `ddl.sql` 다음에 자동으로 적용돼요(`docker-compose.yml`의 `docker-entrypoint-initdb.d`).

## 프로시저 목록

파일마다 Mapper 메서드 순서예요. "하는 일"은 프로시저 머리 주석 첫 줄이에요.
`Ai_SQL.xml`(AI 초안 반영)은 다른 파일의 프로시저만 불러서 자기 프로시저 파일이 없어요.

### 스프린트 (`procedures/sprint.sql` ↔ `Sprint_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_sprint` | 스프린트 한 건 | `SprintMapper.selectSprintById` |
| `sp_sprint_progress_list` | 워크스페이스 스프린트 목록 + 진행 상황 (작업 수, 완료 작업 수) | `SprintMapper.selectSprintProgressList` |
| `sp_sprint_progress` | 스프린트 한 건 + 진행 상황 (작업 수, 완료 작업 수) | `SprintMapper.selectSprintProgressById` |
| `sp_sprint_count_by_status` | 워크스페이스에서 해당 상태인 스프린트 수 | `SprintMapper.selectSprintCountByStatus` |
| `sp_sprint_insert` | 스프린트 등록 (생성 시각은 DB 기본값) | `SprintMapper.insertSprint`, `AiMapper.insertSprint` |
| `sp_sprint_update` | 스프린트 수정 (상태 제외) | `SprintMapper.updateSprint` |
| `sp_start_sprint` | 스프린트 시작 (진행 중이던 다른 스프린트는 계획으로 변경) | `SprintMapper.updateSprintStart` |
| `sp_complete_sprint` | 스프린트 완료 (미완료 작업은 백로그 맨 뒤로 이동, 완료 작업은 유지) | `SprintMapper.updateSprintComplete` |
| `sp_delete_sprint` | 스프린트 삭제 (작업은 백로그 맨 뒤로 이동) | `SprintMapper.deleteSprint` |
| `sp_backlog_task_count` | 백로그(스프린트 없음) 작업 수 | `SprintMapper.selectBacklogTaskCount` |

### 작업(칸반) (`procedures/task.sql` ↔ `Task_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_task_list` | 칸반 작업 목록 + 상태 이름 (스프린트·백로그 공용) | `TaskMapper.selectTaskList` |
| `sp_task_assignee_list` | 칸반 작업 담당자 (이름·프로필 이미지 포함, 작업 조건은 sp_task_list와 동일) | `TaskMapper.selectTaskAssigneeList` |
| `sp_subtask_list` | 여러 작업의 하위 작업 | `TaskMapper.selectSubTaskList` |
| `sp_task` | 작업 한 건 + 상태 이름 (컬럼은 sp_task_list와 동일) | `TaskMapper.selectTaskById` |
| `sp_task_assignee` | 작업 한 건의 담당자 (이름·프로필 이미지 포함, 컬럼은 sp_task_assignee_list와 동일) | `TaskMapper.selectTaskAssigneeListByTaskId` |
| `sp_task_count_by_status` | 워크스페이스에서 해당 상태인 작업 수 | `TaskMapper.selectTaskCountByStatus`, `AiMapper.selectTaskCountByStatus` |
| `sp_task_max_number` | 워크스페이스의 최대 작업 번호 (없으면 0) | `TaskMapper.selectMaxTaskNumber`, `AiMapper.selectMaxTaskNumber` |
| `sp_task_insert` | 작업 등록 (생성 시각은 DB 기본값) | `TaskMapper.insertTask`, `AiMapper.insertTask` |
| `sp_task_assignee_insert` | 작업 담당자 등록 | `TaskMapper.insertTaskAssignee`, `AiMapper.insertTaskAssignee` |
| `sp_task_update` | 작업 수정 | `TaskMapper.updateTask` |
| `sp_task_assignee_user_id_list` | 작업 담당자의 사용자 번호 목록 | `TaskMapper.selectTaskAssigneeUserIds`, `CommentMapper.selectTaskAssigneeUserIds` |
| `sp_task_assignee_delete` | 작업 담당자 모두 삭제 | `TaskMapper.deleteTaskAssignees` |
| `sp_subtask_assignee_clear` | 작업 담당자가 아닌 하위 작업 담당자 해제 (남은 담당자가 없으면 전체 해제) | `TaskMapper.updateSubTaskAssigneeClear` |
| `sp_delete_task` | 작업 삭제 + 같은 칸(스프린트·상태)의 남은 작업 순서 재정렬 (0부터) | `TaskMapper.deleteTask` |
| `sp_task_assignee_count` | 작업 담당자 여부 | `TaskMapper.selectTaskAssigneeCount` |
| `sp_subtask` | 하위 작업 한 건 | `TaskMapper.selectSubTaskById` |
| `sp_subtask_count` | 작업의 하위 작업 수 | `TaskMapper.selectSubTaskCount` |
| `sp_subtask_insert` | 하위 작업 등록 (is_completed는 DB 기본값) | `TaskMapper.insertSubTask`, `AiMapper.insertSubTask` |
| `sp_subtask_update` | 하위 작업 수정 | `TaskMapper.updateSubTask` |
| `sp_subtask_position_update` | 하위 작업 순서 변경 | `TaskMapper.updateSubTaskPosition` |
| `sp_subtask_delete` | 하위 작업 삭제 | `TaskMapper.deleteSubTask` |
| `sp_task_status_list` | 워크스페이스 작업 상태(칸반 열) 목록 (task_statuses + workspace_task_statuses) | `TaskMapper.selectTaskStatusList`, `AiMapper.selectTaskStatusList` |
| `sp_task_status` | 워크스페이스 작업 상태 한 개 (컬럼은 sp_task_status_list와 동일, 연결되지 않은 상태면 0행) | `TaskMapper.selectTaskStatusById` |
| `sp_task_status_count` | 워크스페이스의 작업 상태 수 | `TaskMapper.selectTaskStatusCount` |
| `sp_task_status_insert` | 상태 등록 | `TaskMapper.insertTaskStatus` |
| `sp_workspace_task_status_insert` | 워크스페이스에 상태 연결 (is_default는 DB 기본값) | `TaskMapper.insertWorkspaceTaskStatus` |
| `sp_task_status_update` | 상태 정보 수정 | `TaskMapper.updateTaskStatusInfo` |
| `sp_workspace_task_status_wip_limit_update` | 워크스페이스별 WIP 제한 수정 | `TaskMapper.updateWorkspaceTaskStatusWipLimit` |
| `sp_workspace_task_status_position_update` | 칸반 열 순서 변경 | `TaskMapper.updateTaskStatusPosition` |
| `sp_replace_default_task_status` | 기본 상태(공용 행) 수정 시 해당 워크스페이스만 새 상태로 교체 (공용 행은 유지) | `TaskMapper.updateDefaultTaskStatus` |
| `sp_delete_task_status` | 작업 상태(칸반 열) 삭제 + 해당 상태 작업을 다른 상태로 이동 | `TaskMapper.deleteTaskStatus` |
| `sp_task_search` | 제목으로 작업 검색 (대소문자 구분 없음, 상태 이름 포함) | `TaskMapper.selectTaskSearchList` |
| `sp_task_assignee_list_by_tasks` | 여러 작업의 담당자 (sp_task_assignee_list와 같은 컬럼) | `TaskMapper.selectTaskAssigneeListByTaskIds`, `CalendarMapper.selectCalendarAssigneeList` |

### 작업 관련 문서 (`procedures/task_attachment.sql` ↔ `TaskAttachment_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_task_attachment_list` | 작업 관련 문서 목록 (페이지·파일 정보, 등록자 이름 포함, 휴지통 페이지 제외) | `TaskAttachmentMapper.selectTaskAttachmentList` |
| `sp_task_attachment` | 관련 문서 한 건 (컬럼은 sp_task_attachment_list와 동일) | `TaskAttachmentMapper.selectTaskAttachmentById` |
| `sp_task_attachment_count` | 작업의 관련 문서 수 | `TaskAttachmentMapper.selectTaskAttachmentCount` |
| `sp_task_attachment_page_count` | 페이지 연결 여부 | `TaskAttachmentMapper.selectTaskAttachmentPageCount` |
| `sp_task_attachment_file_id_list` | 작업에 업로드한 파일 번호 | `TaskAttachmentMapper.selectTaskAttachmentFileIds` |
| `sp_active_page_workspace_id` | 연결할 페이지의 워크스페이스 (휴지통 페이지 제외) | `TaskAttachmentMapper.selectActivePageWorkspaceId` |
| `sp_task_attachment_insert` | 관련 문서 등록 (pageId, fileId 중 하나) | `TaskAttachmentMapper.insertTaskAttachment` |
| `sp_task_attachment_delete` | 관련 문서 삭제 | `TaskAttachmentMapper.deleteTaskAttachment` |

### 워크스페이스 (`procedures/workspace.sql` ↔ `Workspace_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_workspace` | 워크스페이스 한 건 | `WorkspaceMapper.selectWorkspaceById` |
| `sp_workspace_member_role` | 워크스페이스에서 사용자의 역할 (enum으로 매핑, 없으면 null) | `WorkspaceMapper.selectMemberRole` |
| `sp_my_workspace_list` | 내 워크스페이스 목록 + 역할 | `WorkspaceMapper.selectMyWorkspaceList` |
| `sp_workspace_member_list` | 워크스페이스 멤버 목록 (이름·프로필 이미지·역할 포함) | `WorkspaceMapper.selectWorkspaceMemberList`, `AiMapper.selectWorkspaceMemberList` |
| `sp_workspace_member` | 워크스페이스 멤버 한 명 (컬럼은 sp_workspace_member_list와 동일) | `WorkspaceMapper.selectWorkspaceMember` |
| `sp_member_open_task_list` | 멤버의 미완료 담당 작업 (프로필 카드용) | `WorkspaceMapper.selectMemberOpenTaskList` |
| `sp_member_task_count` | 멤버 담당 작업의 진행 중·완료 수 (프로필 카드용) | `WorkspaceMapper.selectMemberTaskCount` |
| `sp_my_workspace_count` | 내가 속한 워크스페이스 수 | `WorkspaceMapper.selectMyWorkspaceCount` |
| `sp_workspace_insert` | 워크스페이스 등록 (생성 시각은 DB 기본값) | `WorkspaceMapper.insertWorkspace` |
| `sp_init_workspace` | 새 워크스페이스에 소유자와 기본 작업 상태 3개(할 일·진행 중·완료) 연결 | `WorkspaceMapper.insertWorkspaceInit` |
| `sp_workspace_update` | 워크스페이스 수정 | `WorkspaceMapper.updateWorkspace` |
| `sp_delete_workspace` | 워크스페이스 삭제 (작업은 CASCADE가 없어 먼저 삭제, 나머지는 ON DELETE CASCADE / SET NULL) | `WorkspaceMapper.deleteWorkspace` |
| `sp_pending_invite_count` | 대기 중인 초대 수 | `WorkspaceMapper.selectPendingInviteCount` |
| `sp_invite_insert` | 초대 등록 (생성 시각은 DB 기본값) | `WorkspaceMapper.insertInvite` |
| `sp_my_invite_list` | 받은 초대 중 대기 중인 초대 (워크스페이스·초대한 사람 이름 포함) | `WorkspaceMapper.selectMyInviteList` |
| `sp_invite` | 초대 한 건 | `WorkspaceMapper.selectInviteById` |
| `sp_invite_status_update` | 초대 응답 처리 | `WorkspaceMapper.updateInviteStatus` |
| `sp_workspace_member_insert` | 멤버 추가 (참여 시각은 DB 기본값) | `WorkspaceMapper.insertWorkspaceMember` |
| `sp_workspace_member_role_update` | 멤버 역할 변경 | `WorkspaceMapper.updateMemberRole` |
| `sp_workspace_owner_update` | 워크스페이스 소유자 변경 | `WorkspaceMapper.updateWorkspaceOwner` |
| `sp_workspace_member_delete` | 멤버 삭제 | `WorkspaceMapper.deleteWorkspaceMember` |
| `sp_user_last_workspace_update` | 사용자의 마지막 워크스페이스 지정 (워크스페이스 생성 시 함께 변경) | `WorkspaceMapper.updateUserLastWorkspace` |
| `sp_user_last_workspace_clear` | 마지막 워크스페이스가 이곳이면 해제 | `WorkspaceMapper.clearUserLastWorkspace` |
| `sp_workspace_user_id_list` (공용) | 워크스페이스 멤버의 사용자 번호 | `SprintMapper.selectWorkspaceUserIds`, `AiMapper.selectWorkspaceUserIds` |

### 사용자·로그인 (`procedures/user.sql` ↔ `User_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_user_by_email` | 이메일로 사용자 조회 (프로필 이미지 주소 포함) | `SprintMapper.selectUserByEmail`, `TaskMapper.selectUserByEmail`, `WorkspaceMapper.selectUserByEmail`, `UserMapper.selectUserByEmail`, `AiMapper.selectUserByEmail` |
| `sp_user_profile_update` | 프로필 수정 | `UserMapper.updateUserProfile` |
| `sp_user_profile_file_update` | 프로필 이미지 수정 | `UserMapper.updateUserProfileFile` |
| `sp_user_password_update` | 비밀번호 수정 | `UserMapper.updateUserPassword` |
| `sp_user_first_workspace_id` | 참여 중인 첫 워크스페이스 번호 | `UserMapper.selectFirstWorkspaceId` |
| `sp_refresh_token_expired_at` | refresh token 만료 시각 | `UserMapper.selectRefreshTokenExpiredAt` |
| `sp_refresh_token_delete` | refresh token 삭제 | `UserMapper.deleteRefreshToken` |
| `sp_refresh_token_insert` | refresh token 저장 | `UserMapper.insertRefreshToken` |
| `sp_user` | 번호로 사용자 조회 (프로필 이미지 주소 포함, 컬럼은 sp_user_by_email과 동일) | `TaskMapper.selectUserById`, `WorkspaceMapper.selectUserById`, `UserMapper.selectUserById` |
| `sp_user_insert` | 사용자 등록 (생성 시각은 DB 기본값) | `UserMapper.insertUser` |
| `sp_user_signup_restart_update` | 인증 전 재가입 | `UserMapper.updateUserSignupRestart` |
| `sp_user_email_verified_update` | 이메일 인증 완료 | `UserMapper.updateUserEmailVerified` |
| `sp_user_claim_by_social_update` | 인증 전 계정을 소셜 로그인으로 전환 | `UserMapper.updateUserClaimBySocial` |
| `sp_refresh_token` | 저장된 refresh token | `UserMapper.selectRefreshToken` |
| `sp_social_account_user_id` | 소셜 계정에 연결된 사용자 번호 | `UserMapper.selectSocialUserId` |
| `sp_user_id_by_provider` | 예전 방식으로 가입한 소셜 사용자 번호 | `UserMapper.selectUserIdByProvider` |
| `sp_social_account_count` | 소셜 계정 연결 여부 | `UserMapper.selectSocialAccountCount` |
| `sp_social_account_insert` | 소셜 계정 연결 | `UserMapper.insertSocialAccount` |
| `sp_email_token_last_created_at` | 가장 최근에 보낸 메일 시각 (email_verification_tokens) | `UserMapper.selectLastEmailTokenCreatedAt` |
| `sp_email_token_delete` | 사용자의 토큰 전체 삭제 | `UserMapper.deleteEmailTokens` |
| `sp_email_token_insert` | 토큰 저장 (생성 시각은 DB 기본값) | `UserMapper.insertEmailToken` |
| `sp_email_token_by_hash` | 해시로 토큰 찾기 | `UserMapper.selectEmailTokenByHash` |
| `sp_password_token_last_created_at` | 가장 최근에 보낸 메일 시각 (password_reset_tokens) | `UserMapper.selectLastPasswordTokenCreatedAt` |
| `sp_password_token_delete` | 사용자의 토큰 전체 삭제 | `UserMapper.deletePasswordTokens` |
| `sp_password_token_insert` | 토큰 저장 (생성 시각은 DB 기본값) | `UserMapper.insertPasswordToken` |
| `sp_password_token_by_hash` | 해시로 토큰 찾기 | `UserMapper.selectPasswordTokenByHash` |
| `sp_password_token_hash_delete` | 재설정 토큰 삭제 | `UserMapper.deletePasswordTokenByHash` |
| `sp_withdraw_check` | 탈퇴 전 확인용 소속 워크스페이스 (내 역할, 멤버 수) | `UserMapper.selectWithdrawCheckList` |
| `sp_withdraw_user` | 회원 탈퇴 (사용자 행은 유지하고 개인정보만 삭제, 작업·댓글·활동 기록 보존) | `UserMapper.updateUserWithdraw` |
| `sp_user_lookup` | 이메일이 정확히 일치하는 사용자 조회 + 워크스페이스 멤버·초대 여부 (초대 화면용) | `UserMapper.selectUserLookup` |
| `sp_user_id_by_email` (공용) | 이메일로 사용자 번호 | `TaskAttachmentMapper.selectUserIdByEmail`, `NotificationMapper.selectUserIdByEmail`, `CalendarMapper.selectUserIdByEmail`, `FileMapper.selectUserIdByEmail`, `PageMapper.selectUserIdByEmail`, `BlockMapper.selectUserIdByEmail`, `BlockDatabaseMapper.selectUserIdByEmail`, `PresenceMapper.selectUserIdByEmail` |

### 알림 (`procedures/notification.sql` ↔ `Notification_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_notification_insert` | 알림 등록 (읽음 여부·생성 시각은 DB 기본값) | `NotificationMapper.insertNotification` |
| `sp_notification` | 알림 한 건 (컬럼은 sp_notification_list와 동일, 실시간 전송용) | `NotificationMapper.selectNotificationById` |
| `sp_notification_list` | 내 알림 목록 (최신순, 워크스페이스·보낸 사람 이름 포함) | `NotificationMapper.selectNotificationList` |
| `sp_notification_unread_count` | 읽지 않은 알림 수 | `NotificationMapper.selectUnreadNotificationCount` |
| `sp_notification_user_id` | 알림 수신자 번호 | `NotificationMapper.selectNotificationUserId` |
| `sp_notification_read_update` | 알림 한 건 읽음 | `NotificationMapper.updateNotificationRead` |
| `sp_notification_read_all_update` | 내 알림 모두 읽음 | `NotificationMapper.updateAllNotificationsRead` |
| `sp_notification_read_by_ref_update` | 특정 대상에 대한 내 알림 읽음 | `NotificationMapper.updateNotificationReadByRef` |
| `sp_notification_delete` | 알림 한 건 삭제 | `NotificationMapper.deleteNotification` |
| `sp_notification_old_delete` | 보관 기간이 지난 알림 삭제 | `NotificationMapper.deleteOldNotifications` |
| `sp_due_soon_target_list` | 마감 임박 알림 대상 (해당 마감일의 미완료 작업과 담당자, 오늘 이미 보낸 대상 제외) | `NotificationMapper.selectDueSoonTargetList` |

### 활동 기록 (`procedures/activity.sql` ↔ `Activity_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_activity_insert` | 활동 기록 등록 (생성 시각은 DB 기본값) | `ActivityMapper.insertActivity` |
| `sp_activity_list` | 워크스페이스 활동 기록 목록 (최신순, 사용자·대상 현재 이름 포함) | `ActivityMapper.selectActivityList` |
| `sp_activity_count` | 활동 기록 수 (조건은 sp_activity_list와 동일) | `ActivityMapper.selectActivityCount` |
| `sp_user_count` | 사용자 존재 여부 | `ActivityMapper.selectUserCount` |

### 캘린더·일정 (`procedures/calendar.sql` ↔ `Calendar_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_event_insert` | 일정 등록 (생성·수정 시각은 DB 기본값) | `CalendarMapper.insertEvent`, `AiMapper.insertEvent` |
| `sp_event` | 일정 한 건 (작성자 이름 포함) | `CalendarMapper.selectEventById` |
| `sp_event_update` | 일정 수정 (updated_at은 ON UPDATE CURRENT_TIMESTAMP) | `CalendarMapper.updateEvent` |
| `sp_event_delete` | 일정 삭제 (블록의 event_id는 ON DELETE SET NULL) | `CalendarMapper.deleteEvent` |
| `sp_event_search_list` | 제목으로 일정 검색 (대소문자 구분 없음) | `CalendarMapper.selectEventSearchList` |
| `sp_sprint_workspace_id` | 스프린트가 속한 워크스페이스 | `TaskMapper.selectSprintWorkspaceId`, `CalendarMapper.selectSprintWorkspaceId`, `BlockMapper.selectSprintWorkspaceId` |
| `sp_default_sprint_id` | 기본 스프린트 (진행 중 → 계획 중, 시작일 순) | `CalendarMapper.selectDefaultSprintId` |
| `sp_calendar_task_list` | 캘린더 월별 작업 목록 (시작일이 해당 월인 작업, 상태 색 포함) | `CalendarMapper.selectCalendarTaskList` |
| `sp_calendar_event_list` | 그 달에 시작하는 일정 | `CalendarMapper.selectCalendarEventList` |

### 댓글 (`procedures/comment.sql` ↔ `Comment_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_user_name_by_email` | 이메일로 사용자 번호와 닉네임 | `CommentMapper.selectUserByEmail` |
| `sp_comment` | 댓글 한 건 | `CommentMapper.selectCommentById` |
| `sp_comment_list_by_tasks` | 여러 작업의 댓글 (원 댓글·답글 함께 조회) | `CommentMapper.selectCommentListByTaskIds` |
| `sp_comment_list_by_blocks` | 여러 블록의 댓글 | `CommentMapper.selectCommentListByBlockIds` |
| `sp_comment_insert` | 댓글 등록 (taskId, blockId 중 하나) | `CommentMapper.insertComment` |
| `sp_comment_content_update` | 댓글 내용 수정 | `CommentMapper.updateCommentContent` |
| `sp_comment_delete` | 댓글 삭제 (답글은 ON DELETE CASCADE) | `CommentMapper.deleteComment` |
| `sp_comment_task` | 작업 정보 | `CommentMapper.selectCommentTask` |
| `sp_comment_block` | 블록 댓글용 페이지 정보 (워크스페이스, 제목, 작성자, 휴지통 여부) | `CommentMapper.selectCommentBlock` |
| `sp_workspace_member_id_list` | 멘션된 사용자 중 워크스페이스 멤버 | `CommentMapper.selectWorkspaceMemberIds` |

### 파일 (`procedures/file.sql` ↔ `File_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_file` | 파일 한 건 | `FileMapper.selectFileById` |
| `sp_file_insert` | 파일 등록 (생성 시각은 DB 기본값) | `FileMapper.insertFile` |
| `sp_file_delete` | 파일 삭제 (참조하는 곳은 ON DELETE SET NULL / CASCADE) | `FileMapper.deleteFile` |

### 회고 (`procedures/retrospective.sql` ↔ `Retrospective_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_retrospective_list` | 워크스페이스 스프린트 + 회고 목록 (회고가 없는 스프린트 포함) | `RetrospectiveMapper.selectRetrospectiveList` |
| `sp_retrospective` | 회고 한 건 + 스프린트 정보 (컬럼은 sp_retrospective_list와 동일) | `RetrospectiveMapper.selectRetrospectiveById` |
| `sp_retrospective_id_by_sprint` | 스프린트의 회고 번호 | `RetrospectiveMapper.selectRetrospectiveIdBySprint` |
| `sp_sprint_count` | 스프린트 존재 여부 | `RetrospectiveMapper.selectSprintCount` |
| `sp_create_retrospective` | 스프린트 완료 시 회고 생성 | `RetrospectiveMapper.insertRetrospective` |
| `sp_status_snapshot_list` | 상태 스냅샷 | `RetrospectiveMapper.selectStatusSnapshotList` |
| `sp_task_snapshot_list` | 회고 작업 스냅샷 (상태 이름 포함) | `RetrospectiveMapper.selectTaskSnapshotList` |
| `sp_task_snapshot_assignee_list` | 작업 스냅샷 담당자 (프로필 사진 주소 포함, 범위는 sp_task_snapshot_list와 동일) | `RetrospectiveMapper.selectTaskSnapshotAssigneeList` |
| `sp_subtask_snapshot_list` | 회고의 하위 작업 스냅샷 (담당자 사진 주소 포함) | `RetrospectiveMapper.selectSubTaskSnapshotList` |
| `sp_todo_block_list` | 회고 페이지들의 TODO 블록 | `RetrospectiveMapper.selectTodoBlockList` |

### 페이지 (`procedures/page.sql` ↔ `Page_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_page_list` | 페이지 목록 또는 휴지통 목록 (커버 주소·회고 여부·상위 페이지 휴지통 여부 포함) | `PageMapper.selectPageList` |
| `sp_page` | 페이지 한 개 (휴지통 포함, 컬럼은 sp_page_list와 동일) | `PageMapper.selectPageById`, `BlockMapper.selectBlockPage`, `BlockDatabaseMapper.selectPage`, `PresenceMapper.selectPage` |
| `sp_page_subtree` | 페이지 본인과 모든 하위 페이지 (휴지통 포함, WITH RECURSIVE) | `PageMapper.selectPageSubtree` |
| `sp_page_list_by_ids` | 여러 페이지 (순서 바꾸기 확인용) | `PageMapper.selectPageListByIds` |
| `sp_page_parent_id` | 상위 페이지 번호 | `PageMapper.selectParentPageId` |
| `sp_page_sibling_count` | 같은 상위 페이지 아래의 페이지 수 (<=>: NULL끼리 같음) | `PageMapper.selectSiblingCount` |
| `sp_page_insert` | 페이지 등록 (휴지통 여부·버전·생성 시각은 DB 기본값) | `PageMapper.insertPage` |
| `sp_page_update` | 제목·아이콘·상위 페이지 수정 (상위 페이지 null이면 최상위) | `PageMapper.updatePage` |
| `sp_page_position_update` | 순서 수정 | `PageMapper.updatePagePosition` |
| `sp_page_parent_update` | 상위 페이지와 순서 수정 | `PageMapper.updatePageParent` |
| `sp_page_cover_update` | 커버 수정 | `PageMapper.updatePageCover` |
| `sp_page_trash_update` | 여러 페이지 휴지통 이동 (같은 삭제 시각 = 함께 복원되는 묶음) | `PageMapper.updatePagesTrashed` |
| `sp_page_restore_update` | 여러 페이지 복원 | `PageMapper.updatePagesRestored` |
| `sp_page_outside_database_row_delete` | 휴지통으로 가는 페이지를 가리키는 데이터베이스 행 삭제 (함께 이동하는 페이지 안의 데이터베이스는 제외, 셀은 연쇄 삭제) | `PageMapper.deleteOutsideDatabaseRows` |
| `sp_page_child_list` | 휴지통이 아닌 하위 페이지 | `PageMapper.selectChildPageList` |
| `sp_page_expired_trash_list` | 보관 기간이 지난 휴지통 페이지 (모든 워크스페이스) | `PageMapper.selectExpiredTrashList` |
| `sp_page_retrospective_id_list` | 주어진 페이지 중 회고 페이지 번호 | `PageMapper.selectRetrospectivePageIds` |
| `sp_page_file_id_list` | 페이지 커버·블록 이미지 파일 번호 (UNION으로 중복 제외) | `PageMapper.selectPageFileIds` |
| `sp_page_database_row_delete` | 페이지를 가리키는 데이터베이스 행 삭제 | `PageMapper.deleteDatabaseRowsByPages` |
| `sp_page_delete` | 페이지 삭제 (블록·데이터베이스는 CASCADE) | `PageMapper.deletePage` |
| `sp_page_lock` (공용) | 페이지 정보 + 트랜잭션 끝까지 행 잠금 (FOR UPDATE, 휴지통 제외) | `BlockMapper.selectBlockPageForUpdate`, `BlockDatabaseMapper.selectPageForUpdate` |
| `sp_page_version_update` (공용) | 페이지 버전 +1 | `BlockMapper.updatePageVersion`, `BlockDatabaseMapper.updatePageVersion` |

### 페이지 복제 (`procedures/page_copy.sql` ↔ `PageCopy_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_copy_block_list` | 페이지의 블록 목록 | `PageCopyMapper.selectBlockList` |
| `sp_copy_block_insert` | 블록 복사본 등록 (상위 블록은 나중에 연결) | `PageCopyMapper.insertBlock` |
| `sp_copy_block_parent_update` | 상위 블록 연결 | `PageCopyMapper.updateBlockParent` |
| `sp_copy_database` | 블록의 데이터베이스 | `PageCopyMapper.selectDatabaseByBlock` |
| `sp_copy_database_insert` | 데이터베이스 복사본 등록 | `PageCopyMapper.insertDatabase` |
| `sp_copy_column_list` | 컬럼 목록 | `PageCopyMapper.selectColumnList` |
| `sp_copy_column_insert` | 컬럼 복사본 등록 | `PageCopyMapper.insertColumn` |
| `sp_copy_option_insert` | 옵션 복사 (원본 컬럼 → 새 컬럼) | `PageCopyMapper.insertOptionCopies` |
| `sp_copy_row_list` | 행 목록 | `PageCopyMapper.selectRowList` |
| `sp_copy_row_insert` | 행 복사본 등록 | `PageCopyMapper.insertRow` |
| `sp_copy_cell_insert` | 셀 복사 (원본 행 → 새 행, 컬럼 번호는 원본 → 새 번호 쌍으로 교체) | `PageCopyMapper.insertCellCopies` |

### 블록 (`procedures/block.sql` ↔ `Block_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_block_list` | 페이지의 블록 목록 (이미지 주소·데이터베이스 번호·페이지 정보 포함) | `BlockMapper.selectBlockList` |
| `sp_block` | 블록 한 개 (컬럼은 sp_block_list와 동일) | `BlockMapper.selectBlockById` |
| `sp_page_block_count` | 페이지의 블록 수 | `BlockMapper.selectBlockCount`, `BlockDatabaseMapper.selectBlockCount` |
| `sp_block_list_by_ids` | 여러 블록의 페이지 번호 | `BlockMapper.selectBlockListByIds` |
| `sp_block_parent_id` | 상위 블록 번호 | `BlockMapper.selectParentBlockId` |
| `sp_task_workspace_id` | 작업의 워크스페이스 | `BlockMapper.selectTaskWorkspaceId` |
| `sp_event_workspace_id` | 일정의 워크스페이스 | `BlockMapper.selectEventWorkspaceId` |
| `sp_block_insert` | 블록 등록 (content는 JSON 컬럼) | `BlockMapper.insertBlock` |
| `sp_block_content_update` | 종류·내용 수정 | `BlockMapper.updateBlockContent` |
| `sp_block_sync_update` | 일괄 저장: 종류·내용·연결·상위 블록·순서 | `BlockMapper.updateBlockSync` |
| `sp_block_parent_update` | 상위 블록과 순서 | `BlockMapper.updateBlockParent` |
| `sp_block_position_update` | 순서 | `BlockMapper.updateBlockPosition` |
| `sp_block_image_update` | 이미지 | `BlockMapper.updateBlockImage` |
| `sp_block_parent_clear` | 여러 블록의 상위 블록 연결 끊기 | `BlockMapper.updateBlockParentsCleared` |
| `sp_block_delete` | 블록 삭제 (하위 블록·댓글·데이터베이스는 ON DELETE CASCADE) | `BlockMapper.deleteBlock` |
| `sp_block_list_delete` | 여러 블록 삭제 | `BlockMapper.deleteBlocks` |

### 데이터베이스 블록 (`procedures/block_database.sql` ↔ `BlockDatabase_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_block_database_block_insert` | 데이터베이스 블록 등록 | `BlockDatabaseMapper.insertBlock` |
| `sp_block_database_row_page_insert` | 행 페이지 등록 (제목·순서는 DB 기본값) | `BlockDatabaseMapper.insertPage` |
| `sp_block_database_row_count_by_page` | 페이지를 쓰는 행 수 | `BlockDatabaseMapper.selectRowCountByPage` |
| `sp_block_database` | 데이터베이스 블록 한 개 (블록이 있는 페이지·워크스페이스·휴지통 여부 포함) | `BlockDatabaseMapper.selectDatabase` |
| `sp_block_database_insert` | 데이터베이스 등록 | `BlockDatabaseMapper.insertDatabase` |
| `sp_block_database_title_update` | 제목 수정 | `BlockDatabaseMapper.updateDatabaseTitle` |
| `sp_block_database_view_type_update` | 표 / 데이터베이스 전환 | `BlockDatabaseMapper.updateDatabaseViewType` |
| `sp_block_database_delete` | 데이터베이스 삭제 | `BlockDatabaseMapper.deleteDatabase` |
| `sp_block_database_column_list` | 컬럼 목록 | `BlockDatabaseMapper.selectColumnList` |
| `sp_block_database_column` | 컬럼 한 개 | `BlockDatabaseMapper.selectColumn` |
| `sp_block_database_column_list_by_ids` | 여러 컬럼 | `BlockDatabaseMapper.selectColumnListByIds` |
| `sp_block_database_column_count` | 컬럼 수 | `BlockDatabaseMapper.selectColumnCount` |
| `sp_block_database_column_insert` | 컬럼 등록 (너비는 DB 기본값) | `BlockDatabaseMapper.insertColumn` |
| `sp_block_database_column_update` | 이름·종류 수정 | `BlockDatabaseMapper.updateColumn` |
| `sp_block_database_column_width_update` | 너비 수정 | `BlockDatabaseMapper.updateColumnWidth` |
| `sp_block_database_column_position_update` | 순서 수정 | `BlockDatabaseMapper.updateColumnPosition` |
| `sp_block_database_column_delete` | 컬럼 삭제 | `BlockDatabaseMapper.deleteColumn` |
| `sp_block_database_column_option_list` | 컬럼의 옵션 목록 | `BlockDatabaseMapper.selectOptionList` |
| `sp_block_database_option_list` | 데이터베이스의 모든 컬럼 옵션 | `BlockDatabaseMapper.selectDatabaseOptionList` |
| `sp_block_database_option` | 옵션 한 개 | `BlockDatabaseMapper.selectOption` |
| `sp_block_database_option_list_by_ids` | 여러 옵션 | `BlockDatabaseMapper.selectOptionListByIds` |
| `sp_block_database_option_count` | 컬럼의 옵션 수 | `BlockDatabaseMapper.selectOptionCount` |
| `sp_block_database_option_insert` | 옵션 등록 | `BlockDatabaseMapper.insertOption` |
| `sp_block_database_option_update` | 값·색상·상태 그룹 수정 | `BlockDatabaseMapper.updateOption` |
| `sp_block_database_option_position_update` | 순서 수정 | `BlockDatabaseMapper.updateOptionPosition` |
| `sp_block_database_option_delete` | 옵션 삭제 | `BlockDatabaseMapper.deleteOption` |
| `sp_block_database_row_list` | 행 목록 | `BlockDatabaseMapper.selectRowList` |
| `sp_block_database_row` | 행 한 개 | `BlockDatabaseMapper.selectRow` |
| `sp_block_database_row_list_by_ids` | 여러 행 | `BlockDatabaseMapper.selectRowListByIds` |
| `sp_block_database_row_count` | 행 수 | `BlockDatabaseMapper.selectRowCount` |
| `sp_block_database_row_insert` | 행 등록 | `BlockDatabaseMapper.insertRow` |
| `sp_block_database_row_position_update` | 순서 수정 | `BlockDatabaseMapper.updateRowPosition` |
| `sp_block_database_row_delete` | 행 삭제 | `BlockDatabaseMapper.deleteRow` |
| `sp_block_database_column_cell_insert` | 새 컬럼의 빈 셀 (기존 행마다) | `BlockDatabaseMapper.insertColumnCells` |
| `sp_block_database_row_cell_insert` | 새 행의 빈 셀 (컬럼마다) | `BlockDatabaseMapper.insertRowCells` |
| `sp_block_database_cell` | 셀 한 개 | `BlockDatabaseMapper.selectCell` |
| `sp_block_database_cell_value_update` | 값 수정 | `BlockDatabaseMapper.updateCellValue` |
| `sp_block_database_cell_list` | 데이터베이스의 모든 셀 | `BlockDatabaseMapper.selectDatabaseCellList` |

### 통합 검색 (`procedures/search.sql` ↔ `Search_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_search_page_list` | 제목으로 페이지 검색 (휴지통 제외) | `SearchMapper.selectPageListByTitle` |
| `sp_search_block_list` | 본문 검색 후보 블록 (휴지통 페이지 제외, 블록 내용 JSON을 글자로 바꿔 LIKE) | `SearchMapper.selectBlockList` |
| `sp_search_task_list` | 작업 검색 (제목·설명, 상태 이름 포함) | `SearchMapper.selectTaskList` |
| `sp_search_sprint_list` | 스프린트 검색 (이름·목표) | `SearchMapper.selectSprintList` |
| `sp_search_comment_list` | 댓글 검색 (작업 댓글 + 휴지통이 아닌 페이지의 블록 댓글) | `SearchMapper.selectCommentList` |
| `sp_search_event_list` | 일정 검색 (제목·설명) | `SearchMapper.selectEventList` |

### 접속 상태 (`procedures/presence.sql` ↔ `Presence_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_user_update_last_active_at` | 마지막 접속 시각 갱신 | `PresenceMapper.updateLastActiveAt` |
| `sp_co_member_id_list` | 같은 워크스페이스에 있는 다른 사용자 번호 (중복 제외) | `PresenceMapper.selectCoMemberIds` |
| `sp_workspace_member_count` | 워크스페이스 멤버 여부 | `TaskMapper.selectWorkspaceMemberCount`, `PresenceMapper.selectMemberCount` |

### 만료 데이터 정리 (`procedures/cleanup.sql` ↔ `Cleanup_SQL.xml`)

| 프로시저 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|
| `sp_refresh_token_expired_delete` | 만료된 리프레시 토큰 삭제 | `CleanupMapper.deleteExpiredRefreshTokens` |
| `sp_password_token_expired_delete` | 만료된 비밀번호 재설정 토큰 삭제 | `CleanupMapper.deleteExpiredPasswordTokens` |
| `sp_email_token_expired_delete` | 만료된 이메일 인증 토큰 삭제 | `CleanupMapper.deleteExpiredEmailTokens` |
| `sp_invite_stale_delete` | 오래된 대기(PENDING) 초대 삭제 | `CleanupMapper.deleteStaleInvites` |

## 함께 고쳐야 하는 묶음

아래 프로시저들은 결과 컬럼(또는 조건)이 같고 찾는 기준만 달라요. 하나를 바꾸면 **묶음 전체를 함께** 고쳐야 해요.

| 프로시저 | 맞춰야 하는 것 | 결과를 받는 VO |
|---|---|---|
| `sp_sprint_progress_list`, `sp_sprint_progress` | 결과 컬럼 | `SprintProgressVO` |
| `sp_task_list`, `sp_task` | 결과 컬럼 | `TaskVO` |
| `sp_task_assignee_list`, `sp_task_assignee`, `sp_task_assignee_list_by_tasks` | 결과 컬럼 | `TaskAssigneeVO` |
| `sp_task_status_list`, `sp_task_status` | 결과 컬럼 | `TaskStatusVO` |
| `sp_task_attachment_list`, `sp_task_attachment` | 결과 컬럼 | `TaskAttachmentVO` |
| `sp_workspace_member_list`, `sp_workspace_member` | 결과 컬럼 | `WorkspaceMemberVO` |
| `sp_user_by_email`, `sp_user` | 결과 컬럼 | `UserVO` |
| `sp_notification_list`, `sp_notification` | 결과 컬럼 | `NotificationVO` |
| `sp_activity_list`, `sp_activity_count` | 조건 (`WHERE`) | `ActivityVO` / 개수 |
| `sp_retrospective_list`, `sp_retrospective` | 결과 컬럼 | `RetrospectiveVO` |
| `sp_comment`, `sp_comment_list_by_tasks`, `sp_comment_list_by_blocks` | 결과 컬럼 | `CommentVO` |
| `sp_page_list`, `sp_page` | 결과 컬럼 | `PageVO` |
| `sp_block_list`, `sp_block` | 결과 컬럼 | `BlockVO` |
| `sp_block_database_column_list`, `sp_block_database_column`, `sp_block_database_column_list_by_ids` | 결과 컬럼 | `BlockDatabaseColumnVO` |
| `sp_block_database_column_option_list`, `sp_block_database_option`, `sp_block_database_option_list_by_ids`, `sp_block_database_option_list` | 결과 컬럼 | `BlockDatabaseColumnOptionVO` |
| `sp_block_database_row_list`, `sp_block_database_row`, `sp_block_database_row_list_by_ids` | 결과 컬럼 | `BlockDatabaseRowVO` |
