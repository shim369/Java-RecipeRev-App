package com.shim369.reciperev.exception;

/** Python の傾向分析が実行できなかった(Python が無い・タイムアウト・異常終了など) */
public class AnalysisException extends LocalizedException {

    public AnalysisException(String code, Object... args) {
        super(code, args, null);
    }

    public AnalysisException(Throwable cause, String code, Object... args) {
        super(code, args, cause);
    }
}
