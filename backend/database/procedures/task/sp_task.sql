-- 작업 한 건 + 상태 이름 (컬럼은 sp_task_list와 동일)
--   p_task_id  작업 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_task;

DELIMITER //

CREATE PROCEDURE sp_task(IN p_task_id BIGINT)
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
     WHERE t.task_id = p_task_id;
END //

DELIMITER ;
