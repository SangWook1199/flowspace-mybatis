-- 백엔드 접속 계정(flowspace) 권한: 프로시저 실행(EXECUTE)만 허용
-- 테이블 직접 조회·변경 불가, 프로시저 안의 SQL은 프로시저를 만든 계정(root) 권한으로 실행 (SQL SECURITY DEFINER 기본값)
-- 빈 DB 최초 실행 시 자동 적용, 기존 DB는 아래 명령으로 적용 (root로 실행)
--   docker compose exec mysql mysql -uroot -p -e "source /database/grants.sql"

REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'flowspace'@'%';
GRANT EXECUTE ON flow_space.* TO 'flowspace'@'%';
