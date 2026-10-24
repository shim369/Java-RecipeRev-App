package com.shim369.reciperev.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shim369.reciperev.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ドメインモデル(値オブジェクト・状態遷移)の単体テスト(UT-DOM-xx) */
class DomainTest {

    private static RecipeVersion newVersion() {
        return RecipeVersion.create(new Recipe("カレー", null), 1, null,
                new VersionContent(List.of(new IngredientLine("塩", BigDecimal.ONE, "g", null)), List.of("煮る")), null);
    }

    @Test
    @DisplayName("UT-DOM-01: 手順は改行で分割し、前後の空白と空行を捨てる(\\r\\n も対応)")
    void parseSteps() {
        assertThat(VersionContent.parseSteps("  切る \r\n\n炒める\r煮る\n  ")).containsExactly("切る", "炒める", "煮る");
        assertThat(VersionContent.parseSteps(null)).isEmpty();
    }

    @Test
    @DisplayName("UT-DOM-02: IngredientLine は空文字を null にそろえ、前後の空白を除く")
    void ingredientLineNormalizes() {
        IngredientLine line = new IngredientLine(" 砂糖 ", null, " ", "");
        assertThat(line.name()).isEqualTo("砂糖");
        assertThat(line.unit()).isNull();
        assertThat(line.note()).isNull();
    }

    @Test
    @DisplayName("UT-DOM-03: 分量の表示ラベル")
    void amountLabel() {
        assertThat(new IngredientLine("a", new BigDecimal("120.00"), "g", null).amountLabel()).isEqualTo("120 g");
        assertThat(new IngredientLine("a", new BigDecimal("1.50"), "大さじ", null).amountLabel()).isEqualTo("1.5 大さじ");
        assertThat(new IngredientLine("a", null, null, "少々").amountLabel()).isEqualTo("少々");
        assertThat(new IngredientLine("a", BigDecimal.ONE, "個", "室温").amountLabel()).isEqualTo("1 個(室温)");
        assertThat(new IngredientLine("a", null, null, null).amountLabel()).isEqualTo("—");
    }

    @Test
    @DisplayName("UT-DOM-04: 新しい版は必ず下書き(BR-04)。材料は入力順に sort_order が振られる")
    void newVersionIsDraft() {
        RecipeVersion v = newVersion();
        assertThat(v.getStatus()).isEqualTo(VersionStatus.DRAFT);
        assertThat(v.getIngredients()).extracting(VersionIngredient::getSortOrder).containsExactly(1);
    }

    @Test
    @DisplayName("UT-DOM-05: 下書き → 試作中 → 決定版 と遷移できる")
    void transitions() {
        RecipeVersion v = newVersion();
        v.startTrial();
        assertThat(v.getStatus()).isEqualTo(VersionStatus.TRIAL);
        v.markFinal();
        assertThat(v.isFinal()).isTrue();
    }

    @Test
    @DisplayName("UT-DOM-06: 下書きから直接決定版にできる")
    void draftToFinal() {
        RecipeVersion v = newVersion();
        v.markFinal();
        assertThat(v.getStatus()).isEqualTo(VersionStatus.FINAL);
    }

    @Test
    @DisplayName("UT-DOM-07: 試作中・決定版で「試作を開始」はできない(BR-07)")
    void cannotStartTrialTwice() {
        RecipeVersion v = newVersion();
        v.startTrial();
        assertThatThrownBy(v::startTrial).isInstanceOf(BusinessRuleException.class).hasMessageContaining("試作中");
    }

    @Test
    @DisplayName("UT-DOM-08: 決定版をもう一度決定版にはできない")
    void cannotFinalizeTwice() {
        RecipeVersion v = newVersion();
        v.markFinal();
        assertThatThrownBy(v::markFinal).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("UT-DOM-09: 変更メモの空文字は null として保存する")
    void blankChangeNote() {
        RecipeVersion v = RecipeVersion.create(new Recipe("x", null), 1, null,
                new VersionContent(List.of(), List.of("a")), "  ");
        assertThat(v.getChangeNote()).isNull();
    }
}
