# Null 安全

Kotlin の型システムは null を型レベルで区別する。公式の [Null safety](https://kotlinlang.org/docs/null-safety.html) に基づきます。

## nullable / non-null 型

```kotlin
var a: String = "abc"
a = null            // コンパイルエラー

var b: String? = "abc"
b = null            // OK
```

原則として **non-null を既定** とし、null を許容するときだけ `?` を付ける。

## 演算子早見表

| 演算子 | 戻り | 例外 | 用途 |
| --- | --- | --- | --- |
| `?.` | 値 or `null` | なし | 安全な呼び出し |
| `?:` | 左が非 null なら左、それ以外右辺 | 右辺次第 | デフォルト値 / 早期 return |
| `!!` | 非 null 値 | null なら `NullPointerException` | 「絶対 null でない」アサート（極力避ける） |
| `as?` | キャスト成功なら結果、失敗なら `null` | なし | 安全なキャスト |

### 安全呼び出し `?.`

```kotlin
val len: Int? = name?.length
bob?.department?.head?.name   // 途中で null ならチェーン全体が null
```

### Elvis 演算子 `?:`

```kotlin
val n: Int = name?.length ?: 0
val parent = node.getParent() ?: return null
val id     = config.id ?: throw IllegalStateException("id required")
```

### 非 null アサート `!!`

```kotlin
val len = name!!.length   // null なら NPE
```

使ってよい場面はごく稀。ライブラリの仕様上 null を返さないと分かっており、かつコンパイラには伝わらないケース以外では、`?.` + `?:` か `requireNotNull` / `checkNotNull` を使う。

```kotlin
val v = requireNotNull(value) { "value must not be null" }
```

### 安全キャスト `as?`

```kotlin
val any: Any = "hello"
val s: String? = any as? String  // 成功
val i: Int?    = any as? Int     // null
```

`as` は失敗時に `ClassCastException` を投げる。型が不確かなら `as?` を使う。

## スマートキャスト

null チェックや型チェックの後、コンパイラは型を自動的に絞り込む。

```kotlin
fun length(s: String?): Int {
    if (s == null) return 0
    return s.length   // ここでは String にスマートキャスト済み
}

fun describe(any: Any): String = when (any) {
    is String -> "len=${any.length}"   // String にキャスト
    is Int    -> "int=$any"
    else      -> "other"
}
```

ローカルでない `var` プロパティはスマートキャストが効かないため、ローカル `val` に受け直すか `?.let { ... }` を使う:

```kotlin
class Holder { var name: String? = null }

fun render(h: Holder) {
    h.name?.let { println(it.length) } // 安全
}
```

## プラットフォーム型（Java 連携）

Java から来る型はエラーメッセージ上 `T!` と表示され、null かどうかコンパイラには判断できない。指定なしの場合、Kotlin はそのまま受け入れるが、実行時に NPE になり得る。

対策:

1. Java 側に `@Nullable` / `@NotNull` などのアノテーションを付ける（JSR-305 / Jetbrains / Android annotations）。
2. Kotlin 側で受けるときに型を明示する: `val s: String? = javaObj.getName()`。
3. Kotlin から Java を呼ぶときは戻り値を nullable と仮定して扱う。

## nullable コレクション

```kotlin
val maybeNumbers: List<Int?> = listOf(1, null, 3)
val numbers: List<Int> = maybeNumbers.filterNotNull()
```

`List<Int>?` と `List<Int?>` と `List<Int?>?` は別物。意図を型で表現する。

## 使い分けの指針

| 状況 | 推奨 |
| --- | --- |
| null になり得る値を安全に変換したい | `value?.let { ... } ?: default` |
| null なら早期 return / throw | `val x = nullable ?: return` / `?: throw ...` |
| 「null は起きない」とコードで保証したい | `requireNotNull(x)` / `checkNotNull(x)` |
| キャスト先が不確定 | `as?` |
| `!!` を書きたくなったとき | まず本当に必要か疑う |

## References

- https://kotlinlang.org/docs/null-safety.html
- https://kotlinlang.org/docs/home.html
