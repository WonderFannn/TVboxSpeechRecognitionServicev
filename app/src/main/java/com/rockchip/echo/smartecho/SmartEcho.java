package com.rockchip.echo.smartecho;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

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
import com.rockchip.echo.R;
import com.rockchip.echo.SmartEchoApp;
import com.rockchip.echo.led.LedController;
import com.rockchip.echo.smartecho.audio.PcmRecorder;
import com.rockchip.echo.smartecho.textunderstand.TextUnderstandResult;
import com.rockchip.echo.util.JsonParser;
import com.rockchip.echo.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yhc on 16-11-20.
 */

public class SmartEcho implements CaeWakeupListener {

    private Context mContext;

    private CaeWakeUpFileObserver mCaeWakeUpFileObserver;

    boolean mStartRecognize = false;
    boolean mIsOnTts = false;
    boolean mIsNeedStartIat = false;

    public SmartEcho(Context context) {
        mContext = context;
        init();
    }

    public void init() {
        LogUtil.d("SmartEcho - init");
        initTts();
        initIat();
        initTextUnderstand();
        mCaeWakeUpFileObserver = new CaeWakeUpFileObserver(this);
    }

    public void start() {
        LogUtil.d("SmartEcho - start");
        if (mCaeWakeUpFileObserver != null) {
            mCaeWakeUpFileObserver.startWatching();
        }
    }

    public void stop() {
        LogUtil.d("SmartEcho - stop");
        if (mCaeWakeUpFileObserver != null) {
            mCaeWakeUpFileObserver.stopWatching();
        }
        stopIat();
    }

    @Override
    public void onWakeUp(int angle, int chanel) {
        LogUtil.d("SmartEcho - onWakeUp");

        Log.d("TAG", "Echo  onWakeUp - angle:"+angle+"chane:"+chanel);
        startTtsOutput(getEchoText(), true);
        LedController.flashAllLed();
    }

    private int mEchoIndex = 0;
    private String getEchoText() {
        mEchoIndex++;
        if(mEchoIndex >= Config.ECHO_TEXT_ARRAY.length) {
            mEchoIndex = 0;
        }
        return Config.ECHO_TEXT_ARRAY[mEchoIndex];
    }

    /**
     * ==================================================================================
     *                               tts
     * ==================================================================================
     */
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 默认发音人
    private String voicer = "vinn";

    // 缓冲进度
    private int mPercentForBuffering = 0;
    // 播放进度
    private int mPercentForPlaying = 0;

    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    public void initTts() {
        mTts = SpeechSynthesizer.createSynthesizer(mContext, mTtsInitListener);
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            LogUtil.d("InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                LogUtil.d("tts init error："+code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    public int startTtsOutput(String text) {
        return startTtsOutput(text, false);
    }

    public int startTtsOutput(String text, boolean needStartIatAfterTts) {
        if (mTts == null) {
            return -1;
        }
        mIsOnTts = true;
        mStartRecognize = false;
        mIsNeedStartIat = needStartIatAfterTts;
        // 设置参数
        setTtsParam();
        int code = mTts.startSpeaking(text, mTtsListener);
        if (code != ErrorCode.SUCCESS) {
            LogUtil.d("tts error: " + code);
        }
        return code;
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            LogUtil.d("tts - start play");
        }

        @Override
        public void onSpeakPaused() {
            LogUtil.d("tts - pause play");
        }

        @Override
        public void onSpeakResumed() {
            LogUtil.d("tts - resume play");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
            mPercentForBuffering = percent;
            LogUtil.d(String.format(mContext.getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            mPercentForPlaying = percent;
            LogUtil.d(String.format(mContext.getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                LogUtil.d("tts - play completed");
            } else if (error != null) {
                LogUtil.d(error.getPlainDescription(true));
            }
            new Handler().postDelayed(new Runnable(){
                public void run() {
                    mIsOnTts = false;
                    if (mIsNeedStartIat) {
                        LogUtil.d("tts - onCompleted - need start iat after tts");
                        mIsNeedStartIat = false;
                        startIat();
                    }
                }
            }, 500);
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

    private void setTtsParam() {
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

    /**
     * ==================================================================================
     *                               pcm record
     * ==================================================================================
     */
    PcmRecorder mRecorder;

    PcmRecorder.PcmListener mPcmListener = new PcmRecorder.PcmListener() {

        @Override
        public void onPcmRate(long bytePerMs) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPcmData(byte[] data, int dataLen) {
//			write2File(data);
            if (mStartRecognize && !mIsOnTts) {
                // 写入16K采样率音频，开始听写
                mIat.writeAudio(data, 0, dataLen);
            }
        }
    };

    /**
     * ==================================================================================
     *                               speech recognition
     * ==================================================================================
     */
    private SpeechRecognizer mIat;

    private void initIat() {
        LogUtil.d("SmartEcho - initIat");
        mRecorder = new PcmRecorder();
        mRecorder.startRecording(mPcmListener);

        mIat = SpeechRecognizer.createRecognizer(mContext, null);
        setIatParam();
    }

    // 听写监听器
    private RecognizerListener mIatListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int arg0, byte[] arg1) {
            LogUtil.d("====== mIatListener - onVolumeChanged");
        }

        @Override
        public void onResult(RecognizerResult result, boolean isLast) {
            LogUtil.d("====== mIatListener - onResult");
            printResult(result);

            if(isLast) {
                LogUtil.d("====== It is last result, so stop recognizer");
                stopIat();
            }
        }

        @Override
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
            LogUtil.d("====== mIatListener - onEvent");
        }

        @Override
        public void onError(SpeechError arg0) {
            LogUtil.d("====== mIatListener - onError");
            stopIat();
        }

        @Override
        public void onEndOfSpeech() {
            LogUtil.d("====== mIatListener - onEndOfSpeech");
//			mIsWakeup = false;
//			LogUtil.d("====== mIsWakeup = false");
        }

        @Override
        public void onBeginOfSpeech() {
            LogUtil.d("====== mIatListener - onBeginOfSpeech");
        }
    };

    private void startIat() {
        LogUtil.d("SmartEcho - startIat");
        mStartRecognize = true;
        // start listening user
        if(mIat != null && !mIat.isListening()) {
            mIat.startListening(mIatListener);
        }
        showLedOnListener(true);
    }

    private void stopIat() {
        LogUtil.d("SmartEcho - stopIat");
        mStartRecognize = false;
        if(mIat != null && mIat.isListening()) {
            mIat.stopListening();
        }
        showLedOnListener(false);
    }

    private void setIatParam() {
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

        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/wftest/iat.wav");

        mIat.setParameter(SpeechConstant.NOTIFY_RECORD_DATA, "0");

        mIat.setParameter("domain", "fariat");
    }

    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

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
        Toast.makeText(SmartEchoApp.getContext(),text,Toast.LENGTH_SHORT).show();
        Log.d("TAG", "printResult: "+text);
        int ret = mTextUnderstander.understandText(text, mTextUnderstanderListener);
        if(ret != 0)
        {
            LogUtil.d("text understand error: "+ ret);
        }
    }


    /**
     * ==================================================================================
     *                               text understand
     * ==================================================================================
     */
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
                    if(ttsText != null && !ttsText.equals("")) {
                        startTtsOutput(ttsText);
                    }
                }
            } else {
                LogUtil.d("understander result : null");
            }
            LogUtil.d("========================================================");
        }

        @Override
        public void onError(SpeechError error) {
            // 文本语义不能使用回调错误码14002，请确认您下载sdk时是否勾选语义场景和私有语义的发布
            LogUtil.d("TextUnderstanderListener - onError Code："	+ error.getErrorCode());
        }
    };

    private void initTextUnderstand() {
        mTextUnderstander = TextUnderstander.createTextUnderstander(mContext, mTextUdrInitListener);
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

        int ret = mTextUnderstander.understandText(text, mTextUnderstanderListener);
        if(ret != 0)
        {
            LogUtil.d("text understand error: " + ret);
        }
    }

    /**
     * ==================================================================================
     *                               control led
     * ==================================================================================
     */
    private Timer mTimer;
    private boolean isShowLedGroupA = true;
    private TimerTask mLedTimerTask;

    public void showLedOnListener(boolean isShow) {
        LogUtil.d("SmartEcho - showLedOnListener: " + isShow);
        if (isShow) {
            if (mTimer == null ) {
                mTimer = new Timer();
                mLedTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        controlLedOnListerner();
                    }
                };
                mTimer.schedule(mLedTimerTask, 500, 1000);
            }
        } else {
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
            if (mLedTimerTask != null) {
                mLedTimerTask.cancel();
                mLedTimerTask = null;
            }
            LedController.setAllLedOff();
        }
    }

    public void controlLedOnListerner() {
        if (isShowLedGroupA) {
            LedController.setGroupLedState("A", 255);
            LedController.setGroupLedState("B", 0);
        } else {
            LedController.setGroupLedState("A", 0);
            LedController.setGroupLedState("B", 255);
        }
        isShowLedGroupA = !isShowLedGroupA;
    }

}
