package com.peng.power.memo.manager;

import android.app.Application;

import com.peng.power.memo.R;
import com.peng.power.memo.cert.CertManager;
import com.peng.power.memo.cert.LoadKeyStore;

import java.net.CookieManager;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SelvyCallClient extends Application {

    private static final String TXT_SSL_MODE_TLS = "TLS";
    static Retrofit retrofit = null;
    public static OkHttpClient client = null;

    public static SelvyCallAPI getApiService(){
        return getInstance().create(SelvyCallAPI.class);
    }

    private static Retrofit getInstance(){
        OkHttpClient client = new OkHttpClient.Builder().build();
        return new Retrofit.Builder()
        .baseUrl(DEFINE.SELVY_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build();
    }

    public SSLSocketFactory getSSLSocketFactory()
    {
        SSLSocketFactory factory = null;
        KeyStore keyStore = null;
        try {
            //Load SSL Certification
            LoadKeyStore pLoadKeyStore = new LoadKeyStore(getApplicationContext());
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


    public static Retrofit getClient(String aServer) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .cookieJar( new JavaNetCookieJar( new CookieManager()))
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(aServer)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        return retrofit;
    }

    public static Retrofit getClienSSLSocketFactory(String aServer) {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client = new OkHttpClient.Builder()
                .sslSocketFactory( CertManager.getInstance().getSSLSocketFactory())
                .addInterceptor(interceptor)
                .cookieJar( new JavaNetCookieJar( new CookieManager()))
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(aServer)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        return retrofit;
    }

    public static Retrofit getClienSSLIgnoreSocketFactory(String aServer) {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        client = new OkHttpClient.Builder()
                .sslSocketFactory( CertManager.getInstance().getSSLIgnoreSocketFactory())
                .addInterceptor(interceptor)
                .cookieJar( new JavaNetCookieJar( new CookieManager()))
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(aServer)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        return retrofit;
    }


}
