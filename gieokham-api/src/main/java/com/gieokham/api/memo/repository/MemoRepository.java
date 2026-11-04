package com.gieokham.api.memo.repository;

import com.gieokham.api.memo.domain.Memo;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long>, JpaSpecificationExecutor<Memo> {

    Optional<Memo> findByIdAndMemberId(Long memoId, Long memberId);
}
