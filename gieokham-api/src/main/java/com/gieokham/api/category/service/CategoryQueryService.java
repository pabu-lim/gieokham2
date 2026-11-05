package com.gieokham.api.category.service;

import com.gieokham.api.category.domain.Category;
import com.gieokham.api.category.dto.CategoryMemoCountDto;
import com.gieokham.api.category.dto.CategoryResponse;
import com.gieokham.api.memo.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryQueryService {

    private final CategoryService categoryService;
    private final MemoRepository memoRepository;

    public List<CategoryResponse> getCategories(Long memberId) {
        List<Category> categories = categoryService.getCategories(memberId);

        Map<Long, Long> parentIds = new HashMap<>();
        Map<Long, Long> totalCounts = new HashMap<>();

        for (Category category : categories) {
            parentIds.put(category.getId(), category.getParentId());
        }

        for (CategoryMemoCountDto row : memoRepository.countActiveMemosByCategory(memberId)) {

            Long categoryId = row.getCategoryId();
            long count = row.getMemoCount();
            Set<Long> visited = new HashSet<>();

            while (categoryId != null) {
                if (!visited.add(categoryId)) {
                    throw new IllegalStateException("카테고리 구조에 순환이 있습니다.");
                }

                totalCounts.merge(categoryId, count, Long::sum);
                categoryId = parentIds.get(categoryId);
            }
        }

        List<CategoryResponse> responses = new ArrayList<>();

        for (Category category : categories) {
            long count = totalCounts.getOrDefault(category.getId(), 0L);
            responses.add(CategoryResponse.from(category, count));
        }

        return responses;
    }
}