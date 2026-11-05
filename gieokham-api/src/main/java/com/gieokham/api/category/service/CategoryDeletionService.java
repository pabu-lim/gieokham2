package com.gieokham.api.category.service;

import com.gieokham.api.category.domain.Category;
import com.gieokham.api.category.repository.CategoryRepository;
import com.gieokham.api.member.service.MemberService;
import com.gieokham.api.memo.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CategoryDeletionService {

    private final MemberService memberService;
    private final CategoryService categoryService;
    private final MemoService memoService;
    private final CategoryRepository categoryRepository;

    @Transactional
    public void deleteCategory(Long categoryId, Long memberId) {
        memberService.getMemberForUpdate(memberId);

        Category category = categoryService.getCategory(categoryId, memberId);

        List<Category> categories = categoryService.getCategories(memberId);

        List<Category> targets = findSubtree(category, categories);

        List<Long> categoryIds = new ArrayList<>();

        for (Category target : targets) {
            categoryIds.add(target.getId());
        }

        memoService.deleteMemosByCategoryIds(memberId, categoryIds);

        for (int i = targets.size() - 1; i >= 0; i--) {
            categoryRepository.delete(targets.get(i));
        }

        // 삭제된 자리 남은 형제 순서 정리
        int sortOrder = 0;

        for (Category remainCategory : categories) {
            if(remainCategory.getId().equals(categoryId)) continue;

            if (Objects.equals(remainCategory.getParentId(), category.getParentId())) {
                remainCategory.changeSortOrder(sortOrder++);
            }
        }
    }

    private List<Category> findSubtree(Category root, List<Category> categories) {
        List<Category> targets = new ArrayList<>();
        Set<Long> visitedIds = new HashSet<>();

        targets.add(root);
        visitedIds.add(root.getId());   // A의 부모 B, B의 부모 A 이렇게 잘못된 데이터일 때 무한반복 방지용

        for (int i = 0; i < targets.size(); i++) {
            Category parent = targets.get(i);

            for (Category category : categories) {

                if (!Objects.equals(category.getParentId(), parent.getId())) continue;

                if (!visitedIds.add(category.getId())) {
                    throw new IllegalStateException("저장된 카테고리 구조에 순환이 있습니다.");
                }

                targets.add(category);
            }

        }

        return targets;
    }

}
