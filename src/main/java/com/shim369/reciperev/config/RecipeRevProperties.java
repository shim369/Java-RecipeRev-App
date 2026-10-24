package com.shim369.reciperev.config;

import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.properties の reciperev.* を型付きで受け取る。
 * 文字列のキーを @Value であちこちに書くより、設定の一覧が1か所で分かり、型の間違いも起動時に気づける。
 *
 * @param uploadDir 写真の保存先(reciperev.upload-dir)
 * @param analysis  Python 傾向分析の設定(reciperev.analysis.*)
 */
@ConfigurationProperties(prefix = "reciperev")
public record RecipeRevProperties(Path uploadDir, Analysis analysis) {

    /**
     * @param command        Python を実行するコマンド(既定は uv run ... python)。この後ろにスクリプトと引数を付けて実行する
     * @param script         分析スクリプトのパス
     * @param workDir        入力CSV・出力JSONを置くディレクトリ
     * @param timeoutSeconds Python の実行を打ち切るまでの秒数
     */
    public record Analysis(List<String> command, Path script, Path workDir, int timeoutSeconds) {
    }
}
