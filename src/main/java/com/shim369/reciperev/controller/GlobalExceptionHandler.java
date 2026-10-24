package com.shim369.reciperev.controller;

import com.shim369.reciperev.exception.BusinessRuleException;
import com.shim369.reciperev.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 例外を1か所で HTTP ステータスとエラー画面(SCR-99)に変換する。
 * 各コントローラに try-catch を書かずに済み、エラー時の見た目と記録方法をそろえられる。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception e, Model model) {
        String message = (e instanceof NotFoundException) ? e.getMessage() : "ページが見つかりません";
        return error(model, "見つかりません", message);
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusinessRule(BusinessRuleException e, Model model) {
        log.info("Business rule violation: {}", e.getMessage());
        return error(model, "この操作はできません", e.getMessage());
    }

    /** /recipes/abc のように数値でないIDや、必須パラメータが無いリクエスト */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(Exception e, Model model) {
        return error(model, "不正なリクエスト", "URLまたはパラメータが正しくありません");
    }

    /** 写真が5MBを超えた。コントローラに届く前(multipart解析時)に発生する */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleTooLarge(MaxUploadSizeExceededException e, Model model) {
        return error(model, "ファイルが大きすぎます", "写真は5MB以下のファイルを選んでください");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(Exception e, Model model) {
        log.error("Unexpected error", e); // 想定外の例外だけスタックトレースを残す
        return error(model, "システムエラー", "予期しないエラーが発生しました。時間をおいて再度お試しください");
    }

    private String error(Model model, String title, String message) {
        model.addAttribute("errorTitle", title);
        model.addAttribute("errorMessage", message);
        return "error";
    }
}
