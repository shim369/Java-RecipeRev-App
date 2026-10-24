package com.shim369.reciperev.dto;

import com.shim369.reciperev.domain.CookLog;
import com.shim369.reciperev.domain.Recipe;
import com.shim369.reciperev.domain.RecipeVersion;
import java.math.BigDecimal;
import java.util.List;

/**
 * SCR-06 版詳細の読み取りモデル。
 *
 * @param currentFinalNo 今の決定版の版番号(なければ null)。「決定版にする」の確認文に使う
 * @param averageRating  この版の平均評価(調理ログがなければ null)
 * @param logs           この版の調理ログ(新しい順)
 */
public record VersionDetail(Recipe recipe, RecipeVersion version, Integer currentFinalNo, BigDecimal averageRating,
                            List<CookLog> logs) {

    /** 1つ前の版の番号(第1版なら null) */
    public Integer previousNo() {
        return version.getVersionNo() > 1 ? version.getVersionNo() - 1 : null;
    }
}
