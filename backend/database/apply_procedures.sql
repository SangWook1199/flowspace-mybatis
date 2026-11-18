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
