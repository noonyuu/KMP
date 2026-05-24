# KMPについて

## 比較

### Flutter

UIも含めて完全共通化することを前提としたフレームワーク

### KMP

UIは各プラットフォームで作成し、ロジックのみを共有する

## Compose Multiplatform

Compose UI を複数プラットフォームで使用する
思想はFlutterだがKotlin + Composeベース

## なぜKMPを使う

iOS(Swift)とAndroid(kotlin)で全く同じ仕様のロジックを書く必要があった
KMPはKotlinを使用し特定のプラットフォーム（iOSやAndroidなど）に依存しない共通コードを書くための技術