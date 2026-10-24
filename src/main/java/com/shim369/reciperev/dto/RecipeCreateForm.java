package com.shim369.reciperev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SCR-02 レシピ新規登録 = 基本情報 + 第1版の中身。
 * 材料・手順の検証ルールは VersionContentForm を継承して共通化する。
 */
public class RecipeCreateForm extends VersionContentForm {

    @NotBlank(message = "{validation.name.required}")
    @Size(max = 100, message = "{validation.name.size}")
    private String name;

    @Size(max = 1000, message = "{validation.description.size}")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
