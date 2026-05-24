# iosApp

<!-- このファイルはプロジェクト固有の規約・コンテキストを記述する。
     全SwiftUI共通の規約は ~/.claude/CLAUDE.md に集約済み。重複は書かない。 -->

## プロジェクト概要

<!-- 1-3 文で何のアプリか -->

## 構成

- 最低デプロイメントターゲット: iOS 26
- アーキテクチャ: MV
- 状態管理: `@Observable`
- テスト: Swift Testing
- パッケージ管理: SPM

## モジュール構成

<!-- マルチモジュールの場合のみ記載。シングルモジュールなら削除 -->

```
Sources/
├── App/
├── Features/
├── Models/
├── Services/
└── Resources/
```

## 主要な依存

<!-- 外部 SPM パッケージとその用途 -->

## ビルド・テスト

```sh
swift build
swift test
swiftformat . && swiftlint --fix --quiet
```

## このプロジェクト固有の判断・制約

<!-- 例: API スキーマの場所、特定のドメインルール、外せない設計判断など。
     全SwiftUI共通の規約は書かない（~/.claude/CLAUDE.md に既にある）。 -->

## 関連リンク

<!-- 設計ドキュメント / API ドキュメント / Figma など -->
