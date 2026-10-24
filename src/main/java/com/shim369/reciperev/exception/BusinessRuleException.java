package com.shim369.reciperev.exception;

/**
 * 業務ルール(BR-xx)違反。400 として扱い、メッセージ(キー)は利用者向けの文言に変換して見せる。
 * RuntimeException の仲間にしているのは、@Transactional が既定で「非検査例外でロールバック」するため。
 */
public class BusinessRuleException extends LocalizedException {

    public BusinessRuleException(String code, Object... args) {
        super(code, args, null);
    }
}
