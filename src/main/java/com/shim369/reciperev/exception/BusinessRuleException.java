package com.shim369.reciperev.exception;

/**
 * 業務ルール(BR-xx)違反。400 として扱い、メッセージはそのまま利用者に見せる前提で書く。
 * RuntimeException にしているのは、@Transactional が既定で「非検査例外でロールバック」するため。
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
