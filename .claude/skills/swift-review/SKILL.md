---
name: swift-review
description: Review Swift code for adherence to Apple API Design Guidelines, correct concurrency (async/await, actor, Sendable), proper @Observable usage, SwiftUI state management, value vs reference semantics, and idiomatic error handling. Use when reviewing .swift files.
---

# Swift Code Review チェックリスト

`.swift` ファイルをレビューするときに適用するチェックリスト。各項目は公式ソースに基づく。指摘する際は該当カテゴリ・該当ファイルの該当箇所を明示し、可能な限り公式リンクで根拠を示すこと。

レビュー方針:

- 機能的な正しさより前に「セマンティクス」「並行性安全性」「明確性」を見る。
- 修正候補は具体的な diff サイズの小さい改善を提案する。
- 「好み」と「規約違反」を区別し、規約違反のみを必須指摘とする。

---

## 1. API 命名（Apple API Design Guidelines）

ソース: https://www.swift.org/documentation/api-design-guidelines/

- [ ] 利用箇所（call site）で英文として読めるか。`x.insert(y, at: z)` のように前置詞句が成立しているか。
- [ ] 必要な語が省略されていないか（`remove(at:)` を `remove(_:)` にしていないか）。
- [ ] 型情報から自明な冗長語が含まれていないか（`removeElement(_:)` ではなく `remove(_:)`）。
- [ ] 役割で命名しているか。`string`、`object` のような型ベースの汎用名を避けているか。
- [ ] mutating / nonmutating のペアが規約に従っているか。
  - 動詞ベース: `sort()` / `sorted()`、`reverse()` / `reversed()`
  - 名詞ベース: `formUnion(_:)` / `union(_:)`
- [ ] 副作用のあるメソッドが命令形動詞、副作用なしが名詞句になっているか。
- [ ] ファクトリは `make` プレフィックスか。
- [ ] 第一引数のラベル省略・前置詞ラベル化が文法的に正しいか。
- [ ] ケース規約: 型・プロトコル `UpperCamelCase`、それ以外 `lowerCamelCase`。
- [ ] 頭字語の大文字小文字: 文中位置に応じて統一されているか（`urlString` / `HTTPHeader`）。
- [ ] パブリック宣言にドキュメントコメント（`///`）があるか。

---

## 2. 並行性（Swift Concurrency）

ソース: https://docs.swift.org/swift-book/documentation/the-swift-programming-language/concurrency/, https://developer.apple.com/documentation/swift/concurrency

- [ ] **`DispatchQueue.main.async` で UI 更新していないか**。`@MainActor` 隔離を使うべき。
- [ ] **UI を扱う型・関数に `@MainActor` が付いているか**（ViewModel、Coordinator、UIKit/AppKit ハンドラ）。
- [ ] **共有可変状態を持つクラスに対し、`actor` 化や明示的同期が検討されているか**。
- [ ] **`Task.detached` の濫用はないか**。優先度・アクター継承が必要なら `Task { ... }` を使う。
- [ ] **長時間ループや I/O ループで `try Task.checkCancellation()` を呼んでいるか**。
- [ ] **`Thread.sleep` を使っていないか**。代わりに `try await Task.sleep(for:)`。
- [ ] **`async let` または `TaskGroup` で並列化できる逐次 await はないか**。
- [ ] **クロージャを境界越しに渡す API は `@Sendable` か**。
- [ ] **Sendable 違反が抑制 (`@unchecked Sendable`) で隠されていないか**。隠す場合は同期戦略をコメントで説明しているか。
- [ ] **`await` を挟む箇所で前後の不変条件を仮定していないか**（再入の考慮）。
- [ ] **コールバックベース API を呼んでいるか**。可能なら `withCheckedContinuation` / `withCheckedThrowingContinuation` で async ラップする。

---

## 3. Observation / モデル

ソース: https://developer.apple.com/documentation/observation, https://developer.apple.com/documentation/swiftui/migrating-from-the-observable-object-protocol-to-the-observable-macro

- [ ] **iOS 17 以降をサポートしているプロジェクトで `ObservableObject` + `@Published` を新規追加していないか**。`@Observable` に統一する。
- [ ] **`@Observable` クラスが `final` か**。
- [ ] **View で観測したくない内部状態に `@ObservationIgnored` が付いているか**。
- [ ] **stored property に依らない計算プロパティで、自動追跡が効かないものを手動 (`access` / `withMutation`) で扱っているか**。
- [ ] **`@Observable` を View が所有するときに `@State`、注入には `@Environment(M.self)`、Binding 抽出には `@Bindable` を使っているか**。古い `@StateObject` / `@ObservedObject` / `@EnvironmentObject` を新規に使っていないか。
- [ ] **`withObservationTracking` の `onChange` が「一度だけ」呼ばれる仕様を理解しているか**（継続観測したいなら再登録）。

---

## 4. SwiftUI State

ソース: https://developer.apple.com/documentation/swiftui/state-and-data-flow, https://developer.apple.com/documentation/swiftui/managing-model-data-in-your-app

- [ ] `@State` プロパティが `private` か。
- [ ] View ローカルの値型は `@State`、親が持つ値の読み書きは `@Binding` になっているか。
- [ ] 階層を超える共有モデルは `.environment(model)` + `@Environment(M.self)` になっているか。
- [ ] `@Bindable` の使い所が正しいか（`@Observable` オブジェクトの Binding 抽出）。
- [ ] modifier の順序が意図通りか（`.padding().background()` と `.background().padding()` の差）。
- [ ] `ForEach` に安定した `id:` が渡っているか（`Identifiable` か明示 KeyPath）。
- [ ] View 内で重い処理・I/O を `body` で実行していないか（`task` modifier に逃がす）。
- [ ] View が肥大化していないか。サブ View や `@ViewBuilder` で分割する。

---

## 5. 値 vs 参照（struct vs class）

ソース: https://docs.swift.org/swift-book/documentation/the-swift-programming-language/classesandstructures/

- [ ] 単純なデータの集約に `class` を使っていないか（まず `struct`）。
- [ ] 継承予定がない `class` が `final` になっているか。
- [ ] `struct` が `class` プロパティを抱え、値セマンティクスを壊していないか。
- [ ] 同一性（identity）が意味を持たないモデルが、`===` で比較されていないか。
- [ ] プロパティのデフォルトは `let`、必要なときだけ `var` になっているか。
- [ ] 値型のメソッドが必要に応じて `mutating` になっているか。
- [ ] 巨大な struct を毎回コピーしていないか（プロファイル後、必要なら参照型化）。

---

## 6. プロトコル / ジェネリクス

ソース: https://docs.swift.org/swift-book/documentation/the-swift-programming-language/protocols/

- [ ] プロトコルが目的単位で分割されているか（1 プロトコル 1 責務）。
- [ ] 参照型専用プロトコル（デリゲートなど）に `AnyObject` 制約があるか。
- [ ] デリゲートが `weak var` で保持されているか。
- [ ] `some P`（不透明型）で済むのに `any P`（存在型）を使っていないか。
- [ ] 関連型に必要な制約・`where` が付いているか。
- [ ] 共通実装をプロトコル拡張の既定実装で表現できる箇所はないか。
- [ ] 合成可能（`Equatable` / `Hashable` / `Codable`）なものを手書きしていないか。

---

## 7. エラーハンドリング

ソース: https://docs.swift.org/swift-book/documentation/the-swift-programming-language/errorhandling/

- [ ] `Error` 型が enum で網羅的に列挙されているか（汎用 `struct GenericError` の濫用がないか）。
- [ ] レイヤ境界で適切にエラー型を変換しているか（下位の型を上位に漏らしていないか）。
- [ ] `try!` の使用は本当に失敗があり得ない箇所だけか。
- [ ] `try?` で握りつぶしているが、本来は呼び出し側に判断させるべき箇所はないか。
- [ ] エラーログと伝播が分離されているか（`catch` 内で `print` だけで終わっていないか）。
- [ ] クロージャを受け取り throw を素通しすべき関数で `rethrows` を使っているか。
- [ ] リソース解放が `defer` でフェイルセーフに行われているか。
- [ ] Swift Concurrency 文脈の新規 API でコールバック + `Result` を選んでいないか（`async throws` を優先）。
- [ ] プログラム不変条件の違反に `throws` を使っていないか（`precondition` / `fatalError` を使うべき）。

---

## 出力フォーマット

レビュー結果はカテゴリ別にまとめ、以下の形で各指摘を書く:

```
[カテゴリ] ファイル:行 — 問題の要約
根拠: 公式 URL
推奨: 修正の概要（必要なら短いコード差分）
```

優先度の付け方:

- **Must**: 並行性安全性、Sendable 違反、`@MainActor` 漏れ、`try!` 濫用、API ガイドライン違反
- **Should**: 命名の冗長性、`ObservableObject` 残存、`final` 化、`some` vs `any`
- **Nit**: ドキュメントコメント、modifier 順、`private` 付与忘れ
