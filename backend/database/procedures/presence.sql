-- 접속 상태 프로시저 (Presence_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/presence.sql"


-- 마지막 접속 시각 갱신
--   p_user_id         사용자 번호
--   p_last_active_at  접속 시각
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_user_update_last_active_at;

DELIMITER //

CREATE PROCEDURE sp_user_update_last_active_at(IN p_user_id BIGINT, IN p_last_active_at DATETIME)
    MODIFIES SQL DATA
BEGIN
    UPDATE users
       SET last_active_at = p_last_active_at
     WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 같은 워크스페이스에 있는 다른 사용자 번호 (중복 제외)
--   p_user_id  기준 사용자 번호
-- 결과: 사용자당 한 행 (user_id)

DROP PROCEDURE IF EXISTS sp_co_member_id_list;

DELIMITER //

CREATE PROCEDURE sp_co_member_id_list(IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT DISTINCT user_id
      FROM workspace_members
     WHERE workspace_id IN (SELECT workspace_id
                              FROM workspace_members
                             WHERE user_id = p_user_id)
       AND user_id <> p_user_id;
END //

DELIMITER ;


-- 워크스페이스 멤버 여부
--   p_workspace_id  워크스페이스 번호
--   p_user_id       사용자 번호
-- 결과: 1행 (멤버면 1, 아니면 0)

DROP PROCEDURE IF EXISTS sp_workspace_member_count;

DELIMITER //

CREATE PROCEDURE sp_workspace_member_count(IN p_workspace_id BIGINT, IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
      FROM workspace_members
     WHERE workspace_id = p_workspace_id
       AND user_id = p_user_id;
END //

DELIMITER ;
