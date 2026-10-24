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

        /** 単位があるか(表示名を「材料 (単位)」にするかどうか) */
        public boolean hasUnit() {
            return unit != null && !unit.isEmpty();
        }

        /**
         * 傾向の文章のメッセージキーの末尾(positive / negative / weak)。
         * 文章そのものは言語ごとに messages*.properties の analysis.sentence.* で持つ。
         */
        public String sentenceKey() {
            if ("WEAK".equals(strength)) {
                return "weak";
            }
            return "POSITIVE".equals(direction) ? "positive" : "negative";
        }

        /** 分量の最小値の表示(末尾の 0 は表示しない: 30.0 → 30)。範囲の書式は言語ごとに analysis.range で持つ */
        public String minLabel() {
            return plain(minAmount);
        }

        public String maxLabel() {
            return plain(maxAmount);
        }

        private static String plain(double value) {
            return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        }
    }

    /** データ不足などで相関を計算しなかった材料 */
    public record Insufficient(String ingredient, String unit, int samples, String reason) {
    }
}
