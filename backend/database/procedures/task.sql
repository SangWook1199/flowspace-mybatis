-- 작업(칸반) 프로시저 (Task_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/task.sql"


-- 칸반 작업 목록 + 상태 이름 (스프린트·백로그 공용)
--   p_workspace_id  워크스페이스 번호
--   p_sprint_id     스프린트 번호 (NULL이면 백로그, <=> 비교)
-- 결과: 작업당 한 행, position 순

DROP PROCEDURE IF EXISTS sp_task_list;

DELIMITER //

CREATE PROCEDURE sp_task_list(IN p_workspace_id BIGINT, IN p_sprint_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT t.task_id,
           t.task_number,
           t.workspace_id,
           t.sprint_id,
           t.status_id,
           t.created_by,
           ts.name AS status_name,
           t.position,
           t.title,
           t.description,
           t.start_date,
           t.end_date,
           t.priority,
           t.completed_at,
           t.created_at
      FROM tasks t
      JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE t.workspace_id = p_workspace_id
       AND t.sprint_id <=> p_sprint_id
     ORDER BY t.position, t.task_id;
END //

DELIMITER ;


-- 칸반 작업 담당자 (이름·프로필 이미지 포함, 작업 조건은 sp_task_list와 동일)
--   p_workspace_id  워크스페이스 번호
--   p_sprint_id     스프린트 번호 (NULL이면 백로그)
-- 결과: 담당자당 한 행, 지정 순서

DROP PROCEDURE IF EXISTS sp_task_assignee_list;

DELIMITER //

CREATE PROCEDURE sp_task_assignee_list(IN p_workspace_id BIGINT, IN p_sprint_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT ta.task_id,
           u.user_id,
           u.nickname,
           u.profile_file_id,
           f.file_url AS profile_image_url
      FROM task_assignees ta
      JOIN tasks t ON t.task_id = ta.task_id
      JOIN users u ON u.user_id = ta.user_id
      LEFT JOIN files f ON f.file_id = u.profile_file_id
     WHERE t.workspace_id = p_workspace_id
       AND t.sprint_id <=> p_sprint_id
     ORDER BY ta.task_assignee_id;
END //

DELIMITER ;


-- 여러 작업의 하위 작업
--   p_task_ids  작업 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_subtask_list;

DELIMITER //

CREATE PROCEDURE sp_subtask_list(IN p_task_ids JSON)
    READS SQL DATA
BEGIN
    SELECT subtask_id, task_id, assignee_id, content, is_completed, position
    FROM subtasks
    WHERE task_id IN (SELECT id FROM JSON_TABLE(p_task_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids)
    ORDER BY position;
END //

DELIMITER ;


-- 작업 한 건 + 상태 이름 (컬럼은 sp_task_list와 동일)
--   p_task_id  작업 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_task;

DELIMITER //

CREATE PROCEDURE sp_task(IN p_task_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT t.task_id,
           t.task_number,
           t.workspace_id,
           t.sprint_id,
           t.status_id,
           t.created_by,
           ts.name AS status_name,
           t.position,
           t.title,
           t.description,
           t.start_date,
           t.end_date,
           t.priority,
           t.completed_at,
           t.created_at
      FROM tasks t
      JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE t.task_id = p_task_id;
END //

DELIMITER ;


-- 작업 한 건의 담당자 (이름·프로필 이미지 포함, 컬럼은 sp_task_assignee_list와 동일)
--   p_task_id  작업 번호
-- 결과: 담당자당 한 행, 지정 순서

DROP PROCEDURE IF EXISTS sp_task_assignee;

DELIMITER //

CREATE PROCEDURE sp_task_assignee(IN p_task_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT ta.task_id,
           u.user_id,
           u.nickname,
           u.profile_file_id,
           f.file_url AS profile_image_url
      FROM task_assignees ta
      JOIN users u ON u.user_id = ta.user_id
      LEFT JOIN files f ON f.file_id = u.profile_file_id
     WHERE ta.task_id = p_task_id
     ORDER BY ta.task_assignee_id;
END //

DELIMITER ;


-- 워크스페이스에서 해당 상태인 작업 수
--   p_workspace_id  워크스페이스 번호
--   p_status_id     상태 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_task_count_by_status;

DELIMITER //

CREATE PROCEDURE sp_task_count_by_status(IN p_workspace_id BIGINT, IN p_status_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM tasks
    WHERE workspace_id = p_workspace_id
      AND status_id = p_status_id;
END //

DELIMITER ;


-- 워크스페이스의 최대 작업 번호 (없으면 0)
--   p_workspace_id  워크스페이스 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_task_max_number;

DELIMITER //

CREATE PROCEDURE sp_task_max_number(IN p_workspace_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COALESCE(MAX(task_number), 0)
    FROM tasks
    WHERE workspace_id = p_workspace_id;
END //

DELIMITER ;


-- 작업 등록 (생성 시각은 DB 기본값)
--   p_workspace_id  워크스페이스 번호
--   p_sprint_id     스프린트 번호
--   p_status_id     상태 번호
--   p_task_number   작업 번호 (워크스페이스 안 일련번호)
--   p_position      순서
--   p_created_by    만든 사용자 번호
--   p_title         제목
--   p_description   설명
--   p_start_date    시작일
--   p_end_date      종료일
--   p_priority      우선순위
--   p_task_id       새 번호
-- 결과: 없음 (p_task_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_task_insert;

DELIMITER //

CREATE PROCEDURE sp_task_insert(IN p_workspace_id BIGINT, IN p_sprint_id BIGINT, IN p_status_id BIGINT,
                                IN p_task_number INT, IN p_position DECIMAL(20,10), IN p_created_by BIGINT,
                                IN p_title VARCHAR(200), IN p_description TEXT, IN p_start_date DATE,
                                IN p_end_date DATE, IN p_priority VARCHAR(50), OUT p_task_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO tasks (workspace_id, sprint_id, status_id, task_number, position, created_by,
                       title, description, start_date, end_date, priority)
    VALUES (p_workspace_id, p_sprint_id, p_status_id, p_task_number, p_position, p_created_by,
            p_title, p_description, p_start_date, p_end_date, p_priority);

    SET p_task_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 작업 담당자 등록
--   p_task_id  작업 번호
--   p_user_id  사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_task_assignee_insert;

DELIMITER //

CREATE PROCEDURE sp_task_assignee_insert(IN p_task_id BIGINT, IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO task_assignees (task_id, user_id)
    VALUES (p_task_id, p_user_id);
END //

DELIMITER ;


-- 작업 수정
--   p_sprint_id     스프린트 번호
--   p_status_id     상태 번호
--   p_position      순서
--   p_title         제목
--   p_description   설명
--   p_start_date    시작일
--   p_end_date      종료일
--   p_priority      우선순위
--   p_completed_at  완료 시각
--   p_task_id       작업 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_task_update;

DELIMITER //

CREATE PROCEDURE sp_task_update(IN p_sprint_id BIGINT, IN p_status_id BIGINT, IN p_position DECIMAL(20,10),
                                IN p_title VARCHAR(200), IN p_description TEXT, IN p_start_date DATE,
                                IN p_end_date DATE, IN p_priority VARCHAR(50), IN p_completed_at DATETIME,
                                IN p_task_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE tasks
       SET sprint_id    = p_sprint_id,
           status_id    = p_status_id,
           position     = p_position,
           title        = p_title,
           description  = p_description,
           start_date   = p_start_date,
           end_date     = p_end_date,
           priority     = p_priority,
           completed_at = p_completed_at
     WHERE task_id = p_task_id;
END //

DELIMITER ;


-- 작업 담당자의 사용자 번호 목록
--   p_task_id  작업 번호
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_task_assignee_user_id_list;

DELIMITER //

CREATE PROCEDURE sp_task_assignee_user_id_list(IN p_task_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT user_id
    FROM task_assignees
    WHERE task_id = p_task_id
    ORDER BY task_assignee_id;
END //

DELIMITER ;


-- 작업 담당자 모두 삭제
--   p_task_id  작업 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_task_assignee_delete;

DELIMITER //

CREATE PROCEDURE sp_task_assignee_delete(IN p_task_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM task_assignees
    WHERE task_id = p_task_id;
END //

DELIMITER ;


-- 작업 담당자가 아닌 하위 작업 담당자 해제 (남은 담당자가 없으면 전체 해제)
--   p_task_id       작업 번호
--   p_assignee_ids  남길 담당자 번호 목록 (JSON 배열)
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_subtask_assignee_clear;

DELIMITER //

CREATE PROCEDURE sp_subtask_assignee_clear(IN p_task_id BIGINT, IN p_assignee_ids JSON)
    MODIFIES SQL DATA
BEGIN
    UPDATE subtasks
       SET assignee_id = NULL
     WHERE task_id = p_task_id
       AND assignee_id IS NOT NULL
       AND assignee_id NOT IN (SELECT id FROM JSON_TABLE(p_assignee_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;


-- 작업 삭제 + 같은 칸(스프린트·상태)의 남은 작업 순서 재정렬 (0부터)
-- 담당자·하위 작업·댓글·첨부는 ON DELETE CASCADE
-- 멤버 확인과 첨부 파일(서버 파일) 삭제는 Java에서 처리
--   p_task_id  삭제할 작업 번호

DROP PROCEDURE IF EXISTS sp_delete_task;

DELIMITER //

CREATE PROCEDURE sp_delete_task(IN p_task_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_workspace_id BIGINT;
    DECLARE v_sprint_id BIGINT;
    DECLARE v_status_id BIGINT;

    SELECT workspace_id, sprint_id, status_id
      INTO v_workspace_id, v_sprint_id, v_status_id
      FROM tasks
     WHERE task_id = p_task_id;

    DELETE FROM tasks
     WHERE task_id = p_task_id;

    UPDATE tasks t
      JOIN (SELECT task_id,
                   ROW_NUMBER() OVER (ORDER BY position, task_id) - 1 AS seq
              FROM tasks
             WHERE workspace_id = v_workspace_id
               AND sprint_id <=> v_sprint_id
               AND status_id = v_status_id) remain
        ON remain.task_id = t.task_id
       SET t.position = remain.seq;
END //

DELIMITER ;


-- 작업 담당자 여부
--   p_task_id  작업 번호
--   p_user_id  사용자 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_task_assignee_count;

DELIMITER //

CREATE PROCEDURE sp_task_assignee_count(IN p_task_id BIGINT, IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM task_assignees
    WHERE task_id = p_task_id
      AND user_id = p_user_id;
END //

DELIMITER ;


-- 하위 작업 한 건
--   p_subtask_id  하위 작업 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_subtask;

DELIMITER //

CREATE PROCEDURE sp_subtask(IN p_subtask_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT subtask_id, task_id, assignee_id, content, is_completed, position
    FROM subtasks
    WHERE subtask_id = p_subtask_id;
END //

DELIMITER ;


-- 작업의 하위 작업 수
--   p_task_id  작업 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_subtask_count;

DELIMITER //

CREATE PROCEDURE sp_subtask_count(IN p_task_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM subtasks
    WHERE task_id = p_task_id;
END //

DELIMITER ;


-- 하위 작업 등록 (is_completed는 DB 기본값)
--   p_task_id      작업 번호
--   p_assignee_id  담당자 번호 (NULL이면 없음)
--   p_content      내용
--   p_position     순서
--   p_subtask_id   새 번호
-- 결과: 없음 (p_subtask_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_subtask_insert;

DELIMITER //

CREATE PROCEDURE sp_subtask_insert(IN p_task_id BIGINT, IN p_assignee_id BIGINT, IN p_content VARCHAR(300),
                                   IN p_position INT, OUT p_subtask_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO subtasks (task_id, assignee_id, content, position)
    VALUES (p_task_id, p_assignee_id, p_content, p_position);

    SET p_subtask_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 하위 작업 수정
--   p_content       내용
--   p_assignee_id   담당자 번호 (NULL이면 없음)
--   p_is_completed  완료 여부
--   p_subtask_id    하위 작업 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_subtask_update;

DELIMITER //

CREATE PROCEDURE sp_subtask_update(IN p_content VARCHAR(300), IN p_assignee_id BIGINT,
                                   IN p_is_completed BOOLEAN, IN p_subtask_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE subtasks
       SET content      = p_content,
           assignee_id  = p_assignee_id,
           is_completed = p_is_completed
     WHERE subtask_id = p_subtask_id;
END //

DELIMITER ;


-- 하위 작업 순서 변경
--   p_position    순서
--   p_subtask_id  하위 작업 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_subtask_position_update;

DELIMITER //

CREATE PROCEDURE sp_subtask_position_update(IN p_position INT, IN p_subtask_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE subtasks
       SET position = p_position
     WHERE subtask_id = p_subtask_id;
END //

DELIMITER ;


-- 하위 작업 삭제
--   p_subtask_id  하위 작업 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_subtask_delete;

DELIMITER //

CREATE PROCEDURE sp_subtask_delete(IN p_subtask_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM subtasks
    WHERE subtask_id = p_subtask_id;
END //

DELIMITER ;


-- 워크스페이스 작업 상태(칸반 열) 목록 (task_statuses + workspace_task_statuses)
--   p_workspace_id  워크스페이스 번호
-- 결과: 상태당 한 행, position 순

DROP PROCEDURE IF EXISTS sp_task_status_list;

DELIMITER //

CREATE PROCEDURE sp_task_status_list(IN p_workspace_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT wts.workspace_id,
           ts.status_id,
           ts.name,
           ts.category,
           ts.color,
           wts.position,
           wts.is_default,
           wts.wip_limit
      FROM workspace_task_statuses wts
      JOIN task_statuses ts ON ts.status_id = wts.status_id
     WHERE wts.workspace_id = p_workspace_id
     ORDER BY wts.position;
END //

DELIMITER ;


-- 워크스페이스 작업 상태 한 개 (컬럼은 sp_task_status_list와 동일, 연결되지 않은 상태면 0행)
--   p_workspace_id  워크스페이스 번호
--   p_status_id     상태 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_task_status;

DELIMITER //

CREATE PROCEDURE sp_task_status(IN p_workspace_id BIGINT, IN p_status_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT wts.workspace_id,
           ts.status_id,
           ts.name,
           ts.category,
           ts.color,
           wts.position,
           wts.is_default,
           wts.wip_limit
      FROM workspace_task_statuses wts
      JOIN task_statuses ts ON ts.status_id = wts.status_id
     WHERE wts.workspace_id = p_workspace_id
       AND wts.status_id = p_status_id;
END //

DELIMITER ;


-- 워크스페이스의 작업 상태 수
--   p_workspace_id  워크스페이스 번호
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_task_status_count;

DELIMITER //

CREATE PROCEDURE sp_task_status_count(IN p_workspace_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM workspace_task_statuses
    WHERE workspace_id = p_workspace_id;
END //

DELIMITER ;


-- 상태 등록
--   p_name       이름
--   p_category   상태 분류
--   p_color      색상
--   p_status_id  새 번호
-- 결과: 없음 (p_status_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_task_status_insert;

DELIMITER //

CREATE PROCEDURE sp_task_status_insert(IN p_name VARCHAR(50), IN p_category VARCHAR(50),
                                       IN p_color VARCHAR(50), OUT p_status_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO task_statuses (name, category, color)
    VALUES (p_name, p_category, p_color);

    SET p_status_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 워크스페이스에 상태 연결 (is_default는 DB 기본값)
--   p_workspace_id  워크스페이스 번호
--   p_status_id     상태 번호
--   p_position      순서
--   p_wip_limit     WIP 제한 (NULL이면 없음)
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_workspace_task_status_insert;

DELIMITER //

CREATE PROCEDURE sp_workspace_task_status_insert(IN p_workspace_id BIGINT, IN p_status_id BIGINT,
                                                 IN p_position INT, IN p_wip_limit INT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO workspace_task_statuses (workspace_id, status_id, position, wip_limit)
    VALUES (p_workspace_id, p_status_id, p_position, p_wip_limit);
END //

DELIMITER ;


-- 상태 정보 수정
--   p_name       이름
--   p_category   상태 분류
--   p_color      색상
--   p_status_id  상태 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_task_status_update;

DELIMITER //

CREATE PROCEDURE sp_task_status_update(IN p_name VARCHAR(50), IN p_category VARCHAR(50),
                                       IN p_color VARCHAR(50), IN p_status_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE task_statuses
       SET name     = p_name,
           category = p_category,
           color    = p_color
     WHERE status_id = p_status_id;
END //

DELIMITER ;


-- 워크스페이스별 WIP 제한 수정
--   p_wip_limit     WIP 제한 (NULL이면 없음)
--   p_workspace_id  워크스페이스 번호
--   p_status_id     상태 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_workspace_task_status_wip_limit_update;

DELIMITER //

CREATE PROCEDURE sp_workspace_task_status_wip_limit_update(IN p_wip_limit INT, IN p_workspace_id BIGINT,
                                                           IN p_status_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE workspace_task_statuses
       SET wip_limit = p_wip_limit
     WHERE workspace_id = p_workspace_id
       AND status_id = p_status_id;
END //

DELIMITER ;


-- 칸반 열 순서 변경
--   p_position      순서
--   p_workspace_id  워크스페이스 번호
--   p_status_id     상태 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_workspace_task_status_position_update;

DELIMITER //

CREATE PROCEDURE sp_workspace_task_status_position_update(IN p_position INT, IN p_workspace_id BIGINT,
                                                          IN p_status_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE workspace_task_statuses
       SET position = p_position
     WHERE workspace_id = p_workspace_id
       AND status_id = p_status_id;
END //

DELIMITER ;


-- 기본 상태(공용 행) 수정 시 해당 워크스페이스만 새 상태로 교체 (공용 행은 유지)
-- 완료 시각은 syncCompletedAt과 같은 규칙 (NOW()는 Asia/Seoul 기준)
--   p_workspace_id   워크스페이스 번호
--   p_old_status_id  기존 기본 상태 번호
--   p_new_status_id  Java에서 생성한 새 상태 번호
--   p_wip_limit      WIP 제한 (NULL이면 제한 없음)

DROP PROCEDURE IF EXISTS sp_replace_default_task_status;

DELIMITER //

CREATE PROCEDURE sp_replace_default_task_status(IN p_workspace_id BIGINT, IN p_old_status_id BIGINT,
                                                IN p_new_status_id BIGINT, IN p_wip_limit INT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_position INT;
    DECLARE v_category VARCHAR(20);

    SELECT position INTO v_position
      FROM workspace_task_statuses
     WHERE workspace_id = p_workspace_id
       AND status_id = p_old_status_id;

    SELECT category INTO v_category
      FROM task_statuses
     WHERE status_id = p_new_status_id;

    INSERT INTO workspace_task_statuses (workspace_id, status_id, position, is_default, wip_limit)
    VALUES (p_workspace_id, p_new_status_id, v_position, FALSE, p_wip_limit);

    UPDATE tasks
       SET status_id    = p_new_status_id,
           completed_at = CASE WHEN v_category = 'DONE' THEN COALESCE(completed_at, NOW()) ELSE NULL END
     WHERE workspace_id = p_workspace_id
       AND status_id = p_old_status_id;

    DELETE FROM workspace_task_statuses
     WHERE workspace_id = p_workspace_id
       AND status_id = p_old_status_id;
END //

DELIMITER ;


-- 작업 상태(칸반 열) 삭제 + 해당 상태 작업을 다른 상태로 이동
-- 기본 상태(공용 행)는 연결만 해제, 워크스페이스 전용 상태는 삭제
-- 마지막 상태 여부와 대상 상태 확인은 Java에서 선처리, 완료 시각은 syncCompletedAt과 같은 규칙
--   p_workspace_id      워크스페이스 번호
--   p_status_id         삭제할 상태 번호
--   p_target_status_id  작업을 옮길 상태 번호

DROP PROCEDURE IF EXISTS sp_delete_task_status;

DELIMITER //

CREATE PROCEDURE sp_delete_task_status(IN p_workspace_id BIGINT, IN p_status_id BIGINT,
                                       IN p_target_status_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_is_default BOOLEAN;
    DECLARE v_target_category VARCHAR(20);

    SELECT is_default INTO v_is_default
      FROM workspace_task_statuses
     WHERE workspace_id = p_workspace_id
       AND status_id = p_status_id;

    SELECT category INTO v_target_category
      FROM task_statuses
     WHERE status_id = p_target_status_id;

    UPDATE tasks
       SET status_id    = p_target_status_id,
           completed_at = CASE WHEN v_target_category = 'DONE' THEN COALESCE(completed_at, NOW()) ELSE NULL END
     WHERE workspace_id = p_workspace_id
       AND status_id = p_status_id;

    DELETE FROM workspace_task_statuses
     WHERE workspace_id = p_workspace_id
       AND status_id = p_status_id;

    IF NOT v_is_default THEN
        DELETE FROM task_statuses
         WHERE status_id = p_status_id;
    END IF;
END //

DELIMITER ;


-- 제목으로 작업 검색 (대소문자 구분 없음, 상태 이름 포함)
--   p_workspace_id  워크스페이스 번호
--   p_keyword       검색어 (%, _는 Java에서 이스케이프, 빈 문자열이면 전체)
-- 결과: 작업당 한 행 (검색용 컬럼만)

DROP PROCEDURE IF EXISTS sp_task_search;

DELIMITER //

CREATE PROCEDURE sp_task_search(IN p_workspace_id BIGINT, IN p_keyword VARCHAR(200))
    READS SQL DATA
BEGIN
    SELECT t.task_id,
           t.title,
           t.status_id,
           ts.name AS status_name
      FROM tasks t
      JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE t.workspace_id = p_workspace_id
       AND LOWER(t.title) LIKE CONCAT('%', LOWER(p_keyword), '%')
     ORDER BY t.task_id;
END //

DELIMITER ;


-- 여러 작업의 담당자 (sp_task_assignee_list와 같은 컬럼)
--   p_task_ids  작업 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_task_assignee_list_by_tasks;

DELIMITER //

CREATE PROCEDURE sp_task_assignee_list_by_tasks(IN p_task_ids JSON)
    READS SQL DATA
BEGIN
    SELECT ta.task_id,
           u.user_id,
           u.nickname,
           u.profile_file_id,
           f.file_url AS profile_image_url
      FROM task_assignees ta
      JOIN users u ON u.user_id = ta.user_id
      LEFT JOIN files f ON f.file_id = u.profile_file_id
     WHERE ta.task_id IN (SELECT id FROM JSON_TABLE(p_task_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids)
     ORDER BY ta.task_assignee_id;
END //

DELIMITER ;
