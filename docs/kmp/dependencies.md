# 依存関係の追加

KMP プロジェクトでは、依存関係を **ソースセット単位** で `build.gradle.kts` に宣言する。
`commonMain` に置けば全ターゲットに自動で適切なバリアントが配られる。

## ソースセット単位の宣言

```kotlin
kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.10.0")
        }
        iosMain.dependencies {
            // iOS 固有依存
        }
    }
}
```

## ポイント

| 項目 | 内容 |
| --- | --- |
| 共通アーティファクト名を使う | `kotlinx-coroutines-core` のように **共通名** を指定。`-iosx64` などの個別バリアントは Gradle が自動選択 |
| `commonMain` に書ける条件 | そのライブラリが **マルチプラットフォーム対応** していること |
| 標準ライブラリ | 自動追加。`kotlin-multiplatform` プラグインのバージョンに合うものが使われる |
| テスト | `commonTest` に `kotlin("test")` を書くだけで、各プラットフォーム用のテスト基盤が揃う |

## KMP 対応ライブラリ

`commonMain` に置けるのは KMP（マルチプラットフォーム）対応ライブラリだけ。代表例：

| カテゴリ | ライブラリ |
| --- | --- |
| 非同期 | `kotlinx-coroutines-core` |
| シリアライズ | `kotlinx-serialization-json` |
| HTTP | `Ktor Client` |
| DB | `SQLDelight`, `Room`（KMP 対応版） |
| 日時 | `kotlinx-datetime` |
| 設定 | `multiplatform-settings` |
| DI | `Koin`, `Kotlin-Inject` |
| Image Loading | `Coil 3`（Compose Multiplatform） |

判定基準：

- Maven Central などに `-jvm`, `-iosarm64`, `-iossimulatorarm64` の各バリアントが publish されているか
- ライブラリ側が `kotlin("multiplatform")` プラグインを使っているか
- 公式ドキュメントに iOS / Native サポートが書かれているか

## ターゲット別の依存

ターゲット固有 SDK を使うものはプラットフォームソースセットに置く。

```kotlin
sourceSets {
    androidMain.dependencies {
        implementation("androidx.core:core-ktx:1.13.1")
    }
    iosMain.dependencies {
        // iOS 固有 Kotlin/Native ライブラリ
    }
}
```

iOS の **CocoaPods 経由のネイティブライブラリ** を使いたい場合は `kotlin.cocoapods` プラグインを併用する：

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
}

kotlin {
    cocoapods {
        summary = "Shared module"
        homepage = "https://example.com"
        ios.deploymentTarget = "15.0"
        pod("FirebaseAuth") { version = "10.0.0" }
    }
}
```

## プロジェクト依存

別の KMP モジュールを依存に追加：

```kotlin
commonMain.dependencies {
    implementation(project(":other-shared-module"))
}
```

## Version Catalog（推奨）

`gradle/libs.versions.toml` で依存バージョンを一元管理する。

```toml
[versions]
kotlin = "2.1.0"
coroutines = "1.10.2"
ktor = "3.0.3"
serialization = "1.7.3"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

`build.gradle.kts` から：

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
```

## ハマりポイント

| 症状 | 原因と対処 |
| --- | --- |
| `commonMain` で `Unresolved reference` | ライブラリが KMP 非対応、または対応ターゲットが足りない。プラットフォーム別ソースセットに移すか別ライブラリを選ぶ |
| `expect/actual` でしか動かないようなコードを `commonMain` に書きたくなった | だいたいは「KMP 対応していないライブラリを common で使おうとしている」サイン。インターフェース化して `androidMain` / `iosMain` の実装に委譲する |
| Kotlin / KGP / AGP の組み合わせビルドエラー | バージョン互換マトリクス（Kotlin 公式の Compatibility ページ）を確認。Version Catalog で揃える |
| iOS Framework に依存ライブラリのクラスが含まれない | デフォルトでは依存は **export されない**。Swift から直接見せたい型を含むモジュールは `framework { export(libs.foo) }` で明示 export |
| `transitiveExport` が必要なケース | export したライブラリがさらに別の API 型を返す場合に必要。Gradle KMP の `Framework.transitiveExport = true` |
| 重複した `kotlinx-coroutines-core-jvm` で `DuplicateClass` | `commonMain` と `androidMain` 両方に書いている可能性。共通アーティファクトを `commonMain` だけに書く |

## Framework への export

Swift 側で型を直接参照したいライブラリは `binaries.framework { export(...) }` する。

```kotlin
kotlin {
    iosArm64 {
        binaries.framework {
            baseName = "shared"
            export("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
        }
    }
}
```

`api(...)` 構成で依存にも入っていることが前提（`implementation` だと export 不可）。

## References

- Adding dependencies on multiplatform libraries: <https://kotlinlang.org/docs/multiplatform-add-dependencies.html>
- Hierarchical project structure: <https://kotlinlang.org/docs/multiplatform-hierarchy.html>
- iOS integration overview: <https://kotlinlang.org/docs/multiplatform-ios-integration-overview.html>
