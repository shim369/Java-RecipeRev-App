package com.shim369.reciperev.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

/**
 * ヘッダーの言語切り替えボタンのリンク先を作る。テンプレートから ${@languageSwitcher.url('ja')} で呼ぶ。
 * 今見ているページの URL(クエリ含む)に lang だけを付け替えるので、切り替えても同じ画面にとどまる。
 * エラー画面でも使えるよう、Model ではなく「今のリクエスト」から URL を組み立てる。
 */
@Component
public class LanguageSwitcher {

    public String url(String lang) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return "/recipes?" + LocaleConfig.LANG_PARAM + "=" + lang;
        }
        HttpServletRequest request = attrs.getRequest();
        // POST の結果(入力エラーなど)を表示中に同じ URL を GET すると 405 になるため、一覧へ戻す
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return request.getContextPath() + "/recipes?" + LocaleConfig.LANG_PARAM + "=" + lang;
        }
        UriComponents uri = ServletUriComponentsBuilder.fromRequest(request)
                .replaceQueryParam(LocaleConfig.LANG_PARAM, lang)
                .build(true); // 既存のクエリはエンコード済みなので、二重にエンコードしない
        return uri.getPath() + "?" + uri.getQuery();
    }
}
