package com.gieokham.api.memo.service;

import com.gieokham.api.category.domain.Category;
import com.gieokham.api.category.service.CategoryService;
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

    @Transactional
    public Memo createMemo(String content, MemoPriority priority, Long categoryId) {
        Memo memo = new Memo(content, priority);

        if (categoryId != null) {
            Category category = categoryService.getCategory(categoryId);
            memo.changeCategory(category);
        }

        return memoRepository.save(memo);
    }

    public Page<Memo> getMemos(MemoSearchRequest request) {
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
                request.getCompleted(),
                request.getPriority(),
                request.getKeyword()
        );

        return memoRepository.findAll(specification, pageable);
    }

    public Memo getMemo(Long memoId) {
        return memoRepository.findById(memoId)
                .orElseThrow(() -> new MemoNotFoundException(memoId));
    }

    @Transactional
    public Memo updateMemo(Long memoId, String content, MemoPriority priority) {
        Memo memo = getMemo(memoId);
        memo.update(content, priority);

        return memo;
    }

    @Transactional
    public void deleteMemo(Long memoId) {
        Memo memo = getMemo(memoId);
        memoRepository.delete(memo);
    }

    @Transactional
    public Memo updateCompletion(Long memoId, boolean completed) {
        Memo memo = getMemo(memoId);
        memo.updateCompletion(completed);

        return memo;
    }

}
