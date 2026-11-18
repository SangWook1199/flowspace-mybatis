-- 내가 받은, 아직 답하지 않은 초대를 워크스페이스 이름과 초대한 사람 이름과 함께 조회해요.
--   인자: p_email  내 이메일 (초대는 가입 전에도 보낼 수 있게 이메일로 저장돼요)
--   결과: 초대 한 건당 한 행. 받은 순서대로.

DROP PROCEDURE IF EXISTS sp_my_invite_list;

DELIMITER //

CREATE PROCEDURE sp_my_invite_list(IN p_email VARCHAR(100))
    READS SQL DATA
BEGIN
    SELECT wi.invite_id,
           wi.workspace_id,
           wi.inviter_id,
           wi.email,
           wi.status,
           wi.created_at,
           w.name     AS workspace_name,
           w.initials AS workspace_initials,
           w.color    AS workspace_color,
           u.nickname AS inviter_name
      FROM workspace_invites wi
      JOIN workspaces w ON w.workspace_id = wi.workspace_id
      JOIN users u      ON u.user_id = wi.inviter_id
     WHERE wi.email = p_email
       AND wi.status = 'PENDING'
     ORDER BY wi.invite_id;
END //

DELIMITER ;
