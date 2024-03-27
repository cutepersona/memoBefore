package com.peng.power.memo.manager;

import android.util.Log;

import java.util.ArrayList;

public class SelvyRecordManager {

    private String TAG = this.getClass().getSimpleName();

    private AudioIn mAudioIn;
    private ArrayList<byte[]> mBufferList= null;

    private UICallBack mUICallBack = null;
    int mPosition = 0;

    public interface UICallBack{
        void onUpdate(final byte[] buffer);
    }

    private AudioIn.AudioInCallback mCallback = new AudioIn.AudioInCallback() {
        @Override
        public void onRecordBuffer(final byte[] buffer) {
            //mBuffer = buffer;
            mBufferList.add(buffer);
            if( mUICallBack != null)
                mUICallBack.onUpdate(buffer);
        }
    };

    public boolean start()
    {
        mBufferList = new ArrayList<>();
        mPosition = 0;
        mAudioIn = new AudioIn();
        mAudioIn.setCallBack(mCallback);
        return mAudioIn.startRecording();
    }

    public  void stop()
    {
        if( mAudioIn != null)
            mAudioIn.stopRecording();
    }

    public byte[] getBuffer()
    {
        if(mBufferList != null)
        {
            int nSize = SelvyUtil.getSendBufferSize() * mBufferList.size();
            byte[] buffer = new byte[nSize];
            int nCount = 0;
            for( int i =0; i < mBufferList.size(); i++)
            {
                byte[] subList = mBufferList.get(i);
                for( byte pbyte : subList)
                {
                    buffer[nCount++] = pbyte;
                }
            }
            Log.i(TAG,"[Selvy] getBuffer length : " + buffer.length);
            return buffer;
        }
        return null;
    }

    public boolean IsRecord()
    {
        if( mAudioIn != null && mAudioIn.IsRecording())
            return true;
        return false;
    }

    public void setUICallBack(UICallBack callback)
    {
        mUICallBack = callback;
    }



}

