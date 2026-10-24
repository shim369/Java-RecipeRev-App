package com.shim369.reciperev.dto;

import java.math.BigDecimal;

/** 傾向分析に渡すCSVの1行(版 × 材料) */
public record AnalysisRow(int versionNo, String ingredient, String unit, BigDecimal amount, BigDecimal avgRating) {
}
