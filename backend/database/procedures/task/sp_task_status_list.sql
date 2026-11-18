-- 워크스페이스의 작업 상태(칸반 열) 목록을 조회해요.
-- 상태 정보(task_statuses)와 워크스페이스별 순서·WIP 제한(workspace_task_statuses)을 조인해요.
--   인자: p_workspace_id  워크스페이스 번호
--   결과: 상태 한 개당 한 행. 칸반 열 순서(position) 순.

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
