package com.gieokham.api.memo.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MemoCompletionId implements Serializable {

    private Long memoId;
    private LocalDate scheduledDate;
}
