package com.rockchip.echo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.http.util.EncodingUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.iflytek.cae.util.res.ResourceUtil;
import com.iflytek.cae.util.res.ResourceUtil.RESOURCE_TYPE;
import com.rockchip.echo.smartecho.SmartEcho;
import com.rockchip.echo.smartecho.textunderstand.TextUnderstandResult;
import com.rockchip.echo.util.JsonParser;
import com.rockchip.echo.util.LogUtil;
import com.rockchip.echo.smartecho.audio.PcmRecorder;
import com.rockchip.echo.smartecho.audio.PcmRecorder.PcmListener;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.TextUnderstander;
import com.iflytek.cloud.TextUnderstanderListener;
import com.iflytek.cloud.UnderstanderResult;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SmartEchoActivity extends Activity {
	// 唤醒资源路径
	String mResPath;
	
	EditText mResultEdit;
	Button mRecognizeButton;
	
	// 音频抽取线程
//	ExtractAudioThread mExtractThread;
	// 是否开始抽取
//	boolean mStartExtract;
	// 是否开始识别
	boolean mStartRecognize;
	boolean misOnTts = false;

	FileObserver mFileObserver;

	static final String CAE_WAKEUP_FILE = "/data/cae_wakeup";

//	CAEEngine mCaeEngine;
//
//	CAEListener mCaeListener = new CAEListener() {
//
//		@Override
//		public void onWakeup(String jsonResult) {
//			LogUtil.d("CAEListener - onWakeup()");
//			mStatusText.setText("状态：已唤醒，可进行识别");
//			mResultEdit.setText(jsonResult);
//		}
//
//		@Override
//		public void onError(CAEError error) {
//			// TODO Auto-generated method stub
//
//		}
//
//		@Override
//		public void onAudio(byte[] audio, int audioLen, int param1, int param2) {
//			LogUtil.d("CAEListener - onAudio()");
//			if (mStartRecognize) {
//				// 写入16K采样率音频，开始听写
//				LogUtil.d("CAEListener - writeAudio to iat");
//				mIat.writeAudio(audio, 0, audioLen);
//			}
//		}
//	};
	
	PcmRecorder mRecorder;
	
	// mPcmListener用于抛出96K采样的阵列原始音频
	PcmListener mPcmListener = new PcmListener() {

		@Override
		public void onPcmRate(long bytePerMs) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onPcmData(byte[] data, int dataLen) {
			// 建议不要在读音频线程中做耗时的同步操作，否则会导致音频数据读出不及时造成AudioRecord中的缓存溢出。
			// 所以以下的两个writeAudio都是采用的异步
//			write2File(data);
			if (mStartRecognize && !misOnTts) {
//				LogUtil.d("mIat.isListening(): " + mIat.isListening());
//				LogUtil.d("=============== onPcmData - mIat.writeAudio(data, 0, dataLen)");
//				if(!mIat.isListening()) {
//					int ret = mIat.startListening(mIatListener);
//					if(ret != ErrorCode.SUCCESS){
//						LogUtil.d("========= startListening() error: " + ret);
//					}
//				}
				// 写入16K采样率音频，开始听写
				mIat.writeAudio(data, 0, dataLen);

			}
//			if (null != mCaeEngine) {
//				// 将从阵列读取的96K采样的音频写入CAE引擎
//				mCaeEngine.writeAudio(data, dataLen);
//
//				if (mStartExtract && null != mExtractThread) {
//					mExtractThread.writeAudio(data);
//				}
//			}
		}
	};

//	int count = 0;
//	File path = null;
//	BufferedOutputStream bos = null;
//
//	private void write2File(byte[] data) {
//		if(path == null) {
//			path = new File(Environment.getExternalStorageDirectory(), "test.pcm");
//			LogUtil.d("============== write2File - path: " + path.getAbsolutePath());
//		}
//		if(bos == null) {
//			try {
//				bos = new BufferedOutputStream(new FileOutputStream(path));
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
//		}
//		try {
//			if(count < 1000) {
//				count++;
//				bos.write(data);
//				LogUtil.d("============= Write2File - write to BufferedOutputStream, count: " + count);
//			} else if (count == 1000) {
//				count++;
//				bos.flush();
//				bos.close();
//				LogUtil.d("============= Write2File done, close file");
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void resetWrite2File() {
//		LogUtil.d("============= resetWrite2File");
//		count = 0;
//		path = null;
//		bos = null;
//	}

//	 听写对象
	SpeechRecognizer mIat;
	
	// 听写监听器
	RecognizerListener mIatListener = new RecognizerListener() {
		
		@Override
		public void onVolumeChanged(int arg0, byte[] arg1) {
			LogUtil.d("====== mIatListener onVolumeChanged");
		}
		
		@Override
		public void onResult(RecognizerResult result, boolean isLast) {
			LogUtil.d("====== mIatListener onResult");
			printResult(result);

			if(isLast) {
				LogUtil.d("====== It is last result, so stop recognizer");
//				mIat.stopListening();

				setRecognizerState(false);
//				LogUtil.d("====== mIat.stopListening()");
//				mRecorder.startRecording(mPcmListener);
//				LogUtil.d("====== mRecorder.startRecording(mPcmListener), wait for wakeup...");
			}
		}
		
		@Override
		public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
			LogUtil.d("====== mIatListener onEvent");
		}
		
		@Override
		public void onError(SpeechError arg0) {
			LogUtil.d("====== mIatListener onError");
			setRecognizerState(false);
		}
		
		@Override
		public void onEndOfSpeech() {
			LogUtil.d("====== mIatListener onEndOfSpeech");
//			mStartRecognize = false;
//			LogUtil.d("====== mStartRecognize = false");
		}
		
		@Override
		public void onBeginOfSpeech() {
			LogUtil.d("====== mIatListener onBeginOfSpeech");
		}
	};

	int mAngle = -1;
	int mChanel = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LogUtil.d("SmartEchoActivity - start SmartEchoService");
		setContentView(R.layout.activity_main);
		Intent i = new Intent(this.getApplicationContext(), SmartEchoService.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setAction(SmartEchoService.SMART_ECHO_ACTION_START);
		startService(i);
//		// assets目录下的资源路径
//		mResPath = ResourceUtil.generateResourcePath(SmartEchoActivity.this, RESOURCE_TYPE.assets, "lingxilingxi.jet");
//
//		mRecorder = new PcmRecorder();
//		mIat = SpeechRecognizer.createRecognizer(SmartEchoActivity.this, null);
//		setIatParam();
//
//		mToast = Toast.makeText(this,"",Toast.LENGTH_SHORT);
//
//		mTextUnderstander = TextUnderstander.createTextUnderstander(SmartEchoActivity.this, mTextUdrInitListener);
//
//		// 初始化合成对象
//		mTts = SpeechSynthesizer.createSynthesizer(SmartEchoActivity.this, mTtsInitListener);
//
////		LogUtil.d("====== mIat.startListening(mIatListener)");
////		mIat.startListening(mIatListener);
//
		initUI();
//
//		mFileObserver = new FileObserver(CAE_WAKEUP_FILE) {
//			@Override
//			public void onEvent(int i, String s) {
//				if(i == FileObserver.MODIFY) {
//					LogUtil.d("====== " + CAE_WAKEUP_FILE + " has been modify, read it go!");
//					getCAEWakeFileInfo();
//					if(mStartRecognize) {
//						// tts output echoOnWakeUp word
//						startTtsOutput(getEchoText());
//						misOnTts = true;
//					}
//				}
//			}
//		};
//		LogUtil.d("====== mRecorder.startRecording(mPcmListener), wait for wakeup...");
//		mRecorder.startRecording(mPcmListener);
//		startTtsOutput("你好 我叫灵犀灵犀", true);
	}

	int mEchoIndex = 0;
	public static final String[] ECHO_TEXT_ARRAY = {
			"在呢",
			"我在",
			"在"
	};
	private String getEchoText() {
		mEchoIndex++;
		if(mEchoIndex > 2) {
			mEchoIndex = 0;
		}
		return ECHO_TEXT_ARRAY[mEchoIndex];
	}

	public void getCAEWakeFileInfo() {
		try {
			String cae_wakeup_file_str = readFile(CAE_WAKEUP_FILE);
			LogUtil.d("===== read " + CAE_WAKEUP_FILE +" : " + cae_wakeup_file_str);
			String[] temp_str = cae_wakeup_file_str.split(" ");
			if(temp_str != null && !cae_wakeup_file_str.equals("")) {
				if(temp_str[0] != null && !temp_str[1].equals("")) {
					if("true".equals(temp_str[0])) {
						mStartRecognize = true;
					} else {
						mStartRecognize = false;
					}
				}
				if(temp_str[1] != null && !temp_str[1].equals("")) {
					mAngle = Integer.parseInt(temp_str[1]);
				}
				if(temp_str[2] != null && !temp_str[1].equals("")) {
					mChanel = Integer.parseInt(temp_str[2]);
				}
				LogUtil.d("===== mStartRecognize: " + mStartRecognize + " mAngle: "
						+ mAngle + " mChanel: " + mChanel);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setCaeWakeupFileInfo() {
		String write_str = mStartRecognize + " " + mAngle + " " + mChanel;
		LogUtil.d("====== update " + CAE_WAKEUP_FILE + " : " + write_str);
		try{
			FileOutputStream fout = new FileOutputStream(CAE_WAKEUP_FILE);
			byte [] bytes = write_str.getBytes();
			fout.write(bytes);
			fout.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public String readFile(String fileName) throws IOException{
		String res="";
		try{
			FileInputStream fin = new FileInputStream(new File(CAE_WAKEUP_FILE));
			int length = fin.available();
			byte [] buffer = new byte[length];
			fin.read(buffer);
			res = EncodingUtils.getString(buffer, "UTF-8");
			fin.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return res;
	}

	private void setRecognizerState(boolean state) {
		mStartRecognize = state;
		LogUtil.d("====== update mStartRecognize=" + mStartRecognize);
		setCaeWakeupFileInfo();
	}

	void initUI() {
		mResultEdit = (EditText) findViewById(R.id.edt_result);
		mRecognizeButton = (Button) findViewById(R.id.btn_recognize);

		mRecognizeButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				Intent i = new Intent();
				i.setAction(SmartEchoReceiver.SMART_ECHO_BROADCAST_ACTION_WAKEUP);
				SmartEchoActivity.this.sendBroadcast(i);
			}
		});

//		mRecognizeButton.setOnTouchListener(new View.OnTouchListener() {
//
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				switch (event.getAction()) {
//				case MotionEvent.ACTION_DOWN:
//					LogUtil.d("========== mRecognizeButton ontouch ACTION_DOWN");
//					startTtsOutput(getEchoText());
//					misOnTts = true;
//					break;
//
//				case MotionEvent.ACTION_UP:
//					LogUtil.d("========== mRecognizeButton ontouch ACTION_DOWN");
//					mStartRecognize = false;
//					mIat.stopListening();
//					break;
//
//				default:
//					break;
//				}
//
//				v.performClick();
//
//				return false;
//			}
//		});
		
	}

	CAEWakeupReceiver caeWakeupReceiver;

	@Override
	protected void onResume() {
		super.onResume();
//		if(caeWakeupReceiver == null) {
//			caeWakeupReceiver = new CAEWakeupReceiver();
//		}
//		LogUtil.d("====== registerReceiver(caeWakeupReceiver, new IntentFilter(\"com.rockchip.caedemo.wakeup\"))");
//		registerReceiver(caeWakeupReceiver, new IntentFilter("com.rockchip.caedemo.wakeup"));
//		mFileObserver.startWatching();
	}

	@Override
	protected void onPause() {
		super.onPause();
//		LogUtil.d("====== unregisterReceiver(caeWakeupReceiver)");
//		unregisterReceiver(caeWakeupReceiver);
//		mFileObserver.stopWatching();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
//		if (null != mRecorder) {
//			mRecorder.stopRecording();
//		}
//
//		if(mTextUnderstander.isUnderstanding())
//			mTextUnderstander.cancel();
//		mTextUnderstander.destroy();
//
////		if (null != mCaeEngine) {
////			mCaeEngine.destroy();
////			mCaeEngine = null;
////		}
		
	}

	public void startIat() {
		mResultEdit.setText("");
		mStartRecognize = true;
		// start listening user
		if(!mIat.isListening()) {
			mIat.startListening(mIatListener);
		}
	}

	public void stopIat() {
		mStartRecognize = false;
		if(mIat.isListening()) {
			mIat.stopListening();
		}
	}
	
	public void setIatParam() {
		// 清空参数
		mIat.setParameter(SpeechConstant.PARAMS, null);
		// 设置听写引擎
		mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
		//
		mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
		// 设置返回结果格式
		mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
		// 设置语言
		mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
		// 设置语言区域
		mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
		// 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
		mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
		// 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
		mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
		// 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
		mIat.setParameter(SpeechConstant.ASR_PTT, "1");
		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
		
		mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
		
		mIat.setParameter(SpeechConstant.NOTIFY_RECORD_DATA, "0");

		mIat.setParameter("domain", "fariat");
	}
	
	// 用HashMap存储听写结果
	HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
	
	private void printResult(RecognizerResult results) {
		String text = JsonParser.parseIatResult(results.getResultString());

		String sn = null;
		// 读取json结果中的sn字段
		try {
			JSONObject resultJson = new JSONObject(results.getResultString());
			sn = resultJson.optString("sn");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mIatResults.put(sn, text);

		StringBuffer resultBuffer = new StringBuffer();
		for (String key : mIatResults.keySet()) {
			resultBuffer.append(mIatResults.get(key));
		}

		String resultStr = resultBuffer.toString();

		LogUtil.d("======== result: " + resultStr);

		if(" ".equals(resultStr) || "。 ".equals(resultStr)) {
			LogUtil.d("====== skip result: " + resultStr);
			return;
		}

		mResultEdit.setText(resultStr);
		mResultEdit.setSelection(mResultEdit.length());

		int ret = mTextUnderstander.understandText(text, mTextUnderstanderListener);
		if(ret != 0)
		{
			LogUtil.d("语义理解失败,错误码:"+ ret);
		}
	}

	private void understandResult(RecognizerResult results) {
		String text = JsonParser.parseIatResult(results.getResultString());

		String sn = null;
		// 读取json结果中的sn字段
		try {
			JSONObject resultJson = new JSONObject(results.getResultString());
			sn = resultJson.optString("sn");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mIatResults.put(sn, text);

		StringBuffer resultBuffer = new StringBuffer();
		for (String key : mIatResults.keySet()) {
			resultBuffer.append(mIatResults.get(key));
		}

//		LogUtil.d("======== result: " + resultBuffer.toString());

		mResultEdit.setText(resultBuffer.toString());
		mResultEdit.setSelection(mResultEdit.length());

		int ret = mTextUnderstander.understandText(text, mTextUnderstanderListener);
		if(ret != 0)
		{
			LogUtil.d("语义理解失败,错误码:"+ ret);
		}
	}

	private class CAEWakeupReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			LogUtil.d("========= CAEWakeupReceiver - onReceive");
			LogUtil.dumpIntent(intent);
//			LogUtil.d("====== mRecorder.stopRecording()");
//			mRecorder.stopRecording();
			mStartRecognize = true;
			LogUtil.d("====== mStartRecognize = true");
			if(!mIat.isListening()) {
				mIat.startListening(mIatListener);
				LogUtil.d("====== mIat.startListening(mIatListener)");
			}
		}
	}

	// 语义理解对象（文本到语义）
	private TextUnderstander mTextUnderstander;

	/**
	 * 初始化监听器（文本到语义）。
	 */
	private InitListener mTextUdrInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			LogUtil.d("textUnderstanderListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				Toast.makeText(getApplicationContext(), "初始化失败,错误码："+code, Toast.LENGTH_SHORT)
						.show();
				LogUtil.d("====== mTextUdrInitListener - onInit - init error, code: " + code);
			}
		}
	};

	private TextUnderstanderListener mTextUnderstanderListener = new TextUnderstanderListener() {

		@Override
		public void onResult(final UnderstanderResult result) {
			LogUtil.d("========= TextUnderstanderListener - onResult =========");
			if (null != result) {
				// 显示
				String UnderstandText = result.getResultString();
				if (!TextUtils.isEmpty(UnderstandText)) {
//					String showtext = mResultEdit.getText().toString();
//					showtext += "\r\n";
//					showtext += UnderstandText;
//					mResultEdit.setText(showtext);
					LogUtil.d(UnderstandText);

					TextUnderstandResult textUnderstandResult = new TextUnderstandResult(UnderstandText);
					String ttsText = textUnderstandResult.getTtsText();
					LogUtil.d("====== ttsText: " + ttsText);
					String resultedit = mResultEdit.getText().toString();
					resultedit += "\r\n\r\n";
					resultedit += ttsText;
					mResultEdit.setText(resultedit);
					mResultEdit.setSelection(mResultEdit.length());
					if(ttsText != null && !ttsText.equals("")) {
						startTtsOutput(ttsText);
					}
				}
			} else {
				LogUtil.d("understander result:null");
			}
			LogUtil.d("========================================================");
		}

		@Override
		public void onError(SpeechError error) {
			// 文本语义不能使用回调错误码14002，请确认您下载sdk时是否勾选语义场景和私有语义的发布
			LogUtil.d("TextUnderstanderListener - onError Code："	+ error.getErrorCode());
			if(error.getErrorCode() == 10114) {
				Toast.makeText(SmartEchoActivity.this.getApplicationContext(),
						"网络有问题，请检查网络连接", Toast.LENGTH_LONG).show();
			}
		}
	};

	// 语音合成对象
	private SpeechSynthesizer mTts;
	// 默认发音人
	private String voicer = "vinn";
//	private String voicer = "aisxrong";

	// 缓冲进度
	private int mPercentForBuffering = 0;
	// 播放进度
	private int mPercentForPlaying = 0;

	// 引擎类型
	private String mEngineType = SpeechConstant.TYPE_CLOUD;

	private Toast mToast;

	/**
	 * 初始化监听。
	 */
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			LogUtil.d("InitListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败,错误码："+code);
			} else {
				// 初始化成功，之后可以调用startSpeaking方法
				// 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
				// 正确的做法是将onCreate中的startSpeaking调用移至这里
			}
		}
	};

	private int startTtsOutput(String text, boolean disableSpeechRecog) {
		misOnTts = disableSpeechRecog;
		return startTtsOutput(text);
	}

	private int startTtsOutput(String text) {
		// 设置参数
		setParam();
		int code = mTts.startSpeaking(text, mTtsListener);
		if (code != ErrorCode.SUCCESS) {
			showTip("语音合成失败,错误码: " + code);
		}
		return code;
	}

	/**
	 * 合成回调监听。
	 */
	private SynthesizerListener mTtsListener = new SynthesizerListener() {

		@Override
		public void onSpeakBegin() {
			showTip("tts - start play");
		}

		@Override
		public void onSpeakPaused() {
			showTip("tts - pause play");
		}

		@Override
		public void onSpeakResumed() {
			showTip("tts - resume play");
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos,
									 String info) {
			// 合成进度
			mPercentForBuffering = percent;
			showTip(String.format(getString(R.string.tts_toast_format),
					mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			// 播放进度
			mPercentForPlaying = percent;
			showTip(String.format(getString(R.string.tts_toast_format),
					mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error == null) {
				showTip("tts - play completed");
				if(misOnTts) {
					new Handler().postDelayed(new Runnable(){
						public void run() {
							misOnTts = false;
							startIat();
						}
					}, 500);
				}
			} else if (error != null) {
				showTip(error.getPlainDescription(true));
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			//		Log.d(TAG, "session id =" + sid);
			//	}
		}
	};

	private void showTip(final String str) {
		LogUtil.d(str);
//		mToast.setText(str);
//		mToast.show();
	}

	private void setParam() {
		// 清空参数
		mTts.setParameter(SpeechConstant.PARAMS, null);
		// 根据合成引擎设置相应参数
		if(mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
			mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
			// 设置在线合成发音人
			mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
			//设置合成语速
			mTts.setParameter(SpeechConstant.SPEED, "50");
			//设置合成音调
			mTts.setParameter(SpeechConstant.PITCH, "50");
			//设置合成音量
			mTts.setParameter(SpeechConstant.VOLUME, "100");
		}else {
			mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
			// 设置本地合成发音人 voicer为空，默认通过语记界面指定发音人。
			mTts.setParameter(SpeechConstant.VOICE_NAME, "");
			/**
			 * TODO 本地合成不设置语速、音调、音量，默认使用语记设置
			 * 开发者如需自定义参数，请参考在线合成参数设置
			 */
			//设置合成语速
			mTts.setParameter(SpeechConstant.SPEED, "60");
			//设置合成音调
			mTts.setParameter(SpeechConstant.PITCH, "50");
			//设置合成音量
			mTts.setParameter(SpeechConstant.VOLUME, "100");
		}
		//设置播放器音频流类型
		mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
		// 设置播放合成音频打断音乐播放，默认为true
		mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");
	}

	// 由于音频抽取为耗时的同步操作，最好专门开辟线程执行
//	class ExtractAudioThread extends Thread {
//		private boolean mStop = false;
//		private ConcurrentLinkedQueue<byte[]> mAudioQueue = new ConcurrentLinkedQueue<byte[]>();
//
//		// 存放得到的16K音频的缓冲区，大小为1024，这是由于每24576字节96K音频中可以抽取到1024字节的16K音频
//		private byte[] m16KAudio = new byte[1024];
//
//		public void stopRun() {
//			mStop = true;
//		}
//
//		public void writeAudio(byte[] audio) {
//			mAudioQueue.add(audio);
//		}
//
//		@Override
//		public void run() {
//			super.run();
//
//			while (!mStop) {
//				byte[] audio96K = mAudioQueue.poll();
//
//				if (null != audio96K) {
//					// extract16K的第3个参数为channel，对于四麦克风板可设置成7、1、8、2，对应4个麦克风，设置哪个值就抽取那一路麦克风的音频
//					int outLen = mCaeEngine.extract16K(audio96K, audio96K.length, 1, m16KAudio);
//
//					// 抽取成功时，返回值16K音频的长度，否则返回0
//					if (0 != outLen) {
//						// 为避免听写对象使用冲突，demo只在非唤醒状态才对抽取的音频进行识别
//						if (mStartRecognize && !mCaeEngine.isWakeup()) {
//							// 写入16K采样率音频，开始听写
//							mIat.writeAudio(m16KAudio, 0, outLen);
//						}
//					}
//				}
//			}
//			mAudioQueue.clear();
//		}
//	}

}
