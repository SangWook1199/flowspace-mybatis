# DB 프로시저

DB 안에서 처리하는 것이 나은 일만 프로시저로 만들어요. 나머지 SQL은 `src/main/resources/sqlmap/mappers/*.xml`에 있어요.

| 쓰는 곳 | 기준 |
|---|---|
| 프로시저 | 여러 테이블 조인, 집계, 변수를 쓰는 여러 단계 변경 |
| XML SQL | 테이블 하나, SQL 한 문장, 목록을 넘겨야 하는 SQL(`<foreach>`) |
| 자바 ServiceImpl | 권한 확인, 업무 규칙, 알림 (프로시저는 확인이 끝난 뒤의 데이터 처리만 해요) |

## 폴더 구조

```
database/
├── apply_procedures.sql    모든 프로시저를 DB에 적용하는 목록
└── procedures/
    ├── sprint/             스프린트
    ├── task/               작업(칸반)
    ├── workspace/          워크스페이스
    └── user/               사용자·로그인
```

## 규칙

- 파일 하나에 프로시저 하나, 파일 이름 = 프로시저 이름
- 이름은 `sp_` + 대상 + 동작. 여러 건을 돌려주면 `_list`로 끝나요.
- 파일은 `DROP PROCEDURE IF EXISTS`로 시작해요. 그래서 몇 번을 적용해도 최신 내용으로 다시 만들어져요.
- 데이터를 읽기만 하면 `READS SQL DATA`, 바꾸면 `MODIFIES SQL DATA`를 적어요.
- 새 파일을 만들면 `apply_procedures.sql`에 한 줄 추가하고, 아래 표에도 추가해요.

## 적용

```
docker compose up -d mysql
docker compose exec mysql mysql -uroot -p flow_space -e "source /database/apply_procedures.sql"
docker compose exec mysql mysql -uroot -p flow_space -e "SHOW PROCEDURE STATUS WHERE Db = 'flow_space'"
```

빈 DB로 처음 켤 때는 `ddl.sql` 다음에 자동으로 적용돼요(`docker-compose.yml`의 `docker-entrypoint-initdb.d`).

## 프로시저 목록

### 스프린트 (`procedures/sprint`)

| 프로시저 | 종류 | 인자 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|---|---|
| `sp_sprint_progress_list` | 조인·집계 | 워크스페이스 번호 | 스프린트 목록과 작업 수·완료 수 | `SprintMapper.selectSprintProgressList` |
| `sp_sprint_progress` | 조인·집계 | 스프린트 번호 | 위와 같은데 한 건 | `SprintMapper.selectSprintProgressById` |
| `sp_start_sprint` | 일괄 변경 | 스프린트 번호 | 진행 중이던 다른 스프린트는 계획됨으로, 이 스프린트는 진행 중으로 | `SprintMapper.updateSprintStart` |
| `sp_complete_sprint` | 일괄 변경 | 스프린트 번호 | 끝나지 않은 작업만 백로그 뒤로 옮기고 완료로 | `SprintMapper.updateSprintComplete` |
| `sp_delete_sprint` | 일괄 변경 | 스프린트 번호 | 작업을 모두 백로그 뒤로 옮기고 스프린트 삭제 | `SprintMapper.deleteSprint` |

### 작업 (`procedures/task`)

| 프로시저 | 종류 | 인자 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|---|---|
| `sp_task_list` | 조인 | 워크스페이스 번호, 스프린트 번호(NULL이면 백로그) | 칸반 작업 목록과 상태 이름 | `TaskMapper.selectTaskList` |
| `sp_task` | 조인 | 작업 번호 | 위와 같은데 한 건 | `TaskMapper.selectTaskById` |
| `sp_task_assignee_list` | 조인 | 워크스페이스 번호, 스프린트 번호(NULL이면 백로그) | 칸반 작업들의 담당자와 이름·프로필 이미지 | `TaskMapper.selectTaskAssigneeList` |
| `sp_task_assignee` | 조인 | 작업 번호 | 위와 같은데 작업 한 건 | `TaskMapper.selectTaskAssigneeListByTaskId` |
| `sp_delete_task` | 일괄 변경 | 작업 번호 | 작업 삭제, 같은 칸에 남은 작업 순서를 0부터 다시 매김 | `TaskMapper.deleteTask` |
| `sp_task_status_list` | 조인 | 워크스페이스 번호 | 작업 상태(칸반 열) 목록과 순서·WIP 제한 | `TaskMapper.selectTaskStatusList` |
| `sp_task_status` | 조인 | 워크스페이스 번호, 상태 번호 | 위와 같은데 한 개 (그 워크스페이스의 상태가 아니면 없음) | `TaskMapper.selectTaskStatusById` |
| `sp_replace_default_task_status` | 일괄 변경 | 워크스페이스 번호, 기존·새 상태 번호, WIP 제한 | 기본 상태 대신 새 상태를 같은 자리에 연결하고 작업을 옮김 | `TaskMapper.updateDefaultTaskStatus` |
| `sp_task_search` | 조인 | 워크스페이스 번호, 검색어 | 제목으로 작업 검색 (상태 이름 포함) | `TaskMapper.selectTaskSearchList` |
| `sp_delete_task_status` | 일괄 변경 | 워크스페이스 번호, 삭제할·옮길 상태 번호 | 작업을 다른 상태로 옮기고 상태 삭제 (기본 상태는 연결만 끊음) | `TaskMapper.deleteTaskStatus` |

### 워크스페이스 (`procedures/workspace`)

| 프로시저 | 종류 | 인자 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|---|---|
| `sp_my_workspace_list` | 조인 | 사용자 번호 | 내가 속한 워크스페이스와 내 역할 | `WorkspaceMapper.selectMyWorkspaceList` |
| `sp_workspace_member_list` | 조인 | 워크스페이스 번호 | 멤버 목록과 이름·프로필 이미지·역할 | `WorkspaceMapper.selectWorkspaceMemberList` |
| `sp_workspace_member` | 조인 | 워크스페이스 번호, 사용자 번호 | 위와 같은데 한 명 (멤버가 아니면 없음) | `WorkspaceMapper.selectWorkspaceMember` |
| `sp_member_open_task_list` | 조인 | 워크스페이스 번호, 사용자 번호, 개수 | 맡은 작업 중 끝나지 않은 것 (마감일 순) | `WorkspaceMapper.selectMemberOpenTaskList` |
| `sp_member_task_count` | 조인·집계 | 워크스페이스 번호, 사용자 번호 | 맡은 작업의 진행 중·완료 수 | `WorkspaceMapper.selectMemberTaskCount` |
| `sp_init_workspace` | 일괄 변경 | 워크스페이스 번호, 소유자 번호 | 소유자 멤버와 기본 상태 3개 연결 | `WorkspaceMapper.insertWorkspaceInit` |
| `sp_my_invite_list` | 조인 | 내 이메일 | 받은 초대와 워크스페이스 이름·초대한 사람 | `WorkspaceMapper.selectMyInviteList` |
| `sp_delete_workspace` | 일괄 변경 | 워크스페이스 번호 | 작업을 먼저 지우고 워크스페이스 삭제 (나머지는 CASCADE) | `WorkspaceMapper.deleteWorkspace` |

### 사용자 (`procedures/user`)

| 프로시저 | 종류 | 인자 | 하는 일 | 부르는 Mapper 메서드 |
|---|---|---|---|---|
| `sp_user_by_email` | 조인 | 이메일 | 사용자 한 명과 프로필 이미지 주소 | `UserMapper.selectUserByEmail` |

## 함께 고쳐야 하는 쌍

아래 쌍은 결과 컬럼이 같고 `WHERE`만 달라요. 컬럼을 바꾸면 **두 파일을 함께** 고쳐야 해요(VO 하나가 두 결과를 함께 받아요).

| 목록 | 한 건 | 결과를 받는 VO |
|---|---|---|
| `sp_sprint_progress_list` | `sp_sprint_progress` | `SprintProgressVO` |
| `sp_task_list` | `sp_task` | `TaskVO` |
| `sp_task_assignee_list` | `sp_task_assignee` | `TaskAssigneeVO` |
| `sp_task_status_list` | `sp_task_status` | `TaskStatusVO` |
| `sp_workspace_member_list` | `sp_workspace_member` | `WorkspaceMemberVO` |

담당자 조회는 위 두 프로시저 말고 `Task_SQL.xml`의 `selectTaskAssigneeListByTaskIds`에도 같은 조인이 있어요.
작업 번호 목록(`<foreach>`)을 넘겨야 해서 XML에 뒀어요. 담당자 컬럼을 바꾸면 이 세 곳을 함께 고쳐요.
