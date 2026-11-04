package com.gieokham.api.category.service;

import com.gieokham.api.category.domain.Category;
import com.gieokham.api.category.exception.CategoryNotFoundException;
import com.gieokham.api.category.repository.CategoryRepository;
import com.gieokham.api.member.domain.Member;
import com.gieokham.api.member.service.MemberService;
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
    private final MemberService memberService;

    @Transactional
    public Category createCategory(Long memberId, String name) {

        Member member = memberService.getMember(memberId);

        Category category = new Category(member, name, 0);

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
        Category category = getCategory(categoryId, memberId);
        categoryRepository.delete(category);
    }
}
