# Swift 言語リファレンス

Swift 言語と Apple フレームワーク（SwiftUI、Observation、Swift Concurrency）に関するリファレンスドキュメント集です。プロジェクト固有のコードには触れず、純粋な言語仕様・公式ガイドライン・標準ライブラリのみを扱います。

## ドキュメント一覧

| ファイル | 概要 |
| --- | --- |
| [api-design-guidelines.md](./api-design-guidelines.md) | Apple 公式の API Design Guidelines（命名、利用箇所での明快さ、流暢な使用感） |
| [concurrency.md](./concurrency.md) | `async`/`await`、`Task`、`actor`、`Sendable`、構造化並行性、キャンセル |
| [observation.md](./observation.md) | `@Observable` マクロ（iOS 17+）、`withObservationTracking`、`ObservableObject` からの移行 |
| [swiftui.md](./swiftui.md) | View 合成、`@State` / `@Binding` / `@Environment`、`@Observable` との連携 |
| [error-handling.md](./error-handling.md) | `throws` / `try` / `rethrows`、`Result`、使い分け |
| [value-vs-reference.md](./value-vs-reference.md) | struct と class の選択、値セマンティクス |
| [protocols.md](./protocols.md) | プロトコル、関連型、プロトコル拡張、プロトコル指向プログラミング |

## 参考

- Swift 公式ドキュメントトップ: https://www.swift.org/documentation/
- The Swift Programming Language: https://docs.swift.org/swift-book/
- Apple Developer Documentation: https://developer.apple.com/documentation/
