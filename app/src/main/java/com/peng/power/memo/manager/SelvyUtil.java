package com.peng.power.memo.manager;


public class SelvyUtil {

    public static int getSampleRate()
    {
        int ret = SelvyCommon.SAMPLE_RATE_16K;
        switch (SelvyCommon.OPT_CODEC)
        {
            case SelvyCommon.STT_CODEC_8K:
                ret = SelvyCommon.SAMPLE_RATE_8K;
                break;
            case SelvyCommon.STT_CODEC_16K:
                ret = SelvyCommon.SAMPLE_RATE_16K;
                break;
        }
        return ret;
    }

    public static int getBuffer100()
    {
        return getSampleRate() / 10 * SelvyCommon.SHORT_TO_BYTE;
    }


    public static int getSkipBufferSize()
    {
        return getBuffer100() / 10;     //10ms
    }

    public static int getSendBufferSize()
    {
        return getBuffer100() * SelvyCommon.SEND_BUFFER_UNIT;
    }

}
