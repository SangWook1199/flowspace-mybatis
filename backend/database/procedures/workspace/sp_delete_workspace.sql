-- 워크스페이스 삭제 (작업은 CASCADE가 없어 먼저 삭제, 나머지는 ON DELETE CASCADE / SET NULL)
-- 소유자 여부와 마지막 워크스페이스 여부는 Java에서 선처리
--   p_workspace_id  삭제할 워크스페이스 번호

DROP PROCEDURE IF EXISTS sp_delete_workspace;

DELIMITER //

CREATE PROCEDURE sp_delete_workspace(IN p_workspace_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM tasks
     WHERE workspace_id = p_workspace_id;

    DELETE FROM workspaces
     WHERE workspace_id = p_workspace_id;
END //

DELIMITER ;
