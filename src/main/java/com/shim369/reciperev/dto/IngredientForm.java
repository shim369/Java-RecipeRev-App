package com.shim369.reciperev.dto;

import com.shim369.reciperev.domain.IngredientLine;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 材料1行の入力フォーム。HTML の name="ingredients[0].name" がこのフィールドにバインドされる。
 * フォーム(dto)とエンティティ(domain)を分けるのは、画面の都合(空欄・文字列)をDBの型に持ち込まないため。
 */
public class IngredientForm {

    @NotBlank(message = "{validation.ingredient.name.required}")
    @Size(max = 50, message = "{validation.ingredient.name.size}")
    private String name;

    @DecimalMin(value = "0", message = "{validation.amount.min}")
    @DecimalMax(value = "99999.99", message = "{validation.amount.max}")
    @Digits(integer = 5, fraction = 2, message = "{validation.amount.digits}")
    private BigDecimal amount;

    @Size(max = 20, message = "{validation.unit.size}")
    private String unit;

    @Size(max = 100, message = "{validation.note.size}")
    private String note;

    public IngredientForm() {
    }

    public IngredientForm(String name, BigDecimal amount, String unit, String note) {
        this.name = name;
        this.amount = amount;
        this.unit = unit;
        this.note = note;
    }

    /**
     * 既存の版の材料をフォームの初期値にする。DB の DECIMAL(8,2) は 120.00 のように返るので、
     * 入力欄には 120 と表示されるよう末尾の 0 を落とす(stripTrailingZeros だけだと 1E+2 表記になるため toPlainString を経由)。
     */
    public static IngredientForm from(IngredientLine line) {
        BigDecimal amount = line.amount() == null ? null : new BigDecimal(line.amount().stripTrailingZeros().toPlainString());
        return new IngredientForm(line.name(), amount, line.unit(), line.note());
    }

    public IngredientLine toLine() {
        return new IngredientLine(name, amount, unit, note);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
