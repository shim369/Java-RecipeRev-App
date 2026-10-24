package com.shim369.reciperev.exception;

/** Python の傾向分析が実行できなかった(Python が無い・タイムアウト・異常終了など) */
public class AnalysisException extends RuntimeException {

    public AnalysisException(String message) {
        super(message);
    }

    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
