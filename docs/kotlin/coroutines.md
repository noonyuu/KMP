# コルーチン

非同期処理を逐次的なコードで書くための仕組み。公式の [Coroutines overview](https://kotlinlang.org/docs/coroutines-overview.html)、[Coroutines basics](https://kotlinlang.org/docs/coroutines-basics.html)、[Cancellation and timeouts](https://kotlinlang.org/docs/cancellation-and-timeouts.html)、[Coroutines and channels](https://kotlinlang.org/docs/coroutines-and-channels.html) に基づきます。

## suspend 関数

`suspend` を付けると、その関数はスレッドをブロックせずに中断・再開できる。

```kotlin
suspend fun fetchUser(id: String): User {
    delay(100)
    return api.getUser(id)
}
```

ルール:

- `suspend` 関数は別の `suspend` 関数、もしくはコルーチンビルダーの中からしか呼べない。
- 副作用を持つ長時間処理は基本的に `suspend` にする。

## コルーチンビルダー

| ビルダー | 戻り値 | 用途 |
| --- | --- | --- |
| `launch` | `Job` | 結果を返さない fire-and-forget |
| `async` | `Deferred<T>` | 結果を返す並行計算（`await()` で取得） |
| `runBlocking` | `T` | 非 suspend 文脈から suspend を呼ぶ橋渡し（テスト・`main` 限定） |

```kotlin
coroutineScope {
    val job = launch { doWork() }
    val deferred = async { fetchValue() }
    val value = deferred.await()
    job.join()
}
```

`runBlocking` はカレントスレッドをブロックするので、本番コードでは避ける。`main` 関数のエントリやレガシー API のブリッジに限る。

## 構造化された並行性

すべてのコルーチンは `CoroutineScope` の中で起動される。スコープは木構造を成し:

- 親は全ての子の完了を待つ。
- 親がキャンセルされると子も再帰的にキャンセルされる。
- 子で発生した例外は親に伝搬する（`supervisorScope` を除く）。

```kotlin
suspend fun loadAll() = coroutineScope {
    val a = async { loadA() }
    val b = async { loadB() }
    a.await() to b.await()
} // ここから先は a, b の両方が完了している
```

### `coroutineScope` vs `supervisorScope`

| | 子の失敗が他の子に影響 | 主な用途 |
| --- | --- | --- |
| `coroutineScope` | する（全体失敗） | 全てが揃って初めて意味がある処理 |
| `supervisorScope` | しない | 独立した複数タスク（片方が落ちても残りを生かす） |

### GlobalScope は使わない

`GlobalScope.launch { ... }` はライフサイクルから切り離されるため、リーク・キャンセル不能の原因になる。アプリ／コンポーネントが管理するスコープを使うこと。

## ディスパッチャ

`CoroutineDispatcher` がコルーチンを実行するスレッドを決める。

| Dispatcher | 用途 |
| --- | --- |
| `Dispatchers.Default` | CPU バウンド処理。コア数に応じた共有スレッドプール |
| `Dispatchers.IO` | I/O バウンド処理（ネットワーク・ファイル）。可変サイズのプール |
| `Dispatchers.Main` | UI スレッド（Android / Swing 等）。プラットフォーム固有 |
| `Dispatchers.Unconfined` | 検証用。本番では原則使わない |

切り替えは `withContext`:

```kotlin
suspend fun loadJson(url: String): Json = withContext(Dispatchers.IO) {
    httpClient.get(url).body()
}
```

子コルーチンは特に指定しない限り親のディスパッチャを継承する。`async(Dispatchers.Default)` のような重複指定は不要。

## キャンセル

キャンセルは **協調的**。コルーチンは中断ポイント（`delay`、`yield`、`withContext` など）でキャンセルをチェックする。CPU を回し続けるループはキャンセルされない。

```kotlin
val job = launch {
    while (isActive) {            // 明示的にチェック
        heavyStep()
    }
}
job.cancel()
job.join()                        // = job.cancelAndJoin()
```

中断ポイントを挟む方法:

```kotlin
repeat(1_000_000) {
    yield()                       // 中断 + キャンセルチェック
    compute(it)
}

while (true) {
    ensureActive()                // キャンセル時に即座に CancellationException
    compute()
}
```

### リソース解放

`finally` は実行されるが、その中で suspend を呼ぶとさらにキャンセルされる。確実に完了させたい後始末は `NonCancellable`:

```kotlin
try {
    doWork()
} finally {
    withContext(NonCancellable) {
        flushAndClose()
    }
}
```

### `CancellationException` は再スローする

`CancellationException` は構造化並行性の合図。`catch` で握り潰してはいけない。

```kotlin
try {
    doWork()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    log(e)
}
```

### タイムアウト

```kotlin
val result: Result? = withTimeoutOrNull(2.seconds) { fetch() }
val mustResult = withTimeout(2.seconds) { fetch() } // TimeoutCancellationException
```

## 例外ハンドリング

- `launch` の例外は親まで伝搬し、未捕捉なら `CoroutineExceptionHandler` に渡る。
- `async` の例外は `await()` を呼んだときに再スローされる。
- `coroutineScope { ... }` 内で起きた例外は呼び出し元へ伝搬し、スコープ内の他の子はキャンセルされる。
- `supervisorScope` では子の例外は兄弟へ波及しない。

## 典型パターン

### 並行実行

```kotlin
suspend fun loadDashboard(): Dashboard = coroutineScope {
    val user    = async { userRepo.me() }
    val notices = async { noticeRepo.latest() }
    Dashboard(user.await(), notices.await())
}
```

### キャンセルに強いループ

```kotlin
suspend fun pollUntilReady(): Status {
    while (currentCoroutineContext().isActive) {
        val s = check()
        if (s.isReady) return s
        delay(500)
    }
    throw CancellationException()
}
```

## テスト

`kotlinx-coroutines-test` の `runTest` を使う。仮想時間で `delay` を即座に進められる。

```kotlin
@Test
fun example() = runTest {
    val result = loadDashboard()
    assertEquals(expected, result)
}
```

`runBlocking` を本体コードでは使わないのと同様、テストでも `runTest` を優先する。

## References

- https://kotlinlang.org/docs/coroutines-overview.html
- https://kotlinlang.org/docs/coroutines-basics.html
- https://kotlinlang.org/docs/cancellation-and-timeouts.html
- https://kotlinlang.org/docs/coroutines-and-channels.html
