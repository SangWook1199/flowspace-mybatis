-- 스프린트 시작 (진행 중이던 다른 스프린트는 계획으로 변경)
-- 상태 순서 확인(계획 → 진행 중)은 Java에서 선처리
--   p_sprint_id  시작할 스프린트 번호

DROP PROCEDURE IF EXISTS sp_start_sprint;

DELIMITER //

CREATE PROCEDURE sp_start_sprint(IN p_sprint_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_workspace_id BIGINT;

    SELECT workspace_id INTO v_workspace_id
      FROM sprints
     WHERE sprint_id = p_sprint_id;

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
