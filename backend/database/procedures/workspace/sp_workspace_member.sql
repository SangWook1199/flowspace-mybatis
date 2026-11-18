-- 워크스페이스 멤버 한 명을 조회해요. 결과 모양은 sp_workspace_member_list와 같아요.
--   인자: p_workspace_id  워크스페이스 번호
--         p_user_id       사용자 번호
--   결과: 0행(그 워크스페이스 멤버가 아님) 또는 1행

DROP PROCEDURE IF EXISTS sp_workspace_member;

DELIMITER //

CREATE PROCEDURE sp_workspace_member(IN p_workspace_id BIGINT, IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT wm.workspace_id,
           u.user_id,
           u.nickname,
           u.email,
           u.bio,
           u.profile_file_id,
           f.file_url AS profile_image_url,
           u.last_active_at,
           wm.role,
           wm.joined_at
      FROM workspace_members wm
      JOIN users u ON u.user_id = wm.user_id
      LEFT JOIN files f ON f.file_id = u.profile_file_id
     WHERE wm.workspace_id = p_workspace_id
       AND wm.user_id = p_user_id;
END //

DELIMITER ;
