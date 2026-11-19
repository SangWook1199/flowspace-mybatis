-- 워크스페이스 스프린트 목록 + 진행 상황 (작업 수, 완료 작업 수)
--   p_workspace_id  워크스페이스 번호
-- 결과: 스프린트당 한 행, 시작일 역순

DROP PROCEDURE IF EXISTS sp_sprint_progress_list;

DELIMITER //

CREATE PROCEDURE sp_sprint_progress_list(IN p_workspace_id BIGINT)
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
     WHERE s.workspace_id = p_workspace_id
     GROUP BY s.sprint_id
     ORDER BY s.start_date DESC;
END //

DELIMITER ;
