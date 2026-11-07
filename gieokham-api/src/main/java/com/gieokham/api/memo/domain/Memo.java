package com.gieokham.api.memo.domain;

import com.gieokham.api.category.domain.Category;
import com.gieokham.api.member.domain.Member;
import com.gieokham.api.memo.exception.InvalidMemoScheduleException;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "memo")
@Getter
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10000)
    private String content;

    @Column(length = 10000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemoPriority priority = MemoPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", nullable = false, length = 20)
    private MemoRepeatType repeatType = MemoRepeatType.NONE;

    @ElementCollection
    @CollectionTable(name = "memo_repeat_day", joinColumns = @JoinColumn(name = "memo_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private Set<DayOfWeek> repeatDays = new HashSet<>();

    @Column(name = "repeat_day_of_month")
    private Integer repeatDayOfMonth;

    @Column(name = "repeat_month")
    private Integer repeatMonth;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private  LocalDateTime updatedAt;

    protected Memo(){
    }

    public Memo(Member member, String content, MemoPriority priority){
        this.member = member;
        this.content = content;
        this.priority = priority != null ? priority : MemoPriority.MEDIUM;
    }

    @PrePersist
    protected void onCreate(){
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String content, MemoPriority priority) {
        this.content = content;
        this.priority = priority;
    }

    public void updateMemoCompletion(boolean completed) {
        if(completed){
            if (this.completedAt == null) {
                this.completedAt = LocalDateTime.now();
            }
        } else {
          this.completedAt = null;
        }

    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void changeNotes(String notes) {
        this.notes = notes;
    }

    public void changeCategory(Category category) {
        this.category = category;
    }

    public void changeSchedule(LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime) {

        if (endDate != null && (startDate == null || endDate.isBefore(startDate))) {
            throw new InvalidMemoScheduleException("종료일을 지정하려면 시작일이 있어야 하고, 종료일은 시작일보다 빠를 수 없습니다.");
        }

        if (startTime != null || endTime != null) {
            if (startDate == null || startTime == null) {
                throw new InvalidMemoScheduleException("시간을 지정하면 시작일과 시작 시간을 입력해야 합니다.");
            }
            if (endDate == null && endTime != null) {
                throw new InvalidMemoScheduleException("종료 시간을 지정하려면 기간으로 설정해야 합니다.");
            }
            if (endDate != null) {
                if (endTime == null) {
                    throw new InvalidMemoScheduleException("기간에 시간을 지정하면 종료 시간도 입력해야 합니다.");
                }
                if (endDate.atTime(endTime).isBefore(startDate.atTime(startTime))) {
                    throw new InvalidMemoScheduleException("종료 일시는 시작 일시보다 빠를 수 없습니다.");
                }
            }
        }

        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void changeRepeat(MemoRepeatType repeatType, Set<DayOfWeek> days, Integer dayOfMonth, Integer month) {

        if (repeatType == null) {
            throw new InvalidMemoScheduleException("반복 유형을 선택해 주세요.");
        }

        Set<DayOfWeek> selectedDays = days != null ? new HashSet<>(days) : new HashSet<>();

        if (selectedDays.contains(null)) {
            throw new InvalidMemoScheduleException("반복 요일을 확인해 주세요.");
        }

        if (repeatType != MemoRepeatType.WEEKLY && !selectedDays.isEmpty()) {
            throw new InvalidMemoScheduleException("요일은 매주 반복일 때만 선택할 수 있습니다.");
        }

        switch (repeatType) {
            case NONE, DAILY, WEEKLY -> {
                if (dayOfMonth != null || month != null) {
                    throw new InvalidMemoScheduleException(
                            "이 반복 유형에는 월이나 일자를 지정할 수 없습니다."
                    );
                }

                if (repeatType == MemoRepeatType.WEEKLY && selectedDays.isEmpty()) {
                    throw new InvalidMemoScheduleException("반복할 요일을 하나 이상 선택해 주세요.");
                }
            }

            case MONTHLY -> {
                if (month != null || dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 31) {
                    throw new InvalidMemoScheduleException(
                            "매월 반복은 1일부터 31일 사이의 일자만 선택해 주세요."
                    );
                }
            }

            case YEARLY -> {
                if (month == null || month < 1 || month > 12) {
                    throw new InvalidMemoScheduleException("반복할 월을 1월부터 12월 사이로 선택해 주세요.");
                }

                if (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > Month.of(month).maxLength()) {
                    throw new InvalidMemoScheduleException("선택한 월에 존재하는 일자를 입력해 주세요.");
                }
            }
        }

        if (this.repeatType == MemoRepeatType.NONE && repeatType != MemoRepeatType.NONE) {
            this.completedAt = null;
        }

        this.repeatType = repeatType;
        this.repeatDays.clear();
        this.repeatDays.addAll(selectedDays);
        this.repeatDayOfMonth = dayOfMonth;
        this.repeatMonth = month;
    }

}
