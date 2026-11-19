-- 스프린트 프로시저 (Sprint_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/sprint.sql"


-- 스프린트 한 건
--   p_sprint_id  스프린트 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_sprint;

DELIMITER //

CREATE PROCEDURE sp_sprint(IN p_sprint_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT sprint_id, workspace_id, created_by, name, goal, description,
           color, start_date, end_date, status, created_at
    FROM sprints
    WHERE sprint_id = p_sprint_id;
END //

DELIMITER ;


-- 워크스페이스 스프린트 목록 + 진행 상황 (작업 수, 완료 작업 수)
--   p_workspace_id  워크스페이스 번호
-- 결과: 스프린트당 한 행, 시작일 역순

DROP PROCEDURE IF EXISTS sp_sprint_progress_list;

DELIMITER //

CREATE PROCEDURE sp_sprint_progress_list(IN p_workspace_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT s.sprint_id,
           s.workspace_id,
           s.created_by,
           s.name,
           s.goal,
           s.description,
           s.color,
           s.start_date,
           s.end_date,
           s.status,
           s.created_at,
           COUNT(t.task_id)                       AS task_count,
           COALESCE(SUM(ts.category = 'DONE'), 0) AS completed_task_count
      FROM sprints s
      LEFT JOIN tasks t          ON t.sprint_id = s.sprint_id
      LEFT JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE s.workspace_id = p_workspace_id
     GROUP BY s.sprint_id
     ORDER BY s.start_date DESC;
END //

DELIMITER ;


-- 스프린트 한 건 + 진행 상황 (작업 수, 완료 작업 수)
--   p_sprint_id  스프린트 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_sprint_progress;

DELIMITER //

CREATE PROCEDURE sp_sprint_progress(IN p_sprint_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT s.sprint_id,
           s.workspace_id,
           s.created_by,
           s.name,
           s.goal,
           s.description,
           s.color,
           s.start_date,
           s.end_date,
           s.status,
           s.created_at,
           COUNT(t.task_id)                       AS task_count,
           COALESCE(SUM(ts.category = 'DONE'), 0) AS completed_task_count
      FROM sprints s
      LEFT JOIN tasks t          ON t.sprint_id = s.sprint_id
      LEFT JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE s.sprint_id = p_sprint_id
     GROUP BY s.sprint_id;
END //

DELIMITER ;


-- 워크스페이스에서 해당 상태인 스프린트 수
--   p_workspace_id  워크스페이스 번호
--   p_status        스프린트 상태
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_sprint_count_by_status;

DELIMITER //

CREATE PROCEDURE sp_sprint_count_by_status(IN p_workspace_id BIGINT, IN p_status VARCHAR(50))
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM sprints
    WHERE workspace_id = p_workspace_id
      AND status = p_status;
END //

DELIMITER ;


-- 스프린트 등록 (생성 시각은 DB 기본값)
--   p_workspace_id  워크스페이스 번호
--   p_created_by    만든 사용자 번호
--   p_name          이름
--   p_goal          목표
--   p_description   설명
--   p_color         색상
--   p_start_date    시작일
--   p_end_date      종료일
--   p_status        스프린트 상태
--   p_sprint_id     새 번호
-- 결과: 없음 (p_sprint_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_sprint_insert;

DELIMITER //

CREATE PROCEDURE sp_sprint_insert(IN p_workspace_id BIGINT, IN p_created_by BIGINT, IN p_name VARCHAR(100),
                                  IN p_goal TEXT, IN p_description TEXT, IN p_color VARCHAR(50),
                                  IN p_start_date DATE, IN p_end_date DATE, IN p_status VARCHAR(50),
                                  OUT p_sprint_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO sprints (workspace_id, created_by, name, goal, description,
                         color, start_date, end_date, status)
    VALUES (p_workspace_id, p_created_by, p_name, p_goal, p_description,
            p_color, p_start_date, p_end_date, p_status);

    SET p_sprint_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 스프린트 수정 (상태 제외)
--   p_name         이름
--   p_goal         목표
--   p_description  설명
--   p_color        색상
--   p_start_date   시작일
--   p_end_date     종료일
--   p_sprint_id    스프린트 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_sprint_update;

DELIMITER //

CREATE PROCEDURE sp_sprint_update(IN p_name VARCHAR(100), IN p_goal TEXT, IN p_description TEXT,
                                  IN p_color VARCHAR(50), IN p_start_date DATE, IN p_end_date DATE,
                                  IN p_sprint_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE sprints
       SET name        = p_name,
           goal        = p_goal,
           description = p_description,
           color       = p_color,
           start_date  = p_start_date,
           end_date    = p_end_date
     WHERE sprint_id = p_sprint_id;
END //

DELIMITER ;


-- 스프린트 시작 (진행 중이던 다른 스프린트는 계획으로 변경)
-- 상태 순서 확인(계획 → 진행 중)은 Java에서 선처리
--   p_sprint_id  시작할 스프린트 번호

DROP PROCEDURE IF EXISTS sp_start_sprint;

DELIMITER //

CREATE PROCEDURE sp_start_sprint(IN p_sprint_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_workspace_id BIGINT;

    SELECT workspace_id INTO v_workspace_id
      FROM sprints
     WHERE sprint_id = p_sprint_id;

    UPDATE sprints
       SET status = 'PLANNING'
     WHERE workspace_id = v_workspace_id
       AND status = 'ACTIVE'
       AND sprint_id <> p_sprint_id;

    UPDATE sprints
       SET status = 'ACTIVE'
     WHERE sprint_id = p_sprint_id;
END //

DELIMITER ;


-- 스프린트 완료 (미완료 작업은 백로그 맨 뒤로 이동, 완료 작업은 유지)
-- 상태 순서 확인과 회고 생성은 Java에서 선처리
--   p_sprint_id  완료할 스프린트 번호

DROP PROCEDURE IF EXISTS sp_complete_sprint;

DELIMITER //

CREATE PROCEDURE sp_complete_sprint(IN p_sprint_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_workspace_id BIGINT;
    DECLARE v_backlog_count INT;

    SELECT workspace_id INTO v_workspace_id
      FROM sprints
     WHERE sprint_id = p_sprint_id;

    SELECT COUNT(*) INTO v_backlog_count
      FROM tasks
     WHERE workspace_id = v_workspace_id
       AND sprint_id IS NULL;

    UPDATE tasks t
      JOIN (SELECT tk.task_id,
                   ROW_NUMBER() OVER (ORDER BY tk.position, tk.task_id) - 1 AS seq
              FROM tasks tk
              JOIN task_statuses ts ON ts.status_id = tk.status_id
             WHERE tk.sprint_id = p_sprint_id
               AND ts.category <> 'DONE') moved
        ON moved.task_id = t.task_id
       SET t.sprint_id = NULL,
           t.position  = v_backlog_count + moved.seq;

    UPDATE sprints
       SET status = 'COMPLETED'
     WHERE sprint_id = p_sprint_id;
END //

DELIMITER ;


-- 스프린트 삭제 (작업은 백로그 맨 뒤로 이동)
-- 권한 확인(관리자 이상)은 Java에서 선처리
--   p_sprint_id  삭제할 스프린트 번호

DROP PROCEDURE IF EXISTS sp_delete_sprint;

DELIMITER //

CREATE PROCEDURE sp_delete_sprint(IN p_sprint_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_workspace_id BIGINT;
    DECLARE v_backlog_count INT;

    SELECT workspace_id INTO v_workspace_id
      FROM sprints
     WHERE sprint_id = p_sprint_id;

    SELECT COUNT(*) INTO v_backlog_count
      FROM tasks
     WHERE workspace_id = v_workspace_id
       AND sprint_id IS NULL;

    UPDATE tasks t
      JOIN (SELECT task_id,
                   ROW_NUMBER() OVER (ORDER BY position, task_id) - 1 AS seq
              FROM tasks
             WHERE sprint_id = p_sprint_id) moved
        ON moved.task_id = t.task_id
       SET t.sprint_id = NULL,
           t.position  = v_backlog_count + moved.seq;

    DELETE FROM sprints
     WHERE sprint_id = p_sprint_id;
END //

DELIMITER ;


-- 백로그(스프린트 없음) 작업 수
--   p_workspace_id  워크스페이스 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_backlog_task_count;

DELIMITER //

CREATE PROCEDURE sp_backlog_task_count(IN p_workspace_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM tasks
    WHERE workspace_id = p_workspace_id
      AND sprint_id IS NULL;
END //

DELIMITER ;
