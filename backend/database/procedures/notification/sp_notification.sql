-- 알림 한 건을 조회해요. 결과 모양은 sp_notification_list와 같아요(방금 만든 알림을 실시간으로 보낼 때 써요).
--   인자: p_notification_id  알림 번호
--   결과: 0행(없음) 또는 1행

DROP PROCEDURE IF EXISTS sp_notification;

DELIMITER //

CREATE PROCEDURE sp_notification(IN p_notification_id BIGINT)
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
     WHERE n.notification_id = p_notification_id;
END //

DELIMITER ;
