-- 스프린트를 시작해요. 진행 중이던 다른 스프린트는 계획됨으로 되돌려요.
-- 진행 중인 스프린트는 워크스페이스에 하나만 두기 때문에 두 UPDATE를 한 번에 처리해요.
--   인자: p_sprint_id  시작할 스프린트 번호
--   상태 순서 확인(계획됨 → 진행 중)은 자바 서비스에서 먼저 해요.

DROP PROCEDURE IF EXISTS sp_start_sprint;

DELIMITER //

CREATE PROCEDURE sp_start_sprint(IN p_sprint_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_workspace_id BIGINT;

    SELECT workspace_id INTO v_workspace_id
      FROM sprints
     WHERE sprint_id = p_sprint_id;

    -- 진행 중이던 다른 스프린트는 계획됨으로 (작업은 그 스프린트에 그대로 남아요)
    UPDATE sprints
       SET status = 'PLANNING'
     WHERE workspace_id = v_workspace_id
       AND status = 'ACTIVE'
       AND sprint_id <> p_sprint_id;

    UPDATE sprints
       SET status = 'ACTIVE'
     WHERE sprint_id = p_sprint_id;
END //

DELIMITER ;
