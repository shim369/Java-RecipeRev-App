package com.shim369.reciperev.exception;

/** アップロードされた写真が JPEG/PNG/WebP ではない */
public class InvalidPhotoException extends BusinessRuleException {

    public InvalidPhotoException() {
        super("error.invalidPhoto");
    }
}
