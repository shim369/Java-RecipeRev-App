package com.shim369.reciperev.dto;

import com.shim369.reciperev.domain.VersionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 版履歴の1行(平均評価は調理ログがなければ null) */
public record VersionSummary(
        int versionNo,
        VersionStatus status,
        String changeNote,
        LocalDateTime createdAt,
        BigDecimal averageRating,
        int logCount) {
}
