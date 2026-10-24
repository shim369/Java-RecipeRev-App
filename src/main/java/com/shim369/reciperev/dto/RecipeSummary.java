package com.shim369.reciperev.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** SCR-01 レシピ一覧の1行。画面表示専用の読み取りモデル(エンティティを画面に直接渡さない) */
public record RecipeSummary(
        long id,
        String name,
        String description,
        LocalDateTime updatedAt,
        int latestVersionNo,
        Integer finalVersionNo,
        BigDecimal averageRating) {

    /** 説明の先頭50文字 */
    public String shortDescription() {
        if (description == null) {
            return "";
        }
        return description.length() <= 50 ? description : description.substring(0, 50) + "…";
    }
}
