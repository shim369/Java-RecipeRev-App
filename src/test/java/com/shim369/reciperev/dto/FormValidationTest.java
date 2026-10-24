package com.shim369.reciperev.dto;

import static com.shim369.reciperev.support.TestData.ing;
import static com.shim369.reciperev.support.TestData.recipe;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 入力チェック(Bean Validation)の単体テスト(UT-VAL-xx)。Spring を起動せず Validator を直接使う */
class FormValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<String> invalidPaths(Object form) {
        return validator.validate(form).stream()
                .map(ConstraintViolation::getPropertyPath).map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("UT-VAL-01: 正しい入力ならエラーなし")
    void valid() {
        assertThat(invalidPaths(recipe("カレー", "煮る", ing("塩", "1", "g")))).isEmpty();
    }

    @Test
    @DisplayName("UT-VAL-02: 名前は必須・100文字以内")
    void name() {
        assertThat(invalidPaths(recipe(" ", "煮る", ing("塩", "1", "g")))).contains("name");
        assertThat(invalidPaths(recipe("あ".repeat(101), "煮る", ing("塩", "1", "g")))).contains("name");
    }

    @Test
    @DisplayName("UT-VAL-03: 材料は1行以上、材料名は必須")
    void ingredientsRequired() {
        assertThat(invalidPaths(recipe("x", "煮る"))).contains("ingredients");
        assertThat(invalidPaths(recipe("x", "煮る", ing("", "1", "g")))).contains("ingredients[0].name");
    }

    @Test
    @DisplayName("UT-VAL-04: 同じ材料名は版の中で重複できない(BR-11)")
    void duplicateIngredientNames() {
        assertThat(invalidPaths(recipe("x", "煮る", ing("塩", "1", "g"), ing(" 塩", "2", "g"))))
                .contains("ingredientNamesUnique");
    }

    @Test
    @DisplayName("UT-VAL-05: 分量は 0〜99999.99、小数2桁まで")
    void amountRange() {
        assertThat(invalidPaths(recipe("x", "煮る", ing("塩", "-1", "g")))).contains("ingredients[0].amount");
        assertThat(invalidPaths(recipe("x", "煮る", ing("塩", "100000", "g")))).contains("ingredients[0].amount");
        assertThat(invalidPaths(recipe("x", "煮る", ing("塩", "1.234", "g")))).contains("ingredients[0].amount");
        assertThat(invalidPaths(recipe("x", "煮る", ing("塩", null, null)))).isEmpty(); // 空欄は可(少々など)
    }

    @Test
    @DisplayName("UT-VAL-06: 手順は1〜50行(空行は数えない)、1行500文字以内")
    void steps() {
        assertThat(invalidPaths(recipe("x", "\n \n", ing("塩", "1", "g")))).contains("stepCountValid");
        String fiftyOne = IntStream.rangeClosed(1, 51).mapToObj(i -> "手順" + i).collect(Collectors.joining("\n"));
        assertThat(invalidPaths(recipe("x", fiftyOne, ing("塩", "1", "g")))).contains("stepCountValid");
        assertThat(invalidPaths(recipe("x", "あ".repeat(501), ing("塩", "1", "g")))).contains("stepLengthValid");
    }

    @Test
    @DisplayName("UT-VAL-07: 材料は50行まで")
    void maxIngredients() {
        RecipeCreateForm form = recipe("x", "煮る");
        form.setIngredients(IntStream.rangeClosed(1, 51).mapToObj(i -> ing("材料" + i, "1", "g")).toList());
        assertThat(invalidPaths(form)).contains("ingredients");
    }

    @Test
    @DisplayName("UT-VAL-08: 調理ログ — 調理日は必須で未来日不可、評価は1〜5")
    void cookLog() {
        CookLogForm form = new CookLogForm();
        form.setRating(3);
        assertThat(invalidPaths(form)).isEmpty();

        form.setCookedOn(LocalDate.now().plusDays(1));
        form.setRating(6);
        assertThat(invalidPaths(form)).containsAll(List.of("cookedOn", "rating"));

        form.setCookedOn(null);
        form.setRating(null);
        assertThat(invalidPaths(form)).containsAll(List.of("cookedOn", "rating"));
    }
}
