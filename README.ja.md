# RecipeRev

[English](README.md) | [日本語](README.ja.md)

**RecipeRev**(Recipe + Revision)は、レシピのバージョン管理Webアプリです。レシピを調整するたびに新しい版を記録し、任意の2版を並べて比較し、調理結果を記録して「一番おいしい配合」を探せます。

> **ステータス:** 開発中です。進捗は[ロードマップ](#ロードマップ)を参照してください。

## コンセプト

料理は試行錯誤の連続です。砂糖を少し減らす、醤油を少し足す、加熱時間を変える。RecipeRevは、レシピを「変更履歴・差分表示・承認フローを持つ管理対象」として扱い、何を変えたのか、それで良くなったのかを確認できるようにします。

このプロジェクトはポートフォリオでもあります。産業系の「レシピ管理」システムに典型的な構造(変更履歴・差分・承認)を身近な題材で再現し、要件定義・設計・開発・テスト・リリースの全工程を成果物として残しています。

## 主な機能

- **レシピ管理**: レシピの登録・編集・削除(名前、説明、材料と分量、手順)
- **自動バージョン管理**: 編集のたびに新しい版を作成。材料と手順は版ごとに保持
- **差分表示**: 任意の2版を選び、追加(緑)・削除(赤)・変更(黄)を材料・分量・手順ごとに色付きで表示
- **承認フロー**: `下書き` -> `試作中` -> `決定版`。決定版はレシピごとに1つだけで、新しい決定版を選ぶと以前の決定版は「試作中」に戻ります
- **調理ログ**: 調理日、評価(1〜5)、味メモ、写真(1枚)を版に紐づけて記録
- **評価推移グラフ**: 版ごとの平均評価を折れ線グラフで表示
- **傾向分析**: Pythonスクリプトで材料の分量と評価の相関を分析し(例: 「砂糖が多い版ほど高評価」)、結果を画面に表示

## 技術スタック

| 領域 | 技術 |
| --- | --- |
| バックエンド | Java 21, Spring Boot 3.x, Spring MVC, Spring Data JPA, Maven |
| フロントエンド | Thymeleaf, JavaScript, Chart.js, HTML/CSS |
| データベース | MySQL 8(Docker Compose) |
| 分析 | Python 3(`analysis/`) |
| テスト | JUnit 5, Spring Boot Test, Testcontainers または H2 |

## アーキテクチャ

```
controller  ->  service  ->  repository  ->  MySQL
   (MVC)     (@Transactional,   (JPA / SQL)
              差分ロジック)
```

- レイヤー構成のパッケージ: `controller` / `service` / `repository` / `domain` / `dto`
- 差分ロジックは専用のサービスに分離し、単体テストで検証
- トランザクション境界はservice層に配置
- 入力検証はBean Validation、例外は`@ControllerAdvice`で集約
- Python分析はアプリから疎結合(バッチ実行とJSON/CSVの受け渡し)

## データベース

最小構成のテーブルです。

- `recipes`: レシピ本体
- `recipe_versions`: 版番号、手順、作成日、状態(`recipe_id` + 版番号でユニーク。決定版は1レシピにつき最大1件)
- `version_ingredients`: 版ごとの材料と分量
- `cook_logs`: 版ID、評価、メモ、調理日、写真パス

ER図・制約・版間比較SQLは [docs/04_er_diagram.md](docs/04_er_diagram.md) を参照してください。

## セットアップ

### 前提

- JDK 21
- Docker / Docker Compose
- Python 3(傾向分析を使う場合のみ)

### 起動

```bash
# 1. クローン
git clone https://github.com/shim369/Java-RecipeRev-App.git
cd Java-RecipeRev-App

# 2. MySQLを起動
docker compose up -d

# 3. アプリを起動
./mvnw spring-boot:run
```

起動後、http://localhost:8080 を開いてください。

Dockerを使わずローカルのMySQLを使う場合は、データベースを作成し、`src/main/resources/application.properties` の接続設定を調整してください。

### テストの実行

```bash
./mvnw test
```

## ディレクトリ構成

```
Java-RecipeRev-App/
├── docs/              # 要件定義、画面一覧、画面遷移、ER図、テスト仕様書
├── analysis/          # Pythonによる傾向分析
├── src/
│   ├── main/java/     # controller / service / repository / domain / dto
│   ├── main/resources/ # テンプレート、静的ファイル、アプリ設定
│   └── test/java/
├── docker-compose.yml
├── pom.xml
├── README.md
└── README.ja.md
```

## ロードマップ

- [x] Phase 1: 要件定義、画面一覧、画面遷移、ER図(`docs/`)
- [x] Phase 2: プロジェクト雛形、Docker Compose、レシピCRUD
- [ ] Phase 3: バージョン管理、差分表示、承認フロー
- [ ] Phase 4: 調理ログ(写真含む)、評価推移グラフ
- [ ] Phase 5: Python傾向分析の連携
- [ ] Phase 6: 単体・結合テスト、テスト仕様書、ドキュメント整備

## ライセンス

このプロジェクトは [MIT License](LICENSE) のもとで公開されています。
