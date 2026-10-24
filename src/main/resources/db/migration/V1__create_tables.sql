-- RecipeRev V1: 初期スキーマ(docs/04_er_diagram.md の DDL と同一)
CREATE TABLE recipes (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(1000) NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME      NULL,
    PRIMARY KEY (id),
    INDEX idx_recipes_deleted_updated (deleted_at, updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE recipe_versions (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    recipe_id           BIGINT       NOT NULL,
    version_no          INT          NOT NULL,
    based_on_version_id BIGINT       NULL,
    status              VARCHAR(10)  NOT NULL DEFAULT 'DRAFT',
    steps               TEXT         NOT NULL,
    change_note         VARCHAR(200) NULL,
    final_marker        BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'FINAL' THEN recipe_id END) STORED,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recipe_versions_recipe_no (recipe_id, version_no),
    UNIQUE KEY uk_recipe_versions_final (final_marker),
    CONSTRAINT fk_recipe_versions_recipe   FOREIGN KEY (recipe_id)           REFERENCES recipes (id),
    CONSTRAINT fk_recipe_versions_based_on FOREIGN KEY (based_on_version_id) REFERENCES recipe_versions (id),
    CONSTRAINT ck_recipe_versions_status   CHECK (status IN ('DRAFT', 'TRIAL', 'FINAL'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE version_ingredients (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    version_id BIGINT        NOT NULL,
    sort_order INT           NOT NULL,
    name       VARCHAR(50)   CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    amount     DECIMAL(8, 2) NULL,
    unit       VARCHAR(20)   NULL,
    note       VARCHAR(100)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_version_ingredients_name (version_id, name),
    CONSTRAINT fk_version_ingredients_version FOREIGN KEY (version_id) REFERENCES recipe_versions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE cook_logs (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    version_id BIGINT        NOT NULL,
    cooked_on  DATE          NOT NULL,
    rating     TINYINT       NOT NULL,
    memo       VARCHAR(1000) NULL,
    photo_path VARCHAR(255)  NULL,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_cook_logs_version_cooked (version_id, cooked_on),
    CONSTRAINT fk_cook_logs_version FOREIGN KEY (version_id) REFERENCES recipe_versions (id),
    CONSTRAINT ck_cook_logs_rating  CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
