package com.peng.power.memo.manager;


import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import static com.peng.power.memo.manager.SelvyCommon.MAX_RECORD_COUNT;


public class AudioIn {

    private String TAG = this.getClass().getSimpleName();

    private AudioRecord mAudioRecord = null;
    private int mSampleRate;
    AudioInCallback mAudioInCallback = null;
    int mMarkerPosition = 0;
    int mMaxRecordSize = 0;

    //[20201123] Support STT EPD Issue - Zero Buffer Skip
    boolean bPreZeroCheck = false;
    private int mSkipCnt = 0;

    public interface AudioInCallback
    {
        void onRecordBuffer(byte[] buffer);
    }

    public AudioIn() {
        mSampleRate = SelvyUtil.getSampleRate();
        mMarkerPosition = SelvyUtil.getSkipBufferSize();
        mMaxRecordSize = mSampleRate * MAX_RECORD_COUNT;
    }
    public void setCallBack(AudioInCallback callback)
    {
        mAudioInCallback = callback;
    }

    public boolean IsRecording()
    {
        Log.i(TAG,"IsRecording");
        if (mAudioRecord != null && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            return true;
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public boolean startRecording() {
        Log.i(TAG,"startRecording");
        if (mAudioRecord != null && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            Log.i(TAG,"startRecording RECORDSTATE_RECORDING");
            return false;
        }
        if (mAudioRecord == null) {
            final int sizeInBytes = mMaxRecordSize;
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, sizeInBytes);
        }

        if (mAudioRecord == null) {
            return false;
        }

        try {
            bPreZeroCheck = false;
            mSkipCnt = 0;
            mAudioRecord.setRecordPositionUpdateListener(mListener);
            setNotificationMarkerPosition(mMarkerPosition);
            mAudioRecord.startRecording();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    private void setNotificationMarkerPosition(int markerInFrames)
    {
        Log.d(TAG,"setNotificationMarkerPosition : " + markerInFrames);
        mMarkerPosition = markerInFrames;
        if(mAudioRecord != null) {
            int nRet = mAudioRecord.setNotificationMarkerPosition(mMarkerPosition);
            Log.d(TAG,"setNotificationMarkerPosition nRet : " + nRet);
        }
    }

    //[20201123] Support STT EPD Issue - Zero Buffer Skip
    public int getSkipCount()
    {
        return mSkipCnt;
    }

    public int read(byte[] data) {
        Log.i(TAG,"AudioRecord_read_start");
        return mAudioRecord.read(data, 0, data.length);

    }

    public void stopRecording() {
        Log.i(TAG,"stopRecording");
        if (mAudioRecord != null && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    AudioRecord.OnRecordPositionUpdateListener mListener = new AudioRecord.OnRecordPositionUpdateListener() {
        @Override
        public void onMarkerReached(AudioRecord recorder) {
            Log.d(TAG,"OnRecordPositionUpdateListener");

            byte[] buffer= new byte[mMarkerPosition];
            Log.d(TAG,"recorder.read_start");
            recorder.read(buffer, 0, buffer.length);

            //[20201123] Support STT EPD Issue - Zero Buffer Skip
            if(!bPreZeroCheck) {
                int nNonZeroCheck = 0;
                for (int i = 0; i < buffer.length; i++) {
                    nNonZeroCheck += Math.abs(buffer[i]);
                }

                if (nNonZeroCheck < 1) {
                    mSkipCnt++;
                }
                else{
                    mMarkerPosition = SelvyUtil.getSendBufferSize();
                    Log.i(TAG, "mMarkerPosition : " + mMarkerPosition + ", ms : " + mMarkerPosition / 16 );
                    bPreZeroCheck = true;
                }
                setNotificationMarkerPosition(mMarkerPosition);
                return;
            }


            if( mAudioInCallback != null)
                mAudioInCallback.onRecordBuffer(buffer);
            setNotificationMarkerPosition(mMarkerPosition);
            Log.d(TAG,"recorder.read_end");
        }

        @Override
        public void onPeriodicNotification(AudioRecord recorder) {
            Log.d(TAG,"onPeriodicNotification");

        }
    };
}

