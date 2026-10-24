package com.shim369.reciperev.service;

import com.shim369.reciperev.domain.Recipe;
import com.shim369.reciperev.domain.RecipeDiff;
import com.shim369.reciperev.domain.RecipeVersion;
import com.shim369.reciperev.domain.VersionContent;
import com.shim369.reciperev.domain.VersionStatus;
import com.shim369.reciperev.dto.DiffView;
import com.shim369.reciperev.dto.FinalizeResult;
import com.shim369.reciperev.dto.VersionContentForm;
import com.shim369.reciperev.dto.VersionDetail;
import com.shim369.reciperev.dto.VersionSummary;
import com.shim369.reciperev.exception.BusinessRuleException;
import com.shim369.reciperev.exception.NoChangesException;
import com.shim369.reciperev.exception.NotFoundException;
import com.shim369.reciperev.repository.RecipeQueryRepository;
import com.shim369.reciperev.repository.RecipeRepository;
import com.shim369.reciperev.repository.RecipeVersionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 版のユースケース(新しい版の作成・参照・比較・承認フロー) */
@Service
public class VersionService {

    private static final Logger log = LoggerFactory.getLogger(VersionService.class);

    private final RecipeRepository recipeRepository;
    private final RecipeVersionRepository versionRepository;
    private final RecipeQueryRepository queryRepository;
    private final RecipeDiffService diffService;

    public VersionService(RecipeRepository recipeRepository, RecipeVersionRepository versionRepository,
                          RecipeQueryRepository queryRepository, RecipeDiffService diffService) {
        this.recipeRepository = recipeRepository;
        this.versionRepository = versionRepository;
        this.queryRepository = queryRepository;
        this.diffService = diffService;
    }

    /** 削除されていないレシピの版を取得する。材料も読み込み済みの状態で返す */
    @Transactional(readOnly = true)
    public RecipeVersion getVersion(long recipeId, int versionNo) {
        recipeRepository.findByIdAndDeletedAtIsNull(recipeId)
                .orElseThrow(() -> new NotFoundException("レシピが見つかりません(id=" + recipeId + ")"));
        return versionRepository.findByRecipeIdAndVersionNo(recipeId, versionNo)
                .orElseThrow(() -> new NotFoundException("第" + versionNo + "版が見つかりません"));
    }

    @Transactional(readOnly = true)
    public VersionDetail getDetail(long recipeId, int versionNo) {
        RecipeVersion version = getVersion(recipeId, versionNo);
        Recipe recipe = recipeRepository.getReferenceById(recipeId);
        recipe.getName(); // トランザクション内で読み込んでおく(画面で遅延ロードさせない)
        Integer finalNo = versionRepository.findByRecipeIdAndStatus(recipeId, VersionStatus.FINAL)
                .map(RecipeVersion::getVersionNo).orElse(null);
        return new VersionDetail(recipe, version, finalNo, queryRepository.findAverageRating(version.getId()));
    }

    @Transactional(readOnly = true)
    public int latestVersionNo(long recipeId) {
        return versionRepository.findMaxVersionNo(recipeId);
    }

    /**
     * F-05: 派生元の版を元に、新しい版を作る。
     * <ol>
     *   <li>レシピの行を FOR UPDATE でロックする(同時保存でも版番号が重複しない)</li>
     *   <li>派生元と内容が同じなら作らない(BR-03)</li>
     *   <li>版番号 = 最大の版番号 + 1(BR-01)、状態は下書き(BR-04)</li>
     * </ol>
     */
    @Transactional
    public RecipeVersion createVersion(long recipeId, int baseVersionNo, VersionContentForm form) {
        Recipe recipe = recipeRepository.findActiveForUpdate(recipeId)
                .orElseThrow(() -> new NotFoundException("レシピが見つかりません(id=" + recipeId + ")"));
        RecipeVersion base = versionRepository.findByRecipeIdAndVersionNo(recipeId, baseVersionNo)
                .orElseThrow(() -> new NotFoundException("第" + baseVersionNo + "版が見つかりません"));

        VersionContent content = form.toContent();
        if (!diffService.diff(base.toContent(), content).hasChanges()) {
            throw new NoChangesException();
        }

        int nextNo = versionRepository.findMaxVersionNo(recipeId) + 1;
        RecipeVersion created = versionRepository.save(
                RecipeVersion.create(recipe, nextNo, base, content, form.getChangeNote()));
        recipe.touch();
        log.info("Version created: recipeId={}, versionNo={}, basedOn={}", recipeId, nextNo, baseVersionNo);
        return created;
    }

    /** 下書き → 試作中 */
    @Transactional
    public void startTrial(long recipeId, int versionNo) {
        RecipeVersion version = getVersion(recipeId, versionNo);
        version.startTrial(); // ルール違反ならここで BusinessRuleException → ロールバック
        log.info("Version status changed: recipeId={}, versionNo={}, status=TRIAL", recipeId, versionNo);
    }

    /**
     * F-08「この版を決定版にする」。BR-05 / BR-06 を1トランザクションで守る。
     *
     * <p>更新の順番が重要: 先に今の決定版を試作中に戻し、その後で対象を決定版にする。
     * 逆にすると、一瞬「決定版が2つ」になり、DBの一意制約(uk_recipe_versions_final)に違反する。
     */
    @Transactional
    public FinalizeResult finalizeVersion(long recipeId, int versionNo) {
        // 同じレシピへの同時操作を直列化する(2つの版が同時に決定版になろうとするのを防ぐ)
        recipeRepository.findActiveForUpdate(recipeId)
                .orElseThrow(() -> new NotFoundException("レシピが見つかりません(id=" + recipeId + ")"));
        RecipeVersion target = versionRepository.findByRecipeIdAndVersionNo(recipeId, versionNo)
                .orElseThrow(() -> new NotFoundException("第" + versionNo + "版が見つかりません"));
        if (target.isFinal()) {
            throw new BusinessRuleException("第" + versionNo + "版はすでに決定版です。");
        }
        Integer previousNo = versionRepository.findByRecipeIdAndStatus(recipeId, VersionStatus.FINAL)
                .map(RecipeVersion::getVersionNo).orElse(null);

        // ① 一括UPDATEで前の決定版を戻す(clearAutomatically で永続化コンテキストが空になる)
        versionRepository.demoteFinalToTrial(recipeId, LocalDateTime.now());
        // ② 空になったので対象を読み直してから変更する(古いエンティティを変更しても保存されないため)
        RecipeVersion fresh = versionRepository.findByRecipeIdAndVersionNo(recipeId, versionNo).orElseThrow();
        fresh.markFinal();

        log.info("Version finalized: recipeId={}, versionNo={}, previousFinal={}", recipeId, versionNo, previousNo);
        return new FinalizeResult(versionNo, previousNo);
    }

    /** F-07: 2つの版の差分 */
    @Transactional(readOnly = true)
    public DiffView compare(long recipeId, int fromNo, int toNo) {
        if (fromNo == toNo) {
            throw new BusinessRuleException("同じ版どうしは比較できません。別の版を選んでください。");
        }
        RecipeVersion from = getVersion(recipeId, fromNo);
        RecipeVersion to = getVersion(recipeId, toNo);
        RecipeDiff diff = diffService.diff(from.toContent(), to.toContent());

        List<VersionSummary> summaries = queryRepository.findVersionSummaries(recipeId);
        Recipe recipe = recipeRepository.getReferenceById(recipeId);
        recipe.getName();
        return new DiffView(recipe, summaryOf(summaries, fromNo), summaryOf(summaries, toNo), diff, summaries);
    }

    private static VersionSummary summaryOf(List<VersionSummary> summaries, int versionNo) {
        return summaries.stream().filter(s -> s.versionNo() == versionNo).findFirst().orElseThrow();
    }
}
