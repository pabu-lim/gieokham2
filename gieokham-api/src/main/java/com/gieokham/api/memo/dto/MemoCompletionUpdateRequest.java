package com.gieokham.api.memo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemoCompletionUpdateRequest {

    @NotNull(message = "완료 여부를 입력해 주세요.")
    private Boolean completed;
}
