package com.gieokham.api.memo.dto;

import com.gieokham.api.memo.domain.Memo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemoPageResponse {

    private List<MemoResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;

    public static MemoPageResponse from(Page<Memo> memoPage) {
        return new MemoPageResponse(
                memoPage.getContent().stream()
                        .map(MemoResponse::from)
                        .toList(),
                memoPage.getNumber(),
                memoPage.getSize(),
                memoPage.getTotalElements(),
                memoPage.getTotalPages(),
                memoPage.hasNext()
        );
    }
}
