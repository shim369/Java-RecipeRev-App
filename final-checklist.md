# RecipeRev — Final Checklist

リリース前に、リポジトリのオーナーが手作業で行う項目です。

## 1. Pull Request をマージする(上から順に)

各 PR は1つ前のフェーズのブランチを base にしているので、差分にはそのフェーズの変更だけが表示されます。
上から順にマージしてください。マージするとそのブランチが削除され、次の PR の base は GitHub が自動で `main` に付け替えます。

| 順番 | ブランチ | 内容 |
|---|---|---|
| 1 | `feature/phase-1-requirements` | 設計書(要件定義・画面一覧・画面遷移図・ER図)、README、LICENSE |
| 2 | `feature/phase-2-crud` | 雛形、docker-compose、Flyway、レシピCRUD |
| 3 | `feature/phase-3-versioning` | 版管理、差分表示、承認フロー |
| 4 | `feature/phase-4-cooklog` | 調理ログ、写真、評価推移グラフ |
| 5 | `feature/phase-5-analysis` | Python 傾向分析 |
| 6 | `feature/phase-6-tests-docs` | テスト、テスト仕様書、README、このチェックリスト |

> 「Squash and merge」ではなく、通常の「Create a merge commit」でマージしてください。
> スカッシュするとコミットが作り直されるため、次のブランチとの間で同じ変更が重複して表示されます。

## 2. GitHub のリポジトリ設定

リポジトリのトップページで、About の歯車アイコンから設定します。

**Description**

```
Recipe version-control web app (Java 21 / Spring Boot): track every revision, diff any two versions, approve one final formulation, and analyze ingredient-vs-rating trends with Python.
```

**Topics**

```
java spring-boot spring-mvc spring-data-jpa thymeleaf mysql docker docker-compose python chartjs javascript version-control diff recipe portfolio
```

## 3. リリース(任意)

`main` へのマージがすべて終わったら、タグを付けて GitHub Release を作成します。

```bash
git switch main && git pull
git tag -a v1.0.0 -m "RecipeRev v1.0.0"
git push origin v1.0.0
```

## 4. 手動確認(テスト仕様書で「未実施」になっているもの)

ブラウザでの操作がまだ確認できていない項目です。`./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` で起動して確認してください。

- [ ] MT-01: 新規登録画面で「+ 材料を追加」と「削除」を何回か操作してから登録し、すべての行が保存される
- [ ] MT-02: レシピ詳細の版履歴でラジオボタンを2つ選び、「選んだ2つの版を比較」で差分画面が開く
- [ ] MT-03: 「この版を決定版にする」の確認ダイアログに「現在の決定版 第N版は試作中に戻ります」と出る
- [ ] MT-04: 調理ログで写真を選ぶとプレビューが出る。5MB を超えるファイルを選ぶとその場で警告が出る
- [x] MT-10: 右上の「日本語」/「English」ボタンで、同じ画面のまま言語が切り替わる。別の画面に移っても言語が保たれる

結果は [docs/05_test_spec.md](docs/05_test_spec.md) の 3章に反映してください。
