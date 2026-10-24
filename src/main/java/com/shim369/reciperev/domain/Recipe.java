package com.shim369.reciperev.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * レシピ本体。材料・手順は持たず、それらはすべて {@link RecipeVersion} が持つ。
 * setter を公開せず、意味のある操作(rename, softDelete...)だけをメソッドにしている。
 * こうすると「どこで何が変わりうるか」がクラスを見るだけで分かる。
 */
@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL の AUTO_INCREMENT を使う
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** JPA がリフレクションで生成するための引数なしコンストラクタ(アプリからは使わない) */
    protected Recipe() {
    }

    public Recipe(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void changeBasicInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /** 新しい版ができたときに一覧の並び順(更新日時)を進める */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /** BR-09: 論理削除。版と調理ログは残る */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
