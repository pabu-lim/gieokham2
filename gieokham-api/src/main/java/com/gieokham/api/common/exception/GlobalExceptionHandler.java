package com.gieokham.api.common.exception;

import com.gieokham.api.category.exception.CategoryNotFoundException;
import com.gieokham.api.category.exception.InvalidCategoryParentException;
import com.gieokham.api.category.exception.InvalidCategoryPositionException;
import com.gieokham.api.member.exception.MemberAlreadyRegisteredException;
import com.gieokham.api.member.exception.MemberNotFoundException;
import com.gieokham.api.memo.dto.ErrorResponse;
import com.gieokham.api.memo.dto.ValidationErrorResponse;
import com.gieokham.api.memo.exception.InvalidMemoScheduleException;
import com.gieokham.api.memo.exception.MemoNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> {
                    String message = error.isBindingFailure()
                            ? "입력값의 형식이 올바르지 않습니다."
                            : error.getDefaultMessage();

                    errors.putIfAbsent(error.getField(), message);
                });

        return new ValidationErrorResponse("입력값을 확인해 주세요", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return new ErrorResponse("요청 형식이 올바르지 않습니다. JSON 문법과 입력값의 타입을 확인해 주세요.");
    }

    @ExceptionHandler(MemoNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleMemoNotFoundException(MemoNotFoundException exception) {
        return new ErrorResponse("메모를 찾을 수 없습니다.");
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCategoryNotFoundException(CategoryNotFoundException exception) {
        return new ErrorResponse("카테고리를 찾을 수 없습니다.");
    }

    @ExceptionHandler(MemberAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleMemberAlreadyRegistered(MemberAlreadyRegisteredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(InvalidCategoryParentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCategoryParent(InvalidCategoryParentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(InvalidCategoryPositionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCategoryPosition(InvalidCategoryPositionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(InvalidMemoScheduleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidMemoScheduleException(InvalidMemoScheduleException exception) {
        return new ErrorResponse(exception.getMessage());
    }
}
