package com.peng.power.memo.recognizer

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.naver.speech.clientapi.*
import com.peng.power.memo.util.l
import kotlinx.coroutines.*
import splitties.systemservices.launcherApps
import java.util.*

class NaverRecognizer(
    private val activity: Activity,
    private val callBackResult: (RecognizerCallbackData) -> Unit
) : SpeechRecognitionListener, LifecycleObserver {

    private var recognizer: SpeechRecognizer? = null
    private val clientId = "zyxgu1evvf"

    private var jobWatchRecognizerRunning: Job? = null
    private var defaultScope = CoroutineScope(Dispatchers.Default)

    private var prevResult:String = ""
    private var curResult:String = ""

    private var time:Long = 0
    private var stopTime:Long = 0

    private val intervalTime:Long = 3000

    init {
        try{
            recognizer = SpeechRecognizer(activity, clientId)
        }catch (e: SpeechRecognitionException){
            e.printStackTrace()
        }
        initialize()
        recognizer?.setSpeechRecognitionListener(this)

        (activity as AppCompatActivity).lifecycle.addObserver(this)
    }

    fun initialize(){
        recognizer?.initialize()
    }

    fun release(){
        recognizer?.release()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy(){
        recognizer?.let {
            if(it.isRunning) it.stop()
            it.release()
        }
        recognizer = null
        defaultScope.cancel()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause(){
        recognizer?.let{
            if(it.isRunning) it.stop()
        }
    }

    private fun initResultString(){
        curResult = ""
        prevResult = ""
    }

    private fun isNetworkAvailable(context: Context) =
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).run {
                getNetworkCapabilities(activeNetwork)?.run {
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                } ?: false
            }

    private fun startWatchRecognizerRunning(delayTime:Long){
        cancelWatchRecognizerRunning()

        jobWatchRecognizerRunning = defaultScope.launch {
            l.d("prevSearchResultString : $prevResult, curSearchResultString : $curResult")
            if(prevResult == curResult){
                val pastTime = time + stopTime
                l.d("pastTime : $pastTime, currentTimeMills : ${System.currentTimeMillis()}")
                if(pastTime < System.currentTimeMillis()){
                    l.d("pastTime < System.currentTimeMillis()")
//                    activity.runOnUiThread {
//                        callBackResult(RecognizerCallbackData(NaverRecognizerCallbackMsg.FinalResult, curResult))
//                        initResultString()
//                    }

                    // stop()호출시 onResult 호출됨
                    launch(Dispatchers.IO) {
                        l.d("before recognizer stop -- recognizer?.isRunning: ${recognizer?.isRunning}")
                        if(isNetworkAvailable(activity)){
                            recognizer?.stop()
                        }else{
                            l.d("Network UnAvailable")
                        }

                    }

                    return@launch
                }
            }else{
                time = System.currentTimeMillis()
                prevResult = curResult
            }
            l.d("before delay time : $delayTime")
            delay(delayTime)
            startWatchRecognizerRunning(delayTime)
        }



    }

    private fun cancelWatchRecognizerRunning(){
        jobWatchRecognizerRunning?.cancel()
    }

//    private fun sendFinalResult(result:String){
//        if(finalResult.isEmpty()){
//            finalResult = result
//        }else if(finalResult.length != result.length && result.isNotEmpty()){
//            finalResult = "$finalResult $result"
//        }
//
//    }


    //말이 없을때 음성인식이 중단되는 기본값 시간 : 1500ms
    fun recognize(stopTime:Long? = intervalTime){
        if(stopTime != null){
            this.stopTime = stopTime
        }else{
            this.stopTime = intervalTime
        }

        val languageType = if(Locale.getDefault().toString() == "ko_KR"){
            Log.d("NaverRecognizer","default language : korean")
            SpeechConfig.LanguageType.KOREAN
        }else{
            Log.d("NaverRecognizer","default language : english")
            SpeechConfig.LanguageType.ENGLISH
        }

        recognize(languageType)
    }



    fun recognize(languageType: SpeechConfig.LanguageType?): Boolean {
        return try {
            recognizer!!.recognize(
                SpeechConfig(
                    languageType,
                    SpeechConfig.EndPointDetectType.HYBRID
                )
            )
        } catch (e: SpeechRecognitionException) {
            e.printStackTrace()
            false
        }
    }

    fun stop():Boolean{
        return recognizer?.stop() ?: true
    }

    fun cancel(){
        recognizer?.let {
            if(it.isRunning)
                it.cancel()
        }
    }

    @Synchronized
    fun isRunning():Boolean{
        return recognizer?.isRunning!!
    }

    @WorkerThread
    override fun onInactive() {
        activity.runOnUiThread {
            callBackResult(RecognizerCallbackData(NaverRecognizerCallbackType.ClientInactive, null))
        }
    }
    @WorkerThread
    override fun onReady() {
        l.d("onReady")
        time = System.currentTimeMillis()
        curResult = ""
        prevResult = ""
        startWatchRecognizerRunning(stopTime)
        activity.runOnUiThread {
            callBackResult(RecognizerCallbackData(NaverRecognizerCallbackType.ClientReady, null))
        }
    }

    @WorkerThread
    override fun onPartialResult(partialResult: String?) {
        l.d("onPartialResult : $partialResult")
        curResult = partialResult ?: ""

        activity.runOnUiThread {
            callBackResult(RecognizerCallbackData(NaverRecognizerCallbackType.PartialResult, partialResult))
        }
    }
    @WorkerThread
    override fun onResult(finalResult: SpeechRecognitionResult?) {
        l.d("onResult")
        cancelWatchRecognizerRunning()
        activity.runOnUiThread {
            callBackResult(RecognizerCallbackData(NaverRecognizerCallbackType.FinalResult, curResult))
            initResultString()
        }
    }

    @WorkerThread
    override fun onError(errorCode: Int) {
        l.d("onError")
        cancelWatchRecognizerRunning()
        activity.runOnUiThread {
            callBackResult(RecognizerCallbackData(NaverRecognizerCallbackType.RecognitionError, errorCode.toString()))
        }
    }

    @WorkerThread
    override fun onRecord(speech: ShortArray?) {
//        activity.runOnUiThread {
//            callBackResult(RecognizerCallbackData(NaverRecognizerCallbackType.AudioRecording, null))
//        }
    }

    @WorkerThread
    override fun onEndPointDetected() {
//        activity.runOnUiThread {
//            callBackResult(RecognizerCallbackData(NaverRecognizerCallbackType.EndPointDetected, null))
//        }
    }

    @WorkerThread
    override fun onEndPointDetectTypeSelected(epdType: SpeechConfig.EndPointDetectType?) {
//        activity.runOnUiThread {
//            callBackResult(RecognizerCallbackData(NaverRecognizerCallbackType.AudioRecording, epdType.toString()))
//        }
    }
}