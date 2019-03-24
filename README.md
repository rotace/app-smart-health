# app-smart-health

体重計からArduinoを用いて体重を読み取り、BluetoothのSPPモード(シリアル通信)を用いてAndroidアプリへ体重情報を送信する。また、AndroidアプリからGoogleFitへアクセスし、体重情報を記録する。インストールの流れは、以下のとおり。

1. Androidアプリのインストール（開発者モードのスマホに Android Studio を用いてインストール）
1. OAuthの設定（GoogleFitにアクセスするために必要）
1. Arduinoプログラムのインストール（ Arduino Pro Mini に Arduino IDE を用いてインストール）
1. 体重計とArduinoとBluetoothモジュールを結線
1. AndroidとBluetoothモジュールをペアリング
1. Androidアプリから接続

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

![接続図](https://raw.github.com/wiki/rotace/app-smart-health/images/structure.png)

* 運用時構成は、実際にアプリと体重計をBluetoothで接続する構成
* デバッグ構成１は、Linuxと体重計をBluetoothで接続する構成（ゼロ点補正等はこの構成で行う）
* デバッグ構成２は、Linuxと体重計をUSBで接続する構成（arduinoの書き換えはこの構成で行う）

なお、デバッグ構成２ではBluetoothモジュールを外す必要があるが、面倒であればVCC端子のみ外せば良い。

## ゼロ点補正、勾配補正

1. デバッグ構成２で結線し、IS_DEBUGオプションを1にしてarduinoに書き込む。
1. デバッグ構成１で結線を行う。
1. Bluetooth Managerを起動する。
1. （ペアリング設定が残っていなければ、）LinuxとBluetoothのペアリングを行う。*1
1. Serial Portで接続する。/dev/rfcommX (Xは数字)にデバイスが生成される。
1. screenで接続する。($ sudo screen /dev/rfcommX 9600)
1. arduinoで読み取った電圧値や変換した体重値が1秒ごとに表示される。
1. ゼロ点補正は、体重計に乗らない状態でONを押し、表示される電圧値（0~1023）を読み取る。
1. 勾配補正は、体重計をOFFにして数秒待ったのち、体重計に乗って表示される電圧値（0~1023）と体重値（float値）を読み取る。
1. デバッグ構成２で結線し、先ほど読み取ったゼロ点電圧値と、それらから計算される電圧―体重勾配値（g/volt）を修正して、arduinoに書き込む。また、IS_DEBUGオプションを0に戻す。
1. 運用時構成で結線してテストする。

*1 LinuxとBluetoothのペアリングがうまくいかない場合は、下記のbluetoothctlを使ったペアリングを参照


## LinuxとBluetoothのペアリング方法
参考文献：[LinuxのコマンドラインでBluetooth接続](https://qiita.com/propella/items/6daf3c56e26f709b4141)

1. ターミナルでbluetoothctlを起動
1. [bluetooth]# list
1. [bluetooth]# show
1. [bluetooth]# scan on
1. [bluetooth]# devices
1. [bluetooth]# pair (device address)

