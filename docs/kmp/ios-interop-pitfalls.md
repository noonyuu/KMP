# iOS 連携でのハマりポイント

KMP の `shared` モジュールが生成する Objective-C / Swift ヘッダで起きやすい問題をまとめる。
公式型マッピングについては [ios-integration.md](./ios-integration.md) を参照。

## 1. `description` プロパティの名前衝突

### 概要

`NSObject`（およびそのサブクラス）には Objective-C ランタイムが昔から持っている **`description`** というプロパティがある。
これは Java の `toString()` 相当で、デバッガやログ出力でオブジェクトを文字列表現するために使われる。
Swift の `CustomStringConvertible` プロトコルの `description` もこれと地続き。

### 例

```kotlin
data class Challenge(
    val id: String,
    val description: String,  // ← これが NSObject.description と衝突する
)
```

KMP は Kotlin のクラスを Objective-C / Swift から使えるようにヘッダ (`.h`) を自動生成する。
このとき `Challenge` は `NSObject` を継承する形になり、Kotlin の `description` プロパティが Objective-C の `NSObject.description` と同じ名前のメンバとしてぶつかる。

### 問題点

名前が衝突したときに、コンパイルで止まらず挙動がわかりにくい形で壊れることがある。

1. **オーバーライド扱いになって型が合わない**
   `NSObject.description` は `NSString`（非 null の `String`）を返す契約。
   Kotlin 側の `description` が nullable だったり、KMP のバージョン次第で生成ヘッダの整合が崩れる。
   Swift 側で `challenge.description` と書くとフィールドの値ではなく `"Challenge(id=...,description=...)"` のような `toString()` の結果が返ってくる、といった混乱が起きる
2. **ビルドエラーになるケース**
   KMP のバージョン設定によっては
   `Kotlin property 'description' clashes with Objective-C method` のような警告 / エラーが出る

### 対処

- プロパティ名を別名にする（`detail`, `summary`, `body` など）
- どうしても `description` を使いたい場合は Kotlin 側で `@ObjCName` で別名を付ける：

```kotlin
data class Challenge(
    val id: String,
    @ObjCName("descriptionText")
    val description: String,
)
```

## 2. NSObject 由来の予約的メンバ

`description` 以外にも `NSObject` 由来で衝突しうるメンバが多数ある。
Kotlin 側でこれらの名前を **プロパティ・メソッド名にしない** のが安全。

- `hash`
- `description`
- `debugDescription`
- `isEqual`
- `copy`
- `init`
- `class`
- `mutableCopy`
- `superclass`
- `dealloc`
- `retain` / `release` / `autorelease`（ARC 関連）

`equals()` / `hashCode()` / `toString()` は Kotlin の `Any` 由来で
それぞれ `isEqual:` / `hash` / `description` に自動マップされる
（[ios-integration.md](./ios-integration.md) の型マッピング表参照）。
これらを `data class` で自動生成させること自体は問題ない。問題は **同名の独自プロパティを生やしてしまう** ケース。

## 3. ジェネリクスの暗黙 Optional 化

```kotlin
class Box<T>(val value: T)
```

Swift 側ではこの `value` が **`T?`（Optional）** として見える。
Kotlin 側で `T : Any` の上限境界を付ければ non-null になる：

```kotlin
class Box<T : Any>(val value: T)
```

## 4. `sealed class` / `enum class` の網羅性

Kotlin の `sealed class` は Swift 側では「複数の class が `protocol` を実装している」状態として見える。
Swift 側で `switch` しても網羅性チェックが効かない。

対策：

- SKIE などのツールで `enum` 風に変換する
- Swift 側に **専用のラッパー（discriminator + 個別プロパティ）** を作る

## 5. `Flow` / `StateFlow` の連携

`kotlinx.coroutines.flow.Flow` は **そのまま Swift で `for await` などにはならない**。
Swift で扱いたい場合：

- SKIE を使う（推奨）
- 手書きで `FlowWrapper` / `subscribe(onEach, onCompletion)` のような中間 API を用意する

## 6. デフォルト引数

Kotlin 側のデフォルト引数値は Objective-C / Swift には伝播しない。
Swift からは **すべての引数を明示的に渡す必要がある**。

対策：

- オーバーロードで複数の関数を提供する
- ファクトリ関数を用意する

## References

- Objective-C and Swift interop: <https://kotlinlang.org/docs/native-objc-interop.html>
- iOS integration overview: <https://kotlinlang.org/docs/multiplatform-ios-integration-overview.html>
