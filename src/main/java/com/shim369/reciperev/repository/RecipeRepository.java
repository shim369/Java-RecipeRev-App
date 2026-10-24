package com.shim369.reciperev.repository;

import com.shim369.reciperev.domain.Recipe;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA: インターフェースを書くだけで、実装クラスは起動時に自動生成される。
 * メソッド名(findByIdAndDeletedAtIsNull)からSQLが組み立てられる。
 */
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    /** 論理削除されていないレシピだけを取得する */
    Optional<Recipe> findByIdAndDeletedAtIsNull(Long id);

    /**
     * SELECT ... FOR UPDATE でレシピの行をロックする。
     * 同じレシピに同時に版を作っても、版番号の採番が重ならないようにするため(04_er_diagram.md 4.4)。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Recipe r where r.id = :id and r.deletedAt is null")
    Optional<Recipe> findActiveForUpdate(@Param("id") Long id);
}
