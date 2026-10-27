package com.gieokham.api.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateRequest {

    @NotBlank(message = "카테고리 이름을 입력해 주세요.")
    @Size(max = 100, message = "카테고리 이름은 100자까지 입력할 수 있습니다.")
    private String name;
}
