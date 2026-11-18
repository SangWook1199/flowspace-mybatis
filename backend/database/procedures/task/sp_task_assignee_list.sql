-- 칸반 작업들의 담당자를 이름, 프로필 이미지와 함께 한 번에 조회해요.
-- sp_task_list와 같은 조건으로 작업을 골라요.
--   인자: p_workspace_id  워크스페이스 번호
--         p_sprint_id     스프린트 번호 (NULL이면 백로그)
--   결과: 담당자 한 명당 한 행. 지정된 순서대로.

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
      LEFT JOIN files f ON f.file_id = u.profile_file_id   -- 프로필 이미지가 없는 사람도 나와야 해요
     WHERE t.workspace_id = p_workspace_id
       AND t.sprint_id <=> p_sprint_id
     ORDER BY ta.task_assignee_id;
END //

DELIMITER ;
