package com.peng.power.memo.manager;

import android.util.Log;

import com.google.gson.Gson;
import com.peng.power.memo.util.AppUtils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class ClovaSpeechClient {
    private static final String TAG = ClovaSpeechClient.class.getSimpleName();

    // Clova Speech secret key
    private static final String SECRET = "294afb07d1e946dd9390f5b47fda634d";
    // Clova Speech invoke URL
    private static final String INVOKE_URL = "https://clovaspeech-gw.ncloud.com/external/v1/4708/476b099ff8207747fe12b2a778cfe80090e9d41ce09edcfed78b725d5ca14c13";

    private Gson gson = new Gson();
    HttpClient httpClient;

    private Listener listener;

    private static final Header[] HEADERS = new Header[] {
            new BasicHeader("Accept", "application/json"),
            new BasicHeader("X-CLOVASPEECH-API-KEY", SECRET),
    };

    public interface Listener {
        void onResult(String data, String originData);
    }

    public ClovaSpeechClient(Listener listener)
    {
        this.listener = listener;
        init();
    }

    private void init()
    {
        if(httpClient == null) httpClient = new DefaultHttpClient();
    }

    public void upload(String path)
    {
        NestRequestEntity requestEntity = new NestRequestEntity();
        // 화자 최대 숫자 설정
        Diarization diarization = new Diarization();
        diarization.enable = true;
        diarization.speakerCountMax = 1;
        diarization.speakerCountMin = 1;
        requestEntity.setDiarization(diarization);
        upload(path , requestEntity);
    }

    private void upload(String path, NestRequestEntity nestRequestEntity)
    {
        File file = new File(path);
        HttpPost httpPost = new HttpPost(INVOKE_URL + "/recognizer/upload");
        httpPost.setHeaders(HEADERS);
        HttpEntity httpEntity = MultipartEntityBuilder.create()
                .addTextBody("params", gson.toJson(nestRequestEntity), ContentType.APPLICATION_JSON)
                .addBinaryBody("media", file, ContentType.MULTIPART_FORM_DATA, file.getName())
                .build();
        httpPost.setEntity(httpEntity);

        String text = "";
        try
        {
            httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            final HttpEntity entity = httpResponse.getEntity();
            InputStream is = entity.getContent();
            String result = convertStreamToString(is);
            Log.d(TAG , "Noah run - result " + result);

            JSONObject jsonMsg = new JSONObject(result);

            if(!jsonMsg.isNull("text"))
            {
                text = jsonMsg.getString("text");
            }
            listener.onResult(text, result);

//            Log.d(TAG , "Noah run - text " + text);

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String excute(HttpPost httpPost)
    {
        String text = "";
        try
        {
            httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            final HttpEntity entity = httpResponse.getEntity();
            InputStream is = entity.getContent();
            String result = convertStreamToString(is);
            Log.d(TAG , "Noah run - result " + result);

            JSONObject jsonMsg = new JSONObject(result);

            if(!jsonMsg.isNull("text"))
            {
                text = jsonMsg.getString("text");
            }
//            Log.d(TAG , "Noah run - text " + text);

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return text;
    }

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append((line + "\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }


    public static class Boosting {
        private String words;

        public Boosting(String words) {
            this.words = words;
        }

        public String getWords() {
            return words;
        }

        public void setWords(String words) {
            this.words = words;
        }
    }

    public static class Diarization {
        private Boolean enable = Boolean.TRUE;
        private Integer speakerCountMin = 1;
        private Integer speakerCountMax = 1;

        public Boolean getEnable() {
            return enable;
        }

        public void setEnable(Boolean enable) {
            this.enable = enable;
        }

        public Integer getSpeakerCountMin() {
            return speakerCountMin;
        }

        public void setSpeakerCountMin(Integer speakerCountMin) {
            this.speakerCountMin = speakerCountMin;
        }

        public Integer getSpeakerCountMax() {
            return speakerCountMax;
        }

        public void setSpeakerCountMax(Integer speakerCountMax) {
            this.speakerCountMax = speakerCountMax;
        }
    }

    public static class NestRequestEntity {
        private String language = AppUtils.getCurrentLanguage();
        //completion optional, sync/async
        private String completion = "sync";
        //optional, used to receive the analyzed results
        private String callback;
        //optional, any data
        private Map<String, Object> userdata;
        private Boolean wordAlignment = Boolean.TRUE;
        private Boolean fullText = Boolean.TRUE;
        //boosting object array
        private List<Boosting> boostings;
        //comma separated words
        private String forbiddens;
        private Diarization diarization;

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getCompletion() {
            return completion;
        }

        public void setCompletion(String completion) {
            this.completion = completion;
        }

        public String getCallback() {
            return callback;
        }

        public Boolean getWordAlignment() {
            return wordAlignment;
        }

        public void setWordAlignment(Boolean wordAlignment) {
            this.wordAlignment = wordAlignment;
        }

        public Boolean getFullText() {
            return fullText;
        }

        public void setFullText(Boolean fullText) {
            this.fullText = fullText;
        }

        public void setCallback(String callback) {
            this.callback = callback;
        }

        public Map<String, Object> getUserdata() {
            return userdata;
        }

        public void setUserdata(Map<String, Object> userdata) {
            this.userdata = userdata;
        }

        public String getForbiddens() {
            return forbiddens;
        }

        public void setForbiddens(String forbiddens) {
            this.forbiddens = forbiddens;
        }

        public List<Boosting> getBoostings() {
            return boostings;
        }

        public void setBoostings(List<Boosting> boostings) {
            this.boostings = boostings;
        }

        public Diarization getDiarization() {
            return diarization;
        }

        public void setDiarization(Diarization diarization) {
            this.diarization = diarization;
        }
    }
}
