package com.gieokham.api.memo.service;

import com.gieokham.api.category.domain.Category;
import com.gieokham.api.category.service.CategoryService;
import com.gieokham.api.member.domain.Member;
import com.gieokham.api.member.service.MemberService;
import com.gieokham.api.memo.domain.Memo;
import com.gieokham.api.memo.domain.MemoPriority;
import com.gieokham.api.memo.dto.MemoSearchRequest;
import com.gieokham.api.memo.exception.MemoNotFoundException;
import com.gieokham.api.memo.repository.MemoRepository;
import com.gieokham.api.memo.repository.MemoSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoService {

    private final MemoRepository memoRepository;
    private final CategoryService categoryService;
    private final MemberService memberService;

    @Transactional
    public Memo createMemo(Long memberId, String content, MemoPriority priority,
                           Long categoryId) {
        Member member = memberService.getMember(memberId);

        Memo memo = new Memo(member, content, priority);

        if (categoryId != null) {
            Category category = categoryService.getCategory(categoryId, memberId);
            memo.changeCategory(category);
        }

        return memoRepository.save(memo);
    }

    public Page<Memo> getMemos(Long memberId, MemoSearchRequest request) {
        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                sort
        );

        Specification<Memo> specification = MemoSpecification.search(
                memberId,
                request.getCompleted(),
                request.getPriority(),
                request.getKeyword(),
                request.getCategoryId()
        );

        return memoRepository.findAll(specification, pageable);
    }

    public Memo getMemo(Long memoId, Long memberId) {
        return memoRepository.findByIdAndMemberId(memoId, memberId)
                .orElseThrow(() -> new MemoNotFoundException(memoId));
    }

    @Transactional
    public Memo updateMemo(Long memoId, Long memberId, String content, MemoPriority priority, Long categoryId) {
        Memo memo = getMemo(memoId, memberId);

        if (categoryId != null) {
            memo.changeCategory(categoryService.getCategory(categoryId, memberId));
        } else {
            memo.changeCategory(null);
        }

        memo.update(content, priority);

        return memo;
    }

    @Transactional
    public void deleteMemo(Long memoId, Long memberId) {
        Memo memo = getMemo(memoId, memberId);
        memoRepository.delete(memo);
    }

    @Transactional
    public Memo updateCompletion(Long memoId, Long memberId, boolean completed) {
        Memo memo = getMemo(memoId, memberId);
        memo.updateCompletion(completed);

        return memo;
    }

}
