package com.shim369.reciperev.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** SCR-04 レシピ基本情報(名前・説明)の入力。ここを変えても版は増えない */
public class RecipeBasicForm {

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
