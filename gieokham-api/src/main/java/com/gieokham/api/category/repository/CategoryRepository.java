package com.gieokham.api.category.repository;

import com.gieokham.api.category.domain.Category;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndMemberId(Long categoryId, Long memberId);

    List<Category> findAllByMemberId(Long memberId, Sort sort);

    @Query("""
            select coalesce(max(c.sortOrder), -1) + 1
            from Category c
            where c.member.id = :memberId
             and (
                (:parentId is null and c.parent is null)
                or c.parent.id = :parentId
             )
            """)
    int findNextSortOrder(@Param("memberId") Long memberId, @Param("parentId") Long parentId);

    @Query(value = """
        WITH RECURSIVE subtree(id) AS (
            SELECT id
            FROM category
            WHERE id = :categoryId
              AND member_id = :memberId

            UNION

            SELECT c.id
            FROM category c
            JOIN subtree s ON c.parent_id = s.id
            WHERE c.member_id = :memberId
        )
        SELECT id
        FROM subtree
        """, nativeQuery = true)
    List<Long> findSubtreeIds(@Param("categoryId") Long categoryId, @Param("memberId") Long memberId);
}
