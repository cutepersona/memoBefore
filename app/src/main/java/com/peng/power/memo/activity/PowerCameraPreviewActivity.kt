package com.peng.power.memo.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.graphics.*
//import android.hardware.Camera
//import android.hardware.Camera.CameraInfo
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.internal.Camera2UseCaseConfigFactory
import androidx.core.view.children
import androidx.lifecycle.LifecycleObserver
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
import com.peng.power.memo.sftp.SftpConnect
import com.peng.power.memo.sftp.SftpService
import com.peng.power.memo.sftp.SftpUploadManager
import com.peng.power.memo.util.l
import kotlinx.coroutines.*
import org.jetbrains.annotations.NotNull
import splitties.activities.start
import splitties.toast.toast
import java.io.*
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates


class PowerCameraPreviewActivity :BaseActivity<ActivityPowerCameraPreviewBinding>() ,
    LifecycleObserver {

    companion object{
        private val ORIENTATIONS = SparseIntArray()
        init{
            ORIENTATIONS.append(Surface.ROTATION_0, 90);
            ORIENTATIONS.append(Surface.ROTATION_90, 0);
            ORIENTATIONS.append(Surface.ROTATION_180, 270);
            ORIENTATIONS.append(Surface.ROTATION_270, 180);
        }
    }

    private var connectionStateMonitor = ConnectionStateMonitor()
    private var bindService: SftpService?=null

    private var camera:Camera?=null
    //    private var parameters:Camera.Parameters?=null
    private var preview:Preview?=null

    private var orientationEventListener:OrientationEventListener?=null
//    private val CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK

    private var imageFileSaveDialog: ImageFileSaveDialog?=null

    private var lastRotation = 0

    private var isFirst = true
    private var isFlashStatus = false

    private var fileName = ""
    private var fileDir:String = ""

    private var localBitmap:Bitmap?=null

    private lateinit var currentMemoTableData: MemoTableData

    private val sftpConnect = SftpConnect(this)

//    private var memoSeq = 0
//    private var enSeq = -1
//    private var userID:String? = null
//    private var userName:String? = null
//    private var memoContents: String? = null
//    private var saveTime: String? = null

    private var dataUser: DataUser?=null

    var display: DisplayMetrics? = null
    var fontSize: Float = 21f

    //-----------------------------------------camera2-----------------------------------------------
    private val defaultScope = CoroutineScope(Dispatchers.Default)
    private var jobCheckCameraIdNotNull:Job?=null
    private var jobReOpenCamera: Job?=null

    private var mLastRotation = 0

    private var cameraDevice: CameraDevice?=null
    private var imageReader: ImageReader?=null
    private var previewRequestBuilder: CaptureRequest.Builder?=null
    private var image: Image?=null
    private var captureSession: CameraCaptureSession?=null
    private var cameraCharacteristics: CameraCharacteristics?=null

    private var facing = CameraCharacteristics.LENS_FACING_BACK
    private var aspectRatio = 0.75f
    private val aspectRatioThreshold = 0.05f
    private var sensorOrientation = 0
    private var previewSize: Size?=null
    private var cameraId: String? = null

    //Lock
    private val STATE_PREVIEW = 0
    private val STATE_WAITING_LOCK = 1
    private val STATE_WAITING_PRECAPTURE = 2
    private val STATE_WAITING_NON_PRECAPTURE = 3
    private val STATE_PICTURE_TAKEN = 4
    private val cameraOpenCloseLock = Semaphore(1)
    private var state = STATE_PREVIEW

    // thread
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    //Zooming
    private var maximumZoomLevel = 0f
    private var zoom: Rect? = null

    private var beforeZoom: Rect? = null

    private val defaultFileSaveDir = "${Environment.getExternalStorageDirectory().absolutePath}/Watt/"
    private var isCreated: Boolean = false;
    private val zoomNumberTexts = ArrayList<TextView>()

    private var zoomLevel:Int by Delegates.observable(1){ _, oldValue, newValue->
        Log.d("Noah ==== ", "zoomLevel ==== ${zoomLevel}")

        if(oldValue >0)
            zoomNumberTexts[oldValue - 1].setTextColor(Color.WHITE)
        if(newValue>0)
            zoomNumberTexts[newValue - 1].setTextColor(applicationContext.getColor(R.color.text_yellow))

        when(newValue){
            2 -> {
                // 카메라1기준 zoom == 1.5f
                setZoom(1.25f)
            }
            3 -> {
                // 카메라1기준 zoom == 2.2f
                setZoom(1.84f)
            }
            4 -> {
                // 카메라1기준 zoom == 2.9f
                setZoom(2.42f)
            }
            5 -> {
                // 카메라1기준 zoom == 3.3f
                setZoom(2.75f)
            }
            else->{ // == 1
                Log.d("Noah ==== ", " zoom level call !!!! ${newValue}")
                // 카메라1기준 zoom == 1.2f
                setZoom(1.0f)
            }
        }
    }


    // 망원
    private val ANDROID_SETTINGS_PKG = "com.android.settings"
    private val INTENT_SET_CAMERA_SETTINGS = "com.android.settings.realwear_camera_SET"
    private val INTENT_GET_CAMERA_SETTINGS = "com.android.settings.realwear_camera_GET"
    private val EXTRA_SENSOR = "sensor"
    private val EXTRA_EIS = "eis"
    private val EXTRA_FOV = "fov"
    private val RW_CAMERA_GET_RECEIVER = "com.android.settings.RealwearCameraGetReceiver"
    private val RW_CAMERA_SET_RECEIVER = "com.android.settings.RealwearCameraSetReceiver"

    private val telephoto = "full"
    private var isTelephoto = true
    private val normal = "binning"
    private val fov_wide = "off"
    private var isEisOn = false

    private val mCameraBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("Noah ", " onReceive  Context : $context Intent $intent")
            if (intent.action == INTENT_GET_CAMERA_SETTINGS) {
                val sensor = intent.getStringExtra(EXTRA_SENSOR)

                val eis = intent.getStringExtra(EXTRA_EIS)

                val fov = intent.getStringExtra(EXTRA_FOV)

                if(sensor == telephoto){
                    isTelephoto = true
                    binding.tvTelephotoStatus.setTextColor(context.getColor(com.watt.camera1n2.R.color.text_yellow))
                    binding.tvTelephotoStatus.setText(R.string.telephoto_on)
                }
                else{
                    isTelephoto = false
                    binding.tvTelephotoStatus.setTextColor(context.getColor(com.watt.camera1n2.R.color.colorWhite))
                    binding.tvTelephotoStatus.setText(R.string.telephoto_off)
                }

                isEisOn = eis == "on"


//                resetCam()
                zoom = null
                closeCamera()
                stopBackgroundThread()
                startBackgroundThread()
                if (binding.textureView.isAvailable) {
                    openCamera(binding.textureView.width, binding.textureView.height)
                } else {
                    Log.d("Noah","============================ surfaceTextureListener ==============================")
                    binding.textureView.surfaceTextureListener = surfaceTextureListener
                }

            }
        }
    }

    var memoNum:Int? = null


    private fun initView(){
        //카메라가 사용되는 액티비티의 라이프사이클 수명주기 일치시킴
        lifecycle.addObserver(this)

        binding = ActivityPowerCameraPreviewBinding.inflate(layoutInflater)
//        addView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        orientationEventListener = object : OrientationEventListener(
            this,
            SensorManager.SENSOR_DELAY_NORMAL
        ) {
            override fun onOrientationChanged(orientation: Int) {
                val display = windowManager.defaultDisplay
                val rotation = display.rotation
                if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && rotation != mLastRotation) {
                    Log.d("Noah ==== Camera2Photo", "onOrientationChanged:::::")
                    closeCamera()
                    stopBackgroundThread()
                    startBackgroundThread()
                    if (binding.textureView.isAvailable) {
                        Log.d("Noah","onOrientationChanged ==== openCamera ====")
                        openCamera(binding.textureView.width, binding.textureView.height)
                    } else {
                        binding.textureView.surfaceTextureListener = surfaceTextureListener
                    }
                    mLastRotation = rotation
                }
            }
        }
        orientationEventListener?.let{
            Log.d("Noah ", " ==== orientationEventListener")
            if(it.canDetectOrientation()) it.enable()
        }



        binding.run {

            // 2022-06-27 최상은 수정 -- 수동 초점 버튼 주석
//            tvFocus.setOnClickListener {
//                l.d("onClick tvFocus")
//                manualFocus()---- initSocketIoClient ----
//            }

            tvFlash.setOnClickListener {
                if(!isFlashStatus){
                    isFlashStatus = true
                    previewRequestBuilder?.set(
                        CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH
                    )
                    captureSession?.setRepeatingRequest(previewRequestBuilder!!.build(), null, null)
                    tvFlashStatus.text = resources.getString(R.string.flash_on)
                }else{
                    isFlashStatus = false
                    previewRequestBuilder?.set(
                        CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF
                    )
                    captureSession?.setRepeatingRequest(previewRequestBuilder!!.build(), null, null)
                    tvFlashStatus.text = resources.getString(R.string.flash_off)
                }
            }

            zoomNumberTexts.addAll(
                llZoomLevelNumbers.children.toList().filterIsInstance<TextView>()
            )

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


        }
    }


    private fun addCommand(commandText: String, onClickCallback: () -> Unit){
        val textView = TextView(applicationContext)
        textView.text = commandText
        textView.contentDescription = "hf_no_number"
        val lp = LinearLayout.LayoutParams(1, 1)
        textView.layoutParams = lp
        textView.setOnClickListener {
            onClickCallback()
        }

        binding.llCommand.addView(textView)
    }


    private fun setZoom(magnification: Float){
        try{
//            Log.d("Noah ==== ", " setZoom ==== magnification ==== ${magnification} ==== maximumZoomLevel ==== ${maximumZoomLevel}")

            if(magnification >maximumZoomLevel)
                return
            val rect = cameraCharacteristics!!.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                ?: return

            val ratio = 1.toFloat() / magnification
            l.d("ratio : $ratio")
            val croppedWidth = rect.width() - Math.round(rect.width().toFloat() * ratio)
            val croppedHeight = rect.height() - Math.round(rect.height().toFloat() * ratio)
            zoom = Rect(
                croppedWidth / 2, croppedHeight / 2,
                rect.width() - croppedWidth / 2, rect.height() - croppedHeight / 2
            )
            l.d("zoom left:${zoom?.left}, top:${zoom?.top}, right:${zoom?.right}, bottom:${zoom?.bottom}")
            previewRequestBuilder!!.set(CaptureRequest.SCALER_CROP_REGION, zoom)
            captureSession!!.setRepeatingRequest(
                previewRequestBuilder!!.build(),
                captureCallback,
                backgroundHandler
            )
        }catch (e: Exception){
            l.e(e.toString())
        }
    }


    private val readerListener = ImageReader.OnImageAvailableListener {
        try {
            Log.d("Noah", "==== readerListener ====")
            image = it.acquireLatestImage()
            //image = it.acquireNextImage()
            val buffer: ByteBuffer = image!!.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            localBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//            l.d("Noah temp bitmap width:${localBitmap?.width}, height:${localBitmap?.height}")
            //val bitmap = cropBitmap(mCropRegion, tempBitmap)

            localBitmap?.let{ bitmap->
                showSaveImageDialog(bitmap)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            image?.close()
        }
    }

    // openCamera() 메서드에서 CameraManager.openCamera() 를 실행할때 인자로 넘겨주어야하는 콜백메서드
    // 카메라가 제대로 열렸으면, cameraDevice 에 값을 할당해주고, 카메라 미리보기를 생성한다
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d("Noah ","==== stateCallback : onOpened")
//            Log.d("Noah ","==== stateCallback : camera ==== ${camera}")
//            l.d("stateCallback : onOpened")
            cameraOpenCloseLock.release()
            // MainActivity 의 cameraDevice 에 값을 할당해주고, 카메라 미리보기를 시작한다
            // 나중에 cameraDevice 리소스를 해지할때 해당 cameraDevice 객체의 참조가 필요하므로,
            // 인자로 들어온 camera 값을 전역변수 cameraDevice 에 넣어 준다
            cameraDevice = camera

            Log.d("Noah ==== ","createCameraPreviewSession() ====")
            // createCameraPreview() 메서드로 카메라 미리보기를 생성해준다
            createCameraPreviewSession()

            zoomLevel = zoomLevel
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d("Noah ","==== stateCallback : onDisconnected")
//            l.d("stateCallback : onDisconnected")
            cameraOpenCloseLock.release()
            // 연결이 해제되면 cameraDevice 를 닫아준다
            cameraDevice!!.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d("Noah ","==== stateCallback : onError")
//            l.d("stateCallback : onError")
            cameraOpenCloseLock.release()
            // 에러가 뜨면, cameraDevice 를 닫고, 전역변수 cameraDevice 에 null 값을 할당해 준다
            cameraDevice!!.close()
            cameraDevice = null
        }

    }

    // createCameraPreviewSession() 메서드를 호출해서 카메라 미리보기를 만들어준다
    private fun createCameraPreviewSession() {
        try {
            Log.d("Noah","==== createCameraPreviewSession ====")
            // 캡쳐세션을 만들기 전에 프리뷰를 위한 Surface 를 준비한다
            // 레이아웃에 선언된 textureView 로부터 surfaceTexture 를 얻을 수 있다
            val texture = binding.textureView.surfaceTexture

            // 미리보기를 위한 Surface 기본 버퍼의 크기는 카메라 미리보기크기로 구성
//            com.watt.camera1n2.l.d("Noah image dimension width:${previewSize?.width}, height:${previewSize?.height}")
            texture?.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)

            // 미리보기 해상도 설정
//            texture?.setDefaultBufferSize(1920, 1080)

            // 미리보기를 시작하기 위해 필요한 출력표면인 surface
            val surface = Surface(texture)

            // 미리보기 화면을 요청하는 RequestBuilder 를 만들어준다.
            // 이 요청은 위에서 만든 surface 를 타겟으로 한다
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

//            previewRequestBuilder!!.set(CaptureRequest.SCALER_CROP_REGION, zoom)
            //captureRequestBuilder?.set(CaptureRequest.JPEG_ORIENTATION, cameraCharacteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION))
            previewRequestBuilder?.addTarget(surface)


            // 위에서 만든 surface 에 미리보기를 보여주기 위해 createCaptureSession() 메서드를 시작한다
            // createCaptureSession 의 콜백메서드를 통해 onConfigured 상태가 확인되면
            // CameraCaptureSession 을 통해 미리보기를 보여주기 시작한다
            cameraDevice!!.createCaptureSession(
                listOf(surface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        l.d("Configuration change")
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) {
                            // 카메라가 이미 닫혀있는경우, 열려있지 않은 경우
                            return
                        }
                        // session 이 준비가 완료되면, 미리보기를 화면에 뿌려주기 시작한다
                        captureSession = session

                        previewRequestBuilder?.set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        )

//                            captureRequestBuilder?.set(
//                                    CaptureRequest.CONTROL_AF_MODE,
//                                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
//                            )
                        try {
                            captureSession?.setRepeatingRequest(
                                previewRequestBuilder!!.build(),
                                null,
                                null
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                },
                null
            )


        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getOrientation(rotation: Int): Int {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS[rotation] + sensorOrientation + 270) % 360
    }

    private fun cropBitmap(rect: Rect, bitmap: Bitmap):Bitmap{
        l.d("top:${rect.top}, bottom:${rect.bottom}, left:${rect.left}, right:${rect.right}")
        l.d("bitmap width:${bitmap.width}, bitmap height:${bitmap.height}, rect width:${rect.width()}, height:${rect.height()}")
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    }


    // 카메라 객체를 시스템에 반환하는 메서드
    // 카메라는 싱글톤 객체이므로 사용이 끝나면 무조건 시스템에 반환해줘야한다
    // 그래야 다른 앱이 카메라를 사용할 수 있다
    private fun closeCamera() {
        l.d("close camera")
        try {
            cameraOpenCloseLock.acquire()
            if (null != captureSession) {
                captureSession!!.close()
                captureSession = null
            }
            if (null != cameraDevice) {
                cameraDevice!!.close()
                cameraDevice = null
            }
            if (null != imageReader) {
                imageReader!!.close()
                imageReader = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }


//    companion object{
//        /**
//         * @param activity
//         * @param cameraId Camera.CameraInfo.CAMERA_FACING_FRONT,
//         *                 Camera.CameraInfo.CAMERA_FACING_BACK
//         * @param camera   Camera Orientation
//         *                 reference by https://developer.android.com/reference/android/hardware/Camera.html
//         */
//        private fun setCameraDisplayOrientation(activity: Activity, cameraId: Int, camera: Camera):Int{
//            val info = CameraInfo()
//            Camera.getCameraInfo(cameraId, info)
//            val rotation = activity.windowManager.defaultDisplay
//                    .rotation
//            var degrees = 0
//            when (rotation) {
//                Surface.ROTATION_0 -> degrees = 0
//                Surface.ROTATION_90 -> degrees = 90
//                Surface.ROTATION_180 -> degrees = 180
//                Surface.ROTATION_270 -> degrees = 270
//            }
//
//            var result: Int
//            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
//                result = (info.orientation + degrees) % 360
//                result = (360 - result) % 360 // compensate the mirror
//            } else {  // back-facing
//                result = (info.orientation - degrees + 360) % 360
//            }
//
//            return result
//        }
//    }

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

    override fun initViewBinding() {
        initView()

//        binding = ActivityPowerCameraPreviewBinding.inflate(layoutInflater)
    }

    override fun initAfterBinding() {
        connectionStateMonitor.enable(this)
        powerStoreManager.register()

        // 메모 상단에 표기하기 위한 번호
        memoNum = intent.getIntExtra("memoNum", -1)

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
//        command(resources.getString(R.string.zoom) + " 1")
//        command(resources.getString(R.string.zoom) + " 2")
//        command(resources.getString(R.string.zoom) + " 3")
//        command(resources.getString(R.string.zoom) + " 4")
//        command(resources.getString(R.string.zoom) + " 5")
//        command(resources.getString(R.string.manual_focus))
//        command(resources.getString(R.string.flash))
//        command(resources.getString(R.string.telephoto))
        //command(resources.getString(R.string.capture))
        //command(resources.getString(R.string.video))

        binding.tvBack.setOnClickListener {
            start<InputMemoActivity> {
                putExtra(this, currentMemoTableData)
            }
            finish()
        }

        binding.tvCaptureBtn.setOnClickListener {
//            setScreenShot()
            lockFocus()
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

//        orientationEventListener = object : OrientationEventListener(
//            this,
//            SensorManager.SENSOR_DELAY_NORMAL
//        ) {
//            override fun onOrientationChanged(orientation: Int) {
//                val display = windowManager.defaultDisplay
//                val rotation = display.rotation
//                if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && rotation != mLastRotation) {
////                    Log.d("Camera2Photo", "onOrientationChanged:::::")
//                    closeCamera()
//                    stopBackgroundThread()
//                    startBackgroundThread()
//                    if (binding.textureView.isAvailable) {
//                        openCamera(binding.textureView.width, binding.textureView.height)
//                    } else {
//                        binding.textureView.surfaceTextureListener = surfaceTextureListener
//                    }
//                    mLastRotation = rotation
//                }
//            }
//        }

//        orientationEventListener?.let{
//            if(it.canDetectOrientation()) it.enable()
//        }

        binding.tvMenu.setOnClickListener {
            dataUser?.menuStatus?.let{
                if(it) hideMenu()
                else showMenu()
            }
        }

        binding.tvTelephoto.setOnClickListener {
            Log.d("Noah", "망원 클릭");
            setTelephotoOn()
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

//            binding.tvFocus.setTextSize(fontSize)
            binding.tvFlash.setTextSize(fontSize)
            binding.tvFlashStatus.setTextSize(fontSize)
            binding.tvMenu.setTextSize(fontSize)
            binding.tvPhoto.setTextSize(fontSize)
            binding.tvVideo.setTextSize(fontSize)
            binding.tvCaptureBtn.setTextSize(fontSize)
            binding.tvTelephoto.setTextSize(fontSize)
        }

        setEmergencyCall()

//         offTelePhoto()
    }

    fun setTelephotoOn() {
        Log.d("Noah ", " setTelephotoOn ==== ")
        //        cameraManager.setTelephotoOn();
//        if ("망원".equals(tv_telephoto.getText)){
//            barcodeScannerView.setTelephotoOn();
//        cameraManager.setReceiveTelephoto(cameraInstance.getSface());
        Log.d("Noah ", "Telephoto Status $telephoto")
        if (isTelephoto) {
            setNormal()
        } else {
            setTelephoto()
        }

//        }
    }


    private fun setTelephoto() {
//        zoomLevel = 1
        com.watt.camera1n2.l.d("Noah setTelephoto " + zoomLevel)
        dataUser!!.zoom = zoomLevel
        val intent = Intent(INTENT_SET_CAMERA_SETTINGS)
        intent.component = ComponentName(ANDROID_SETTINGS_PKG, RW_CAMERA_SET_RECEIVER)
        intent.putExtra(EXTRA_SENSOR, "null")
        sendBroadcast(intent)
        requestCurrentCameraSettings()
    }

    private fun setNormal() {
//        zoomLevel =1
        com.watt.camera1n2.l.d("Noah setNormal")
        val intent = Intent(INTENT_SET_CAMERA_SETTINGS)
        intent.component = ComponentName(ANDROID_SETTINGS_PKG, RW_CAMERA_SET_RECEIVER)
        intent.putExtra(EXTRA_SENSOR, "binning")
        intent.putExtra(EXTRA_EIS, "off") // off: full , on : on
        intent.putExtra(EXTRA_FOV, "off") // WIDE : off , NARROW : on
        sendBroadcast(intent)
        requestCurrentCameraSettings()

    }

    private fun requestCurrentCameraSettings() {
        val intent = Intent(INTENT_GET_CAMERA_SETTINGS)
        intent.component = ComponentName(ANDROID_SETTINGS_PKG, RW_CAMERA_GET_RECEIVER)
        sendBroadcast(intent)
    }


//    fun offTelePhoto(){
//        Log.d("Noah ==== ", " setNormal ---- 망원 취소");
//        telephoto = false;
//        binding.tvTelephotoStatus.setTextColor(getColor(R.color.text_yellow))
//        binding.tvTelephotoStatus.setText("꺼짐")
//
//        val intent = Intent(INTENT_SET_CAMERA_SETTINGS)
//        intent.component = ComponentName(ANDROID_SETTINGS_PKG, RW_CAMERA_SET_RECEIVER)
//        intent.putExtra(EXTRA_SENSOR, "binning")
//        intent.putExtra(EXTRA_EIS, "on") // off: full , on : on
//        intent.putExtra(EXTRA_FOV, "off") // WIDE : off , NARROW : on
//        applicationContext.sendBroadcast(intent)
//        requestCurrentCameraSettings()
//    }
//
//    fun onTelePhoto(){
//        Log.d("Noah ==== ", " CameraManager setTelephoto ----- 망원 요청");
//        telephoto = true;
//        binding.tvTelephotoStatus.setTextColor(getColor(R.color.white))
//        binding.tvTelephotoStatus.setText("켜짐")
//
//        val intent = Intent(INTENT_SET_CAMERA_SETTINGS)
//        intent.component = ComponentName(ANDROID_SETTINGS_PKG, RW_CAMERA_SET_RECEIVER)
//        intent.putExtra(EXTRA_SENSOR, "full")
//        applicationContext.sendBroadcast(intent)
//        requestCurrentCameraSettings()
//    }

    override fun onResume() {
        super.onResume()
//        Log.d("Noah","==== onResume() ==== ${isFirst}")
        zoom = null
//        dataUser!!.zoom = 1
        startBackgroundThread()
        Log.d("Noah ==== onResume ==== ","cameraDevice ==== ${cameraDevice}")
        if(binding.textureView.isAvailable){
            if(cameraDevice == null){
                Log.d("Noah","onResume() ==== openCamera ====")
                openCamera(binding.textureView.width, binding.textureView.height)
            }

        }else{
            binding.textureView.surfaceTextureListener = surfaceTextureListener
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            registerReceiver(mCameraBroadcastReceiver, IntentFilter(INTENT_GET_CAMERA_SETTINGS))
//            requestCurrentCameraSettings()

        }

        setNormal()
    }

    private var surfaceTextureListener: TextureView.SurfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, w: Int, h: Int) {
//            l.d("onSurfaceTextureSizeChanged")
            Log.d("Noah"," ==== onSurfaceTextureSizeChanged")
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
            //l.d("onSurfaceTextureUpdated")
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
            // 지정된 SurfaceTexture 를 파괴하고자 할 때 호출된다
            // true 를 반환하면 메서드를 호출한 후 SurfaceTexture 에서 랜더링이 발생하지 않는다
            // 대부분의 응용프로그램은 true 를 반환한다
            // false 를 반환하면 SurfaceTexture#release() 를 호출해야 한다
//            l.d("onSurfaceTextureDestroyed")
            Log.d("Noah"," ==== onSurfaceTextureDestroyed")
            return false
        }

        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, width: Int, height: Int) {
            // TextureListener 에서 SurfaceTexture 가 사용가능한 경우, openCamera() 메서드를 호출한다
//            Log.d("Noah"," ==== onSurfaceTextureAvailable, open camera")
            Log.d("Noah","surfaceTextureListener() ==== openCamera ====")
            openCamera(width, height)
        }

    }

    private fun chooseOptimalSize(
        choices: Array<Size>, textureViewWidth: Int,
        textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size
    ): Size {

        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = java.util.ArrayList()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough: MutableList<Size> = java.util.ArrayList()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight && option.height == option.width * h / w) {
                if (option.width >= textureViewWidth &&
                    option.height >= textureViewHeight) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        return if (bigEnough.size > 0) {
            Collections.min(bigEnough, CompareSizesByArea())
        } else if (notBigEnough.size > 0) {
            Collections.max(notBigEnough, CompareSizesByArea())
        } else {
            l.e("Couldn't find any suitable preview size")
            choices[0]
        }
    }

    private val captureCallback: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        private fun process(result: CaptureResult) {
            when (state) {
                STATE_PREVIEW -> {
                }
                STATE_WAITING_LOCK -> {
                    //l.d("STATE_WAITING_LOCK")
                    val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                    //Log.i("cameraFocus", "" + afState)
//                    Log.d("Noah","STATE_WAITING_LOCK - " + afState);
                    if (afState == null) {
                        Log.d("Noah","captureStillPicture() ==== afState NULL !!!!!!");
                        captureStillPicture()
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_INACTIVE == afState /*add this*/ || CaptureRequest.CONTROL_AF_STATE_PASSIVE_SCAN == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
//                        Log.d("Noah","STATE_WAITING_LOCK CONTROL_AE_STATE - " + afState);
                        if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED
                        ) {
                            state = STATE_PICTURE_TAKEN
                            Log.d("Noah","captureStillPicture() ==== ${state}");
                            captureStillPicture()
                        } else {
                            Log.d("Noah","runPrecaptureSequence() ====");
                            runPrecaptureSequence()
                        }
                    }
                }
                STATE_WAITING_PRECAPTURE -> {
                    //l.d("STATE_WAITING_PRECAPTURE")
                    Log.d("Noah","Noah STATE_WAITING_PRECAPTURE");
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        state = STATE_WAITING_NON_PRECAPTURE
                    }
                }
                STATE_WAITING_NON_PRECAPTURE -> {
                    //l.d("STATE_WAITING_NON_PRECAPTURE")
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    Log.d("Noah ==== ", "STATE_WAITING_NON_PRECAPTURE - " + aeState);
//                  if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    state = STATE_PICTURE_TAKEN
                    captureStillPicture()
//                    }
                }
            }
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)
        }
    }


    private fun captureStillPicture() {
        try {
            Log.d("Noah ","cameraDevice ==== ${cameraDevice}")
            if (null == cameraDevice) {
                return
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader!!.surface)
//            Log.d("Noah ","captureBuilder ==== ${captureBuilder}")
            // Use the same AE and AF modes as the preview.
//            captureBuilder.set(
//                CaptureRequest.CONTROL_AF_MODE,
//                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
//            )
//            Log.d("Noah ","captureStillPicture ==== ${isFlashStatus}")
            if (isFlashStatus)
            {
                captureBuilder!!.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                captureBuilder!!.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            }
            else
            {
                captureBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
//                captureBuilder!!.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            }

            // Orientation
            val rotation = windowManager.defaultDisplay.rotation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation))

//            Log.d("Noah ","zoom ==== ${zoom}")
            //Zoom
            if (zoom != null) {
//                Log.d("Noah ","captureStillPicture ==== zoom != null")
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom)
            }
            val captureCallback: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Log.d("Noah ","captureCallback ==== unlockFocus()")
                    unlockFocus()
                }
            }
            captureSession!!.stopRepeating()
            captureSession!!.abortCaptures()
            captureSession!!.capture(captureBuilder.build(), captureCallback, null)
        } catch (e: CameraAccessException) {
            com.watt.camera1n2.l.e(e.toString())
        }
    }

    private fun lockFocus() {
        try {
//            com.watt.camera1n2.l.d("Noah lockFocus!!!!!!!!!!!!")
//            Log.d("Noah ","==== readerListener ${readerListener}")
//            Log.d("Noah ","==== imageReader ${imageReader}")
            // This is how to tell the camera to lock focus.
            previewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )

            // 사진 촬영 할때마다 readerListener를 imageReader에 등록하도록 함
            imageReader!!.setOnImageAvailableListener(readerListener, null)

            // Tell #captureCallback to wait for the lock.
            state = STATE_WAITING_LOCK
            captureSession!!.capture(
                previewRequestBuilder!!.build(), captureCallback,
                backgroundHandler
            )

        } catch (e: IllegalStateException)
        {
            com.watt.camera1n2.l.e(e.toString())
        }
        catch (e: CameraAccessException) {
            com.watt.camera1n2.l.e(e.toString())
        }
    }

    private fun unlockFocus() {
        try {
            com.watt.camera1n2.l.d("Noah un----------lockFocus!!!!!!!!!!!!")
            // Reset the auto-focus trigger
            previewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            )
            captureSession!!.capture(
                previewRequestBuilder!!.build(), captureCallback,
                backgroundHandler
            )
            // After this, the camera will go back to the normal state of preview.
            state = STATE_PREVIEW
            //resume Zoom effect after taking a picture
            previewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
//            if (zoom != null) previewRequestBuilder!!.set(CaptureRequest.SCALER_CROP_REGION, zoom)
            captureSession!!.setRepeatingRequest(
                previewRequestBuilder!!.build(), captureCallback,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            com.watt.camera1n2.l.e(e.toString())
        }
    }

    private fun runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            // Tell #captureCallback to wait for the precapture sequence to be set.
            state = STATE_WAITING_PRECAPTURE
            captureSession!!.capture(
                previewRequestBuilder!!.build(), captureCallback,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            com.watt.camera1n2.l.e(e.toString())
        }
    }


    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera2 Background")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    @SuppressLint("NewApi")
    private fun stopBackgroundThread() {
        if (backgroundThread == null) {
            backgroundHandler = null
            return
        }
        backgroundThread!!.quitSafely()
        try {
            backgroundThread!!.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            com.watt.camera1n2.l.e(e.toString())
        }
    }

    @SuppressLint("MissingPermission")
    // openCamera() 메서드는 TextureListener 에서 SurfaceTexture 가 사용 가능하다고 판단했을 시 실행된다
    private fun openCamera(width: Int, height: Int) {
        Log.e("Noah","openCamera() : openCamera()메서드가 호출되었음")

        setUpCameraOutputs(width, height)


        // 카메라의 정보를 가져와서 cameraId 와 imageDimension 에 값을 할당하고, 카메라를 열어야 하기 때문에
        // CameraManager 객체를 가져온다
        if(cameraId.isNullOrEmpty()){
            return
        }
        jobCheckCameraIdNotNull?.cancel()

        val manager = applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        l.e("camera id : ${cameraId}")

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraId!!, stateCallback, backgroundHandler)
        } catch (e: Exception) {
            l.e(e.toString())
        }
    }


    internal class CompareSizesByArea : Comparator<Size?> {
        override fun compare(lhs: Size?, rhs: Size?): Int {
            return java.lang.Long.signum(
                lhs!!.width.toLong() * lhs.height -
                        rhs!!.width.toLong() * rhs.height
            )
        }
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        Log.d("Noah","==== setUpCameraOutputs ====")
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var cameraId: String? = null
        try {
            for (each in manager.cameraIdList) {
                Log.e("Noah","cameraId is each:$each")
                if (facing == manager.getCameraCharacteristics(each).get(CameraCharacteristics.LENS_FACING)) {
                    cameraId = each
//                    break
                }
            }
            Log.e("Noah","cameraId is $cameraId")
            if (cameraId == null){
                jobCheckCameraIdNotNull = defaultScope.launch {
                    delay(500)
                    setUpCameraOutputs(width, height)
                }
                return
            }
            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            cameraCharacteristics = characteristics
            maximumZoomLevel = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)!!
            val map = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )
                ?: throw Exception("configuration map is null.")

            // For still image captures, we use the largest available size.
            val largest: Size
            val sizes = java.util.ArrayList<Size>()
            for (each in map.getOutputSizes(ImageFormat.JPEG)) {
                val thisAspect = each.height.toFloat() / each.width
//                l.d("Noah outputsize: w:${each.width} h:${each.height}")
//                if (Math.abs(thisAspect - aspectRatio) < aspectRatioThreshold) {
//                    l.d("Noah outputsize------------------------------- in : w:${each.width} h:${each.height}")
//                    sizes.add(each)
//                }
                if(each.width == 1920 && each.height == 1080)
//                    if(each.width == 854 && each.height == 480)
                {
//                    l.d("Noah outputsize-----------------------------------------------------: w:${each.width} h:${each.height}")
                    sizes.add(each)
                }
            }
            if (sizes.size == 0) return
            largest = Collections.max(sizes, CompareSizesByArea())
//            imageReader = ImageReader.newInstance(largest.width, largest.height,
//                    ImageFormat.JPEG,  /*maxImages*/3)

//            Log.d("Noah ==== ","imageReader ============================================ ${imageReader}")

            // 사진 해상도 설정
            imageReader = ImageReader.newInstance(
                3840, 2160,
                ImageFormat.JPEG,  /*maxImages*/3
            )




            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            val displayRotation = windowManager.defaultDisplay.rotation
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            var swappedDimensions = false
            when (displayRotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
                Surface.ROTATION_90, Surface.ROTATION_270 -> if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
                else -> com.watt.camera1n2.l.e("Display rotation is invalid: $displayRotation")
            }
            val displaySize = Point()
            windowManager.defaultDisplay.getSize(displaySize)

            displaySize.x = 1920
            displaySize.y = 1080

            var rotatedPreviewWidth = width
            var rotatedPreviewHeight = height
            var maxPreviewWidth = displaySize.x
            var maxPreviewHeight = displaySize.y
            if (swappedDimensions) {
                rotatedPreviewWidth = height
                rotatedPreviewHeight = width
                maxPreviewWidth = displaySize.y
                maxPreviewHeight = displaySize.x
            }

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            previewSize = chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture::class.java),
                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                maxPreviewHeight, largest
            )

//            for (size in map.getOutputSizes(SurfaceTexture::class.java)) {
//                l.d("imageDimension ${size.width} x ${size.height}")
//            }

            com.watt.camera1n2.l.d("Noah previewSize w ${previewSize!!.width} h ${previewSize!!.height} rotatedPreviewWidth $rotatedPreviewWidth rotatedPreviewHeight $rotatedPreviewHeight --- view w:$width h:$height")

            this.cameraId = cameraId

            configureTransform(width, height)

        } catch (e: Exception) {
            com.watt.camera1n2.l.e(e.toString())
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        if (null == previewSize) {
            return
        }
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
//        val viewRect = RectF(0f, 0f, 1920f, 1080f)
        val bufferRect = RectF(
            0f,
            0f,
            previewSize!!.width.toFloat(),
            previewSize!!.height.toFloat()
//            previewSize!!.height.toFloat(),
//            previewSize!!.width.toFloat()
        )
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        com.watt.camera1n2.l.d("Noah configureTransform rotation : $rotation")
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / previewSize!!.height,
                viewWidth.toFloat() / previewSize!!.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        }
        else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        else if (Surface.ROTATION_0 == rotation/* || Surface.ROTATION_180 == rotation*/)
        {

        }
//            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
//            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
//            val scale = Math.max(
//                    viewHeight.toFloat() / previewSize!!.height,
//                    viewWidth.toFloat() / previewSize!!.width
//            )
//            l.d("Noah configureTransform rotation : viewWidth.toFloat():${viewWidth.toFloat()} previewSize.width:${previewSize!!.width}")
//            l.d("Noah configureTransform rotation : viewHeight.toFloat():${viewHeight.toFloat()} previewSize.height:${previewSize!!.height}")
//            l.d("Noah configureTransform rotation : scale:$scale centerX:$centerX centerY:$centerY")
//            matrix.postScale(scale, scale, centerX, centerY)
//
//            if(Surface.ROTATION_180 == rotation)
//                matrix.postRotate(180f, centerX, centerY)
//        }
        binding.textureView.setTransform(matrix)
    }




    override fun onPause() {
        super.onPause()
        l.d("Noah ==== onPause")
//            if(camera != null){
//                camera?.stopPreview()
//                preview?.setCamera(null)
////                camera?.release()                     //2022-09-02   주석처리    카메라 촬영 후 취소시 오류 발생
//                camera = null
//                isFirst = false
//            }

        defaultScope.cancel()
        zoomLevel = 1
        closeCamera()
        stopBackgroundThread()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            unregisterReceiver(mCameraBroadcastReceiver)
        }


//        binding.layout.removeView(preview)
//        preview = null
//


    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            SftpUploadManager.saveUploadingInfo(this)
            SignalingSendData.removeObserveDbDataNotFound(this)
            connectionStateMonitor.disable()
            powerStoreManager.unregister()
            orientationEventListener?.disable()
        } catch (e: java.lang.Exception) {
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


//    private fun startCamera(){
//        l.d("startCamera::::::::::: ")
//        if(preview == null){
//            l.e("preview is null")
//            preview = Preview(this, binding.surfaceView)
//            preview?.layoutParams = ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT
//            )
//            binding.layout.addView(preview)
//            preview?.keepScreenOn = true
//        }
//
//        preview?.setCamera(null)
//        if(camera != null){
//            l.e("camera is not null")
////            camera?.release()                 //2022-09-02   주석처리    카메라 촬영 후 취소시 오류 발생
//            camera = null
//        }
//
//        l.d("before get number of cameras")
//        val numCams = Camera.getNumberOfCameras()
//        if(numCams > 0){
//            try {
//                camera =
//                        Camera.open(CAMERA_FACING)
//                // camera orientation
//                camera?.setDisplayOrientation(
//                    setCameraDisplayOrientation(
//                        this, CAMERA_FACING,
//                        camera!!
//                    )
//                )
//
//
//                // get Camera parameters
//                var params = camera?.getParameters()
//                val pictureSizeList = params?.supportedPictureSizes
//                val previewSizeList = params?.supportedPreviewSizes
//                var picWidthMaxSize = 1920
//                var picHeightMaxSize = 1080
//
//
//
////                val focusModes: List<String> = params!!.getSupportedFocusModes()
////
////                if(focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
////                    //Phone supports autofocus!
////                    Log.d("Noah ", "autofocus ==========================")
////                }
////                else {
////                    //Phone does not support autofocus!
////                    Log.d("Noah ", "Phone does not support autofocus ==========================")
////                }
//
////                for (size in pictureSizeList!!) {
////                    l.d("Noah pictureSize : " + size.width + "/" + size.height)
////                    picWidthMaxSize = size.width
////                    picHeightMaxSize = size.height
////                    //break
////                }
//
////                for(size in previewSizeList!!){
////                    l.d("Noah pictureSize : " + size.width + "/" + size.height)
////                }
//
//
//
//                params!!.setRotation(
//                    setCameraDisplayOrientation(
//                        this,
//                        CAMERA_FACING,
//                        camera!!
//                    )
//                )
//
//
//
//                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q){
//                    params.setPreviewSize(1920, 1080)
//                    //params.setPictureSize(3840, 2160)
//                    //params.setPictureSize(4608, 3456)
//                }else{
//                    params.setPictureSize(3840, 2160)
////                    params.setPictureSize(1920, 1080)                     // 2023-07-12      해상도 줄임
//                }
//
//                camera?.setParameters(params)
//                camera?.startPreview()
////                l.d("Noah Camera preview size: " + params.previewSize.width + " x " + params.previewSize.height)
////                l.d("Noah Camera preview-picture size: " + params.pictureSize.width + " x " + params.pictureSize.height)
//                parameters = camera?.getParameters()
//
//                parameters!!.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//
//                if (isFirst) {
//                    parameters?.setZoom(0)
//                    camera?.setParameters(parameters)
//                } else {
//                    parameters?.setZoom(dataUser?.zoom!!)
//                    camera?.setParameters(parameters)
//                }
//            } catch (ex: RuntimeException) {
//                toast("camera_not_found " + ex.message.toString())
//                l.d("camera_not_found " + ex.message.toString())
//                return
//            }
//
//            preview?.setCamera(camera)
//        }else{
//            l.e("number of cameras == 0")
//        }
//
//    }


//    private fun command(name: String){
//        val createTextView = CreateTextView(this)
//        createTextView.setText(name){ view ->
//            //back ground command
//            if (view is TextView) {
//                if (view.text.toString() == resources.getString(R.string.go_back)) {
//                    start<InputMemoActivity> {
//                        putExtra(this, currentMemoTableData)
//                    }
//                    finish()
//                } else if (view.text.toString() == resources.getString(R.string.zoom) + " 1") {
//                    binding.tvZoom1.setTextColor(getColor(R.color.text_yellow))
//                    binding.tvZoom2.setTextColor(Color.WHITE)
//                    binding.tvZoom3.setTextColor(Color.WHITE)
//                    binding.tvZoom4.setTextColor(Color.WHITE)
//                    binding.tvZoom5.setTextColor(Color.WHITE)
//                    parameters?.zoom = 0
//                    dataUser?.zoom = 0
//                    camera!!.parameters = parameters
//                    camera?.autoFocus { success, _ ->
//                        if (!success) toast(resources.getString(R.string.fail_focusing))
//
//                    }
//                } else if (view.text.toString() == resources.getString(R.string.zoom) + " 2") {
//                    binding.tvZoom1.setTextColor(Color.WHITE)
//                    binding.tvZoom2.setTextColor(getColor(R.color.text_yellow))
//                    binding.tvZoom3.setTextColor(Color.WHITE)
//                    binding.tvZoom4.setTextColor(Color.WHITE)
//                    binding.tvZoom5.setTextColor(Color.WHITE)
//                    parameters?.setZoom(10)
//                    dataUser?.zoom = 10
//                    camera!!.parameters = parameters
//                    camera?.autoFocus { success, _ ->
//                        if (!success) toast(resources.getString(R.string.fail_focusing))
//                    }
//                } else if (view.text.toString() == resources.getString(R.string.zoom) + " 3") {
//                    binding.tvZoom1.setTextColor(Color.WHITE)
//                    binding.tvZoom2.setTextColor(Color.WHITE)
//                    binding.tvZoom3.setTextColor(getColor(R.color.text_yellow))
//                    binding.tvZoom4.setTextColor(Color.WHITE)
//                    binding.tvZoom5.setTextColor(Color.WHITE)
//                    parameters?.setZoom(30)
//                    dataUser?.zoom = 30
//                    camera!!.parameters = parameters
//                    camera!!.autoFocus { success, _ ->
//                        if (!success) toast(resources.getString(R.string.fail_focusing))
//                    }
//                } else if (view.text.toString() == resources.getString(R.string.zoom) + " 4") {
//                    binding.tvZoom1.setTextColor(Color.WHITE)
//                    binding.tvZoom2.setTextColor(Color.WHITE)
//                    binding.tvZoom3.setTextColor(Color.WHITE)
//                    binding.tvZoom4.setTextColor(getColor(R.color.text_yellow))
//                    binding.tvZoom5.setTextColor(Color.WHITE)
//                    parameters?.setZoom(60)
//                    dataUser?.zoom = 60
//                    camera!!.parameters = parameters
//                    camera?.autoFocus { success, camera ->
//                        if (!success) toast(resources.getString(R.string.fail_focusing))
//                    }
//                } else if (view.text.toString() == resources.getString(R.string.zoom) + " 5") {
//                    binding.tvZoom1.setTextColor(Color.WHITE)
//                    binding.tvZoom2.setTextColor(Color.WHITE)
//                    binding.tvZoom3.setTextColor(Color.WHITE)
//                    binding.tvZoom4.setTextColor(Color.WHITE)
//                    binding.tvZoom5.setTextColor(getColor(R.color.text_yellow))
//                    parameters?.setZoom(88)
//                    dataUser?.zoom = 88
//                    camera!!.parameters = parameters
//                    camera?.autoFocus { success, _ ->
//                        if (!success) toast(resources.getString(R.string.fail_focusing))
//                    }
//                } else if (view.text.toString() == resources.getString(R.string.manual_focus)) {
//                    camera!!.autoFocus { success, _ ->
//                        if (!success) toast(resources.getString(R.string.fail_focusing))
//                    }
//                } else if (view.text.toString() == resources.getString(R.string.flash)) {
//                    if (!isFlashStatus) {
//                        parameters?.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
//                        camera!!.parameters = parameters
//                        isFlashStatus = true
//                        binding.tvFlashStatus.text = resources.getString(R.string.flash_on)
//                    } else {
//                        parameters?.setFlashMode(Camera.Parameters.FLASH_MODE_OFF)
//                        camera!!.parameters = parameters
//                        isFlashStatus = false
//                        binding.tvFlashStatus.setText(resources.getString(R.string.flash_off))
//                    }
//                } else if (view.text.toString() == resources.getString(R.string.telephoto)){
//                    setTelephotoOn()
//                }
////                else if (view.text.toString() == resources.getString(R.string.capture)) {
////                    setScreenShot()
////                } else if (view.text.toString() == resources.getString(R.string.video)) {
////                    start<VideoActivity> {
////                        putExtra("memo_seq", memoSeq)
////                        putExtra("en_seq", enSeq)
////                        putExtra("user_id", userID)
////                        putExtra("user_name", userName)
////                        putExtra("memo_contents", memoContents)
////                        putExtra("save_time", saveTime)
////                    }
////                    finish()
////                }
//            }
//        }
//        binding.llCommand.addView(createTextView)
//    }


    private fun showMenu() {
        binding.flGoBack.visibility = View.VISIBLE
        binding.llZoom.visibility = View.VISIBLE
//        binding.tvFocus.visibility = View.VISIBLE
        binding.llFlash.visibility = View.VISIBLE
        binding.llModeSelect.visibility = View.VISIBLE
        binding.llTelephoto.visibility = View.VISIBLE
        binding.tvMenu.text = resources.getString(R.string.hide_menu)
        dataUser?.menuStatus = true
    }

    private fun hideMenu() {
        binding.flGoBack.visibility = View.GONE
        binding.llZoom.visibility = View.GONE
//        binding.tvFocus.visibility = View.GONE
        binding.llFlash.visibility = View.GONE
        binding.llModeSelect.visibility = View.GONE
        binding.llTelephoto.visibility = View.GONE
        binding.tvMenu.text = resources.getString(R.string.show_menu)
        dataUser?.menuStatus = false
    }

//    private fun resetTelephoto(){
//        isFirst = false
////        startCamera()
//        defaultScope.cancel()
//        closeCamera()
//        stopBackgroundThread()
//
//        zoom = null
//        startBackgroundThread()
//        if(binding.textureView.isAvailable){
//            if(cameraDevice == null){
//                openCamera(binding.textureView.width, binding.textureView.height)
//            }
//        }else{
//            binding.textureView.surfaceTextureListener = surfaceTextureListener
//        }
//    }

    private fun resetCam(){
        isFirst = false
//        startCamera()

        defaultScope.cancel()
        closeCamera()
        stopBackgroundThread()

        zoom = null
        startBackgroundThread()
        if(binding.textureView.isAvailable){
            if(cameraDevice == null){
                Log.d("Noah","resetCam ==== openCamera ====")
                openCamera(binding.textureView.width, binding.textureView.height)
            }

        }else{
            binding.textureView.surfaceTextureListener = surfaceTextureListener
        }
    }


//    private fun setScreenShot() {
//        camera!!.autoFocus { success, camera ->
//            if (success) {
//                camera.takePicture(shutterCallback, rawCallback, jpegCallback)
//            } else {
//                Toast.makeText(
//                    applicationContext,
//                    resources.getString(R.string.fail_focusing),
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//    }

//    private val shutterCallback = Camera.ShutterCallback {
//        l.d("onShutter'd")
//    }
//
//    private val rawCallback = Camera.PictureCallback{ data, camera ->
//        l.d("onPictureTaken - raw")
//    }
//
//    private val jpegCallback = Camera.PictureCallback{ data, camera ->
//        l.d("onJpegCallback")
//        val options = BitmapFactory.Options()
//        options.inPreferredConfig = Bitmap.Config.ARGB_8888
//        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
//
//        localBitmap = cropBitmap(bitmap)
//        localBitmap?.let{
//            showSaveImageDialog(it)
//        }
//    }

    @SuppressLint("MissingPermission")
    private fun showSaveImageDialog(bitmap: Bitmap){
        if(imageFileSaveDialog ==null){

            imageFileSaveDialog = ImageFileSaveDialog(this, bitmap){
                when(it){
                    0 -> { // save
                        saveImage()
//                        resetCam()
                    }
                    1 -> { // cancel
                        localBitmap = null

//                        resetCam()
//                        listener?.onCanceledSavePhoto()
//
//                        callbackCanceledSavePhoto?.let { callback ->
//                            callback()
//                        }
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


//    private fun showSaveImageDialog(bitmap: Bitmap){
//        if(imageFileSaveDialog ==null){
//            imageFileSaveDialog = ImageFileSaveDialog(this, bitmap){
//                when(it){
//                    0 -> { // save
//                        saveImage()
////                        startCamera()
//                    }
//                    1 -> { // cancel
//                        try {
//                            camera?.stopPreview()
//                            preview = null
//                            preview?.setCamera(null)
////                        camera?.release()                 //2022-09-02   주석처리    카메라 촬영 후 취소시 오류 발생
//                            camera = null
//
//                            localBitmap = null
//                            resetCam()
//                        } catch (e: java.lang.Exception) {
//                            Log.d("Noah Camera Cancel ", e.stackTraceToString())
//                        }
//                    }
//                }
//            }
//        }
//
//        imageFileSaveDialog?.setOnDismissListener {
//            imageFileSaveDialog = null
//            bitmap.recycle()
//        }
//
//        imageFileSaveDialog?.let{
//            if(!it.isShowing)
//                it.show()
//        }
//    }

    // 사진 저장
    var currentPictureData: ByteArray?=null
    private fun saveImage(){
//        val orientation: Int =
//                setCameraDisplayOrientation(
//                    this,
//                    CAMERA_FACING, camera!!
//                )
//
//        val matrix = Matrix()
//        matrix.postRotate(orientation.toFloat())
//        val stream = ByteArrayOutputStream()
//        if (localBitmap != null) {
//            //localBitmap = cropBitmap(localBitmap!!)
//            localBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
//            currentPictureData = stream.toByteArray()
//            onLaunchDictation()
//        }
//        Log.d("Noah", "==== localBitmap ==== ${localBitmap}")

        val stream = ByteArrayOutputStream()
        if (localBitmap != null) {
            //localBitmap = cropBitmap(localBitmap!!)
//            l.d("local bitmap width : ${localBitmap?.width}, height : ${localBitmap?.height}")
            localBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            currentPictureData = stream.toByteArray()
//            l.d("current picture data size : ${currentPictureData?.size}")

            onLaunchDictation()
        }

    }


    private fun cropBitmap(bitmap: Bitmap):Bitmap{
        val originalWidth = bitmap.width
        l.d("bitmap width : $originalWidth, height : ${bitmap.height}")
        if(originalWidth != 4608){
            return bitmap
        }
        return Bitmap.createBitmap(bitmap, 384, 648, 3840, 2160)                          // 2023-07-12      해상도 줄임
//        return Bitmap.createBitmap(bitmap, 384, 648, 1920, 1080)
    }

    // 사진 저장
    private fun onLaunchDictation(){
        Log.d("Noah ==== ", "onLaunchDictation")
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
                fileDir = SignalingSendData.getRootPath() + SignalingSendData.EVENT_CREATE_TEMP_FOLDER              //  /TEMP

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
                    // 썸네일 생성
                    createThumbnail(fileDir, fileName)
                    // fileUpload 호출
                    Log.d("Noah ==== ", "파일 업로드 호출 ===== ")
//                    sftpConnect.fileUpload(
//                        SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER,
//                        fileDir,
//                        fileName,
//                        currentMemoTableData.memo_seq,
//                        "P"
//                    ){
//                        l.d("Noah ==== file upload complete")
//                    }

                    bindService?.sftpConnect?.fileUpload(
                            SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER,                    //   /ORIGINAL
                            fileDir,
                            fileName,
                        currentMemoTableData.memo_seq,
                            "P"
                    ){
                        l.d("original file upload complete")
                    }
                } else {
                    // memo_seq가 없을때
                    Log.d("Noah ==== ", "SEQ 없이 입력 사진 등록 중 .......")
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
        Log.d("Noah ==== ", "handleMessage: ==============EVENT_INSERT_DATA_RESULT")
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
                Log.d("Noah ==== ", " New Seq !!!!!!!!!!!!!!!!!!!!!")
                SignalingSendData.selectLastNumberQuery(
                    currentMemoTableData.user_id,
                    currentMemoTableData.en_seq ?: -1
                ) {
                    handleSelectLastNumberQuery(it.toString().toInt())
                }
            }
        }
    }

    // memo_seq를 전달받아 fileUpload 한다.
    private fun handleSelectLastNumberQuery(result: Int){
        currentMemoTableData.memo_seq = result
        // Memo_seq가 없는 경우 썸네일 저장
        createThumbnail(fileDir, fileName)          // TEMP 경로에 있는 파일
        // fileUpload 호출
        Log.d("Noah ==== ", "파일 업로드 호출 ===== ")
        sftpConnect.fileUpload(
            SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER,
            fileDir,
            fileName,
            currentMemoTableData.memo_seq,
            "P"
        ){
            l.d("Noah ==== file upload complete")
        }
    }

    // 썸네일 업로드
    private fun uploadThumbnailFile(path: String){
        l.d("start upload thumbnail file")
        // thumbnailUpload 호출
        Log.d("Noah ==== ", "uploadThumbnailFile ==== path ==== ${path}");
        sftpConnect.fileUpload(
            SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER,
            path,
            "t_$fileName",
            currentMemoTableData.memo_seq,
            "P"
        ){
            l.d("Noah ==== upload thumbnail file complete")
        }
    }

    // 썸네일 생성
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
                uploadThumbnailFile(thumbnailPath)
                l.d("reset cam before")
                resetCam()
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