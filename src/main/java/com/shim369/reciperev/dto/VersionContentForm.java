package com.shim369.reciperev.dto;

import com.shim369.reciperev.domain.VersionContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 版の中身(材料・手順・変更メモ)の入力。レシピ新規登録(SCR-02)と版編集(SCR-05)で共通に使う。
 * 「1つの項目では判定できないルール」は @AssertTrue のメソッドで書く(PHPでいうカスタムバリデーションのクロージャに近い)。
 * message の "{キー}" は messages*.properties から利用者の言語で取り出される(Spring Boot が MessageSource と連携させている)。
 */
public class VersionContentForm {

    public static final int MAX_STEPS = 50;
    public static final int MAX_STEP_LENGTH = 500;

    @Valid // リストの中の IngredientForm も1件ずつ検証する
    @Size(min = 1, max = 50, message = "{validation.ingredients.size}")
    private List<IngredientForm> ingredients = new ArrayList<>();

    private String steps;

    @Size(max = 200, message = "{validation.changeNote.size}")
    private String changeNote;

    /** BR-11: 材料名は版の中で重複しない */
    @AssertTrue(message = "{validation.ingredients.unique}")
    public boolean isIngredientNamesUnique() {
        Set<String> seen = new HashSet<>();
        for (IngredientForm f : ingredients) {
            if (f.getName() != null && !f.getName().isBlank() && !seen.add(f.getName().strip())) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "{validation.steps.count}")
    public boolean isStepCountValid() {
        int count = VersionContent.parseSteps(steps).size();
        return count >= 1 && count <= MAX_STEPS;
    }

    @AssertTrue(message = "{validation.steps.length}")
    public boolean isStepLengthValid() {
        return VersionContent.parseSteps(steps).stream().allMatch(s -> s.length() <= MAX_STEP_LENGTH);
    }

    public VersionContent toContent() {
        return new VersionContent(
                ingredients.stream().map(IngredientForm::toLine).toList(),
                VersionContent.parseSteps(steps));
    }

    /** 既存の版の内容をフォームの初期値にする(SCR-05「この版を元に編集」) */
    public void fill(VersionContent content) {
        this.ingredients = new ArrayList<>(content.ingredients().stream().map(IngredientForm::from).toList());
        this.steps = content.stepsAsText();
    }

    public List<IngredientForm> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<IngredientForm> ingredients) {
        this.ingredients = ingredients;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }
}
