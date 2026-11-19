-- 알림 프로시저 (Notification_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/notification.sql"


-- 알림 등록 (읽음 여부·생성 시각은 DB 기본값)
--   p_user_id          받는 사람 번호
--   p_actor_id         보낸 사람 번호 (시스템 알림이면 NULL)
--   p_workspace_id     워크스페이스 번호
--   p_type             알림 종류
--   p_message          알림 내용
--   p_ref_id           대상 번호
--   p_link_path        눌렀을 때 이동할 경로
--   p_notification_id  새 번호
-- 결과: 없음 (p_notification_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_notification_insert;

DELIMITER //

CREATE PROCEDURE sp_notification_insert(IN p_user_id BIGINT, IN p_actor_id BIGINT, IN p_workspace_id BIGINT,
                                        IN p_type VARCHAR(30), IN p_message VARCHAR(300), IN p_ref_id BIGINT,
                                        IN p_link_path VARCHAR(255), OUT p_notification_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO notifications (user_id, actor_id, workspace_id, type, message, ref_id, link_path)
    VALUES (p_user_id, p_actor_id, p_workspace_id, p_type, p_message, p_ref_id, p_link_path);

    SET p_notification_id = LAST_INSERT_ID();
END //

DELIMITER ;


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


-- 읽지 않은 알림 수
--   p_user_id  받는 사람 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_notification_unread_count;

DELIMITER //

CREATE PROCEDURE sp_notification_unread_count(IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM notifications
    WHERE user_id = p_user_id
      AND is_read = FALSE;
END //

DELIMITER ;


-- 알림 수신자 번호
--   p_notification_id  알림 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_notification_user_id;

DELIMITER //

CREATE PROCEDURE sp_notification_user_id(IN p_notification_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT user_id
    FROM notifications
    WHERE notification_id = p_notification_id;
END //

DELIMITER ;


-- 알림 한 건 읽음
--   p_notification_id  알림 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_notification_read_update;

DELIMITER //

CREATE PROCEDURE sp_notification_read_update(IN p_notification_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE notifications
       SET is_read = TRUE
     WHERE notification_id = p_notification_id;
END //

DELIMITER ;


-- 내 알림 모두 읽음
--   p_user_id  받는 사람 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_notification_read_all_update;

DELIMITER //

CREATE PROCEDURE sp_notification_read_all_update(IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE notifications
       SET is_read = TRUE
     WHERE user_id = p_user_id
       AND is_read = FALSE;
END //

DELIMITER ;


-- 특정 대상에 대한 내 알림 읽음
--   p_user_id  받는 사람 번호
--   p_type     알림 종류
--   p_ref_id   대상 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_notification_read_by_ref_update;

DELIMITER //

CREATE PROCEDURE sp_notification_read_by_ref_update(IN p_user_id BIGINT, IN p_type VARCHAR(30),
                                                    IN p_ref_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE notifications
       SET is_read = TRUE
     WHERE user_id = p_user_id
       AND type = p_type
       AND ref_id = p_ref_id
       AND is_read = FALSE;
END //

DELIMITER ;


-- 알림 한 건 삭제
--   p_notification_id  알림 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_notification_delete;

DELIMITER //

CREATE PROCEDURE sp_notification_delete(IN p_notification_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM notifications
    WHERE notification_id = p_notification_id;
END //

DELIMITER ;


-- 보관 기간이 지난 알림 삭제
--   p_before  이 시각 이전에 만든 알림 삭제
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_notification_old_delete;

DELIMITER //

CREATE PROCEDURE sp_notification_old_delete(IN p_before DATETIME)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM notifications
    WHERE created_at < p_before;
END //

DELIMITER ;


-- 마감 임박 알림 대상 (해당 마감일의 미완료 작업과 담당자, 오늘 이미 보낸 대상 제외)
--   p_end_date  마감일 (오늘 또는 내일)
--   p_since     이 시각 이후 발송분은 발송 완료로 간주 (오늘 0시)
-- 결과: (담당자, 작업)당 한 행

DROP PROCEDURE IF EXISTS sp_due_soon_target_list;

DELIMITER //

CREATE PROCEDURE sp_due_soon_target_list(IN p_end_date DATE, IN p_since DATETIME)
    READS SQL DATA
BEGIN
    SELECT ta.user_id,
           t.task_id,
           t.workspace_id,
           t.sprint_id,
           t.title
      FROM tasks t
      JOIN task_statuses ts  ON ts.status_id = t.status_id
      JOIN task_assignees ta ON ta.task_id = t.task_id
     WHERE t.end_date = p_end_date
       AND ts.category <> 'DONE'
       AND NOT EXISTS (SELECT 1
                         FROM notifications n
                        WHERE n.type = 'TASK_DUE_SOON'
                          AND n.user_id = ta.user_id
                          AND n.ref_id = t.task_id
                          AND n.created_at > p_since)
     ORDER BY t.task_id, ta.task_assignee_id;
END //

DELIMITER ;
