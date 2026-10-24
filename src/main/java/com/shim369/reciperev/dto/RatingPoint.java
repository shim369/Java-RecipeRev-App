package com.shim369.reciperev.dto;

import java.math.BigDecimal;

/**
 * 評価推移グラフの1点(JSON API の要素)。record はそのまま JSON に変換される。
 *
 * @param avgRating 調理ログがない版は null(グラフでは線を切る)
 */
public record RatingPoint(int versionNo, BigDecimal avgRating, int logCount, String status) {

    public static RatingPoint from(VersionSummary s) {
        return new RatingPoint(s.versionNo(), s.averageRating(), s.logCount(), s.status().name());
    }
}
