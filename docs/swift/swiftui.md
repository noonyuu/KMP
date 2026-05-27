# SwiftUI: View 合成と状態管理

SwiftUI の View 合成、状態とデータフローのプロパティラッパー、`@Observable` 連携をまとめる。詳細は [State and data flow](https://developer.apple.com/documentation/swiftui/state-and-data-flow) および [Managing model data](https://developer.apple.com/documentation/swiftui/managing-model-data-in-your-app) を参照。

## View 合成の原則

- View は値型（`struct`）であり、`View` プロトコルに準拠する。
- 単一の計算プロパティ `body` が UI を宣言する。
- View は「現在の状態のスナップショット」を返す関数として扱う。SwiftUI が状態の差分から再評価する。
- 小さい View に分割して合成する。`@ViewBuilder` 関数・`some View` を返すメソッドで再利用する。
- modifier は新しい View を返す。順序が意味を持つ（`.padding().background(.red)` と `.background(.red).padding()` は別物）。

```swift
struct Greeting: View {
    let name: String
    var body: some View {
        VStack(spacing: 8) {
            Text("Hello, \(name)")
                .font(.title)
            Divider()
            footer
        }
        .padding()
    }

    @ViewBuilder
    private var footer: some View {
        Text("Welcome").foregroundStyle(.secondary)
    }
}
```

## 状態管理プロパティラッパー一覧

| ラッパー | 対象 | 所有 | 用途 |
| --- | --- | --- | --- |
| `@State` | 値型 / `@Observable` 参照型 | View が所有 | View ローカルの真実の出所 |
| `@Binding` | 別の場所が持つ値への参照 | しない | 親 View の状態を子 View で読み書き |
| `@Environment` | システム値 or `@Observable` オブジェクト | しない | 階層全体に注入された値を読む |
| `@Bindable` | `@Observable` 参照型 | しない | `@Observable` オブジェクトから Binding を作る |

`ObservableObject` を使っていた頃の `@StateObject` / `@ObservedObject` / `@EnvironmentObject` は、`@Observable` 時代には上記 4 つに置き換わる（詳細は [observation.md](./observation.md)）。

## @State

View 自身が所有するローカルな真実の出所。値型に対して使うのが基本。`private` 推奨。

```swift
struct CounterView: View {
    @State private var count: Int = 0

    var body: some View {
        Button("Count: \(count)") { count += 1 }
    }
}
```

`@Observable` クラスを View が所有する場合も `@State` を使う。

```swift
@Observable
final class CartModel { var items: [Item] = [] }

struct RootView: View {
    @State private var cart = CartModel()
    var body: some View { CartView(cart: cart) }
}
```

`init` 時に一度だけ作られ、View の再生成に耐える。

## @Binding

別の場所が持つ状態への参照。`$` プレフィックスで `Binding` を取り出す。

```swift
struct ToggleRow: View {
    @Binding var isOn: Bool
    var body: some View {
        Toggle("Enable", isOn: $isOn)
    }
}

struct Parent: View {
    @State private var enabled = false
    var body: some View {
        ToggleRow(isOn: $enabled)
    }
}
```

子 View に渡したいが、子に所有させたくないときに使う。

## @Environment

ビュー階層に注入された値を読む。SwiftUI が提供する組み込み環境値（`\.colorScheme`、`\.locale` など）と、`@Observable` オブジェクトの両方に使える。

```swift
// 組み込み環境値
struct Themed: View {
    @Environment(\.colorScheme) private var scheme
    var body: some View {
        Text(scheme == .dark ? "Dark" : "Light")
    }
}
```

```swift
// @Observable オブジェクト
@Observable
final class AppModel { var user: String = "Guest" }

@main
struct App: SwiftUI.App {
    @State private var model = AppModel()
    var body: some Scene {
        WindowGroup {
            ContentView().environment(model)
        }
    }
}

struct ContentView: View {
    @Environment(AppModel.self) private var model
    var body: some View { Text(model.user) }
}
```

`@Environment(MyType.self)` は対応するオブジェクトが注入されていないと実行時にクラッシュする。任意にしたい場合は `@Environment(MyType?.self)` を使う。

## @Bindable

`@Observable` 参照型のプロパティを `Binding` として取り出すために使う。

```swift
@Observable
final class Profile {
    var name: String = ""
}

struct EditView: View {
    @Bindable var profile: Profile
    var body: some View {
        TextField("Name", text: $profile.name)
    }
}
```

`@Environment` で受け取ったオブジェクトから Binding が欲しいときは、`body` 内でローカル束縛する。

```swift
struct SettingsView: View {
    @Environment(Profile.self) private var profile
    var body: some View {
        @Bindable var profile = profile
        TextField("Name", text: $profile.name)
    }
}
```

## 使い分けの指針

| 状況 | 使うべきもの |
| --- | --- |
| View ローカルの値型 | `@State` |
| View 自身が所有する `@Observable` モデル | `@State` |
| 親が持つ値を子で読み書き | `@Binding` |
| 階層を超えた共有モデル | `.environment(model)` + `@Environment(M.self)` |
| `@Observable` モデルから Binding を作る | `@Bindable` |
| システム環境値（colorScheme, locale 等） | `@Environment(\.keyPath)` |
| 単に値を子に渡すだけ（変更不要） | 通常のプロパティ |

## 識別とパフォーマンス

- `ForEach` には `id:` で安定な識別子を渡す（`Identifiable` の `id` を使うのが基本）。
- `equatable()` modifier や `Equatable` 準拠で再評価を抑制できる。
- `@Observable` を使えば、View が実際に読んだプロパティのみが再評価のトリガーになる。

## References

- https://developer.apple.com/documentation/swiftui/state-and-data-flow
- https://developer.apple.com/documentation/swiftui/managing-model-data-in-your-app
- https://developer.apple.com/documentation/swiftui/model-data
- https://developer.apple.com/documentation/swiftui/migrating-from-the-observable-object-protocol-to-the-observable-macro
