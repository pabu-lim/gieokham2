package com.gieokham.api.category.controller;

import com.gieokham.api.auth.security.CustomOidcUser;
import com.gieokham.api.category.domain.Category;
import com.gieokham.api.category.dto.*;
import com.gieokham.api.category.service.CategoryDeletionService;
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
    private final CategoryDeletionService categoryDeletionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@AuthenticationPrincipal CustomOidcUser principal,
                                           @Valid @RequestBody CategoryCreateRequest request) {
        return CategoryResponse.from(
                categoryService.createCategory(principal.getMemberId(), request.getName(), request.getParentId())
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
        Category category = categoryService.updateCategory(categoryId, principal.getMemberId(), request.getName());
        return CategoryResponse.from(category);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable("categoryId") Long categoryId,
                               @AuthenticationPrincipal CustomOidcUser principal) {
        categoryDeletionService.deleteCategory(categoryId, principal.getMemberId());
    }

    @PatchMapping("/{categoryId}/move")
    public CategoryResponse moveCategory(@PathVariable("categoryId") Long categoryId,
                                         @AuthenticationPrincipal CustomOidcUser principal,
                                         @Valid @RequestBody CategoryMoveRequest request) {
        Category category = categoryService.moveCategory(categoryId, principal.getMemberId(), request.getParentId(), request.getPosition());
        return CategoryResponse.from(category);
    }
}
