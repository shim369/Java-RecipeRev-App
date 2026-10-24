package com.shim369.reciperev.controller;

import com.shim369.reciperev.exception.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * JSON API 用の例外ハンドラ。画面用の GlobalExceptionHandler より先に評価させ(@Order)、
 * エラーも HTML ではなく JSON(RFC 9457 の ProblemDetail 形式)で返す。
 */
@RestControllerAdvice(assignableTypes = RatingTrendApiController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
