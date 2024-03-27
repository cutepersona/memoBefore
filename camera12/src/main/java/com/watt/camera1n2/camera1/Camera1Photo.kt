package com.watt.camera1n2.camera1

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.watt.camera1n2.ImageFileSaveDialog
import com.watt.camera1n2.R
import com.watt.camera1n2.databinding.Camera1PhotoBinding
import com.watt.camera1n2.l
import kotlinx.coroutines.*
import splitties.systemservices.layoutInflater
import splitties.systemservices.windowManager
import splitties.toast.toast
import java.io.*
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

/**
 * Created by khm on 2021-09-28.
 */

class Camera1Photo : ConstraintLayout, LifecycleObserver{
    private lateinit var binding: Camera1PhotoBinding
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val defaultFileSaveDir = "${Environment.getExternalStorageDirectory().absolutePath}/Camera1/"
    private val CameraSettingDir = "CameraSettingSharedPreference"
    private val CameraMenuHide = "CameraSettingMenuHide"

    private var camera: Camera?=null
    private var parameters: Camera.Parameters?=null
    private var preview: Preview?=null

    private var orientationEventListener: OrientationEventListener?=null
    private val CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK

    private var imageFileSaveDialog: ImageFileSaveDialog?=null

    private var lastRotation = 0

    private var isFirst = true
    private var isFlashStatus = false

    private var fileName = ""
    private var fileDir:String = ""

    private var localBitmap: Bitmap?=null


    private var isFhdQuality = true


    private val zoomNumberTexts = ArrayList<TextView>()

    private fun setZoomLevelText1(){
        for(element in zoomNumberTexts){
            element.setTextColor(Color.WHITE)
        }
        zoomNumberTexts[0].setTextColor(context.getColor(R.color.text_yellow))
    }

    private fun getZoomLevelScale():Int{
        return when(zoomLevel){
            2->{
                10
            }
            3->{
                30
            }
            4->{
                60
            }
            5->{
                88
            }
            else->{ // == 1
                0
            }
        }
    }

    private var zoomLevel:Int by Delegates.observable(1){ _,oldValue,newValue->
        if(oldValue >0)
            zoomNumberTexts[oldValue-1].setTextColor(Color.WHITE)
        if(newValue>0)
            zoomNumberTexts[newValue-1].setTextColor(context.getColor(R.color.text_yellow))

        when(newValue){
            2->{
                parameters?.setZoom(10)
            }
            3->{
                parameters?.setZoom(30)
            }
            4->{
                parameters?.setZoom(60)
            }
            5->{
                parameters?.setZoom(88)
            }
            else->{ // == 1
                parameters?.zoom = 0
            }
        }

        camera!!.parameters = parameters
        camera?.autoFocus { success, _ ->
            if (!success) toast(resources.getString(R.string.fail_focusing))
        }
    }


    private var hideMenu:Boolean by Delegates.observable(false){ _, _, newValue ->
        l.d("hideMenu : $newValue")
        if(newValue)
            hideMenu()
        else
            showMenu()

        context.getSharedPreferences(CameraSettingDir, 0).edit().putBoolean(
            CameraMenuHide, newValue).apply()
    }


    companion object{
        /**
         * @param activity
         * @param cameraId Camera.CameraInfo.CAMERA_FACING_FRONT,
         *                 Camera.CameraInfo.CAMERA_FACING_BACK
         * @param camera   Camera Orientation
         *                 reference by https://developer.android.com/reference/android/hardware/Camera.html
         */
        private fun setCameraDisplayOrientation(activity: Activity, cameraId: Int, camera: Camera):Int{
            val info = Camera.CameraInfo()
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
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360
                result = (360 - result) % 360 // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360
            }

            return result
        }
    }


    // Callback Listeners
    var callbackOnClickBack:(()->Unit)? = null
    var callbackCompleteSavePhoto:((uri:String)->Unit)? = null
    var callbackCanceledSavePhoto:(()->Unit)? = null
    var callbackOnClickVideo:(()->Unit)? =null




    constructor(context: Context):super(context){
        initView()
    }

    constructor(context: Context, attrs: AttributeSet):super(context, attrs){
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defSty: Int):super(context, attrs, defSty){
        initView()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume(){
        if(isFhdQuality){
            binding.tvFHD.setTextColor(context.getColor(R.color.text_yellow))
            binding.tv4K.setTextColor(context.getColor(R.color.colorWhite))
        }else{
            binding.tvFHD.setTextColor(context.getColor(R.color.colorWhite))
            binding.tv4K.setTextColor(context.getColor(R.color.text_yellow))
        }

        // 망원카메라를 사용하기 위해 필요한 리시버 등록
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            context.registerReceiver(mCameraBroadcastReceiver, IntentFilter(INTENT_GET_CAMERA_SETTINGS))
            requestCurrentCameraSettings()
        }

        startCamera()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun onPause(){
        l.d("onPause")
        // 망원카메라를 사용하기 위해 필요한 리시버 해제
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            context.unregisterReceiver(mCameraBroadcastReceiver)
        }

        // 카메라 할당 해제
        if(camera != null){
            if(zoomLevel != 1){
                zoomLevel =1
            }
            camera?.stopPreview()
            preview?.setCamera(null)
            camera?.release()
            camera = null
            isFirst = false
        }

        binding.layout.removeView(preview)
        preview = null
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy(){
        ioScope.cancel()

        // 카메라 할당 해제
        if(camera != null){
            camera?.stopPreview()
            preview?.setCamera(null)
            camera?.release()
            camera = null
            isFirst = false
        }

        binding.layout.removeView(preview)
        preview = null
        orientationEventListener?.disable()
    }



    // Setting Values - 사진을 저장할 내부저장 경로를 설정한다.
    fun setSaveDir(filePath:String?){
        if(filePath.isNullOrEmpty()){
            l.e("setSaveDir is null or empty --> default save dir : $defaultFileSaveDir")
            return
        }
        fileDir = filePath
    }


    // 사진, 비디오 전환 버튼 숨김
    fun hideModeSelectButton(){
        binding.llModeSelect.visibility = View.GONE
    }

    // 사진, 비디오 전환 버튼 표시
    fun showModeSelectButton(){
        binding.llModeSelect.visibility = View.VISIBLE
    }





    private fun initView(){
        //카메라가 사용되는 액티비티의 라이프사이클 수명주기 일치시킴
        if(context is AppCompatActivity){
            (context as AppCompatActivity).lifecycle.addObserver(this)
        }else{
            l.e("This camera1 library only works in activities.")
            return
        }

        binding = Camera1PhotoBinding.inflate(layoutInflater)
        addView(binding.root)

        binding.run{
            hideMenu = context.getSharedPreferences(CameraSettingDir, 0).getBoolean(CameraMenuHide, false)

            zoomNumberTexts.addAll(llZoomLevelNumbers.children.toList().filterIsInstance<TextView>())

//            // for test
            for(i in zoomNumberTexts.indices){
                zoomNumberTexts[i].setOnClickListener {
                    l.d("onClick zoomNumberText : $i")
                    zoomLevel = i+1
                }
            }

            tvBack.setOnClickListener { _->
                callbackOnClickBack?.let{
                    it()
                }
            }

            tvCaptureBtn.setOnClickListener {
                setScreenShot()
            }

            tvFocus.setOnClickListener {
                camera?.autoFocus { success, _ ->
                    if (!success) toast(resources.getString(R.string.fail_focusing))
                }
            }

            tvFlash.setOnClickListener {
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


            tvFHD.setOnClickListener {
                tvFHD.setTextColor(context.getColor(R.color.text_yellow))
                tv4K.setTextColor(context.getColor(R.color.colorWhite))
                isFhdQuality = true
            }

            tv4K.setOnClickListener {
                tvFHD.setTextColor(context.getColor(R.color.colorWhite))
                tv4K.setTextColor(context.getColor(R.color.text_yellow))
                isFhdQuality = false
            }

            tvMenu.setOnClickListener {
                hideMenu = !hideMenu
            }

            tvVideo.setOnClickListener {
                callbackOnClickVideo?.let{ callback->
                    callback()
                }
            }

            //망원
            tvTelephoto.setOnClickListener {
                l.d("onClick telephoto")
                if(isTelephoto){
                    setNormal()
                }else{
                    setTelephoto()
                }
            }

        }

        // 추가 음성 명령
        addCommand(resources.getString(R.string.zoom) + " 1"){
            zoomLevel = 1
        }
        addCommand(resources.getString(R.string.zoom) + " 2"){
            zoomLevel = 2
        }
        addCommand(resources.getString(R.string.zoom) + " 3"){
            zoomLevel = 3
        }
        addCommand(resources.getString(R.string.zoom) + " 4"){
            zoomLevel = 4
        }
        addCommand(resources.getString(R.string.zoom) + " 5"){
            zoomLevel = 5
        }





        // 기기의 화면 방향에 따라 카메라 방향도 돌려준다.
        orientationEventListener = object : OrientationEventListener(
                context,
                SensorManager.SENSOR_DELAY_UI
        ) {
            override fun onOrientationChanged(orientation: Int) {
                val display: Display = windowManager.defaultDisplay
                val rotation = display.rotation
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                    if (rotation != lastRotation) {
                        lastRotation = rotation
                        Log.d("camera1","onOrientationChanged: :::::::::$rotation")
                        resetCam()
                    }
                }
            }
        }

        orientationEventListener?.let{
            if(it.canDetectOrientation()) it.enable()
        }


    }


    // 망원카메라 사용을 위한 세팅
    private fun requestCurrentCameraSettings() {
        val intent = Intent(INTENT_GET_CAMERA_SETTINGS)
        intent.component = ComponentName(ANDROID_SETTINGS_PKG, RW_CAMERA_GET_RECEIVER)
        context.sendBroadcast(intent)
    }

    private val ANDROID_SETTINGS_PKG = "com.android.settings"
    private val INTENT_SET_CAMERA_SETTINGS = "com.android.settings.realwear_camera_SET"
    private val INTENT_GET_CAMERA_SETTINGS = "com.android.settings.realwear_camera_GET"
    private val EXTRA_SENSOR = "sensor"
    private val EXTRA_EIS = "eis"
    private val RW_CAMERA_GET_RECEIVER = "com.android.settings.RealwearCameraGetReceiver"
    private val RW_CAMERA_SET_RECEIVER = "com.android.settings.RealwearCameraSetReceiver"

    private val telephoto = "full"
    private val normal = "binning"
    private var isTelephoto = false
    private var isEisOn = false

    private val mCameraBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent) {
            if (intent.action == INTENT_GET_CAMERA_SETTINGS) {
                val sensor = intent.getStringExtra(EXTRA_SENSOR)

                val eis = intent.getStringExtra(EXTRA_EIS)

                // 센서가 망원일때
                if(sensor == telephoto){
                    isTelephoto = true
                    binding.tvTelephoto.setTextColor(context.getColor(R.color.text_yellow))
                }else{  // 센서가 일반일때
                    isTelephoto = false
                    binding.tvTelephoto.setTextColor(context.getColor(R.color.colorWhite))
                }

                // 흔들림 방지기능 켜져있는지 체크
                isEisOn = eis == "on"

                l.d("sensor:$sensor, eis:$eis")

                startCamera()
            }

        }
    }



    // 망원 카메라 사용
    private fun setTelephoto() {
        zoomLevel = 1
        val intent = Intent(INTENT_SET_CAMERA_SETTINGS)
        intent.component = ComponentName(ANDROID_SETTINGS_PKG, RW_CAMERA_SET_RECEIVER)
        intent.putExtra(EXTRA_SENSOR, telephoto)
        context.sendBroadcast(intent)
        requestCurrentCameraSettings()
    }

    // 일반 카메라 사용
    private fun setNormal() {
        zoomLevel =1
        val intent = Intent(INTENT_SET_CAMERA_SETTINGS)
        intent.component = ComponentName(ANDROID_SETTINGS_PKG, RW_CAMERA_SET_RECEIVER)
        intent.putExtra(EXTRA_SENSOR, normal)
        context.sendBroadcast(intent)
        requestCurrentCameraSettings()
    }




    // 카메라를 시작한다.
    private fun
            startCamera(){
        l.d("startCamera::::::::::: ")
        if(preview == null){
            l.e("preview is null")
            preview =
                Preview(context, binding.surfaceView)
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
            camera?.release()
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
                                context as AppCompatActivity, CAMERA_FACING,
                                camera!!
                        )
                )
                // get Camera parameters
                val params = camera?.getParameters()

                val previewSizeList = params?.supportedPreviewSizes

                val pictureSizeList = params?.supportedPictureSizes

                val sb = StringBuilder()
                for(size in previewSizeList!!){
                    sb.append("[${size.width},${size.height}]")
                }
                l.d("previewSizeList - $sb")

                sb.clear()
                for(size in pictureSizeList!!){
                    sb.append("[${size.width},${size.height}]")
                }
                l.d("pictureSizeList - $sb")

                params.setRotation(
                        setCameraDisplayOrientation(
                                context as AppCompatActivity,
                                CAMERA_FACING,
                                camera!!
                        )
                )

                // hmt1 안드로이드10버전에서는 setPictureSize를 지정하면 카메라 사용 불가하여 Preview사이즈만 설정
                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q){
                    params.setPreviewSize(1920,1080)
                }else{
                    params.setPictureSize(3840, 2160)
                }


                camera?.setParameters(params)
                camera?.startPreview()
                parameters = camera?.getParameters()
                if (isFirst) {
                    parameters?.setZoom(0)
                    camera?.setParameters(parameters)
                } else {
                    parameters?.setZoom(getZoomLevelScale())
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

    //카메라 재시작
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
                toast(resources.getString(R.string.fail_focusing))
            }
        }
    }

    private val shutterCallback = Camera.ShutterCallback {
        l.d("onShutter'd")
    }

    private val rawCallback = Camera.PictureCallback{ data, camera ->
        l.d("onPictureTaken - raw")
    }


    // 사진을 촬영하면 이 메서드로 콜백이 온다.
    private val jpegCallback = Camera.PictureCallback{ data, camera ->
        l.d("onJpegCallback")
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)

        localBitmap = cropBitmap(bitmap)
        localBitmap?.let {
            l.d("localbitmap width : ${it.width}, height : ${it.height}")
            showSaveImageDialog(it)
        }
    }


    private fun showSaveImageDialog(bitmap: Bitmap){
        if(imageFileSaveDialog ==null){
            imageFileSaveDialog = ImageFileSaveDialog(context, bitmap , "확인"){
                when(it){
                    0 -> { // save
                        saveImage()
                    }
                    1 -> { // cancel
                        callbackCanceledSavePhoto?.let { callback->
                            callback()
                        }
                        localBitmap = null
                        resetCam()
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
                        context as AppCompatActivity,
                        CAMERA_FACING, camera!!
                )

        val matrix = Matrix()
        matrix.postRotate(orientation.toFloat())
        val stream = ByteArrayOutputStream()
        if (localBitmap != null) {
            //localBitmap = cropBitmap(localBitmap!!)
            l.d("local bitmap width : ${localBitmap?.width}, height : ${localBitmap?.height}")
            localBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            currentPictureData = stream.toByteArray()
            l.d("current picture data size : ${currentPictureData?.size}")

            onLaunchDictation()
        }
    }


    // 촬영된 사진을 가공한다.
    private fun cropBitmap(bitmap:Bitmap):Bitmap{
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        var width = 3840f
        var height = 2160f

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isEisOn){
            // 손떨림 방지 기능일때 Preview에서 보여지는 만큼 사진사이즈를 똑같이 맞출 경우 80%의 비율로 맞춘다
            val ratio = 0.8f

            width = originalWidth * ratio
            height = originalHeight * ratio
        }

        // 원본사이즈보다 내가 원하는 너비가 클때는 1920으로 맞춤
        if(width > originalWidth){
            l.e("width > originalWidth--> width=1920")
            width = 1920f
        }

        // 원본사이즈보다 내가 원하는 높이가 클때는 1080으로 맞춤
        if(height > originalHeight){
            l.e("height > originalHeight--> height=1080")
            height = 1080f
        }

        val x = (originalWidth - width) * 0.5f
        val y = (originalHeight - height) * 0.5f


        l.d("converted width:${width.toInt()}, height:${height.toInt()}, x:${x.toInt()}, y:${y.toInt()}")
        val cropBitmap = Bitmap.createBitmap(bitmap, x.toInt(), y.toInt(), width.toInt(), height.toInt())

        if(isFhdQuality){
            val resizeBitmap = Bitmap.createScaledBitmap(cropBitmap, 1920, 1080, true)
            l.d("resizeBitmap width:${resizeBitmap.width}, height:${resizeBitmap.height}")
            return resizeBitmap
        }else{
            l.d("cropBitmap width:${cropBitmap.width}, height:${cropBitmap.height}")
            return cropBitmap
        }
    }


    // 사진을 파일로 저장하는 메서드
    private fun onLaunchDictation(){
        // 사진파일명은 현재시간으로 정한다.
        fileName = getNowDate() + ".jpg" //temp

        ioScope.launch {
            var outStream: FileOutputStream?=null

            var uri:String = ""
            // Write to SD Card
            try {
                if(fileDir.isEmpty()){
                    fileDir = defaultFileSaveDir
                }

                l.d("file dir : $fileDir")

                val dir = File(fileDir)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                if(fileName.trim().isEmpty()){
                    fileName = String.format("%d", System.currentTimeMillis())
                }
                val outFile = File(dir, fileName)
                uri = outFile.path
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
                // 카메라 초기화
                withContext(Dispatchers.Main){
                    callbackCompleteSavePhoto?.let{
                        it(uri)
                    }
                    resetCam()
                }
            }
        }
    }


    // 새로 생성된 파일을 기기에서 인식하게 해주는 메서드
    private fun refreshGallery(file: File){
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = Uri.fromFile(file)
        context.sendBroadcast(mediaScanIntent)
    }



    @SuppressLint("SimpleDateFormat")
    private fun getNowDate():String{
        val now = System.currentTimeMillis()
        val date = Date(now)
        val sdf = SimpleDateFormat("yyyyMMddHHmmssSSS")

        return sdf.format(date)
    }



    private fun addCommand(commandText: String, onClickCallback: () -> Unit){
        val textView = TextView(context)
        textView.text = commandText
        textView.contentDescription = "hf_no_number"
        val lp = LinearLayout.LayoutParams(1, 1)
        textView.layoutParams = lp
        textView.setOnClickListener {
            onClickCallback()
        }

        binding.llCommand.addView(textView)
    }


    private fun showMenu() {
        binding.run {
            llGoBack.visibility = View.VISIBLE
            llZoom.visibility = View.VISIBLE
            tvFocus.visibility = View.VISIBLE
            llFlash.visibility = View.VISIBLE
            llQuality.visibility = View.VISIBLE
            tvTelephoto.visibility = View.VISIBLE
            tvMenu.text = resources.getString(R.string.hide_menu)
        }
    }

    private fun hideMenu() {
        binding.run {
            llGoBack.visibility = View.GONE
            llZoom.visibility = View.GONE
            tvFocus.visibility = View.GONE
            llFlash.visibility = View.GONE
            llQuality.visibility = View.GONE
            tvTelephoto.visibility = View.GONE
            tvMenu.text = resources.getString(R.string.show_menu)
        }
    }




}