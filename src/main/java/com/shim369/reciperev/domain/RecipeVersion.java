package com.shim369.reciperev.domain;

import com.shim369.reciperev.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * レシピの版。材料・手順は作成時に確定し、以後変えられるのは状態だけ(BR-02)。
 * 状態遷移のルール(BR-04, BR-07)はこのクラスのメソッドに閉じ込め、サービス層から直接 status を書き換えさせない。
 *
 * <p>DB の生成列 final_marker はマッピングしていない。値を決めるのは DB なので、アプリが触る必要がないため。
 */
@Entity
@Table(name = "recipe_versions")
public class RecipeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "based_on_version_id")
    private RecipeVersion basedOn;

    // Hibernate 6 は MySQL だと enum を ENUM 型の列として扱おうとするため、VARCHAR として扱うよう明示する
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private VersionStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String steps;

    @Column(name = "change_note", length = 200)
    private String changeNote;

    // 材料は版の一部なので、版を保存すると一緒に保存される(cascade)
    @OneToMany(mappedBy = "version", cascade = CascadeType.PERSIST)
    @OrderBy("sortOrder ASC")
    private List<VersionIngredient> ingredients = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RecipeVersion() {
    }

    /**
     * 新しい版を作る。状態は必ず下書きから始まる(BR-04)ので、引数で状態を受け取らない。
     *
     * @param basedOn 派生元の版。第1版は null
     */
    public static RecipeVersion create(Recipe recipe, int versionNo, RecipeVersion basedOn,
                                       VersionContent content, String changeNote) {
        RecipeVersion v = new RecipeVersion();
        v.recipe = recipe;
        v.versionNo = versionNo;
        v.basedOn = basedOn;
        v.status = VersionStatus.DRAFT;
        v.steps = content.stepsAsText();
        v.changeNote = (changeNote == null || changeNote.isBlank()) ? null : changeNote.strip();
        int order = 1;
        for (IngredientLine line : content.ingredients()) {
            v.ingredients.add(new VersionIngredient(v, order++, line));
        }
        return v;
    }

    /** 下書き → 試作中。下書き以外からは呼べない */
    public void startTrial() {
        if (status != VersionStatus.DRAFT) {
            throw new BusinessRuleException("第" + versionNo + "版は「" + status.getLabel() + "」のため、試作を開始できません。");
        }
        status = VersionStatus.TRIAL;
    }

    /**
     * 決定版にする。前の決定版を試作中に戻す処理(BR-06)は、同じレシピの別の行を書き換えるため
     * このメソッドではなく VersionService が一括更新SQLで行う。
     */
    public void markFinal() {
        if (status == VersionStatus.FINAL) {
            throw new BusinessRuleException("第" + versionNo + "版はすでに決定版です。");
        }
        status = VersionStatus.FINAL;
    }

    public VersionContent toContent() {
        return new VersionContent(
                ingredients.stream().map(VersionIngredient::toLine).toList(),
                VersionContent.parseSteps(steps));
    }

    public List<String> getStepList() {
        return VersionContent.parseSteps(steps);
    }

    public boolean isDraft() {
        return status == VersionStatus.DRAFT;
    }

    public boolean isFinal() {
        return status == VersionStatus.FINAL;
    }

    public Long getId() {
        return id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public RecipeVersion getBasedOn() {
        return basedOn;
    }

    public VersionStatus getStatus() {
        return status;
    }

    public String getSteps() {
        return steps;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public List<VersionIngredient> getIngredients() {
        return ingredients;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
