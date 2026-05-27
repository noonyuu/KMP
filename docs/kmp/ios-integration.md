# iOS 連携

KMP の `shared` モジュールは **iOS Framework** として書き出され、Xcode プロジェクトに取り込まれる。
Swift / Objective-C 側からは Framework として `import shared` のように使える。

## 統合方式の選択

公式は **Local / Remote** の 2 軸 × **Direct / CocoaPods / SwiftPM** の組み合わせで整理している。

| 方式 | 種類 | 配布形式 | 主な用途 |
| --- | --- | --- | --- |
| Direct integration | Local | Framework (Xcode Build Phase) | モノレポ、CocoaPods 不使用、IDE プラグインのデフォルト |
| SwiftPM (Local) | Local | Local Swift Package | モノレポ、SwiftPM 中心、CocoaPods 不要 |
| CocoaPods (Local) | Local | Local Podspec | モノレポ、CocoaPods を併用したい |
| SwiftPM (Remote) | Remote | XCFramework + Swift Package | shared を独立配布、外部ライブラリ扱い |
| CocoaPods (Remote) | Remote | XCFramework + Podspec | shared を CocoaPods で配布 |

選び方：

- **モバイル単一リポジトリ・反映即時性が欲しい** → Local（Direct が最もシンプル）
- **shared を別リポジトリで管理・ビルド済みを配布したい** → Remote（XCFramework）

## Framework の生成

`shared/build.gradle.kts` 例：

```kotlin
kotlin {
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
}
```

`./gradlew :shared:linkReleaseFrameworkIosArm64` などで Framework を生成。
複数アーキ向けに 1 個にまとめたい場合は **XCFramework** タスクを使う。

## XCFramework

```kotlin
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

kotlin {
    val xcf = XCFramework("shared")
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            xcf.add(this)
        }
    }
}
```

`./gradlew :shared:assembleSharedXCFramework` で生成され、SwiftPM / CocoaPods どちらでも配布可能。

## Swift からの参照

```swift
import shared

let greeting = Greeting().greet()
```

## 型マッピング（Kotlin ↔ Swift / Objective-C）

| Kotlin | Objective-C | Swift |
| --- | --- | --- |
| `class` | `@interface` | `class` |
| `interface` | `@protocol` | `protocol` |
| `enum class` | `@interface` | `class` |
| `String` | `NSString` | `String` |
| `List<T>` | `NSArray<T>` | `[T]` |
| `Map<K,V>` | `NSDictionary` | `[K:V]` |
| `Set<T>` | `NSSet` | `Set<T>` |
| `Unit` | `void` | `Void` |
| `null` | `nil` | `nil` |
| primitive | primitive / `NSNumber` | primitive / `NSNumber` |
| `suspend fun` | `completionHandler:` 付きメソッド | `async` 関数（Swift 5.5+） |

### `Any` のメソッドマッピング

`kotlin.Any` のメソッドは `NSObject` の標準メソッドにマップされる。
これが [ios-interop-pitfalls.md](./ios-interop-pitfalls.md) の名前衝突問題の原因。

| Kotlin | Objective-C | Swift |
| --- | --- | --- |
| `equals()` | `isEqual:` | `isEqual(_:)` |
| `hashCode()` | `hash` | `hash` |
| `toString()` | `description` | `description` |

## Null 安全

| Kotlin | Swift |
| --- | --- |
| `String` | `String`（non-null） |
| `String?` | `String?`（Optional） |

**ジェネリクスの注意**：上限境界がない型パラメータは **Swift では Optional 扱い** になる。

```kotlin
class Sample<T>() { fun myVal(): T = TODO() }
// Swift: func myVal() -> T?

class Sample<T : Any>() { fun myVal(): T = TODO() }
// Swift: func myVal() -> T
```

ジェネリクスを Swift で扱う場合は `T : Any` 制約を付けるのが基本。

## suspend 関数

Kotlin の `suspend` 関数は Swift では `async` / completion handler の両方で呼べる。

```kotlin
// commonMain
suspend fun fetchData(): String { /* ... */ }
```

```swift
// Swift
let result = try await SharedClass().fetchData()

// completion handler 形式（Objective-C / 旧 Swift）
SharedClass().fetchData(completionHandler: { result, error in
    // ...
})
```

注意：

- Completion handler には **常に `NSError*` が追加される**。`@Throws` を付けていなくても `CancellationException` は伝播し得る
- `Flow` は標準では Swift 連携が苦手。`SKIE`（[SKIE](https://skie.touchlab.co/)）や手書きの `FlowWrapper` を併用するのが一般的

## 例外伝播

Kotlin 例外を Swift 側で `try` で受けたい場合は `@Throws` を付ける。

```kotlin
@Throws(IOException::class)
fun readFile(): String { /* ... */ }
```

```swift
// Swift
do {
    let s = try obj.readFile()
} catch {
    // ...
}
```

ルール：

- `@Throws` に **マッチした例外** → `NSError` に変換され Swift で `catch` 可能
- マッチしない例外が Swift 側に到達 → **プログラム強制終了**
- `suspend` で `@Throws` 未指定なら、Swift 側に渡るのは `CancellationException` のみ
- 非 `suspend` で `@Throws` 未指定なら、例外は Swift 側に伝播しない（落ちる）

逆方向（Swift の `throws` → Kotlin の例外）は **未対応**。

## 名前のリネーム：`@ObjCName`

Kotlin 側の宣言を Swift 側で読みやすく改名できる。

```kotlin
@ObjCName(swiftName = "MySwiftArray")
class MyKotlinArray {
    @ObjCName("index")
    fun indexOf(@ObjCName("of") element: String): Int = TODO()
}
```

```swift
let array = MySwiftArray()
let index = array.index(of: "element")
```

## 隠蔽・洗練

| アノテーション | 効果 |
| --- | --- |
| `@HiddenFromObjC` | その宣言を Objective-C / Swift 側から見えなくする |
| `@ShouldRefineInSwift` | `swift_private` 扱いとなり、`__` プレフィックスで隠される。Swift 側で extension などでラップする前提 |

## KDoc → Xcode

Kotlin の KDoc コメントは **生成ヘッダにそのまま転送され、Xcode のオートコンプリートで表示される**。

```kotlin
/**
 * Prints the sum of the arguments.
 * Properly handles the case when the sum doesn't fit in 32-bit integer.
 */
fun printSum(a: Int, b: Int) = println(a.toLong() + b)
```

## References

- iOS integration overview: <https://kotlinlang.org/docs/multiplatform-ios-integration-overview.html>
- Objective-C and Swift interop: <https://kotlinlang.org/docs/native-objc-interop.html>
- Project structure: <https://kotlinlang.org/docs/multiplatform-discover-project.html>
