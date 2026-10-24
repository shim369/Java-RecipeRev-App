package com.shim369.reciperev.domain;

/**
 * 差分の種類。画面では 追加=緑 / 削除=赤 / 変更=黄 / 変更なし=色なし で表示する。
 * 表示名は messages*.properties の change.ADDED などで持つ。
 */
public enum ChangeType {
    ADDED("+"),
    REMOVED("−"),
    CHANGED("~"),
    UNCHANGED(" ");

    private final String mark;

    ChangeType(String mark) {
        this.mark = mark;
    }

    /** 色が見分けにくい人のために、色と一緒に表示する記号(言語に依存しない) */
    public String getMark() {
        return mark;
    }

    /** 表示名のメッセージキー(change.ADDED など) */
    public String messageKey() {
        return "change." + name();
    }

    /** CSS クラス名(diff-added など) */
    public String cssClass() {
        return this == UNCHANGED ? "" : "diff-" + name().toLowerCase();
    }
}
