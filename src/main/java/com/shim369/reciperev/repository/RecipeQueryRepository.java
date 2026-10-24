package com.shim369.reciperev.repository;

import com.shim369.reciperev.domain.ChangeType;
import com.shim369.reciperev.dto.RecipeSummary;
import com.shim369.reciperev.dto.VersionSummary;
import com.shim369.reciperev.domain.VersionStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * 集計・版間比較など、JPA(エンティティ単位の操作)では書きにくい読み取り専用のSQLをまとめる。
 * JdbcClient は JdbcTemplate を使いやすくした Spring 6.1 の API。SQLは名前付きパラメータ(:recipeId)でバインドする。
 */
@Repository
public class RecipeQueryRepository {

    private final JdbcClient jdbc;

    public RecipeQueryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** SCR-01 レシピ一覧。決定版は生成列 final_marker のユニークインデックスで引く */
    public List<RecipeSummary> findRecipeSummaries() {
        return jdbc.sql("""
                SELECT r.id, r.name, r.description, r.updated_at,
                       (SELECT MAX(v.version_no) FROM recipe_versions v WHERE v.recipe_id = r.id) AS latest_no,
                       (SELECT v.version_no FROM recipe_versions v WHERE v.final_marker = r.id)   AS final_no,
                       (SELECT ROUND(AVG(l.rating), 2)
                          FROM cook_logs l JOIN recipe_versions v ON v.id = l.version_id
                         WHERE v.recipe_id = r.id)                                                 AS avg_rating
                  FROM recipes r
                 WHERE r.deleted_at IS NULL
                 ORDER BY r.updated_at DESC, r.id DESC
                """)
                .query((rs, i) -> new RecipeSummary(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("updated_at").toLocalDateTime(),
                        rs.getInt("latest_no"),
                        (Integer) rs.getObject("final_no", Integer.class),
                        rs.getBigDecimal("avg_rating")))
                .list();
    }

    /** 版ごとの平均評価と調理回数(SCR-03 版履歴・評価推移グラフ)。04_er_diagram.md 5.2 */
    public List<VersionSummary> findVersionSummaries(long recipeId) {
        return jdbc.sql("""
                SELECT v.version_no, v.status, v.change_note, v.created_at,
                       ROUND(AVG(l.rating), 2) AS avg_rating,
                       COUNT(l.id)             AS log_count
                  FROM recipe_versions v
                  LEFT JOIN cook_logs l ON l.version_id = v.id
                 WHERE v.recipe_id = :recipeId
                 GROUP BY v.id, v.version_no, v.status, v.change_note, v.created_at
                 ORDER BY v.version_no
                """)
                .param("recipeId", recipeId)
                .query((rs, i) -> new VersionSummary(
                        rs.getInt("version_no"),
                        VersionStatus.valueOf(rs.getString("status")),
                        rs.getString("change_note"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getBigDecimal("avg_rating"),
                        rs.getInt("log_count")))
                .list();
    }

    /** 1つの版の平均評価(調理ログがなければ null) */
    public BigDecimal findAverageRating(long versionId) {
        return jdbc.sql("SELECT ROUND(AVG(rating), 2) FROM cook_logs WHERE version_id = :versionId")
                .param("versionId", versionId)
                .query(BigDecimal.class)
                .optional()
                .orElse(null);
    }

    /**
     * 2つの版の材料の差分をSQLで求める(04_er_diagram.md 5.1)。
     * 画面表示は RecipeDiffService(Java)で計算するが、結合テストで「SQLとJavaの結果が一致するか」を確かめるのに使う。
     * MySQL には FULL OUTER JOIN がないため、両方の版の材料名一覧に LEFT JOIN する。
     * {@code <=>} は NULL 同士も「等しい」と判定する比較演算子。
     */
    public List<IngredientDiffRow> findIngredientDiff(long fromVersionId, long toVersionId) {
        return jdbc.sql("""
                SELECT n.name,
                       CASE
                         WHEN a.id IS NULL THEN 'ADDED'
                         WHEN b.id IS NULL THEN 'REMOVED'
                         WHEN NOT (a.amount <=> b.amount)
                           OR NOT (a.unit   <=> b.unit)
                           OR NOT (a.note   <=> b.note) THEN 'CHANGED'
                         ELSE 'UNCHANGED'
                       END AS change_type
                  FROM (SELECT DISTINCT name
                          FROM version_ingredients
                         WHERE version_id IN (:fromId, :toId)) n
                  LEFT JOIN version_ingredients a ON a.version_id = :fromId AND a.name = n.name
                  LEFT JOIN version_ingredients b ON b.version_id = :toId   AND b.name = n.name
                 ORDER BY COALESCE(b.sort_order, a.sort_order), b.id IS NOT NULL
                """)
                .param("fromId", fromVersionId)
                .param("toId", toVersionId)
                .query((rs, i) -> new IngredientDiffRow(rs.getString("name"), ChangeType.valueOf(rs.getString("change_type"))))
                .list();
    }

    public record IngredientDiffRow(String name, ChangeType type) {
    }
}
