package com.flowspace.sprint.service;

import java.util.List;

import com.flowspace.sprint.dto.request.SprintCreateRequest;
import com.flowspace.sprint.dto.request.SprintStatusUpdateRequest;
import com.flowspace.sprint.dto.request.SprintUpdateRequest;
import com.flowspace.sprint.dto.response.SprintResponse;

// 스프린트 업무 기능의 목록이에요. Controller는 이 인터페이스만 알고, 실제 동작은 service.impl의 SprintServiceImpl에 있어요.
public interface SprintService {

    // 스프린트 생성
    SprintResponse createSprint(Long workspaceId, SprintCreateRequest request, String email);

    // 워크스페이스 스프린트 목록 조회 (맨 앞에 백로그가 함께 와요)
    List<SprintResponse> getSprints(Long workspaceId, String email);

    // 스프린트 단건 조회
    SprintResponse getSprint(Long sprintId, String email);

    // 스프린트 수정 (이름·목표·설명·색·기간)
    SprintResponse updateSprint(Long sprintId, SprintUpdateRequest request, String email);

    // 스프린트 상태 변경 (계획 → 진행 중 → 완료)
    SprintResponse updateStatus(Long sprintId, SprintStatusUpdateRequest request, String email);

    // 스프린트 삭제 (속한 작업은 백로그로 가요)
    void deleteSprint(Long sprintId, String email);
}
