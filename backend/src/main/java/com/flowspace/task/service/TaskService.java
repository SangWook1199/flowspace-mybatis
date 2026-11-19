package com.flowspace.task.service;

import java.util.List;

import com.flowspace.task.dto.request.SubTaskCreateRequest;
import com.flowspace.task.dto.request.SubTaskReorderRequest;
import com.flowspace.task.dto.request.SubTaskUpdateRequest;
import com.flowspace.task.dto.request.TaskCreateRequest;
import com.flowspace.task.dto.request.TaskReorderRequest;
import com.flowspace.task.dto.request.TaskSprintUpdateRequest;
import com.flowspace.task.dto.request.TaskStatusCreateRequest;
import com.flowspace.task.dto.request.TaskStatusDeleteRequest;
import com.flowspace.task.dto.request.TaskStatusEditRequest;
import com.flowspace.task.dto.request.TaskStatusReorderRequest;
import com.flowspace.task.dto.request.TaskStatusUpdateRequest;
import com.flowspace.task.dto.request.TaskUpdateRequest;
import com.flowspace.task.dto.response.SubTaskResponse;
import com.flowspace.task.dto.response.TaskResponse;
import com.flowspace.task.dto.response.TaskSearchResponse;
import com.flowspace.task.dto.response.TaskStatusResponse;

// 작업(칸반) 서비스
public interface TaskService {

    // Task 상태 생성
    TaskStatusResponse createStatus(Long workspaceId, TaskStatusCreateRequest request, String email);

    // Task 상태 목록 조회
    List<TaskStatusResponse> getStatuses(Long workspaceId, String email);

    // Task 상태 수정
    TaskStatusResponse updateStatusInfo(Long statusId, TaskStatusEditRequest request, String email);

    // Task 상태 순서 변경
    void reorderStatuses(TaskStatusReorderRequest request, String email);

    // Task 상태 삭제
    void deleteStatus(Long statusId, TaskStatusDeleteRequest request, String email);

    // Task 생성
    TaskResponse createTask(Long sprintId, TaskCreateRequest request, String email);

    // 백로그 작업 생성 (스프린트 없음)
    TaskResponse createBacklogTask(Long workspaceId, TaskCreateRequest request, String email);

    // 스프린트 Task 목록 조회
    List<TaskResponse> getTasksBySprint(Long sprintId, String email);

    // Task 단건 조회
    TaskResponse getTask(Long taskId, String email);

    // Task 수정
    TaskResponse updateTask(Long taskId, TaskUpdateRequest request, String email);

    // Backlog Task 목록 조회
    List<TaskResponse> getBacklogTasks(Long workspaceId, String email);

    // Task 상태 변경
    TaskResponse updateTaskStatus(Long taskId, TaskStatusUpdateRequest request, String email);

    // Task 삭제
    void deleteTask(Long taskId, String email);

    // SubTask 생성
    SubTaskResponse createSubTask(Long taskId, SubTaskCreateRequest request, String email);

    // SubTask 목록 조회
    List<SubTaskResponse> getSubTasks(Long taskId, String email);

    // SubTask 수정
    SubTaskResponse updateSubTask(Long subTaskId, SubTaskUpdateRequest request, String email);

    // SubTask 순서 변경
    void reorderSubTasks(SubTaskReorderRequest request, String email);

    // SubTask 삭제
    void deleteSubTask(Long subtaskId, String email);

    // Task 스프린트 이동
    TaskResponse updateTaskSprint(Long taskId, TaskSprintUpdateRequest request, String email);

    // Task 순서 변경
    void reorderTasks(TaskReorderRequest request, String email);

    // Task 검색
    List<TaskSearchResponse> searchTasks(Long workspaceId, String keyword, String email);
}
