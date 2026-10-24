package com.shim369.reciperev.support;

import com.shim369.reciperev.dto.IngredientForm;
import com.shim369.reciperev.dto.RecipeCreateForm;
import com.shim369.reciperev.dto.VersionContentForm;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** テスト用の入力データを簡潔に作るためのヘルパ */
public final class TestData {

    private TestData() {
    }

    public static IngredientForm ing(String name, String amount, String unit) {
        return new IngredientForm(name, amount == null ? null : new BigDecimal(amount), unit, null);
    }

    public static RecipeCreateForm recipe(String name, String steps, IngredientForm... ingredients) {
        RecipeCreateForm form = new RecipeCreateForm();
        form.setName(name);
        form.setSteps(steps);
        form.setIngredients(new ArrayList<>(List.of(ingredients)));
        return form;
    }

    public static VersionContentForm version(String steps, IngredientForm... ingredients) {
        VersionContentForm form = new VersionContentForm();
        form.setSteps(steps);
        form.setIngredients(new ArrayList<>(List.of(ingredients)));
        return form;
    }
}
