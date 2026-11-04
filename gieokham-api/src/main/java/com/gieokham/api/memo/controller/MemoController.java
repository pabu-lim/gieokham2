package com.gieokham.api.memo.controller;

import com.gieokham.api.auth.security.CustomOidcUser;
import com.gieokham.api.memo.domain.Memo;
import com.gieokham.api.memo.dto.*;
import com.gieokham.api.memo.service.MemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memos")
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MemoResponse> createMemo(@AuthenticationPrincipal CustomOidcUser principal,
            @Valid @RequestBody MemoCreateRequest request) {
        Memo memo = memoService.createMemo(
                principal.getMemberId(),
                request.getContent(),
                request.getPriority(),
                request.getCategoryId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MemoResponse.from(memo));
    }

    @GetMapping
    public MemoPageResponse getMemos(@AuthenticationPrincipal CustomOidcUser principal,
                                     @Valid @ModelAttribute MemoSearchRequest request) {
        return MemoPageResponse.from(
                memoService.getMemos(principal.getMemberId(), request)
        );
    }

    @GetMapping("/{memoId}")
    public MemoResponse getMemo(@PathVariable("memoId") Long memoId,
                                @AuthenticationPrincipal CustomOidcUser principal) {
        return MemoResponse.from(
                memoService.getMemo(memoId, principal.getMemberId())
        );
    }

    @PatchMapping("/{memoId}")
    public MemoResponse updateMemo(@PathVariable("memoId") Long memoId,
                                   @AuthenticationPrincipal CustomOidcUser principal,
                                   @Valid @RequestBody MemoUpdateRequest request) {
        return MemoResponse.from(
                memoService.updateMemo(memoId, principal.getMemberId(), request.getContent(), request.getPriority(), request.getCategoryId())
        );
    }

    @DeleteMapping("/{memoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMemo(@PathVariable("memoId") Long memoId,
                           @AuthenticationPrincipal CustomOidcUser principal) {
        memoService.deleteMemo(memoId, principal.getMemberId());
    }

    @PatchMapping("/{memoId}/completion")
    public MemoResponse updateCompletion(@PathVariable("memoId") Long memoId,
                                         @AuthenticationPrincipal CustomOidcUser principal,
                                         @Valid @RequestBody MemoCompletionUpdateRequest request) {
        return MemoResponse.from(memoService.updateCompletion(memoId, principal.getMemberId(), request.getCompleted()));
    }


}
