package com.shim369.reciperev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * RecipeRev のエントリポイント。
 * このクラスがあるパッケージ配下(controller / service / ...)が自動でコンポーネントスキャンされる。
 */
@SpringBootApplication
@ConfigurationPropertiesScan // reciperev.* の設定値を record にバインドするため
public class RecipeRevApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecipeRevApplication.class, args);
    }
}
