package com.gieokham.api.memo.dto;

import com.gieokham.api.memo.domain.Memo;
import com.gieokham.api.memo.domain.MemoPriority;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemoResponse {

    private Long id;
    private String content;
    private MemoPriority priority;
    private long categoryId;
    private  LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemoResponse from(Memo memo){
        return new MemoResponse(
                memo.getId(),
                memo.getContent(),
                memo.getPriority(),
                memo.getCategory() != null ? memo.getCategory().getId() : null,
                memo.getCompletedAt(),
                memo.getCreatedAt(),
                memo.getUpdatedAt()
        );
    }
}
