package com.shim369.reciperev.service;

import com.shim369.reciperev.domain.CookLog;
import com.shim369.reciperev.domain.RecipeVersion;
import com.shim369.reciperev.dto.CookLogForm;
import com.shim369.reciperev.exception.NotFoundException;
import com.shim369.reciperev.repository.CookLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 調理ログのユースケース(F-09) */
@Service
public class CookLogService {

    private static final Logger log = LoggerFactory.getLogger(CookLogService.class);

    private final CookLogRepository cookLogRepository;
    private final VersionService versionService;
    private final PhotoStorageService photoStorage;

    public CookLogService(CookLogRepository cookLogRepository, VersionService versionService,
                          PhotoStorageService photoStorage) {
        this.cookLogRepository = cookLogRepository;
        this.versionService = versionService;
        this.photoStorage = photoStorage;
    }

    /**
     * 調理ログを登録する。下書きの版なら同じトランザクションで試作中にする(BR-08)。
     *
     * <p>写真ファイルは DB のトランザクションでは巻き戻せない。そこで「ロールバックされたら写真を消す」処理を
     * トランザクションの完了時に呼ばれるよう登録しておく(TransactionSynchronization)。
     *
     * @return 下書きから試作中に変わったら true
     */
    @Transactional
    public boolean register(long recipeId, int versionNo, CookLogForm form) {
        RecipeVersion version = versionService.getVersion(recipeId, versionNo);
        String photoPath = photoStorage.store(form.getPhoto()).orElse(null);
        if (photoPath != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        photoStorage.delete(photoPath);
                    }
                }
            });
        }

        cookLogRepository.save(new CookLog(version, form.getCookedOn(), form.getRating(), form.getMemo(), photoPath));

        boolean startedTrial = false;
        if (version.isDraft()) {
            version.startTrial();
            startedTrial = true;
        }
        log.info("Cook log registered: recipeId={}, versionNo={}, rating={}, autoTrial={}",
                recipeId, versionNo, form.getRating(), startedTrial);
        return startedTrial;
    }

    /**
     * 調理ログを削除し、コミットに成功したら写真も消す(先に消すと、DB がロールバックしたとき写真だけ失われるため)。
     *
     * @return 削除したログの版番号(リダイレクト先に使う)
     */
    @Transactional
    public int delete(long recipeId, long logId) {
        CookLog cookLog = cookLogRepository.findByIdAndVersionRecipeId(logId, recipeId)
                .orElseThrow(() -> new NotFoundException("調理ログが見つかりません(id=" + logId + ")"));
        int versionNo = cookLog.getVersion().getVersionNo();
        String photoPath = cookLog.getPhotoPath();
        cookLogRepository.delete(cookLog);
        if (photoPath != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    photoStorage.delete(photoPath);
                }
            });
        }
        log.info("Cook log deleted: recipeId={}, logId={}", recipeId, logId);
        return versionNo;
    }
}
