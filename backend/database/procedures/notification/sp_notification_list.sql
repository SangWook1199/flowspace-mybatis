-- 내 알림 목록을 워크스페이스 이름, 보낸 사람 이름과 함께 최신순으로 조회해요.
--   인자: p_user_id  받는 사람 번호
--         p_offset   건너뛸 개수 (페이지 번호 × 페이지 크기)
--         p_limit    가져올 개수 (자바에서 다음 페이지가 있는지 보려고 한 개 더 달라고 해요)
--   결과: 알림 한 건당 한 행

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
      LEFT JOIN workspaces w ON w.workspace_id = n.workspace_id   -- 워크스페이스·보낸 사람이 없는 알림도 있어요
      LEFT JOIN users a      ON a.user_id = n.actor_id
     WHERE n.user_id = p_user_id
     ORDER BY n.created_at DESC, n.notification_id DESC
     LIMIT p_limit OFFSET p_offset;
END //

DELIMITER ;
