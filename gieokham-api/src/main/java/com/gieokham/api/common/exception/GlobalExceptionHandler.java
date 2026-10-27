package com.gieokham.api.common.exception;

import com.gieokham.api.memo.dto.ErrorResponse;
import com.gieokham.api.memo.dto.ValidationErrorResponse;
import com.gieokham.api.memo.exception.MemoNotFoundException;
import org.springframework.http.HttpStatus;
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
}
