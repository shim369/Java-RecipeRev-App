package com.shim369.reciperev.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 材料1行の「値」。DBやHTTPに依存しない record なので、差分計算や単体テストでそのまま使える。
 *
 * @param name   材料名(版内で一意。差分の照合キー)
 * @param amount 分量の数値。「少々」などは null
 * @param unit   単位。未入力は null
 * @param note   メモ。未入力は null
 */
public record IngredientLine(String name, BigDecimal amount, String unit, String note) {

    public IngredientLine {
        Objects.requireNonNull(name, "name");
        // 空文字と null を同じ扱いにしておくと、比較や表示で分岐が減る
        name = name.strip();
        unit = blankToNull(unit);
        note = blankToNull(note);
    }

    /**
     * 分量・単位・メモがすべて同じか。
     * BigDecimal は equals だと 100 と 100.00 を別物と判定するため compareTo で比べる。
     */
    public boolean sameContentAs(IngredientLine other) {
        boolean sameAmount = (amount == null || other.amount == null)
                ? amount == other.amount
                : amount.compareTo(other.amount) == 0;
        return sameAmount && Objects.equals(unit, other.unit) && Objects.equals(note, other.note);
    }

    /** 画面表示用: 「120 g」「少々」「1 pcs (room temp)」など(どの言語でも読めるよう半角かっこを使う) */
    public String amountLabel() {
        String quantity = ((amount == null ? "" : amount.stripTrailingZeros().toPlainString())
                + (unit == null ? "" : " " + unit)).strip();
        if (note == null) {
            return quantity.isEmpty() ? "—" : quantity;
        }
        return quantity.isEmpty() ? note : quantity + " (" + note + ")";
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }
}
