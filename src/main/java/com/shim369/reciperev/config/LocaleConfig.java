package com.shim369.reciperev.config;

import java.time.Duration;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * 多言語対応(英語が既定、日本語に切り替え可能)。
 * <ul>
 *   <li>画面の文言は messages.properties(英語)/ messages_ja.properties(日本語)に置き、テンプレートでは #{キー} で参照する</li>
 *   <li>URL に ?lang=ja / ?lang=en を付けると言語が切り替わり(LocaleChangeInterceptor)、選んだ言語は Cookie に保存される</li>
 *   <li>ブラウザの Accept-Language は見ない(英語を既定にするため)。Cookie が無ければ英語</li>
 * </ul>
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    public static final String LANG_PARAM = "lang";

    /** Bean 名は "localeResolver" でなければならない(DispatcherServlet がこの名前で探す) */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("reciperev-lang");
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName(LANG_PARAM);
        interceptor.setIgnoreInvalidLocale(true); // ?lang=@@ のような不正な値は無視する(例外にしない)
        registry.addInterceptor(interceptor);
    }
}
