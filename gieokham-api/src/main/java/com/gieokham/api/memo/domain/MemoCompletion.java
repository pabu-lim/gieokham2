package com.gieokham.api.memo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "memo_completion")
@IdClass(MemoCompletionId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoCompletion {

    @Id
    @Column(name = "memo_id", nullable = false)
    private Long memoId;

    @Id
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}