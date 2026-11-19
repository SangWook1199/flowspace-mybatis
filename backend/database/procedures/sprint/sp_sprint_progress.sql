-- 스프린트 한 건 + 진행 상황 (작업 수, 완료 작업 수)
--   p_sprint_id  스프린트 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_sprint_progress;

DELIMITER //

CREATE PROCEDURE sp_sprint_progress(IN p_sprint_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT s.sprint_id,
           s.workspace_id,
           s.created_by,
           s.name,
           s.goal,
           s.description,
           s.color,
           s.start_date,
           s.end_date,
           s.status,
           s.created_at,
           COUNT(t.task_id)                       AS task_count,
           COALESCE(SUM(ts.category = 'DONE'), 0) AS completed_task_count
      FROM sprints s
      LEFT JOIN tasks t          ON t.sprint_id = s.sprint_id
      LEFT JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE s.sprint_id = p_sprint_id
     GROUP BY s.sprint_id;
END //

DELIMITER ;
