package com.shim369.reciperev.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** 調理ログ。版を実際に作った記録 */
@Entity
@Table(name = "cook_logs")
public class CookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private RecipeVersion version;

    @Column(name = "cooked_on", nullable = false)
    private LocalDate cookedOn;

    // 列は TINYINT。Java 側は扱いやすい int にして、型だけ JDBC に伝える
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String memo;

    /** uploads/ からの相対パス(ファイル名のみ) */
    @Column(name = "photo_path", length = 255)
    private String photoPath;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CookLog() {
    }

    public CookLog(RecipeVersion version, LocalDate cookedOn, int rating, String memo, String photoPath) {
        this.version = version;
        this.cookedOn = cookedOn;
        this.rating = rating;
        this.memo = (memo == null || memo.isBlank()) ? null : memo.strip();
        this.photoPath = photoPath;
    }

    public Long getId() {
        return id;
    }

    public RecipeVersion getVersion() {
        return version;
    }

    public LocalDate getCookedOn() {
        return cookedOn;
    }

    public int getRating() {
        return rating;
    }

    public String getMemo() {
        return memo;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
