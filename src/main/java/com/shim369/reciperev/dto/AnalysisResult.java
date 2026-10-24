package com.shim369.reciperev.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Python(analysis/analyze.py)が出力するJSONの形。Jackson が JSON をこの record に変換する。
 * Python 側とはこのファイル形式だけを約束事にしているので、どちらかを作り直しても影響が広がらない。
 */
public record AnalysisResult(
        long recipeId,
        LocalDateTime generatedAt,
        int versionCount,
        int minSamples,
        List<Correlation> results,
        List<Insufficient> insufficient) {

    /** 相関が計算できた材料 */
    public record Correlation(String ingredient, String unit, int samples, double r,
                              String strength, String direction, double minAmount, double maxAmount) {

        public String strengthLabel() {
            return switch (strength) {
                case "STRONG" -> "強い";
                case "MODERATE" -> "中程度";
                default -> "弱い/なし";
            };
        }

        /** 例: 「30 〜 50」(末尾の 0 は表示しない) */
        public String amountRange() {
            return plain(minAmount) + " 〜 " + plain(maxAmount);
        }

        private static String plain(double value) {
            return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        }

        /** 例: 「砂糖(g)が多い版ほど評価が高い傾向」 */
        public String sentence() {
            String name = ingredient + (unit == null || unit.isEmpty() ? "" : "(" + unit + ")");
            if ("WEAK".equals(strength)) {
                return name + "の量と評価には、はっきりした関係は見られません";
            }
            return name + ("POSITIVE".equals(direction) ? "が多い版ほど評価が高い傾向" : "が少ない版ほど評価が高い傾向");
        }
    }

    /** データ不足などで相関を計算しなかった材料 */
    public record Insufficient(String ingredient, String unit, int samples, String reason) {

        public String reasonLabel() {
            return switch (reason) {
                case "NOT_ENOUGH_SAMPLES" -> "評価のある版が足りません";
                case "NO_AMOUNT_VARIATION" -> "どの版でも分量が同じです";
                case "NO_RATING_VARIATION" -> "どの版でも評価が同じです";
                default -> reason;
            };
        }
    }
}
