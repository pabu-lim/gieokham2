package com.gieokham.api.category.service;

import com.gieokham.api.category.domain.Category;
import com.gieokham.api.category.dto.CategoryResponse;
import com.gieokham.api.category.exception.CategoryNotFoundException;
import com.gieokham.api.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category createCategory(String name) {
        Category category = new Category(name.strip(), 0);
        return categoryRepository.save(category);
    }

    public List<Category> getCategories() {
        Sort sort = Sort.by(
                Sort.Order.asc("sortOrder"),
                Sort.Order.asc("id")
        );

        return categoryRepository.findAll(sort);
    }

    public Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}
