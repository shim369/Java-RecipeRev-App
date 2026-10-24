package com.shim369.reciperev.exception;

import org.springframework.context.MessageSourceResolvable;

/**
 * 利用者に見せるメッセージを「文言」ではなく「メッセージキー + 引数」で持つ例外の基底クラス。
 * 文言そのものを持つと、例外を投げた時点の言語に固定されてしまう。キーで持っておけば、
 * 画面を表示する時点で利用者の言語(英語/日本語)に変換できる。
 *
 * <p>MessageSourceResolvable を実装しているので、messageSource.getMessage(例外, locale) でそのまま文言にできる。
 * getMessage() はキーを返す(ログやテストで、どのエラーかを言語に関係なく判別できる)。
 */
public abstract class LocalizedException extends RuntimeException implements MessageSourceResolvable {

    private final String code;
    private final Object[] arguments;

    protected LocalizedException(String code, Object[] arguments, Throwable cause) {
        super(code, cause);
        this.code = code;
        this.arguments = arguments;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String[] getCodes() {
        return new String[]{code};
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    /** キーがメッセージファイルに無い場合は、キーをそのまま表示する */
    @Override
    public String getDefaultMessage() {
        return code;
    }
}
