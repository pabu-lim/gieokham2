package com.gieokham.api.memo.repository;

import com.gieokham.api.memo.domain.Memo;
import com.gieokham.api.memo.domain.MemoPriority;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemoSpecification {

    private MemoSpecification() {
    }

    public static Specification<Memo> search(Long memberId, Boolean completed, MemoPriority priority,
                                             String keyword, Long categoryId, List<Long> categoryIds) {
        return((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("member").get("id"), memberId));

            if (categoryId != null) {
                predicates.add(categoryId == 0L
                        ? cb.isNull(root.get("category"))
                        : root.get("category").get("id").in(categoryIds));
            }

            if (completed != null) {
                predicates.add(completed
                        ? cb.isNotNull(root.get("completedAt"))
                        : cb.isNull(root.get("completedAt"))
                );
            }

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if(keyword != null && !keyword.isBlank()){
                String escapedKeyword = keyword.strip()
                        .toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");

                predicates.add(
                        cb.like(
                                cb.lower(root.<String>get("content")),
                            "%" + escapedKeyword + "%",
                            '\\')
                );
            }

            return cb.and(predicates);
        });
    }

    public static Specification<Memo> orderByPriority() {
        return (root, query, cb) -> {
            // 페이지의 전체 개수를 구하는 COUNT 쿼리에는 정렬 제외
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {

                var priorityOrder = cb.<Integer>selectCase()
                        .when(cb.equal(root.get("priority"), MemoPriority.HIGHEST), 0)
                        .when(cb.equal(root.get("priority"), MemoPriority.HIGH), 1)
                        .when(cb.equal(root.get("priority"), MemoPriority.MEDIUM), 2)
                        .when(cb.equal(root.get("priority"), MemoPriority.LOW), 3)
                        .when(cb.equal(root.get("priority"), MemoPriority.LOWEST), 4)
                        .otherwise(5);

                query.orderBy(
                        cb.asc(priorityOrder),
                        cb.desc(root.get("createdAt")),
                        cb.desc(root.get("id"))
                );
            }

            return cb.conjunction();
        };
    }
}
