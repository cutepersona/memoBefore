package com.peng.power.memo.manager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class AppUtil {

    public static boolean getPermissionsResult(Context context, String[] permissions, int[] grantResults)
    {
        int grantResultsLength = grantResults.length;

        if (grantResultsLength > 0) {

            for( int i = 0; i < grantResultsLength; i++)
            {
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(context, "You denied permission " + permissions[i], Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        } else {

            Toast.makeText(context, "You denied permission ALL.", Toast.LENGTH_LONG).show();
            return false;
        }


        return true;
    }

    public static int getRecrodGauge(byte [] buffer)
    {
        int nGauge = 0;

        ByteBuffer pByteBuffer = ByteBuffer.allocate(buffer.length);
        pByteBuffer = pByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        pByteBuffer = pByteBuffer.put(buffer);
        pByteBuffer.position(0);

        int shortLen = buffer.length / 2;
        short[] drawBuffer = drawBuffer = new short[shortLen];
        ShortBuffer pShortBuffer = pByteBuffer.asShortBuffer();
        pShortBuffer.get(drawBuffer);
        int nSum = 0;
        for(int i = 0; i < pShortBuffer.capacity(); i++)
        {
            nSum += Math.abs(pShortBuffer.get(i));
        }
        nGauge = nSum / pShortBuffer.capacity();

        return nGauge;
    }

    public static String resultParser( String result) {

        String szResult  = result.replace(" i ", " I ");

        //first Upper case
        char first = szResult.charAt(0);
        String pre = first+"";
        String pre_uppre = Character.toUpperCase(first)+"";
        szResult = szResult.replaceFirst(pre, pre_uppre);


        //i Uppercase
        szResult  = szResult.replace(" i ", " I ");
        szResult  = szResult.replace(" i'm ", " I'm ");

        //yes case
        if( szResult.contains(" yes ") )
        {
            if( szResult.indexOf(" yes ") < szResult.length() - 6)
            {
                szResult  = szResult.replace(" yes ", " yes, ");
            }
        }

        if( szResult.contains("Yes ") )
        {
            if( szResult.indexOf("Yes ") < szResult.length() - 5)
            {
                szResult  = szResult.replace("Yes ", "Yes, ");
            }
        }

        //no case
        if( szResult.contains(" no ") )
        {
            if( szResult.indexOf(" no ") < szResult.length() - 5)
            {
                szResult  = szResult.replace(" no ", " no, ");
            }
        }

        if( szResult.contains("No ") )
        {
            if( szResult.indexOf("No ") < szResult.length() - 4)
            {
                szResult  = szResult.replace("No ", "No, ");
            }
        }


        return szResult;
    }
}
