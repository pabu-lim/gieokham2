package com.gieokham.api.category.service;

import com.gieokham.api.category.domain.Category;
import com.gieokham.api.category.exception.CategoryNotFoundException;
import com.gieokham.api.category.exception.InvalidCategoryParentException;
import com.gieokham.api.category.exception.InvalidCategoryPositionException;
import com.gieokham.api.category.repository.CategoryRepository;
import com.gieokham.api.member.domain.Member;
import com.gieokham.api.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MemberService memberService;

    @Transactional
    public Category createCategory(Long memberId, String name, Long parentId) {

        Member member = memberService.getMemberForUpdate(memberId);

        Category parent = null;

        if (parentId != null) {
            parent = getCategory(parentId, memberId);
        }

        Category category = new Category(member, name, 0);
        category.changeParent(parent);

        return categoryRepository.save(category);
    }

    public List<Category> getCategories(Long memberId) {
        Sort sort = Sort.by(
                Sort.Order.asc("sortOrder"),
                Sort.Order.asc("id")
        );

        return categoryRepository.findAllByMemberId(memberId, sort);
    }

    public Category getCategory(Long categoryId, Long memberId) {
        return categoryRepository.findByIdAndMemberId(categoryId, memberId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    @Transactional
    public Category updateCategory(Long categoryId, Long memberId, String name) {
        Category category = getCategory(categoryId, memberId);
        category.rename(name);

        return category;
    }

    @Transactional
    public void deleteCategory(Long categoryId, Long memberId) {
        memberService.getMemberForUpdate(memberId);
        Category category = getCategory(categoryId, memberId);
        categoryRepository.delete(category);
    }

    @Transactional
    public Category moveCategory(Long categoryId, Long memberId, Long parentId, int position) {

        memberService.getMemberForUpdate(memberId);

        Category category = getCategory(categoryId, memberId);

        Category parent = null;

        if (parentId != null) {
            parent = getCategory(parentId, memberId);
            validateParent(category, parent);
        }

        Long previousParentId = category.getParent() != null
                ? category.getParent().getId()
                : null;

        List<Category> categories = getCategories(memberId);

        List<Category> previousSiblings = findSiblings(categories, previousParentId, categoryId);

        List<Category> targetSiblings = findSiblings(categories, parentId, categoryId);

        if (position < 0 || position > targetSiblings.size()) {
            throw new InvalidCategoryPositionException();
        }

        category.changeParent(parent);
        targetSiblings.add(position, category);

        if (!Objects.equals(previousParentId, parentId)) {
            applySortOrder(previousSiblings);
        }

        applySortOrder(targetSiblings);

        return category;
    }

    private List<Category> findSiblings(List<Category> categories, Long parentId, Long excludedCategoryId) {
        List<Category> siblings = new ArrayList<>();

        for (Category category : categories) {
            if (category.getId().equals(excludedCategoryId)) {
                continue;
            }

            Long categoryParentId = category.getParent() != null
                    ? category.getParent().getId()
                    : null;

            if (Objects.equals(categoryParentId, parentId)) {
                siblings.add(category);
            }
        }

        return siblings;
    }

    private void validateParent(Category category, Category parent) {
        Category current = parent;

        while (current != null) {
            if (category.getId().equals(current.getId())) {
                throw new InvalidCategoryParentException();
            }

            current = current.getParent();
        }
    }

    private void applySortOrder(List<Category> categories) {
        for (int i = 0; i < categories.size(); i++) {
            categories.get(i).changeSortOrder(i);
        }
    }
}
