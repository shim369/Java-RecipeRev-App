package com.shim369.reciperev.exception;

/** BR-03: 派生元の版と材料・手順が同じなので、新しい版を作らなかった */
public class NoChangesException extends BusinessRuleException {

    public NoChangesException() {
        super("派生元の版から変更がありません。材料か手順を変更してから保存してください。");
    }
}
