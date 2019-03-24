package com.example.rotac.appsmarthealth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

// 参考WEB https://qiita.com/kazhida/items/91a15a1cf8ec0c443dbb

public class MyBroadcastReceiver extends BroadcastReceiver {

    private static final String ACTION_INVOKED = "com.example.rotac.appsmarthealth.action_invoked";
    private static final String ID = "ID";
    private static final String VALUE = "VALUE";

    public static final int CMD_CONNECT_BLUETOOTH = 1000;
    public static final int STATE_NOT_CONNECTED = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_CONNECTED_WITH_WEIGHT = 4;

    public interface Callback {
        void onEventInvoked(int id, double value);
    }
    // onReceive()メソッドだと何をしているのかわからないので、
    // onReceive()からコールバックを呼ぶようにする（名前大事！）

    private Callback callback;

    private MyBroadcastReceiver( Context context, Callback callback) {
        super();
        this.callback = callback;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_INVOKED);

        context.registerReceiver(this, filter);
    }

    public static MyBroadcastReceiver register( Context context, Callback callback) {
        return new MyBroadcastReceiver(context, callback);
    }
    // コンストラクタでregisterReceiver()しているのだけど、
    // そんなこと想像しづらいので、コンストラクタはprivateにして、
    // register()というファクトリメソッドを用意する（名前大事！）

    @Override
    public void onReceive( Context context, Intent intent) {
        String action = intent.getAction();

        if (ACTION_INVOKED.equals(action)) {
            int id = intent.getIntExtra(ID, 0);
            double value = intent.getDoubleExtra(VALUE, 0.0);
            callback.onEventInvoked(id, value);
        }
    }
    // onReceive()内でインテントから必要な情報を引っ張り出して、コールバックを呼ぶ。

    public static void sendBroadcast( Context context, int id, double value) {
        Intent intent = new Intent(ACTION_INVOKED);
        intent.putExtra(ID, id);
        intent.putExtra(VALUE, value);

        context.sendBroadcast(intent);
    }
    // 必要な情報をインテントに詰め込んで、sendBroadcast()するクラスメソッドを用意しておけば、イベントの配信も簡単。
    // onReceive()メソッドの動作と合わせて、完全にインテントの存在を隠蔽してます。

    public void unregister(Context context) {
        if(context != null){
            context.unregisterReceiver(this);
        }
    }

}
