package com.jinxin.boxster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jinxin.boxster.util.LogUtil;


public class TVBoxSpeechRecognitionReceiver extends BroadcastReceiver {

    public static final String SMART_ECHO_BROADCAST_ACTION_WAKEUP =
            "com.rockchip.caedemo.wakeup";


    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtil.d("TVBoxSpeechRecognitionReceiver - onReceive");
        if(intent == null) {
            return;
        }
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("rk", "==================== TVBoxSpeechRecognitionService Start ======================");
            Intent i = new Intent(context, TVBoxSpeechRecognitionService.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(TVBoxSpeechRecognitionService.SMART_ECHO_ACTION_START);
            context.startService(i);
        }else if (SMART_ECHO_BROADCAST_ACTION_WAKEUP.equals(intent.getAction())) {
            Log.d("rk", "==================== TVBoxSpeechRecognitionService Wakeup ======================");
            Intent i = new Intent(context, TVBoxSpeechRecognitionService.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(TVBoxSpeechRecognitionService.SMART_ECHO_ACTION_WAKEUP);
            context.startService(i);
        }
    }
}
