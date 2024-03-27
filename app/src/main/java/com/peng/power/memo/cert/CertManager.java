package com.peng.power.memo.cert;

import android.content.Context;

import com.peng.power.memo.R;

import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


public class CertManager {

    private static final String TXT_SSL_MODE_TLS = "TLS";
    private static CertManager instance;
    private Context mContext;

    public static CertManager getInstance(Context context)
    {
        if( instance == null && context != null)
            instance = new CertManager(context.getApplicationContext());
        return instance;
    }

    public static CertManager getInstance()
    {
        return instance;
    }

    CertManager(Context context)
    {
        mContext = context;
    }

    public SSLSocketFactory getSSLSocketFactory()
    {
        SSLSocketFactory factory = null;
        KeyStore keyStore = null;
        try {
            //Load SSL Certification
            LoadKeyStore pLoadKeyStore = new LoadKeyStore(mContext);
            keyStore = pLoadKeyStore.LoadCrt(R.raw.selvy_crt);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance(TXT_SSL_MODE_TLS);
            sslContext.init(null, tmf.getTrustManagers(), null);
            factory = sslContext.getSocketFactory();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return factory;
    }

    public SSLSocketFactory getSSLIgnoreSocketFactory()
    {
        SSLSocketFactory factory = null;
        try {

            TrustManager[] ignoreAllCerts = new TrustManager[]{ new X509TrustManager() {
                public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) { }
                public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) { }
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
            }};

            SSLContext sslContext = SSLContext.getInstance(TXT_SSL_MODE_TLS);
            sslContext.init(null, ignoreAllCerts, null);
            factory = sslContext.getSocketFactory();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return factory;
    }

}
