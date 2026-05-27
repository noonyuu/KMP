# Observation フレームワーク

iOS 17 / macOS 14 以降で導入された、参照型モデルの変更追跡を行うフレームワーク。`@Observable` マクロが中核。SwiftUI と組み合わせると、View が「実際にアクセスしたプロパティ」だけで再評価されるため、`ObservableObject` + `@Published` よりも細粒度・低オーバーヘッド。

## 必要環境

| プラットフォーム | 最小 OS |
| --- | --- |
| iOS / iPadOS | 17.0+ |
| macOS | 14.0+ |
| tvOS | 17.0+ |
| watchOS | 10.0+ |
| visionOS | 1.0+ |

Swift 5.9 以降のマクロ機能を利用する。

## @Observable マクロ

```swift
import Observation

@Observable
final class FoodTruckModel {
    var orders: [Order] = []
    var donuts: [Donut] = Donut.all
}
```

このマクロは以下を自動生成する:

- `Observable` プロトコル準拠
- 各 stored property の getter/setter にトラッキング（`access(keyPath:)` / `withMutation(keyPath:)`）を挿入
- 内部の `ObservationRegistrar`

利用側の View では特別なプロパティラッパー不要で参照できる。

```swift
struct DonutMenu: View {
    let model: FoodTruckModel        // ラッパー不要

    var body: some View {
        List {
            ForEach(model.donuts) { donut in
                Text(donut.name)     // 読み取りがトラッキングされる
            }
        }
    }
}
```

### 非対象プロパティ

トラッキングしたくない stored property には `@ObservationIgnored` を付ける。

```swift
@Observable
final class Model {
    var visible: Int = 0
    @ObservationIgnored var cache: [String: Data] = [:]
}
```

### 計算プロパティ

stored property から導かれる計算プロパティは、依存する stored property を通じて自動的にトラッキングされる。stored 以外から導かれる場合は手動で `access` / `withMutation` を呼ぶ。

## withObservationTracking

SwiftUI に依らず、任意のクロージャでアクセスされたプロパティを 1 回だけ追跡する低レベル API。

```swift
withObservationTracking {
    render(model.donuts)
} onChange: {
    // 次のいずれかのプロパティが変化した「最初の」タイミングで一度だけ呼ばれる
    scheduleRerender()
}
```

- `onChange` は **最初の変更時に一度だけ** 呼ばれる（継続観測したい場合は再度 `withObservationTracking` を呼び直す）。
- `onChange` はプロパティが変更される直前（willSet 相当）に呼ばれる。

## ObservationRegistrar

低レベル API。マクロ展開で自動的に使われる。手動で `Observable` を実装する稀なケースで利用する。

```swift
final class Manual: Observable {
    private let _$observationRegistrar = ObservationRegistrar()

    private var _value: Int = 0
    var value: Int {
        get {
            _$observationRegistrar.access(self, keyPath: \.value)
            return _value
        }
        set {
            _$observationRegistrar.withMutation(of: self, keyPath: \.value) {
                _value = newValue
            }
        }
    }
}
```

通常は `@Observable` を使えばよい。

## ObservableObject からの移行

| Before（`Combine.ObservableObject`） | After（`Observation.@Observable`） |
| --- | --- |
| `class M: ObservableObject` | `@Observable final class M` |
| `@Published var x` | `var x`（マクロが自動追跡） |
| `@StateObject var m = M()` | `@State private var m = M()` |
| `@ObservedObject var m: M` | `var m: M`（パラメータ）または `@Bindable var m: M` |
| `@EnvironmentObject var m: M` | `@Environment(M.self) var m` |
| `.environmentObject(m)` | `.environment(m)` |

### Before

```swift
final class Counter: ObservableObject {
    @Published var value: Int = 0
}

struct CounterView: View {
    @StateObject var counter = Counter()
    var body: some View {
        Stepper("\(counter.value)", value: $counter.value)
    }
}
```

### After

```swift
@Observable
final class Counter {
    var value: Int = 0
}

struct CounterView: View {
    @State private var counter = Counter()
    var body: some View {
        @Bindable var counter = counter
        Stepper("\(counter.value)", value: $counter.value)
    }
}
```

ローカルで `@Bindable var counter = counter` と書くことで、`$counter.value` のような Binding が得られる。

## パフォーマンス特性

- View body 評価中にアクセスされたプロパティだけが追跡対象になる。
- 未アクセスのプロパティが変更されても再評価されない（`@Published` は型全体が変更通知だった）。
- ループや条件分岐で動的にアクセス対象が変わっても、その時々で正しくトラッキングされる。

## ベストプラクティス

- モデルクラスは `final` にする。
- View に直接アクセスされる必要がない内部状態は `@ObservationIgnored` を付ける。
- 共有モデルは `@Environment(MyModel.self)` で配布し、書き込みが必要な View 側で `@Bindable` を使う。
- 値型で済むローカル UI 状態は `@State`（Observation ではなく SwiftUI 側）を使う。

## References

- https://developer.apple.com/documentation/observation
- https://developer.apple.com/documentation/swiftui/migrating-from-the-observable-object-protocol-to-the-observable-macro
- https://developer.apple.com/videos/play/wwdc2023/10149/
