# Swift Error Handling

Swift のエラーハンドリングは「回復可能なエラー」を扱う仕組み。プログラマブルエラー（前提条件違反）には `precondition` / `fatalError`、回復可能な失敗には `throws` を使い分ける。詳細は [The Swift Programming Language: Error Handling](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/errorhandling/) を参照。

## エラー型の表現

エラーは `Error` プロトコルに準拠する型で表す。`enum` が定石。

```swift
enum VendingMachineError: Error {
    case invalidSelection
    case insufficientFunds(coinsNeeded: Int)
    case outOfStock
}
```

- 関連値で文脈情報を運べる。
- `LocalizedError` に準拠すると UI 表示用文字列を提供できる。
- ライブラリ境界では `enum` で網羅的に列挙し、内部実装の例外は包んで再 throw する。

## throws 関数

エラーを送出し得る関数は `throws` を宣言に付ける。

```swift
func vend(itemNamed name: String) throws {
    guard let item = inventory[name] else {
        throw VendingMachineError.invalidSelection
    }
    guard item.count > 0 else {
        throw VendingMachineError.outOfStock
    }
    guard item.price <= coinsDeposited else {
        throw VendingMachineError.insufficientFunds(coinsNeeded: item.price - coinsDeposited)
    }
    // ...
}
```

- 戻り値型より前、`async` より後に書く: `func f() async throws -> T`。
- イニシャライザも `throws` 可能。失敗する初期化を表現できる（`init?` より情報量が多い）。

### typed throws（Swift 6+）

特定の Error 型のみを投げると宣言できる。

```swift
func parse() throws(ParseError) -> Tree { ... }
```

汎用ライブラリのパブリック API では型を絞りすぎず、内部では typed throws で意図を明確にする、といった使い分けができる。

## エラーの伝播と処理

### try

`throws` 関数の呼び出しには `try` が必要。

```swift
func buyFavoriteSnack() throws {
    try vend(itemNamed: "Candy Bar")  // 失敗したら上に伝播
}
```

### do-catch

```swift
do {
    try vend(itemNamed: name)
    print("Success")
} catch VendingMachineError.invalidSelection {
    print("Invalid selection.")
} catch VendingMachineError.outOfStock {
    print("Out of stock.")
} catch VendingMachineError.insufficientFunds(let coinsNeeded) {
    print("Insufficient funds. Need \(coinsNeeded) more.")
} catch {
    print("Unexpected error: \(error)")
}
```

- パターンマッチで個別ケースを処理できる。
- 末尾の `catch` は `error` 暗黙パラメータを束縛する。
- 非網羅的な `catch` は許容され、未処理エラーは外側に再 throw される（`throws` 文脈の場合）。

### try?

エラーを `nil` に変換する。

```swift
let x = try? someThrowingFunction()  // 成功なら値、失敗なら nil
```

エラーの種別が不要、または失敗が想定内で値の有無のみを問題にするときに使う。

### try!

エラーが起こらないことが確実なときの強制実行。失敗するとランタイムクラッシュ。

```swift
let photo = try! loadImage(at: bundledPath)
```

リソースバンドル同梱ファイルの読み込みなど、失敗が論理的にあり得ない場面に限定する。

## defer

スコープ脱出時に必ず実行されるクリーンアップ。

```swift
func processFile(named name: String) throws {
    let file = try open(name)
    defer { close(file) }       // throw / return / 正常終了いずれでも実行
    try work(with: file)
}
```

- 複数の `defer` は宣言とは逆順に実行される。
- `defer` の中で `throw` してはいけない（コンパイル時に許されない）。

## rethrows

クロージャ引数の throw のみを伝播する関数に使う。

```swift
func map<T>(_ transform: (Element) throws -> T) rethrows -> [T]
```

- 呼び出し側がスローしないクロージャを渡せば、その呼び出しは `try` 不要になる。
- 関数自身が throw する場合は使えない。

## Result

非同期コールバックやストレージ用に、成否と値を 1 値として持ち回りたい場合は `Result<Success, Failure>` を使う。

```swift
func fetch(completion: (Result<Data, NetworkError>) -> Void)

switch result {
case .success(let data): handle(data)
case .failure(let error): show(error)
}
```

- `result.get()` で値を取り出すと throw 形式に戻せる。
- `Result(catching:)` で throws 関数から Result を作れる。
- Swift Concurrency 普及後、新規 API は `async throws` を優先し、Result は境界（コールバック API、データ永続化）に限定するのが推奨。

## 使い分け

| 状況 | 使うもの |
| --- | --- |
| 失敗を呼び出し側に強制的に意識させたい | `throws` |
| 失敗時の値が不要、有無のみ | `try?` |
| 失敗が論理的にあり得ない | `try!`（最小限） |
| プログラム不変条件の違反 | `precondition` / `fatalError` |
| コールバック API、Combine、ストレージ | `Result` |
| クロージャを受け取る高階関数で透過的に伝播 | `rethrows` |

## アンチパターン

- 文字列メッセージだけの `Error`（`struct GenericError: Error { let message: String }` を量産）→ ケースを列挙する。
- `try!` の常用 → 失敗をコンパイラに伝えられない。
- 例外を吸って `print` で終わる `catch` → ログ手段とエラー伝播を分離する。
- 上位レイヤで下位の型を漏らす → レイヤ境界で適切な型に変換（包む）する。

## References

- https://docs.swift.org/swift-book/documentation/the-swift-programming-language/errorhandling/
