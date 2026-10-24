package com.shim369.reciperev.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.shim369.reciperev.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 画面(コントローラ〜テンプレート〜DB)を通した結合テスト(IT-WEB-xx)。
 * MockMvc はサーバを起動せずに HTTP リクエストを模擬し、ステータス・リダイレクト先・HTML を検証できる。
 */
@AutoConfigureMockMvc
class WebIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;

    long recipeId;

    private static MockHttpServletRequestBuilder recipeForm(MockHttpServletRequestBuilder req, String sugar) {
        return req.param("ingredients[0].name", "チョコ").param("ingredients[0].amount", "100").param("ingredients[0].unit", "g")
                .param("ingredients[1].name", "砂糖").param("ingredients[1].amount", sugar).param("ingredients[1].unit", "g")
                .param("steps", "溶かす\n焼く");
    }

    @BeforeEach
    void createRecipe() throws Exception {
        String location = mvc.perform(recipeForm(post("/recipes").param("name", "ガトーショコラ"), "30"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        recipeId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    @DisplayName("IT-WEB-01: / はレシピ一覧へリダイレクトし、一覧に登録したレシピが出る")
    void listRecipes() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/recipes"));
        mvc.perform(get("/recipes")).andExpect(status().isOk()).andExpect(content().string(containsString("ガトーショコラ")));
    }

    @Test
    @DisplayName("IT-WEB-02: 入力エラーは同じ画面に戻り、項目ごとのメッセージを表示する")
    void validationErrors() throws Exception {
        mvc.perform(post("/recipes").param("name", "").param("steps", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/new"))
                .andExpect(model().attributeHasFieldErrors("form", "name", "ingredients", "stepCountValid"))
                .andExpect(content().string(containsString("Please enter a name")));
    }

    @Test
    @DisplayName("IT-WEB-03: 新しい版の保存 → 版詳細へ PRG リダイレクト、変更なしはフォームに戻る")
    void createVersion() throws Exception {
        mvc.perform(recipeForm(post("/recipes/{id}/versions", recipeId).param("base", "1"), "40"))
                .andExpect(redirectedUrl("/recipes/" + recipeId + "/versions/2"))
                .andExpect(flash().attribute("message", "Created v2 (draft)"));
        mvc.perform(recipeForm(post("/recipes/{id}/versions", recipeId).param("base", "2"), "40"))
                .andExpect(status().isOk())
                .andExpect(view().name("versions/form"))
                .andExpect(content().string(containsString("Nothing changed from the base version")));
    }

    @Test
    @DisplayName("IT-WEB-04: 差分画面に 変更(黄)の行と変更前後の値が表示される")
    void diffPage() throws Exception {
        mvc.perform(recipeForm(post("/recipes/{id}/versions", recipeId).param("base", "1"), "40"));
        mvc.perform(get("/recipes/{id}/diff", recipeId).param("from", "1").param("to", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"diff-changed\"")))
                .andExpect(content().string(containsString("30 g")))
                .andExpect(content().string(containsString("40 g")));
        mvc.perform(get("/recipes/{id}/diff", recipeId))
                .andExpect(redirectedUrl("/recipes/" + recipeId + "/diff?from=1&to=2"));
    }

    @Test
    @DisplayName("IT-WEB-05: 決定版にする → フラッシュメッセージ、同じ操作の繰り返しは 400")
    void finalizeVersion() throws Exception {
        mvc.perform(post("/recipes/{id}/versions/1/finalize", recipeId))
                .andExpect(redirectedUrl("/recipes/" + recipeId + "/versions/1"))
                .andExpect(flash().attribute("message", "v1 is now the final version"));
        mvc.perform(post("/recipes/{id}/versions/1/finalize", recipeId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("v1 is already the final version.")));
    }

    @Test
    @DisplayName("IT-WEB-06: 存在しないレシピ・版は 404、数値でない ID は 400")
    void errors() throws Exception {
        mvc.perform(get("/recipes/999999")).andExpect(status().isNotFound()).andExpect(view().name("error"));
        mvc.perform(get("/recipes/{id}/versions/9", recipeId)).andExpect(status().isNotFound());
        mvc.perform(get("/recipes/abc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("IT-WEB-07: 評価推移 API は JSON を返す。存在しないレシピは ProblemDetail の 404")
    void ratingTrendApi() throws Exception {
        mvc.perform(post("/recipes/{id}/versions/1/logs", recipeId).param("cookedOn", "2026-01-01").param("rating", "4"))
                .andExpect(flash().attribute("message", "Cooking log added (v1 moved to Trial)"));
        mvc.perform(get("/api/recipes/{id}/rating-trend", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionNo").value(1))
                .andExpect(jsonPath("$[0].avgRating").value(4.0))
                .andExpect(jsonPath("$[0].status").value("TRIAL"));
        mvc.perform(get("/api/recipes/999999/rating-trend"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Recipe not found (id=999999)"));
    }

    @Test
    @DisplayName("IT-WEB-08: レシピ削除後は一覧から消え、詳細は 404")
    void deleteRecipe() throws Exception {
        mvc.perform(post("/recipes/{id}/delete", recipeId)).andExpect(redirectedUrl("/recipes"));
        mvc.perform(get("/recipes/{id}", recipeId)).andExpect(status().isNotFound());
    }
}
