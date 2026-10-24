package com.shim369.reciperev.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shim369.reciperev.config.RecipeRevProperties;
import com.shim369.reciperev.exception.InvalidPhotoException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/** 写真の保存(UT-PHOTO-xx)。@TempDir はテストごとに一時フォルダを作り、終わったら消してくれる */
class PhotoStorageServiceTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @TempDir
    Path tempDir;

    PhotoStorageService service;

    @BeforeEach
    void setUp() {
        service = new PhotoStorageService(new RecipeRevProperties(tempDir, null));
    }

    @Test
    @DisplayName("UT-PHOTO-01: PNG/JPEG/WebP を中身で判定し、UUID名+正しい拡張子で保存する")
    void storesByMagicBytes() {
        // ファイル名や Content-Type が嘘でも、中身で判定する
        String png = service.store(new MockMultipartFile("photo", "a.jpg", "image/jpeg", PNG)).orElseThrow();
        String jpg = service.store(new MockMultipartFile("photo", "b.png", "text/plain", JPEG)).orElseThrow();
        String webp = service.store(new MockMultipartFile("photo", "c", null, WEBP)).orElseThrow();

        assertThat(png).matches("[0-9a-f-]{36}\\.png");
        assertThat(jpg).endsWith(".jpg");
        assertThat(webp).endsWith(".webp");
        assertThat(tempDir.resolve(png)).exists();
        assertThat(service.mediaTypeOf(webp)).isEqualTo("image/webp");
    }

    @Test
    @DisplayName("UT-PHOTO-02: 画像でないファイルは拒否する")
    void rejectsNonImage() {
        MockMultipartFile fake = new MockMultipartFile("photo", "x.png", "image/png", "<?php echo 1;".getBytes());
        assertThatThrownBy(() -> service.store(fake)).isInstanceOf(InvalidPhotoException.class);
    }

    @Test
    @DisplayName("UT-PHOTO-03: ファイルが選ばれていなければ何もしない")
    void emptyFile() {
        assertThat(service.store(new MockMultipartFile("photo", new byte[0]))).isEmpty();
        assertThat(service.store(null)).isEmpty();
    }

    @Test
    @DisplayName("UT-PHOTO-04: 保存名の形式でないパスは解決しない(パストラバーサル対策)")
    void resolveRejectsTraversal() throws Exception {
        Files.writeString(tempDir.resolve("secret.txt"), "x");
        assertThat(service.resolve("../secret.txt")).isEmpty();
        assertThat(service.resolve("secret.txt")).isEmpty();
        assertThat(service.resolve(null)).isEmpty();
    }

    @Test
    @DisplayName("UT-PHOTO-05: 削除するとファイルが消える。存在しなくても例外にしない")
    void delete() {
        String name = service.store(new MockMultipartFile("photo", "a.png", null, PNG)).orElseThrow();
        service.delete(name);
        assertThat(tempDir.resolve(name)).doesNotExist();
        service.delete(name);
        service.delete(null);
    }
}
