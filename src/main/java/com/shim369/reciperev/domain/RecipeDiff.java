package com.shim369.reciperev.domain;

import java.util.List;
import java.util.function.Function;

/** 2つの版の差分(材料・手順)。RecipeDiffService が作る */
public record RecipeDiff(List<IngredientDiff> ingredients, List<StepDiff> steps) {

    public RecipeDiff {
        ingredients = List.copyOf(ingredients);
        steps = List.copyOf(steps);
    }

    /** 1つでも変更があるか。BR-03(変更がなければ新しい版を作らない)の判定に使う */
    public boolean hasChanges() {
        return ingredients.stream().anyMatch(d -> d.type() != ChangeType.UNCHANGED)
                || steps.stream().anyMatch(d -> d.type() != ChangeType.UNCHANGED);
    }

    public long count(ChangeType type) {
        return countIn(ingredients, IngredientDiff::type, type) + countIn(steps, StepDiff::type, type);
    }

    private static <T> long countIn(List<T> list, Function<T, ChangeType> typeOf, ChangeType type) {
        return list.stream().filter(d -> typeOf.apply(d) == type).count();
    }
}
