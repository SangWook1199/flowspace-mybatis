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

    // 한 작업에 붙일 수 있는 관련 문서 수예요.
    static final int MAX_ATTACHMENTS_PER_TASK = 30;

    // 관련 문서 SQL, 작업 SQL (같은 패키지라 import가 없어요)
    private final TaskAttachmentMapper taskAttachmentMapper;
    private final TaskMapper taskMapper;
    private final WorkspaceService workspaceService;
    private final FileService fileService;

    // 관련 문서 목록 (휴지통에 있는 페이지는 프로시저에서 빼고 읽어요)
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

        // 다른 워크스페이스의 페이지는 연결할 수 없어요.
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

        // 페이지 제목·올린 사람 이름까지 담으려고 다시 읽어요
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

    // 관련 문서 삭제 (올린 파일이면 서버의 파일도 같이 지워요)
    @Override
    public void deleteAttachment(Long taskId, Long attachmentId, String email) {

        loadTaskForMember(taskId, email);

        TaskAttachmentVO attachment = taskAttachmentMapper.selectTaskAttachmentById(attachmentId);

        // 다른 작업의 관련 문서 번호로는 지울 수 없어요.
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

    // 작업을 찾고, 요청한 사람이 그 워크스페이스 멤버인지 확인해요.
    private TaskAndUser loadTaskForMember(Long taskId, String email) {

        Long userId = taskAttachmentMapper.selectUserIdByEmail(email);

        if (userId == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        TaskVO task = taskMapper.selectTaskById(taskId);

        if (task == null) {
            throw new FlowSpaceException(ErrorCode.TASK_NOT_FOUND);
        }

        // 워크스페이스 멤버인지는 워크스페이스 서비스의 규칙을 그대로 써요
        workspaceService.getWorkspace(task.getWorkspaceId(), email);

        return new TaskAndUser(task, userId);
    }

    private record TaskAndUser(TaskVO task, Long userId) {
    }
}
