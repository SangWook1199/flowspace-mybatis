-- 알림 한 건 (컬럼은 sp_notification_list와 동일, 실시간 전송용)
--   p_notification_id  알림 번호
-- 결과: 0행 또는 1행

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
      LEFT JOIN workspaces w ON w.workspace_id = n.workspace_id
      LEFT JOIN users a      ON a.user_id = n.actor_id
     WHERE n.notification_id = p_notification_id;
END //

DELIMITER ;
