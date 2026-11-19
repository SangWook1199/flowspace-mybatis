# 백엔드 (JPA → MyBatis)

JPA로 만든 FlowSpace를 복제해서, 같은 기능을 MyBatis와 DB 프로시저로 다시 만든 버전이에요.
이 문서는 JPA 버전과 무엇이 달라졌는지 정리해요. 프로시저 규칙과 전체 목록은 [`database/README.md`](database/README.md)에 있어요.

## 한눈에 비교

| 항목 | JPA 버전 | MyBatis 버전 |
|---|---|---|
| SQL을 만드는 곳 | 리포지토리 메서드 이름과 JPQL을 보고 Hibernate가 만들어요 | 모든 SQL을 DB 프로시저로 직접 써요 (253개) |
| DB 접근 클래스 | 리포지토리 32개 (`JpaRepository`) | Mapper 인터페이스 19개 + Mapper XML 19개 (`{call ...}`만) |
| 데이터를 담는 클래스 | 엔티티 33개 (연관관계 포함) | VO 39개 (프로시저 결과 컬럼 그대로) |
| 수정 | 엔티티 값을 바꾸면 트랜잭션이 끝날 때 UPDATE (변경 감지) | 수정 프로시저를 직접 호출 |
| 연관 데이터 조회 | 지연 로딩 + `default_batch_fetch_size: 100` | 프로시저에서 JOIN, 여러 건은 번호 목록(JSON)으로 한 번에 |
| 생성 시각 | `@EnableJpaAuditing` | 테이블 기본값 (`DEFAULT CURRENT_TIMESTAMP`) |
| 테이블 구조 확인 | `ddl-auto: validate`가 엔티티와 테이블을 비교 | 없음, `db/ddl.sql`이 기준 |
| SQL을 고칠 때 | 자바 수정 → 다시 빌드·배포 | 프로시저 파일만 다시 적용 (빌드 필요 없음) |
| 백엔드 DB 계정 | 테이블 전체 권한 | 프로시저 실행(`EXECUTE`)만 ([`grants.sql`](database/grants.sql)) |
| 패키지 | 계층별 (`controller`, `service`, `repository`, `entity`, `dto`) | 도메인별 (`web`, `service`, `service.impl`, `dto`) |
| 서비스 테스트 | 리포지토리를 가짜(mock)로 | 다른 서비스와 Mapper를 가짜(mock)로 |

## 패키지 구조

JPA 버전은 계층별로 나눠서, 스프린트 기능 하나를 보려면 다섯 폴더를 오가야 했어요.

```
controller/SprintController
service/SprintService
repository/SprintRepository
entity/Sprint
dto/sprint/...
```

MyBatis 버전은 전자정부 프레임워크처럼 도메인 폴더 안에 계층을 둬요.

```
sprint/
├── web/SprintController             요청·응답
├── service/
│   ├── SprintService                서비스 인터페이스
│   ├── SprintVO, SprintProgressVO   프로시저 결과를 담는 VO
│   └── impl/
│       ├── SprintServiceImpl        권한 확인, 업무 규칙, 알림
│       └── SprintMapper             프로시저 호출 메서드 (Sprint_SQL.xml)
└── dto/request, dto/response        API 요청·응답 형식
```

스케줄러는 컨트롤러처럼 서비스를 부르는 쪽이라 `web` 옆의 `scheduler`에 둬요 (`cleanup`, `notification`, `page`).
여러 도메인이 같이 쓰는 것은 `common`(enum, 메일, 멘션 파싱, MyBatis 타입 핸들러), `security`, `config`에 있어요.

## 한 요청이 처리되는 순서

```
SprintController → SprintService(인터페이스) → SprintServiceImpl → SprintMapper → Sprint_SQL.xml → sp_complete_sprint
```

| 단계 | 하는 일 |
|---|---|
| ServiceImpl | 로그인한 사람 확인, 워크스페이스 멤버·관리자 확인, 상태 변경 규칙, 알림·활동 기록 |
| Mapper + XML | 프로시저 호출만 (`{call sp_complete_sprint(#{sprintId})}`) |
| 프로시저 | 확인이 끝난 뒤의 데이터 처리 |

## 예시: 스프린트 완료

스프린트를 완료하면 끝나지 않은 작업을 백로그 맨 뒤로 보내요.

**JPA 버전**: 작업을 엔티티로 읽어서 하나씩 값을 바꿔요. 트랜잭션이 끝날 때 바뀐 작업마다 UPDATE가 한 번씩 나가요.

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

**MyBatis 버전**: 서비스는 규칙만 확인하고 프로시저를 불러요. 작업이 몇 개든 UPDATE는 한 번이에요.

```java
retrospectiveService.createRetrospective(sprintId, sprint.getWorkspaceId(), sprint.getName(), user.getUserId());
sprintMapper.updateSprintComplete(sprintId);
```

```sql
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

## 장단점

좋아진 점

- SQL을 고칠 때 프로시저만 다시 적용하면 돼요. 백엔드를 다시 빌드하지 않아도 돼요.
- 실행되는 SQL이 파일에 그대로 있어서, 요청 하나에 쿼리가 몇 번 나가는지 바로 보여요.
- 백엔드 계정은 프로시저 실행만 할 수 있어서, 정해진 프로시저 말고는 데이터를 바꿀 수 없어요.
- 여러 행을 바꾸는 일(작업 이동, 페이지 복제 등)을 SQL 한 번으로 처리할 수 있어요.

불편해진 점

- 컬럼 하나를 추가하면 `ddl.sql`, 프로시저, VO, XML 인자를 함께 고쳐야 해요. 같은 컬럼을 돌려주는 프로시저 묶음은 `database/README.md`의 "함께 고쳐야 하는 묶음"에 적어 뒀어요.
- 변경 감지가 없어서 수정할 때마다 프로시저가 필요해요. 그래서 프로시저 수가 많아요.
- SQL이 틀려도 빌드에서는 알 수 없고, `validate` 같은 테이블 구조 확인도 없어요. 실제 DB에서 실행해 봐야 알 수 있어요.
- 서비스 테스트는 Mapper를 가짜로 바꿔서 하기 때문에, 프로시저 자체는 DB에서 따로 확인해야 해요.

## 바꾼 순서

1. MyBatis를 추가하고 연결을 확인했어요.
2. 도메인마다 패키지를 `web / service / service.impl / dto`로 옮기고, 조회부터 등록·수정·삭제까지 MyBatis로 바꿨어요 (스프린트 → 작업 → 워크스페이스 → 사용자 → 알림·활동 → 캘린더·댓글·파일·회고 → 페이지·블록 → 검색·접속 상태).
3. XML에 남아 있던 SQL을 모두 프로시저로 옮기고, Mapper XML 하나에 프로시저 파일 하나로 정리했어요.
4. 서비스에 남아 있던 리포지토리 사용(스프린트, 작업, 로그인, 만료 정리, AI)을 없앴어요.
5. 엔티티·리포지토리·JPA 의존성을 지우고, enum은 `common/enums`로 옮겼어요.
6. 백엔드 DB 계정 권한을 프로시저 실행만 남겼어요.
