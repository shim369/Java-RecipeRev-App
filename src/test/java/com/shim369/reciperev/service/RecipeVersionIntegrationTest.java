package com.shim369.reciperev.service;

import static com.shim369.reciperev.support.TestData.ing;
import static com.shim369.reciperev.support.TestData.recipe;
import static com.shim369.reciperev.support.TestData.version;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shim369.reciperev.domain.Recipe;
import com.shim369.reciperev.domain.RecipeVersion;
import com.shim369.reciperev.domain.VersionStatus;
import com.shim369.reciperev.dto.FinalizeResult;
import com.shim369.reciperev.exception.BusinessRuleException;
import com.shim369.reciperev.exception.NoChangesException;
import com.shim369.reciperev.exception.NotFoundException;
import com.shim369.reciperev.support.IntegrationTestBase;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

/** レシピ・版・承認フローの結合テスト(IT-VER-xx)。本物の MySQL で、トランザクションと制約まで確かめる */
class RecipeVersionIntegrationTest extends IntegrationTestBase {

    @Autowired
    RecipeService recipeService;
    @Autowired
    VersionService versionService;

    long recipeId;

    @BeforeEach
    void createRecipe() {
        Recipe r = recipeService.create(recipe("ガトーショコラ", "溶かす\n焼く",
                ing("チョコ", "100", "g"), ing("砂糖", "30", "g")));
        recipeId = r.getId();
    }

    private String statuses() {
        return String.join(",", jdbcTemplate.queryForList(
                "SELECT CONCAT(version_no, ':', status) FROM recipe_versions WHERE recipe_id = ? ORDER BY version_no",
                String.class, recipeId));
    }

    @Test
    @DisplayName("IT-VER-01: レシピ登録で第1版(下書き)と材料が1トランザクションで作られる")
    void createRecipeCreatesFirstVersion() {
        RecipeVersion v1 = versionService.getVersion(recipeId, 1);
        assertThat(v1.getStatus()).isEqualTo(VersionStatus.DRAFT);
        assertThat(v1.getBasedOn()).isNull();
        assertThat(v1.getIngredients()).extracting("name").containsExactly("チョコ", "砂糖");
        assertThat(v1.getStepList()).containsExactly("溶かす", "焼く");
    }

    @Test
    @DisplayName("IT-VER-02: 新しい版は 最大版番号+1・下書き・派生元つき で作られ、派生元は変わらない(BR-01,02,04)")
    void createVersion() {
        versionService.createVersion(recipeId, 1, version("溶かす\n焼く", ing("チョコ", "100", "g"), ing("砂糖", "40", "g")));
        // 最新版でない第1版を元にしても、番号は 3 になる
        RecipeVersion v3 = versionService.createVersion(recipeId, 1,
                version("溶かす\n長めに焼く", ing("チョコ", "100", "g"), ing("砂糖", "30", "g")));

        assertThat(v3.getVersionNo()).isEqualTo(3);
        assertThat(versionService.getVersion(recipeId, 3).getBasedOn().getVersionNo()).isEqualTo(1);
        assertThat(versionService.getVersion(recipeId, 1).getIngredients())
                .extracting(i -> i.getName() + "=" + i.getAmount().toPlainString())
                .containsExactly("チョコ=100.00", "砂糖=30.00");
        assertThat(statuses()).isEqualTo("1:DRAFT,2:DRAFT,3:DRAFT");
    }

    @Test
    @DisplayName("IT-VER-03: 派生元と内容が同じなら版を作らない(BR-03)")
    void noChanges() {
        assertThatThrownBy(() -> versionService.createVersion(recipeId, 1,
                version("溶かす\n\n焼く ", ing("チョコ", "100.00", "g"), ing("砂糖", "30", "g"))))
                .isInstanceOf(NoChangesException.class);
        assertThat(versionService.latestVersionNo(recipeId)).isEqualTo(1);
    }

    @Test
    @DisplayName("IT-VER-04: 同時に版を作っても版番号は重複しない(FOR UPDATE による直列化)")
    void concurrentVersionCreation() throws Exception {
        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Callable<Integer>> tasks = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                String sugar = String.valueOf(40 + i);
                tasks.add(() -> {
                    start.await();
                    return versionService.createVersion(recipeId, 1,
                            version("溶かす\n焼く", ing("チョコ", "100", "g"), ing("砂糖", sugar, "g"))).getVersionNo();
                });
            }
            List<Future<Integer>> futures = tasks.stream().map(pool::submit).toList();
            start.countDown(); // 全スレッドを同時にスタートさせる
            List<Integer> numbers = new java.util.ArrayList<>();
            for (Future<Integer> f : futures) {
                numbers.add(f.get());
            }
            assertThat(numbers).containsExactlyInAnyOrder(2, 3, 4, 5);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("IT-VER-05: 決定版を切り替えると、前の決定版は試作中に戻る(BR-05,06)")
    void finalizeSwitches() {
        versionService.createVersion(recipeId, 1, version("溶かす\n焼く", ing("チョコ", "120", "g")));

        FinalizeResult first = versionService.finalizeVersion(recipeId, 1);
        assertThat(first.previousFinalNo()).isNull();
        assertThat(statuses()).isEqualTo("1:FINAL,2:DRAFT");

        FinalizeResult second = versionService.finalizeVersion(recipeId, 2);
        assertThat(second.previousFinalNo()).isEqualTo(1);
        assertThat(second.message()).isEqualTo("第2版を決定版にしました(第1版は試作中に戻りました)");
        assertThat(statuses()).isEqualTo("1:TRIAL,2:FINAL");
    }

    @Test
    @DisplayName("IT-VER-06: 決定版をもう一度決定版にはできない/試作中を再度試作開始できない")
    void invalidTransitions() {
        versionService.finalizeVersion(recipeId, 1);
        assertThatThrownBy(() -> versionService.finalizeVersion(recipeId, 1)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> versionService.startTrial(recipeId, 1)).isInstanceOf(BusinessRuleException.class);
        assertThat(statuses()).isEqualTo("1:FINAL"); // 失敗した操作はロールバックされている
    }

    @Test
    @DisplayName("IT-VER-07: DB の一意制約が「決定版は1レシピ1件」を直接の UPDATE でも守る")
    void uniqueFinalConstraint() {
        versionService.createVersion(recipeId, 1, version("焼く", ing("チョコ", "100", "g")));
        jdbcTemplate.update("UPDATE recipe_versions SET status = 'FINAL' WHERE recipe_id = ? AND version_no = 1", recipeId);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE recipe_versions SET status = 'FINAL' WHERE recipe_id = ? AND version_no = 2", recipeId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_recipe_versions_final");
    }

    @Test
    @DisplayName("IT-VER-08: DB の一意制約が版番号の重複を防ぐ/CHECK 制約が不正な状態を防ぐ")
    void otherConstraints() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO recipe_versions (recipe_id, version_no, status, steps) VALUES (?, 1, 'DRAFT', 'x')", recipeId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_recipe_versions_recipe_no");
        // CHECK 制約違反(MySQL エラー 3819)は Spring では UncategorizedSQLException になる
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE recipe_versions SET status = 'APPROVED' WHERE recipe_id = ?", recipeId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_recipe_versions_status");
    }

    @Test
    @DisplayName("IT-VER-09: 論理削除したレシピは一覧に出ず、参照すると NotFound(BR-09)。版は DB に残る")
    void softDelete() {
        recipeService.delete(recipeId);
        assertThat(recipeService.listSummaries()).isEmpty();
        assertThatThrownBy(() -> recipeService.getDetail(recipeId)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> versionService.getVersion(recipeId, 1)).isInstanceOf(NotFoundException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recipe_versions WHERE recipe_id = ?",
                Integer.class, recipeId)).isEqualTo(1);
    }

    @Test
    @DisplayName("IT-VER-10: レシピ詳細の代表版は 決定版があれば決定版、なければ最新版")
    void representativeVersion() {
        versionService.createVersion(recipeId, 1, version("焼く", ing("チョコ", "100", "g")));
        assertThat(recipeService.getDetail(recipeId).representative().getVersionNo()).isEqualTo(2);
        versionService.finalizeVersion(recipeId, 1);
        assertThat(recipeService.getDetail(recipeId).representative().getVersionNo()).isEqualTo(1);
    }

    @Test
    @DisplayName("IT-VER-11: 同じ版どうしは比較できない")
    void compareSameVersion() {
        assertThatThrownBy(() -> versionService.compare(recipeId, 1, 1)).isInstanceOf(BusinessRuleException.class);
    }
}
