package com.shim369.reciperev.repository;

import static com.shim369.reciperev.support.TestData.ing;
import static com.shim369.reciperev.support.TestData.recipe;
import static com.shim369.reciperev.support.TestData.version;
import static org.assertj.core.api.Assertions.assertThat;

import com.shim369.reciperev.domain.IngredientDiff;
import com.shim369.reciperev.domain.RecipeVersion;
import com.shim369.reciperev.dto.AnalysisRow;
import com.shim369.reciperev.dto.CookLogForm;
import com.shim369.reciperev.dto.RecipeSummary;
import com.shim369.reciperev.dto.VersionSummary;
import com.shim369.reciperev.repository.RecipeQueryRepository.IngredientDiffRow;
import com.shim369.reciperev.service.CookLogService;
import com.shim369.reciperev.service.RecipeDiffService;
import com.shim369.reciperev.service.RecipeService;
import com.shim369.reciperev.service.VersionService;
import com.shim369.reciperev.support.IntegrationTestBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 集計・版間比較SQLの結合テスト(IT-SQL-xx) */
class RecipeQueryRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    RecipeQueryRepository queryRepository;
    @Autowired
    RecipeService recipeService;
    @Autowired
    VersionService versionService;
    @Autowired
    CookLogService cookLogService;
    @Autowired
    RecipeDiffService diffService;

    long recipeId;

    @BeforeEach
    void setUp() {
        recipeId = recipeService.create(recipe("ガトーショコラ", "溶かす\n焼く",
                ing("チョコ", "100", "g"), ing("砂糖", "30", "g"), ing("バター", "50", "g"), ing("塩", null, null))).getId();
        versionService.createVersion(recipeId, 1, version("溶かす\n焼く",
                ing("チョコ", "100", "g"), ing("砂糖", "40", "g"), ing("生クリーム", "50", "ml"), ing("塩", null, null)));
        versionService.createVersion(recipeId, 2, version("溶かす\n長めに焼く",
                ing("チョコ", "120", "g"), ing("砂糖", "50", "g"), ing("塩", "1", "g")));
    }

    private void log(int versionNo, int rating) {
        CookLogForm form = new CookLogForm();
        form.setCookedOn(LocalDate.now());
        form.setRating(rating);
        cookLogService.register(recipeId, versionNo, form);
    }

    @Test
    @DisplayName("IT-SQL-01: 材料差分のSQL(LEFT JOIN + <=>)と Java の RecipeDiffService の結果が一致する")
    void sqlDiffMatchesJavaDiff() {
        for (int[] pair : new int[][]{{1, 2}, {2, 3}, {1, 3}, {3, 1}}) {
            RecipeVersion from = versionService.getVersion(recipeId, pair[0]);
            RecipeVersion to = versionService.getVersion(recipeId, pair[1]);

            List<IngredientDiffRow> sql = queryRepository.findIngredientDiff(from.getId(), to.getId());
            List<IngredientDiff> java = diffService.diff(from.toContent(), to.toContent()).ingredients();

            assertThat(sql).extracting(IngredientDiffRow::name, IngredientDiffRow::type)
                    .as("第%d版 → 第%d版", pair[0], pair[1])
                    .containsExactlyElementsOf(java.stream()
                            .map(d -> org.assertj.core.groups.Tuple.tuple(d.name(), d.type())).toList());
        }
    }

    @Test
    @DisplayName("IT-SQL-02: 版ごとの平均評価と調理回数(ログのない版は null / 0)")
    void versionSummaries() {
        log(1, 3);
        log(1, 4);
        log(3, 5);
        List<VersionSummary> s = queryRepository.findVersionSummaries(recipeId);
        assertThat(s).extracting(VersionSummary::versionNo).containsExactly(1, 2, 3);
        assertThat(s.get(0).averageRating()).isEqualByComparingTo("3.50");
        assertThat(s.get(0).logCount()).isEqualTo(2);
        assertThat(s.get(1).averageRating()).isNull();
        assertThat(s.get(1).logCount()).isZero();
    }

    @Test
    @DisplayName("IT-SQL-03: レシピ一覧の集計(最新版・決定版・全体平均)")
    void recipeSummaries() {
        log(1, 2);
        log(2, 4);
        versionService.finalizeVersion(recipeId, 2);
        RecipeSummary summary = queryRepository.findRecipeSummaries().get(0);
        assertThat(summary.latestVersionNo()).isEqualTo(3);
        assertThat(summary.finalVersionNo()).isEqualTo(2);
        assertThat(summary.averageRating()).isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("IT-SQL-04: 分析用の表 — 評価のある版だけ、無い材料は0、分量空欄の行は除外、単位違いは別材料")
    void analysisRows() {
        log(1, 3);
        log(2, 5);
        log(3, 4);
        List<AnalysisRow> rows = queryRepository.findAnalysisRows(recipeId);

        assertThat(rows).filteredOn(r -> r.ingredient().equals("生クリーム"))
                .extracting(AnalysisRow::versionNo, r -> r.amount().stripTrailingZeros().toPlainString())
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1, "0"),
                        org.assertj.core.groups.Tuple.tuple(2, "50"),
                        org.assertj.core.groups.Tuple.tuple(3, "0"));
        // 塩: 第1・2版は分量空欄(少々・単位なし)なので 0 にせず除外、第3版だけ 1g
        assertThat(rows).filteredOn(r -> r.ingredient().equals("塩"))
                .extracting(AnalysisRow::versionNo).containsExactly(3);
        assertThat(rows).filteredOn(r -> r.versionNo() == 2 && r.ingredient().equals("砂糖"))
                .extracting(AnalysisRow::avgRating).first().satisfies(v -> assertThat((BigDecimal) v).isEqualByComparingTo("5"));
    }

    @Test
    @DisplayName("IT-SQL-05: 調理ログが1件もなければ分析用の表は空")
    void analysisRowsEmpty() {
        assertThat(queryRepository.findAnalysisRows(recipeId)).isEmpty();
    }
}
