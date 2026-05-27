# Kotlin Multiplatform (KMP) リファレンス

Kotlin Multiplatform の言語・フレームワーク観点でのリファレンス集。
特定プロジェクトに依存しない、言語仕様・公式ドキュメント準拠の内容のみ。

## ドキュメント一覧

| ドキュメント | 概要 |
| --- | --- |
| [concept.md](./concept.md) | KMP とは何か、Flutter との比較、Compose Multiplatform、基本的な仕組み |
| [source-sets.md](./source-sets.md) | ソースセットの階層、`commonMain` / `androidMain` / `iosMain`、デフォルト階層テンプレート |
| [expect-actual.md](./expect-actual.md) | `expect` / `actual` 宣言のルール、制約、インターフェース + DI による代替 |
| [ios-integration.md](./ios-integration.md) | shared モジュールを Swift から使う方法（Framework / CocoaPods / SPM / XCFramework）、型マッピング、`suspend` → `async` |
| [ios-interop-pitfalls.md](./ios-interop-pitfalls.md) | iOS 連携での落とし穴（`description` などの `NSObject` 名前衝突） |
| [dependencies.md](./dependencies.md) | KMP 対応ライブラリの選び方、Gradle 設定、Version Catalog、ハマりポイント |

## 参考

すべて公式ドキュメント (`kotlinlang.org`) に基づいて記述している。各ドキュメントの末尾にある `References` セクションを参照。
