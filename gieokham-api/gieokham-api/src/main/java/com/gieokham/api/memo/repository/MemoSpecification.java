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

    public static Specification<Memo> search(Boolean completed, MemoPriority priority, String keyword) {
        return((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

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
}
