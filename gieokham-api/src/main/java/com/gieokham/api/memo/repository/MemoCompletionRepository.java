package com.gieokham.api.memo.repository;

import com.gieokham.api.memo.domain.MemoCompletion;
import com.gieokham.api.memo.domain.MemoCompletionId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoCompletionRepository
        extends JpaRepository<MemoCompletion, MemoCompletionId> {

    List<MemoCompletion> findAllByMemoIdOrderByScheduledDateDesc(
            Long memoId,
            Pageable pageable
    );
}