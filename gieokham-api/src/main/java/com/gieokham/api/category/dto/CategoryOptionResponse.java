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
    private Long parentId;

    public static CategoryOptionResponse uncategorized() {
        return new CategoryOptionResponse(null, "미분류", null);
    }

    public static CategoryOptionResponse from(Category category) {
        return new CategoryOptionResponse(category.getId(),
                category.getName(),
                category.getParent() != null
                        ? category.getParent().getId()
                        : null
                );
    }
}
