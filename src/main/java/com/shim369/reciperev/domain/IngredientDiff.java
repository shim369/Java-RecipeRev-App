package com.shim369.reciperev.domain;

/**
 * 材料1行の差分。
 *
 * @param before 比較元の行(ADDED のときは null)
 * @param after  比較先の行(REMOVED のときは null)
 */
public record IngredientDiff(ChangeType type, IngredientLine before, IngredientLine after) {

    public String name() {
        return after != null ? after.name() : before.name();
    }
}
