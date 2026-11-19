-- 회고 프로시저 (Retrospective_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/retrospective.sql"


-- 워크스페이스 스프린트 + 회고 목록 (회고가 없는 스프린트 포함)
--   p_workspace_id  워크스페이스 번호
-- 결과: 스프린트당 한 행 (회고가 없으면 retrospective_id, page_id는 NULL), 시작일 역순

DROP PROCEDURE IF EXISTS sp_retrospective_list;

DELIMITER //

CREATE PROCEDURE sp_retrospective_list(IN p_workspace_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT s.sprint_id,
           s.workspace_id,
           s.name        AS sprint_name,
           s.status      AS sprint_status,
           s.start_date,
           s.end_date,
           s.goal,
           r.retrospective_id,
           r.page_id,
           r.created_at
      FROM sprints s
      LEFT JOIN retrospectives r ON r.sprint_id = s.sprint_id
     WHERE s.workspace_id = p_workspace_id
     ORDER BY s.start_date DESC, s.sprint_id DESC;
END //

DELIMITER ;


-- 회고 한 건 + 스프린트 정보 (컬럼은 sp_retrospective_list와 동일)
--   p_retrospective_id  회고 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_retrospective;

DELIMITER //

CREATE PROCEDURE sp_retrospective(IN p_retrospective_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT s.sprint_id,
           s.workspace_id,
           s.name        AS sprint_name,
           s.status      AS sprint_status,
           s.start_date,
           s.end_date,
           s.goal,
           r.retrospective_id,
           r.page_id,
           r.created_at
      FROM retrospectives r
      JOIN sprints s ON s.sprint_id = r.sprint_id
     WHERE r.retrospective_id = p_retrospective_id;
END //

DELIMITER ;


-- 스프린트의 회고 번호
--   p_sprint_id  스프린트 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_retrospective_id_by_sprint;

DELIMITER //

CREATE PROCEDURE sp_retrospective_id_by_sprint(IN p_sprint_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT retrospective_id
    FROM retrospectives
    WHERE sprint_id = p_sprint_id;
END //

DELIMITER ;


-- 스프린트 존재 여부
--   p_sprint_id  스프린트 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_sprint_count;

DELIMITER //

CREATE PROCEDURE sp_sprint_count(IN p_sprint_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM sprints
    WHERE sprint_id = p_sprint_id;
END //

DELIMITER ;


-- 스프린트 완료 시 회고 생성
--   1) 회고 페이지  2) 회고  3) KPT 표 (블록 → 데이터베이스 → 열 3개 → 행 4개 → 셀 12개)
--   4) 상태 스냅샷  5) 작업 스냅샷  6) 담당자 스냅샷  7) 하위 작업 스냅샷
-- 한글·이모지 값은 문자 집합 문제를 피하기 위해 인자로 전달
--   p_sprint_id      완료하는 스프린트 번호
--   p_workspace_id   워크스페이스 번호
--   p_user_id        생성자 번호
--   p_page_title     회고 페이지 제목
--   p_page_icon      회고 페이지 아이콘
--   p_table_title    KPT 표 제목
--   p_page_id (OUT)  생성된 회고 페이지 번호

DROP PROCEDURE IF EXISTS sp_create_retrospective;

DELIMITER //

CREATE PROCEDURE sp_create_retrospective(IN p_sprint_id BIGINT, IN p_workspace_id BIGINT, IN p_user_id BIGINT,
                                         IN p_page_title VARCHAR(200), IN p_page_icon VARCHAR(20),
                                         IN p_table_title VARCHAR(100), OUT p_page_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_retrospective_id BIGINT;
    DECLARE v_block_id BIGINT;
    DECLARE v_database_id BIGINT;

    INSERT INTO pages (workspace_id, parent_page_id, title, icon, position, created_by, is_deleted, version)
    VALUES (p_workspace_id, NULL, p_page_title, p_page_icon, 0, p_user_id, FALSE, 0);
    SET p_page_id = LAST_INSERT_ID();

    INSERT INTO retrospectives (sprint_id, page_id)
    VALUES (p_sprint_id, p_page_id);
    SET v_retrospective_id = LAST_INSERT_ID();

    INSERT INTO blocks (page_id, type, position, created_by)
    VALUES (p_page_id, 'DATABASE', 0, p_user_id);
    SET v_block_id = LAST_INSERT_ID();

    INSERT INTO block_databases (block_id, title, view_type)
    VALUES (v_block_id, p_table_title, 'TABLE');
    SET v_database_id = LAST_INSERT_ID();

    INSERT INTO block_database_columns (database_id, name, type, position, width)
    VALUES (v_database_id, 'Keep', 'TEXT', 0, 200),
           (v_database_id, 'Problem', 'TEXT', 1, 200),
           (v_database_id, 'Try', 'TEXT', 2, 200);

    INSERT INTO block_database_rows (database_id, position)
    VALUES (v_database_id, 0), (v_database_id, 1), (v_database_id, 2), (v_database_id, 3);

    INSERT INTO block_database_cells (row_id, column_id, value)
    SELECT r.row_id, c.column_id, NULL
      FROM block_database_rows r
      CROSS JOIN block_database_columns c
     WHERE r.database_id = v_database_id
       AND c.database_id = v_database_id;

    INSERT INTO retrospective_status_snapshots (retrospective_id, original_status_id, name, color, position)
    SELECT v_retrospective_id, ts.status_id, ts.name, ts.color, wts.position
      FROM workspace_task_statuses wts
      JOIN task_statuses ts ON ts.status_id = wts.status_id
     WHERE wts.workspace_id = p_workspace_id
     ORDER BY wts.position;

    INSERT INTO task_snapshots (retrospective_id, snapshot_status_id, original_task_id, title, priority, position)
    SELECT v_retrospective_id, ss.snapshot_status_id, t.task_id, t.title, t.priority, t.position
      FROM tasks t
      JOIN retrospective_status_snapshots ss
        ON ss.retrospective_id = v_retrospective_id
       AND ss.original_status_id = t.status_id
     WHERE t.sprint_id = p_sprint_id
     ORDER BY t.position, t.task_id;

    INSERT INTO task_snapshot_assignees (snapshot_id, original_user_id, nickname, profile_file_id)
    SELECT s.snapshot_id, u.user_id, u.nickname, u.profile_file_id
      FROM task_snapshots s
      JOIN task_assignees ta ON ta.task_id = s.original_task_id
      JOIN users u           ON u.user_id = ta.user_id
     WHERE s.retrospective_id = v_retrospective_id
     ORDER BY ta.task_assignee_id;

    INSERT INTO subtask_snapshots (snapshot_id, original_subtask_id, content, is_completed, assignee_name,
                                   assignee_profile_file_id, position)
    SELECT s.snapshot_id, st.subtask_id, st.content, COALESCE(st.is_completed, FALSE), u.nickname,
           u.profile_file_id, COALESCE(st.position, 0)
      FROM task_snapshots s
      JOIN subtasks st    ON st.task_id = s.original_task_id
      LEFT JOIN users u   ON u.user_id = st.assignee_id
     WHERE s.retrospective_id = v_retrospective_id
     ORDER BY st.task_id, st.position;
END //

DELIMITER ;


-- 상태 스냅샷
--   p_retrospective_id  회고 번호
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_status_snapshot_list;

DELIMITER //

CREATE PROCEDURE sp_status_snapshot_list(IN p_retrospective_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT snapshot_status_id,
           retrospective_id,
           name,
           color,
           position
    FROM retrospective_status_snapshots
    WHERE retrospective_id = p_retrospective_id
    ORDER BY position, snapshot_status_id;
END //

DELIMITER ;


-- 회고 작업 스냅샷 (상태 이름 포함)
--   p_workspace_id      워크스페이스 번호
--   p_retrospective_id  회고 번호 (NULL이면 워크스페이스 전체, 회고 목록 요약용)
-- 결과: 스냅샷당 한 행, 상태 순서 → position 순

DROP PROCEDURE IF EXISTS sp_task_snapshot_list;

DELIMITER //

CREATE PROCEDURE sp_task_snapshot_list(IN p_workspace_id BIGINT, IN p_retrospective_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT s.snapshot_id,
           s.retrospective_id,
           s.snapshot_status_id,
           ss.name       AS status_name,
           s.title,
           s.priority,
           s.position
      FROM task_snapshots s
      JOIN retrospective_status_snapshots ss ON ss.snapshot_status_id = s.snapshot_status_id
      JOIN retrospectives r                  ON r.retrospective_id = s.retrospective_id
      JOIN sprints sp                        ON sp.sprint_id = r.sprint_id
     WHERE sp.workspace_id = p_workspace_id
       AND (p_retrospective_id IS NULL OR s.retrospective_id = p_retrospective_id)
     ORDER BY s.retrospective_id, ss.position, s.position, s.snapshot_id;
END //

DELIMITER ;


-- 작업 스냅샷 담당자 (프로필 사진 주소 포함, 범위는 sp_task_snapshot_list와 동일)
--   p_workspace_id      워크스페이스 번호
--   p_retrospective_id  회고 번호 (NULL이면 워크스페이스 전체)
-- 결과: 담당자당 한 행, snapshot_assignee_id 순

DROP PROCEDURE IF EXISTS sp_task_snapshot_assignee_list;

DELIMITER //

CREATE PROCEDURE sp_task_snapshot_assignee_list(IN p_workspace_id BIGINT, IN p_retrospective_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT a.snapshot_id,
           a.original_user_id,
           a.nickname,
           a.profile_file_id,
           f.file_url    AS profile_image_url
      FROM task_snapshot_assignees a
      JOIN task_snapshots s  ON s.snapshot_id = a.snapshot_id
      JOIN retrospectives r  ON r.retrospective_id = s.retrospective_id
      JOIN sprints sp        ON sp.sprint_id = r.sprint_id
      LEFT JOIN files f      ON f.file_id = a.profile_file_id
     WHERE sp.workspace_id = p_workspace_id
       AND (p_retrospective_id IS NULL OR s.retrospective_id = p_retrospective_id)
     ORDER BY a.snapshot_assignee_id;
END //

DELIMITER ;


-- 회고의 하위 작업 스냅샷 (담당자 사진 주소 포함)
--   p_retrospective_id  회고 번호
-- 결과: 하위 작업당 한 행, position 순

DROP PROCEDURE IF EXISTS sp_subtask_snapshot_list;

DELIMITER //

CREATE PROCEDURE sp_subtask_snapshot_list(IN p_retrospective_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT st.subtask_snapshot_id,
           st.snapshot_id,
           st.content,
           st.is_completed,
           st.assignee_name,
           st.assignee_profile_file_id,
           f.file_url    AS assignee_profile_image_url,
           st.position
      FROM subtask_snapshots st
      JOIN task_snapshots s  ON s.snapshot_id = st.snapshot_id
      LEFT JOIN files f      ON f.file_id = st.assignee_profile_file_id
     WHERE s.retrospective_id = p_retrospective_id
     ORDER BY st.position, st.subtask_snapshot_id;
END //

DELIMITER ;


-- 회고 페이지들의 TODO 블록
--   p_page_ids  회고 페이지 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_todo_block_list;

DELIMITER //

CREATE PROCEDURE sp_todo_block_list(IN p_page_ids JSON)
    READS SQL DATA
BEGIN
    SELECT page_id,
           content
    FROM blocks
    WHERE type = 'TODO'
      AND page_id IN (SELECT id FROM JSON_TABLE(p_page_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;
