package com.shim369.reciperev.repository;

import com.shim369.reciperev.domain.RecipeVersion;
import com.shim369.reciperev.domain.VersionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeVersionRepository extends JpaRepository<RecipeVersion, Long> {

    /** 材料も一緒に1回のSQLで読む(@EntityGraph)。版詳細で材料を必ず表示するので、N+1 を避ける */
    @EntityGraph(attributePaths = {"ingredients", "basedOn"})
    Optional<RecipeVersion> findByRecipeIdAndVersionNo(Long recipeId, int versionNo);

    List<RecipeVersion> findByRecipeIdOrderByVersionNoDesc(Long recipeId);

    Optional<RecipeVersion> findByRecipeIdAndStatus(Long recipeId, VersionStatus status);

    @Query("select coalesce(max(v.versionNo), 0) from RecipeVersion v where v.recipe.id = :recipeId")
    int findMaxVersionNo(@Param("recipeId") Long recipeId);

    /**
     * BR-06: 今の決定版を試作中に戻す(一括 UPDATE)。
     * flushAutomatically: 実行前に、まだDBに書いていない変更を先に書き出す。
     * clearAutomatically: 実行後に永続化コンテキストを空にして、古い状態のエンティティが残らないようにする。
     * updated_at は MySQL の ON UPDATE に任せず明示する(アプリと同じ時計にそろえるため)。
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RecipeVersion v
               set v.status = com.shim369.reciperev.domain.VersionStatus.TRIAL, v.updatedAt = :now
             where v.recipe.id = :recipeId
               and v.status = com.shim369.reciperev.domain.VersionStatus.FINAL
            """)
    int demoteFinalToTrial(@Param("recipeId") Long recipeId, @Param("now") LocalDateTime now);
}
