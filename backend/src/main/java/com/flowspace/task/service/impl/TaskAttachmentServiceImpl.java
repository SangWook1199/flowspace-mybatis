package com.flowspace.task.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.file.service.FileService;
import com.flowspace.file.service.FileVO;
import com.flowspace.task.dto.request.TaskAttachmentPageRequest;
import com.flowspace.task.dto.response.TaskAttachmentResponse;
import com.flowspace.task.service.TaskAttachmentService;
import com.flowspace.task.service.TaskAttachmentVO;
import com.flowspace.task.service.TaskVO;
import com.flowspace.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskAttachmentServiceImpl implements TaskAttachmentService {

    // 작업당 관련 문서 최대 수
    static final int MAX_ATTACHMENTS_PER_TASK = 30;

    // 관련 문서 SQL, 작업 SQL
    private final TaskAttachmentMapper taskAttachmentMapper;
    private final TaskMapper taskMapper;
    private final WorkspaceService workspaceService;
    private final FileService fileService;

    // 관련 문서 목록 (휴지통 페이지는 프로시저에서 제외)
    @Transactional(readOnly = true)
    @Override
    public List<TaskAttachmentResponse> getAttachments(Long taskId, String email) {

        loadTaskForMember(taskId, email);

        return taskAttachmentMapper.selectTaskAttachmentList(taskId).stream().map(TaskAttachmentResponse::from)
            .toList();
    }

    // 페이지 연결
    @Override
    public TaskAttachmentResponse addPage(Long taskId, TaskAttachmentPageRequest request, String email) {

        TaskAndUser loaded = loadTaskForMember(taskId, email);
        TaskVO task = loaded.task();

        Long pageWorkspaceId = taskAttachmentMapper.selectActivePageWorkspaceId(request.pageId());

        if (pageWorkspaceId == null) {
            throw new FlowSpaceException(ErrorCode.PAGE_NOT_FOUND);
        }

        if (!pageWorkspaceId.equals(task.getWorkspaceId())) {
            throw new FlowSpaceException(ErrorCode.ACCESS_DENIED);
        }

        if (taskAttachmentMapper.selectTaskAttachmentPageCount(taskId, request.pageId()) > 0) {
            throw new FlowSpaceException(ErrorCode.TASK_ATTACHMENT_ALREADY_EXISTS);
        }

        checkLimit(taskId);

        TaskAttachmentVO attachment = new TaskAttachmentVO();
        attachment.setTaskId(taskId);
        attachment.setPageId(request.pageId());
        attachment.setCreatedBy(loaded.userId());

        taskAttachmentMapper.insertTaskAttachment(attachment);

        return TaskAttachmentResponse.from(taskAttachmentMapper.selectTaskAttachmentById(attachment.getAttachmentId()));
    }

    // 문서 파일 올리기
    @Override
    public TaskAttachmentResponse addFile(Long taskId, MultipartFile multipartFile, String email) {

        TaskAndUser loaded = loadTaskForMember(taskId, email);

        checkLimit(taskId);

        FileVO file = fileService.uploadDocument(multipartFile, loaded.task().getWorkspaceId(), email);

        TaskAttachmentVO attachment = new TaskAttachmentVO();
        attachment.setTaskId(taskId);
        attachment.setFileId(file.getFileId());
        attachment.setCreatedBy(loaded.userId());

        taskAttachmentMapper.insertTaskAttachment(attachment);

        return TaskAttachmentResponse.from(taskAttachmentMapper.selectTaskAttachmentById(attachment.getAttachmentId()));
    }

    // 관련 문서 삭제 (업로드 파일이면 서버 파일도 삭제)
    @Override
    public void deleteAttachment(Long taskId, Long attachmentId, String email) {

        loadTaskForMember(taskId, email);

        TaskAttachmentVO attachment = taskAttachmentMapper.selectTaskAttachmentById(attachmentId);

        if (attachment == null || !attachment.getTaskId().equals(taskId)) {
            throw new FlowSpaceException(ErrorCode.TASK_ATTACHMENT_NOT_FOUND);
        }

        taskAttachmentMapper.deleteTaskAttachment(attachmentId);

        if (attachment.getFileId() != null) {
            fileService.delete(attachment.getFileId());
        }
    }

    private void checkLimit(Long taskId) {

        if (taskAttachmentMapper.selectTaskAttachmentCount(taskId) >= MAX_ATTACHMENTS_PER_TASK) {
            throw new FlowSpaceException(ErrorCode.TASK_ATTACHMENT_LIMIT);
        }
    }

    // 작업 조회 + 요청자 워크스페이스 멤버 확인
    private TaskAndUser loadTaskForMember(Long taskId, String email) {

        Long userId = taskAttachmentMapper.selectUserIdByEmail(email);

        if (userId == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        TaskVO task = taskMapper.selectTaskById(taskId);

        if (task == null) {
            throw new FlowSpaceException(ErrorCode.TASK_NOT_FOUND);
        }

        workspaceService.getWorkspace(task.getWorkspaceId(), email);

        return new TaskAndUser(task, userId);
    }

    private record TaskAndUser(TaskVO task, Long userId) {
    }
}
