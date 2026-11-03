package com.gieokham.api.memo.dto;

import com.gieokham.api.memo.domain.MemoPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemoSearchRequest {

    private Boolean completed;
    private MemoPriority priority;
    private String keyword;
    @PositiveOrZero(message = "카테고리 ID는 0 이상이어야 합니다.")
    private Long categoryId;

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 20;
}
