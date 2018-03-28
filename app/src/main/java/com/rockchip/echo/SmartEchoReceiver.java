package com.rockchip.echo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.rockchip.echo.util.LogUtil;

public class SmartEchoReceiver extends BroadcastReceiver {

    public static final String SMART_ECHO_BROADCAST_ACTION_WAKEUP =
            "com.rockchip.caedemo.wakeup";

    public SmartEchoReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtil.d("SmartEchoReceiver - onReceive");
        if(intent == null) {
            return;
        }
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("rk", "==================== SmartEchoService Start ======================");
            Intent i = new Intent(context, SmartEchoService.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(SmartEchoService.SMART_ECHO_ACTION_START);
            context.startService(i);
        }else if (SMART_ECHO_BROADCAST_ACTION_WAKEUP.equals(intent.getAction())) {
            Log.d("rk", "==================== SmartEchoService Wakeup ======================");
            Intent i = new Intent(context, SmartEchoService.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(SmartEchoService.SMART_ECHO_ACTION_WAKEUP);
            context.startService(i);
        }
    }
}
