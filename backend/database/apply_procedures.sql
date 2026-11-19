-- 프로시저를 DB에 적용하는 목록이에요. 프로시저 파일을 추가하면 여기에도 한 줄 추가해요.
-- 빈 DB로 처음 켤 때는 자동으로 실행되고, 이미 만들어진 DB에는 아래 명령으로 직접 적용해요.
--   docker compose exec mysql mysql -uroot -p flow_space -e "source /database/apply_procedures.sql"
-- 프로시저마다 하는 일은 같은 폴더의 README.md에 정리돼 있어요.

-- 스프린트
source /database/procedures/sprint/sp_sprint_progress_list.sql
source /database/procedures/sprint/sp_sprint_progress.sql
source /database/procedures/sprint/sp_start_sprint.sql
source /database/procedures/sprint/sp_complete_sprint.sql
source /database/procedures/sprint/sp_delete_sprint.sql

-- 작업
source /database/procedures/task/sp_task_list.sql
source /database/procedures/task/sp_task.sql
source /database/procedures/task/sp_task_assignee_list.sql
source /database/procedures/task/sp_task_assignee.sql
source /database/procedures/task/sp_delete_task.sql
source /database/procedures/task/sp_task_status_list.sql
source /database/procedures/task/sp_task_status.sql
source /database/procedures/task/sp_replace_default_task_status.sql
source /database/procedures/task/sp_delete_task_status.sql
source /database/procedures/task/sp_task_search.sql
source /database/procedures/task/sp_task_attachment_list.sql
source /database/procedures/task/sp_task_attachment.sql

-- 워크스페이스
source /database/procedures/workspace/sp_my_workspace_list.sql
source /database/procedures/workspace/sp_workspace_member_list.sql
source /database/procedures/workspace/sp_workspace_member.sql
source /database/procedures/workspace/sp_member_open_task_list.sql
source /database/procedures/workspace/sp_member_task_count.sql
source /database/procedures/workspace/sp_init_workspace.sql
source /database/procedures/workspace/sp_delete_workspace.sql
source /database/procedures/workspace/sp_my_invite_list.sql

-- 사용자
source /database/procedures/user/sp_user_by_email.sql
source /database/procedures/user/sp_user.sql
source /database/procedures/user/sp_withdraw_check.sql
source /database/procedures/user/sp_withdraw_user.sql
source /database/procedures/user/sp_user_lookup.sql

-- 알림
source /database/procedures/notification/sp_notification_list.sql
source /database/procedures/notification/sp_notification.sql
source /database/procedures/notification/sp_due_soon_target_list.sql

-- 활동 기록
source /database/procedures/activity/sp_activity_list.sql

-- 캘린더·일정
source /database/procedures/calendar/sp_event.sql
source /database/procedures/calendar/sp_calendar_task_list.sql

-- 댓글
source /database/procedures/comment/sp_comment_block.sql

-- 회고
source /database/procedures/retrospective/sp_create_retrospective.sql
source /database/procedures/retrospective/sp_retrospective_list.sql
source /database/procedures/retrospective/sp_retrospective.sql
source /database/procedures/retrospective/sp_task_snapshot_list.sql
source /database/procedures/retrospective/sp_task_snapshot_assignee_list.sql
source /database/procedures/retrospective/sp_subtask_snapshot_list.sql

-- 페이지
source /database/procedures/page/sp_page_list.sql
source /database/procedures/page/sp_page.sql
source /database/procedures/page/sp_page_subtree.sql

-- 블록
source /database/procedures/block/sp_block_list.sql
source /database/procedures/block/sp_block.sql
