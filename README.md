# FanLauncher

Android 29+ 向けの右端スワイプ式の扇状ランチャーです。

## 技術構成
- Kotlin
- minSdk 29 / targetSdk 34
- AndroidX
- Palette API
- AccessibilityService.takeScreenshot()
- ML Kit Text Recognition (Japanese)
- ML Kit Translation (日本語→英語)

## ビルド
1. Android Studio でリポジトリを開く
2. JDK 17 を選択
3. Gradle Sync
4. Android 11+ の実機を接続
5. app を Run

## 初回設定
1. 「他のアプリの上に表示」を許可
2. アクセシビリティ設定で FanLauncher を有効化
3. Android 13+ では通知権限を許可

## 操作
右端48dpから左へ24dp以上スワイプすると扇状ランチャーが開きます。
指を離した時点のセクターだけが実行対象です。
選択中のセクターはアルファ255、それ以外は180です。

## 既知の制限
- 右端の戻るジェスチャーとの競合を避けるため48dpをジェスチャー除外領域として扱います。端末によって挙動差があります。
- スクリーンショットはAndroid 11 (API 30)以降が前提です。
- DRMやFLAG_SECUREで保護された画面は取得できない場合があります。
- 翻訳モデルは初回利用時にダウンロードされます。Wi-Fi推奨です。
- オーバーレイサービスは端末によって電池最適化の影響を受ける場合があります。
- Play公開時のQUERY_ALL_PACKAGESは使用せず、ランチャーIntentから候補アプリを列挙します。

## 拡張ポイント
OverlayService.buildItems() の候補取得部分を変更することで、表示するアプリを任意に絞り込めます。
現在はランチャーアプリを最大8個表示し、Screenshot / Translateを追加します。
