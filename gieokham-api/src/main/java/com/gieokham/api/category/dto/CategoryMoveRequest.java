package com.gieokham.api.category.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryMoveRequest {

    @Positive(message = "부모 카테고리 ID는 양수여야 합니다.")
    private Long parentId;

    @NotNull(message = "이동할 위치를 입력해 주세요.")
    @Min(value = 0, message = "이동할 위치는 0 이상이어야 합니다.")
    private Integer position;
}