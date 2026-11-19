-- 칸반 작업 목록 + 상태 이름 (스프린트·백로그 공용)
--   p_workspace_id  워크스페이스 번호
--   p_sprint_id     스프린트 번호 (NULL이면 백로그, <=> 비교)
-- 결과: 작업당 한 행, position 순

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
           t.created_by,
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
       AND t.sprint_id <=> p_sprint_id
     ORDER BY t.position, t.task_id;
END //

DELIMITER ;
