# FlowSpace Backend (MyBatis)

JPA 기반 FlowSpace 백엔드를 MyBatis + MySQL 프로시저 구조로 전환한 버전.
프로시저 규칙과 목록은 [`database/README.md`](database/README.md) 참고.

## 기술 스택

- Spring Boot, Spring Security (JWT, OAuth2)
- MyBatis (`mybatis-spring-boot-starter`)
- MySQL 8 (Stored Procedure)

## 구조

```
com.flowspace
├── <도메인>/
│   ├── web/                컨트롤러
│   ├── scheduler/          정기 작업 (cleanup, notification, page)
│   ├── service/            서비스 인터페이스, VO
│   │   └── impl/           서비스 구현, Mapper 인터페이스
│   └── dto/
│       ├── request/
│       └── response/
├── common/                 enum, 메일, 멘션 파싱, MyBatis 타입 핸들러
├── security/               JWT, 인증 필터, 요청 횟수 제한
├── config/                 Async, Scheduling, Swagger, Web, WebSocket 설정
├── exception/              에러 코드, 전역 예외 처리
└── websocket/              실시간 알림·접속 상태
```

도메인: `activity`, `ai`, `block`, `calendar`, `cleanup`, `comment`, `database`, `file`, `notification`, `page`, `presence`, `retrospective`, `search`, `sprint`, `task`, `user`, `workspace`

```
src/main/resources/
├── sqlmap/mappers/*_SQL.xml    Mapper XML (프로시저 호출만)
└── db/ddl.sql                  테이블 정의

database/
├── apply_procedures.sql        프로시저 적용 목록
├── grants.sql                  백엔드 DB 계정 권한
└── procedures/*.sql            Mapper XML별 프로시저
```

## 요청 처리 흐름

```
Controller → Service → ServiceImpl → Mapper → *_SQL.xml → Procedure
```

| 계층 | 역할 |
|---|---|
| Controller | 요청·응답 변환 |
| ServiceImpl | 로그인 사용자 확인, 멤버·관리자 권한 확인, 업무 규칙, 알림·활동 기록 |
| Mapper / XML | 프로시저 호출 (`{call sp_x(...)}`) |
| Procedure | 조회·등록·수정·삭제 |

- SQL은 모두 프로시저에 있고, XML에는 호출문만 있다. SQL 수정 시 프로시저만 다시 적용하면 되고 백엔드 빌드는 필요 없다.
- 백엔드 DB 계정(`flowspace`)은 `EXECUTE` 권한만 가진다. 테이블에는 프로시저를 통해서만 접근한다.
- 새로 생성된 번호는 프로시저 `OUT` 인자로 받는다.
- 번호 목록은 JSON 배열로 넘긴다 (`IdListTypeHandler`, `IdMapTypeHandler`).

## JPA 버전과 비교

| 항목 | JPA | MyBatis |
|---|---|---|
| SQL | Hibernate가 생성 (메서드 이름, JPQL) | 프로시저로 직접 작성 (253개) |
| DB 접근 | Repository 32개 | Mapper 인터페이스 19개 + XML 19개 |
| 데이터 객체 | Entity 33개 | VO 39개 |
| 수정 | 변경 감지 (트랜잭션 종료 시 UPDATE) | 수정 프로시저 호출 |
| 연관 데이터 | 지연 로딩, `default_batch_fetch_size: 100` | 프로시저 JOIN, 번호 목록으로 일괄 조회 |
| 생성 시각 | `@EnableJpaAuditing` | 컬럼 기본값 `DEFAULT CURRENT_TIMESTAMP` |
| 스키마 검증 | `ddl-auto: validate` | 없음 (`db/ddl.sql` 기준) |
| SQL 변경 반영 | 백엔드 재빌드·재배포 | 프로시저 재적용 |
| DB 계정 권한 | 테이블 전체 | 프로시저 실행만 |
| 패키지 | 계층별 (`controller`, `service`, `repository`, `entity`, `dto`) | 도메인별 (`web`, `service`, `service.impl`, `dto`) |

### 예: 스프린트 완료 시 미완료 작업을 백로그로 이동

JPA: 작업 엔티티를 조회해 하나씩 변경한다. 변경된 작업 수만큼 UPDATE가 실행된다.

```java
int position = taskRepository.findByWorkspaceAndSprintIsNullOrderByPositionAsc(sprint.getWorkspace()).size();

for (Task task : taskRepository.findBySprintOrderByPositionAsc(sprint)) {
    if (task.getStatus().getCategory() == TaskStatusCategory.DONE) {
        continue;
    }
    task.updateSprint(null);
    task.updatePosition(BigDecimal.valueOf(position));
    position++;
}
```

MyBatis: 서비스는 회고 생성 후 프로시저를 호출한다. 작업 수와 관계없이 UPDATE 한 번으로 처리된다.

```java
retrospectiveService.createRetrospective(sprintId, sprint.getWorkspaceId(), sprint.getName(), user.getUserId());
sprintMapper.updateSprintComplete(sprintId);
```

```sql
-- sp_complete_sprint
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
```

### 차이점

- 실행되는 SQL이 프로시저 파일에 그대로 있어 요청별 쿼리 수를 확인할 수 있다.
- 여러 행 변경(작업 이동, 페이지 복제 등)을 SQL 한 번으로 처리한다.
- 컬럼 추가 시 `ddl.sql`, 프로시저, VO, XML 인자를 함께 수정해야 한다. 같은 결과 컬럼을 쓰는 프로시저 묶음은 `database/README.md`에 정리했다.
- SQL 오류와 스키마 불일치는 빌드 단계에서 확인되지 않는다.
- 서비스 테스트는 Mapper를 mock으로 대체하므로 프로시저는 DB에서 별도로 확인한다.

## 전환 과정

1. MyBatis 추가, 연결 확인
2. 도메인별 패키지 이동과 MyBatis 전환 (스프린트 → 작업 → 워크스페이스 → 사용자 → 알림·활동 → 캘린더·댓글·파일·회고 → 페이지·블록 → 검색·접속 상태)
3. XML의 SQL을 프로시저로 이동, Mapper XML당 프로시저 파일 하나로 정리
4. 남은 Repository 사용 제거 (스프린트, 작업, 로그인, 만료 정리, AI)
5. Entity·Repository·JPA 의존성 삭제, enum을 `common/enums`로 이동
6. 백엔드 DB 계정 권한을 프로시저 실행으로 제한
