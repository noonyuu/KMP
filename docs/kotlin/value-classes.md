# value class（inline value class）

単一の値をラップして「型としての意味」を与えるための仕組み。実行時のオーバーヘッドを最小限にしつつ型安全性を上げられる。公式の [Inline value classes](https://kotlinlang.org/docs/inline-classes.html) に基づきます。

## 構文

```kotlin
@JvmInline
value class UserId(val raw: String)

@JvmInline
value class Email(val value: String) {
    init {
        require("@" in value) { "invalid email: $value" }
    }
}
```

- `value class` は **プライマリコンストラクタにプロパティを 1 つだけ** 持つ。
- JVM ターゲットでは `@JvmInline` が必須。Multiplatform で JVM 以外を含む場合も付けて支障ない。

## メンバとして書けるもの

- 計算プロパティ（バッキングフィールドなし、`lateinit` 不可、委譲不可）
- 関数
- `init` ブロック
- セカンダリコンストラクタ（プライマリに委譲する形）

```kotlin
@JvmInline
value class Person(private val fullName: String) {
    init { require(fullName.isNotBlank()) }
    constructor(first: String, last: String) : this("$first $last")
    val length: Int get() = fullName.length
    fun greet() = "Hello, $fullName"
}
```

## 継承

- インタフェースを実装することはできる。
- クラスを継承することはできない（常に `final`）。

```kotlin
interface Printable { fun pretty(): String }

@JvmInline
value class Name(val s: String) : Printable {
    override fun pretty() = "Name($s)"
}
```

委譲も可:

```kotlin
@JvmInline
value class Wrapper(val inner: Printable) : Printable by inner
```

## 実行時の表現（ボックス化）

コンパイラは可能な限り **underlying type のまま展開（unboxing）** する。ただし以下の状況ではボックス化される:

- ジェネリックパラメータに渡るとき
- インタフェース型として扱われるとき
- nullable 型（`UserId?`）として扱われるとき
- 配列要素になるとき（基本型のとき）

```kotlin
@JvmInline value class Foo(val i: Int) : I
fun asInline(f: Foo)      {}  // unboxed
fun asGeneric(x: Any)     {}  // boxed
fun asInterface(i: I)     {}  // boxed
fun asNullable(f: Foo?)   {}  // boxed
```

つまり「内部表現が Int / String などのプリミティブ寄り」になるほどメリットが大きい。

## value class vs data class

| | value class | data class |
| --- | --- | --- |
| プロパティ数 | **1 つだけ** | 1 つ以上 |
| 主目的 | 「単一の値に型としての意味を与える」 | 「複数の値を組として扱う」 |
| ランタイム表現 | 多くの場合 unbox される | 常に通常のオブジェクト |
| `copy` / 分解 | あり（プロパティ 1 つ） | あり |
| 継承 | 不可（`final`） | 不可（`final`） |
| equals/hashCode | underlying 値で決まる | 全プロパティで決まる |

ID やドメイン上の単一値（メール、URL、トークン、金額、座標の各軸など）は value class。複数フィールドの集合は data class。

## value class vs typealias

| | value class | typealias |
| --- | --- | --- |
| 新しい型を作るか | **作る**（型安全） | 作らない（別名） |
| 代入互換性 | underlying 型と互換でない | 完全互換 |
| 実行時表現 | unbox 可能 | コンパイル時のみ |

```kotlin
typealias NameAlias = String
@JvmInline value class NameClass(val s: String)

fun takeString(s: String) {}
takeString(NameAlias(""))   // OK（同じ String）
takeString(NameClass(""))   // コンパイルエラー
```

混同しやすい引数を取り違えにくくしたいなら value class、単に長い型名を別名にしたいだけなら typealias。

## JVM 命名マングリング

オーバーロード曖昧性を避けるため、value class を取る関数はバイトコード上で名前マングリングされる:

```kotlin
@JvmInline value class UInt(val x: Int)

fun compute(x: Int)  { } // compute(I)
fun compute(x: UInt) { } // compute-<hash>(I)
```

Java から呼ぶには `@JvmName` を付けてマングリングを上書きする:

```kotlin
@JvmName("computeUInt")
fun compute(x: UInt) { }
```

## Swift 連携（Kotlin/Native）の注意

JVM のマングリングと同様、Kotlin/Native でも value class は underlying type にコンパイルされ、Objective-C / Swift 側からは underlying type として見える。Swift から `UserId` のような型安全性を保ちたい場合は、Swift 側でもラッパー型を作る、もしくはブリッジ用の通常クラスを介する必要がある。

実用上の指針:

- 公開 API（Objective-C ヘッダ経由で Swift から見える層）に value class を直接出すと、Swift 側では String / Int として扱われ型安全性が失われる。
- 内部実装の型安全性向上には積極的に使ってよい。
- Swift から型として区別したいなら、Swift 側で thin wrapper（struct）を定義する。

## 使うべきとき / 使わないべきとき

使うべき:

- ドメインの単一値（ID、メール、トークン、金額単位、距離、角度…）
- プリミティブを取り違える事故を防ぎたい関数シグネチャ
- 計算プロパティで underlying 値からの派生情報を提供したい

使わないべき:

- フィールドが 2 つ以上必要 → `data class`
- underlying 型と完全互換のままにしたい → `typealias`
- Java/Swift から型として区別される必要がある → 通常クラス（または各言語側でのラップ）

## References

- https://kotlinlang.org/docs/inline-classes.html
- https://kotlinlang.org/docs/idioms.html
