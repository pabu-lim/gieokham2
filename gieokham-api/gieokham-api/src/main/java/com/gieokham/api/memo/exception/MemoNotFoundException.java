package com.gieokham.api.memo.exception;

public class MemoNotFoundException extends RuntimeException {

    public MemoNotFoundException(Long memoId) {
        super("메모를 찾을 수 없습니다. ID: " + memoId);
    }
}
