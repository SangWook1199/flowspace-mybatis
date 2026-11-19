-- 워크스페이스 작업 상태 한 개 (컬럼은 sp_task_status_list와 동일, 연결되지 않은 상태면 0행)
--   p_workspace_id  워크스페이스 번호
--   p_status_id     상태 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_task_status;

DELIMITER //

CREATE PROCEDURE sp_task_status(IN p_workspace_id BIGINT, IN p_status_id BIGINT)
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
       AND wts.status_id = p_status_id;
END //

DELIMITER ;
