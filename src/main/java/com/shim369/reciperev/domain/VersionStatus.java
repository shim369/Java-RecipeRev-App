package com.shim369.reciperev.domain;

/**
 * 版の状態(承認フロー)。
 * DBには name()(DRAFT など)を文字列で保存する。序数(0,1,2)で保存すると、並び替えたときにデータが壊れるため。
 * 画面の表示名は言語ごとに messages*.properties の status.DRAFT などで持つ。
 */
public enum VersionStatus {
    DRAFT,
    TRIAL,
    FINAL;

    /** 表示名のメッセージキー(status.DRAFT など) */
    public String messageKey() {
        return "status." + name();
    }
}
