package com.gieokham.api.category.exception;

public class InvalidCategoryPositionException extends RuntimeException {

    public InvalidCategoryPositionException() {
        super("이동할 위치가 카테고리 목록의 범위를 벗어났습니다.");
    }
}
