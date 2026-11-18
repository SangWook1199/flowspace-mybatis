-- 칸반에 보여 줄 작업 목록과 상태 이름을 한 번에 조회해요.
-- 스프린트 작업과 백로그 작업을 같은 프로시저로 읽어요.
--   인자: p_workspace_id  워크스페이스 번호
--         p_sprint_id     스프린트 번호 (NULL이면 백로그)
--   결과: 작업 한 건당 한 행. 칸반 순서(position) 순.

DROP PROCEDURE IF EXISTS sp_task_list;

DELIMITER //

CREATE PROCEDURE sp_task_list(IN p_workspace_id BIGINT, IN p_sprint_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT t.task_id,
           t.task_number,
           t.workspace_id,
           t.sprint_id,
           t.status_id,
           ts.name AS status_name,
           t.position,
           t.title,
           t.description,
           t.start_date,
           t.end_date,
           t.priority,
           t.completed_at,
           t.created_at
      FROM tasks t
      JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE t.workspace_id = p_workspace_id
       AND t.sprint_id <=> p_sprint_id   -- <=> 는 NULL끼리도 같다고 봐요 (NULL이면 백로그)
     ORDER BY t.position, t.task_id;
END //

DELIMITER ;
