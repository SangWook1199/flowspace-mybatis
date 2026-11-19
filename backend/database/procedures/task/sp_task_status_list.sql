-- 워크스페이스 작업 상태(칸반 열) 목록 (task_statuses + workspace_task_statuses)
--   p_workspace_id  워크스페이스 번호
-- 결과: 상태당 한 행, position 순

DROP PROCEDURE IF EXISTS sp_task_status_list;

DELIMITER //

CREATE PROCEDURE sp_task_status_list(IN p_workspace_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT wts.workspace_id,
           ts.status_id,
           ts.name,
           ts.category,
           ts.color,
           wts.position,
           wts.is_default,
           wts.wip_limit
      FROM workspace_task_statuses wts
      JOIN task_statuses ts ON ts.status_id = wts.status_id
     WHERE wts.workspace_id = p_workspace_id
     ORDER BY wts.position;
END //

DELIMITER ;
