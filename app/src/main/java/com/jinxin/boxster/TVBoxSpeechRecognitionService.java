package com.jinxin.boxster;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.jinxin.boxster.util.LogUtil;


public class TVBoxSpeechRecognitionService extends Service {

    public static final String SMART_ECHO_ACTION_START = "com.rockchip.echoOnWakeUp.ACTION.START";
    public static final String SMART_ECHO_ACTION_WAKEUP = "com.rockchip.echoOnWakeUp.ACTION.CAE.WAKEUP";

    Boxster mBoxster;

    private boolean isEchoRunning = false;

    public TVBoxSpeechRecognitionService() {
        LogUtil.d("TVBoxSpeechRecognitionService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        LogUtil.d("TVBoxSpeechRecognitionService - onCreate");
        super.onCreate();
        mBoxster = new Boxster(getApplicationContext());
        mBoxster.start();
    }

    @Override
    public void onDestroy() {
        LogUtil.d("TVBoxSpeechRecognitionService - onDestroy");
        super.onDestroy();
        mBoxster.stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
            LogUtil.d("TVBoxSpeechRecognitionService - onStartCommand - " + action);
            if (SMART_ECHO_ACTION_START.equals(action)) {
                if (!isEchoRunning) {
                    mBoxster.startTtsOutput("电视盒子语音服务启动了", false);
                }
            } else if(SMART_ECHO_ACTION_WAKEUP.equals(action)) {
//                mBoxster.onWakeUp(0, 0);
                mBoxster.startIat();

            }
        }
        return START_STICKY;
    }
}
