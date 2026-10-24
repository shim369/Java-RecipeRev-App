package com.shim369.reciperev.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.shim369.reciperev.domain.ChangeType;
import com.shim369.reciperev.domain.IngredientDiff;
import com.shim369.reciperev.domain.IngredientLine;
import com.shim369.reciperev.domain.RecipeDiff;
import com.shim369.reciperev.domain.StepDiff;
import com.shim369.reciperev.domain.VersionContent;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * RecipeDiffService の単体テスト(UT-DIFF-xx)。
 * DB も Spring も使わず new するだけなので、数ミリ秒で終わる。
 */
class RecipeDiffServiceTest {

    private final RecipeDiffService service = new RecipeDiffService();

    private static IngredientLine line(String name, String amount, String unit) {
        return new IngredientLine(name, amount == null ? null : new BigDecimal(amount), unit, null);
    }

    private static VersionContent content(List<IngredientLine> ingredients, String... steps) {
        return new VersionContent(ingredients, List.of(steps));
    }

    @Nested
    @DisplayName("材料の差分")
    class Ingredients {

        @Test
        @DisplayName("UT-DIFF-01: 追加・削除・変更・変更なしを判定できる")
        void classifiesAllChangeTypes() {
            List<IngredientDiff> diff = service.diffIngredients(
                    List.of(line("チョコ", "100", "g"), line("砂糖", "30", "g"), line("バター", "50", "g")),
                    List.of(line("チョコ", "100", "g"), line("砂糖", "40", "g"), line("生クリーム", "50", "ml")));

            assertThat(diff).extracting(IngredientDiff::name, IngredientDiff::type).containsExactly(
                    tuple("チョコ", ChangeType.UNCHANGED),
                    tuple("砂糖", ChangeType.CHANGED),
                    tuple("バター", ChangeType.REMOVED),     // 同じ位置では削除行を先に出す
                    tuple("生クリーム", ChangeType.ADDED));
        }

        @Test
        @DisplayName("UT-DIFF-02: 100 と 100.00 は同じ分量とみなす(BigDecimal の scale の違いを無視)")
        void ignoresBigDecimalScale() {
            List<IngredientDiff> diff = service.diffIngredients(
                    List.of(line("砂糖", "100", "g")), List.of(line("砂糖", "100.00", "g")));
            assertThat(diff).extracting(IngredientDiff::type).containsExactly(ChangeType.UNCHANGED);
        }

        @Test
        @DisplayName("UT-DIFF-03: 単位だけ・メモだけの違いも変更として扱う")
        void unitOrNoteChangeIsChanged() {
            List<IngredientDiff> diff = service.diffIngredients(
                    List.of(line("塩", null, null), line("砂糖", "1", "大さじ")),
                    List.of(new IngredientLine("塩", null, null, "少々"), line("砂糖", "1", "小さじ")));
            assertThat(diff).extracting(IngredientDiff::type).containsOnly(ChangeType.CHANGED);
        }

        @Test
        @DisplayName("UT-DIFF-04: 分量が両方とも空欄なら変更なし(NULL 同士は等しい)")
        void bothNullAmountsAreEqual() {
            List<IngredientDiff> diff = service.diffIngredients(
                    List.of(line("こしょう", null, null)), List.of(line("こしょう", null, null)));
            assertThat(diff).extracting(IngredientDiff::type).containsExactly(ChangeType.UNCHANGED);
        }

        @Test
        @DisplayName("UT-DIFF-05: 空欄 → 数値 は変更")
        void nullToValueIsChanged() {
            List<IngredientDiff> diff = service.diffIngredients(
                    List.of(line("塩", null, "g")), List.of(line("塩", "2", "g")));
            assertThat(diff).extracting(IngredientDiff::type).containsExactly(ChangeType.CHANGED);
        }

        @Test
        @DisplayName("UT-DIFF-06: 変更前と変更後の値を両方保持する")
        void keepsBeforeAndAfter() {
            IngredientDiff d = service.diffIngredients(
                    List.of(line("砂糖", "30", "g")), List.of(line("砂糖", "40", "g"))).get(0);
            assertThat(d.before().amountLabel()).isEqualTo("30 g");
            assertThat(d.after().amountLabel()).isEqualTo("40 g");
        }
    }

    @Nested
    @DisplayName("手順の差分(行単位 LCS)")
    class Steps {

        @Test
        @DisplayName("UT-DIFF-11: 同じ手順なら全行が変更なし")
        void identical() {
            List<StepDiff> diff = service.diffSteps(List.of("a", "b"), List.of("a", "b"));
            assertThat(diff).extracting(StepDiff::type).containsOnly(ChangeType.UNCHANGED);
        }

        @Test
        @DisplayName("UT-DIFF-12: 途中に挿入した行は追加、他は変更なし")
        void insertInMiddle() {
            List<StepDiff> diff = service.diffSteps(List.of("a", "c"), List.of("a", "b", "c"));
            assertThat(diff).extracting(StepDiff::type, StepDiff::after).containsExactly(
                    tuple(ChangeType.UNCHANGED, "a"), tuple(ChangeType.ADDED, "b"), tuple(ChangeType.UNCHANGED, "c"));
        }

        @Test
        @DisplayName("UT-DIFF-13: 消した行は削除")
        void deleteLine() {
            List<StepDiff> diff = service.diffSteps(List.of("a", "b", "c"), List.of("a", "c"));
            assertThat(diff).extracting(StepDiff::type, StepDiff::before).containsExactly(
                    tuple(ChangeType.UNCHANGED, "a"), tuple(ChangeType.REMOVED, "b"), tuple(ChangeType.UNCHANGED, "c"));
        }

        @Test
        @DisplayName("UT-DIFF-14: 同じ位置の 削除1 + 追加1 は変更の対になる")
        void replacedLineIsChanged() {
            List<StepDiff> diff = service.diffSteps(List.of("a", "170度で30分焼く", "c"), List.of("a", "160度で40分焼く", "c"));
            assertThat(diff.get(1)).isEqualTo(new StepDiff(ChangeType.CHANGED, "170度で30分焼く", "160度で40分焼く"));
        }

        @Test
        @DisplayName("UT-DIFF-15: 削除2 + 追加1 なら 変更1 + 削除1")
        void unevenHunk() {
            List<StepDiff> diff = service.diffSteps(List.of("x", "y", "z"), List.of("w", "z"));
            assertThat(diff).extracting(StepDiff::type).containsExactly(
                    ChangeType.CHANGED, ChangeType.REMOVED, ChangeType.UNCHANGED);
            assertThat(diff.get(0)).isEqualTo(new StepDiff(ChangeType.CHANGED, "x", "w"));
        }

        @Test
        @DisplayName("UT-DIFF-16: 並べ替えは LCS で一致しない側が削除+追加(変更)になる")
        void reorder() {
            List<StepDiff> diff = service.diffSteps(List.of("a", "b"), List.of("b", "a"));
            assertThat(diff).hasSize(3);
            assertThat(diff).extracting(StepDiff::type).contains(ChangeType.UNCHANGED);
        }

        @Test
        @DisplayName("UT-DIFF-17: 空 → 手順あり は全行追加、手順あり → 空 は全行削除")
        void emptySides() {
            assertThat(service.diffSteps(List.of(), List.of("a", "b"))).extracting(StepDiff::type)
                    .containsExactly(ChangeType.ADDED, ChangeType.ADDED);
            assertThat(service.diffSteps(List.of("a"), List.of())).extracting(StepDiff::type)
                    .containsExactly(ChangeType.REMOVED);
        }
    }

    @Nested
    @DisplayName("全体")
    class Whole {

        @Test
        @DisplayName("UT-DIFF-21: 内容が同じなら hasChanges() は false(BR-03 の判定)")
        void noChanges() {
            VersionContent c = content(List.of(line("砂糖", "30", "g")), "混ぜる", "焼く");
            assertThat(service.diff(c, c).hasChanges()).isFalse();
        }

        @Test
        @DisplayName("UT-DIFF-22: 材料の並び順だけ変えても変更なし(材料は名前で照合するため)")
        void ingredientOrderOnly() {
            VersionContent from = content(List.of(line("A", "1", "g"), line("B", "2", "g")), "x");
            VersionContent to = content(List.of(line("B", "2", "g"), line("A", "1", "g")), "x");
            assertThat(service.diff(from, to).hasChanges()).isFalse();
        }

        @Test
        @DisplayName("UT-DIFF-23: 種類ごとの件数を数えられる")
        void counts() {
            RecipeDiff diff = service.diff(
                    content(List.of(line("A", "1", "g"), line("B", "1", "g")), "x", "y"),
                    content(List.of(line("A", "2", "g"), line("C", "1", "g")), "x", "z", "w"));
            assertThat(diff.count(ChangeType.ADDED)).isEqualTo(2);   // 材料C, 手順w
            assertThat(diff.count(ChangeType.REMOVED)).isEqualTo(1); // 材料B
            assertThat(diff.count(ChangeType.CHANGED)).isEqualTo(2); // 材料A, 手順 y→z
            assertThat(diff.hasChanges()).isTrue();
        }
    }
}
