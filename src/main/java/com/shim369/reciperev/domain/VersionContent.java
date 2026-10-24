package com.shim369.reciperev.domain;

import java.util.Arrays;
import java.util.List;

/**
 * 版の「中身」(材料と手順)のスナップショット。
 * 差分計算・変更なし判定(BR-03)の入力になる。エンティティと切り離しておくことで、
 * RecipeDiffService を DB なしで単体テストできる。
 */
public record VersionContent(List<IngredientLine> ingredients, List<String> steps) {

    public VersionContent {
        ingredients = List.copyOf(ingredients);
        steps = List.copyOf(steps);
    }

    /** テキストエリアの入力(改行区切り)を手順のリストにする。前後の空白と空行は捨てる */
    public static List<String> parseSteps(String text) {
        if (text == null) {
            return List.of();
        }
        return Arrays.stream(text.split("\\R")) // \R = あらゆる改行コード(\r\n, \n, \r)
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    /** DB の steps 列に保存する形(\n 区切り) */
    public String stepsAsText() {
        return String.join("\n", steps);
    }
}
