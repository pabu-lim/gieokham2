package com.gieokham.api.memo.dto;

import com.gieokham.api.memo.domain.Memo;
import com.gieokham.api.memo.domain.MemoPriority;
import com.gieokham.api.memo.domain.MemoRepeatType;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemoResponse {

    private Long id;
    private String content;
    private String notes;
    private MemoPriority priority;
    private Long categoryId;
    private  LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private MemoRepeatType repeatType;
    private Set<@NotNull DayOfWeek> repeatDays;
    private Integer repeatDayOfMonth;
    private Integer repeatMonth;

    public static MemoResponse from(Memo memo){
        return new MemoResponse(
                memo.getId(),
                memo.getContent(),
                memo.getNotes(),
                memo.getPriority(),
                memo.getCategory() != null ? memo.getCategory().getId() : null,
                memo.getCompletedAt(),
                memo.getCreatedAt(),
                memo.getUpdatedAt(),
                memo.getStartDate(),
                memo.getEndDate(),
                memo.getStartTime(),
                memo.getEndTime(),
                memo.getRepeatType(),
                Set.copyOf(memo.getRepeatDays()),
                memo.getRepeatDayOfMonth(),
                memo.getRepeatMonth()
        );
    }
}
