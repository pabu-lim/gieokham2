package com.gieokham.api.category.repository;

import com.gieokham.api.category.domain.Category;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndMemberId(Long categoryId, Long memberId);

    List<Category> findAllByMemberId(Long memberId, Sort sort);
}
