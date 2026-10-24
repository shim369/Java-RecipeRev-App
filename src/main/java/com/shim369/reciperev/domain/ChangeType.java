package com.shim369.reciperev.domain;

/** 差分の種類。画面では 追加=緑 / 削除=赤 / 変更=黄 / 変更なし=色なし で表示する */
public enum ChangeType {
    ADDED("+", "追加"),
    REMOVED("−", "削除"),
    CHANGED("~", "変更"),
    UNCHANGED(" ", "変更なし");

    private final String mark;
    private final String label;

    ChangeType(String mark, String label) {
        this.mark = mark;
        this.label = label;
    }

    /** 色が見分けにくい人のために、色と一緒に表示する記号 */
    public String getMark() {
        return mark;
    }

    public String getLabel() {
        return label;
    }

    /** CSS クラス名(diff-added など) */
    public String cssClass() {
        return this == UNCHANGED ? "" : "diff-" + name().toLowerCase();
    }
}
