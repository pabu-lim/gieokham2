package com.gieokham.api.memo.service;

import com.gieokham.api.category.domain.Category;
import com.gieokham.api.category.service.CategoryService;
import com.gieokham.api.member.domain.Member;
import com.gieokham.api.member.service.MemberService;
import com.gieokham.api.memo.domain.Memo;
import com.gieokham.api.memo.domain.MemoPriority;
import com.gieokham.api.memo.domain.MemoRepeatType;
import com.gieokham.api.memo.domain.MemoSortType;
import com.gieokham.api.memo.dto.*;
import com.gieokham.api.memo.exception.InvalidMemoScheduleException;
import com.gieokham.api.memo.exception.MemoNotFoundException;
import com.gieokham.api.memo.repository.MemoCompletionRepository;
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
    private final MemoCompletionRepository memoCompletionRepository;

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

        // 기존 내용·카테고리·일정·반복 설정 다음
        int sortOrder = findNewMemoSortOrder(memberId, memo.getPriority());

        memoRepository.shiftSortOrdersForInsert(memberId, sortOrder);
        memo.changeSortOrder(sortOrder);

        return MemoResponse.from(memoRepository.save(memo));
    }

    public MemoPageResponse getMemos(Long memberId, MemoSearchRequest request) {
        MemoSortType sortType = MemoSortType.CUSTOM;    //기본은 커스텀 정렬기준으로 조회
        if (request.getSort() != null) {
            sortType = request.getSort();
        }

        Sort sort = switch (sortType) {
            case CUSTOM -> Sort.by(
                    Sort.Order.asc("sortOrder"),
                    Sort.Order.desc("id")
            );

            case LATEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );

            case PRIORITY -> Sort.unsorted();
        };

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

        if (sortType == MemoSortType.PRIORITY) {
            specification = specification.and(MemoSpecification.orderByPriority());
        }

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

        memo.changeRepeat(
                request.getRepeatType(), request.getRepeatDays(),
                request.getRepeatDayOfMonth(), request.getRepeatMonth()
        );

        return MemoResponse.from(memo);
    }

    @Transactional
    public void moveMemo(Long memoId, Long memberId, MemoMoveRequest request) {
        memberService.getMemberForUpdate(memberId);

        Memo memo = getMemo(memoId, memberId);

        Long nextMemoId = request.getNextMemoId();
        int currentOrder = memo.getSortOrder();
        int newOrder;

        if (nextMemoId != null) {
            Memo nextMemo = getMemo(nextMemoId, memberId);

            if (memo.getId().equals(nextMemo.getId())) {
                return;
            }

            int nextOrder = nextMemo.getSortOrder();

            // 아래로 이동하면 기존 자리가 빠지므로 한 칸 앞에 배치
            newOrder = currentOrder < nextOrder
                    ? nextOrder - 1
                    : nextOrder;
        } else {
            // 현재 전체 목록의 마지막 순서
            newOrder = memoRepository.findNextSortOrder(memberId) - 1;
        }

        if (currentOrder == newOrder) {
            return;
        }

        int startOrder = Math.min(currentOrder, newOrder);
        int endOrder = Math.max(currentOrder, newOrder);
        int offset = currentOrder < newOrder ? -1 : 1;

        memoRepository.shiftSortOrdersForMove(
                memberId,
                memoId,
                startOrder,
                endOrder,
                offset
        );

        memoRepository.updateSortOrder(memberId, memoId, newOrder);
    }

    @Transactional
    public void deleteMemo(Long memoId, Long memberId) {
        memberService.getMemberForUpdate(memberId);
        Memo memo = getMemo(memoId, memberId);
        memoRepository.delete(memo);
    }

    @Transactional
    public void deleteMemoCompletion(
            Long memoId,
            Long memberId,
            LocalDate scheduledDate
    ) {
        memberService.getMemberForUpdate(memberId);

        // 메모 존재 여부와 소유권 확인
        getMemo(memoId, memberId);

        memoRepository.deleteCompletion(memoId, scheduledDate);
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

    private int findNewMemoSortOrder(Long memberId, MemoPriority priority) {
        Integer sortOrder =
                memoRepository.findFirstSortOrderByPriority(memberId, priority);

        if (sortOrder != null) {
            return sortOrder;
        }

        List<MemoPriority> lowerPriorities = switch (priority) {
            case HIGHEST -> List.of(
                    MemoPriority.HIGH,
                    MemoPriority.MEDIUM,
                    MemoPriority.LOW,
                    MemoPriority.LOWEST
            );
            case HIGH -> List.of(
                    MemoPriority.MEDIUM,
                    MemoPriority.LOW,
                    MemoPriority.LOWEST
            );
            case MEDIUM -> List.of(
                    MemoPriority.LOW,
                    MemoPriority.LOWEST
            );
            case LOW -> List.of(MemoPriority.LOWEST);
            case LOWEST -> List.of();
        };

        if (!lowerPriorities.isEmpty()) {
            sortOrder = memoRepository.findFirstSortOrderByPriorities(
                    memberId, lowerPriorities
            );

            if (sortOrder != null) {
                return sortOrder;
            }
        }

        return memoRepository.findNextSortOrder(memberId);
    }

    @Transactional(readOnly = true)
    public List<MemoCompletionResponse> getMemoCompletions(
            Long memoId,
            Long memberId,
            int page,
            int size
    ) {
        // 메모 존재 여부와 소유권 확인
        getMemo(memoId, memberId);

        return memoCompletionRepository
                .findAllByMemoIdOrderByScheduledDateDesc(
                        memoId,
                        PageRequest.of(page, size)
                )
                .stream()
                .map(MemoCompletionResponse::from)
                .toList();
    }
}
