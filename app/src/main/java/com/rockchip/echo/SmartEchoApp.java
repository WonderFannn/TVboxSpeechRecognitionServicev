package com.rockchip.echo;

import android.app.Application;
import android.content.Context;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

public class SmartEchoApp extends Application {
	public static Context context;
	@Override
	public void onCreate() {
		super.onCreate();
		context = getApplicationContext();
		SpeechUtility.createUtility(SmartEchoApp.this,
				SpeechConstant.APPID + "=" + getString(R.string.app_id));
	}

	public static Context getContext() {
		return context;
	}
}