package com.peng.power.memo.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.peng.plant.powerreceiver.POWERDEFINE
import com.peng.plant.powerreceiver.PowerReceiverData
import com.peng.plant.powerreceiver.PowerStoreManager
import com.peng.power.memo.broadcastreceiver.ConnectionStateMonitor
import com.peng.power.memo.data.MemoTblData
import com.peng.power.memo.data.SignalingSendData
import com.peng.power.memo.databinding.ActivityVideoCameraXBinding
import com.peng.power.memo.preference.DataUser
import com.peng.power.memo.preference.PreferenceValue
import com.peng.power.memo.sftp.SftpService
import com.peng.power.memo.sftp.SftpConnect
import com.peng.power.memo.util.l
import com.peng.power.memo.R
import com.peng.power.memo.data.MemoTableData
import com.peng.power.memo.dialog.VideoFileSaveDialog
import com.peng.power.memo.sftp.SftpUploadManager
import kotlinx.coroutines.*
import splitties.activities.start
import splitties.toast.toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class VideoCameraXActivity : BaseActivity<ActivityVideoCameraXBinding>() {

    private var connectionStateMonitor = ConnectionStateMonitor()

    private lateinit var currentMemoTableData: MemoTableData

    private val CameraSettingDir = "camerasettingdir"
    private val CameraBitrate = "camerabitrate"

//    private var memoSeq = 0
//    private var enSeq = -1
//    private var userID = ""
//    private var userName = ""
//    private var memoContents: String? = null
//    private var saveTime: String? = null

    var bindService: SftpService? = null
    private var fileName: String? = null
    private var thumbnailBitmap: Bitmap? = null


    private val sftpConnect = SftpConnect(this)
    private var dataUser: DataUser?=null

    var display: DisplayMetrics? = null
    var fontSize: Float = 21f

    // An instance for display manager to get display change callbacks
    private val displayManager by lazy { getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }

    private val RATIO_4_3_VALUE = 4.0 / 3.0 // aspect ratio 4x3
    private val RATIO_16_9_VALUE = 16.0 / 9.0 // aspect ratio 16x9

    private val fhdBitrate = 4*1024*1024  //4mbps
    private val hdBitrate = 2621440   //2.5mbps
    private val fhdResolution: Size = Size(1920, 1080)
    private val hdResolution: Size = Size(1280, 720)
    private var selectedResolution: Size = fhdResolution
    //private var selectedBitRate:Int = fhdBitrate

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture? = null

    private var displayId = -1

    // Selector showing which camera is selected (front or back)
    private var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA


    private var currentRecordedFile: File?=null


    // Selector showing is recording currently active
    private var isRecording = false

//    private val animateRecord by lazy {
//        ObjectAnimator.ofFloat(binding.ibRecordeStop, View.ALPHA, 1f, 0.5f).apply {
//            duration = 1000
//            repeatMode = ObjectAnimator.REVERSE
//            repeatCount = ObjectAnimator.INFINITE
//            doOnCancel { binding.ibRecordeStop.alpha = 1f }
//        }
//    }


    private var selectedBitRate:Int by Delegates.observable(fhdBitrate){ _, _, newValue ->
        // save sharedpreferences camera bitrate
        getSharedPreferences(CameraSettingDir, 0).edit().putInt(
                CameraBitrate, newValue).apply()
    }



    private var zoomLevel:Int by Delegates.observable(1){ _, oldValue, newValue->
        if(oldValue>0)
            zoomNumberTexts[oldValue - 1].setTextColor(Color.WHITE)
        if(newValue>0)
            zoomNumberTexts[newValue - 1].setTextColor(getColor(R.color.text_yellow))

        if(camera == null){
            Log.d("VideoFragment","camera is null")
        }

        when(newValue){
            2 -> {
                camera?.cameraControl?.setZoomRatio(1.6f)
            }
            3 -> {
                camera?.cameraControl?.setZoomRatio(3.0f)
            }
            4 -> {
                camera?.cameraControl?.setZoomRatio(4.5f)
            }
            5 -> {
                camera?.cameraControl?.setZoomRatio(5.5f)
            }
            else->{ // == 1
                camera?.cameraControl?.setZoomRatio(1f)
                //camera?.cameraControl?.setLinearZoom(100f/100f)
            }
        }
    }

    private val zoomLevelTexts = ArrayList<TextView>()
    private val zoomNumberTexts = ArrayList<TextView>()

    private var videoFileSaveDialog: VideoFileSaveDialog?=null




    private fun setEmergencyCall(){
        val textView = TextView(this)
        textView.text = "긴급전화"
        textView.contentDescription = "hf_no_number"
        val lp = LinearLayout.LayoutParams(1,1)
        textView.layoutParams = lp
        textView.setOnClickListener {
            val intent = Intent("com.peng.emergencycall")
            sendBroadcast(intent)
        }
        binding.root.addView(textView)
    }

    override fun initViewBinding() {
        binding = ActivityVideoCameraXBinding.inflate(layoutInflater)
    }

    @SuppressLint("RestrictedApi")
    override fun initAfterBinding() {
        connectionStateMonitor.enable(this)
        powerStoreManager.register()


        SignalingSendData.addObserveDbDataNotFound(this) {
            l.d("Db Data not found")
        }


        connectBindService()

        videoFileSaveDialog = VideoFileSaveDialog(this)

        dataUser = PreferenceValue.getDataUser(this)

        currentMemoTableData = getMemoTableDataFromJsonString(intent)

        // 2023-02-06    Navigate520으로 인해 DP로 구분짓기 위함
        display = this.applicationContext?.resources?.displayMetrics

//        memoSeq = intent.getIntExtra("memo_seq", 0)
//        enSeq = intent.getIntExtra("en_seq", -1)
//        userID = intent.getStringExtra("user_id")
//        userName = intent.getStringExtra("user_name")
//        memoContents = intent.getStringExtra("memo_contents")
//        saveTime = intent.getStringExtra("save_time")

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        displayManager.registerDisplayListener(displayListener, null)

        binding.run {
            viewFinder.addOnAttachStateChangeListener(object :
                View.OnAttachStateChangeListener {
                override fun onViewDetachedFromWindow(v: View) =
                    displayManager.registerDisplayListener(displayListener, null)

                override fun onViewAttachedToWindow(v: View) =
                    displayManager.unregisterDisplayListener(displayListener)
            })

            zoomLevelTexts.add(tvZoomLevel1)
            zoomLevelTexts.add(tvZoomLevel2)
            zoomLevelTexts.add(tvZoomLevel3)
            zoomLevelTexts.add(tvZoomLevel4)
            zoomLevelTexts.add(tvZoomLevel5)

            for(i in zoomLevelTexts.indices){
                zoomLevelTexts[i].setOnClickListener {
                    zoomLevel = i+1
                }
            }

            zoomNumberTexts.add(tvZoom1)
            zoomNumberTexts.add(tvZoom2)
            zoomNumberTexts.add(tvZoom3)
            zoomNumberTexts.add(tvZoom4)
            zoomNumberTexts.add(tvZoom5)

//            if(BuildConfig.DEBUG_MODE){
//                for(i in zoomNumberTexts.indices){
//                    zoomNumberTexts[i].setOnClickListener {
//                        zoomLevel = i+1
//                    }
//                }
//            }


            tvZoom1.setTextColor(getColor(R.color.text_yellow))



            ibRecordeStart.setOnClickListener {
                l.d("onclick record start")
                hideButtonOnRecording()
                ibRecordeStart.visibility = View.GONE
                ibRecordeStop.visibility = View.VISIBLE
                startRecordTime()
                recordVideo()
            }

            ibRecordeStop.setOnClickListener {
                showButtonOnReady()
                isRecording = false
                //animateRecord.cancel()
                ibRecordeStop.visibility = View.GONE
                ibRecordeStart.visibility = View.VISIBLE
                stopRecordTime()
                videoCapture?.stopRecording()
            }

            tvPhoto.setOnClickListener {
                if(isRecording)
                    return@setOnClickListener
                start<PowerCameraPreviewActivity> {
                    putExtra(this, currentMemoTableData)
                    finish()
                }
            }

            tvBack.setOnClickListener {
                start<InputMemoActivity> {
                    putExtra(this, currentMemoTableData)
                    finish()
                }
            }


            tvFHD.setOnClickListener {
                if(isRecording)
                    return@setOnClickListener
                tvFHD.background = resources.getDrawable(R.drawable.btn_bg_round_line, null)
                tvHD.background = resources.getDrawable(R.drawable.btn_bg_round, null)
                selectedBitRate = fhdBitrate
                selectedResolution = fhdResolution
                availableTime.text = getCurrentRemainRecordTime()
                startCamera()
            }

            tvHD.setOnClickListener {
                if(isRecording)
                    return@setOnClickListener
                tvFHD.background = resources.getDrawable(R.drawable.btn_bg_round, null)
                tvHD.background = resources.getDrawable(R.drawable.btn_bg_round_line, null)
                selectedBitRate = hdBitrate
                selectedResolution = hdResolution
                availableTime.text = getCurrentRemainRecordTime()
                startCamera()
            }

            selectedBitRate = getSharedPreferences(CameraSettingDir, 0).getInt(
                    CameraBitrate, fhdBitrate)


            if(selectedBitRate == fhdBitrate){
                tvFHD.background = resources.getDrawable(R.drawable.btn_bg_round_line, null)
                tvHD.background = resources.getDrawable(R.drawable.btn_bg_round, null)
                selectedResolution = fhdResolution
            }else{
                tvFHD.background = resources.getDrawable(R.drawable.btn_bg_round, null)
                tvHD.background = resources.getDrawable(R.drawable.btn_bg_round_line, null)
                selectedResolution = hdResolution
            }

            availableTime.text = getCurrentRemainRecordTime()

            llRecordTime.visibility = View.GONE

        }

        // 2023-02-06   Navigate520
        if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
            Log.d("Noah", " Navigate520 ")
            binding.tvBack.setTextSize(fontSize)
            binding.tvZoomLevelText.setTextSize(fontSize)
            binding.tvZoom1.setTextSize(fontSize)
            binding.tvZoom2.setTextSize(fontSize)
            binding.tvZoom3.setTextSize(fontSize)
            binding.tvZoom4.setTextSize(fontSize)
            binding.tvZoom5.setTextSize(fontSize)
            binding.tvHD.setTextSize(fontSize)
            binding.tvFHD.setTextSize(fontSize)
            binding.tvPhoto.setTextSize(fontSize)
            binding.tvVideo.setTextSize(fontSize)
            binding.ibRecordeStart.setTextSize(fontSize)
            binding.ibRecordeStop.setTextSize(fontSize)
            binding.availableTime.setTextSize(fontSize)
            binding.tvRecordTime.setTextSize(fontSize)
            binding.tvAvailableTimeBottom.setTextSize(fontSize)
        }


        setEmergencyCall()


    }


    private fun hideButtonOnRecording(){
        binding.run {
            //llAvailableTime.visibility = View.GONE
            if(selectedBitRate == fhdBitrate){
                tvFHD.background = resources.getDrawable(R.drawable.btn_bg_round, null)
                tvHD.visibility = View.GONE
            }else{
                tvHD.background = resources.getDrawable(R.drawable.btn_bg_round, null)
                tvFHD.visibility = View.GONE
            }
            llBack.visibility = View.GONE
            llModeSelect.visibility = View.GONE
        }
    }

    private fun showButtonOnReady(){
        binding.run {
            //llAvailableTime.visibility = View.VISIBLE
            if(selectedBitRate == fhdBitrate){
                tvFHD.background = resources.getDrawable(R.drawable.btn_bg_round_line, null)
                tvHD.visibility = View.VISIBLE
            }else{
                tvHD.background = resources.getDrawable(R.drawable.btn_bg_round_line, null)
                tvFHD.visibility = View.VISIBLE
            }
            llBack.visibility = View.VISIBLE
            llModeSelect.visibility = View.VISIBLE
        }
    }


    private fun connectBindService() {
        val intent = Intent(this, SftpService::class.java)
        // intent 객체, 서비스와 연결에 대한 정의
        bindService(intent, conn, BIND_AUTO_CREATE)
    }

    private var conn: ServiceConnection = object : ServiceConnection {
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
            l.d( "onServiceDisconnected:======== ")
            Toast.makeText(applicationContext, "서비스 연결 해제", Toast.LENGTH_LONG).show()
        }
    }




    /**
     * A display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit

        @SuppressLint("UnsafeExperimentalUsageError", "RestrictedApi", "UnsafeOptInUsageError")
        override fun onDisplayChanged(displayId: Int){
            if (displayId == this@VideoCameraXActivity.displayId) {
                preview?.targetRotation = displayManager.getDisplay(displayId).rotation
                videoCapture?.setTargetRotation(displayManager.getDisplay(displayId).rotation)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        l.e("onResume")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.availableTime.text = getCurrentRemainRecordTime()

        binding.ibRecordeStart.requestFocus()

        launch {
            //delay(200)
            startCamera()
        }

    }


    @SuppressLint("RestrictedApi")
    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        stopRecordTime()

        cameraProvider?.unbindAll()
    }

    @SuppressLint("RestrictedApi")
    override fun onDestroy() {
        super.onDestroy()
        stopRecordTime()
        SftpUploadManager.saveUploadingInfo(this)
        connectionStateMonitor.disable()
        powerStoreManager.unregister()
        SignalingSendData.removeObserveDbDataNotFound(this)
        displayManager.unregisterDisplayListener(displayListener)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    private var powerStoreManager = PowerStoreManager(this) { _, o ->
        val data = o as PowerReceiverData
        l.d("Noah powerManager onUpdateMessage!! " + data.messageId)
        data.print()
        onDestroy()
        if (data.messageId == POWERDEFINE.MESSAGE_ID.PROCESS_KILL.value()) {
            finishAndRemoveTask()
            Process.killProcess(Process.myPid())
        }
    }



    private var baseTime:Long = 0L
    private var jobRecordTime: Job? =null
    private var availableRecordSeconds:Long =0L

    @SuppressLint("RestrictedApi")
    private fun startRecordTime(){
        getCurrentRemainRecordTime()
        if(availableRecordSeconds <= 0){
            toast(resources.getString(R.string.not_enough_storage))
            return
        }

        if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
            Log.d("Noah", " Navigate520 reco")
            binding.tvRecordTime.setTextSize(26f)
            binding.tvAvailableTimeBottom.setTextSize(26f)
        }


        baseTime = System.currentTimeMillis()
        binding.llRecordTime.visibility = View.VISIBLE
        jobRecordTime?.cancel()

        jobRecordTime = CoroutineScope(Dispatchers.Default).launch {
            repeat(availableRecordSeconds.toInt()){
                val pastTime = (System.currentTimeMillis() - baseTime)/1000
                withContext(Dispatchers.Main){
                    binding.tvAvailableTimeBottom.text = "/ ${convertPastTimeMillsToHHMMSSColon(availableRecordSeconds)}"
                    binding.tvRecordTime.text = convertPastTimeMillsToHHMMSSColon(pastTime)
                }
                if(pastTime >= availableRecordSeconds){
                    l.d("pastTime >= availableRecordSeconds")
                    withContext(Dispatchers.Main){
                        l.d("stop recording process")
                        showButtonOnReady()
                        isRecording = false
                        //animateRecord.cancel()
                        binding.ibRecordeStop.visibility = View.GONE
                        binding.ibRecordeStart.visibility = View.VISIBLE
                        videoCapture?.stopRecording()
                        stopRecordTime()
                    }
                }

                delay(1000)
            }
            withContext(Dispatchers.Main){
                l.d("exit repeat and stop recording")
                showButtonOnReady()
                isRecording = false
                //animateRecord.cancel()
                binding.ibRecordeStop.visibility = View.GONE
                binding.ibRecordeStart.visibility = View.VISIBLE
                videoCapture?.stopRecording()
                stopRecordTime()
            }
        }
    }


    private fun stopRecordTime(){
        jobRecordTime?.cancel()
        binding.llRecordTime.visibility = View.GONE
        binding.ibRecordeStop.visibility = View.GONE
        binding.ibRecordeStart.visibility = View.VISIBLE
    }




    private fun convertPastTimeMillsToHHMMSSColon(pastTime:Long):String{
        var minutes = 0
        var seconds = 0

        if(pastTime == 3600L){
            return "60:00"
        }

        minutes = (pastTime / 60).toInt()
        seconds = (pastTime % 60).toInt()
        minutes %= 60




        var availableRecordTime = ""

        availableRecordTime += if(minutes < 10){
            "0${minutes}:"
        }else{
            "${minutes}:"
        }

        availableRecordTime += if(seconds<10){
            "0${seconds}"
        }else{
            "${seconds}"
        }

        return availableRecordTime
    }



    private fun convertPastTimeMillsToHHMMSS(pastTime:Long):String{
        var hours = 0
        var minutes = 0
        var seconds = 0

        minutes = (pastTime / 60).toInt()
        hours = minutes / 60
        seconds = (pastTime % 60).toInt()
        minutes %= 60

        var availableRecordTime = ""

        if(hours > 0){
            availableRecordTime += if(hours < 10)
                "0${hours}:"
            else
                "${hours}:"

        }


        availableRecordTime += if(minutes < 10){
            "0${minutes}:"
        }else{
            "${minutes}:"
        }

        availableRecordTime += if(seconds<10){
            "0${seconds}"
        }else{
            "${seconds}"
        }

        return availableRecordTime
    }


    private fun getCurrentRemainRecordTime():String {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong;
        val availableBlocks = stat.availableBlocksLong;

        val availableMemory = availableBlocks * blockSize - 1073741824  //- 1073741824 // -1gb ( 1024 * 1024 * 1024 ) 안드로이드 여유 내부 용량 확보 - 다른앱 고려

        l.d("available Memory : $availableMemory")

        //fhd일때 초당 620000 byte, hd일때 초당 380000
        availableRecordSeconds = if(selectedBitRate == fhdBitrate){
            availableMemory / 620000
        }else{
            availableMemory / 380000
        }

        l.d("available record seconds at getCurrentRemainRecordTime : $availableRecordSeconds")

        if(availableRecordSeconds < 0){
            availableRecordSeconds = 0
        }

        if(availableRecordSeconds > 3600){
            availableRecordSeconds = 3600
        }

//        if(availableRecordSeconds > 120){
//            availableRecordSeconds = 120
//        }



        //return resources.getString(R.string.available_record_time) + " : " + convertPastTimeMillsToHHMMSS(availableRecordSeconds)
        return convertPastTimeMillsToHHMMSS(availableRecordSeconds)
        //return formatMemorySize(availableBlocks * blockSize);
    }


    private fun formatMemorySize(memory: Long):String {
        var suffix:String? = null

        var size:Double = 0.0

        if(memory >= 1024){
            suffix = " KB";
            size = (memory / 1024).toDouble()

            if(size >= 1024) {
                suffix = " MB";
                size = (size / 1024)

                if(size >= 1024) {
                    suffix = " GB";
                    size = (size / 1024)
                }
            }
        }

        size = ((size * 10).roundToInt() /10).toDouble()

        val resultBuffer = StringBuilder(size.toString());

        if(!suffix.isNullOrEmpty()){
            resultBuffer.append(suffix);
        }

        return resultBuffer.toString();
    }




    /**
     * Unbinds all the lifecycles from CameraX, then creates new with new parameters
     * */
    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        zoomLevel = 1
        // This is the Texture View where the camera will be rendered
        val viewFinder = binding.viewFinder

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // The display information
            val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
            // The ratio for the output image and preview
            val aspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
            // The display rotation
            val rotation = viewFinder.display.rotation

            val localCameraProvider = cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

            // The Configuration of camera preview
            preview = Preview.Builder()
                .setTargetAspectRatio(aspectRatio) // set the camera aspect ratio
                .setTargetRotation(rotation) // set the camera rotation
                //.setTargetResolution(Size(1280, 720))
                .build()

            val videoCaptureConfig =
                VideoCapture.DEFAULT_CONFIG.config // default config for video capture
            // The Configuration of video capture
            videoCapture = VideoCapture.Builder
                .fromConfig(videoCaptureConfig)
                .setBitRate(selectedBitRate)
                .setDefaultResolution(selectedResolution)
                //.setTargetResolution(Size(1280, 720))
                .setMaxResolution(selectedResolution)
                .build()

            localCameraProvider.unbindAll() // unbind the use-cases before rebinding them

            try {
                // Bind all use cases to the camera with lifecycle
                camera = localCameraProvider.bindToLifecycle(
                    this, // current lifecycle owner
                    lensFacing, // either front or back facing
                    preview, // camera preview use case
                    videoCapture, // video capture use case
                )

                // Attach the viewfinder's surface provider to preview use case
                preview?.setSurfaceProvider(viewFinder.surfaceProvider)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     *  Detecting the most suitable aspect ratio for current dimensions
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    private val outputDirectory:String by lazy {
        SignalingSendData.getRootPath() + SignalingSendData.EVENT_CREATE_TEMP_FOLDER
    }


    @SuppressLint("SimpleDateFormat")
    fun getNowDate(): String? {
        val now = System.currentTimeMillis()
        val date = Date(now)
        val sdf = SimpleDateFormat("yyyyMMddHHmmssSSS")
        return sdf.format(date)
    }


    private fun checkPermissionForRecordAudio(){
        TedPermission.with(this).setPermissionListener(permissionListener)
                .setPermissions(Manifest.permission.RECORD_AUDIO).check()
    }

    private var permissionListener: PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            l.d("permission granted")
        }

        override fun onPermissionDenied(deniedPermissions: java.util.ArrayList<String?>?) {
            l.d("permission denied")
            toast("오디오 녹음 권한이 허용되지 않으면 비디오를 실행할 수 없습니다.")
        }
    }


    @SuppressLint("RestrictedApi")
    private fun recordVideo() {
        val localVideoCapture = videoCapture ?: throw IllegalStateException("Camera initialization failed.")

        val enSeq = currentMemoTableData.en_seq?:-1



        fileName = if (enSeq >= 0) {
            if (enSeq < 10) {
                "00" + enSeq + "_" + getNowDate()
            } else if (enSeq < 100) {
                "0" + enSeq + "_" + getNowDate()
            } else {
                enSeq.toString() + "_" + getNowDate()
            }
        } else {
            getNowDate()
        }

        File(outputDirectory).mkdirs()
        currentRecordedFile = File("$outputDirectory$fileName.mp4")
        val outputOptions = VideoCapture.OutputFileOptions.Builder(currentRecordedFile!!).build()





        // Options fot the output video file
//        val outputOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            val contentValues = ContentValues().apply {
//                put(MediaStore.MediaColumns.DISPLAY_NAME, getNowDate())
//                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
//                put(MediaStore.MediaColumns.RELATIVE_PATH, outputDirectory)
//            }
//
//            contentResolver.run {
//                val contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
//
//                VideoCapture.OutputFileOptions.Builder(this, contentUri, contentValues)
//            }
//        } else {
//            File(outputDirectory).mkdirs()
//            currentRecordedFile = File("$outputDirectory${getNowDate()}.mp4")
//
//            VideoCapture.OutputFileOptions.Builder(currentRecordedFile!!)
//        }.build()

        if (!isRecording) {
            //animateRecord.start()
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                checkPermissionForRecordAudio()
                return
            }
            localVideoCapture.startRecording(
                outputOptions, // the options needed for the final video
                ContextCompat.getMainExecutor(this), // the executor, on which the task will run
                object : VideoCapture.OnVideoSavedCallback { // the callback after recording a video
                    override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                        // Create small preview
//                        outputFileResults.savedUri?.path?.let {
//                            refreshGallery(File(it))
//                        }
                        //animateRecord.cancel()
                        showButtonOnReady()
                        stopRecordTime()
                        currentRecordedFile?.let{
                            videoFileSaveDialog?.showDialog(it.path){ isSelectSave ->
                                if(!isSelectSave){
                                    if(it.exists()){
                                        if(it.delete())
                                            Log.d("onVideoSaved :","delete file : $it")
                                        else
                                            Log.d("onVideoSaved :","delete fail")
                                        startCamera()
                                        refreshGallery(it)
                                    }
                                }else{
                                    startCamera()
                                    refreshGallery(it)
                                    onSave()
                                }
                                binding.availableTime.text = getCurrentRemainRecordTime()
                            }
                        }
                    }

                    override fun onError(
                        videoCaptureError: Int,
                        message: String,
                        cause: Throwable?
                    ) {
                        stopRecordTime()
                        // This function is called if there is an error during recording process
                        //animateRecord.cancel()
                        val msg = "Video capture failed: $message"
                        toast(msg)
                        cause?.printStackTrace()
                    }
                })
        } else {
            //animateRecord.cancel()
            localVideoCapture.stopRecording()
        }
        isRecording = !isRecording
    }




    private fun onSave(){
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource("$outputDirectory$fileName.mp4")
        thumbnailBitmap = mediaMetadataRetriever.getFrameAtTime(1000000)

        if (currentMemoTableData.memo_seq != 0) {
            saveBitmapToJPG(thumbnailBitmap, fileName)
            //P -> Photo / V -> Video
            sftpConnect.fileUpload(SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER, outputDirectory, "$fileName.mp4", currentMemoTableData.memo_seq, "V"){
                l.d("file upload complete ===== callback on videocameraxactivity")
            }
        } else {
            val memoTblData = MemoTblData()
            memoTblData.dbEnSeq = currentMemoTableData.en_seq
            memoTblData.dbHqSeq = currentMemoTableData.hq_seq
            memoTblData.dbBrSeq = currentMemoTableData.br_seq
            memoTblData.dbUserID = currentMemoTableData.user_id
            memoTblData.dbUserName = currentMemoTableData.user_name
            memoTblData.dbMemoContents = currentMemoTableData.memo_contents
            memoTblData.dbSaveTime = currentMemoTableData.save_time
            SignalingSendData.insertMemoData(memoTblData){ result ->
                l.d("====== event insert data result")
                when (result.toString().toInt()) {
                    -1 -> toast(resources.getString(com.peng.power.memo.R.string.insert_error))
                    0 -> toast(resources.getString(com.peng.power.memo.R.string.fail))
                    1 -> SignalingSendData.selectLastNumberQuery(currentMemoTableData.user_id, currentMemoTableData.en_seq?:-1) {
                        currentMemoTableData.memo_seq = it.toString().toInt()

                        //P -> Photo / V -> Video
                        saveBitmapToJPG(thumbnailBitmap, fileName)
                        sftpConnect.fileUpload(SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER, outputDirectory, "$fileName.mp4", currentMemoTableData.memo_seq, "V") {
                            l.d("file upload complete ===== callback on videocameraxactivity -- new memo")
                        }
                    }
                }
            }
        }
    }


    private fun saveBitmapToJPG(bitmap: Bitmap?, fileName: String?) { //date
        val fileItem = File(outputDirectory + "t_" + fileName + ".jpg")
        var outStream: OutputStream? = null
        try {
            fileItem.createNewFile()
            outStream = FileOutputStream(fileItem)
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                outStream!!.close()
                sftpConnect.fileUpload(SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER, outputDirectory, fileItem.name, currentMemoTableData.memo_seq, "V") { result ->
                    l.d("thumbnail file upload success")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    private fun refreshGallery(file: File) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = Uri.fromFile(file)
        sendBroadcast(mediaScanIntent)
    }

    override fun onBackPressed() {
//        super.onBackPressed();
        l.d( "onBackPressed")
    }

}