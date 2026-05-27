---
name: kmp-review
description: Review Kotlin Multiplatform code for correct source-set placement, expect/actual usage, iOS interop safety, and KMP-ready dependency choices. Use when reviewing changes touching shared/, commonMain/, androidMain/, iosMain/, or expect/actual declarations.
---

# KMP コードレビュー チェックリスト

Kotlin Multiplatform のコード差分をレビューするときに必ず確認するチェック項目。
**カテゴリ単位で順番に見ること**。各項目には根拠の公式ドキュメント URL を付記。

レビュー対象になりやすいパスの例：

- `shared/src/commonMain/**`
- `shared/src/androidMain/**`
- `shared/src/iosMain/**`
- `shared/build.gradle.kts`
- `*.kt` に含まれる `expect` / `actual` 宣言
- `gradle/libs.versions.toml`

---

## 1. ソースセット配置 (Source Set Placement)

> Ref: <https://kotlinlang.org/docs/multiplatform-discover-project.html>, <https://kotlinlang.org/docs/multiplatform-hierarchy.html>

- [ ] **`commonMain` にプラットフォーム固有 API がリークしていない**
  - `java.*`, `android.*` を `commonMain` で `import` していないか
  - `platform.UIKit.*`, `platform.Foundation.*` を `commonMain` で `import` していないか
  - これらが見つかったら、対応するソースセット（`androidMain`, `iosMain`）に移すか、共通インターフェースで抽象化する
- [ ] **共有可能なコードが個別ターゲットソースセットに孤立していない**
  - `iosArm64Main` と `iosSimulatorArm64Main` に **同じコードがコピペ** されていたら `iosMain` に集約
  - Apple 共通の処理 (`UIDevice` 使わないが `NSUUID` など Foundation だけ) は `appleMain` に置けないか検討
- [ ] **デフォルト階層テンプレートに乗っているか**
  - `kotlin {}` ブロックでターゲットが標準名 (`androidTarget()`, `iosArm64()`, `iosSimulatorArm64()`) で宣言されている
  - 手動 `dependsOn` で再発明していない（デフォルトテンプレートが提供するなら使う）
- [ ] **`commonMain` のテストは `commonTest` にある**
  - プラットフォーム依存のないテストが `androidUnitTest` だけに置かれていないか
- [ ] **不要な platform-specific source set を増やしていない**
  - 例：`iosMain` で済むものを `iosArm64Main` に書いていないか

---

## 2. expect / actual

> Ref: <https://kotlinlang.org/docs/multiplatform-expect-actual.html>

- [ ] **`expect` と `actual` のパッケージが一致**
  - パッケージが違うとマッチしない
- [ ] **すべてのプラットフォームで `actual` が提供されている**
  - 宣言したターゲット全部に対応する `actual` が必要（足りないとビルドエラー）
- [ ] **`expect class` を使う前に代替を検討したか**
  - `expect class` は Beta。**interface + expect factory function** で書き換えられないか確認
  - 例：

    ```kotlin
    // NG (avoidable)
    expect class FooService { fun run() }

    // OK (preferred)
    interface FooService { fun run() }
    expect fun fooService(): FooService
    ```

- [ ] **DI で代替できないか**
  - 既に Koin / Kotlin-Inject 等を使っているなら、`expect`/`actual` ではなくプラットフォーム実装を DI コンテナに登録する形にするのが望ましい
- [ ] **`expect` 側に実装が混入していない**
  - 関数本体・プロパティ初期化子を書いてはいけない
- [ ] **`actual typealias` の選択が妥当**
  - `java.time.Month` を `actual typealias Month = java.time.Month` のように、既存の安定 API があるなら活用
- [ ] **`expect` を濫用していない**
  - 共通ロジックで完結する内容は `commonMain` だけで書く。プラットフォーム差が **実際にある** ものだけ `expect` 化

---

## 3. iOS Interop

> Ref: <https://kotlinlang.org/docs/native-objc-interop.html>, <https://kotlinlang.org/docs/multiplatform-ios-integration-overview.html>

- [ ] **NSObject 由来の予約名と衝突していない**
  - 禁則プロパティ・メソッド名：`description`, `hash`, `debugDescription`, `isEqual`, `copy`, `mutableCopy`, `init`, `class`, `superclass`, `dealloc`
  - これらを Kotlin 側で同名宣言していたら、リネームするか `@ObjCName("xxx")` で別名を付ける
- [ ] **ジェネリクスに `: Any` の上限境界が付いている**
  - Swift から見たときに **null になってほしくない** プロパティ・戻り値を持つジェネリッククラスは `class Foo<T : Any>` で宣言
- [ ] **`suspend` 関数の例外契約が明示されている**
  - Swift で `try await` したい例外は `@Throws(SomeException::class)` を付ける
  - 付け忘れると `CancellationException` 以外は **アプリ強制終了** につながる
- [ ] **`sealed class` を Swift で扱う方針が決まっている**
  - `switch` での網羅性は効かない。SKIE 等を入れているか、ラッパーを用意しているか確認
- [ ] **`Flow` の Swift 連携が考慮されている**
  - 生 `Flow` を直接 Swift API に露出していないか
  - SKIE / `FlowWrapper` 経由になっているか
- [ ] **デフォルト引数に依存していない**
  - Swift にはデフォルト引数が伝わらない。Swift から呼ばれる API はオーバーロードかファクトリ関数で対応する
- [ ] **公開する KDoc が書かれている**
  - Swift 側の Xcode オートコンプリートに表示される。public API には `/** ... */` を付ける

---

## 4. 依存関係 (Dependencies)

> Ref: <https://kotlinlang.org/docs/multiplatform-add-dependencies.html>

- [ ] **`commonMain` の依存は KMP 対応ライブラリのみ**
  - Maven Central で `-iosarm64` / `-iossimulatorarm64` バリアントが公開されているか確認
  - JVM 専用ライブラリ（OkHttp, Retrofit 等）を `commonMain` に書いていないか
- [ ] **共通アーティファクト名を使っている**
  - `kotlinx-coroutines-core`（OK）／ `kotlinx-coroutines-core-jvm`（NG, Gradle が自動選択する）
- [ ] **テスト依存は `commonTest` に `kotlin("test")`**
  - プラットフォーム別 `kotlin("test-junit")` などを書きすぎていないか
- [ ] **同じ依存を複数ソースセットに重複宣言していない**
  - `commonMain` に書けば `androidMain` / `iosMain` にも自動で伝播する
- [ ] **Version Catalog (`libs.versions.toml`) を使っている**
  - バージョン直書きが散らばっていないか
  - 同じライブラリで複数バージョンが宣言されていないか
- [ ] **Framework export が必要なら明示されている**
  - Swift 側で型を直接参照したい外部ライブラリは `binaries.framework { export(libs.foo) }` で export されているか
  - export するには依存が `api(...)` で宣言されている必要がある

---

## 5. ビルド設定 (Build Config)

> Ref: <https://kotlinlang.org/docs/multiplatform-discover-project.html>, <https://kotlinlang.org/docs/multiplatform-ios-integration-overview.html>

- [ ] **`kotlin {}` ブロックで宣言したターゲットが過不足ない**
  - 使わないターゲットを宣言しているとビルドが遅くなる
- [ ] **iOS Framework の baseName が安定している**
  - PR ごとに `baseName` が変わっていないか（Swift 側の `import` 名と一致）
- [ ] **`isStatic` の選択が一貫している**
  - Static / Dynamic を統合方式（Direct / SPM / CocoaPods）に合わせて選んでいるか
- [ ] **XCFramework / Framework タスクの命名規約に従っている**
  - `assembleSharedXCFramework` などの標準タスクが動作する状態
- [ ] **`kotlin.cocoapods` プラグインを使うなら `ios.deploymentTarget` が指定されている**
- [ ] **Kotlin / KGP / AGP / Compose Compiler のバージョン互換**
  - JetBrains の互換マトリクスに沿っているか確認

---

## 進め方

1. 差分を眺めて **触られているレイヤー** を特定（`commonMain` のみ？ `iosMain` も？ `build.gradle.kts` も？）
2. 該当カテゴリのチェックを上から順に当てる
3. 違反を指摘するときは **このスキル中の参照 URL** を併記し、推奨形のコード例を添える
4. 「とりあえず動くがリファクタしたほうがいい」レベルのものは `nit:` / `suggestion:` でラベリング

## References

- Kotlin Multiplatform: <https://kotlinlang.org/docs/multiplatform.html>
- Project structure: <https://kotlinlang.org/docs/multiplatform-discover-project.html>
- Hierarchical project structure: <https://kotlinlang.org/docs/multiplatform-hierarchy.html>
- Expect/actual declarations: <https://kotlinlang.org/docs/multiplatform-expect-actual.html>
- iOS integration overview: <https://kotlinlang.org/docs/multiplatform-ios-integration-overview.html>
- Objective-C and Swift interop: <https://kotlinlang.org/docs/native-objc-interop.html>
- Adding dependencies on multiplatform libraries: <https://kotlinlang.org/docs/multiplatform-add-dependencies.html>
