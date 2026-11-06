package com.gieokham.api.memo.dto;

import com.gieokham.api.memo.domain.MemoCompletion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemoCompletionResponse {

    private Long memoId;
    private LocalDate scheduledDate;
    private LocalDateTime completedAt;

    public static MemoCompletionResponse from(MemoCompletion completion) {
        return new MemoCompletionResponse(
                completion.getMemoId(),
                completion.getScheduledDate(),
                completion.getCompletedAt()
        );
    }
}