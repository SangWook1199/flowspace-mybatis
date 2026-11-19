package com.flowspace.search.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowspace.notification.service.NotificationService;
import com.flowspace.search.dto.response.SearchResponse;
import com.flowspace.search.dto.response.SearchResponse.SearchItem;
import com.flowspace.search.service.SearchResultVO;
import com.flowspace.search.service.SearchService;
import com.flowspace.service.MentionParser;
import com.flowspace.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    // 종류별 최대 결과 수
    private static final int LIMIT = 6;

    // 검색어 최대 길이
    private static final int MAX_KEYWORD_LENGTH = 50;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 일정 날짜 표시 형식
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일");

    // 검색 SQL
    private final SearchMapper searchMapper;
    private final WorkspaceService workspaceService;

    // 통합 검색 (페이지 제목·본문, 작업, 스프린트, 댓글, 일정, 휴지통 페이지 제외)
    @Override
    public SearchResponse search(Long workspaceId, String query, String email) {

        workspaceService.getWorkspace(workspaceId, email);

        String keyword = query == null ? "" : query.trim();

        if (keyword.isEmpty()) {
            return SearchResponse.empty("");
        }

        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
        }

        String lower = keyword.toLowerCase();
        String like = "%" + escapeLike(lower) + "%";

        return new SearchResponse(keyword, searchPages(workspaceId, lower, like), searchTasks(workspaceId, lower, like),
            searchSprints(workspaceId, lower, like), searchComments(workspaceId, lower, like),
            searchEvents(workspaceId, lower, like));
    }

    // ---------- 종류별 검색 ----------

    // 페이지 검색 (제목 일치 먼저, 그다음 본문 블록 일치)
    private List<SearchItem> searchPages(Long workspaceId, String lower, String like) {

        Map<Long, SearchItem> found = new LinkedHashMap<>();

        for (SearchResultVO page : searchMapper.selectPageListByTitle(workspaceId, like, LIMIT)) {
            found.put(page.getId(), pageItem(page.getId(), page.getTitle(), page.getIcon(), null));
        }

        if (found.size() < LIMIT) {
            for (SearchResultVO block : searchMapper.selectBlockList(workspaceId, like)) {

                if (found.containsKey(block.getPageId())) {
                    continue;
                }

                String text = blockText(block.getBody());

                if (text == null || !text.toLowerCase().contains(lower)) {
                    continue;
                }

                found.put(block.getPageId(),
                    pageItem(block.getPageId(), block.getTitle(), block.getIcon(), excerpt(text, lower)));

                if (found.size() >= LIMIT) {
                    break;
                }
            }
        }

        return new ArrayList<>(found.values());
    }

    // 작업 검색 (제목·설명, 상태 이름 표시)
    private List<SearchItem> searchTasks(Long workspaceId, String lower, String like) {

        return searchMapper.selectTaskList(workspaceId, like, LIMIT).stream()
            .map(task -> new SearchItem("TASK", task.getId(), task.getTitle(),
                bodySnippet(task.getTitle(), task.getBody(), lower), null, task.getMeta(),
                NotificationService.taskLink(task.getSprintId(), task.getId())))
            .toList();
    }

    // 스프린트 검색 (이름·목표)
    private List<SearchItem> searchSprints(Long workspaceId, String lower, String like) {

        return searchMapper.selectSprintList(workspaceId, like, LIMIT).stream()
            .map(sprint -> new SearchItem("SPRINT", sprint.getId(), sprint.getTitle(),
                bodySnippet(sprint.getTitle(), sprint.getBody(), lower), null, null, "/sprints/" + sprint.getId()))
            .toList();
    }

    // 댓글 검색 (작업 또는 페이지 이름을 제목으로, 멘션은 @이름으로 변환)
    private List<SearchItem> searchComments(Long workspaceId, String lower, String like) {

        List<SearchItem> items = new ArrayList<>();

        for (SearchResultVO comment : searchMapper.selectCommentList(workspaceId, like, LIMIT)) {
            String link;

            if (comment.getTaskId() != null) {
                link = NotificationService.taskLink(comment.getSprintId(), comment.getTaskId());
            } else if (comment.getPageId() != null) {
                link = "/pages/" + comment.getPageId();
            } else {
                continue;
            }

            items.add(new SearchItem("COMMENT", comment.getId(), comment.getTitle(),
                excerpt(MentionParser.toPlain(comment.getBody()), lower), null, comment.getMeta(), link));
        }

        return items;
    }

    // 일정 검색 (제목·설명, 시작 날짜 표시)
    private List<SearchItem> searchEvents(Long workspaceId, String lower, String like) {

        return searchMapper.selectEventList(workspaceId, like, LIMIT).stream()
            .map(event -> new SearchItem("EVENT", event.getId(), event.getTitle(),
                bodySnippet(event.getTitle(), event.getBody(), lower), null,
                event.getStartDatetime() == null ? null : event.getStartDatetime().format(DATE_FORMAT), "/calendar"))
            .toList();
    }

    // ---------- 도우미 ----------

    // 페이지 결과 한 줄
    private SearchItem pageItem(Long pageId, String title, String icon, String snippet) {
        return new SearchItem("PAGE", pageId, title, snippet, icon, null, "/pages/" + pageId);
    }

    // 본문 발췌 (제목에 검색어가 있거나 본문이 없으면 null)
    private static String bodySnippet(String title, String body, String lower) {

        if (body == null || title.toLowerCase().contains(lower)) {
            return null;
        }

        return excerpt(body, lower);
    }

    // 블록 content(JSON)의 글자 (서식 태그 제거, 글이 없는 블록은 null)
    private String blockText(String content) {

        if (content == null || content.isBlank()) {
            return null;
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(content);
            JsonNode text = node.isObject() ? node.get("text") : node;

            if (text == null || !text.isTextual()) {
                return null;
            }

            return stripHtml(text.asText());
        } catch (Exception e) {
            return null;
        }
    }

    // HTML 태그·엔티티 제거
    private static String stripHtml(String html) {
        return html.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
    }

    // 검색어 앞뒤 발췌 ("…앞글 검색어 뒷글…")
    private static String excerpt(String text, String lower) {

        String flat = text.replaceAll("\\s+", " ").trim();
        int index = flat.toLowerCase().indexOf(lower);

        if (index < 0) {
            return flat.length() > 80 ? flat.substring(0, 80) + "…" : flat;
        }

        int start = Math.max(0, index - 24);
        int end = Math.min(flat.length(), index + lower.length() + 56);

        return (start > 0 ? "…" : "") + flat.substring(start, end) + (end < flat.length() ? "…" : "");
    }

    // LIKE 특수문자(! % _) 이스케이프
    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
