package com.shim369.reciperev.repository;

import com.shim369.reciperev.domain.CookLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CookLogRepository extends JpaRepository<CookLog, Long> {

    /** 版の調理ログを新しい順に(同じ日なら後から登録した順に) */
    List<CookLog> findByVersionIdOrderByCookedOnDescIdDesc(Long versionId);

    /** 「レシピ X の調理ログ Y」として探す。別レシピのログIDを URL に入れられても削除できないようにするため */
    Optional<CookLog> findByIdAndVersionRecipeId(Long id, Long recipeId);
}
