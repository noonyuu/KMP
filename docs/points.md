# KMPにおけるdescription

## 概要

NSObject（およびそのサブクラス）には、Objective-Cランタイムが昔から持っているdescriptionというプロパティがある
これはJavaでいうtoString()相当で、デバッガやログ出力でオブジェクトを文字列表現するために使われる
SwiftのCustomStringConvertibleプロトコルのdescriptionもこれと地続き

## 例

``` Kotlin
data class Challenge(
    val id: String,
    val description: String,  // ← これ
)
```

KMPはKotlinのクラスをObjective-C/Swiftから使えるようにヘッダ（.h）を自動生成する
このときChallengeがNSObjectを継承する形になり、KotlinのdescriptionプロパティがObjective-CのNSObject.descriptionと同じ名前のメンバとしてぶつかる

## 問題点

名前が衝突した時にコンパイルで止まらず、挙動がわかりにくい形で壊れる

1. オーバーライド扱いになって型が合わない `NSObject.description` は `NSString` (非nullのString)を返す契約
Kotlin側の `description` がnullableだったりKMPのバージョン次第で生成ヘッダの整合が崩れ `description` を呼ぶとKotlinで入れた値ではなく `"Challenge(id=...,description=...))"` のような `toString()` の結果がかっえってくることがある
Swift側で `challenge.description` と書いた人は「フィールドの値が取れるはず」と思っているのでデバッグで混乱する
2. ビルドエラーになるケースもありKMPのバージョン設定によっては `Kotlin propety 'description' clashes with Objective-C method` のような警告/エラーが出る

`description` のほかにもikamo 
NSObject由来の予約的メンバ
- hash
- debugDescription
- isEqual
- copy
- init
etc