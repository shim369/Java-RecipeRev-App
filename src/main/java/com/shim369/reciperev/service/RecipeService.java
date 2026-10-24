package com.shim369.reciperev.service;

import com.shim369.reciperev.domain.Recipe;
import com.shim369.reciperev.domain.RecipeVersion;
import com.shim369.reciperev.domain.VersionStatus;
import com.shim369.reciperev.dto.RecipeBasicForm;
import com.shim369.reciperev.dto.RecipeCreateForm;
import com.shim369.reciperev.dto.RecipeDetail;
import com.shim369.reciperev.dto.RecipeSummary;
import com.shim369.reciperev.exception.NotFoundException;
import com.shim369.reciperev.repository.RecipeQueryRepository;
import com.shim369.reciperev.repository.RecipeRepository;
import com.shim369.reciperev.repository.RecipeVersionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * レシピ本体のユースケース(登録・参照・基本情報の編集・削除)。
 * トランザクションの境界はこの層に置く。コントローラは「HTTPの入出力」、リポジトリは「SQL」だけを担当する。
 */
@Service
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    private final RecipeRepository recipeRepository;
    private final RecipeVersionRepository versionRepository;
    private final RecipeQueryRepository queryRepository;

    // コンストラクタインジェクション: 依存が final になり、テストで差し替えるのも簡単(@Autowired は省略できる)
    public RecipeService(RecipeRepository recipeRepository,
                         RecipeVersionRepository versionRepository,
                         RecipeQueryRepository queryRepository) {
        this.recipeRepository = recipeRepository;
        this.versionRepository = versionRepository;
        this.queryRepository = queryRepository;
    }

    @Transactional(readOnly = true)
    public List<RecipeSummary> listSummaries() {
        return queryRepository.findRecipeSummaries();
    }

    /** F-02: レシピと第1版(下書き)を1トランザクションで作る。途中で失敗すればどちらも残らない */
    @Transactional
    public Recipe create(RecipeCreateForm form) {
        Recipe recipe = recipeRepository.save(new Recipe(form.getName().strip(), blankToNull(form.getDescription())));
        RecipeVersion first = RecipeVersion.create(recipe, 1, null, form.toContent(), form.getChangeNote());
        versionRepository.save(first);
        log.info("Recipe created: id={}, name={}", recipe.getId(), recipe.getName());
        return recipe;
    }

    @Transactional(readOnly = true)
    public Recipe getActive(long recipeId) {
        return recipeRepository.findByIdAndDeletedAtIsNull(recipeId)
                .orElseThrow(() -> new NotFoundException("error.recipeNotFound", recipeId));
    }

    /**
     * SCR-03 に必要なデータを読む。
     * open-in-view を無効にしているので、画面で使う遅延ロード(材料など)はトランザクション内で読み終えておく。
     */
    @Transactional(readOnly = true)
    public RecipeDetail getDetail(long recipeId) {
        Recipe recipe = getActive(recipeId);
        int representativeNo = versionRepository.findByRecipeIdAndStatus(recipeId, VersionStatus.FINAL)
                .map(RecipeVersion::getVersionNo)
                .orElseGet(() -> versionRepository.findMaxVersionNo(recipeId));
        RecipeVersion representative = versionRepository.findByRecipeIdAndVersionNo(recipeId, representativeNo)
                .orElseThrow(() -> new IllegalStateException("Recipe has no versions: id=" + recipeId));
        return new RecipeDetail(recipe, representative, queryRepository.findVersionSummaries(recipeId));
    }

    /** F-03: 名前・説明の変更。dirty checking により、トランザクション終了時に UPDATE が自動で発行される */
    @Transactional
    public void updateBasicInfo(long recipeId, RecipeBasicForm form) {
        Recipe recipe = getActive(recipeId);
        recipe.changeBasicInfo(form.getName().strip(), blankToNull(form.getDescription()));
    }

    /** F-04: 論理削除(BR-09) */
    @Transactional
    public void delete(long recipeId) {
        Recipe recipe = getActive(recipeId);
        recipe.softDelete();
        log.info("Recipe soft-deleted: id={}", recipeId);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }
}
