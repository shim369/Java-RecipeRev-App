package com.shim369.reciperev.domain;

/**
 * 手順1行の差分。
 *
 * @param before 比較元の行(ADDED のときは null)
 * @param after  比較先の行(REMOVED のときは null)
 */
public record StepDiff(ChangeType type, String before, String after) {
}
