# expect / actual 宣言

`commonMain` から「プラットフォーム固有 API を抽象的に呼ぶ」ための仕組み。
`expect` で共通側に「型シグネチャだけ」宣言し、各プラットフォームソースセットで `actual` として実装を与える。

## 仕組み

- `expect` キーワード：`commonMain` で「実装なしの宣言」を置く
- `actual` キーワード：各プラットフォームソースセットで対応する実装を置く
- コンパイラがペアをマッチさせて結合する

## 宣言できるもの

| 種別 | サポート状況 |
| --- | --- |
| function | Stable |
| property | Stable |
| object | Stable |
| interface | Stable |
| enum | Stable |
| annotation | Stable |
| class | Beta（公式は interface + factory を推奨） |

## 基本ルール

1. `commonMain` で `expect` を使って宣言する
2. すべてのプラットフォームソースセットで対応する `actual` を提供する（足りないとビルドエラー）
3. `expect` と `actual` で **パッケージ名を一致させる**
4. `expect` 側に実装を書いてはいけない
5. シグネチャは完全に一致させる

## サンプル：関数

```kotlin
// commonMain
package identity

class Identity(val userName: String, val processID: Long)
expect fun buildIdentity(): Identity
```

```kotlin
// jvmMain
package identity

actual fun buildIdentity() = Identity(
    System.getProperty("user.name") ?: "None",
    ProcessHandle.current().pid()
)
```

```kotlin
// nativeMain
package identity

actual fun buildIdentity() = Identity(
    getlogin()?.toKString() ?: "None",
    getpid().toLong()
)
```

## サンプル：プロパティ・object・クラス

```kotlin
// expect property
expect val identity: Identity
// actual
actual val identity: Identity = JVMIdentity()
```

```kotlin
// expect object
expect object IdentityBuilder {
    fun build(): Identity
}
// actual
actual object IdentityBuilder {
    actual fun build() = Identity(...)
}
```

```kotlin
// expect class（Beta）
expect class Identity() {
    val userName: String
    val processID: Long
}
// actual
actual class Identity {
    actual val userName: String = System.getProperty("user.name") ?: "None"
    actual val processID: Long = ProcessHandle.current().pid()
}
```

## 型エイリアスによる `actual`

既存のプラットフォーム型を `actual` として再利用できる。

```kotlin
// commonMain
expect enum class Month { JANUARY, FEBRUARY /* ... */ }

// jvmMain
actual typealias Month = java.time.Month
```

```kotlin
// commonMain
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
expect annotation class XmlSerializable()

// jvmMain
actual typealias XmlSerializable = javax.xml.bind.annotation.XmlRootElement
```

## 制約・注意点

- `expect` 宣言は `private` 不可
- `expect class` のコンストラクタも `expect` 側でシグネチャだけ書く
- `actual enum` は `expect enum` よりエントリを **追加** できる（`when` で `else` 必須）
- `expect` の存在自体が共通コードを「プラットフォーム個数だけビルドが必要」にするので濫用しない

## 代替手段：interface + factory function（推奨）

公式ドキュメントは **可能なら標準的な言語機能で書け** と推奨している。
`expect class` を使う代わりに、`commonMain` 側にインターフェースを置き、`actual` は「インターフェースを返すファクトリ関数」にする。

```kotlin
// commonMain
interface Identity {
    val userName: String
    val processID: Long
}

expect fun buildIdentity(): Identity
```

```kotlin
// jvmMain
class JVMIdentity(
    override val userName: String = System.getProperty("user.name") ?: "none",
    override val processID: Long = ProcessHandle.current().pid()
) : Identity

actual fun buildIdentity(): Identity = JVMIdentity()
```

利点：

- プラットフォームごとに **複数実装**を持てる
- テストで Fake / Mock を差し込みやすい
- 設計上の柔軟性が高い

## 代替手段：Dependency Injection

DI フレームワーク（Koin / Kotlin-Inject など）または手動コンストラクタ注入で
「プラットフォーム実装は外から渡す」ようにすると、`expect`/`actual` をほぼ排除できる。

```kotlin
// commonMain
interface IdentityProvider {
    fun getIdentity(): Identity
}

class Greeting(private val provider: IdentityProvider) {
    fun greet(): String = "Hello, ${provider.getIdentity().userName}"
}
```

```kotlin
// androidMain / iosMain
class AndroidIdentityProvider : IdentityProvider { /* ... */ }
class IOSIdentityProvider : IdentityProvider { /* ... */ }
```

公式の推奨：**プロジェクトで DI を既に使っているなら、プラットフォーム固有依存も DI に寄せる**。

## 判断フロー

```text
プラットフォーム固有実装が必要？
 ├─ No → commonMain にそのまま書く
 └─ Yes
     ├─ interface + factory で表現できる？ → 推奨（柔軟・テスト容易）
     ├─ DI を使っている？ → DI 経由で注入
     └─ 上記が困難で、単純な関数 / プロパティだけ？ → expect / actual
```

## References

- Expect/actual declarations: <https://kotlinlang.org/docs/multiplatform-expect-actual.html>
- Project structure: <https://kotlinlang.org/docs/multiplatform-discover-project.html>
