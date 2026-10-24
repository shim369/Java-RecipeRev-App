package com.shim369.reciperev.exception;

/** 指定したレシピ・版・調理ログが存在しない(または論理削除済み)。404 として扱う */
public class NotFoundException extends LocalizedException {

    public NotFoundException(String code, Object... args) {
        super(code, args, null);
    }
}
