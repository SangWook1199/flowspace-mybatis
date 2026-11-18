-- 회원 탈퇴를 처리해요. 사용자 행은 지우지 않고 개인정보만 지워서 "탈퇴한 사용자"로 남겨요
-- (다른 멤버들이 보는 작업·댓글·활동 기록이 이 사용자를 가리키고 있어서요).
--   인자: p_user_id   탈퇴할 사용자 번호
--         p_nickname  탈퇴 후 보일 이름 (자바의 User.WITHDRAWN_NICKNAME을 넘겨요 — 이름을 한 곳에서만 정하려고요)
--   본인 확인과 "다른 멤버가 있는 내 워크스페이스가 없는지" 확인은 자바 서비스에서 먼저 해요.
--   그래서 여기까지 오면 내가 소유한 워크스페이스는 모두 나 혼자 쓰는 곳이에요.

DROP PROCEDURE IF EXISTS sp_withdraw_user;

DELIMITER //

CREATE PROCEDURE sp_withdraw_user(IN p_user_id BIGINT, IN p_nickname VARCHAR(30))
    MODIFIES SQL DATA
BEGIN
    DECLARE v_email VARCHAR(100);

    -- 받은 초대를 지우려면 바꾸기 전 이메일이 필요해요
    SELECT email INTO v_email
      FROM users
     WHERE user_id = p_user_id;

    -- 1. 다른 사람의 워크스페이스에서 나가요
    DELETE FROM workspace_members
     WHERE user_id = p_user_id
       AND role <> 'OWNER';

    -- 2. 나 혼자 쓰던 내 워크스페이스를 지워요 (작업은 CASCADE가 없어서 먼저 지워요)
    DELETE FROM tasks
     WHERE workspace_id IN (SELECT workspace_id FROM workspaces WHERE owner_id = p_user_id);

    DELETE FROM workspaces
     WHERE owner_id = p_user_id;

    -- 3. 나와 연결된 기록을 지워요 (탈퇴한 계정으로 다시 로그인되거나 알림이 남지 않게)
    DELETE FROM task_assignees        WHERE user_id = p_user_id;
    DELETE FROM notifications         WHERE user_id = p_user_id;
    DELETE FROM refresh_tokens        WHERE user_id = p_user_id;
    DELETE FROM user_social_accounts  WHERE user_id = p_user_id;
    DELETE FROM password_reset_tokens WHERE user_id = p_user_id;

    -- 이 이메일로 온, 아직 답하지 않은 초대는 받을 사람이 없어요
    DELETE FROM workspace_invites
     WHERE email = v_email
       AND status = 'PENDING';

    -- 4. 개인정보를 지워요. 이메일은 유일해야 해서 번호가 들어간 값으로 바꿔 두면, 같은 이메일로 다시 가입할 수 있어요.
    UPDATE users
       SET email             = CONCAT('withdrawn-', p_user_id, '@deleted.flowspace'),
           nickname          = p_nickname,
           password          = NULL,
           provider_id       = NULL,
           bio               = NULL,
           profile_file_id   = NULL,
           last_workspace_id = NULL,
           deleted_at        = NOW()
     WHERE user_id = p_user_id;
END //

DELIMITER ;
