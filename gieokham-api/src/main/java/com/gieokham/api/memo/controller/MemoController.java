package com.gieokham.api.memo.controller;

import com.gieokham.api.memo.domain.MemoPriority;
import com.gieokham.api.memo.dto.*;
import com.gieokham.api.memo.service.MemoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memos")
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemoResponse createMemo(@Valid @RequestBody MemoCreateRequest request) {
        return MemoResponse.from(
                memoService.createMemo(request.getContent(), request.getPriority(), request.getCategoryId())
        );
    }

    @GetMapping
    public MemoPageResponse getMemos(
            @Valid @ModelAttribute MemoSearchRequest request) {
        return MemoPageResponse.from(
                memoService.getMemos(request)
        );
    }

    @GetMapping("/{memoId}")
    public MemoResponse getMemo(@PathVariable("memoId") Long memoId) {
        return MemoResponse.from(
                memoService.getMemo(memoId)
        );
    }

    @PatchMapping("/{memoId}")
    public MemoResponse updateMemo(@PathVariable("memoId") Long memoId,
                                   @Valid @RequestBody MemoUpdateRequest request) {
        return MemoResponse.from(
                memoService.updateMemo(memoId, request.getContent(), request.getPriority())
        );
    }

    @DeleteMapping("/{memoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMemo(@PathVariable("memoId") Long memoId) {
        memoService.deleteMemo(memoId);
    }

    @PatchMapping("/{memoId}/completion")
    public MemoResponse updateCompletion(@PathVariable("memoId") Long memoId,
                                         @Valid @RequestBody MemoCompletionUpdateRequest request) {
        return MemoResponse.from(memoService.updateCompletion(memoId, request.getCompleted()));
    }


}
