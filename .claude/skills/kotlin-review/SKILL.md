---
name: kotlin-review
description: Review Kotlin code for idiomatic style, null-safety, coroutine correctness (structured concurrency, dispatcher choice, cancellation), Flow usage, and adherence to official coding conventions. Use when reviewing .kt files.
---

# kotlin-review

`.kt` ファイルの差分を、以下のチェックリストに沿って機械的にレビューする。各項目には根拠となる公式ドキュメントを示す。重大度の目安: [BLOCK] = 修正必須、[WARN] = 強く推奨、[NIT] = 好みの問題。

## 進め方

1. 変更されたファイルを列挙し、`.kt` のみを対象にする。
2. 各ファイルをカテゴリ順（idioms → null safety → coroutines → Flow → conventions）に確認する。
3. 違反を見つけたら、該当箇所・該当ルール・修正案を併記する。
4. 修正案は必ずコードで示す。
5. 同じ違反が複数ある場合はまとめて報告する。

## 1. Idioms

source: <https://kotlinlang.org/docs/idioms.html>

- [WARN] 値のみを集約するクラスは `data class` にする。手書きの `equals`/`hashCode`/`toString` がある場合は理由を確認。
- [WARN] enum で表現していて各バリアントに固有データが必要になる場合は `sealed interface` / `sealed class` を提案する。
- [BLOCK] `when` の対象が `sealed` / `enum` のとき、不要な `else ->` を入れていないか（網羅性チェックが効かなくなる）。
- [WARN] 2 分岐は `if`、3 分岐以上は `when` を使う。長い `if/else if` チェーンは `when` に置き換え提案。
- [WARN] スコープ関数は適切な選択になっているか:
  - 設定 → `apply`
  - 副作用のみ → `also`
  - nullable の変換 → `let`
  - 結果を返す複数メソッド呼び出し → `with` / `run`
  - 入れ子で `it` / `this` の参照が不明瞭になるなら通常の変数を勧める。
- [WARN] `for` で済むコレクション処理を冗長に書いていないか。`filter` / `map` / `sumOf` / `groupBy` などの高階関数を提案。
- [WARN] 単一式関数は `=` 形式で書く（公式コーディング規約）。
- [NIT] 文字列連結 `+` ではなく文字列テンプレート（`"${...}"`）を使う。
- [WARN] 公開する不変コレクションは `List` / `Map` / `Set` で返し、`MutableList` 等を露出していないか。
- [WARN] プリミティブで複数の意味（`String` の userId と orderId など）が混在しているなら `value class` を提案（root: value-classes.md）。

## 2. Null safety

source: <https://kotlinlang.org/docs/null-safety.html>

- [BLOCK] `!!` の使用は原則 NG。本当に必要か（API 側の制約 / Java 連携など）コメントを求める。代替:
  - `?.let { ... } ?: default`
  - `requireNotNull(x) { "..." }` / `checkNotNull(x)`
  - 早期 return: `val v = x ?: return`
- [WARN] 同じ式で `?.` を 3 段以上チェーンしている → `let` で受けるか分割を提案。
- [WARN] Elvis で `throw` する場合はメッセージを必ず付ける: `?: error("id required")` / `?: throw IllegalStateException(...)`。
- [WARN] 型キャスト `as` を使っているとき、失敗の可能性があるなら `as?` + null チェックに置き換え。
- [WARN] Java 由来の戻り値を non-null として受けているコードは、プラットフォーム型由来の NPE リスクあり。`?` を明示するか、Java 側にアノテーションを付ける提案を行う。
- [WARN] スマートキャストできない `var` プロパティを直接参照していないか（複数スレッドで書き換わる可能性）。ローカル `val` に受け直すか `?.let { }` を使う。
- [NIT] `if (x != null) x.foo() else null` のような書き方は `x?.foo()` に短縮。

## 3. Coroutines

sources:

- <https://kotlinlang.org/docs/coroutines-overview.html>
- <https://kotlinlang.org/docs/coroutines-basics.html>
- <https://kotlinlang.org/docs/cancellation-and-timeouts.html>
- <https://kotlinlang.org/docs/coroutines-and-channels.html>

### 構造化された並行性

- [BLOCK] `GlobalScope.launch` / `GlobalScope.async` の使用。ライフサイクルから切り離されリークの原因になる。明示的なスコープ（`coroutineScope`、`viewModelScope` 等）に置き換える。
- [BLOCK] `suspend` 関数内で `runBlocking` を呼んでいる。`coroutineScope` などに置き換える。
- [WARN] `suspend` 関数の中でトップレベル `CoroutineScope(...)` を作って `launch` していないか（親子関係が切れる）。
- [WARN] 兄弟タスクの片方の失敗で全体を落とすべきでない用途には `supervisorScope` を使う。逆に「全部成功してこそ意味がある」処理に `supervisorScope` を使っていないか。

### ビルダーの選択

- [WARN] 結果を使わないのに `async { }.await()` を直列に書いている → `launch` または素の `suspend` 呼び出しに。
- [WARN] 並行で動かしたい複数の `async` の `await()` を順に呼ぶのは OK だが、片方の `async` の本体内で別の `async` を直列に await している場合は並行性が失われている。
- [BLOCK] 本番コードで `runBlocking` を使用（テスト・`main` ブリッジ以外）。

### Dispatcher

- [WARN] ブロッキング I/O が `Dispatchers.Default` 上で行われている → `Dispatchers.IO` を `withContext` で指定。
- [WARN] CPU バウンド処理を `Dispatchers.IO` で回している → `Dispatchers.Default` に。
- [WARN] 子コルーチンに `Dispatchers.Default` を冗長に指定していないか（親から継承される）。
- [WARN] UI フレームワーク非依存のコード（共有ロジック）で `Dispatchers.Main` を直接参照している → 注入可能にするか上位で `withContext` する。

### キャンセル

- [BLOCK] `CancellationException` を握り潰している `catch (e: Exception)` / `catch (e: Throwable)`。先に `if (e is CancellationException) throw e` を入れるか、`CancellationException` を再スロー。
- [WARN] 長時間 CPU を回すループに中断ポイント（`yield()` / `ensureActive()` / `isActive` チェック）がない。
- [WARN] `finally` で suspend を呼んでいて、キャンセル時に確実に完了させたい後始末 → `withContext(NonCancellable)` で囲む。
- [WARN] `Job` をマニュアル管理していて `cancelAndJoin` を呼んでいない。

### タイムアウト

- [WARN] 「失敗してほしくない」タイムアウトには `withTimeoutOrNull`、例外で扱いたいなら `withTimeout`。意図と一致しているか確認。

### 例外

- [WARN] `async` の結果を `await` せずに捨てていないか（例外が失われる）。
- [WARN] `CoroutineExceptionHandler` を `async` のスコープに付けている（`launch` でしか効かない）。

## 4. Flow

source: <https://kotlinlang.org/docs/flow.html>

- [BLOCK] `flow { ... }` の中で `withContext(...)` を直接呼んでいる（`IllegalStateException`）。上流の Dispatcher 変更は `.flowOn(...)` を使う。
- [WARN] `flowOn` の位置が誤っている。上流側にだけ効果があるため、変えたい処理より **下流** に置く。
- [WARN] UI / 状態のホルダーに `Flow<T>` を使っている → 初期値が必要なら `StateFlow`、イベントなら `SharedFlow` を提案。
- [WARN] `StateFlow` に意味のない初期値（`null` プレースホルダなど）を入れている。`UiState.Initial` のようなドメインに沿った値を使う。
- [WARN] `MutableStateFlow` / `MutableSharedFlow` を外部に公開している → `asStateFlow()` / `asSharedFlow()` で読み取り専用にする。
- [WARN] `stateIn` / `shareIn` の `started` が常時 `Eagerly` になっている → 購読者がいる間だけ上流を生かすなら `SharingStarted.WhileSubscribed(...)`。
- [WARN] `catch { }` が下流の `onEach`/`map` よりも下に置かれていない場合、下流例外をカバーしないことに注意（catch は上流のみ）。
- [WARN] busy ループな Flow（中断ポイントを含まない）に `.cancellable()` がない。
- [WARN] `collectLatest` を使うべき場面（新しい値で進行中処理をキャンセルしたい）で `collect` を使っていないか。
- [WARN] テストで `runBlocking` を使っている → `runTest` を使う。
- [NIT] 公開 Flow の型は `Flow<T>` / `StateFlow<T>` / `SharedFlow<T>` の読み取り専用型で返す。

## 5. Conventions

source: <https://kotlinlang.org/docs/coding-conventions.html>

### 命名

- [WARN] パッケージ名にアンダースコアや大文字が混じっている。
- [WARN] クラスが UpperCamelCase でない。
- [WARN] 関数・プロパティ・変数が lowerCamelCase でない。
- [WARN] `const val` / トップレベル不変が SCREAMING_SNAKE_CASE でない。
- [WARN] 略語の大文字化が不適切（2 文字なら全大文字、3 文字以上は先頭のみ大文字）。例: `XMLParser` ではなく `XmlParser`。
- [NIT] バッキングプロパティはアンダースコア接頭辞: `private val _items`、公開側は `items`。

### 書式

- [WARN] タブインデント、または 4 スペース以外。
- [WARN] 修飾子順序が公式順と異なる（`public/internal/private` → `expect/actual` → `override` → `suspend` → `data` … の順）。
- [WARN] 1 行で書けるシグネチャを `{ return ... }` で書いている → 式本体 `= ...` に。
- [WARN] 戻り型 `Unit` を明示している。
- [WARN] 文末セミコロン。
- [NIT] 文字列テンプレートで単純変数を `${x}` と書いている → `$x`。
- [NIT] 複数行シグネチャに末尾カンマがない。

### クラスレイアウト

- [NIT] 並び順: プロパティ → セカンダリコンストラクタ → メソッド → companion → ネスト。アルファベット順や可視性順で並べ替えていない。

### 不変性

- [WARN] 公開する型に `ArrayList` / `HashMap` 等の実装型が露出している → インタフェース型に。
- [WARN] 不要に `var` を使っている。`val` に置き換えられないか確認。

## 出力フォーマット

各指摘は次の形式で報告する:

```
[BLOCK] path/to/File.kt:42  Coroutines / GlobalScope の使用
  根拠: https://kotlinlang.org/docs/coroutines-basics.html
  問題: GlobalScope.launch はライフサイクルに紐づかずリークの原因になる。
  修正案:
    coroutineScope {
        launch { ... }
    }
```

最後に「[BLOCK] N 件 / [WARN] N 件 / [NIT] N 件」のサマリを付ける。
