# Kotlin 言語リファレンス

Kotlin 言語そのものに関するリファレンスドキュメント集です。プロジェクト固有の話題は含まず、純粋な言語仕様・慣習・標準ライブラリのみを扱います。Kotlin Multiplatform 固有のトピック（`expect`/`actual`、ソースセット構成など）は別ドキュメントセットを参照してください。

## ドキュメント一覧

| ファイル | 概要 |
| --- | --- |
| [idioms.md](./idioms.md) | データクラス・sealed クラス・スコープ関数・`when`・コレクション操作などの慣用的な書き方 |
| [null-safety.md](./null-safety.md) | `?`、`!!`、`?.`、`?:`、スマートキャスト、Java からのプラットフォーム型の扱い |
| [coroutines.md](./coroutines.md) | `suspend`、構造化された並行性、スコープ、ディスパッチャ、`launch`/`async`/`runBlocking`、キャンセル |
| [flow.md](./flow.md) | Flow / StateFlow / SharedFlow、コールド・ホット、オペレータ、テスト |
| [conventions.md](./conventions.md) | 公式コーディング規約（命名、書式、ファイル構成） |
| [value-classes.md](./value-classes.md) | `value class` の使いどころ、`data class` との違い、JVM / Swift 連携の注意点 |

## 参考

- Kotlin 公式ドキュメントトップ: https://kotlinlang.org/docs/home.html
