package com.gieokham.api.category.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryMemoCountDto {

    private final Long categoryId;
    private final Long memoCount;
}