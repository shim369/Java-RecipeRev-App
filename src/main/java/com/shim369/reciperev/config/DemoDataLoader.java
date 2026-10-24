package com.shim369.reciperev.config;

import com.shim369.reciperev.dto.CookLogForm;
import com.shim369.reciperev.dto.IngredientForm;
import com.shim369.reciperev.dto.RecipeCreateForm;
import com.shim369.reciperev.dto.VersionContentForm;
import com.shim369.reciperev.repository.RecipeRepository;
import com.shim369.reciperev.service.CookLogService;
import com.shim369.reciperev.service.RecipeService;
import com.shim369.reciperev.service.VersionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * デモ用のサンプルデータ。{@code --spring.profiles.active=demo} のときだけ有効になる(@Profile)。
 * レシピが1件もないときだけ投入するので、何度起動してもデータが増え続けない。
 * 投入は画面と同じサービス経由で行い、業務ルール(版番号・状態遷移)を必ず通す。
 */
@Component
@Profile("demo")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final RecipeRepository recipeRepository;
    private final RecipeService recipeService;
    private final VersionService versionService;
    private final CookLogService cookLogService;

    public DemoDataLoader(RecipeRepository recipeRepository, RecipeService recipeService,
                          VersionService versionService, CookLogService cookLogService) {
        this.recipeRepository = recipeRepository;
        this.recipeService = recipeService;
        this.versionService = versionService;
        this.cookLogService = cookLogService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (recipeRepository.count() > 0) {
            log.info("Demo data skipped (recipes already exist)");
            return;
        }
        loadGateauChocolat();
        loadCurry();
        log.info("Demo data loaded");
    }

    private void loadGateauChocolat() {
        String steps = "チョコレートとバターを湯煎で溶かす\n卵黄と砂糖をすり混ぜる\nメレンゲを作る\n全部を合わせて型に流す\n170度で30分焼く";
        RecipeCreateForm form = new RecipeCreateForm();
        form.setName("ガトーショコラ");
        form.setDescription("18cm丸型。しっとり濃厚を目指して配合を調整中。");
        form.setSteps(steps);
        form.setChangeNote("基本の配合");
        form.setIngredients(ingredients("チョコレート", "100", "g", "砂糖", "60", "g", "バター", "60", "g", "卵", "3", "個", "薄力粉", "20", "g"));
        long id = recipeService.create(form).getId();

        version(id, 1, steps.replace("170度で30分", "160度で40分"), "低温で長めに焼く",
                "チョコレート", "100", "g", "砂糖", "70", "g", "バター", "60", "g", "卵", "3", "個", "薄力粉", "20", "g");
        version(id, 2, steps.replace("170度で30分", "160度で40分"), "砂糖を増やし、生クリームを追加",
                "チョコレート", "100", "g", "砂糖", "80", "g", "バター", "50", "g", "卵", "3", "個", "薄力粉", "15", "g", "生クリーム", "50", "ml");
        version(id, 3, steps.replace("170度で30分", "160度で40分"), "砂糖をさらに増量",
                "チョコレート", "120", "g", "砂糖", "90", "g", "バター", "50", "g", "卵", "3", "個", "薄力粉", "15", "g", "生クリーム", "50", "ml");
        version(id, 2, steps, "第2版から温度を戻して比較",
                "チョコレート", "100", "g", "砂糖", "50", "g", "バター", "70", "g", "卵", "3", "個", "薄力粉", "20", "g");

        log(id, 1, 30, 3, "ぱさつく。甘さ控えめすぎ");
        log(id, 1, 25, 2, null);
        log(id, 2, 20, 3, "しっとり感が少し出た");
        log(id, 3, 14, 4, "濃厚。生クリームが効いている");
        log(id, 3, 10, 5, "家族に好評");
        log(id, 4, 6, 5, "一番おいしい");
        log(id, 4, 3, 4, "少し甘すぎるかも");
        log(id, 5, 1, 2, "ぱさつきが戻った");
        versionService.finalizeVersion(id, 4);
    }

    private void loadCurry() {
        RecipeCreateForm form = new RecipeCreateForm();
        form.setName("定番チキンカレー");
        form.setDescription("4人分");
        form.setSteps("玉ねぎを飴色になるまで炒める\n鶏肉を焼く\n水を加えて20分煮込む\nルウを溶かす");
        form.setIngredients(ingredients("鶏もも肉", "400", "g", "玉ねぎ", "2", "個", "カレールウ", "100", "g", "塩", null, null));
        long id = recipeService.create(form).getId();
        version(id, 1, "玉ねぎを飴色になるまで炒める\n鶏肉を焼く\nトマト缶を加える\n水を加えて30分煮込む\nルウを溶かす", "トマト缶を追加",
                "鶏もも肉", "400", "g", "玉ねぎ", "2", "個", "カレールウ", "100", "g", "トマト缶", "200", "g", "塩", null, null);
        log(id, 1, 7, 3, "普通においしい");
        log(id, 2, 2, 4, "酸味が加わって良い");
    }

    private void version(long recipeId, int baseNo, String steps, String note, String... triples) {
        VersionContentForm form = new VersionContentForm();
        form.setSteps(steps);
        form.setChangeNote(note);
        form.setIngredients(ingredients(triples));
        versionService.createVersion(recipeId, baseNo, form);
    }

    private void log(long recipeId, int versionNo, int daysAgo, int rating, String memo) {
        CookLogForm form = new CookLogForm();
        form.setCookedOn(LocalDate.now().minusDays(daysAgo));
        form.setRating(rating);
        form.setMemo(memo);
        cookLogService.register(recipeId, versionNo, form);
    }

    /** "名前", "分量", "単位" の3つ組を並べて材料リストを作る */
    private static List<IngredientForm> ingredients(String... triples) {
        List<IngredientForm> list = new ArrayList<>();
        for (int i = 0; i < triples.length; i += 3) {
            list.add(new IngredientForm(triples[i], triples[i + 1] == null ? null : new BigDecimal(triples[i + 1]),
                    triples[i + 2], triples[i + 1] == null ? "少々" : null));
        }
        return list;
    }
}
