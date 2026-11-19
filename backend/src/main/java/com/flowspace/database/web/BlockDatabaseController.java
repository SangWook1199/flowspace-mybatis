package com.flowspace.database.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.flowspace.database.dto.response.BlockDatabaseCellResponse;
import com.flowspace.database.dto.request.BlockDatabaseCellUpdateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnCreateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnOptionCreateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnOptionReorderRequest;
import com.flowspace.database.dto.response.BlockDatabaseColumnOptionResponse;
import com.flowspace.database.dto.request.BlockDatabaseColumnOptionUpdateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnReorderRequest;
import com.flowspace.database.dto.response.BlockDatabaseColumnResponse;
import com.flowspace.database.dto.request.BlockDatabaseColumnUpdateRequest;
import com.flowspace.database.dto.request.BlockDatabaseColumnWidthUpdateRequest;
import com.flowspace.database.dto.request.BlockDatabaseRowReorderRequest;
import com.flowspace.database.dto.response.BlockDatabaseRowResponse;
import com.flowspace.database.dto.request.BlockDatabaseRowCreateRequest;
import com.flowspace.database.dto.request.DatabaseCreateRequest;
import com.flowspace.database.dto.response.DatabaseDetailResponse;
import com.flowspace.database.dto.response.DatabaseResponse;
import com.flowspace.database.dto.request.DatabaseUpdateRequest;
import com.flowspace.database.dto.request.DatabaseViewUpdateRequest;
import com.flowspace.database.service.BlockDatabaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "OAuth2")
public class BlockDatabaseController {

    private final BlockDatabaseService blockDatabaseService;

    @Operation(summary = "데이터베이스 생성")
    @PostMapping("/pages/{pageId}/databases")
    public DatabaseResponse createDatabase(@PathVariable Long pageId, @Valid @RequestBody DatabaseCreateRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        return blockDatabaseService.createDatabase(pageId, request, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 단건 조회")
    @GetMapping("/databases/{databaseId}")
    public DatabaseResponse getDatabase(@PathVariable Long databaseId,
        @AuthenticationPrincipal UserDetails userDetails) {
        return blockDatabaseService.getDatabase(databaseId, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 제목 수정")
    @PatchMapping("/databases/{databaseId}")
    public DatabaseResponse updateDatabase(@PathVariable Long databaseId,
        @Valid @RequestBody DatabaseUpdateRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        return blockDatabaseService.updateDatabase(databaseId, request, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 삭제")
    @DeleteMapping("/databases/{databaseId}")
    public void deleteDatabase(@PathVariable Long databaseId, @AuthenticationPrincipal UserDetails userDetails) {
        blockDatabaseService.deleteDatabase(databaseId, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 컬럼 생성")
    @PostMapping("/databases/{databaseId}/columns")
    public BlockDatabaseColumnResponse createColumn(@PathVariable Long databaseId,
        @Valid @RequestBody BlockDatabaseColumnCreateRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        return blockDatabaseService.createColumn(databaseId, request, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 컬럼 수정")
    @PatchMapping("/databases/{databaseId}/columns/{columnId}")
    public BlockDatabaseColumnResponse updateColumn(@PathVariable Long databaseId, @PathVariable Long columnId,
        @Valid @RequestBody BlockDatabaseColumnUpdateRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        return blockDatabaseService.updateColumn(columnId, request, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 컬럼 너비 변경")
    @PatchMapping("/databases/{databaseId}/columns/{columnId}/width")
    public BlockDatabaseColumnResponse updateColumnWidth(@PathVariable Long databaseId, @PathVariable Long columnId,
        @Valid @RequestBody BlockDatabaseColumnWidthUpdateRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        return blockDatabaseService.updateColumnWidth(columnId, request, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 컬럼 순서 변경")
    @PatchMapping("/databases/{databaseId}/columns/reorder")
    public void reorderColumns(@PathVariable Long databaseId,
        @Valid @RequestBody BlockDatabaseColumnReorderRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        blockDatabaseService.reorderColumns(databaseId, request, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 컬럼 삭제")
    @DeleteMapping("/databases/{databaseId}/columns/{columnId}")
    public void deleteColumn(@PathVariable Long databaseId, @PathVariable Long columnId,
        @AuthenticationPrincipal UserDetails userDetails) {
        blockDatabaseService.deleteColumn(columnId, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 컬럼 옵션 생성")
    @PostMapping("/databases/{databaseId}/columns/{columnId}/options")
    public BlockDatabaseColumnOptionResponse createOption(@PathVariable Long databaseId, @PathVariable Long columnId,
        @Valid @RequestBody BlockDatabaseColumnOptionCreateRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        return blockDatabaseService.createOption(columnId, request, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 컬럼 옵션 수정")
    @PatchMapping("/databases/{databaseId}/columns/{columnId}/options/{optionId}")
    public BlockDatabaseColumnOptionResponse updateOption(@PathVariable Long databaseId, @PathVariable Long columnId,
        @PathVariable Long optionId, @Valid @RequestBody BlockDatabaseColumnOptionUpdateRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        return blockDatabaseService.updateOption(optionId, request, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 컬럼 옵션 순서 변경")
    @PatchMapping("/databases/{databaseId}/columns/{columnId}/options/reorder")
    public void reorderOptions(@PathVariable Long databaseId, @PathVariable Long columnId,
        @Valid @RequestBody BlockDatabaseColumnOptionReorderRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        blockDatabaseService.reorderOptions(columnId, request, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 컬럼 옵션 삭제")
    @DeleteMapping("/databases/{databaseId}/columns/{columnId}/options/{optionId}")
    public void deleteOption(@PathVariable Long databaseId, @PathVariable Long columnId, @PathVariable Long optionId,
        @AuthenticationPrincipal UserDetails userDetails) {
        blockDatabaseService.deleteOption(optionId, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 행 생성")
    @PostMapping("/databases/{databaseId}/rows")
    public BlockDatabaseRowResponse createRow(@PathVariable Long databaseId,
        @RequestBody(required = false) BlockDatabaseRowCreateRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        return blockDatabaseService.createRow(databaseId, request, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 행 순서 변경")
    @PatchMapping("/databases/{databaseId}/rows/reorder")
    public void reorderRows(@PathVariable Long databaseId, @Valid @RequestBody BlockDatabaseRowReorderRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        blockDatabaseService.reorderRows(databaseId, request, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 행 삭제")
    @DeleteMapping("/databases/{databaseId}/rows/{rowId}")
    public void deleteRow(@PathVariable Long databaseId, @PathVariable Long rowId,
        @AuthenticationPrincipal UserDetails userDetails) {
        blockDatabaseService.deleteRow(rowId, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 셀 수정")
    @PatchMapping("/databases/{databaseId}/cells")
    public BlockDatabaseCellResponse updateCell(@PathVariable Long databaseId,
        @Valid @RequestBody BlockDatabaseCellUpdateRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        return blockDatabaseService.updateCell(databaseId, request, userDetails.getUsername());
    }

    @Operation(summary = "데이터베이스 전체 조회")
    @GetMapping("/databases/{databaseId}/detail")
    public DatabaseDetailResponse getDatabaseDetail(@PathVariable Long databaseId,
        @AuthenticationPrincipal UserDetails userDetails) {
        return blockDatabaseService.getDatabaseDetail(databaseId, userDetails.getUsername());
    }

    @Operation(summary = "표 / 데이터베이스 전환")
    @PatchMapping("/databases/{databaseId}/view")
    public DatabaseResponse updateViewType(@PathVariable Long databaseId,
        @Valid @RequestBody DatabaseViewUpdateRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        return blockDatabaseService.updateViewType(databaseId, request, userDetails.getUsername());
    }
}