-- 워크스페이스를 삭제해요.
--   인자: p_workspace_id  삭제할 워크스페이스 번호
--   소유자인지, 마지막 워크스페이스가 아닌지는 자바 서비스에서 먼저 확인해요.

DROP PROCEDURE IF EXISTS sp_delete_workspace;

DELIMITER //

CREATE PROCEDURE sp_delete_workspace(IN p_workspace_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    -- 1. 작업은 워크스페이스를 따라 지워지지 않아서(외래 키에 CASCADE가 없어요) 먼저 지워요.
    --    담당자·하위 작업·댓글·첨부 행은 작업을 따라 함께 지워져요.
    DELETE FROM tasks
     WHERE workspace_id = p_workspace_id;

    -- 2. 멤버·스프린트·페이지·파일·상태 연결 등은 외래 키(ON DELETE CASCADE)로 함께 지워져요.
    --    이 워크스페이스를 마지막으로 보던 사용자는 last_workspace_id가 NULL이 돼요(ON DELETE SET NULL).
    DELETE FROM workspaces
     WHERE workspace_id = p_workspace_id;
END //

DELIMITER ;
