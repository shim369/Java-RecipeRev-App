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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * デモ用のサンプルデータ。{@code --spring.profiles.active=demo} のときだけ有効になる(@Profile)。
 * レシピが1件もないときだけ投入するので、何度起動してもデータが増え続けない。
 * 投入は画面と同じサービス経由で行い、業務ルール(版番号・状態遷移)を必ず通す。
 *
 * <p>データの言語は reciperev.demo.locale(en / ja、既定は en)で選ぶ。
 * レシピ名や材料名は「画面の文言」ではなく「利用者が入力したデータ」なので、メッセージファイルではなくここで持つ。
 */
@Component
@Profile("demo")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final RecipeRepository recipeRepository;
    private final RecipeService recipeService;
    private final VersionService versionService;
    private final CookLogService cookLogService;
    private final boolean japanese;

    public DemoDataLoader(RecipeRepository recipeRepository, RecipeService recipeService,
                          VersionService versionService, CookLogService cookLogService,
                          @Value("${reciperev.demo.locale:en}") String locale) {
        this.recipeRepository = recipeRepository;
        this.recipeService = recipeService;
        this.versionService = versionService;
        this.cookLogService = cookLogService;
        this.japanese = "ja".equalsIgnoreCase(locale);
    }

    /** 英語/日本語のどちらかを選ぶ */
    private String t(String en, String ja) {
        return japanese ? ja : en;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (recipeRepository.count() > 0) {
            log.info("Demo data skipped (recipes already exist)");
            return;
        }
        loadGateauChocolat();
        loadCurry();
        log.info("Demo data loaded (locale={})", japanese ? "ja" : "en");
    }

    private void loadGateauChocolat() {
        String melt = t("Melt the chocolate and butter over a water bath", "チョコレートとバターを湯煎で溶かす");
        String steps = String.join("\n", melt,
                t("Whisk the egg yolks with the sugar", "卵黄と砂糖をすり混ぜる"),
                t("Whip the egg whites into a meringue", "メレンゲを作る"),
                t("Fold everything together and pour into the pan", "全部を合わせて型に流す"),
                t("Bake at 170°C for 30 minutes", "170度で30分焼く"));
        String lowAndSlow = steps.replace(t("170°C for 30 minutes", "170度で30分"), t("160°C for 40 minutes", "160度で40分"));

        String choc = t("Chocolate", "チョコレート");
        String sugar = t("Sugar", "砂糖");
        String butter = t("Butter", "バター");
        String egg = t("Egg", "卵");
        String eggUnit = t("pcs", "個");
        String flour = t("Cake flour", "薄力粉");
        String cream = t("Heavy cream", "生クリーム");

        RecipeCreateForm form = new RecipeCreateForm();
        form.setName(t("Gâteau au chocolat", "ガトーショコラ"));
        form.setDescription(t("18 cm round pan. Tuning the formula for a moist, rich cake.", "18cm丸型。しっとり濃厚を目指して配合を調整中。"));
        form.setSteps(steps);
        form.setChangeNote(t("Base formula", "基本の配合"));
        form.setIngredients(ingredients(choc, "100", "g", sugar, "60", "g", butter, "60", "g", egg, "3", eggUnit, flour, "20", "g"));
        long id = recipeService.create(form).getId();

        version(id, 1, lowAndSlow, t("Bake lower and longer", "低温で長めに焼く"),
                choc, "100", "g", sugar, "70", "g", butter, "60", "g", egg, "3", eggUnit, flour, "20", "g");
        version(id, 2, lowAndSlow, t("More sugar, add heavy cream", "砂糖を増やし、生クリームを追加"),
                choc, "100", "g", sugar, "80", "g", butter, "50", "g", egg, "3", eggUnit, flour, "15", "g", cream, "50", "ml");
        version(id, 3, lowAndSlow, t("Even more sugar", "砂糖をさらに増量"),
                choc, "120", "g", sugar, "90", "g", butter, "50", "g", egg, "3", eggUnit, flour, "15", "g", cream, "50", "ml");
        version(id, 2, steps, t("Back to the original temperature, from v2", "第2版から温度を戻して比較"),
                choc, "100", "g", sugar, "50", "g", butter, "70", "g", egg, "3", eggUnit, flour, "20", "g");

        log(id, 1, 30, 3, t("Dry. Not sweet enough.", "ぱさつく。甘さ控えめすぎ"));
        log(id, 1, 25, 2, null);
        log(id, 2, 20, 3, t("A little more moist", "しっとり感が少し出た"));
        log(id, 3, 14, 4, t("Rich. The cream makes a difference.", "濃厚。生クリームが効いている"));
        log(id, 3, 10, 5, t("The family loved it", "家族に好評"));
        log(id, 4, 6, 5, t("The best one so far", "一番おいしい"));
        log(id, 4, 3, 4, t("Maybe slightly too sweet", "少し甘すぎるかも"));
        log(id, 5, 1, 2, t("Dry again", "ぱさつきが戻った"));
        versionService.finalizeVersion(id, 4);
    }

    private void loadCurry() {
        String chicken = t("Chicken thigh", "鶏もも肉");
        String onion = t("Onion", "玉ねぎ");
        String onionUnit = t("pcs", "個");
        String roux = t("Curry roux", "カレールウ");
        String salt = t("Salt", "塩");
        String tomato = t("Canned tomatoes", "トマト缶");
        String saute = t("Saute the onions until golden brown", "玉ねぎを飴色になるまで炒める");
        String sear = t("Sear the chicken", "鶏肉を焼く");
        String rouxStep = t("Dissolve the roux", "ルウを溶かす");

        RecipeCreateForm form = new RecipeCreateForm();
        form.setName(t("Classic chicken curry", "定番チキンカレー"));
        form.setDescription(t("Serves 4", "4人分"));
        form.setSteps(String.join("\n", saute, sear, t("Add water and simmer for 20 minutes", "水を加えて20分煮込む"), rouxStep));
        form.setIngredients(ingredients(chicken, "400", "g", onion, "2", onionUnit, roux, "100", "g", salt, null, null));
        long id = recipeService.create(form).getId();
        version(id, 1, String.join("\n", saute, sear, t("Add the canned tomatoes", "トマト缶を加える"),
                        t("Add water and simmer for 30 minutes", "水を加えて30分煮込む"), rouxStep),
                t("Add canned tomatoes", "トマト缶を追加"),
                chicken, "400", "g", onion, "2", onionUnit, roux, "100", "g", tomato, "200", "g", salt, null, null);
        log(id, 1, 7, 3, t("Good, nothing special", "普通においしい"));
        log(id, 2, 2, 4, t("The acidity works well", "酸味が加わって良い"));
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

    /** "名前", "分量", "単位" の3つ組を並べて材料リストを作る。分量が null の材料はメモに「少々」を入れる */
    private List<IngredientForm> ingredients(String... triples) {
        List<IngredientForm> list = new ArrayList<>();
        for (int i = 0; i < triples.length; i += 3) {
            boolean pinch = triples[i + 1] == null;
            list.add(new IngredientForm(triples[i], pinch ? null : new BigDecimal(triples[i + 1]),
                    triples[i + 2], pinch ? t("a pinch", "少々") : null));
        }
        return list;
    }
}
