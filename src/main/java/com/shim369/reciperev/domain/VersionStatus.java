package com.shim369.reciperev.domain;

/**
 * 版の状態(承認フロー)。
 * DBには name()(DRAFT など)を文字列で保存する。序数(0,1,2)で保存すると、並び替えたときにデータが壊れるため。
 */
public enum VersionStatus {
    DRAFT("下書き"),
    TRIAL("試作中"),
    FINAL("決定版");

    private final String label;

    VersionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
