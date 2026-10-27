package com.gieokham.api.category.controller;

import com.gieokham.api.category.dto.CategoryCreateRequest;
import com.gieokham.api.category.dto.CategoryResponse;
import com.gieokham.api.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return CategoryResponse.from(
                categoryService.createCategory(request.getName())
        );
    }

    @GetMapping
    public List<CategoryResponse> getCategories() {
        return categoryService.getCategories().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
