package com.gieokham.api.category.dto;

import com.gieokham.api.category.domain.Category;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CategoryOptionResponse {

    private final Long id;
    private final String name;

    public static CategoryOptionResponse uncategorized() {
        return new CategoryOptionResponse(null, "미분류");
    }

    public static CategoryOptionResponse from(Category category) {
        return new CategoryOptionResponse(category.getId(), category.getName());
    }
}
