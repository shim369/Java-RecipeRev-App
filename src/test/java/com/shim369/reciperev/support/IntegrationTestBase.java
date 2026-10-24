package com.shim369.reciperev.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 結合テストの共通設定。
 * <ul>
 *   <li>Spring のアプリ全体を起動し、MySQL は Testcontainers のものを使う</li>
 *   <li>設定が同じテストクラスどうしでは Spring のコンテキスト(とコンテナ)が使い回されるので、2クラス目以降は速い</li>
 *   <li>サービスはトランザクションをコミットするので、各テストの前にテーブルを空にしてテスト同士を独立させる</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "reciperev.upload-dir=target/test-uploads",
        "reciperev.analysis.work-dir=target/test-analysis"
})
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // 外部キーの子テーブルから順に消す
        jdbcTemplate.update("DELETE FROM cook_logs");
        jdbcTemplate.update("UPDATE recipe_versions SET based_on_version_id = NULL");
        jdbcTemplate.update("DELETE FROM version_ingredients");
        jdbcTemplate.update("DELETE FROM recipe_versions");
        jdbcTemplate.update("DELETE FROM recipes");
    }
}
