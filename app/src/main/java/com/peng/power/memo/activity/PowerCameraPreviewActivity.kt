package com.peng.power.memo.activity



import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.peng.plant.powerreceiver.POWERDEFINE
import com.peng.plant.powerreceiver.PowerReceiverData
import com.peng.plant.powerreceiver.PowerStoreManager
import com.peng.power.memo.R
import com.peng.power.memo.broadcastreceiver.ConnectionStateMonitor
import com.peng.power.memo.data.*
import com.peng.power.memo.databinding.ActivityPowerCameraPreviewBinding
import com.peng.power.memo.dialog.ImageFileSaveDialog
import com.peng.power.memo.preference.DataUser
import com.peng.power.memo.preference.PreferenceValue
import com.peng.power.memo.sftp.SftpService
import com.peng.power.memo.sftp.SftpUploadManager
import com.peng.power.memo.util.l
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import splitties.activities.start
import splitties.toast.toast
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class PowerCameraPreviewActivity :BaseActivity<ActivityPowerCameraPreviewBinding>() {
    private var connectionStateMonitor = ConnectionStateMonitor()
    private var bindService: SftpService?=null

    private var camera:Camera?=null
    private var parameters:Camera.Parameters?=null
    private var preview:Preview?=null

    private var orientationEventListener:OrientationEventListener?=null
    private val CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK

    private var imageFileSaveDialog: ImageFileSaveDialog?=null

    private var lastRotation = 0

    private var isFirst = true
    private var isFlashStatus = false

    private var fileName = ""
    private var fileDir:String = ""

    private var localBitmap:Bitmap?=null

    private lateinit var currentMemoTableData: MemoTableData

//    private var memoSeq = 0
//    private var enSeq = -1
//    private var userID:String? = null
//    private var userName:String? = null
//    private var memoContents: String? = null
//    private var saveTime: String? = null

    private var dataUser: DataUser?=null

    var display: DisplayMetrics? = null
    var fontSize: Float = 21f


    companion object{
        /**
         * @param activity
         * @param cameraId Camera.CameraInfo.CAMERA_FACING_FRONT,
         *                 Camera.CameraInfo.CAMERA_FACING_BACK
         * @param camera   Camera Orientation
         *                 reference by https://developer.android.com/reference/android/hardware/Camera.html
         */
        private fun setCameraDisplayOrientation(activity: Activity, cameraId: Int, camera: Camera):Int{
            val info = CameraInfo()
            Camera.getCameraInfo(cameraId, info)
            val rotation = activity.windowManager.defaultDisplay
                    .rotation
            var degrees = 0
            when (rotation) {
                Surface.ROTATION_0 -> degrees = 0
                Surface.ROTATION_90 -> degrees = 90
                Surface.ROTATION_180 -> degrees = 180
                Surface.ROTATION_270 -> degrees = 270
            }

            var result: Int
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360
                result = (360 - result) % 360 // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360
            }

            return result
        }
    }

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
        binding = ActivityPowerCameraPreviewBinding.inflate(layoutInflater)
    }

    override fun initAfterBinding() {
        connectionStateMonitor.enable(this)
        powerStoreManager.register()

        SignalingSendData.addObserveDbDataNotFound(this) {
            l.d("Db Data not found")

        }

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

        connectBindService()

        //command(resources.getString(R.string.go_back))
        command(resources.getString(R.string.zoom) + " 1")
        command(resources.getString(R.string.zoom) + " 2")
        command(resources.getString(R.string.zoom) + " 3")
        command(resources.getString(R.string.zoom) + " 4")
        command(resources.getString(R.string.zoom) + " 5")
        command(resources.getString(R.string.manual_focus))
        command(resources.getString(R.string.flash))
        //command(resources.getString(R.string.capture))
        //command(resources.getString(R.string.video))

        binding.tvBack.setOnClickListener {
            start<InputMemoActivity> {
                putExtra(this, currentMemoTableData)
            }
            finish()
        }

        binding.tvCaptureBtn.setOnClickListener {
            setScreenShot()
        }

        binding.tvVideo.setOnClickListener {
//            start<VideoActivity> {
//                putExtra("memo_seq", memoSeq)
//                putExtra("en_seq", enSeq)
//                putExtra("user_id", userID)
//                putExtra("user_name", userName)
//                putExtra("memo_contents", memoContents)
//                putExtra("save_time", saveTime)
//            }

            start<VideoCameraXActivity> {
                putExtra(this, currentMemoTableData)
            }

            finish()
        }



        dataUser?.menuStatus?.let{
            if(it) showMenu()
            else hideMenu()
        }

        orientationEventListener = object : OrientationEventListener(
                this,
                SensorManager.SENSOR_DELAY_UI
        ) {
            override fun onOrientationChanged(orientation: Int) {
                val display: Display = windowManager.defaultDisplay
                val rotation = display.rotation
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                    if (rotation != lastRotation) {
                        lastRotation = rotation
                        l.d("onOrientationChanged: :::::::::$rotation")
                        resetCam()
                    }
                }
            }
        }

        orientationEventListener?.let{
            if(it.canDetectOrientation()) it.enable()
        }

        binding.tvMenu.setOnClickListener {
            dataUser?.menuStatus?.let{
                if(it) hideMenu()
                else showMenu()
            }
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
            binding.tvFocus.setTextSize(fontSize)
            binding.tvFlash.setTextSize(fontSize)
            binding.tvFlashStatus.setTextSize(fontSize)
            binding.tvMenu.setTextSize(fontSize)
            binding.tvPhoto.setTextSize(fontSize)
            binding.tvVideo.setTextSize(fontSize)
            binding.tvCaptureBtn.setTextSize(fontSize)
        }

        setEmergencyCall()

    }

    override fun onResume() {
        super.onResume()
        startCamera()
    }


    override fun onPause() {
        super.onPause()
            l.d("onPause")
            if(camera != null){
                camera?.stopPreview()
                preview?.setCamera(null)
//                camera?.release()                     //2022-09-02   주석처리    카메라 촬영 후 취소시 오류 발생
                camera = null
                isFirst = false
            }


        binding.layout.removeView(preview)
        preview = null
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            SftpUploadManager.saveUploadingInfo(this)
            SignalingSendData.removeObserveDbDataNotFound(this)
            connectionStateMonitor.disable()
            powerStoreManager.unregister()
            orientationEventListener?.disable()
            dataUser?.zoom = 0
        } catch (e:java.lang.Exception) {
            Log.d("Noah ", e.stackTraceToString())
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
    }

    private fun connectBindService(){
        val intent = Intent(this, SftpService::class.java)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val localBinder = service as SftpService.LocalBinder
            bindService = localBinder.getService()
            l.d("onServiceConnected : =============")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            l.d("onServiceDisconnected")
            toast("서비스 연결 해제")
        }
    }


    private fun startCamera(){
        l.d("startCamera::::::::::: ")
        if(preview == null){
            l.e("preview is null")
            preview = Preview(this, binding.surfaceView)
            preview?.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            )
            binding.layout.addView(preview)
            preview?.keepScreenOn = true
        }

        preview?.setCamera(null)
        if(camera != null){
            l.e("camera is not null")
//            camera?.release()                 //2022-09-02   주석처리    카메라 촬영 후 취소시 오류 발생
            camera = null
        }

        l.d("before get number of cameras")
        val numCams = Camera.getNumberOfCameras()
        if(numCams > 0){
            try {
                camera =
                        Camera.open(CAMERA_FACING)
                // camera orientation
                camera?.setDisplayOrientation(
                        setCameraDisplayOrientation(
                                this, CAMERA_FACING,
                                camera!!
                        )
                )
                // get Camera parameters
                val params = camera?.getParameters()
                val pictureSizeList = params?.supportedPictureSizes
                val previewSizeList = params?.supportedPreviewSizes
                var picWidthMaxSize = 1920
                var picHeightMaxSize = 1080
//                for (size in pictureSizeList!!) {
//                    l.d("Noah pictureSize : " + size.width + "/" + size.height)
//                    picWidthMaxSize = size.width
//                    picHeightMaxSize = size.height
//                    //break
//                }

                for(size in previewSizeList!!){
                    l.d("Noah pictureSize : " + size.width + "/" + size.height)
                }
                params.setRotation(
                        setCameraDisplayOrientation(
                                this,
                                CAMERA_FACING,
                                camera!!
                        )
                )


                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q){
                    params.setPreviewSize(1920,1080)
                    //params.setPictureSize(3840, 2160)
                    //params.setPictureSize(4608, 3456)
                }else{
                    params.setPictureSize(3840, 2160)
//                    params.setPictureSize(1920, 1080)                     // 2023-07-12      해상도 줄임
                }
                camera?.setParameters(params)
                camera?.startPreview()
//                l.d("Noah Camera preview size: " + params.previewSize.width + " x " + params.previewSize.height)
//                l.d("Noah Camera preview-picture size: " + params.pictureSize.width + " x " + params.pictureSize.height)
                parameters = camera?.getParameters()
                if (isFirst) {
                    parameters?.setZoom(0)
                    camera?.setParameters(parameters)
                } else {
                    parameters?.setZoom(dataUser?.zoom!!)
                    camera?.setParameters(parameters)
                }
            } catch (ex: RuntimeException) {
                toast("camera_not_found " + ex.message.toString())
                l.d("camera_not_found " + ex.message.toString())
                return
            }

            preview?.setCamera(camera)

        }else{
            l.e("number of cameras == 0")
        }

    }


    private fun command(name: String){
        val createTextView = CreateTextView(this)
        createTextView.setText(name){ view ->
            //back ground command
            if (view is TextView) {
                if (view.text.toString() == resources.getString(R.string.go_back)) {
                    start<InputMemoActivity> {
                        putExtra(this, currentMemoTableData)
                    }
                    finish()
                } else if (view.text.toString() == resources.getString(R.string.zoom) + " 1") {
                    binding.tvZoom1.setTextColor(getColor(R.color.text_yellow))
                    binding.tvZoom2.setTextColor(Color.WHITE)
                    binding.tvZoom3.setTextColor(Color.WHITE)
                    binding.tvZoom4.setTextColor(Color.WHITE)
                    binding.tvZoom5.setTextColor(Color.WHITE)
                    parameters?.zoom = 0
                    dataUser?.zoom = 0
                    camera!!.parameters = parameters
                    camera?.autoFocus { success, _ ->
                        if (!success) toast(resources.getString(R.string.fail_focusing))

                    }
                } else if (view.text.toString() == resources.getString(R.string.zoom) + " 2") {
                    binding.tvZoom1.setTextColor(Color.WHITE)
                    binding.tvZoom2.setTextColor(getColor(R.color.text_yellow))
                    binding.tvZoom3.setTextColor(Color.WHITE)
                    binding.tvZoom4.setTextColor(Color.WHITE)
                    binding.tvZoom5.setTextColor(Color.WHITE)
                    parameters?.setZoom(10)
                    dataUser?.zoom = 10
                    camera!!.parameters = parameters
                    camera?.autoFocus { success, _ ->
                        if (!success) toast(resources.getString(R.string.fail_focusing))
                    }
                } else if (view.text.toString() == resources.getString(R.string.zoom) + " 3") {
                    binding.tvZoom1.setTextColor(Color.WHITE)
                    binding.tvZoom2.setTextColor(Color.WHITE)
                    binding.tvZoom3.setTextColor(getColor(R.color.text_yellow))
                    binding.tvZoom4.setTextColor(Color.WHITE)
                    binding.tvZoom5.setTextColor(Color.WHITE)
                    parameters?.setZoom(30)
                    dataUser?.zoom = 30
                    camera!!.parameters = parameters
                    camera!!.autoFocus { success, _ ->
                        if (!success) toast(resources.getString(R.string.fail_focusing))
                    }
                } else if (view.text.toString() == resources.getString(R.string.zoom) + " 4") {
                    binding.tvZoom1.setTextColor(Color.WHITE)
                    binding.tvZoom2.setTextColor(Color.WHITE)
                    binding.tvZoom3.setTextColor(Color.WHITE)
                    binding.tvZoom4.setTextColor(getColor(R.color.text_yellow))
                    binding.tvZoom5.setTextColor(Color.WHITE)
                    parameters?.setZoom(60)
                    dataUser?.zoom = 60
                    camera!!.parameters = parameters
                    camera?.autoFocus { success, camera ->
                        if (!success) toast(resources.getString(R.string.fail_focusing))
                    }
                } else if (view.text.toString() == resources.getString(R.string.zoom) + " 5") {
                    binding.tvZoom1.setTextColor(Color.WHITE)
                    binding.tvZoom2.setTextColor(Color.WHITE)
                    binding.tvZoom3.setTextColor(Color.WHITE)
                    binding.tvZoom4.setTextColor(Color.WHITE)
                    binding.tvZoom5.setTextColor(getColor(R.color.text_yellow))
                    parameters?.setZoom(88)
                    dataUser?.zoom = 88
                    camera!!.parameters = parameters
                    camera?.autoFocus { success, _ ->
                        if (!success) toast(resources.getString(R.string.fail_focusing))
                    }
                } else if (view.text.toString() == resources.getString(R.string.manual_focus)) {
                    camera!!.autoFocus { success, _ ->
                        if (!success) toast(resources.getString(R.string.fail_focusing))
                    }
                } else if (view.text.toString() == resources.getString(R.string.flash)) {
                    if (!isFlashStatus) {
                        parameters?.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
                        camera!!.parameters = parameters
                        isFlashStatus = true
                        binding.tvFlashStatus.text = resources.getString(R.string.flash_on)
                    } else {
                        parameters?.setFlashMode(Camera.Parameters.FLASH_MODE_OFF)
                        camera!!.parameters = parameters
                        isFlashStatus = false
                        binding.tvFlashStatus.setText(resources.getString(R.string.flash_off))
                    }
                }
//                else if (view.text.toString() == resources.getString(R.string.capture)) {
//                    setScreenShot()
//                } else if (view.text.toString() == resources.getString(R.string.video)) {
//                    start<VideoActivity> {
//                        putExtra("memo_seq", memoSeq)
//                        putExtra("en_seq", enSeq)
//                        putExtra("user_id", userID)
//                        putExtra("user_name", userName)
//                        putExtra("memo_contents", memoContents)
//                        putExtra("save_time", saveTime)
//                    }
//                    finish()
//                }
            }
        }
        binding.llCommand.addView(createTextView)
    }


    private fun showMenu() {
        binding.flGoBack.visibility = View.VISIBLE
        binding.flZoom.visibility = View.VISIBLE
        binding.tvFocus.visibility = View.VISIBLE
        binding.llFlash.visibility = View.VISIBLE
        binding.llModeSelect.visibility = View.VISIBLE
        binding.tvMenu.text = resources.getString(R.string.hide_menu)
        dataUser?.menuStatus = true
    }

    private fun hideMenu() {
        binding.flGoBack.visibility = View.GONE
        binding.flZoom.visibility = View.GONE
        binding.tvFocus.visibility = View.GONE
        binding.llFlash.visibility = View.GONE
        binding.llModeSelect.visibility = View.GONE
        binding.tvMenu.text = resources.getString(R.string.show_menu)
        dataUser?.menuStatus = false
    }


    private fun resetCam(){
        isFirst = false
        startCamera()
        l.d("resetCam::::::::::: ")
    }


    private fun setScreenShot() {
        camera!!.autoFocus { success, camera ->
            if (success) {
                camera.takePicture(shutterCallback, rawCallback, jpegCallback)
            } else {
                Toast.makeText(
                        applicationContext,
                        resources.getString(R.string.fail_focusing),
                        Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val shutterCallback = Camera.ShutterCallback {
        l.d("onShutter'd")
    }

    private val rawCallback = Camera.PictureCallback{ data, camera ->
        l.d("onPictureTaken - raw")
    }

    private val jpegCallback = Camera.PictureCallback{ data, camera ->
        l.d("onJpegCallback")
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)

        localBitmap = cropBitmap(bitmap)
        localBitmap?.let{
            showSaveImageDialog(it)
        }
    }


    private fun showSaveImageDialog(bitmap: Bitmap){
        if(imageFileSaveDialog ==null){
            imageFileSaveDialog = ImageFileSaveDialog(this, bitmap){
                when(it){
                    0 -> { // save
                        saveImage()
//                        startCamera()
                    }
                    1 -> { // cancel
                        try{
                        camera?.stopPreview()
                        preview = null
                        preview?.setCamera(null)
//                        camera?.release()                 //2022-09-02   주석처리    카메라 촬영 후 취소시 오류 발생
                        camera = null

                            localBitmap = null
                            resetCam()
                        } catch (e:java.lang.Exception) {
                            Log.d("Noah Camera Cancel ", e.stackTraceToString())
                        }
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

    var currentPictureData: ByteArray?=null
    private fun saveImage(){
        val orientation: Int =
                setCameraDisplayOrientation(
                        this,
                        CAMERA_FACING, camera!!
                )

        val matrix = Matrix()
        matrix.postRotate(orientation.toFloat())
        val stream = ByteArrayOutputStream()
        if (localBitmap != null) {
            //localBitmap = cropBitmap(localBitmap!!)
            localBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            currentPictureData = stream.toByteArray()
            onLaunchDictation()
        }
    }


    private fun cropBitmap(bitmap:Bitmap):Bitmap{
        val originalWidth = bitmap.width
        l.d("bitmap width : $originalWidth, height : ${bitmap.height}")
        if(originalWidth != 4608){
            return bitmap
        }
        return Bitmap.createBitmap(bitmap, 384, 648, 3840, 2160)                          // 2023-07-12      해상도 줄임
//        return Bitmap.createBitmap(bitmap, 384, 648, 1920, 1080)
    }


    private fun onLaunchDictation(){
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
            var outStream:FileOutputStream?=null
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
                    SignalingSendData.insertMemoData(memoTblData){ resultForInsert ->
                        handleInsertMemoData(resultForInsert.toString().toInt())
                    }
                }
            }
        }
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


    private fun uploadThumbnailFile(path: String){
        l.d("start upload thumbnail file")
        bindService?.sftpConnect?.fileUpload(SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER, path, "t_$fileName", currentMemoTableData.memo_seq, "P"){
            l.d("upload thumbnail file complete")
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
                resetCam()
                uploadThumbnailFile(thumbnailPath)
            } catch (e: Exception) {
                e.printStackTrace()
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
}