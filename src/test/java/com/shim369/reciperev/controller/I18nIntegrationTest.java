package com.shim369.reciperev.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shim369.reciperev.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 多言語対応(英語が既定、日本語に切り替え)の結合テスト(IT-I18N-xx) */
@AutoConfigureMockMvc
class I18nIntegrationTest extends IntegrationTestBase {

    static final Cookie JA = new Cookie("reciperev-lang", "ja");

    @Autowired
    MockMvc mvc;

    long recipeId;

    private static MockHttpServletRequestBuilder recipeForm(MockHttpServletRequestBuilder req) {
        return req.param("name", "Curry")
                .param("ingredients[0].name", "Salt").param("ingredients[0].amount", "1").param("ingredients[0].unit", "g")
                .param("steps", "Simmer");
    }

    @BeforeEach
    void createRecipe() throws Exception {
        String location = mvc.perform(recipeForm(post("/recipes"))).andReturn().getResponse().getRedirectedUrl();
        recipeId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    @DisplayName("IT-I18N-01: 既定は英語。ブラウザが日本語を希望していても英語で表示する")
    void defaultIsEnglish() throws Exception {
        mvc.perform(get("/recipes").header("Accept-Language", "ja-JP,ja;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"en\"")))
                .andExpect(content().string(containsString(">Recipes</h1>")))
                .andExpect(content().string(containsString(">日本語</a>"))); // 切り替えボタンは日本語へのリンク
    }

    @Test
    @DisplayName("IT-I18N-02: ?lang=ja で日本語になり、Cookie に保存される")
    void switchToJapanese() throws Exception {
        mvc.perform(get("/recipes").param("lang", "ja"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("reciperev-lang", "ja"))
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString(">レシピ一覧</h1>")))
                .andExpect(content().string(containsString(">English</a>")));
    }

    @Test
    @DisplayName("IT-I18N-03: Cookie の言語で、状態名・版番号・JS用の文言も切り替わる")
    void cookieKeepsLanguage() throws Exception {
        mvc.perform(get("/recipes/{id}", recipeId).cookie(JA))
                .andExpect(content().string(containsString("第1版")))
                .andExpect(content().string(containsString(">下書き</span>")))
                .andExpect(content().string(containsString("data-label-version=\"第{0}版\"")));
        mvc.perform(get("/recipes/{id}", recipeId))
                .andExpect(content().string(containsString(">v1</a>")))
                .andExpect(content().string(containsString(">Draft</span>")))
                .andExpect(content().string(not(containsString("下書き"))));
    }

    @Test
    @DisplayName("IT-I18N-04: 切り替えリンクは今のページの URL とクエリを保ったまま lang だけを付け替える")
    void switchLinkKeepsQuery() throws Exception {
        mvc.perform(recipeForm(post("/recipes/{id}/versions", recipeId).param("base", "1")).param("steps", "Simmer longer"));
        // .param() ではクエリ文字列が設定されないため、ブラウザと同じく URL にクエリを直接書く
        mvc.perform(get("/recipes/{id}/diff?from=1&to=2&lang=en", recipeId))
                .andExpect(content().string(containsString(
                        "href=\"/recipes/" + recipeId + "/diff?from=1&amp;to=2&amp;lang=ja\"")));
    }

    @Test
    @DisplayName("IT-I18N-05: 入力エラー・業務ルール違反・フラッシュ・API のメッセージも利用者の言語になる")
    void serverMessages() throws Exception {
        mvc.perform(post("/recipes").cookie(JA).param("name", ""))
                .andExpect(content().string(containsString("名前を入力してください")));
        mvc.perform(post("/recipes/{id}/versions/1/finalize", recipeId).cookie(JA))
                .andExpect(flash().attribute("message", "第1版を決定版にしました"));
        mvc.perform(post("/recipes/{id}/versions/1/finalize", recipeId).cookie(JA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("第1版はすでに決定版です。")));
        mvc.perform(post("/recipes/{id}/versions/1/start-trial", recipeId))
                .andExpect(content().string(containsString("v1 is Final, so a trial cannot be started.")));
        mvc.perform(get("/api/recipes/999999/rating-trend").cookie(JA))
                .andExpect(jsonPath("$.detail").value("レシピが見つかりません(id=999999)"));
    }

    @Test
    @DisplayName("IT-I18N-06: 不正な lang の値は無視して英語のまま表示する")
    void invalidLang() throws Exception {
        mvc.perform(get("/recipes").param("lang", "@@"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Recipes</h1>")));
    }
}
