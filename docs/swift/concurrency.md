# Swift Concurrency

Swift 5.5 以降の言語組み込み並行性モデル。`async`/`await`、`Task`、`actor`、`Sendable` を中核とする。詳細は [The Swift Programming Language: Concurrency](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/concurrency/) を参照。

## async / await

非同期関数は `async` 修飾子を付け、呼び出し側で `await` する。

```swift
func fetchPhotoNames() async throws -> [String] {
    let data = try await URLSession.shared.data(from: photoURL)
    return parseNames(from: data.0)
}

let names = try await fetchPhotoNames()
```

- `async` は「中断（suspend）し得る」ことを示すマーカ。スレッドをブロックしない。
- `await` は中断点（suspension point）を明示する。`await` のない箇所は中断しない。
- `async` 関数は `async` 文脈、または `Task { ... }` の中からのみ呼べる。
- `throws` と組み合わせるときの順序は `async throws`、呼び出し側は `try await`。

### async プロパティ・初期化子

読み取り専用の計算プロパティは `async` にできる。`init` も `async` 可。

```swift
extension Photo {
    var thumbnail: UIImage { get async throws { ... } }
}
```

## 非同期シーケンス（AsyncSequence）

`for await ... in` で非同期に要素を受け取る。

```swift
for try await line in url.lines {
    print(line)
}
```

`AsyncSequence` プロトコルは標準ライブラリの非同期反復インターフェース。`AsyncStream` で自作できる。

## 並列に呼ぶ（async let）

互いに独立な非同期処理を並列実行する。

```swift
async let first  = downloadPhoto(named: names[0])
async let second = downloadPhoto(named: names[1])
async let third  = downloadPhoto(named: names[2])

let photos = await [first, second, third]
```

- `async let` で束ねた処理は宣言時点で開始される。
- `await` するまで結果は使えないが、待っている間に他の処理は進む。

## Task

非同期処理の単位。スコープに紐付く構造化された Task と、独立した非構造化 Task がある。

### 構造化された並行性（Structured Concurrency）

`async let`、`TaskGroup`、子 Task は親スコープに従属し、スコープ終了時に自動的に待たれる/キャンセルされる。

```swift
try await withThrowingTaskGroup(of: Data.self) { group in
    for name in photoNames {
        group.addTask { try await downloadPhoto(named: name) }
    }
    var photos: [Data] = []
    for try await photo in group {
        photos.append(photo)
    }
    return photos
}
```

種類:

| API | 用途 |
| --- | --- |
| `withTaskGroup(of:)` | 同型の子 Task を動的に追加・結果集約 |
| `withThrowingTaskGroup(of:)` | `throws` 版 |
| `withDiscardingTaskGroup` | 結果不要・大量の子 Task |

### 非構造化 Task

```swift
let handle = Task {
    return try await fetchPhotoNames()
}
let names = try await handle.value
```

- `Task { ... }` は現在のアクター・優先度を継承する。
- `Task.detached { ... }` は継承せず独立する（使用は最小限に）。
- 戻り値が不要なら `Task { ... }` を「投げっぱなし」にもできるが、所有者を明確に。

## キャンセル（Cancellation）

Swift Concurrency は協調的キャンセル方式。

```swift
try Task.checkCancellation()        // キャンセル時に CancellationError を throw
if Task.isCancelled { return }      // キャンセル時に早期 return
try await Task.sleep(for: .seconds(1)) // sleep はキャンセルで throw
```

- 親 Task がキャンセルされると子 Task にも伝播する。
- ネットワーク・I/O 系の標準 API はキャンセルに応答する。自前の長時間処理ではループ内で `checkCancellation` を呼ぶ。
- `withTaskCancellationHandler` で外部リソースの後始末を登録できる。

## Actor

並行アクセスから状態を保護する参照型。`actor` 内部の可変状態は actor 自身を通してしかアクセスできない（actor 隔離）。

```swift
actor TemperatureLogger {
    let label: String
    var measurements: [Int] = []
    var max: Int

    init(label: String, measurement: Int) {
        self.label = label
        self.measurements.append(measurement)
        self.max = measurement
    }

    func update(with measurement: Int) {
        measurements.append(measurement)
        if measurement > max { max = measurement }
    }
}

let logger = TemperatureLogger(label: "Outdoors", measurement: 25)
await logger.update(with: 27)
print(await logger.max)
```

- 外部から actor のメソッド/プロパティにアクセスする際は `await` が必要。
- actor 内部（self を介する）アクセスは `await` 不要。
- 不変なプロパティ（`let`）は actor 外部から `await` なしでアクセス可。

### グローバルアクター / @MainActor

特定のアクターでの実行を型・関数に強制する属性。`@MainActor` は UI 操作のためメインスレッドに固定する。

```swift
@MainActor
final class ViewModel {
    var title: String = ""
}

@MainActor
func updateUI() { ... }

Task { @MainActor in
    label.text = "..."
}
```

## Sendable

Task 境界・actor 境界をまたいで安全に値を渡せることを示すマーカープロトコル。

- 値型かつ全プロパティが `Sendable` な struct / enum は自動的に `Sendable` 適合可。
- final class で全プロパティが immutable な値型なら明示的に `Sendable` 宣言できる。
- 可変参照を含むクラスは `@unchecked Sendable` にして自前で同期する（多用しない）。
- クロージャを境界越しに渡す場合は `@Sendable` クロージャである必要がある。

```swift
struct Photo: Sendable {
    let url: URL
    let bytes: Data
}

func process(_ work: @Sendable @escaping () async -> Void) { ... }
```

Strict Concurrency Checking を有効にすると、Sendable 違反がコンパイル時に検出される。

## 落とし穴と推奨

| アンチパターン | 推奨 |
| --- | --- |
| `DispatchQueue.main.async { ... }` で UI 更新 | `@MainActor` で関数を隔離する |
| `Task.detached` を多用 | デフォルトは `Task { ... }`、必要な場合のみ detached |
| `Thread.sleep` でブロック | `try await Task.sleep(for:)` |
| キャンセル無視のループ | `try Task.checkCancellation()` を入れる |
| `await` の前後で前提が変わる前提を置く | `await` を挟むと他処理が走り得る（再入を考慮） |

## References

- https://docs.swift.org/swift-book/documentation/the-swift-programming-language/concurrency/
- https://developer.apple.com/documentation/swift/concurrency
