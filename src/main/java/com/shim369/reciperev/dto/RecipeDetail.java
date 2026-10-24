package com.shim369.reciperev.dto;

import com.shim369.reciperev.domain.Recipe;
import com.shim369.reciperev.domain.RecipeVersion;
import java.util.List;

/**
 * SCR-03 レシピ詳細に必要なものを1つにまとめた読み取りモデル。
 *
 * @param representative 代表版(決定版があれば決定版、なければ最新版)
 * @param versions       版履歴(版番号の昇順)
 */
public record RecipeDetail(Recipe recipe, RecipeVersion representative, List<VersionSummary> versions) {

    public int latestVersionNo() {
        return versions.isEmpty() ? 0 : versions.get(versions.size() - 1).versionNo();
    }
}
