# Flow

複数の値を非同期に返すための型。公式の [Asynchronous Flow](https://kotlinlang.org/docs/flow.html) に基づきます。

## Flow の特徴

- **コールド**: `collect` するまで生成側のコードは実行されない。`collect` するたびに最初から流れる。
- **逐次**: 既定では発行と消費が同じコルーチンで直列に動く。
- **構造化された並行性に従う**: 収集元のコルーチンがキャンセルされれば、Flow も止まる。
- **コンテキスト保持**: `flow { ... }` の中身は収集側の context で実行される（変更には `flowOn`）。

## ビルダー

```kotlin
val a: Flow<Int> = flow {
    for (i in 1..3) {
        delay(100)
        emit(i)
    }
}

val b: Flow<String> = flowOf("a", "b", "c")
val c: Flow<Int>    = (1..3).asFlow()
```

`channelFlow { ... }` は別コルーチンから `send` できる（ホットな入力を Flow に橋渡しするとき）。

## オペレータ

中間オペレータは Flow を返し、終端オペレータが収集を開始する。

### 中間（一例）

| オペレータ | 役割 |
| --- | --- |
| `map { }` | 各値を変換 |
| `filter { }` | 条件を満たす値だけ通す |
| `transform { emit(...) }` | 任意個の値に変換 |
| `take(n)` | 先頭 n 個だけ |
| `onEach { }` | 副作用を挟む（変換しない） |
| `flowOn(d)` | 上流の実行 Dispatcher を切り替え |
| `buffer()` | 上流と下流を並行化 |
| `conflate()` | 最新値以外を捨てる |
| `debounce(d)` | 値の連続発行を間引く |
| `distinctUntilChanged()` | 連続する重複を除去 |
| `combine` / `zip` | 複数 Flow を合成 |
| `flatMapConcat` / `flatMapMerge` / `flatMapLatest` | Flow をネスト展開 |

### 終端

| オペレータ | 役割 |
| --- | --- |
| `collect { }` | 1 件ずつ処理 |
| `collectLatest { }` | 新しい値が来たら処理中をキャンセル |
| `toList()` / `toSet()` | コレクション化 |
| `first()` / `firstOrNull()` | 先頭だけ |
| `reduce` / `fold` | 集約 |
| `launchIn(scope)` | `scope` 上で起動（onEach と組み合わせる） |

```kotlin
flow
    .onEach { log(it) }
    .launchIn(viewModelScope)
```

## コンテキスト保持と `flowOn`

`flow { ... }` の中で `withContext` を直接使うのは禁止（`IllegalStateException`）。上流の実行スレッドを変えるには `flowOn`:

```kotlin
flow {
    emit(loadFromDisk()) // IO
}
    .flowOn(Dispatchers.IO)
    .map { it.parse() }  // 収集側コンテキスト
    .collect { render(it) }
```

`flowOn` より下流（`map` 以降）は呼び出し側の context で動く。

## 例外処理

例外は上流から下流に流れる。`catch` オペレータは **上流のみ** をカバーする。

```kotlin
flow
    .catch { e -> emit(fallback) }    // 上流の例外を補足
    .onEach { check(it.isValid) }     // ここの例外は catch されない
    .collect { render(it) }
```

下流の例外も処理したいなら、`collect` ブロック内で try/catch するか、`onEach` の後に再度 `catch` を置く。

## 完了

```kotlin
flow
    .onCompletion { cause ->
        if (cause == null) log("done")
        else               log("failed: $cause")
    }
    .collect { ... }
```

## キャンセル

中断ポイントごとにキャンセルがチェックされる。busy ループでは `.cancellable()` を入れる:

```kotlin
(1..Int.MAX_VALUE).asFlow()
    .cancellable()
    .collect { ... }
```

## ホット Flow: StateFlow / SharedFlow

`Flow` は基本コールドだが、ホットな状態・イベント伝搬には **StateFlow** と **SharedFlow** を使う（`kotlinx.coroutines` 提供）。

### StateFlow

- 「現在の状態」を表す。必ず初期値を持つ。
- 直近の値を保持し、新しい collector に即座に発行する。
- 同値（`equals`）の連続発行はスキップされる（conflate 相当）。
- UI の状態保持に向く。

```kotlin
private val _state = MutableStateFlow(UiState.Initial)
val state: StateFlow<UiState> = _state.asStateFlow()

_state.update { it.copy(loading = true) }
```

### SharedFlow

- 「イベント／ブロードキャスト」用途。初期値を持たない。
- `replay`、`extraBufferCapacity`、`onBufferOverflow` で挙動を細かく制御。
- ワンショットイベント（スナックバー表示など）に向く。

```kotlin
private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
val events: SharedFlow<Event> = _events.asSharedFlow()

_events.tryEmit(Event.Saved)
```

### 比較

| | StateFlow | SharedFlow | Flow |
| --- | --- | --- | --- |
| ホット/コールド | ホット | ホット | コールド |
| 初期値 | 必須 | なし | なし |
| 直近値の保持 | 1 件 | replay 指定数 | なし |
| 同値スキップ | あり | なし | なし |
| 主用途 | 状態 | イベント | ストリーム |

## コールド Flow をホットに変える

`stateIn` / `shareIn` で、上流コールド Flow を共有可能なホット Flow に変換できる。

```kotlin
val users: StateFlow<List<User>> = repo.observeUsers()
    .stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
```

`WhileSubscribed` は購読者がいる間だけ上流を生かし、リソースリークを防ぐ。

## テスト

`kotlinx-coroutines-test` の `runTest` と `Turbine`（サードパーティ）を使うのが定石。標準 API でも:

```kotlin
@Test
fun emitsValues() = runTest {
    val values = flow.take(3).toList()
    assertEquals(listOf(1, 2, 3), values)
}
```

時間依存（`debounce` 等）は `TestScope.testScheduler.advanceTimeBy(...)` で進める。

## References

- https://kotlinlang.org/docs/flow.html
- https://kotlinlang.org/docs/coroutines-and-channels.html
