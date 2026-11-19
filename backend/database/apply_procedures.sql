-- 프로시저 적용 목록 (Mapper XML 하나당 파일 하나)
-- 빈 DB 최초 실행 시 자동 적용, 기존 DB는 아래 명령으로 적용
--   docker compose exec mysql mysql -uroot -p flow_space -e "source /database/apply_procedures.sql"
-- 프로시저 설명: README.md

source /database/procedures/sprint.sql
source /database/procedures/task.sql
source /database/procedures/task_attachment.sql
source /database/procedures/workspace.sql
source /database/procedures/user.sql
source /database/procedures/notification.sql
source /database/procedures/activity.sql
source /database/procedures/calendar.sql
source /database/procedures/comment.sql
source /database/procedures/file.sql
source /database/procedures/retrospective.sql
source /database/procedures/page.sql
source /database/procedures/page_copy.sql
source /database/procedures/block.sql
source /database/procedures/block_database.sql
source /database/procedures/search.sql
source /database/procedures/presence.sql
