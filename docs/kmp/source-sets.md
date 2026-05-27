# ソースセットと階層構造

KMP プロジェクトのコードは **ソースセット (source set)** という単位で管理される。
ソースセットは「どのターゲットに対してコンパイルされるか」と「依存関係・コンパイラオプション」を持つコードの集合。

## 主なソースセット

| ソースセット | 説明 |
| --- | --- |
| `commonMain` | 宣言された全ターゲットで共有されるコード |
| `commonTest` | `commonMain` のテスト |
| `androidMain` | Android ターゲット専用 |
| `iosMain` | iOS 系ターゲット（`iosArm64` / `iosSimulatorArm64` / `iosX64`）で共有される中間ソースセット |
| `iosArm64Main` 等 | 特定のターゲット専用 |
| `appleMain` | Apple 系（iOS / macOS / tvOS / watchOS）で共有される中間ソースセット |
| `nativeMain` | Kotlin/Native の全ターゲットで共有される中間ソースセット |
| `jvmMain` / `jsMain` | JVM / JS ターゲット専用 |

`commonMain` で使えるのは Kotlin 標準ライブラリ + マルチプラットフォーム対応ライブラリのみ。
プラットフォーム固有 API（`java.io.File`, `UIKit` など）は対応するソースセットでしか使えない。

## ターゲット (target)

ターゲットは Kotlin がコードをコンパイルする対象プラットフォーム。`kotlin {}` ブロックで宣言する。

```kotlin
kotlin {
    jvm()
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    js()
}
```

## ディレクトリ構造

```text
shared/src/
 ├── commonMain/kotlin/...
 ├── commonTest/kotlin/...
 ├── androidMain/kotlin/...
 ├── androidUnitTest/kotlin/...
 ├── iosMain/kotlin/...
 ├── iosArm64Main/kotlin/...
 └── iosSimulatorArm64Main/kotlin/...
```

## コンパイルの仕組み

特定ターゲットへコンパイルする際、Kotlin はそのターゲットに紐づくすべてのソースセットを集めて 1 つのバイナリにまとめる。

| ターゲット | コンパイル対象 |
| --- | --- |
| JVM | `commonMain` + `jvmMain` |
| Android | `commonMain` + `androidMain` |
| iOS (arm64) | `commonMain` + `appleMain` + `iosMain` + `iosArm64Main` |

可視性は **片方向**：
- プラットフォームソースセットは `commonMain` のコードを参照できる
- 逆 (`commonMain` がプラットフォームソースセットを参照) は **不可**

## デフォルト階層テンプレート (Default Hierarchy Template)

Kotlin 1.9.20 以降、Kotlin Gradle Plugin が **default hierarchy template** を提供しており、
ターゲットを宣言するだけで `iosMain` / `appleMain` / `nativeMain` 等の中間ソースセットが自動的に作られる。

```kotlin
kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    // iosMain / appleMain / nativeMain などが自動生成される
}
```

宣言していないターゲット用のソースセット（例：`watchosMain`）は無視される。

### 型安全なアクセサ

デフォルトテンプレートを使うと、`by getting` / `by creating` 不要で型安全にアクセスできる。

```kotlin
kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        iosMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
    }
}
```

### 階層イメージ

```text
commonMain
 ├── jvmMain
 ├── androidMain
 └── nativeMain
      └── appleMain
           ├── iosMain
           │    ├── iosArm64Main
           │    ├── iosSimulatorArm64Main
           │    └── iosX64Main
           ├── macosMain
           ├── tvosMain
           └── watchosMain
```

## 追加の中間ソースセット

デフォルトテンプレートに含まれない共有が必要な場合は、`applyDefaultHierarchyTemplate()` を明示呼び出ししてから独自に追加する。

```kotlin
kotlin {
    jvm()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val jvmAndMacos by creating {
            dependsOn(commonMain.get())
        }
        macosArm64Main.get().dependsOn(jvmAndMacos)
        jvmMain.get().dependsOn(jvmAndMacos)
    }
}
```

## ソースセット配置の判断フロー

```text
そのコードはプラットフォーム固有 API を使うか？
 ├─ No → commonMain
 └─ Yes
     ├─ Apple 系（iOS / macOS など）共通で同じ実装で動く？
     │   ├─ Yes → appleMain
     │   └─ No
     │       ├─ iOS だけで使う？ → iosMain
     │       ├─ Android だけ？ → androidMain
     │       └─ 特定ターゲットのみ？ → iosArm64Main など
     └─ Kotlin/Native 全般？ → nativeMain
```

## ベストプラクティス

- まずは `commonMain` に書こうとする。プラットフォーム API が必要になったら下に降ろす
- 共有可能な範囲はできる限り中間ソースセット（`iosMain`, `appleMain`）に置く
- 個別ターゲットのソースセット (`iosArm64Main` 等) に直接置くのは最終手段
- デフォルト階層テンプレートに乗れるようターゲット名は標準的なもの (`androidTarget()`, `iosArm64()` 等) を使う

## References

- Project structure: <https://kotlinlang.org/docs/multiplatform-discover-project.html>
- Hierarchical project structure: <https://kotlinlang.org/docs/multiplatform-hierarchy.html>
