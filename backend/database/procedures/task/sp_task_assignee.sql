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
