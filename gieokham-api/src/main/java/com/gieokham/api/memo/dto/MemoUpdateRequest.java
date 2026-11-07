package com.gieokham.api.memo.dto;

import com.gieokham.api.memo.domain.MemoPriority;
import com.gieokham.api.memo.domain.MemoRepeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemoUpdateRequest {

    @NotBlank(message = "메모 내용을 입력해 주세요.")
    @Size(max = 10000, message = "메모는 10,000자까지 입력할 수 있습니다.")
    private String content;

    @Size(max = 10000, message = "추가 메모는 10,000자까지 입력할 수 있습니다.")
    private String notes;

    @NotNull(message = "중요도를 선택해 주세요.")
    private MemoPriority priority;

    @Positive(message = "카테고리 ID는 양수여야 합니다.")
    private Long categoryId;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @NotNull(message = "반복 유형을 선택해 주세요.")
    private MemoRepeatType repeatType = MemoRepeatType.NONE;

    private Set<@NotNull DayOfWeek> repeatDays;
    private Integer repeatDayOfMonth;
    private Integer repeatMonth;
}
