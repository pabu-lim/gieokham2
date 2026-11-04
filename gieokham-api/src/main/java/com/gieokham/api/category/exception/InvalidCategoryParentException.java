package com.gieokham.api.category.exception;

public class InvalidCategoryParentException extends RuntimeException {

    public InvalidCategoryParentException() {
        super("자기 자신이나 하위 카테고리를 부모로 지정할 수 없습니다.");
    }
}
