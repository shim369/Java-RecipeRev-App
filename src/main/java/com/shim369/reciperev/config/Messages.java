package com.shim369.reciperev.config;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * コントローラなど Java 側で、今のリクエストの言語でメッセージを取り出すための小さな窓口。
 * LocaleContextHolder には、LocaleResolver が決めた「このリクエストの言語」が入っている。
 */
@Component
public class Messages {

    private final MessageSource messageSource;

    public Messages(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /** 例外など「キーと引数を持つオブジェクト」からメッセージを作る */
    public String get(MessageSourceResolvable resolvable) {
        return messageSource.getMessage(resolvable, LocaleContextHolder.getLocale());
    }
}
