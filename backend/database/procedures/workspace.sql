-- 워크스페이스 프로시저 (Workspace_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/workspace.sql"


-- 워크스페이스 한 건
--   p_workspace_id  워크스페이스 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_workspace;

DELIMITER //

CREATE PROCEDURE sp_workspace(IN p_workspace_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT workspace_id, owner_id, name, initials, color, icon, created_at
    FROM workspaces
    WHERE workspace_id = p_workspace_id;
END //

DELIMITER ;


-- 워크스페이스에서 사용자의 역할 (enum으로 매핑, 없으면 null)
--   p_workspace_id  워크스페이스 번호
--   p_user_id       사용자 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_workspace_member_role;

DELIMITER //

CREATE PROCEDURE sp_workspace_member_role(IN p_workspace_id BIGINT, IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT role
    FROM workspace_members
    WHERE workspace_id = p_workspace_id
      AND user_id = p_user_id;
END //

DELIMITER ;


-- 내 워크스페이스 목록 + 역할
--   p_user_id  사용자 번호
-- 결과: 워크스페이스당 한 행, 참여 순

DROP PROCEDURE IF EXISTS sp_my_workspace_list;

DELIMITER //

CREATE PROCEDURE sp_my_workspace_list(IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT w.workspace_id,
           w.owner_id,
           w.name,
           w.initials,
           w.color,
           w.icon,
           w.created_at,
           wm.role
      FROM workspace_members wm
      JOIN workspaces w ON w.workspace_id = wm.workspace_id
     WHERE wm.user_id = p_user_id
     ORDER BY wm.member_id;
END //

DELIMITER ;


-- 워크스페이스 멤버 목록 (이름·프로필 이미지·역할 포함)
--   p_workspace_id  워크스페이스 번호
-- 결과: 멤버당 한 행, 참여 순

DROP PROCEDURE IF EXISTS sp_workspace_member_list;

DELIMITER //

CREATE PROCEDURE sp_workspace_member_list(IN p_workspace_id BIGINT)
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
     ORDER BY wm.member_id;
END //

DELIMITER ;


-- 워크스페이스 멤버 한 명 (컬럼은 sp_workspace_member_list와 동일)
--   p_workspace_id  워크스페이스 번호
--   p_user_id       사용자 번호
-- 결과: 0행 또는 1행

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


-- 멤버의 미완료 담당 작업 (프로필 카드용)
--   p_workspace_id  워크스페이스 번호
--   p_user_id       사용자 번호
--   p_limit         최대 개수
-- 결과: 마감일 순 (마감일 없음은 맨 뒤, 그 안에서는 최신 생성 순)

DROP PROCEDURE IF EXISTS sp_member_open_task_list;

DELIMITER //

CREATE PROCEDURE sp_member_open_task_list(IN p_workspace_id BIGINT, IN p_user_id BIGINT, IN p_limit INT)
    READS SQL DATA
BEGIN
    SELECT t.task_id,
           t.sprint_id,
           t.title,
           t.end_date,
           t.priority,
           ts.name AS status_name
      FROM task_assignees ta
      JOIN tasks t ON t.task_id = ta.task_id
      JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE ta.user_id = p_user_id
       AND t.workspace_id = p_workspace_id
       AND ts.category <> 'DONE'
     ORDER BY t.end_date IS NULL,
              t.end_date,
              t.task_id DESC
     LIMIT p_limit;
END //

DELIMITER ;


-- 멤버 담당 작업의 진행 중·완료 수 (프로필 카드용)
--   p_workspace_id  워크스페이스 번호
--   p_user_id       사용자 번호
-- 결과: 항상 1행

DROP PROCEDURE IF EXISTS sp_member_task_count;

DELIMITER //

CREATE PROCEDURE sp_member_task_count(IN p_workspace_id BIGINT, IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COALESCE(SUM(ts.category <> 'DONE'), 0) AS open_task_count,
           COALESCE(SUM(ts.category = 'DONE'), 0)  AS done_task_count
      FROM task_assignees ta
      JOIN tasks t ON t.task_id = ta.task_id
      JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE ta.user_id = p_user_id
       AND t.workspace_id = p_workspace_id;
END //

DELIMITER ;


-- 내가 속한 워크스페이스 수
--   p_user_id  사용자 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_my_workspace_count;

DELIMITER //

CREATE PROCEDURE sp_my_workspace_count(IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM workspace_members
    WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 워크스페이스 등록 (생성 시각은 DB 기본값)
--   p_owner_id      소유자 번호
--   p_name          이름
--   p_initials      머리글자
--   p_color         색상
--   p_icon          아이콘
--   p_workspace_id  새 번호
-- 결과: 없음 (p_workspace_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_workspace_insert;

DELIMITER //

CREATE PROCEDURE sp_workspace_insert(IN p_owner_id BIGINT, IN p_name VARCHAR(100), IN p_initials VARCHAR(4),
                                     IN p_color VARCHAR(50), IN p_icon VARCHAR(20), OUT p_workspace_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO workspaces (owner_id, name, initials, color, icon)
    VALUES (p_owner_id, p_name, p_initials, p_color, p_icon);

    SET p_workspace_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 새 워크스페이스에 소유자와 기본 작업 상태 3개(할 일·진행 중·완료) 연결
-- 워크스페이스 행은 Java에서 먼저 등록, 기본 상태 1~3번은 공용 행 (ddl.sql)
--   p_workspace_id  워크스페이스 번호
--   p_owner_id      생성자 번호

DROP PROCEDURE IF EXISTS sp_init_workspace;

DELIMITER //

CREATE PROCEDURE sp_init_workspace(IN p_workspace_id BIGINT, IN p_owner_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO workspace_members (workspace_id, user_id, role)
    VALUES (p_workspace_id, p_owner_id, 'OWNER');

    INSERT INTO workspace_task_statuses (workspace_id, status_id, position, is_default)
    VALUES (p_workspace_id, 1, 0, TRUE),
           (p_workspace_id, 2, 1, TRUE),
           (p_workspace_id, 3, 2, TRUE);
END //

DELIMITER ;


-- 워크스페이스 수정
--   p_name          이름
--   p_initials      머리글자
--   p_color         색상
--   p_icon          아이콘
--   p_workspace_id  워크스페이스 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_workspace_update;

DELIMITER //

CREATE PROCEDURE sp_workspace_update(IN p_name VARCHAR(100), IN p_initials VARCHAR(4), IN p_color VARCHAR(50),
                                     IN p_icon VARCHAR(20), IN p_workspace_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE workspaces
       SET name     = p_name,
           initials = p_initials,
           color    = p_color,
           icon     = p_icon
     WHERE workspace_id = p_workspace_id;
END //

DELIMITER ;


-- 워크스페이스 삭제 (작업은 CASCADE가 없어 먼저 삭제, 나머지는 ON DELETE CASCADE / SET NULL)
-- 소유자 여부와 마지막 워크스페이스 여부는 Java에서 선처리
--   p_workspace_id  삭제할 워크스페이스 번호

DROP PROCEDURE IF EXISTS sp_delete_workspace;

DELIMITER //

CREATE PROCEDURE sp_delete_workspace(IN p_workspace_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM tasks
     WHERE workspace_id = p_workspace_id;

    DELETE FROM workspaces
     WHERE workspace_id = p_workspace_id;
END //

DELIMITER ;


-- 대기 중인 초대 수
--   p_workspace_id  워크스페이스 번호
--   p_email         이메일
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_pending_invite_count;

DELIMITER //

CREATE PROCEDURE sp_pending_invite_count(IN p_workspace_id BIGINT, IN p_email VARCHAR(100))
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM workspace_invites
    WHERE workspace_id = p_workspace_id
      AND email = p_email
      AND status = 'PENDING';
END //

DELIMITER ;


-- 초대 등록 (생성 시각은 DB 기본값)
--   p_workspace_id  워크스페이스 번호
--   p_inviter_id    초대한 사용자 번호
--   p_email         이메일
--   p_status        초대 상태
--   p_invite_id     새 번호
-- 결과: 없음 (p_invite_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_invite_insert;

DELIMITER //

CREATE PROCEDURE sp_invite_insert(IN p_workspace_id BIGINT, IN p_inviter_id BIGINT, IN p_email VARCHAR(100),
                                  IN p_status VARCHAR(50), OUT p_invite_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO workspace_invites (workspace_id, inviter_id, email, status)
    VALUES (p_workspace_id, p_inviter_id, p_email, p_status);

    SET p_invite_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 받은 초대 중 대기 중인 초대 (워크스페이스·초대한 사람 이름 포함)
--   p_email  내 이메일 (초대는 이메일 기준 저장)
-- 결과: 초대당 한 행, 받은 순

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


-- 초대 한 건
--   p_invite_id  초대 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_invite;

DELIMITER //

CREATE PROCEDURE sp_invite(IN p_invite_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT invite_id, workspace_id, inviter_id, email, status, created_at
    FROM workspace_invites
    WHERE invite_id = p_invite_id;
END //

DELIMITER ;


-- 초대 응답 처리
--   p_status     초대 상태
--   p_invite_id  초대 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_invite_status_update;

DELIMITER //

CREATE PROCEDURE sp_invite_status_update(IN p_status VARCHAR(50), IN p_invite_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE workspace_invites
       SET status       = p_status,
           responded_at = NOW()
     WHERE invite_id = p_invite_id;
END //

DELIMITER ;


-- 멤버 추가 (참여 시각은 DB 기본값)
--   p_workspace_id  워크스페이스 번호
--   p_user_id       사용자 번호
--   p_role          역할
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_workspace_member_insert;

DELIMITER //

CREATE PROCEDURE sp_workspace_member_insert(IN p_workspace_id BIGINT, IN p_user_id BIGINT,
                                            IN p_role VARCHAR(50))
    MODIFIES SQL DATA
BEGIN
    INSERT INTO workspace_members (workspace_id, user_id, role)
    VALUES (p_workspace_id, p_user_id, p_role);
END //

DELIMITER ;


-- 멤버 역할 변경
--   p_role          역할
--   p_workspace_id  워크스페이스 번호
--   p_user_id       사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_workspace_member_role_update;

DELIMITER //

CREATE PROCEDURE sp_workspace_member_role_update(IN p_role VARCHAR(50), IN p_workspace_id BIGINT,
                                                 IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE workspace_members
       SET role = p_role
     WHERE workspace_id = p_workspace_id
       AND user_id = p_user_id;
END //

DELIMITER ;


-- 워크스페이스 소유자 변경
--   p_owner_id      소유자 번호
--   p_workspace_id  워크스페이스 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_workspace_owner_update;

DELIMITER //

CREATE PROCEDURE sp_workspace_owner_update(IN p_owner_id BIGINT, IN p_workspace_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE workspaces
       SET owner_id = p_owner_id
     WHERE workspace_id = p_workspace_id;
END //

DELIMITER ;


-- 멤버 삭제
--   p_workspace_id  워크스페이스 번호
--   p_user_id       사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_workspace_member_delete;

DELIMITER //

CREATE PROCEDURE sp_workspace_member_delete(IN p_workspace_id BIGINT, IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM workspace_members
    WHERE workspace_id = p_workspace_id
      AND user_id = p_user_id;
END //

DELIMITER ;


-- 사용자의 마지막 워크스페이스 지정 (워크스페이스 생성 시 함께 변경)
--   p_workspace_id  워크스페이스 번호
--   p_user_id       사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_user_last_workspace_update;

DELIMITER //

CREATE PROCEDURE sp_user_last_workspace_update(IN p_workspace_id BIGINT, IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE users
       SET last_workspace_id = p_workspace_id
     WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 마지막 워크스페이스가 이곳이면 해제
--   p_user_id       사용자 번호
--   p_workspace_id  워크스페이스 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_user_last_workspace_clear;

DELIMITER //

CREATE PROCEDURE sp_user_last_workspace_clear(IN p_user_id BIGINT, IN p_workspace_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE users
       SET last_workspace_id = NULL
     WHERE user_id = p_user_id
       AND last_workspace_id = p_workspace_id;
END //

DELIMITER ;
