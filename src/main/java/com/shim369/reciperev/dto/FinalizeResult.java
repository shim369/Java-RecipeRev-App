package com.shim369.reciperev.dto;

/**
 * 「この版を決定版にする」の結果。
 *
 * @param previousFinalNo 試作中に戻った元の決定版の番号(なければ null)
 */
public record FinalizeResult(int finalizedNo, Integer previousFinalNo) {

    public String message() {
        return "第" + finalizedNo + "版を決定版にしました"
                + (previousFinalNo == null ? "" : "(第" + previousFinalNo + "版は試作中に戻りました)");
    }
}
