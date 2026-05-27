# Protocols / Protocol-Oriented Programming

プロトコルはメソッド・プロパティ等の要件を宣言する型。クラス・構造体・列挙型・actor が準拠できる。プロトコル拡張による既定実装と組み合わせる「プロトコル指向プログラミング（POP）」が Swift の基本姿勢。詳細は [The Swift Programming Language: Protocols](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/protocols/) を参照。

## 構文

```swift
protocol FullyNamed {
    var fullName: String { get }
}

struct Person: FullyNamed {
    var fullName: String
}
```

複数のプロトコルや親クラスは `,` で並べる: `class C: SuperClass, P1, P2 { ... }`。

## プロパティ要件

`{ get }` / `{ get set }` で取得・設定の必要を宣言する。実装側は計算プロパティでも stored プロパティでもよい（`{ get set }` の場合は `let` は不可）。

```swift
protocol SomeProtocol {
    var mustBeSettable: Int { get set }
    var doesNotNeedToBeSettable: Int { get }
    static var someTypeProperty: Int { get set }
}
```

## メソッド要件

```swift
protocol RandomNumberGenerator {
    func random() -> Double
}
```

- 型メソッド要件は `static func` を付ける。
- 値型で内部状態を変更するメソッドは `mutating func` で宣言する。クラス実装側では `mutating` 不要。

```swift
protocol Toggleable {
    mutating func toggle()
}
```

## イニシャライザ要件

```swift
protocol Named {
    init(name: String)
}

class Sub: Named {
    required init(name: String) { ... }
}
```

クラスで準拠するイニシャライザには `required` が必要（`final class` の場合は不要）。

## プロトコルを型として使う

プロトコルは「プロトコル型」として変数・引数・戻り値の型に使える。

```swift
let generator: any RandomNumberGenerator = LinearCongruentialGenerator()
```

- `any P` は存在型（実行時に動的ディスパッチ）。型情報が消えるためコストあり。
- ジェネリクスで `some P` を使うと不透明型として静的に解決され高速。
- パフォーマンス上は `some P`、ヘテロなコレクションが必要なときに `any P`。

```swift
func makeGenerator() -> some RandomNumberGenerator { ... }  // 不透明型
let generators: [any RandomNumberGenerator] = [...]          // 存在型コレクション
```

## デリゲーション

```swift
protocol DiceGameDelegate: AnyObject {
    func gameDidStart(_ game: DiceGame)
    func game(_ game: DiceGame, didEndRound round: Int)
    func gameDidEnd(_ game: DiceGame)
}

class DiceGame {
    weak var delegate: DiceGameDelegate?
}
```

- 通常は `AnyObject` 制約を付けて `weak` 参照を可能にする（循環参照を防ぐ）。

## 拡張による準拠追加

既存型に後付けで準拠させられる。

```swift
extension Int: TextRepresentable {
    var textualDescription: String { "The number \(self)" }
}
```

すでに要件を満たしている型は空の `extension` 宣言で準拠できる:

```swift
extension Hamster: TextRepresentable {}
```

## 合成された実装（Synthesized）

特定のプロトコルは Swift がコンパイラ合成する:

- `Equatable` / `Hashable` / `Comparable`: メンバが該当プロトコルに準拠する struct / enum。
- `Codable`: stored property がすべて Codable な struct / enum。

```swift
struct Vector: Hashable { var x: Double; var y: Double }
```

`extension` で空の準拠を書くだけで実装が得られる。

## プロトコル継承

```swift
protocol InheritingProtocol: SomeProtocol, AnotherProtocol {
    // 追加要件
}
```

## クラス専用プロトコル

```swift
protocol SomeClassOnlyProtocol: AnyObject { ... }
```

参照型でしか意味をなさない（デリゲートなど）プロトコルに使う。

## プロトコル合成

```swift
func wishHappyBirthday(to celebrator: Named & Aged) { ... }
```

複数プロトコルへの同時準拠を要求する。`typealias HasName = Named & Aged` のように別名化も可能。

## 関連型（Associated Types）

プロトコル内で「型のプレースホルダ」を宣言する。

```swift
protocol Container {
    associatedtype Item
    mutating func append(_ item: Item)
    var count: Int { get }
    subscript(i: Int) -> Item { get }
}

struct IntStack: Container {
    typealias Item = Int      // 推論可能なら省略可
    var items: [Int] = []
    mutating func append(_ item: Int) { items.append(item) }
    var count: Int { items.count }
    subscript(i: Int) -> Int { items[i] }
}
```

`associatedtype` に制約を付けられる:

```swift
protocol Container {
    associatedtype Item: Equatable
}
```

### 関連型制約と where

```swift
protocol Container {
    associatedtype Item
}

extension Container where Item: Equatable {
    func startsWith(_ item: Item) -> Bool { ... }
}

func allItemsMatch<C1: Container, C2: Container>(
    _ a: C1, _ b: C2
) -> Bool where C1.Item == C2.Item, C1.Item: Equatable { ... }
```

### Primary Associated Types（Swift 5.7+）

頻繁に絞り込まれる関連型は山括弧で書ける:

```swift
protocol Container<Item> { associatedtype Item }

func process(_ c: some Container<Int>) { ... }
let any: any Container<String> = ...
```

## 条件付き準拠（Conditional Conformance）

ジェネリック型に特定の制約下でのみ準拠を付与する。

```swift
extension Array: TextRepresentable where Element: TextRepresentable {
    var textualDescription: String { ... }
}
```

## プロトコル拡張（既定実装）

要件外のメソッドや、要件のデフォルト実装を提供できる。

```swift
extension RandomNumberGenerator {
    func randomBool() -> Bool { random() > 0.5 }
}

extension Collection where Element: Equatable {
    func allEqual() -> Bool {
        guard let first else { return true }
        return allSatisfy { $0 == first }
    }
}
```

- 準拠側でオーバーライドされた要件は動的ディスパッチ、拡張のみで定義された非要件メソッドは静的ディスパッチ（プロトコル型から呼ぶと既定実装が選ばれる）。

## プロトコル指向プログラミング（POP）の指針

1. **共通動作はプロトコル + 既定実装で表現**: 継承階層を作らずに振る舞いを共有する。
2. **値型ファースト**: 値セマンティクスとプロトコルを組み合わせる（[value-vs-reference.md](./value-vs-reference.md)）。
3. **小さく直交したプロトコルを合成**: 1 つの大きなプロトコルより、小さな複数プロトコルを `&` で合成する。
4. **`some` を優先、必要なら `any`**: 静的解決でコストを下げる。
5. **要件はインターフェース、振る舞いは拡張**: 要件最小化で実装側の負担を下げる。

## チェックリスト

- [ ] プロトコルは目的ごとに分割されているか（責務 1 つ）
- [ ] クラス専用なら `AnyObject` 制約があるか
- [ ] デリゲートは `weak` 参照されているか
- [ ] 関連型に必要な制約が付いているか
- [ ] `some` で済むのに `any` を使っていないか
- [ ] プロトコル拡張で既定実装を提供できる箇所はないか

## References

- https://docs.swift.org/swift-book/documentation/the-swift-programming-language/protocols/
- https://docs.swift.org/swift-book/documentation/the-swift-programming-language/generics/
- https://www.swift.org/documentation/api-design-guidelines/
