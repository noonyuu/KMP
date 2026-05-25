# KMPについて

## 比較

### Flutter

UIも含めて完全共通化することを前提としたフレームワーク

### KMP

UIは各プラットフォームで作成し、ロジックのみを共有する

## Compose Multiplatform

Compose UI を複数プラットフォームで使用する
思想はFlutterだがKotlin + Composeベース

## なぜKMPを使う

iOS(Swift)とAndroid(kotlin)で全く同じ仕様のロジックを書く必要があった
KMPはKotlinを使用し特定のプラットフォーム（iOSやAndroidなど）に依存しない共通コードを書くための技術

## 仕組み

`settings.gradle.kts`で2つのGradleモジュールが登録されている

```text
prc/
 ├── shared/      ← ロジック共通化モジュール（KMPライブラリ）
 ├── composeApp/  ← Android アプリ（Compose Multiplatform）
 └── prcApp/      ← iOS アプリ（ネイティブ SwiftUI / Xcode プロジェクト）
```

### `common/Platform.kt`

普通のOOPのインターフェース

```kotlin
interface Platform {
    val name: String
}
```

expect 関数（各プラットフォームが本体を用意する

```kotlin
expect fun getPlatform(): Platform
```

### `common/Greeting.kt`

Greeting がやっているのは「getPlatform() を呼んで結果を使う」共通ロジック

```kotlin
class Greeting {
    private val platform = getPlatform()  // 実態を「もらって」使うだけ
    fun greet(): String = "Hello, ${platform.name}!"
}
```

### `android/ios の Platform`

```kotlin
class AndroidPlatform : Platform {                       // インターフェース実装
    override val name: String = "Android ${Build.VERSION.SDK_INT}"   // override
}
```

expect の充足（actual）

```kotlin
actual fun getPlatform(): Platform = AndroidPlatform()
```

### 流れ

```text
commonMain ─┬─ interface Platform { name } ← 形だけ定義
            ├─ expect getPlatform() ← 「誰かが用意してね」の約束
            └─ Greeting { getPlatform().name を使う } ← 共通ロジック（消費者）
                                  │
          ┌───────────────────────┴───────────────────────┐
          androidMain                                   iosMain
          AndroidPlatform : Platform (override)         IOSPlatform : Platform (override)
          actual getPlatform() = AndroidPlatform()      actual getPlatform() = IOSPlatform()
```
