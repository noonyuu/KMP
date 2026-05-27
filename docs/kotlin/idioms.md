# 慣用的な Kotlin

Kotlin で「Kotlin らしく」書くための定型パターン集。公式の [Idioms](https://kotlinlang.org/docs/idioms.html) に基づきます。

## データクラス

値の集約には `data class` を使う。`equals` / `hashCode` / `toString` / `copy` / 分解宣言が自動生成される。

```kotlin
data class Customer(val name: String, val email: String)

val c = Customer("Alice", "a@example.com")
val updated = c.copy(email = "new@example.com")
val (name, email) = updated // 分解宣言
```

ルール:

- すべてのプロパティは `val` を基本とし、不変に保つ。
- ビヘイビアを持たせない（メソッドを追加するなら通常の `class` を検討）。

## sealed クラス / sealed interface

「閉じた多態性」を表現する。`when` でのスマートな網羅性チェックが効く。

```kotlin
sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure(val error: Throwable) : Result<Nothing>
    data object Loading : Result<Nothing>
}

fun <T> render(r: Result<T>): String = when (r) {
    is Result.Success -> "OK: ${r.value}"
    is Result.Failure -> "NG: ${r.error.message}"
    Result.Loading    -> "..."
} // else 不要（網羅）
```

- enum で十分なら enum、各バリアントに固有のデータを持たせたいなら sealed。
- 状態を持たないバリアントは `data object`。

## スコープ関数

| 関数 | レシーバ | 戻り値 | 主な用途 |
| --- | --- | --- | --- |
| `let` | `it` | ラムダの結果 | nullable 値の変換、一時変数の局所化 |
| `run` | `this` | ラムダの結果 | オブジェクトの設定 + 値の計算 |
| `with` | `this` | ラムダの結果 | 既存オブジェクトに対する複数操作 |
| `apply` | `this` | レシーバ自身 | オブジェクトの初期化（ビルダー風） |
| `also` | `it` | レシーバ自身 | 副作用（ログ、追加処理） |

```kotlin
// nullable 値を変換し、null ならデフォルト
val mapped = value?.let { transform(it) } ?: default

// ビルダー風の初期化
val rect = Rectangle().apply {
    length = 4
    breadth = 5
}

// 副作用を挟む
val list = mutableListOf(1, 2, 3).also { println("init: $it") }

// 複数メソッド呼び出し
with(turtle) {
    penDown()
    forward(100.0)
    penUp()
}
```

濫用しないこと。意味のある名前のローカル変数の方が読みやすい場合は普通に書く。

## when 式

```kotlin
fun classify(n: Int): String = when {
    n < 0   -> "negative"
    n == 0  -> "zero"
    else    -> "positive"
}

fun transform(color: String): Int = when (color) {
    "Red", "Crimson" -> 0
    "Green"          -> 1
    "Blue"           -> 2
    else             -> throw IllegalArgumentException(color)
}
```

- 値を返す式として使う。
- 2 分岐なら `if` を優先、3 分岐以上は `when`。
- sealed / enum を対象にすると網羅性チェックが効くため `else` を書かない。

## コレクション

```kotlin
val list = listOf("a", "b", "c")
val map  = mapOf("a" to 1, "b" to 2)

val positives = numbers.filter { it > 0 }
val doubled   = numbers.map { it * 2 }
val sum       = numbers.sum()

// in 演算子
if ("a" in list) { /* ... */ }

// ペアのループ
for ((k, v) in map) println("$k = $v")
```

- 不変版（`List`、`Map`、`Set`）をデフォルトとし、書き換える必要があるときだけ `MutableList` を使う。
- `for` ループより高階関数（`filter` / `map` / `fold`）を優先する。

## 文字列テンプレート

```kotlin
val name = "Kotlin"
println("Hello, $name! Length is ${name.length}.")
```

連結 `+` ではなくテンプレートを使う。

## 単一式関数 / 式本体

```kotlin
fun theAnswer() = 42
fun double(x: Int) = x * 2
```

一行で書ける関数は `=` 形式にする。

## デフォルト引数・名前付き引数

```kotlin
fun greet(name: String = "world", excited: Boolean = false): String =
    "Hello, $name" + if (excited) "!" else "."

greet(excited = true)
```

オーバーロードの多用を避けるためにデフォルト引数を使う。

## lazy プロパティ

```kotlin
val config: Config by lazy { loadConfig() }
```

初回アクセス時に一度だけ計算される（既定でスレッドセーフ）。

## 型安全な ID

プリミティブを直接持ち回らず、`value class` で意味を型に持たせる（詳細は [value-classes.md](./value-classes.md)）。

```kotlin
@JvmInline value class UserId(val raw: String)
@JvmInline value class OrderId(val raw: String)
```

## References

- https://kotlinlang.org/docs/idioms.html
- https://kotlinlang.org/docs/home.html
