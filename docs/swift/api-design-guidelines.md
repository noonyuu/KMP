# Swift API Design Guidelines

Apple 公式の [Swift API Design Guidelines](https://www.swift.org/documentation/api-design-guidelines/) に基づく要点まとめ。

## 基本原則（Fundamentals）

| 原則 | 内容 |
| --- | --- |
| **Clarity at the point of use** | 宣言は一度きりだが利用箇所は繰り返される。利用側の読みやすさを最優先する |
| **Clarity is more important than brevity** | 簡潔さよりも明快さ。短くするためだけに省略しない |
| **Write a documentation comment for every declaration** | すべての宣言にドキュメントコメントを書く。説明できないなら設計を見直す |

## 命名（Naming）

### 明確な使用感を促す（Promote Clear Usage）

- **必要な語は省略しない**: 曖昧さを避ける語は残す。

  ```swift
  // OK
  employees.remove(at: x)
  // NG（何を remove するのか曖昧）
  employees.remove(x)
  ```

- **冗長な語は省く**: 型情報から自明な語は重複させない。

  ```swift
  // OK
  allViews.remove(cancelButton)
  // NG
  allViews.removeElement(cancelButton)
  ```

- **役割で命名する、型では命名しない**: `string` ではなく `greeting`、`widgetFactory` ではなく `supplier`。
- **弱い型情報を補う**: `Any` / `NSObject` などには説明的な前置詞を付ける。

  ```swift
  // 例: addObserver(_:for:) のように for: が補足する
  ```

### 流暢な使用感を目指す（Strive for Fluent Usage）

- 呼び出し側が英文として読めるように設計する。

  ```swift
  x.insert(y, at: z)          // "x, insert y at z"
  x.subViews(havingColor: y)  // "x's subviews having color y"
  ```

- **ファクトリメソッドは `make` で始める**: `x.makeIterator()`。
- **副作用の有無で文法を変える**:
  - 副作用なし → 名詞句（`x.distance(to: y)`、`i.successor()`）
  - 副作用あり → 命令形動詞（`x.sort()`、`x.append(y)`）

### Mutating / Nonmutating のペア

| 種別 | Mutating | Nonmutating |
| --- | --- | --- |
| 動詞由来 | `x.sort()` | `x.sorted()` |
| 名詞由来 | `x.formUnion(y)` | `x.union(y)` |
| 副作用 | `print(x)` を返さない | — |

ed / ing で nonmutating 版を作るのが基本。

### 用語を正しく使う（Use Terminology Well）

- **一般語で済むなら専門用語を使わない**: `skin` で十分なところに `epidermis` を出さない。
- **既存の用語は厳密な意味で使う**: 技術用語の意味を捻じ曲げない。
- **既存の慣習に従う**: `Array`（一般的）を `List` に置き換えない。

## 規約（Conventions）

### 一般

- O(1) でない計算プロパティはドキュメントに計算量を書く。
- 自由関数より型のメソッド・プロパティを優先する（ただし `min(x, y, z)` のように特定の型に属さないもの、`print`、ジェネリックな自由関数は例外）。
- ケース規約:
  - 型・プロトコル: `UpperCamelCase`
  - それ以外: `lowerCamelCase`

### パラメータ

- パラメータ名はドキュメントの一部。明確な名前を付ける。
- 共通の単一値にはデフォルト引数を活用する。
- デフォルトを持つパラメータは末尾に置く。

### 引数ラベル（Argument Labels）

```swift
func functionName(argumentLabel parameterName: Type)
```

ルール:

| ケース | ラベル |
| --- | --- |
| 区別する意味がない（同じ抽象レベルのピア） | 省略する: `min(number1, number2)` |
| 値を保つ型変換 | 第一引数のラベルを省略: `Int64(someUInt32)` |
| 第一引数が前置詞句を形成 | 前置詞をラベルにする: `removeBoxes(havingLength:)` |
| 第一引数が文法句の一部 | ラベル省略: `view.addSubview(_:)` |
| その他 | 必ずラベルを付ける |

デフォルト引数を持つパラメータも原則すべてラベルを付ける。

## 特別な指針

- **タプルメンバ・クロージャ引数に意味のある名前を付ける**: 説明力を上げる。
- **無制約な多相を慎重に扱う**: `contains(_:)` のような汎用 API は曖昧さを生むため、`append(contentsOf:)` のような明示的命名で衝突を避ける。
- **頭字語は通常通り大文字小文字を統一**: `urlString`（lowerCamelCase の冒頭）、`HTTPHeader`（UpperCamelCase）。

## チェックリスト

- [ ] 利用箇所で英文として読めるか
- [ ] 第一引数のラベル省略が文法的に正しいか
- [ ] mutating / nonmutating のペアが命名規則に従っているか（ed/ing or form-prefix）
- [ ] 副作用のあるメソッドが命令形動詞になっているか
- [ ] 型名や冗長な語を含んでいないか
- [ ] すべての宣言にドキュメントコメントがあるか

## References

- https://www.swift.org/documentation/api-design-guidelines/
