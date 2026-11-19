-- 회원 탈퇴 (사용자 행은 유지하고 개인정보만 삭제, 작업·댓글·활동 기록 보존)
--   1) 다른 사용자의 워크스페이스에서 나가기  2) 단독 소유 워크스페이스 삭제
--   3) 연결된 기록 삭제 (작업 담당, 알림, 토큰, 소셜 계정, 대기 중인 초대)  4) 개인정보 익명 처리 (이메일은 재가입 가능한 값으로 변경)
-- 본인 확인과 다른 멤버가 있는 소유 워크스페이스 확인은 Java에서 선처리
--   p_user_id   탈퇴할 사용자 번호
--   p_nickname  탈퇴 후 표시 이름 (User.WITHDRAWN_NICKNAME)

DROP PROCEDURE IF EXISTS sp_withdraw_user;

DELIMITER //

CREATE PROCEDURE sp_withdraw_user(IN p_user_id BIGINT, IN p_nickname VARCHAR(30))
    MODIFIES SQL DATA
BEGIN
    DECLARE v_email VARCHAR(100);

    SELECT email INTO v_email
      FROM users
     WHERE user_id = p_user_id;

    DELETE FROM workspace_members
     WHERE user_id = p_user_id
       AND role <> 'OWNER';

    DELETE FROM tasks
     WHERE workspace_id IN (SELECT workspace_id FROM workspaces WHERE owner_id = p_user_id);

    DELETE FROM workspaces
     WHERE owner_id = p_user_id;

    DELETE FROM task_assignees        WHERE user_id = p_user_id;
    DELETE FROM notifications         WHERE user_id = p_user_id;
    DELETE FROM refresh_tokens        WHERE user_id = p_user_id;
    DELETE FROM user_social_accounts  WHERE user_id = p_user_id;
    DELETE FROM password_reset_tokens WHERE user_id = p_user_id;

    DELETE FROM workspace_invites
     WHERE email = v_email
       AND status = 'PENDING';

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
