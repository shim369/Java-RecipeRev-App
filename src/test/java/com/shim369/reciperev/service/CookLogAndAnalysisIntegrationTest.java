package com.shim369.reciperev.service;

import static com.shim369.reciperev.support.TestData.ing;
import static com.shim369.reciperev.support.TestData.recipe;
import static com.shim369.reciperev.support.TestData.version;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import com.shim369.reciperev.config.RecipeRevProperties;
import com.shim369.reciperev.domain.VersionStatus;
import com.shim369.reciperev.dto.AnalysisResult;
import com.shim369.reciperev.dto.CookLogForm;
import com.shim369.reciperev.exception.AnalysisException;
import com.shim369.reciperev.exception.NotFoundException;
import com.shim369.reciperev.support.IntegrationTestBase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

/** 調理ログ(写真)と傾向分析の結合テスト(IT-LOG-xx / IT-ANA-xx) */
class CookLogAndAnalysisIntegrationTest extends IntegrationTestBase {

    static final byte[] PNG = PhotoStorageServiceTest.PNG;

    @Autowired
    RecipeService recipeService;
    @Autowired
    VersionService versionService;
    @Autowired
    CookLogService cookLogService;
    @Autowired
    AnalysisService analysisService;
    @Autowired
    RecipeRevProperties properties;

    long recipeId;

    @BeforeEach
    void setUp() {
        recipeId = recipeService.create(recipe("クッキー", "混ぜる\n焼く", ing("砂糖", "30", "g"), ing("卵", "1", "個"))).getId();
        versionService.createVersion(recipeId, 1, version("混ぜる\n焼く", ing("砂糖", "40", "g"), ing("卵", "1", "個")));
        versionService.createVersion(recipeId, 2, version("混ぜる\n焼く", ing("砂糖", "50", "g"), ing("卵", "1", "個")));
    }

    private CookLogForm form(int rating, MockMultipartFile photo) {
        CookLogForm form = new CookLogForm();
        form.setCookedOn(LocalDate.now());
        form.setRating(rating);
        form.setMemo("メモ");
        form.setPhoto(photo);
        return form;
    }

    private String photoPathOfLatestLog() {
        return jdbcTemplate.queryForObject("SELECT photo_path FROM cook_logs ORDER BY id DESC LIMIT 1", String.class);
    }

    @Test
    @DisplayName("IT-LOG-01: 下書きの版に調理ログを登録すると試作中になる(BR-08)")
    void draftBecomesTrial() {
        assertThat(cookLogService.register(recipeId, 1, form(4, null))).isTrue();
        assertThat(versionService.getVersion(recipeId, 1).getStatus()).isEqualTo(VersionStatus.TRIAL);
        // 2回目以降は状態を変えない
        assertThat(cookLogService.register(recipeId, 1, form(5, null))).isFalse();
    }

    @Test
    @DisplayName("IT-LOG-02: 決定版に調理ログを登録しても状態は変わらない")
    void finalStaysFinal() {
        versionService.finalizeVersion(recipeId, 2);
        cookLogService.register(recipeId, 2, form(5, null));
        assertThat(versionService.getVersion(recipeId, 2).getStatus()).isEqualTo(VersionStatus.FINAL);
    }

    @Test
    @DisplayName("IT-LOG-03: 写真つきで登録すると uploads に保存され、ログ削除のコミット後に写真も消える")
    void photoLifecycle() {
        cookLogService.register(recipeId, 1, form(4, new MockMultipartFile("photo", "a.png", "image/png", PNG)));
        String photo = photoPathOfLatestLog();
        Path file = properties.uploadDir().resolve(photo);
        assertThat(file).exists();

        long logId = jdbcTemplate.queryForObject("SELECT id FROM cook_logs WHERE photo_path = ?", Long.class, photo);
        assertThat(cookLogService.delete(recipeId, logId)).isEqualTo(1);
        assertThat(file).doesNotExist();
    }

    @Test
    @DisplayName("IT-LOG-04: 登録がロールバックされたら、保存した写真も消える(存在しない版)")
    void photoRemovedOnRollback() throws Exception {
        long before = countUploads();
        assertThatThrownBy(() -> cookLogService.register(recipeId, 99,
                form(4, new MockMultipartFile("photo", "a.png", "image/png", PNG))))
                .isInstanceOf(NotFoundException.class);
        assertThat(countUploads()).isEqualTo(before);
    }

    @Test
    @DisplayName("IT-LOG-05: 別のレシピの調理ログは削除できない")
    void cannotDeleteOtherRecipesLog() {
        cookLogService.register(recipeId, 1, form(3, null));
        long logId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM cook_logs", Long.class);
        long otherRecipe = recipeService.create(recipe("別", "x", ing("a", "1", "g"))).getId();
        assertThatThrownBy(() -> cookLogService.delete(otherRecipe, logId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("IT-ANA-01: CSV出力 → Python 実行 → JSON 読み込みまで通しで動く")
    void runAnalysisEndToEnd() throws Exception {
        assumeThat(pandasAvailable()).as("python3 + pandas が必要").isTrue();
        cookLogService.register(recipeId, 1, form(2, null));
        cookLogService.register(recipeId, 2, form(3, null));
        cookLogService.register(recipeId, 3, form(5, null));

        AnalysisResult result = analysisService.run(recipeId);

        assertThat(result.recipeId()).isEqualTo(recipeId);
        assertThat(result.versionCount()).isEqualTo(3);
        assertThat(result.results()).singleElement().satisfies(c -> {
            assertThat(c.ingredient()).isEqualTo("砂糖");
            assertThat(c.strength()).isEqualTo("STRONG");
            assertThat(c.direction()).isEqualTo("POSITIVE");
        });
        assertThat(result.insufficient()).extracting(AnalysisResult.Insufficient::reason)
                .containsExactly("NO_AMOUNT_VARIATION"); // 卵はずっと1個
        assertThat(analysisService.loadLatestJson(recipeId)).get().asString().contains("\"recipeId\"");
    }

    @Test
    @DisplayName("IT-ANA-02: Python を起動できなければ AnalysisException(画面にメッセージを出せる)")
    void pythonNotFound() {
        RecipeRevProperties broken = new RecipeRevProperties(properties.uploadDir(),
                new RecipeRevProperties.Analysis("python-does-not-exist", properties.analysis().script(),
                        properties.analysis().workDir(), 5));
        AnalysisService service = new AnalysisService(
                new com.shim369.reciperev.repository.RecipeQueryRepository(
                        org.springframework.jdbc.core.simple.JdbcClient.create(jdbcTemplate)),
                recipeService, new com.fasterxml.jackson.databind.ObjectMapper(), broken);
        assertThatThrownBy(() -> service.run(recipeId))
                .isInstanceOf(AnalysisException.class)
                .hasMessage("error.analysis.pythonNotFound");
    }

    private long countUploads() throws Exception {
        Path dir = properties.uploadDir();
        if (!Files.exists(dir)) {
            return 0;
        }
        try (var files = Files.list(dir)) {
            return files.count();
        }
    }

    private static boolean pandasAvailable() {
        try {
            Process p = new ProcessBuilder("python3", "-c", "import pandas").start();
            return p.waitFor(20, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
