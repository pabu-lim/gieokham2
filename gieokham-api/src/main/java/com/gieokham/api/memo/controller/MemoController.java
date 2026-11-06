package com.gieokham.api.memo.controller;

import com.gieokham.api.auth.security.CustomOidcUser;
import com.gieokham.api.memo.domain.Memo;
import com.gieokham.api.memo.dto.*;
import com.gieokham.api.memo.service.MemoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/memos")
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemoResponse createMemo(@AuthenticationPrincipal CustomOidcUser principal,
            @Valid @RequestBody MemoCreateRequest request) {

        return memoService.createMemo(principal.getMemberId(), request);
    }

    @GetMapping
    public MemoPageResponse getMemos(@AuthenticationPrincipal CustomOidcUser principal,
                                     @Valid @ModelAttribute MemoSearchRequest request) {
        return memoService.getMemos(principal.getMemberId(), request);
    }

    @GetMapping("/{memoId}")
    public MemoResponse getMemo(@PathVariable("memoId") Long memoId,
                                @AuthenticationPrincipal CustomOidcUser principal) {
        return memoService.getMemoResponse(memoId, principal.getMemberId());
    }

    @PatchMapping("/{memoId}")
    public MemoResponse updateMemo(@PathVariable("memoId") Long memoId,
                                   @AuthenticationPrincipal CustomOidcUser principal,
                                   @Valid @RequestBody MemoUpdateRequest request) {
        return memoService.updateMemo(memoId, principal.getMemberId(), request);
    }

    @DeleteMapping("/{memoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMemo(@PathVariable("memoId") Long memoId,
                           @AuthenticationPrincipal CustomOidcUser principal) {
        memoService.deleteMemo(memoId, principal.getMemberId());
    }

    @PatchMapping("/{memoId}/completion")
    public MemoResponse updateMemoCompletion(@PathVariable("memoId") Long memoId,
                                         @AuthenticationPrincipal CustomOidcUser principal,
                                         @Valid @RequestBody MemoCompletionUpdateRequest request) {
        return memoService.updateMemoCompletion(memoId, principal.getMemberId(), request.getCompleted());
    }

    @GetMapping("/completions/today")
    public List<Long> getCompletedRepeatMemoIdsToday(@AuthenticationPrincipal CustomOidcUser principal) {
        return memoService.getCompletedRepeatMemoIdsToday(principal.getMemberId());
    }

    @GetMapping("/today")
    public List<MemoResponse> getTodayMemos(@AuthenticationPrincipal CustomOidcUser principal) {
        return memoService.getTodayMemos(principal.getMemberId());
    }

    @PatchMapping("/{memoId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveMemo(
            @PathVariable Long memoId,
            @AuthenticationPrincipal CustomOidcUser principal,
            @Valid @RequestBody MemoMoveRequest request
    ) {
        memoService.moveMemo(memoId, principal.getMemberId(), request);
    }

    @GetMapping("/{memoId}/completions")
    public List<MemoCompletionResponse> getMemoCompletions(
            @PathVariable Long memoId,
            @AuthenticationPrincipal CustomOidcUser principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return memoService.getMemoCompletions(
                memoId,
                principal.getMemberId(),
                page,
                size
        );
    }

    @DeleteMapping("/{memoId}/completions/{scheduledDate}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMemoCompletion(
            @PathVariable Long memoId,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate scheduledDate,
            @AuthenticationPrincipal CustomOidcUser principal
    ) {
        memoService.deleteMemoCompletion(
                memoId,
                principal.getMemberId(),
                scheduledDate
        );
    }
}
