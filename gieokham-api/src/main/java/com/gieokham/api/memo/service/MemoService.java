package com.gieokham.api.memo.service;

import com.gieokham.api.category.domain.Category;
import com.gieokham.api.category.service.CategoryService;
import com.gieokham.api.member.domain.Member;
import com.gieokham.api.member.service.MemberService;
import com.gieokham.api.memo.domain.Memo;
import com.gieokham.api.memo.domain.MemoPriority;
import com.gieokham.api.memo.domain.MemoRepeatType;
import com.gieokham.api.memo.dto.*;
import com.gieokham.api.memo.exception.InvalidMemoScheduleException;
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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoService {

    private final MemoRepository memoRepository;
    private final CategoryService categoryService;
    private final MemberService memberService;

    @Transactional
    public MemoResponse createMemo(Long memberId, MemoCreateRequest request) {
        Member member = memberService.getMemberForUpdate(memberId);

        Memo memo = new Memo(member, request.getContent(), request.getPriority());

        //카테고리
        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategory(request.getCategoryId(), memberId);
            memo.changeCategory(category);
        }

        //일정
        memo.changeSchedule(
                request.getStartDate(), request.getEndDate(), request.getStartTime(), request.getEndTime()
        );
        
        //반복
        memo.changeRepeat(
                request.getRepeatType(), request.getRepeatDays(), request.getRepeatDayOfMonth(), request.getRepeatMonth()
        );

        return MemoResponse.from(memoRepository.save(memo));
    }

    public MemoPageResponse getMemos(Long memberId, MemoSearchRequest request) {
        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                sort
        );

        Long categoryId = request.getCategoryId();
        List<Long> categoryIds = List.of();

        if (categoryId != null && categoryId > 0) {
            categoryIds = categoryService.getSubtreeIds(categoryId, memberId);
        }

        Specification<Memo> specification = MemoSpecification.search(
                memberId,
                request.getCompleted(),
                request.getPriority(),
                request.getKeyword(),
                categoryId,
                categoryIds
        );


        Page<Memo> memos = memoRepository.findAll(specification, pageable);
        return MemoPageResponse.from(memos);
    }

    public Memo getMemo(Long memoId, Long memberId) {
        return memoRepository.findByIdAndMemberId(memoId, memberId)
                .orElseThrow(() -> new MemoNotFoundException(memoId));
    }

    public MemoResponse getMemoResponse(Long memoId, Long memberId) {
        Memo memo = getMemo(memoId, memberId);
        return MemoResponse.from(memo);
    }

    @Transactional
    public MemoResponse updateMemo(Long memoId, Long memberId, MemoUpdateRequest request) {
        memberService.getMemberForUpdate(memberId);
        Memo memo = getMemo(memoId, memberId);

        if (request.getCategoryId() != null) {
            memo.changeCategory(categoryService.getCategory(request.getCategoryId(), memberId));
        } else {
            memo.changeCategory(null);
        }

        memo.update(request.getContent(), request.getPriority());

        memo.changeSchedule(
                request.getStartDate(),
                request.getEndDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        return MemoResponse.from(memo);
    }

    @Transactional
    public void deleteMemo(Long memoId, Long memberId) {
        Memo memo = getMemo(memoId, memberId);
        memoRepository.delete(memo);
    }

    @Transactional
    public MemoResponse updateMemoCompletion(Long memoId, Long memberId, boolean completed) {
        memberService.getMemberForUpdate(memberId);

        Memo memo = getMemo(memoId, memberId);

        if (memo.getRepeatType() == MemoRepeatType.NONE) {
            memo.updateMemoCompletion(completed);   //반복 아닌경우
        } else {
            if(completed){
                memoRepository.insertCompletion(memoId, LocalDate.now());
            } else {
                memoRepository.deleteCompletion(memoId, LocalDate.now());
            }
        }

        return MemoResponse.from(memo);
    }

    @Transactional
    public void deleteMemosByCategoryIds(Long memberId, List<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return;
        }

        memoRepository.deleteByCategoryIds(memberId, categoryIds);
    }

    public List<Long> getCompletedRepeatMemoIdsToday(Long memberId) {
        return memoRepository.findCompletedRepeatMemoIds(memberId, LocalDate.now());
    }

    public List<MemoResponse> getTodayMemos(Long memberId) {
        LocalDate today = LocalDate.now();

        List<Memo> memos = memoRepository.findTodayMemos(
                memberId,
                today,
                today.getDayOfWeek().name(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                today.lengthOfMonth()
        );

        return memos.stream()
                .map(MemoResponse::from)
                .toList();
    }
}
