package com.gieokham.api.category.controller;

import com.gieokham.api.auth.security.CustomOidcUser;
import com.gieokham.api.category.domain.Category;
import com.gieokham.api.category.dto.CategoryCreateRequest;
import com.gieokham.api.category.dto.CategoryOptionResponse;
import com.gieokham.api.category.dto.CategoryResponse;
import com.gieokham.api.category.dto.CategoryUpdateRequest;
import com.gieokham.api.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@AuthenticationPrincipal CustomOidcUser principal,
                                           @Valid @RequestBody CategoryCreateRequest request) {
        return CategoryResponse.from(
                categoryService.createCategory(principal.getMemberId(), request.getName())
        );
    }

    @GetMapping
    public List<CategoryResponse> getCategories(@AuthenticationPrincipal CustomOidcUser principal) {
        return categoryService.getCategories(principal.getMemberId()).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @GetMapping("/options")
    public List<CategoryOptionResponse> getCategoryOptions(@AuthenticationPrincipal CustomOidcUser principal) {
        List<CategoryOptionResponse> options = new ArrayList<>();
        options.add(CategoryOptionResponse.uncategorized());

        for (Category category : categoryService.getCategories(principal.getMemberId())) {
            options.add(CategoryOptionResponse.from(category));
        }

        return options;
    }

    @PatchMapping("/{categoryId}")
    public CategoryResponse updateCategory(
            @PathVariable("categoryId") Long categoryId,
            @AuthenticationPrincipal CustomOidcUser principal,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        return CategoryResponse.from(
                categoryService.updateCategory(categoryId, principal.getMemberId(), request.getName())
        );
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable("categoryId") Long categoryId,
                               @AuthenticationPrincipal CustomOidcUser principal) {
        categoryService.deleteCategory(categoryId, principal.getMemberId());
    }
}
