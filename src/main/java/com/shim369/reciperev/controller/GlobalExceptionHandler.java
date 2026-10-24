package com.shim369.reciperev.controller;

import com.shim369.reciperev.config.Messages;
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
 * 例外はメッセージキーを持っているので、ここで利用者の言語の文言に変換する。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Messages messages;

    public GlobalExceptionHandler(Messages messages) {
        this.messages = messages;
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NotFoundException e, Model model) {
        return error(model, "error.title.notFound", messages.get(e));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResource(NoResourceFoundException e, Model model) {
        return error(model, "error.title.notFound", messages.get("error.message.pageNotFound"));
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusinessRule(BusinessRuleException e, Model model) {
        log.info("Business rule violation: {}", e.getCode());
        return error(model, "error.title.businessRule", messages.get(e));
    }

    /** /recipes/abc のように数値でないIDや、必須パラメータが無いリクエスト */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(Exception e, Model model) {
        return error(model, "error.title.badRequest", messages.get("error.message.badRequest"));
    }

    /** 写真が5MBを超えた。コントローラに届く前(multipart解析時)に発生する */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleTooLarge(MaxUploadSizeExceededException e, Model model) {
        return error(model, "error.title.tooLarge", messages.get("error.message.tooLarge"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(Exception e, Model model) {
        log.error("Unexpected error", e); // 想定外の例外だけスタックトレースを残す
        return error(model, "error.title.system", messages.get("error.message.system"));
    }

    private String error(Model model, String titleKey, String message) {
        model.addAttribute("errorTitle", messages.get(titleKey));
        model.addAttribute("errorMessage", message);
        return "error";
    }
}
