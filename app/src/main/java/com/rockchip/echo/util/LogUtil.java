package com.rockchip.echo.util;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Set;

/**
 * Created by yhc on 16-11-19.
 */

public class LogUtil {

    public static final String TAG = "TAG";

    public static void d(String s) {
        Log.d(TAG, s);
    }

    public static void dumpIntent(Intent i) {
        if(i == null) {
            return;
        }
        Bundle bundle = i.getExtras();
        Log.d(TAG, "-----------------------dump intent---------------------------");
        Log.d(TAG, "action: " + i.getAction());
        Log.d(TAG, "-------------------------------------------------------------");
        Log.d(TAG, "extras:");
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            for (String key : keys) {
                Log.d(TAG, "    " + key + "=" + bundle.get(key));
            }
        } else {
            Log.d(TAG, "    null");
        }
        Log.d(TAG, "-------------------------------------------------------------");
    }
}
