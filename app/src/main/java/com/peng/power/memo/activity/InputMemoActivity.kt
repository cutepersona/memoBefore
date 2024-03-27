package com.peng.power.memo.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimationDrawable
import android.media.AudioFormat
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import com.peng.plant.powerreceiver.POWERDEFINE
import com.peng.plant.powerreceiver.PowerReceiverData
import com.peng.plant.powerreceiver.PowerStoreManager
import com.peng.power.memo.BuildConfig
import com.peng.power.memo.R
import com.peng.power.memo.broadcastreceiver.ConnectionStateMonitor
import com.peng.power.memo.data.ConvertDate
import com.peng.power.memo.data.MemoTableData
import com.peng.power.memo.data.MemoTblData
import com.peng.power.memo.data.SignalingSendData
import com.peng.power.memo.databinding.ActivityInputMemoBinding
import com.peng.power.memo.dialog.DeleteDialog
import com.peng.power.memo.dialog.ImageFileSaveDialog
import com.peng.power.memo.manager.*
import com.peng.power.memo.model.STTResult
import com.peng.power.memo.model.SettingData
import com.peng.power.memo.model.SettingHelper
import com.peng.power.memo.preference.DataUser
import com.peng.power.memo.preference.PreferenceValue
import com.peng.power.memo.recognizer.NaverRecognizer
import com.peng.power.memo.recognizer.NaverRecognizerCallbackType
import com.peng.power.memo.recognizer.RecognizerCallbackData
import com.peng.power.memo.sftp.SftpProgressDialog
import com.peng.power.memo.sftp.SftpService
import com.peng.power.memo.sftp.SftpUploadManager
import com.peng.power.memo.util.AudioUtils
import com.peng.power.memo.util.Global
import com.peng.power.memo.util.l
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.activities.start
import splitties.toast.longToast
import splitties.toast.toast
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class InputMemoActivity : BaseActivity<ActivityInputMemoBinding>() {

    private lateinit var progressDialog: SftpProgressDialog
    private lateinit var deleteDialog: DeleteDialog

    private val TAG: String = "InputMemoActivity"

    private var dataUser: DataUser? = null
    //private var dataUpload: DataUpload?=null

    private var connectionStateMonitor = ConnectionStateMonitor()

    var bindService: SftpService? = null

    private lateinit var currentMemoTableData:MemoTableData // = MemoTableInfo(0,-1,-1,-1,"","","","")


    private var prevInputText:String = ""

    private var naverRecognizer: NaverRecognizer? = null

    var fileDeleteTotalCount = 0
    var fileDeleteResultCount = 0

    var display: DisplayMetrics? = null

    val cameraRequestCode: Int = 1337

    private var imageFileSaveDialog: ImageFileSaveDialog?=null
    private var fileName = ""
    private var fileDir:String = ""
    private var localBitmap: Bitmap?=null

    // selvy  --------------------------------------------
    var mData: SettingData = SettingData()
    var mSettingHelper: SettingHelper? = null
    var mSelvySTTManager: SelvySTTManager? = null

    // selvy  --------------------------------------------

    // 네이버 WAV API ----------------------------------------------------
    // 음성 녹음 API
    private var mRecorder: MediaRecorder? = null
    // 녹음 관련 작업 Handler
    private val mRecodingHandler: Handler = Handler(Looper.getMainLooper())
    // 음성 입력 마지막으로 들어온 시간
    var mVoiceLastInputTime: Long = 0
    // Animation Handler
    var mHandler = Handler(Looper.getMainLooper())
    // 음성 감지 된 Count
    var mVoiceDectectCount = 0
    // 음성 중지 하기 위한 최소 간격
    private var mIsStopMinTerm = false
    // 애니메이션 동작중 상태 값
    var mIsPlayingAnim = false

    // Naver RealTime STT 관련 리소스
    var mRealTimeText = ""
    var wavFileName:String = ""

    // 네이버 WAV API ----------------------------------------------------

    override fun initViewBinding() {
        binding = ActivityInputMemoBinding.inflate(layoutInflater)

        if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            var permissions = arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            )
            ActivityCompat.requestPermissions(this, permissions, cameraRequestCode)
        }
    }

    override fun initAfterBinding() {

        // 글라스 모델 - wav to stt, mobile edit text 입력
        if (Build.MODEL == "T1100G" || Build.MODEL == "T1100S" || Build.MODEL == "T1200G" || Build.MODEL == "T21G" || Build.MODEL == "MZ1000") {
            Global.mIsGlassDevice = true
        } else {
            Global.mIsGlassDevice = false
        }

        Log.d("Noah", " ==== Global.mIsGlassDevice ==== ${Global.mIsGlassDevice}")

        if (Global.mIsGlassDevice) {
            binding.voiceInputHint.setVisibility(View.VISIBLE);
//            checkAutoInputMode();
        } else {
            binding.voiceInputHint.setVisibility(View.GONE);
        }

        var folerPath = "/storage/emulated/0/Android/data/com.peng.power.memo/files/Waves"

//        Log.d("Noah", " ==== makeDir Check ==== ${folerPath}")
        val makeDir = File(folerPath)

//        Log.d("Noah", " ==== makeDir Check ==== ${makeDir}")
        if (!makeDir.exists()) {
            makeDir.mkdir()
        }

        connectionStateMonitor.enable(this)
        powerStoreManager.register()

        currentMemoTableData = getMemoTableDataFromJsonString(intent)

        dataUser = PreferenceValue.getDataUser(this)
        //dataUpload = PreferenceValue.getDataUpload(this)

        connectBindService()

        deleteDialog = DeleteDialog(this)
        progressDialog = SftpProgressDialog(this, 0)

        // 2023-02-06    Navigate520으로 인해 DP로 구분짓기 위함
        display = this.applicationContext?.resources?.displayMetrics

        SignalingSendData.signalingSendData {
            l.d("EVENT_SOCKET_CONNECTION_OK::::::::connect ")
            SignalingSendData.addObserveDbDataNotFound(this) {
                l.d("Db Data Not Found")
                SignalingSendData.sendDeleteMemoQuery(currentMemoTableData.memo_seq){
                    successDeleteMemo(it.toString().toInt())
                }
            }
            connectBindService()
        }

        naverRecognizer = NaverRecognizer(this){
            handleNaverCallback(it)
        }
        Log.d("Noah ==== ", "initAfterBinding() ===== ")
        initUI()

        setEmergencyCall()
    }

    override fun onResume() {
        super.onResume()
        // ----------------------------------- Selvas API --------------------------------------------------------
//        mData.auth = mSettingHelper?.getValue(SettingHelper.NAME.AUTH, AppConfig.AUTHCODE_STT_APP)
//        mData.domain = mSettingHelper?.getValue(SettingHelper.NAME.DOMAIN, AppConfig.STT_DOMAIN)
////        mData.port = mSettingHelper!!.getValue(SettingHelper.NAME.PORT, AppConfig.STT_PORT)
//        mData.model = mSettingHelper!!.getValue(
//            SettingHelper.NAME.MODEL,
//            SelvyCommon.OPT_BASE_MODEL
//        )
////        mData.endmargin = mSettingHelper!!.getValue(SettingHelper.NAME.EPD_MARGIN, SelvyCommon.OPT_ENDMARGIN)
//        mData.endmargin = mSettingHelper!!.getValue(SettingHelper.NAME.EPD_MARGIN, 3000)
//        mData.midresult = mSettingHelper?.getValue(
//            SettingHelper.NAME.MID_RESULT,
//            SelvyCommon.OPT_MID_RESULT
//        );
//
//        initSTT(mData.getURL());

        // ----------------------------------- Selvas API --------------------------------------------------------

        // ----------------------------------- Clova API --------------------------------------------------------
        binding.lottieVoiceCount.bringToFront()
        binding.lottieVoiceCount.setSpeed(2.5f)
        binding.lottieVoiceCount.setAnimation("lotti_count_down.json")

        binding.lottieVoiceCount.addAnimatorListener(object :
            AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                Log.d("Noah", " ==== onAnimationEnd")
//                val preTime = mVoiceLastInputTime
//                DEBUG.d("# preTime: $preTime")
                stopRecording()
                binding.visualizer.setVisibility(View.INVISIBLE)
                binding.lottieVoiceCount.setVisibility(View.GONE)
            }
        })
        // ----------------------------------- Clova API --------------------------------------------------------

    }

    fun initSTT(url: String?) {
        Log.d("Noah", " ==== url ==== ${url}")
        mSelvySTTManager = SelvySTTManager(url, AppConfig.CERT_USE)
        mSelvySTTManager!!.setResultCallback(mSTTResultCallBack)
    }

    val mSTTResultCallBack: SelvySTTManager.STTResultCallBack = object :
        SelvySTTManager.STTResultCallBack {
        override fun onMidResult(szResult: String) {
            Log.d(TAG, "Noah ==== [STTResultCallBack] onMidResult: $szResult")
            Log.d(TAG, "Noah ==== [STTResultCallBack] prevInputText: $prevInputText")
            binding.etMemoInput.setText(prevInputText + szResult)
        }

        override fun onError(aType: Int, msg: String, aCode: Int) {
            Log.d(TAG, "Noah ==== [STTResultCallBack] onError aType : $aType, $msg")
            Toast.makeText(applicationContext, "[Error]STTResultCallBack : $msg", Toast.LENGTH_LONG)
                .show()
            if (aType == SelvySTTManager.STTResultCallBack.ERROR_STT.BEFORE.ordinal) {
                //[Todo] Cancle Callback Nothing
                binding.etMemoInput.setText("[Cancle]")
                offMic()
                return
            } else if (aType == SelvySTTManager.STTResultCallBack.ERROR_STT.RECORED.ordinal) {
                Log.d("Noah", " ==== ERROR_STT.RECORED.ordinal")
                //[Todo] Device Record Fail
                binding.etMemoInput.setText("[Error]")
                offMic()
            } else {
                //[Todo] Send UI
                if (aCode == SelvySTTManager.RES_BUSY) {
                    //[Todo] Channel allocation fail
                    binding.etMemoInput.setText("[Busy]")
                    offMic()
                } else {
                    //ETC
                    Log.d("Noah", " ==== ETC ${msg}")
                    binding.etMemoInput.setText("")
                    offMic()
                }
            }
        }

        override fun onUpdate(buffer: ByteArray) {
            //[Todo] Send progressbar
            val nGauge: Int = AppUtil.getRecrodGauge(buffer)
//            Log.d(TAG, "[Selvy] mMapCallback onRecordBuffer nGauge : $nGauge")
//            mProgressBarL.setProgress(nGauge)
//            mProgressBarR.setProgress(nGauge)
        }

        override fun onResult(szResult: String) {
            Log.i(TAG, "[STTResultCallBack] onResultSTT: $szResult")
            if (szResult == null || szResult.isEmpty()) {
                //UNKNOWN
                return
            }

            //[Todo] Not Use.
        }

        override fun onResultFinish(pResult: STTResult, recTime: Double) {
            Log.i(TAG, "[STTResultCallBack] onResultFinish: " + pResult.analysisResult.result)
            //[Todo] UI update.
            if (pResult.analysisResult.result == null || pResult.analysisResult.result.isEmpty()) {
                binding.etMemoInput.setText("")
            } else {
                updateResult(pResult)
            }
            offMic()
        }

        override fun onCancel() {}
    }

    fun updateResult(pResult: STTResult) {
        var resultText = ""
        resultText = resultText + pResult.analysisResult.result
        resultText = """
             $resultText
             """.trimIndent()
        val epdTime =
            (pResult.analysisResult.endTime.toInt() - pResult.analysisResult.startTime.toInt()).toDouble() / 1000.0
        Log.i(TAG, "updateResultLV epdTime: $epdTime")
        binding.etMemoInput.setText(prevInputText + resultText)
    }

    override fun onDestroy() {
        super.onDestroy()
        SftpUploadManager.saveUploadingInfo(this)
        powerStoreManager.unregister()
        connectionStateMonitor.disable()
    }

    private fun setEmergencyCall(){
        val textView = TextView(this)
        textView.text = "긴급전화"
        textView.contentDescription = "hf_no_number"
        val lp = LinearLayout.LayoutParams(1, 1)
        textView.layoutParams = lp
        textView.setOnClickListener {
            val intent = Intent("com.peng.emergencycall")
            sendBroadcast(intent)
        }
        binding.root.addView(textView)
    }


    @SuppressLint("SetTextI18n")
    private fun initUI(){
        // 액티비티 전환시 MemoTableData형태의 데이터를 인텐트에 담아서 전달
        currentMemoTableData = getMemoTableDataFromJsonString(intent)

        l.d("currentmemotabledata : ${currentMemoTableData.memo_contents}")

        Log.d("Noah", "==== dataUser.thermal ==== " + dataUser?.thermal)

        binding.run {
            if(currentMemoTableData.memo_seq != 0){     // 기존 메모 수정
                tvInputMemoTitle.text = resources.getString(R.string.memo) + " " + currentMemoTableData.memo_seq
                tvUserId.text = currentMemoTableData.user_id
                tvUserName.text = currentMemoTableData.user_name
                tvMemoDelete.visibility = View.VISIBLE
                if(currentMemoTableData.memo_contents.isNotEmpty()){
                    l.d("memo contents is null or empty")
                    etMemoInput.setText(currentMemoTableData.memo_contents)
                    tvInputHint.visibility = View.GONE
                    l.d("etMemoInput : ${binding.etMemoInput}")
                }else{
                    tvClearText.visibility = View.GONE
                    tvClearTextAll.visibility = View.GONE
                }
                tvSaveTime.text = ConvertDate.getLocalDateTime(currentMemoTableData.save_time)
            }else{      // 신규 메모
                dataSetting()
            }


            etMemoInput.setTextIsSelectable(true)
            etMemoInput.setSelection(binding.etMemoInput.length())
            etMemoInput.showSoftInputOnFocus = false

            etMemoInput.setOnCloseListener {
                l.d("onClose")
                etMemoInput.setTextIsSelectable(true)
                etMemoInput.setSelection(etMemoInput.length())
                etMemoInput.showSoftInputOnFocus = false
                offMic()
            }

            tvKeypad.setOnClickListener {
                if(checkAuthorUser()) etMemoInput.start()
            }

            tvBack.setOnClickListener {
                if(SftpUploadManager.isUploading(currentMemoTableData.memo_seq)){
                    toast(resources.getString(R.string.file_saving))
                    return@setOnClickListener
                }else{
                    activityMemoListCall()
                }
            }

            tvMemoDelete.setOnClickListener {
                if(checkAuthorUser()){
                    if(SftpUploadManager.isUploading(currentMemoTableData.memo_seq)){
                        toast(resources.getString(R.string.remain_upload_file))
                        return@setOnClickListener
                    }

                    showDeleteDialog()
                }
            }

            tvMemoSave.setOnClickListener {
                l.d("btn_memo_save:::save()::::")
                saveMemo()
            }

            tvInputStart.setOnClickListener {
                if(checkAuthorUser()){
                    onMic()
                    prevInputText = etMemoInput.text.toString()

                    startRecording()


// ------------------------------------------------ 셀바스 STT ----------------------------------------------------
//                    val savePath = "AOS_" + System.currentTimeMillis() + ".mp3"
//
//                    val body: STTPrepare = STTPrepare()
//                        .setModelId(mData.model)
//                        .setAuthCode(mData.auth)
////                        .setTicketIdPrefix(mData.auth.toString() + "_AOS")
//                        .setTicketIdPrefix(mData.auth.toString())
//                        .setEPDEndMargin(mData.endmargin)
//                        .setMidResult(mData.midresult)
//                        .setContentSave(
//                            savePath,
//                            SelvyCommon.OPT_FILE_TYPE,
//                            SelvyCommon.OPT_SAVE_TYPE
//                        )
//
//                    mSelvySTTManager?.prepare(body)
// ------------------------------------------------ 셀바스 STT ----------------------------------------------------


// ------------------------------------------------ 기존 네이버 STT ----------------------------------------------------
//                    if(BuildConfig.PING_CHECK_NAVER){
//                        if(checkNaverPing()){
//                            prevInputText = etMemoInput.text.toString()
//                            naverRecognizer?.let{ recognizer ->
//                                if(!recognizer.isRunning()){
//                                    recognizer.recognize()
//                                }else{
//                                    launch(Dispatchers.IO) {
//                                        recognizer.stop()
//                                    }
//                                }
//                            }
//                        }else{
//                            longToast(resources.getString(R.string.bad_internet_connection))
//                        }
//                    }else{
//                        prevInputText = etMemoInput.text.toString()
//                        naverRecognizer?.let{ recognizer ->
//                            if(!recognizer.isRunning()){
//                                recognizer.recognize()
//                            }else{
//                                launch(Dispatchers.IO) {
//                                    recognizer.stop()
//                                }
//                            }
//                        }
//                    }
// ------------------------------------------------ 기존 네이버 STT ----------------------------------------------------

                }
            }

            tvClearText.setOnClickListener {
                if(checkAuthorUser()) deleteText()
            }

            tvClearTextAll.setOnClickListener {
                if(checkAuthorUser()) deleteTextAll()
            }

            tvCamera.setOnClickListener {
                if(checkAuthorUser()){
                    start<PowerCameraPreviewActivity> {
                        SignalingSendData.removeObserveDbDataNotFound(this@InputMemoActivity)
                        currentMemoTableData.memo_contents = etMemoInput.text.toString()
                        putExtra(this, currentMemoTableData)
                    }
                    finish()
                }
            }

            // 열화상 카메라 연동
            tvThermal.setOnClickListener {
                if(checkAuthorUser()){
                    onLaunchThermalCamera(it)
//                    finish()
                }
            }

            tvGallery.setOnClickListener {
                if(SftpUploadManager.isUploading(currentMemoTableData.memo_seq)){
                    toast(resources.getString(R.string.remain_upload_file))
                    return@setOnClickListener
                }else{
                    start<FileListActivity> {
                        SignalingSendData.removeObserveDbDataNotFound(this@InputMemoActivity)
                        currentMemoTableData.memo_contents = etMemoInput.text.toString()
                        putExtra(this, currentMemoTableData)
                    }
                    finish()
                }
            }
        }

        if (dataUser?.thermal == null
            || dataUser?.thermal == 0){
                binding.ivThermal.visibility = View.GONE;
                binding.tvThermal.visibility = View.GONE;
        } else {
            binding.ivThermal.visibility = View.VISIBLE;
            binding.tvThermal.visibility = View.VISIBLE;
        }

        // 2023-02-06   Navigate520
        if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
            Log.d("Noah", " Navigate520 ")

            binding.etMemoInput.setTextSize(30f)
            binding.tvInputHint.setTextSize(30f)
            binding.tvMemoDelete.setTextSize(24f)
            binding.tvClearText.setTextSize(24f)
            binding.tvClearTextAll.setTextSize(24f)
            binding.tvKeypad.setTextSize(24f)
            binding.tvInputStart.setTextSize(24f)
            binding.tvMemoSave.setTextSize(24f)

            binding.tvBack.setTextSize(23f)
            binding.tvInputMemoTitle.setTextSize(27f)
            binding.tvThermal.setTextSize(23f)
            binding.tvCamera.setTextSize(23f)
            binding.tvGallery.setTextSize(23f)

            binding.Date.setTextSize(20f)
            binding.tvSaveTime.setTextSize(20f)
            binding.Writer.setTextSize(20f)
            binding.tvUserName.setTextSize(20f)
            binding.ID.setTextSize(20f)
            binding.tvUserId.setTextSize(20f)




        }


    }

    //-------------------------------------- 네이버 WAV API --------------------------------------------
    /**
     * 애니메이션 및 주파수 출력 Runnable
     */
    val speechUIRunnable: Runnable = object : Runnable {
        override fun run() {
            if (mRecorder != null) {
                val maxAmplitude = mRecorder!!.maxAmplitude
                if (maxAmplitude > 0) {
                    binding.visualizer.addAmplitude(maxAmplitude)
                    if (maxAmplitude > DEFINE.SPEECH_INPUT_MIN_DECIBEL_VAULE) {
                        mVoiceDectectCount++
                        if (mVoiceDectectCount >= DEFINE.CONTINUOUS_VOICE_INPUT_THRESHOLD) {
                            mVoiceLastInputTime = System.currentTimeMillis()
                            mVoiceDectectCount = 0
                        }
                        if (mIsPlayingAnim) {
                            mIsPlayingAnim = false
                            binding.lottieVoiceCount.setVisibility(View.GONE)
                            binding.lottieVoiceCount.pauseAnimation()
                        }
                    } else mVoiceDectectCount = 0
                }
                mRecodingHandler.postDelayed(this, 10)
            }
        }
    }

    /**
     * count down dialog 표시
     */
    private fun showInputWaitCountDown() {
        Log.d("Noah", "==== showInputWaitCountDown ====")
//        DEBUG.d("# showInputWaitCountDown")
        binding.lottieVoiceCount.setVisibility(View.VISIBLE)
        binding.lottieVoiceCount.playAnimation()
        mIsPlayingAnim = true
    }


    /**
     * 음성 입력 시간 체크 Runnable
     * 음성 인식이 해당 시간 안에 없는 경우 애니메이션 start
     * 해당 시간 + 애니메이션 시간 만큼 입력이 되지 않는 경우 음성 인식 종료 및 애니메이션 종료
     */
    private val mInPutTieCheckRunnable: Runnable = object : Runnable {
        override fun run() {
            val noInputTime = System.currentTimeMillis() - mVoiceLastInputTime
            //            DEBUG.d("# noInputTime :" + noInputTime);
//            DEBUG.d("# mVoiceLastInputTime :" + mVoiceLastInputTime);
            if (noInputTime > 1000 && !mIsPlayingAnim) {
                showInputWaitCountDown()
            }
            mHandler.postDelayed(this, 300)
        }
    }


    /**
     * 음성 인식 시작, 종료
     * 인식 시작 할 때 버튼 비활성화, 풀릴 때 다시 활성화
     *
     * @param isPlaying - true : 음성인식 시작, false : 음성 인식 종료
     */
    private fun voiceRecordToggle(isPlaying: Boolean) {
//        if (isPlaying) {
//            mComponentVoiceInputBinding.tvInput.setText(getString(R.string.voice_input_stop))
//            mComponentVoiceInputBinding.tvClose.setAlpha(0.2f)
        val anim = binding.ivRecordAnim.getDrawable() as AnimationDrawable
        anim.start()
//            //            binding.tvVoiceInputContent.setText("");
//        } else {
//            mComponentVoiceInputBinding.tvInput.setText(getString(R.string.input))
//            mComponentVoiceInputBinding.tvClose.setAlpha(1.0f)
//            //            binding.tvVoiceInputContent.setText(mContent);
//        }
//        mComponentVoiceInputBinding.tvClose.setClickable(!isPlaying)
//        mVoiceAdapter.setEnable(!isPlaying)
//        mVoiceAdapter.notifyDataSetChanged()
//        mActivity.setInputTopButtonEnable(!isPlaying) // 음성 입력 중인 경우 disable 상태이므로 반대 값 전달
    }



    /**
     * 녹음 시작 (.Wav)
     */
    private fun startRecording() {
//        DEBUG.d("# startRecording")

        binding.visualizer.setVisibility(View.VISIBLE)

        Log.d("Noah", " ==== startRecording ====")
        mVoiceLastInputTime = System.currentTimeMillis()
        var fileName: String = mVoiceLastInputTime.toString()
//        val fileName: String = Global.getLocalDirPath()
//            .toString() + "/" + DEFINE.SPEECH_ORIGIN_FILE_NAME + DEFINE.WAV_FORMAT
        mRecorder = MediaRecorder()
        mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder!!.setOutputFormat(AudioFormat.ENCODING_PCM_16BIT)
        mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        //        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        Log.d("Noah", "==== fileName ==== ${Environment.getExternalStorageDirectory().toString() + "/Android/data/com.peng.power.memo/files/Waves/"}")

        wavFileName = Environment.getExternalStorageDirectory() .toString() + "/Android/data/com.peng.power.memo/files/Waves/" + fileName + ".wav"

        mRecorder!!.setOutputFile(wavFileName)
        try {
            mRecorder!!.prepare()
        } catch (e: IOException) {
//            DEBUG.error("prepare() failed")
            Log.d(
                "Noah ",
                " ==== mRecorder!!.prepare() ==== FAIL !!!!!!!!!!!!!!!!! ==== ${e.message}"
            )
        }
        mRecorder!!.start()

        mRecodingHandler.post(speechUIRunnable)

        mHandler.post(mInPutTieCheckRunnable)
        voiceRecordToggle(true)
        mVoiceDectectCount = 0
        mIsStopMinTerm = false
        Handler().postDelayed({ mIsStopMinTerm = true }, 1000)
    }

    /**
     * 녹음 중단 후 wav 파일 전송
     */
    private fun stopRecording() {
//        DEBUG.d("# stopRecording")
        // 시작 후 최소 중지 시간 간격 지나지 않았으면 return
        if (!mIsStopMinTerm) return
        naverRecognizer!!.stop()
//        mActivity.showProgressDlg()
        if (mRecorder != null){
            mRecorder!!.stop()
            mRecorder!!.release()
            mRecorder = null
        }

        mVoiceDectectCount = 0
        animStop()
//        val fileName =
//            Global.getLocalDirPath() + "/" + DEFINE.SPEECH_ORIGIN_FILE_NAME + DEFINE.WAV_FORMAT

        Thread {
            AudioUtils.callNaverSttApi(wavFileName, object : AsyncCallback<ArrayList<String>> {
                override fun onCallback(result: ArrayList<String>) {
                    val data = result[0]
                    mHandler.post { resultWavToText(data) }
                }
//                override fun onCallback(result: ArrayList<String>) {
//                    val data = result[0]
////                    DEBUG.d("# runFileToText data: $data")
//                    mHandler.post { resultWavToText(data) }
////                    mActivity.dismissProgressDlg()
//                }
            })
        }.start()
    }

    /**
     * Naver API Result 값 처리
     *
     * @param wavToTextReslut - Speech to Text
     */
    private fun resultWavToText(wavToTextReslut: String) {
        var wavToTextReslut = wavToTextReslut
//        DEBUG.d("# finalResultByWaveFile : $wavToTextReslut")
        // 버튼 음성 인식 (음성 입력 완료) 대한 Text 변환 내용 제거
        wavToTextReslut = wavToTextReslut.replace(
            ("(" + getString(R.string.voice_input_stop) + "\\s*)+").toRegex(),
            ""
        ).trim { it <= ' ' }
        voiceRecordToggle(false)

        Log.d("Noah", " ==== wavToTextReslut ${wavToTextReslut}")

        binding.etMemoInput.setText(prevInputText + wavToTextReslut)

        offMic()

        wavFileDelete(wavFileName)

//        // Wav STT 변환 된 내용이 없는 경우 검사
//        if (wavToTextReslut.length <= 0) {
//            if (mRealTimeText.length > 0) {
//                // 실시간 STT로 감지가 된 내용이 있는 경우 대체
//                wavToTextReslut = mRealTimeText
//            } else {
////                // todo - 현대차인경우 현재 입력 된 내용이 없어도 다음 tab으로 넘어가도록 설정
////                if (CompanyInfoUtils.isCompanyMatching(
////                        mActivity.mUserInfo,
////                        CompanyInfoUtils.HYUNDAI_ENVIRONMENT_MANAGEMENT
////                    ) && nextTabCheck()
////                ) {
////                    CustomToast.makeText(
////                        mActivity,
////                        getContext().getResources()
////                            .getString(R.string.toast_voice_input_empty_messge_next_tab),
////                        CustomToast.LONG,
////                        CustomToast.SUCCESS,
////                        true
////                    ).show()
////                    mActivity.nextTabSwitch()
////                } else {
////                    // 변환 된 텍스트가 있는 경우에만 처리 (wav to text 응답 오지 않는 경우 있음)
////                    CustomToast.makeText(
////                        mActivity,
////                        getContext().getResources()
////                            .getString(R.string.toast_voice_input_empty_messge),
////                        CustomToast.LONG,
////                        CustomToast.SUCCESS,
////                        true
////                    ).show()
////                }
////                return
//            }
//        }

//        // 파일 복사
//        val oringFilePath =
//            Global.getLocalDirPath() + "/" + DEFINE.SPEECH_ORIGIN_FILE_NAME + DEFINE.WAV_FORMAT
//        val destFilePath = Global.getLocalDirPath() + "/" + FileManager.getInsertFileName(
////            mActivity.mUserInfo.user_id,
////            mActivity.getSeletedMenu().item_seq,
//            TimeUtils.getTime("yyyyMMddHHmmssSSS")
//        ) + DEFINE.WAV_FORMAT
//        FileManager.copy(File(oringFilePath), File(destFilePath))
//
//        // 파일 이름
//        val destVoiceFile = File(destFilePath)
////        DEBUG.d("# create voice file name :" + destVoiceFile.name)
//        if (Global.mIsNewRegistration) {
//            // 신규 작성 시 Local 즉시 반영
////            mCurrentContent.item_content.add(wavToTextReslut)
////            mCurrentContent.voice_file_name.add(destVoiceFile.name)
////            mComponentVoiceInputBinding.listVoiceText.scrollToPosition(mCurrentContent.item_content.size() - 1)
////            mVoiceAdapter.notifyDataSetChanged()
//            voiceRecordToggle(false)
////            if (nextTabCheck()) mActivity.nextTabSwitch()
//        } else {
//            // 수정 중인 경우 API 전송 후 추가 - 업데이트 위한 임시 Content 만든 후 성공 시 Local 반영
//            val updateContent = PatrolContent()
//            updateContent.crud = NETWORK.CRUD_CREATE
//            updateContent.content_seq = mCurrentContent.content_seq
//            updateContent.item_content.addAll(mCurrentContent.item_content)
//            updateContent.voice_file_name.addAll(mCurrentContent.voice_file_name)
//            updateContent.item_content.add(wavToTextReslut)
//            updateContent.voice_file_name.add(destVoiceFile.name)
//            updateContent.delete_voice_file = ""
//            mActivity.mRetrofitCallManager.updateContent(updateContent, mActivity,
//                AsyncCallback { isSuccessCode: Boolean? ->
//                    updateContentReponseProcess(
//                        updateContent,
//                        destVoiceFile,
//                        isSuccessCode
//                    )
//                })
//        }
    }


    /**
     * Voice 관련 Anim Stop
     */
    private fun animStop() {
        // voice count down anim stop
        binding.lottieVoiceCount.setVisibility(View.GONE)
        binding.lottieVoiceCount.pauseAnimation()
        mHandler.removeMessages(0)
        // voice input anim stop
        val anim = binding.ivRecordAnim.getDrawable() as AnimationDrawable
        anim.stop()
        mIsPlayingAnim = false
    }

    fun wavFileDelete(fileName: String) {
        val file = File(fileName)
        file.delete()
//        Log.d("SftpController", "removefile-=-=-$fileName")
//        setMediaScanner(path)
    }


    //-------------------------------------- 네이버 WAV API --------------------------------------------

    //    fun initPrepare(){
//        val call = SelvyCallClient.getApiService().prepare(none = null)
//        call?.enqueue(object : Callback<STTResult> {
//            override fun onResponse(call: Call<STTResult>, response: Response<STTResult>) {
//                if (response.isSuccessful) {
//                    Log.d("Noah", " ==== initPrepare ==== isSuccessful")
//                } else {
//                    Log.d("Noah", " ==== initPrepare ==== isSuccessful ==== NOT !!!")
//                }
//
//            }
//
//            override fun onFailure(call: Call<STTResult>, t: Throwable) {
//                Log.d("Noah", " ==== initPrepare ==== onFailure ==== ${t.message}")
//                offMic()
//            }
//
//        })
//    }

    /**
     * Button callback method.
     */
    fun onLaunchThermalCamera(view: View) {
//        Log.d("Noah", " ==== onLaunchThermalCamera ==== " + view)

        takePhoto()
    }

    fun takePhoto() {
        val thermalContentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "memo_thermal.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
        }


        val thermalContentUri = baseContext.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            thermalContentValues
        )

//        Log.d("Noah ", "takePhoto / thermalContentUri " + thermalContentUri)
//        Log.d("Noah ", "takePhoto / thermalContentUri " + MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//        Log.d("Noah ", "takePhoto / thermalContentUri " + thermalContentValues)


        val imageCaptureIntent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            putExtra(MediaStore.EXTRA_OUTPUT, thermalContentUri)

            // "thermal" for OPTION 1
            // "req-thermal" for OPTION 2
            putExtra("rw_camera_mode", "thermal")
        }

//        Log.d("Noah ", "takePhoto / imageCaptureIntent " + imageCaptureIntent)
//
//        Log.d("Noah", "==== Before startActivityForResult ====")
//        Log.d("Noah", "==== imageCaptureIntent ====      " + imageCaptureIntent)
//        Log.d("Noah", "==== cameraRequestCode ====      " + cameraRequestCode)
        startActivityForResult(imageCaptureIntent, cameraRequestCode)
    }

    override
    fun onActivityResult(
        requestCode: Int, resultCode: Int, data: Intent?,
    )
    {
        val cameraRequestCode: Int = 1337

        Log.d("Noah ", "onActivityResult ============== ")

        if (resultCode == Activity.RESULT_OK && requestCode == cameraRequestCode) {
            data?.let { cameraIntent ->
//                val imageUri = cameraIntent.data
//                imageUri?.also {
//                    imageView.setImageURI(imageUri)
//                }

                var captureBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, cameraIntent.data)
                localBitmap = cropBitmap(captureBitmap)
                showSaveImageDialog(captureBitmap)
//                imageView.setImageBitmap(bitmap)
//                Log.d(TAG, "Noah ==== onActivityResult " + currentMemoTableData.memo_seq)

            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showSaveImageDialog(bitmap: Bitmap){
        if(imageFileSaveDialog ==null){
            imageFileSaveDialog = ImageFileSaveDialog(this, bitmap){
                when(it){
                    0 -> { // save
                        saveImage()
                    }
                    1 -> { // cancel
                        localBitmap = null
                    }
                }
            }
        }

        imageFileSaveDialog?.setOnDismissListener {
            imageFileSaveDialog = null
            bitmap.recycle()
        }

        imageFileSaveDialog?.let{
            if(!it.isShowing)
                it.show()
        }
    }

    private fun cropBitmap(bitmap: Bitmap): Bitmap {
        val originalWidth = bitmap.width
        l.d("bitmap width : $originalWidth, height : ${bitmap.height}")
        if(originalWidth != 4608){
            return bitmap
        }
        return Bitmap.createBitmap(bitmap, 384, 648, 3840, 2160)                   // 2023-07-12      해상도 줄임
//        return Bitmap.createBitmap(bitmap, 384, 648, 1920, 1080)
    }


    //443
    private fun checkNaverPing():Boolean{
        val host = "49.236.142.31"
        val cmd = "ping -c 1 -W 5 $host"
        var result:Int = 2
        try {
            val proc = Runtime.getRuntime().exec(cmd)
            proc.waitFor()
            result = proc.exitValue()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        l.d("check naver ping test result : $result // 0:success, 1:fail, 2:error")
        return result == 0
    }


    private fun checkAuthorUser():Boolean{
        return if(currentMemoTableData.user_id == dataUser?.userId){
            l.d("checkAuthorUser : true")
            true
        }else{
            toast(resources.getString(R.string.user_not_match))
            false
        }
    }


    private fun deleteText(){
        val text = binding.etMemoInput.text.toString()
        var result = ""
        if(text.isEmpty()){
            binding.tvClearText.visibility = View.GONE
            binding.tvClearTextAll.visibility = View.GONE
            binding.tvInputHint.visibility = View.VISIBLE
        }else{
            result = text.substring(0, text.length - 1)
            binding.etMemoInput.setText(result)
            binding.etMemoInput.setSelection(binding.etMemoInput.length())
            if(result.isEmpty()){
                binding.tvClearText.visibility = View.GONE
                binding.tvClearTextAll.visibility = View.GONE
                binding.tvInputHint.visibility = View.VISIBLE
            }
        }
    }

    private fun deleteTextAll(){
        binding.etMemoInput.setText("")
        binding.tvClearText.visibility = View.GONE
        binding.tvClearTextAll.visibility = View.GONE
        binding.tvInputHint.visibility = View.VISIBLE
    }


    private fun handleNaverCallback(callbackData: RecognizerCallbackData){
        when(callbackData.type){
            NaverRecognizerCallbackType.RecognitionError -> {
                toast("recognizer error code : ${callbackData.result}")
                offMic()
            }
            NaverRecognizerCallbackType.ClientReady -> {
                onMic()
            }
            NaverRecognizerCallbackType.PartialResult -> {
                val curText = prevInputText + callbackData.result
                inputSpeechToText(curText)
            }
            NaverRecognizerCallbackType.FinalResult -> {
                val curText = prevInputText + callbackData.result
                inputSpeechToText(curText)
                offMic()
            }
            else->{

            }
        }
    }

    private fun inputSpeechToText(result: String){
        binding.etMemoInput.setText(result)
        binding.etMemoInput.focusable = View.FOCUSABLE
        binding.etMemoInput.setTextIsSelectable(true)
        binding.etMemoInput.setSelection(binding.etMemoInput.length())
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun onMic(){
        binding.tvInputHint.visibility = View.GONE
        binding.tvInputStart.visibility = View.GONE
        binding.tvMemoSave.visibility = View.GONE
        binding.tvClearText.visibility = View.GONE
        binding.tvClearTextAll.visibility = View.GONE
        binding.tvMemoDelete.visibility = View.GONE
        binding.tvBack.visibility = View.GONE
        binding.tvKeypad.visibility = View.GONE
        binding.tvCamera.isEnabled = false
        binding.tvGallery.isEnabled = false
        binding.etMemoInput.background = resources.getDrawable(
            R.drawable.popup_box_memo_white,
            null
        )
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun offMic(){
        binding.tvCamera.isEnabled = true
        binding.tvGallery.isEnabled = true
        if(binding.etMemoInput.text.isNullOrEmpty()){
            binding.tvInputHint.visibility = View.VISIBLE
            binding.tvClearTextAll.visibility = View.GONE
            binding.tvClearText.visibility = View.GONE
        }else{
            binding.tvInputHint.visibility = View.GONE
            binding.tvClearTextAll.visibility = View.VISIBLE
            binding.tvClearText.visibility = View.VISIBLE
        }
        binding.tvInputStart.visibility = View.VISIBLE
        binding.tvMemoSave.visibility = View.VISIBLE
        if(currentMemoTableData.memo_seq != 0) binding.tvMemoDelete.visibility = View.VISIBLE
        binding.tvKeypad.visibility = View.VISIBLE
        binding.tvBack.visibility = View.VISIBLE
        binding.etMemoInput.background = resources.getDrawable(R.drawable.popup_box_memo_gray, null)
        l.d("off Mic")
//        launch(Dispatchers.Main) {
//            naverRecognizer?.let{
//                if(it.isRunning()) it.stop()
//            }
//        }

    }


    private fun saveMemo(){
        if(checkAuthorUser()){
            if(currentMemoTableData.memo_seq != 0){ //기존메모 수정
                progressDialog.setTitle(resources.getString(R.string.save_changes))
                SignalingSendData.updateQuery(currentMemoTableData.memo_seq,
                    binding.etMemoInput.text.toString()){ resultForUpdate->
                    val resultString = resultForUpdate as String
                    val resultInt = resultString.toInt()
                    when(resultInt){
                        -1 -> {
                            if (progressDialog.isShowing) progressDialog.dismiss()
                            toast(resources.getString(R.string.update_error))
                            activityMemoListCall()
                        }
                        0 -> {
                            if (progressDialog.isShowing) progressDialog.dismiss()
                            toast(resources.getString(R.string.fail))
                            activityMemoListCall()
                        }
                        1 -> {
                            if (progressDialog.isShowing) progressDialog.dismiss()
                            activityMemoListCall()
                        }
                    }
                }
            }else{  //신규메모 작성
                progressDialog.setTitle(resources.getString(R.string.saving_new_note))
                val memoTblData = MemoTblData()
                memoTblData.dbUserID = binding.tvUserId.text.toString()
                memoTblData.dbEnSeq =  currentMemoTableData.en_seq
                memoTblData.dbHqSeq = currentMemoTableData.hq_seq
                memoTblData.dbBrSeq = currentMemoTableData.br_seq
                memoTblData.dbMemoContents = binding.etMemoInput.text.toString()
                memoTblData.dbSaveTime = currentMemoTableData.save_time
                SignalingSendData.insertMemoData(memoTblData){ resultForInsert->
                    val resultString = resultForInsert as String
                    when(resultString.toInt()){
                        -1 -> {
                            if (progressDialog.isShowing) progressDialog.dismiss()
                            toast(resources.getString(R.string.insert_error))
                            activityMemoListCall()
                        }
                        0 -> {
                            if (progressDialog.isShowing) progressDialog.dismiss()
                            toast(resources.getString(R.string.fail))
                            activityMemoListCall()
                        }
                        1 -> {
                            if (progressDialog.isShowing) progressDialog.dismiss()
                            activityMemoListCall()
                        }
                    }
                }
            }
            progressDialog.setCancelable(true)
            if(!progressDialog.isShowing) progressDialog.show()
        }
    }


    private fun activityMemoListCall(){
        l.d("start activity MainActivity")
        start<MainActivity> {
            SignalingSendData.removeObserveDbDataNotFound(this@InputMemoActivity)
            putExtra("callActivity", "ActivityInputMemo")
            putExtra(this, currentMemoTableData)
        }
        finish()
    }





    // 신규 메모일때 초기 데이터 세팅
    @SuppressLint("SimpleDateFormat")
    private fun dataSetting(){
        val now = System.currentTimeMillis()
        val date = Date(now)

        currentMemoTableData.save_time = ConvertDate.getUtcEpochSeconds()
        binding.tvSaveTime.text = ConvertDate.getLocalDateTime(currentMemoTableData.save_time)

        launch {
            delay(500)
            binding.tvInputMemoTitle.text = resources.getString(R.string.create_new_memo)
            if(currentMemoTableData.user_id.isNotEmpty() && currentMemoTableData.user_name.isNotEmpty()){
                binding.tvUserId.text = currentMemoTableData.user_id
                binding.tvUserName.text = currentMemoTableData.user_name
            }else{
                binding.tvUserId.text = dataUser?.userId
                binding.tvUserName.text = dataUser?.userName
            }

            if(currentMemoTableData.memo_contents.isNotEmpty()){
                binding.etMemoInput.setText(currentMemoTableData.memo_contents)
                binding.tvInputHint.visibility = View.GONE
            }else{
                l.d("dataSetting : isEmpty :: ${binding.etMemoInput.text.toString()}")
                if(binding.etMemoInput.text.isNullOrEmpty()){
                    binding.tvClearText.visibility = View.GONE
                    binding.tvClearTextAll.visibility = View.GONE
                    binding.tvInputHint.visibility = View.VISIBLE
                }else{
                    binding.tvClearText.visibility = View.VISIBLE
                    binding.tvClearTextAll.visibility = View.VISIBLE
                    binding.tvInputHint.visibility = View.GONE
                }
            }
        }
    }


    private fun showDeleteDialog(){
        l.d("show Delete dialog")
        deleteDialog.showDialog(currentMemoTableData.memo_seq){
            //해당 메모가 파일 업로드 중이면 중지 시킴
            SftpUploadManager.stopUpload(currentMemoTableData.memo_seq)

            progressDialog.setMessage(resources.getString(R.string.delete_progress))
            progressDialog.setCancelable(true)
            progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Horizontal)
            progressDialog.show()

            // 해당메모에 포함된 파일이름들을 받는다.
            SignalingSendData.selectDeleteFileName(currentMemoTableData.memo_seq){ result ->
                val temp = (result as ArrayList<*>).filterIsInstance<ArrayList<String>>()
                val deleteOriginalFileList = temp[0]
                val deleteThumbnailFileList = temp[1]
                fileDeleteTotalCount = deleteOriginalFileList.size

                // 사진 파일 삭제
                for (i in deleteOriginalFileList.indices) {
                    sftpOriginalFileDelete(deleteOriginalFileList[i])
                }

                // 썸네일 사진 파일 삭제
                for (i in deleteThumbnailFileList.indices) {
                    sftpThumbFileDelete(deleteThumbnailFileList[i])
                }

            }
        }

    }


    private fun sftpOriginalFileDelete(deleteFileName: String) {
        val type = SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER //"/Original/"
        bindService?.sftpConnect?.deleteFile(deleteFileName, type){
            fileDeleteResultCount++
            if (fileDeleteTotalCount == fileDeleteResultCount) {
                l.d("EVENT_DELETE_ORIGINAL_FILE_OK")
                SignalingSendData.sendAllDeleteQuery(currentMemoTableData.memo_seq){
                    l.d("EVENT_DELETE_DATA_RESULT")

                    val deleteResult = it.toString().toInt()
                    //result data => [int (-1: unknown , 0: fail , 1: success))]
                    if (deleteResult == -1) {
                        toast(resources.getString(R.string.delete_error))
                        activityMemoListCall()
                    } else if (deleteResult == 0) {
                        toast(resources.getString(R.string.fail))
                        activityMemoListCall()
                    } else if (deleteResult == 1) {
                        SignalingSendData.sendDeleteMemoQuery(currentMemoTableData.memo_seq){ deleteSuccessMemoResult ->
                            val deleteMemoResult = deleteSuccessMemoResult.toString().toInt()
                            successDeleteMemo(deleteMemoResult)
                        }
                    }
                }
            }
        }
    }

    private fun successDeleteMemo(result: Int){
        // result data => [int (-1: unknown , 0: fail , 1: success))]
        if (result == -1) {
            toast(resources.getString(R.string.delete_error))
            activityMemoListCall()
        } else if (result == 0) {
            toast(resources.getString(R.string.fail))
            activityMemoListCall()
        } else if (result == 1) {
            if (progressDialog.isShowing) {
                progressDialog.dismiss()
            }
            activityMemoListCall()
        }
    }


    private fun sftpThumbFileDelete(deleteFileName: String) {
        val type = SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER //"/Original/"
        bindService?.sftpConnect?.deleteFile(deleteFileName, type){
            l.d("EVENT_DELETE_THUMBNAIL_FILE_OK")
        }
    }




    private var powerStoreManager = PowerStoreManager(
        this
    ) { observable, o ->
        val data = o as PowerReceiverData
        l.d("Noah powerManager onUpdateMessage!! " + data.messageId)
        data.print()

        // onDestroy(); 여기는 종료 전에 해야될 사용함수
        onDestroy()
        if (data.messageId == POWERDEFINE.MESSAGE_ID.PROCESS_KILL.value()) {
            finishAndRemoveTask()
            Process.killProcess(Process.myPid())
        }
    }


    private fun connectBindService(){
        val intent = Intent(this, SftpService::class.java)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    private var mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // 서비스와 연결되었을 때 호출되는 메서드
            // 서비스 객체를 전역변수로 저장
            val localBinder: SftpService.LocalBinder = service as SftpService.LocalBinder
            bindService = localBinder.getService() // 서비스가 제공하는 메소드 호출하여
            // 서비스쪽 객체를 전달받을수 있음
            l.d("onServiceConnected:========== ")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            // 서비스와 연결이 끊겼을 때 호출되는 메서드
            l.d("onServiceDisconnected:======== ")
            toast("서비스 연결 해제")
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        l.d("dataSetting: isEmpty(" + binding.etMemoInput.text.toString())
        l.d("onBackPressed")
    }

    private fun handleInsertMemoData(resultForInsert: Int){
        l.d("handleMessage: ==============EVENT_INSERT_DATA_RESULT")
        when(resultForInsert){
            -1 -> {
                toast(resources.getString(R.string.insert_error))
                start<InputMemoActivity> {
                    putExtra(this, currentMemoTableData)
                }
                finish()
            }
            0 -> {
                toast(resources.getString(R.string.fail))
                start<InputMemoActivity> {
                    putExtra(this, currentMemoTableData)
                }
                finish()
            }
            1 -> {
                SignalingSendData.selectLastNumberQuery(currentMemoTableData.user_id, currentMemoTableData.en_seq?:-1) {
                    handleSelectLastNumberQuery(it.toString().toInt())
                }
            }
        }
    }

    private fun handleSelectLastNumberQuery(result: Int){
        currentMemoTableData.memo_seq = result
        createThumbnail(fileDir, fileName)
        bindService?.sftpConnect?.fileUpload(
            SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER,
            fileDir,
            fileName,
            currentMemoTableData.memo_seq,
            "P"
        ){
            l.d("file upload complete")
        }
    }

    private fun createThumbnail(originalPath: String, fileName: String) {
        l.d("create thumb nail")
        var bitmap: Bitmap
        bitmap = BitmapFactory.decodeFile(originalPath + fileName)
        val thumbnailPath = fileDir
        val dir = File(thumbnailPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val fileCacheItem = File(thumbnailPath + "t_" + fileName)
        var out: OutputStream? = null
        try {
            out = FileOutputStream(fileCacheItem)
            //160 부분을 자신이 원하는 크기로 변경할 수 있습니다.
//            bitmap = Bitmap.createScaledBitmap(bitmap, 416, height/(width/200), true);
//            bitmap = Bitmap.createScaledBitmap(bitmap, 160, 90, true);
            bitmap = Bitmap.createScaledBitmap(bitmap, 854, 480, true)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            refreshGallery(fileCacheItem)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                out!!.close()
                l.d("reset cam before")
//                resetCam()
                uploadThumbnailFile(thumbnailPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var currentPictureData: ByteArray?=null
    private fun saveImage(){

        Log.d(TAG, "Noah ==== saveImage")

//        val orientation: Int =
//            PowerCameraPreviewActivity.setCameraDisplayOrientation(
//                this,
//                CAMERA_FACING, camera!!
//            )
//        val matrix = Matrix()
//        matrix.postRotate(orientation.toFloat())
        val stream = ByteArrayOutputStream()

        Log.d(TAG, "Noah ==== saveImage ==== localBitmap" + localBitmap)

        if (localBitmap != null) {
            //localBitmap = cropBitmap(localBitmap!!)
            localBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            currentPictureData = stream.toByteArray()
            onLaunchDictation()
        }
    }

    private fun onLaunchDictation(){

        Log.d(TAG, "Noah ==== onLaunchDictation()" + currentMemoTableData.en_seq)

        val enSeq = currentMemoTableData.en_seq?:-1
        fileName = if (enSeq >= 0) {
            if (enSeq < 10) {
                "00" + enSeq + "_" + getNowDate() + ".jpg"
            } else if (enSeq < 100) {
                "0" + enSeq + "_" + getNowDate() + ".jpg"
            } else {
                enSeq.toString() + "_" + getNowDate() + ".jpg"
            }
        } else {
            getNowDate() + ".jpg" //temp
        }

        launch(Dispatchers.Default) {
            var outStream: FileOutputStream?=null
            // Write to SD Card
            try {
                fileDir = SignalingSendData.getRootPath() + SignalingSendData.EVENT_CREATE_TEMP_FOLDER

                val dir = File(fileDir)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                if(fileName.trim().isEmpty()){
                    fileName = String.format("%d", System.currentTimeMillis())
                }
                val outFile = File(dir, fileName)
                outStream = FileOutputStream(outFile)
                outStream.write(currentPictureData)
                outStream.flush()
                outStream.close()
                l.d("onPictureTaken - wrote bytes: " + currentPictureData?.size + " to " + outFile.absolutePath)
                refreshGallery(outFile)
                currentPictureData = null


            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (currentMemoTableData.memo_seq != 0) {
                    createThumbnail(fileDir, fileName)
                    bindService?.sftpConnect?.fileUpload(
                        SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER,
                        fileDir,
                        fileName,
                        currentMemoTableData.memo_seq,
                        "P"
                    ){
                        l.d("original file upload complete")
                    }
                } else {
                    val memoTblData = MemoTblData()
                    memoTblData.dbUserID = currentMemoTableData.user_id
                    memoTblData.dbEnSeq = currentMemoTableData.en_seq
                    memoTblData.dbHqSeq = currentMemoTableData.hq_seq
                    memoTblData.dbBrSeq = currentMemoTableData.br_seq
                    memoTblData.dbUserName = currentMemoTableData.user_name
                    memoTblData.dbMemoContents = currentMemoTableData.memo_contents
                    memoTblData.dbSaveTime = currentMemoTableData.save_time
//                    memoTblData.dbQrSeq = currentMemoTableData.qr_seq
                    SignalingSendData.insertMemoData(memoTblData){ resultForInsert ->
                        handleInsertMemoData(resultForInsert.toString().toInt())
                    }
                }
            }
        }
    }

    private fun refreshGallery(file: File){
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = Uri.fromFile(file)
        sendBroadcast(mediaScanIntent)
    }


    @SuppressLint("SimpleDateFormat")
    private fun getNowDate():String{
        val now = System.currentTimeMillis()
        val date = Date(now)
        val sdf = SimpleDateFormat("yyyyMMddHHmmssSSS")

        return sdf.format(date)
    }

    private fun uploadThumbnailFile(path: String){
        l.d("start upload thumbnail file")
        bindService?.sftpConnect?.fileUpload(SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER, path, "t_$fileName", currentMemoTableData.memo_seq, "P"){
            l.d("upload thumbnail file complete")
        }
    }
}