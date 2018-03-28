package com.rockchip.echo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.rockchip.echo.smartecho.SmartEcho;
import com.rockchip.echo.util.LogUtil;


public class SmartEchoService extends Service {

    public static final String SMART_ECHO_ACTION_START = "com.rockchip.echoOnWakeUp.ACTION.START";
    public static final String SMART_ECHO_ACTION_WAKEUP = "com.rockchip.echoOnWakeUp.ACTION.CAE.WAKEUP";

    SmartEcho mSmartEcho;

    private boolean isEchoRunning = false;

    public SmartEchoService() {
        LogUtil.d("SmartEchoService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        LogUtil.d("SmartEchoService - onCreate");
        super.onCreate();
        mSmartEcho = new SmartEcho(getApplicationContext());
        mSmartEcho.start();
    }

    @Override
    public void onDestroy() {
        LogUtil.d("SmartEchoService - onDestroy");
        super.onDestroy();
        mSmartEcho.stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
            LogUtil.d("SmartEchoService - onStartCommand - " + action);
            if (SMART_ECHO_ACTION_START.equals(action)) {
                if (!isEchoRunning) {
                    mSmartEcho.startTtsOutput("灵犀灵犀来了", false);
                }
            } else if(SMART_ECHO_ACTION_WAKEUP.equals(action)) {
                mSmartEcho.onWakeUp(0, 0);
            }
        }
        return START_STICKY;
    }
}
