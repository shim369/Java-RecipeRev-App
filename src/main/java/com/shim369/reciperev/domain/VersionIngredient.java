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
import java.math.BigDecimal;

/** 版ごとの材料1行。版と一緒に作られ、以後変更されない(BR-02)ので setter を持たない */
@Entity
@Table(name = "version_ingredients")
public class VersionIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: 材料を読むたびに版まで JOIN しないようにする(JPA の既定 EAGER は性能事故のもと)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private RecipeVersion version;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(precision = 8, scale = 2)
    private BigDecimal amount;

    @Column(length = 20)
    private String unit;

    @Column(length = 100)
    private String note;

    protected VersionIngredient() {
    }

    VersionIngredient(RecipeVersion version, int sortOrder, IngredientLine line) {
        this.version = version;
        this.sortOrder = sortOrder;
        this.name = line.name();
        this.amount = line.amount();
        this.unit = line.unit();
        this.note = line.note();
    }

    public IngredientLine toLine() {
        return new IngredientLine(name, amount, unit, note);
    }

    public Long getId() {
        return id;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getUnit() {
        return unit;
    }

    public String getNote() {
        return note;
    }
}
