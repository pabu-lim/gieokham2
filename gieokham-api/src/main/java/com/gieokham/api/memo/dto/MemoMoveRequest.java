package com.gieokham.api.memo.dto;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemoMoveRequest {

    @Positive(message = "메모 ID는 양수여야 합니다.")
    private Long nextMemoId;
}