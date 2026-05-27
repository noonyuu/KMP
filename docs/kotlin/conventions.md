# コーディング規約

公式の [Coding conventions](https://kotlinlang.org/docs/coding-conventions.html) に基づく要点まとめ。IntelliJ IDEA / Android Studio の既定フォーマッタはこの規約に従う。

## ファイル

| 項目 | ルール |
| --- | --- |
| 拡張子 | `.kt`（スクリプトは `.kts`） |
| 単一クラスのファイル名 | クラス名と一致（`UpperCamelCase.kt`） |
| 複数宣言を含むファイル名 | 内容を表す名前を UpperCamelCase で（`Util` のような無意味な名前は避ける） |
| ディレクトリ構造 | パッケージ構造と一致させる（Java と混在しないなら共通プレフィックスは省略可） |

## 命名

| 対象 | 規約 | 例 |
| --- | --- | --- |
| パッケージ | すべて小文字、`_` なし | `com.example.network.socket` |
| クラス・オブジェクト | UpperCamelCase | `UserRepository` |
| 関数・プロパティ・ローカル変数 | lowerCamelCase | `loadUser`, `userId` |
| 定数（`const val` / トップレベル不変） | SCREAMING_SNAKE_CASE | `const val MAX_COUNT = 8` |
| 可変・振る舞いを持つプロパティ | lowerCamelCase | `val mutableQueue = ArrayDeque<...>()` |
| バッキングプロパティ | アンダースコア接頭辞 | `private val _items` / `val items` |
| テストメソッド | バッククォート + スペース許容（テストコードのみ） | `` `returns null when empty`() `` |

略語は 2 文字なら全大文字、3 文字以上は先頭のみ大文字:

- `IOStream`, `XmlParser`, `HttpClient`

## 書式

- インデント: **スペース 4 つ**。タブは使わない。
- 1 行の長さ: 公式の厳密な数値指定はないが、IDE 既定（120）に従うのが一般的。
- 開き波括弧は行末、閉じ波括弧は独立行。
- 二項演算子の前後にスペース。範囲演算子 `..` は前後なし。
- `if`, `for`, `when`, `while` の後にスペース。関数宣言・呼び出しの `(` の前にはスペースを入れない。
- `.` と `?.` の前後にはスペースを入れない。
- `:` は、型/上位型の区切りやオブジェクト宣言では **前後にスペース**、宣言部（`x: Int`）では **前のみなし**。コロンの **後ろは常にスペース**。

```kotlin
class Person(
    id: Int,
    name: String,
) : Human(id, name) { /* ... */ }

fun foo(x: Int = 1): Int = x + 1
```

## 修飾子の順序

```
public / protected / private / internal
expect / actual
final / open / abstract / sealed / const
external
override
lateinit
tailrec
vararg
suspend
inner
enum / annotation / fun
companion
inline / value
infix
operator
data
```

例: `private inline fun ...`、`internal data class ...`。

## クラスのレイアウト

並び順（公式推奨）:

1. プロパティ宣言・初期化子ブロック
2. セカンダリコンストラクタ
3. メソッド
4. コンパニオンオブジェクト
5. ネストクラス（外部からも使うなら末尾に）

アルファベット順や可視性順では並べず、関連するコードをまとめる。

## 関数

- 単一式関数は `=` で書く。

```kotlin
fun double(x: Int) = x * 2          // OK
fun double(x: Int): Int { return x * 2 } // 冗長
```

- 戻り値が `Unit` の場合は型を省略する。
- 1 行に収まらないシグネチャは、各引数を 1 行ずつ書き、末尾カンマを付ける。

```kotlin
fun process(
    request: Request,
    options: Options = Options.Default,
    listener: Listener? = null,
): Result { /* ... */ }
```

- ラムダ:

```kotlin
list.filter { it > 10 }
list.map { value ->
    val doubled = value * 2
    doubled.toString()
}
```

## 制御フロー

- 式形式を優先: `return if (x) a else b`。
- 2 分岐は `if`、3 分岐以上は `when`。
- ループより `filter` / `map` / `forEach` 等の高階関数を優先。
- 不要な `else` を書かない（網羅性チェックが効く `when` では特に）。

## 冗長性を減らす

- セミコロンは原則使わない。
- `Unit` 戻り型は書かない。
- 文字列テンプレートの単純変数では波括弧を省く: `"Hello, $name"`。
- 比較演算で `compareTo` を直接呼ばず演算子を使う: `a < b`。

## 不変性

- `val` を既定にして `var` は必要最小限。
- 公開する型はインタフェース（`List<T>`、`Map<K,V>`）を使い、実装型（`ArrayList` 等）を露出しない。
- 公開するコレクションは原則 immutable インタフェースで返す。

## プロパティ vs 関数

「プロパティ」にしてよいのは以下を満たすとき:

- 例外を投げない
- 計算が安価
- 同じオブジェクト状態なら同じ値を返す

それ以外は関数にする。

## 文字列

- 連結ではなく文字列テンプレートを使う。
- 複数行は `"""..."""` と `trimIndent()` / `trimMargin()` を組み合わせる。

## 末尾カンマ

宣言（パラメータリスト・enum 定数列など）では推奨。差分が見やすくなる。

## References

- https://kotlinlang.org/docs/coding-conventions.html
- https://kotlinlang.org/docs/home.html
