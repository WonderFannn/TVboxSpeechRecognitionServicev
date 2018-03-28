package com.rockchip.echo.smartecho.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class PcmRecorder {
	private static final String TAG = PcmRecorder.class.getSimpleName();
	
	public interface PcmListener {
		public void onPcmData(byte[] data, int dataLen);
		public void onPcmRate(long bytePerMs);
	}
	
	private int mSampleRate = 16000;
	private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
	private int mBufferSize;
	private PcmListener mPcmListener;
	private AudioRecord mAudioRecord;
	private PcmReadThread mPcmReadThread;
	
	private long mStartRecTime;
	private long mStopRecTime;
	private long mDataCount;
	
	public PcmRecorder() {
		
	}
	
	public int getSampleRate() {
		return mSampleRate;
	}

	public void setSampleRate(int sampleRate) {
		this.mSampleRate = sampleRate;
	}
	
	public int getChannelConfig() {
		return mChannelConfig;
	}

	public void setChannelConfig(int channelConfig) {
		this.mChannelConfig = channelConfig;
	}

	public void startRecording(PcmListener listener) {
		if (null != mAudioRecord) {
			if (AudioRecord.RECORDSTATE_RECORDING == mAudioRecord.getRecordingState()) {
				return;
			}
		}
		
		mPcmListener = listener;
		mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, AudioFormat.ENCODING_PCM_16BIT);
		
		Log.d(TAG, String.format("minBufferSize=%d", mBufferSize));
		
		mAudioRecord = new AudioRecord(AudioSource.MIC, mSampleRate, 
									mChannelConfig, 
									AudioFormat.ENCODING_PCM_16BIT, 
									mBufferSize * 8);
		
		if (AudioRecord.STATE_INITIALIZED == mAudioRecord.getState()) {
			mAudioRecord.startRecording();
			mStartRecTime = System.currentTimeMillis();
			mDataCount = 0;
			
			mPcmReadThread = new PcmReadThread();
			mPcmReadThread.start();
		}
	}
	
	public void stopRecording() {
		if (null != mAudioRecord) {
			if (AudioRecord.STATE_INITIALIZED == mAudioRecord.getState()) {
				mStopRecTime = System.currentTimeMillis();
				mAudioRecord.stop();
				mAudioRecord.release();
			}
		}
			
		if (null != mPcmReadThread) {
			mPcmReadThread.stopRun();
		}
	}
	
	
	class PcmReadThread extends Thread {
		private boolean mStop = false;
		
		public PcmReadThread() {
			setPriority(Thread.MAX_PRIORITY);
		}
		
		public void stopRun() {
			mStop = true;
		}
		
		@Override
		public void run() {
			super.run();
			
			while (!mStop) {
				if (null != mAudioRecord) {
					byte[] pcmBuffer = new byte[mBufferSize];
					
					int readCount = mAudioRecord.read(pcmBuffer, 0, mBufferSize);
					mDataCount += readCount;
					long time = System.currentTimeMillis();
					
					Log.d(TAG, String.format("bufferSize=%d, readCount=%d, time=%dms", mBufferSize, readCount, time));
					
					if (0 >= readCount) {
						break;
					}
//					byte[] pcmBuffer_t = new byte[mBufferSize * 2];
//					for (int i = 0, j = 0; i<mBufferSize * 2;) {
//                    	pcmBuffer_t[i] = pcmBuffer[j];
//						pcmBuffer_t[i+1] = pcmBuffer[j+1];
//						pcmBuffer_t[i+2] = pcmBuffer[j];
//						pcmBuffer_t[i+3] = pcmBuffer[j+1];
//						i += 4;
//						j += 2;
//					}
					if (null != mPcmListener) {
						//mPcmListener.onPcmData(pcmBuffer, readCount);
						mPcmListener.onPcmData(pcmBuffer, readCount);
					}
				}
			}
			
			long interval = mStopRecTime - mStartRecTime;
			long rateMs = 0;
			
			if (0 != interval) {
				rateMs = mDataCount / interval;
				Log.d(TAG, String.format("rate=%d byte/ms", rateMs));
			}
			
			if (null != mPcmListener) {
				mPcmListener.onPcmRate(rateMs);
			}
			
		}
	}
	
}
