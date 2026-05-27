# 値型 vs 参照型（struct vs class）

Swift では `struct` と `enum` が値型、`class` が参照型。設計の出発点は値型を選ぶこと。詳細は [The Swift Programming Language: Structures and Classes](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/classesandstructures/) を参照。

## セマンティクスの違い

| 項目 | 値型（struct / enum） | 参照型（class） |
| --- | --- | --- |
| 代入・引数渡し | コピー（実体は CoW で必要時のみ） | 参照のコピー |
| 同一性 | 値の等価性のみ | `===` で同一性、`==` で等価性 |
| 継承 | なし | 単一継承可 |
| 自動メンバーワイズ init | あり | なし |
| `mutating` メソッド | プロパティを変更するなら必要 | 不要 |
| ARC によるライフサイクル | スコープ | 参照カウント |
| デフォルト | スレッド境界をまたぎやすい（Sendable に近い） | 同期が必要 |

### コピーの例

```swift
struct Resolution { var width = 0; var height = 0 }

let hd = Resolution(width: 1920, height: 1080)
var cinema = hd
cinema.width = 2048
print(hd.width)     // 1920（影響なし）
print(cinema.width) // 2048
```

```swift
class VideoMode { var frameRate = 0.0 }

let a = VideoMode()
let b = a
b.frameRate = 60
print(a.frameRate) // 60.0（同じインスタンス）
```

### 同一性演算子

```swift
if a === b { /* 同一インスタンス */ }
if a !== b { /* 異なるインスタンス */ }
```

`===` / `!==` はクラスにのみ使う。

## どちらを選ぶか

Apple 公式の指針は「**まず `struct` を使い、必要が生じたら `class`**」。

### struct を選ぶ

- 単純なデータ値を表す（座標、色、識別子、DTO）。
- 値そのものに意味があり、同一性が問題にならない。
- 継承で振る舞いを差し替える必要がない。
- 並行性境界をまたいで安全に渡したい（Sendable 化しやすい）。
- 不変性をデフォルトにしたい。

### class を選ぶ

- 同一性（identity）が概念上重要（DB ハンドル、ファイル、ウィンドウなど Apple フレームワークの参照型）。
- 状態が共有・観測される必要がある（`@Observable`、`actor` を含む）。
- Objective-C との相互運用が必要。
- 継承階層を活用する Apple フレームワーク（`UIView`、`URLSessionTask` など）。
- 自前で deinit が必要なリソース管理。

## 値セマンティクス

値セマンティクスとは「コピーを保持しても元の値が変化しない」性質。Apple 標準ライブラリのコレクション（`Array`、`Dictionary`、`Set`、`String`）も値セマンティクスを持つ。

```swift
var a = [1, 2, 3]
var b = a
b.append(4)
print(a) // [1, 2, 3]
```

- 内部的には Copy-on-Write（CoW）で実体共有しつつ、書き込み時にだけコピーされ、性能と安全性を両立する。
- `class` を内部に持つ `struct` は値セマンティクスを壊しうるので注意（クラスへの参照経由で共有可変状態が漏れる）。

## struct 設計のコツ

- プロパティは原則 `let`、変更が必要な場合に限り `var`。
- 自動生成のメンバーワイズイニシャライザを活用する。
- 振る舞いを変える必要があれば `mutating func` を定義する。

```swift
struct Counter {
    private(set) var value = 0
    mutating func increment() { value += 1 }
}
```

## final class / actor との関係

クラスを使う場合の追加指針:

- 継承予定がないなら `final class` にする（最適化の余地と意図の明示）。
- 並行アクセスから状態を守るなら `actor`（[concurrency.md](./concurrency.md) 参照）。
- SwiftUI のモデルとして使うなら `@Observable final class`（[observation.md](./observation.md) 参照）。

## 値型の落とし穴

| 落とし穴 | 対策 |
| --- | --- |
| 巨大な struct を頻繁にコピー | プロパティを精査、`inout` で渡す、参照型化を検討 |
| struct に class プロパティを持たせて共有可変状態が漏れる | クラスを `let` + 内部不変にする、もしくは値型化 |
| 循環参照したい階層を struct で表す | 木構造が向く。グラフは `class` |
| Protocol 型のコレクションで値が boxing される | ジェネリクスで型を保つ |

## References

- https://docs.swift.org/swift-book/documentation/the-swift-programming-language/classesandstructures/
- https://www.swift.org/documentation/api-design-guidelines/
