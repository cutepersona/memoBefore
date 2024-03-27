package com.peng.power.memo.cert;


import android.content.Context;

import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * Created by kbg82 on 2018-07-18.
 */

public class LoadKeyStore {

    private Context mContext;

    public LoadKeyStore(Context context)
    {
        mContext = context;
    }

    public KeyStore LoadP12toBKS(int id, String pass)
    {
        KeyStore bks  = null;
        try {
            bks = KeyStore.getInstance("BKS");
            //Load P12
            final String alias = "geronimo";
            KeyStore ks = KeyStore.getInstance("PKCS12");
            //ks.load( new FileInputStream("trustedCerts"), "passphrase".toCharArray());
            ks.load(mContext.getResources().openRawResource(id), pass.toCharArray());

            //Make BKS
            Certificate certificate = ks.getCertificate(alias);
            final Key key = ks.getKey(alias, pass.toCharArray());

            bks.load(null, pass.toCharArray());
            bks.setKeyEntry(alias, key, pass.toCharArray(), new Certificate[]{certificate});
        }
        catch (Exception e)
        {
            e.printStackTrace();
            bks = null;
        }
        finally {
            return bks;
        }
    }

    public KeyStore LoadCrt(int id)
    {
        KeyStore keyStore  = null;
        try {
            //Load crt
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate ca = cf.generateCertificate(mContext.getResources().openRawResource(id));

            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            keyStore = null;
        }
        finally {
            return keyStore;
        }
    }

}

