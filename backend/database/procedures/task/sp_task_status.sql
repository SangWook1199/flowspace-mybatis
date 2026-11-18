-- 워크스페이스의 작업 상태 한 개를 조회해요. 결과 모양은 sp_task_status_list와 같아요.
-- 그 워크스페이스에 연결된 상태가 아니면 결과가 없어요(다른 워크스페이스의 상태를 쓰지 못하게 확인할 때 써요).
--   인자: p_workspace_id  워크스페이스 번호
--         p_status_id     상태 번호
--   결과: 0행(없음) 또는 1행

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
