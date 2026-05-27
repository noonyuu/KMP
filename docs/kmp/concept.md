# KMPについて

## 概要

**Kotlin Multiplatform (KMP)** は、Kotlin で書いた共通コードを Android / iOS / JVM / JS / Native などの複数プラットフォームに対してコンパイルするための技術。
ロジック層を共有し、UI 層は各プラットフォームに任せるのが基本戦略。

## 比較

| 観点 | Flutter | Kotlin Multiplatform |
| --- | --- | --- |
| 言語 | Dart | Kotlin |
| UI | 完全共通化（Flutter Widget） | 各プラットフォームでネイティブ実装（または Compose Multiplatform） |
| 共有範囲 | UI 含めすべて | ロジック中心、UI はオプション |
| ランタイム | Flutter Engine（Skia/Impeller） | 各プラットフォームのネイティブ（JVM / Kotlin/Native） |
| 思想 | "Write once, run anywhere" | "Share what you want" |

## Compose Multiplatform

Compose UI を複数プラットフォーム（Android / iOS / Desktop / Web）で使うための JetBrains 製フレームワーク。
思想は Flutter に近いが、Kotlin + Jetpack Compose ベース。
KMP の中で **UI まで共有したい場合に追加で採用する** 位置づけ。

## なぜ KMP を使うのか

- iOS (Swift) と Android (Kotlin) で同一仕様のロジックを二重管理したくない
- ネイティブの UI / プラットフォーム API は活かしたい
- 後から段階的に導入できる（共有モジュールから少しずつ）

KMP は Kotlin を使い、特定プラットフォームに依存しない共通コードを書くための技術。

## 仕組み

### モジュール構成

典型的な KMP プロジェクト（モバイル）は Gradle マルチモジュール構成。

```text
project-root/
 ├── shared/      ← ロジック共通化モジュール（KMP ライブラリ）
 ├── androidApp/  ← Android アプリ（Compose もしくは View System）
 └── iosApp/      ← iOS アプリ（SwiftUI / UIKit / Xcode プロジェクト）
```

`settings.gradle.kts` にこれらの Gradle モジュールを登録する。
iOS アプリは Xcode プロジェクトとして独立しており、`shared` モジュールから生成された Framework を取り込む（[ios-integration.md](./ios-integration.md)）。

### ソースセット

`shared` モジュール内は `commonMain` / `androidMain` / `iosMain` といったソースセットに分かれる（詳細は [source-sets.md](./source-sets.md)）。

```text
shared/src/
 ├── commonMain/   ← すべてのターゲットで共有されるコード
 ├── androidMain/  ← Android 専用
 └── iosMain/      ← iOS 専用（appleMain などの中間ソースセットもあり）
```

### `expect` / `actual` の基本

`commonMain` から特定プラットフォーム API を抽象的に呼び出すための仕組み。

```kotlin
// commonMain
interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
```

`commonMain` 内で使う共通ロジック：

```kotlin
// commonMain
class Greeting {
    private val platform = getPlatform()
    fun greet(): String = "Hello, ${platform.name}!"
}
```

各プラットフォームで `actual` を提供する：

```kotlin
// androidMain
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}
actual fun getPlatform(): Platform = AndroidPlatform()
```

```kotlin
// iosMain
class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}
actual fun getPlatform(): Platform = IOSPlatform()
```

### 流れ

```text
commonMain ─┬─ interface Platform { name }          ← 形だけ定義
            ├─ expect getPlatform()                 ← 各プラットフォームが用意する約束
            └─ Greeting { getPlatform().name を使う } ← 共通ロジック
                          │
          ┌───────────────┴────────────────┐
          androidMain                     iosMain
          AndroidPlatform : Platform      IOSPlatform : Platform
          actual getPlatform()            actual getPlatform()
```

### `expect` / `actual` の使い分け

`expect` / `actual` は最も単純な方法だが、**インターフェース + DI（コンストラクタ注入や DI フレームワーク）で代替できるケースが多い**。
クラス単位の `expect` は Beta 扱いであり、公式も「可能なら標準的な言語機能（インターフェース + ファクトリ）を使え」と推奨している。
詳しくは [expect-actual.md](./expect-actual.md) を参照。

## References

- Kotlin Multiplatform: <https://kotlinlang.org/docs/multiplatform.html>
- Project structure: <https://kotlinlang.org/docs/multiplatform-discover-project.html>
- Expect/actual declarations: <https://kotlinlang.org/docs/multiplatform-expect-actual.html>
