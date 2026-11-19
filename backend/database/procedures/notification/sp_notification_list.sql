-- 내 알림 목록 (최신순, 워크스페이스·보낸 사람 이름 포함)
--   p_user_id  받는 사람 번호
--   p_offset   건너뛸 개수
--   p_limit    가져올 개수 (다음 페이지 확인용으로 1개 추가 요청)
-- 결과: 알림당 한 행

DROP PROCEDURE IF EXISTS sp_notification_list;

DELIMITER //

CREATE PROCEDURE sp_notification_list(IN p_user_id BIGINT, IN p_offset INT, IN p_limit INT)
    READS SQL DATA
BEGIN
    SELECT n.notification_id,
           n.user_id,
           n.actor_id,
           n.workspace_id,
           n.type,
           n.message,
           n.ref_id,
           n.link_path,
           n.is_read,
           n.created_at,
           w.name     AS workspace_name,
           a.nickname AS actor_name
      FROM notifications n
      LEFT JOIN workspaces w ON w.workspace_id = n.workspace_id
      LEFT JOIN users a      ON a.user_id = n.actor_id
     WHERE n.user_id = p_user_id
     ORDER BY n.created_at DESC, n.notification_id DESC
     LIMIT p_limit OFFSET p_offset;
END //

DELIMITER ;
