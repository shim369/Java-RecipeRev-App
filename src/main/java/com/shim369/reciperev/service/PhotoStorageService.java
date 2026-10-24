package com.shim369.reciperev.service;

import com.shim369.reciperev.config.RecipeRevProperties;
import com.shim369.reciperev.exception.InvalidPhotoException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 写真ファイルをローカル(uploads/)に保存・削除・読み出しする。
 *
 * <p>セキュリティ上の工夫:
 * <ul>
 *   <li>ファイルの種類は、ブラウザが送る Content-Type や拡張子ではなく「中身の先頭バイト(マジックナンバー)」で判定する</li>
 *   <li>保存名は UUID にする(元のファイル名に含まれる ../ などでフォルダの外に書き込まれるのを防ぐ)</li>
 *   <li>読み出し時もファイル名の形式を検査し、uploads/ の外を読めないようにする</li>
 * </ul>
 */
@Service
public class PhotoStorageService {

    private static final Logger log = LoggerFactory.getLogger(PhotoStorageService.class);

    /** 保存名の形式: UUID + 拡張子 */
    private static final Pattern STORED_NAME = Pattern.compile("[0-9a-f\\-]{36}\\.(jpg|png|webp)");

    private final Path uploadDir;

    public PhotoStorageService(RecipeRevProperties properties) {
        this.uploadDir = properties.uploadDir().toAbsolutePath().normalize();
    }

    /**
     * 写真を保存して、保存名(photo_path に入れる値)を返す。写真が選ばれていなければ空を返す。
     */
    public Optional<String> store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            ImageType type = ImageType.detect(head)
                    .orElseThrow(() -> new InvalidPhotoException("写真は JPEG / PNG / WebP のファイルを選んでください"));
            String storedName = UUID.randomUUID() + "." + type.extension;
            Files.createDirectories(uploadDir);
            // 先頭12バイトは読んでしまったので、改めてストリームを開いて全体を書き込む
            try (InputStream all = file.getInputStream()) {
                Files.copy(all, uploadDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Photo stored: {}", storedName);
            return Optional.of(storedName);
        } catch (IOException e) {
            throw new UncheckedIOException("写真の保存に失敗しました", e);
        }
    }

    /** 写真を削除する。消せなくても例外にはしない(ゴミファイルが残るだけで、調理ログの操作は成功させたいため) */
    public void delete(String storedName) {
        resolve(storedName).ifPresent(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Photo delete failed: {}", storedName, e);
            }
        });
    }

    /** 保存名から実ファイルのパスを得る。形式が不正・存在しない場合は空 */
    public Optional<Path> resolve(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) {
            return Optional.empty();
        }
        Path path = uploadDir.resolve(storedName).normalize();
        return (path.startsWith(uploadDir) && Files.isRegularFile(path)) ? Optional.of(path) : Optional.empty();
    }

    /** 受け付ける画像の種類と、そのファイルの先頭バイト */
    enum ImageType {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp");

        final String extension;
        final String mediaType;

        ImageType(String extension, String mediaType) {
            this.extension = extension;
            this.mediaType = mediaType;
        }

        static Optional<ImageType> detect(byte[] head) {
            if (startsWith(head, 0, 0xFF, 0xD8, 0xFF)) {
                return Optional.of(JPEG);
            }
            if (startsWith(head, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
                return Optional.of(PNG);
            }
            if (startsWith(head, 0, 'R', 'I', 'F', 'F') && startsWith(head, 8, 'W', 'E', 'B', 'P')) {
                return Optional.of(WEBP);
            }
            return Optional.empty();
        }

        static Optional<ImageType> fromFileName(String name) {
            return Arrays.stream(values()).filter(t -> name.endsWith("." + t.extension)).findFirst();
        }

        private static boolean startsWith(byte[] data, int offset, int... expected) {
            if (data.length < offset + expected.length) {
                return false;
            }
            for (int i = 0; i < expected.length; i++) {
                if ((data[offset + i] & 0xFF) != expected[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /** 画像の Content-Type(レスポンス用) */
    public String mediaTypeOf(String storedName) {
        return ImageType.fromFileName(storedName).map(t -> t.mediaType).orElse("application/octet-stream");
    }
}
