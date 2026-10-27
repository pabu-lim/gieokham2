package com.gieokham.api.memo.dto;

import com.gieokham.api.memo.domain.MemoPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemoUpdateRequest {

    @NotBlank(message = "메모 내용을 입력해 주세요.")
    @Size(max = 10000, message = "메모는 10,000자까지 입력할 수 있습니다.")
    private String content;

    @NotNull(message = "중요도를 선택해 주세요.")
    private MemoPriority priority;
}
