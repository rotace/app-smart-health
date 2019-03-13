# app-smart-health

体重計からArduinoを用いて体重を読み取り、BluetoothのSPPモード(シリアル通信)を用いてAndroidアプリへ体重情報を送信する。また、AndroidアプリからGoogleFitへアクセスし、体重情報を記録する。インストールの流れは、以下のとおり。

1. Androidアプリのインストール（開発者モードのスマホに Android Studio を用いてインストール）
1. OAuthの設定（GoogleFitにアクセスするために必要）
1. Arduinoプログラムのインストール（ Arduino Pro Mini に Arduino IDE を用いてインストール）
1. 体重計とArduinoとBluetoothモジュールを結線
1. AndroidアプリとBluetoothモジュールをペアリング

また、デバッグ用アプリケーション（python）の機能は、以下のとおり。
1. GoogleFitへREST_APIを用いてアクセスし、記録内容を確認する。

## 関連WEBサイト
* [Google Fit](https://fit.google.com)
* [Google Cloud Console](https://console.cloud.google.com/)

## OAuthの設定
参考文献：[グーグルのAPIを使うときに欠かせないGoogle OAuthの作り方と使い方](http://www.atmarkit.co.jp/ait/articles/1509/15/news017.html)  


全体の流れとしては、
1. OAuthを作成するために必要なフィンガープリントを取得しておく。
1. 利用したいアカウントの[Google Cloud Console](https://console.cloud.google.com/)にアクセスし、必要事項を入力してOAuthを作成する。

となる。OAuth作成の際に以下の情報を入力する。詳細は参考文献を参照のこと。
| 入力事項 | 入力内容 |
|:-----------|:------------|
| アプリケーションの種類 | Android |
| パッケージ名 | com.example.rotac.appsmarthealth |
| 署名証明書フィンガープリント | 「keytool」を用いて取得する |

「keytool」は、opensslやopensshのssh-keygenのような、暗号化通信のための秘密鍵と公開鍵を生成するツールらしい。java同梱のツールであり、JRE又はJDKをインストールしているならば使うことができる。Android Studioに同梱のjavaはパスが通っていないので、以下の保存先から起動する。
* C:\Program Files\Android\Android Studio\jre\bin\keytool.exe

## 体重計とArduinoとBluetoothモジュールの結線方法
参考文献：[体重計をハックして無線化してみた](https://qiita.com/shozaburo/items/8c0aa7ad5c16878bc3c5)  

～作成中～