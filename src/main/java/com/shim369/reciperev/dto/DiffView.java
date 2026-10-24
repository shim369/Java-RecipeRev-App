package com.shim369.reciperev.dto;

import com.shim369.reciperev.domain.ChangeType;
import com.shim369.reciperev.domain.Recipe;
import com.shim369.reciperev.domain.RecipeDiff;
import java.util.List;

/** SCR-07 版比較の読み取りモデル */
public record DiffView(Recipe recipe, VersionSummary from, VersionSummary to, RecipeDiff diff,
                       List<VersionSummary> allVersions) {

    public long added() {
        return diff.count(ChangeType.ADDED);
    }

    public long removed() {
        return diff.count(ChangeType.REMOVED);
    }

    public long changed() {
        return diff.count(ChangeType.CHANGED);
    }
}
