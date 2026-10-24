# 05. テスト仕様書 — RecipeRev

関連ドキュメント: [01_要件定義書](01_requirements.md) / [02_画面一覧](02_screens.md) / [04_ER図・テーブル定義](04_er_diagram.md)

---

## 1. 方針

| 種別 | 対象 | ツール | DB | 実行コマンド |
|---|---|---|---|---|
| 単体テスト(UT) | 差分ロジック・ドメインモデル・入力チェック・写真保存・CSV出力 | JUnit 5 + AssertJ | 使わない | `./mvnw test` |
| 結合テスト(IT) | サービス〜SQL〜DB制約、画面(MockMvc)、Python連携 | Spring Boot Test + Testcontainers | MySQL 8.4(Docker) | `./mvnw test` |
| Python 単体テスト(PY) | `analysis/analyze.py` | unittest + pandas | 使わない | `python3 -m unittest discover -s analysis/tests` |
| 手動確認(MT) | 画面の見た目・JavaScript・ブラウザ操作 | Chrome | docker compose の MySQL | 3章の手順 |

- **結合テストは H2 ではなく本物の MySQL で行う。** 生成列による一意制約、CHECK 制約、`SELECT ... FOR UPDATE`、`<=>`、CTE など MySQL 固有の動きを確かめたいため。
- 結合テストでは、各テストの前にテーブルを空にする。サービスがトランザクションをコミットするため、テストに `@Transactional` を付けてロールバックさせる方式は使わない(コミット後に動く処理も確かめたいため)。
- テストメソッドの `@DisplayName` には、この仕様書のテストIDを付けている。

### 実行結果(2026-10-24)

| 種別 | 件数 | 結果 |
|---|---|---|
| Java(単体 + 結合) | 77 | すべて成功 |
| Python | 10 | すべて成功 |

---

## 2. テストケース

### 2.1 差分ロジック — `RecipeDiffServiceTest`(UT-DIFF)

| ID | 観点 | 入力 | 期待結果 | 要件 |
|---|---|---|---|---|
| UT-DIFF-01 | 材料の4分類 | チョコ同じ・砂糖30→40・バター削除・生クリーム追加 | 変更なし/変更/削除/追加。削除行は同じ位置の追加行より先 | F-07 |
| UT-DIFF-02 | 数値の桁 | 100 と 100.00 | 変更なし | F-07 |
| UT-DIFF-03 | 単位・メモだけの違い | 大さじ→小さじ、メモ追加 | 変更 | F-07 |
| UT-DIFF-04 | NULL 同士 | 分量が両方とも空欄 | 変更なし | F-07 |
| UT-DIFF-05 | NULL → 値 | 空欄 → 2 | 変更 | F-07 |
| UT-DIFF-06 | 変更前後の値 | 30g → 40g | before/after の両方を保持 | F-07 |
| UT-DIFF-11 | 手順が同じ | a,b / a,b | すべて変更なし | F-07 |
| UT-DIFF-12 | 行の挿入 | a,c → a,b,c | b のみ追加 | F-07 |
| UT-DIFF-13 | 行の削除 | a,b,c → a,c | b のみ削除 | F-07 |
| UT-DIFF-14 | 行の書き換え | 170度で30分 → 160度で40分 | 変更(対になる) | F-07 |
| UT-DIFF-15 | 削除が多い塊 | x,y,z → w,z | 変更(x→w)+ 削除(y)+ 変更なし(z) | F-07 |
| UT-DIFF-16 | 並べ替え | a,b → b,a | 共通部分1行 + 変更1行(計3行) | F-07 |
| UT-DIFF-17 | 空との比較 | 空 ↔ 手順あり | 全行追加 / 全行削除 | F-07 |
| UT-DIFF-21 | 変更なしの判定 | 同じ内容 | `hasChanges() = false` | BR-03 |
| UT-DIFF-22 | 材料の並び順だけ変えた場合 | A,B → B,A | 変更なし(材料は名前で照合するため) | BR-03 |
| UT-DIFF-23 | 件数の集計 | 材料・手順の混在 | 追加2 / 削除1 / 変更2 | F-07 |

### 2.2 ドメインモデル — `DomainTest`(UT-DOM)

| ID | 観点 | 期待結果 | 要件 |
|---|---|---|---|
| UT-DOM-01 | 手順の分割 | \r\n / \n / \r で分割し、空行と前後の空白を除く | F-02 |
| UT-DOM-02 | 材料の正規化 | 空文字は null に、名前の前後の空白は除く | F-02 |
| UT-DOM-03 | 分量の表示 | 「120 g」「1.5 大さじ」「少々」「1 個(室温)」「—」 | F-06 |
| UT-DOM-04 | 新しい版の状態 | 必ず DRAFT。sort_order は1から | BR-04 |
| UT-DOM-05 | 状態遷移 | DRAFT → TRIAL → FINAL | F-08 |
| UT-DOM-06 | 決定版への直行 | DRAFT → FINAL が可能 | F-08 |
| UT-DOM-07 | 不正な遷移 | TRIAL で試作開始すると BusinessRuleException | BR-07 |
| UT-DOM-08 | 二重の決定 | FINAL で決定版にすると BusinessRuleException | F-08 |
| UT-DOM-09 | 空の変更メモ | null として保存 | F-05 |

### 2.3 入力チェック — `FormValidationTest`(UT-VAL)

| ID | 項目 | NG になる例 | 要件 |
|---|---|---|---|
| UT-VAL-01 | 正常系 | (エラーなし) | 4.4 |
| UT-VAL-02 | 名前 | 空白のみ、101文字 | 4.4 |
| UT-VAL-03 | 材料 | 0行、材料名が空 | 4.4 |
| UT-VAL-04 | 材料名の重複 | 「塩」と「 塩」 | BR-11 |
| UT-VAL-05 | 分量 | -1、100000、1.234(空欄は OK) | 4.4 |
| UT-VAL-06 | 手順 | 空行のみ、51行、501文字 | 4.4 |
| UT-VAL-07 | 材料の行数 | 51行 | 4.4 |
| UT-VAL-08 | 調理ログ | 未来日、評価6、未入力 | 4.4 |

### 2.4 写真・分析の部品 — `PhotoStorageServiceTest` / `AnalysisCsvTest`(UT-PHOTO / UT-ANA)

| ID | 観点 | 期待結果 | 要件 |
|---|---|---|---|
| UT-PHOTO-01 | 種類の判定 | ファイル名・Content-Type が実際と違っても、中身の先頭バイトで PNG/JPEG/WebP を判定し、UUID名で保存する | F-09 |
| UT-PHOTO-02 | 画像以外 | PHP のコードなどは InvalidPhotoException | 5章 セキュリティ |
| UT-PHOTO-03 | 写真なし | 何もしない(空の Optional) | F-09 |
| UT-PHOTO-04 | パストラバーサル | `../secret.txt` などを解決しない | 5章 セキュリティ |
| UT-PHOTO-05 | 削除 | ファイルが消える。2回消しても例外にならない | F-09 |
| UT-ANA-01 | CSV のエスケープ | カンマ・ダブルクォートを含む材料名を RFC 4180 形式で出力する | F-11 |
| UT-ANA-02 | 傾向の文章 | 正の相関は positive、負の相関は negative、弱い相関は weak のメッセージキーを選ぶ。分量の範囲は末尾の0を表示しない | F-11 |

### 2.5 版・承認フロー — `RecipeVersionIntegrationTest`(IT-VER)

| ID | 観点 | 期待結果 | 要件 |
|---|---|---|---|
| IT-VER-01 | レシピ登録 | 第1版(DRAFT)と材料が作られる | F-02 |
| IT-VER-02 | 版の作成 | 最大版番号 + 1、DRAFT、派生元が記録される。過去の版を元にしても番号は最新。派生元の中身は変わらない | BR-01, 02, 04 |
| IT-VER-03 | 変更なし | NoChangesException。版は増えない | BR-03 |
| IT-VER-04 | **同時実行** | 4スレッドで同時に版を作っても、版番号は 2,3,4,5 で重複しない | 5章 同時実行 |
| IT-VER-05 | 決定版の切り替え | 前の決定版が TRIAL に戻り、メッセージに番号が入る | BR-05, 06 |
| IT-VER-06 | 不正な遷移 | 例外が発生し、状態はロールバックされる | BR-07 |
| IT-VER-07 | **DB の一意制約** | SQL を直接実行して決定版を2つにしようとすると `uk_recipe_versions_final` 違反 | BR-05 |
| IT-VER-08 | DB のその他の制約 | 版番号の重複は `uk_recipe_versions_recipe_no` 違反、不正な状態は CHECK 制約違反 | BR-01 |
| IT-VER-09 | 論理削除 | 一覧に出ない、参照すると NotFound、版は DB に残る | BR-09 |
| IT-VER-10 | 代表版 | 決定版があれば決定版、なければ最新版 | SCR-03 |
| IT-VER-11 | 同じ版どうしの比較 | BusinessRuleException | F-07 |

### 2.6 SQL — `RecipeQueryRepositoryIntegrationTest`(IT-SQL)

| ID | 観点 | 期待結果 | 要件 |
|---|---|---|---|
| IT-SQL-01 | **SQL と Java の差分が一致** | 4組の版の組み合わせで、LEFT JOIN + `<=>` の SQL と RecipeDiffService の結果(名前・種類・順序)が一致する | F-07 |
| IT-SQL-02 | 版ごとの平均評価 | 平均と件数。ログのない版は null / 0 | F-10 |
| IT-SQL-03 | 一覧の集計 | 最新版・決定版(生成列で検索)・全体の平均 | SCR-01 |
| IT-SQL-04 | 分析用の表 | 評価のある版だけを使う。入っていない材料は0。分量空欄の版は除外。単位が違えば別の材料 | F-11 |
| IT-SQL-05 | ログなし | 空 | F-11 |

### 2.7 調理ログ・分析 — `CookLogAndAnalysisIntegrationTest`(IT-LOG / IT-ANA)

| ID | 観点 | 期待結果 | 要件 |
|---|---|---|---|
| IT-LOG-01 | 自動で試作中へ | DRAFT の版にログを登録すると TRIAL になる。2回目は変わらない | BR-08 |
| IT-LOG-02 | 決定版へのログ | FINAL のまま | BR-08 |
| IT-LOG-03 | 写真の一連の流れ | 保存 → ログ削除のコミット後にファイルも消える | F-09 |
| IT-LOG-04 | **ロールバック時の写真** | 登録が失敗(存在しない版)したら、保存した写真も消える(TransactionSynchronization) | F-09 |
| IT-LOG-05 | 他レシピのログ | 削除できない(NotFound) | 5章 セキュリティ |
| IT-ANA-01 | **Python 連携の通し** | CSV → `python3 analyze.py` → JSON。砂糖は STRONG / POSITIVE、卵は NO_AMOUNT_VARIATION。python3 + pandas がない環境ではスキップする | F-11 |
| IT-ANA-02 | Python が起動できない | 「Python を起動できませんでした」という AnalysisException | F-11 |

### 2.8 画面 — `WebIntegrationTest`(IT-WEB、MockMvc)

| ID | 観点 | 期待結果 | 画面 |
|---|---|---|---|
| IT-WEB-01 | トップと一覧 | `/` は `/recipes` へリダイレクト。一覧に登録したレシピが出る | SCR-01 |
| IT-WEB-02 | 入力エラー | 200 で同じ画面に戻り、項目ごとのエラーメッセージを出す(既定の英語で確認) | SCR-02 |
| IT-WEB-03 | 版の保存(PRG) | 版詳細へリダイレクトし、フラッシュメッセージを出す。変更なしならフォームに戻る | SCR-05 |
| IT-WEB-04 | 差分画面 | `diff-changed` の行と変更前後の値を表示する。パラメータを省略すると from/to を補ってリダイレクトする | SCR-07 |
| IT-WEB-05 | 決定版にする | フラッシュメッセージを出す。もう一度実行すると 400 とエラー画面 | SCR-06 |
| IT-WEB-06 | エラー | 存在しないレシピ・版は 404、数値でない ID は 400 | SCR-99 |
| IT-WEB-07 | JSON API | 版ごとの平均評価を JSON で返す。存在しないレシピは ProblemDetail の 404 | API |
| IT-WEB-08 | 削除 | 一覧へリダイレクトし、以後の詳細は 404 | SCR-03 |

### 2.9 多言語対応 — `I18nIntegrationTest`(IT-I18N、MockMvc)

| ID | 観点 | 期待結果 | 要件 |
|---|---|---|---|
| IT-I18N-01 | 既定の言語 | ブラウザが `Accept-Language: ja` を送っても英語で表示する。切り替えボタンは「日本語」 | F-12 |
| IT-I18N-02 | 切り替え | `?lang=ja` で日本語になり、Cookie `reciperev-lang=ja` が保存される。`<html lang="ja">` | F-12 |
| IT-I18N-03 | Cookie による維持 | 状態名(下書き/Draft)・版番号(第1版/v1)・JavaScript に渡す文言が言語ごとに切り替わる | F-12 |
| IT-I18N-04 | 切り替えリンク | 今のページの URL とクエリ(from/to)を保ったまま `lang` だけを付け替える | F-12 |
| IT-I18N-05 | サーバ側のメッセージ | 入力エラー・業務ルール違反(状態名の引数も翻訳される)・フラッシュ・API の ProblemDetail が利用者の言語になる | F-12 |
| IT-I18N-06 | 不正な値 | `?lang=@@` は無視して英語のまま表示する | F-12 |

### 2.10 Python — `analysis/tests/test_analyze.py`(PY)

| ID | テストメソッド | 期待結果 |
|---|---|---|
| PY-01 | test_positive_strong_correlation | r = 1.0、STRONG、POSITIVE |
| PY-02 | test_negative_correlation | NEGATIVE |
| PY-03 | test_not_enough_samples | 版が2つなら NOT_ENOUGH_SAMPLES |
| PY-04 | test_no_amount_variation | 分量が一定なら NO_AMOUNT_VARIATION |
| PY-05 | test_no_rating_variation | 評価が一定なら NO_RATING_VARIATION |
| PY-06 | test_same_name_different_unit_are_separate | g と 大さじ は別の材料として集計する |
| PY-07 | test_strength_thresholds | 0.7 / 0.4 の境界値 |
| PY-08 | test_results_sorted_by_absolute_r | \|r\| の大きい順に並ぶ |
| PY-09 | test_missing_column_raises | 必要な列がなければ ValueError |
| PY-10 | test_main_reads_csv_with_empty_unit_and_writes_json | カンマを含む材料名・空の単位を含む CSV を読み、JSON を書き出す |

---

## 3. 手動確認(MT)

`docker compose up -d` → `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` で起動し、http://localhost:8080 で確認する。
「結果」欄には、何で確認したかを書いている。ブラウザでのクリック操作がまだのものは「未実施」とし、[final-checklist.md](../final-checklist.md) に確認項目として残している。

| ID | 操作 | 確認内容 | 結果 |
|---|---|---|---|
| MT-01 | 新規登録で「+ 材料を追加」「削除」を操作して登録する | 行番号が振り直され、すべての行が保存される | 未実施(フォーム送信は curl・MockMvc で確認済み) |
| MT-02 | 版履歴でラジオボタンを2つ選び「比較」 | SCR-07 に遷移し、緑・赤・黄と記号 + − ~ が表示される | 未実施(差分画面の表示はヘッドレス Chrome のスクリーンショットで確認済み) |
| MT-03 | 「この版を決定版にする」 | 確認ダイアログに、試作中に戻る決定版の番号が表示される | 未実施(確認文の HTML 出力は curl で確認済み) |
| MT-04 | 写真を選ぶ | 送信前にプレビューが出る。5MB を超えるファイルはその場で警告する | 未実施 |
| MT-05 | レシピ詳細のグラフ | 決定版の点が金色で大きく、ログのない版で線が途切れる | OK(ヘッドレス Chrome のスクリーンショット) |
| MT-06 | 傾向分析を実行 | 材料ごとの r・強さ・傾向の文章と、結果JSONが表示される | OK(curl + スクリーンショット) |
| MT-07 | 画面幅 375px | 横スクロールが出ない。材料の入力欄がカード形式に組み替わる | OK(375px 幅の iframe でスクリーンショット) |
| MT-08 | 6MB のファイルを送る | 「ファイルが大きすぎます」のエラー画面(400) | OK(curl で 400 を確認) |
| MT-09 | 全画面を英語・日本語で開く | すべて 200(存在しない ID は 404)。英語の画面に日本語が残っていない(切り替えボタンと入力データを除く) | OK(curl で全11画面を確認) |
| MT-10 | 右上の言語ボタンを押す | 同じ画面のまま言語が切り替わり、別の画面に移っても言語が保たれる | OK(Chrome で実際にクリック。版比較画面で「日本語」→ from/to を保ったまま日本語に、パンくずで詳細へ移っても日本語(グラフの軸も「第1版」)、「English」→ 英語に戻り、一覧でも英語のまま) |
