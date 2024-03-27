package com.peng.power.memo.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.recyclerview.widget.PagerSnapHelper
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.peng.plant.powerreceiver.POWERDEFINE
import com.peng.plant.powerreceiver.PowerReceiverData
import com.peng.plant.powerreceiver.PowerStoreManager
import com.peng.power.memo.BuildConfig
import com.peng.power.memo.R
import com.peng.power.memo.adapter.RecyclerViewAdapter
import com.peng.power.memo.broadcastreceiver.ConnectionStateMonitor
import com.peng.power.memo.data.*
import com.peng.power.memo.databinding.ActivityMainBinding
import com.peng.power.memo.dialog.QuitDialog
import com.peng.power.memo.dialog.SortDialog
import com.peng.power.memo.dialog.StartRemainUploadDialog
import com.peng.power.memo.dialog.SystemCheckDialog
import com.peng.power.memo.preference.DataUser
import com.peng.power.memo.preference.PreferenceValue
import com.peng.power.memo.sftp.SftpService
import com.peng.power.memo.sftp.SftpUploadManager
import com.peng.power.memo.sftp.UploadStatus
import com.peng.power.memo.util.l
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONException
import splitties.activities.start
import splitties.toast.longToast
import splitties.toast.toast
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs


class MainActivity : BaseActivity<ActivityMainBinding>(){
    //Debug모드 활성화 시 테스트 서버 설정
    private var testServerInfo: TestServerInfo.serverInfo = TestServerInfo().meet_test

    private var connectionStateMonitor = ConnectionStateMonitor()
    private var mMemoDataList = ArrayList<MemoTblData>()

    private var scrollZoomLayoutManager:ScrollZoomLayoutManager?=null
    private var tiltScrollController: TiltScrollController? = null
//ViewById
//    private var moveUsingGyro:MoveUsingGyro?= null
//    private val scaleFactor = 100

    private var sortDialog: SortDialog?=null

    private var quitDialog:QuitDialog?=null
    private var startRemainUploadDialog:StartRemainUploadDialog?=null


    private var bindService: SftpService?=null

    private var dataUser: DataUser?=null

    var display: DisplayMetrics? = null

    override fun initViewBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initAfterBinding() {
        SignalingSendData.initRootPath(this)

        initTimeSettingDialog();
        checkSystemAutoTime();

        // SharedPreference에 저장되어있는 유저데이터를 가져온다.
        dataUser = PreferenceValue.getDataUser(this)

        // 2023-02-06    Navigate520으로 인해 DP로 구분짓기 위함
        display = this.applicationContext?.resources?.displayMetrics

        initUI()
        connectionStateMonitor.enable(this)
        powerStoreManager.register()


        SignalingSendData.addObserveDbDataNotFound(this) {
            dbDataNotFound()
        }

        setUploadingObserver()
    }


    var mSystemCheckDialog: SystemCheckDialog? = null
    private val TIME_SETTING_REQUEST_CODE = 99

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TIME_SETTING_REQUEST_CODE) {
            checkSystemAutoTime()
        }
    }


    private fun checkSystemAutoTime(): Boolean {
        var isTimeEnabled = true
        var autoTime = 0
        var autoTimezone = 1
        try {
            autoTime = Settings.System.getInt(contentResolver, Settings.System.AUTO_TIME)
            autoTimezone = Settings.System.getInt(contentResolver, Settings.System.AUTO_TIME_ZONE)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
//        l.d("Noah checkSystemAutoTime / $autoTime / $autoTimezone")
        if (autoTime == 0 || autoTimezone == 0) {
            if (mSystemCheckDialog != null && !mSystemCheckDialog!!.isShowing()) {
                mSystemCheckDialog?.show()
                mSystemCheckDialog?.setText(resources.getString(R.string.date_time_setting_popup_msg))
            }
            isTimeEnabled = false
        }
        return isTimeEnabled
    }

    private fun initTimeSettingDialog() {
        if (mSystemCheckDialog == null) {
            mSystemCheckDialog = SystemCheckDialog(this, object : SystemCheckDialog.Listener {
                override fun onTimeSetting() {
                    val intent = Intent(Settings.ACTION_DATE_SETTINGS)
                    startActivityForResult(intent, TIME_SETTING_REQUEST_CODE)
                    mSystemCheckDialog?.dismiss()
                }

                override fun onExit() {
                    mSystemCheckDialog?.dismiss()
                    onBackPressed()
                }

                override fun onShow() {}
                override fun onDismiss() {}
                override fun onCancel() {}
            })
        }
    }


    // 현재 업로드되고 있는 파일이 있는지의 상태값을 관찰하여 업로드 파일이 있으면 전체적으로 몇퍼센트가 완료되었는지 화면 하단에 표시해 준다.
    @SuppressLint("SetTextI18n")
    private fun setUploadingObserver(){
        SftpUploadManager.uploadStatus.observe(this){
            when(it){
                UploadStatus.None -> {
                    l.d("upload status none")
                    binding.uploadingPercent.text = "(0%)"
                    binding.llUploadInfo.visibility = View.GONE

                }
                UploadStatus.Uploading -> {
                    binding.uploadingPercent.text = "(${SftpUploadManager.progressPercent.value}%)"
                    binding.llUploadInfo.visibility = View.VISIBLE
                }else->{
                    binding.uploadingPercent.text = "(0%)"
                    binding.llUploadInfo.visibility = View.GONE
                }
            }
        }
    }


    // 테스트 시 필요한 메서드 - 하드코딩된 AppDetailJson을 사용
    private fun setDataUserAndAppDetail(serverInfo: TestServerInfo.serverInfo){

        Log.d("Noah ", "${serverInfo}")

        dataUser?.enSeq = serverInfo.enSeq
        dataUser?.hqSeq = serverInfo.hqSeq
        dataUser?.brSeq = serverInfo.brSeq
        dataUser?.userId = serverInfo.userId
        dataUser?.userName = serverInfo.userName

        try{
            SignalingSendData.sftpData = Json.decodeFromString<SignalingSendData.SftpData>(
                serverInfo.appDetailJson
            )

//            Log.d("Noah ", "SignalingSendData.sftpData.thermal ---- " + SignalingSendData.sftpData?.thermal)

            Log.d("Noah ", "setDataUserAndDetail ---- ")

            checkAppDetailEmpty()

            dataUser?.thermal = SignalingSendData.sftpData?.thermal!!

//            Log.d("Noah ", "dataUser.thermal ---- " + dataUser?.thermal)

            if(checkNotUseLastSlash(SignalingSendData.sftpData?.sftp_root_folder))
                SignalingSendData.sftpData?.sftp_root_folder = SignalingSendData.sftpData?.sftp_root_folder + "/"

            if(SignalingSendData.sftpData?.sftp_url.isNullOrEmpty()){
                toast(resources.getString(R.string.invalid_sftp_url))
                Log.d("Noah ", " " + SignalingSendData.sftpData?.sftp_url)
            }else{
                if(checkNotUseLastSlash(SignalingSendData.sftpData?.sftp_url))
                    SignalingSendData.sftpData?.sftp_url = SignalingSendData.sftpData?.sftp_url + "/"
            }

        }catch (e: JSONException){
            e.printStackTrace()
        }

//        l.d("sftp root folder : ${SignalingSendData.sftpData?.sftp_root_folder}")
    }

    private fun checkNotUseLastSlash(str: String?):Boolean{
        return str?.last().toString() != "/"
    }


    private fun executeQuit(){
//        l.d("execute quit process")
        launch(Dispatchers.IO) {
            //SftpUploadManager.stopAllUpload()
            SftpUploadManager.deleteDownloadFile()
            SignalingSendData.socketDisconnect()
            moveTaskToBack(true);						// 태스크를 백그라운드로 이동
            finishAndRemoveTask();						// 액티비티 종료 + 태스크 리스트에서 지우기
            Process.killProcess(Process.myPid());	// 앱 프로세스 종료
        }
    }


    // 업로드 도중 중지된 파일이 있나 체크
    private fun checkRemainUploadFile(){
        if(!SftpUploadManager.isFirstNoticeForRemainUpload){
            return
        }

        SftpUploadManager.isFirstNoticeForRemainUpload = false

        launch(Dispatchers.IO) {
            val insidePath = SignalingSendData.getRootPath() + SignalingSendData.EVENT_CREATE_TEMP_FOLDER

//            l.d("check remain upload file -- check inside path : $insidePath")
            val allFile = File(insidePath)

            if(allFile.listFiles() == null){
//                withContext(Dispatchers.Main){
//                    startRemainUploadDialog?.show()
//                }
                return@launch
            }

            var fileCount = 0

            for (file in allFile.listFiles()) {
//                l.d("in temp folder file name : ${file.name}")
                val tempFile = File(insidePath + file.name)
                if (file.name == "temp") {
//                    l.d("found temp file --- delete temp file and continue~~")
                    tempFile.delete()
                    continue
                }else{
                    fileCount++
                }
            }

            if (fileCount == 0) {
//                l.d("file count == 0")
                return@launch
            } else {
                withContext(Dispatchers.Main){
                    startRemainUploadDialog?.show()
                }
            }
        }
    }


    // AppDetailJson에서 필요한 정보가 다 들어왔는지 확인한다.
    private fun checkAppDetailEmpty(){
        var errors =""
        if(dataUser?.enSeq == -1){
            errors += "enSeq == null\n"
        }
        if(dataUser?.hqSeq == -1){
            errors += "hqSeq == null\n"
        }

        if(dataUser?.brSeq == -1){
            errors += "brSeq == null\n"
        }

        if(dataUser?.userId.isNullOrEmpty()){
            errors += "userId == null\n"
        }
        if(dataUser?.userName.isNullOrEmpty()){
            errors += "userName == null\n"
        }

//        Log.d("Noah", " ==== dataUser?.thermal ==== " + dataUser?.thermal)

        Log.d("Noah ",
            "datauser: ${dataUser?.userName} / ${dataUser?.userId} / ${dataUser?.enSeq} / ${dataUser?.hqSeq} / ${dataUser?.brSeq}")

        if(SignalingSendData.sftpData?.sftp_root_folder.isNullOrEmpty()) {
            errors += "sftp_root_folder == null\n"
            //toast("root folder의 값이 올바르지 않습니다.")
            Log.d("Noah ", "datauser: ${SignalingSendData.sftpData?.sftp_root_folder}")
        }
        if(SignalingSendData.sftpData?.socket_url.isNullOrEmpty()){
            errors += "socket_url == null\n"
            //toast("socket_url의 값이 올바르지 않습니다.")
            Log.d("Noah ", "datauser: ${SignalingSendData.sftpData?.socket_url}")
        }
        if(SignalingSendData.sftpData?.sftp_host.isNullOrEmpty()){
            errors += "sftp_host == null\n"
            //toast("sftp_host의 값이 올바르지 않습니다.")
            Log.d("Noah ", "datauser: ${SignalingSendData.sftpData?.sftp_host}")
        }
        if(SignalingSendData.sftpData?.sftp_user.isNullOrEmpty()){
            errors += "sftp_user == null\n"
            //toast("sftp_user의 값이 올바르지 않습니다.")
            Log.d("Noah ", "datauser: ${SignalingSendData.sftpData?.sftp_user}")
        }
        if(SignalingSendData.sftpData?.sftp_user_pw.isNullOrEmpty()){
            errors += "sftp_user_pw == null\n"
            //toast("sftp_user_pw의 값이 올바르지 않습니다.")
            Log.d("Noah ", "datauser: ${SignalingSendData.sftpData?.sftp_user_pw}")
        }
        if(SignalingSendData.sftpData?.sftp_port == -1) {
            errors += "sftp_port == null\n"
            //toast("sftp_port의 값이 올바르지 않습니다.")
            Log.d("Noah ", "datauser: ${SignalingSendData.sftpData?.sftp_port}")
        }
        if(SignalingSendData.sftpData?.sftp_url.isNullOrEmpty()){
            errors += "sftp_url == null"
            Log.d("Noah ", "datauser: ${SignalingSendData.sftpData?.sftp_url}")
        }

        if(errors.isNotEmpty()){
            Log.d("Noah ", "errors: ${errors}")
//            l.e("errors is not empty")
            longToast(resources.getString(R.string.fail_init_setting))
            launch {
                delay(1500)
                moveTaskToBack(true);						// 태스크를 백그라운드로 이동
                finishAndRemoveTask();						// 액티비티 종료 + 태스크 리스트에서 지우기
                Process.killProcess(Process.myPid());	// 앱 프로세스 종료
            }
        }
    }


    private fun initUI(){
        quitDialog = QuitDialog(this)
        startRemainUploadDialog = StartRemainUploadDialog(this)


        checkRemainUploadFile()

        val receiveUserID = intent.getStringExtra("user_id")
        val receiveUserName = intent.getStringExtra("user_name")
        val callActivity = intent.getStringExtra("callActivity")
        val receiveEnSeq = intent.getIntExtra("en_seq", -1)
        val receiveHqSeq = intent.getIntExtra("hq_seq", -1)
        val receiveBrSeq = intent.getIntExtra("br_seq", -1)

//        l.d("Noah receiveUserID:$receiveUserID / receiveUserName:$receiveUserName / callActivity:$callActivity / enSeq:$receiveEnSeq / hqSeq:$receiveHqSeq / brSeq:$receiveBrSeq")

        receiveUserID?.let {
            dataUser?.userId = it.toLowerCase(Locale.ROOT)
        }

        receiveUserName?.let{
            dataUser?.userName = it
        }

//        l.d("Noah enSeq:$receiveEnSeq, hqSeq:$receiveHqSeq, brSeq:$receiveBrSeq")

        if(receiveEnSeq >= 0){
            dataUser?.enSeq = receiveEnSeq
            dataUser?.hqSeq = receiveHqSeq
            dataUser?.brSeq = receiveBrSeq
        }else{
            if(BuildConfig.SAVE_APPDETAILINFO){
                dataUser = PreferenceValue.getDataUser(this)
//                l.d("Noah receiveEnSeq == $receiveEnSeq")
//                l.d("Noah load preferenceValue -- enseq:${dataUser?.enSeq}, brseq:${dataUser?.brSeq}, hqseq:${dataUser?.hqSeq}")
            }
        }


        if(callActivity == null){
//            l.d("call activity is null")
            dataUser?.sort = 2
            dataUser?.page = 1
        }


        var appDetailJson = intent.getStringExtra("app_detail_json")

        if(appDetailJson == null){
            if(BuildConfig.SAVE_APPDETAILINFO){
                appDetailJson = PreferenceValue.getAppDetailJson(this)
                l.d("Noah ==== loadedAppDetailJson : $appDetailJson")
            }
            if(BuildConfig.DEBUG_MODE){
                Log.d("Noah ", "!!!! Debug mode!!!!")
//                l.e("!!!! Debug mode!!!!")
                setDataUserAndAppDetail(testServerInfo)
            }
        }

//        Log.d("Noah ", "appDetailJson: $appDetailJson ")
        if(appDetailJson.isNullOrEmpty()){
            checkAppDetailEmpty()
        }else{
            if(BuildConfig.SAVE_APPDETAILINFO){
                PreferenceValue.setAppDetailJson(this, appDetailJson)
            }

//            l.d("Noah ==== loadedAppDetailJson : $appDetailJson")
            Log.d("Noah", " ==== loadedAppDetailJson ==== " + appDetailJson)

            try{
                SignalingSendData.sftpData = Json.decodeFromString<SignalingSendData.SftpData>(
                    appDetailJson
                )

                checkAppDetailEmpty()

                Log.d("Noah", " ==== initUI() ==== MainActivity ==== thermal ==== " + SignalingSendData.sftpData?.thermal!!)


                 dataUser?.thermal = SignalingSendData.sftpData?.thermal!!
//                Log.d("Noah", " ==== initUI() ==== MainActivity ==== " + dataUser?.thermal)

                if(checkNotUseLastSlash(SignalingSendData.sftpData?.sftp_root_folder))
                    SignalingSendData.sftpData?.sftp_root_folder = SignalingSendData.sftpData?.sftp_root_folder + "/"

                if(checkNotUseLastSlash(SignalingSendData.sftpData?.sftp_url))
                    SignalingSendData.sftpData?.sftp_url = SignalingSendData.sftpData?.sftp_url + "/"

            }catch (e: JSONException){
//                l.e("app detail json exception : $e")
            }
        }

//        Log.d("Noah ", " recycler.size ---- " + binding.recycler.size)

        scrollZoomLayoutManager = ScrollZoomLayoutManager(this, dp2Px(5f))
        binding.recycler.addOnScrollListener(CenterScrollListener())
        binding.recycler.layoutManager = scrollZoomLayoutManager

        val pagerSnapHelper = PagerSnapHelper()
        pagerSnapHelper.attachToRecyclerView(binding.recycler)

        if(dataUser?.page == null){
            binding.tvNowPage.text = "1"
        }else{
            binding.tvNowPage.text = dataUser?.page.toString()
        }

        tiltScrollController = TiltScrollController(this){ x, y, deltaX ->
            //   기존 글라스용 감도
            //l.d("x : $x, deltaX : $deltaX")
//            when {
//                abs(deltaX) > 1.8 -> {
//                    binding.recycler.smoothScrollBy(
//                        x * scrollZoomLayoutManager!!.eachItemWidth / 3,
//                        0
//                    )
//                }
//                abs(deltaX) > 1.4 -> {
//                    binding.recycler.smoothScrollBy(
//                        x * scrollZoomLayoutManager!!.eachItemWidth / 7,
//                        0
//                    )
//                }
//                abs(deltaX) > 1.0 -> {
//                    binding.recycler.smoothScrollBy(
//                        x * scrollZoomLayoutManager!!.eachItemWidth / 20,
//                        0
//                    )
//                }
//                abs(deltaX) > 0.6 -> {
//                    binding.recycler.smoothScrollBy(
//                        x * scrollZoomLayoutManager!!.eachItemWidth / 30,
//                        0
//                    )
//                }
//                else -> binding.recycler.smoothScrollBy(
//                    x * (scrollZoomLayoutManager!!.eachItemWidth / 3000),
//                    0
//                )
//            }

            if (binding.recycler.adapter != null){
                //  새 글라스용 감도
                when {
                    Math.abs(deltaX) > 1.8 -> {
                        binding.recycler.smoothScrollBy(x * (binding.recycler.adapter!!.itemCount) * 5,
                            0)
                    }
                    Math.abs(deltaX) > 1.4 -> {
                        binding.recycler.smoothScrollBy(x * (binding.recycler.adapter!!.itemCount) * 3,
                            0)
                    }
                    Math.abs(deltaX) > 0.6 -> {
                        binding.recycler.smoothScrollBy(x * (binding.recycler.adapter!!.itemCount),
                            0)
                    }
                    Math.abs(deltaX) > 0.3 -> {
                        binding.recycler.smoothScrollBy(x * (binding.recycler.adapter!!.itemCount / 15),
                            0)
                    }
                    else -> {
                        binding.recycler.smoothScrollBy(x * (binding.recycler.adapter!!.itemCount / 4000),
                            0)
                    }
                }
            }




//            if (Math.abs(deltaX) > 1.8) {
//                binding.recycler.smoothScrollBy(x * (scrollZoomLayoutManager!!.eachItemWidth)*5, 0)
//            } else if(Math.abs(deltaX) > 1.4){
//                binding.recycler.smoothScrollBy(x * (scrollZoomLayoutManager!!.eachItemWidth)*3, 0)
//            }  else if(Math.abs(deltaX) > 0.6){
//                binding.recycler.smoothScrollBy(x * (scrollZoomLayoutManager!!.eachItemWidth), 0)
//            }  else if(Math.abs(deltaX) > 0.3){
//                binding.recycler.smoothScrollBy(x * (scrollZoomLayoutManager!!.eachItemWidth / 15), 0)
//            }  else
//                binding.recycler.smoothScrollBy(x * (scrollZoomLayoutManager!!.eachItemWidth / 4000), 0)

        }



        when(dataUser?.sort){
            1 -> binding.tvSortContents.text = resources.getString(R.string.all_memo_asc)
            2 -> binding.tvSortContents.text = resources.getString(R.string.all_memo_desc)
            3 -> binding.tvSortContents.text = resources.getString(R.string.my_memo_asc)
            4 -> binding.tvSortContents.text = resources.getString(R.string.my_memo_desc)
        }

        binding.tvMemoListTitle.text = resources.getString(R.string.memo_list)

        if(Locale.getDefault() == Locale.KOREA){
            binding.tvEnPage.visibility = View.GONE
            binding.tvKrPage.visibility = View.VISIBLE
        }else{
            binding.tvEnPage.visibility = View.VISIBLE
            binding.tvKrPage.visibility = View.GONE
        }



        binding.tvBack.setOnClickListener {
            if(SftpUploadManager.uploadStatus.value == UploadStatus.Uploading){
                quitDialog?.showDialog {
                    executeQuit()
                }
            }else{
                executeQuit()
            }
        }

        binding.tvSort.setOnClickListener {
            showSortDialog()
        }

        binding.tvRefresh.setOnClickListener {
            SignalingSendData.selectMemoListTotalCount(
                dataUser?.userId!!,
                dataUser?.enSeq!!,
                dataUser?.sort!!
            ){ result ->
                onSelectMemoListTotalCount(result)
            }
        }

        binding.tvCreateMemo.setOnClickListener {
            start<InputMemoActivity> {
                val memoTableData = MemoTableData(
                    0,
                    dataUser?.enSeq,
                    dataUser?.hqSeq,
                    dataUser?.brSeq,
                    dataUser?.userId ?: "",
                    dataUser?.userName ?: "",
                    "",
                    0
                )
                putExtra(this, memoTableData)
            }
            finish()
        }

        binding.tvCreateMemoCommand.setOnClickListener {
            start<InputMemoActivity> {
                val memoTableData = MemoTableData(
                    0,
                    dataUser?.enSeq,
                    dataUser?.hqSeq,
                    dataUser?.brSeq,
                    dataUser?.userId ?: "",
                    dataUser?.userName ?: "",
                    "",
                    0
                )
                putExtra(this, memoTableData)
            }
            finish()
        }

        binding.tvSelect.setOnClickListener {
            scrollZoomLayoutManager?.currentPosition?.let{
//                l.d("mMemoDataList[it].dbMemoSeq : ${mMemoDataList[it].dbMemoSeq}")
                val memoTableData = MemoTableData(
                    mMemoDataList[it].dbMemoSeq!!,
                    mMemoDataList[it].dbEnSeq,
                    mMemoDataList[it].dbHqSeq,
                    mMemoDataList[it].dbBrSeq,
                    mMemoDataList[it].dbUserID ?: "",
                    mMemoDataList[it].dbUserName ?: "",
                    mMemoDataList[it].dbMemoContents ?: "",
                    mMemoDataList[it].dbSaveTime ?: 0
                )
//                l.d("after memotabledata : ${memoTableData.memo_contents}")
                selectMemo(memoTableData)
            }
        }

        // 2023-02-06   Navigate520
        if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
            binding.tvBack.setTextSize(23f)
            binding.tvMemoListTitle.setTextSize(27f)
            binding.tvNowPageTitle.setTextSize(23f)
            binding.tvNowPage.setTextSize(23f)
            binding.tvMovePage.setTextSize(21f)
            binding.tvKrPage.setTextSize(23f)
            binding.tvSort.setTextSize(21f)
            binding.tvSortContents.setTextSize(21f)
            binding.tvCreateMemo.setTextSize(20f)
            binding.tvRefresh.setTextSize(23f)
            binding.tvSelect.setTextSize(23f)
        }


        setEmergencyCall()

        checkPermissions()
    }


    private fun selectMemo(memoTableData: MemoTableData){
        SignalingSendData.selectUserIdFromMemoSeq(memoTableData.memo_seq){ result ->
            if(result.toString() == "-1"){
//                l.d("result == -1 ::: select memo is deleted")
                toast(resources.getString(R.string.deleted_memo))
                SignalingSendData.selectMemoListTotalCount(
                    dataUser?.userId!!,
                    dataUser?.enSeq!!,
                    dataUser?.sort!!
                ){ resultTotalCount ->
                    onSelectMemoListTotalCount(resultTotalCount)
                }
            }else{
                start<InputMemoActivity> {
                    putExtra(this, memoTableData)
                }
                finish()
            }
        }

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


    override fun onDestroy() {
        super.onDestroy()
//        l.d("onDestroy")
        SftpUploadManager.saveUploadingInfo(this)
        SignalingSendData.removeObserveDbDataNotFound(this)
        connectionStateMonitor.disable()
        powerStoreManager.unregister()

    }


    private var powerStoreManager = PowerStoreManager(
        this
    ) { _, o ->
        val data = o as PowerReceiverData
//        l.d("Noah powerManager onUpdateMessage!! " + data.messageId)
        data.print()

        // onDestroy(); 여기는 종료 전에 해야될 사용함수
        onDestroy()
        if (data.messageId == POWERDEFINE.MESSAGE_ID.PROCESS_KILL.value()) {
            finishAndRemoveTask()
            Process.killProcess(Process.myPid())
        }
    }


    // sftp서비스를 바인드 하여 sftp기능을 사용 가능하게 한다.
    fun connectBindService(){
        val intent = Intent(this, SftpService::class.java)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val localBinder = service as SftpService.LocalBinder
            bindService = localBinder.getService()
//            l.d("onServiceConnected : =============")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
//            l.d("onServiceDisconnected")
            toast("서비스 연결 해제")
        }
    }

    override fun onResume() {
        super.onResume()
        launch(Dispatchers.Default) {
            delay(1000)
            tiltScrollController?.requestAllSensors()
        }
//        if(Build.MODEL == "T1100S") {
//            moveUsingGyro?.requestAllSensor()
//        }
    }

    override fun onPause() {
        super.onPause()
//        if(Build.MODEL == "T1100S"){
//            moveUsingGyro?.stop()
//        }
        tiltScrollController?.releaseAllSensors()
    }


    override fun onBackPressed() {
        //super.onBackPressed()
    }




    private fun dp2Px(dp: Float):Int{
        val scale:Float = resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }



    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            TedPermission.with(this)
                    .setPermissionListener(permissionListener)
                    .setPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_MEDIA_LOCATION
                    )
                    .check()
        }else{
            TedPermission.with(this)
                    .setPermissionListener(permissionListener)
                    .setPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                    )
                    .check()
        }

    }



    private var permissionListener: PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            Log.d("MainActivity", "onPermissionGranted:::::::: ")
            SignalingSendData.signalingSendData {
//                l.d("EVENT_SOCKET_CONNECTION_OK::::::::connect ")

                connectBindService()
//                SignalingSendData.selectMemoSeqs(dataUser?.enSeq!!){
//                    val memoSeqs = ArrayList<Int>()
//                    memoSeqs.addAll((it as ArrayList<*>).filterIsInstance<Int>() as ArrayList<Int>)
//                    for(element in memoSeqs){
//                        l.d("element : $element")
//                    }
//                }

                launch(Dispatchers.Default) {
                    delay(500)
//                    l.d("dataUser userId:${dataUser?.userId}, enseq:${dataUser?.enSeq!!}, socketUrl:${SignalingSendData.sftpData?.socket_url}")
                    val userId = dataUser?.userId ?: return@launch
                    SignalingSendData.selectMemoListTotalCount(
                        userId,
                        dataUser?.enSeq!!,
                        dataUser?.sort!!
                    ){ result ->
                        onSelectMemoListTotalCount(result)
                    }
                }
            }
        }

        override fun onPermissionDenied(deniedPermissions: java.util.ArrayList<String?>?) {
            Log.d("MainActivity", "onPermissionDenied:::::::: ")
            toast("권한이 허용되지 않으면 앱을 실행 할 수 없습니다.")
            finish()
        }
    }


    // 현재 페이지 기준으로 해당 페이지의 메모 5개를 가져온다.
    @SuppressLint("SetTextI18n")
    private fun onSelectMemoListTotalCount(result: Any?){
        var orderBy = "asc"
        if (dataUser?.sort == 2 || dataUser?.sort == 4)
            orderBy = "desc"

        val count = result.toString().toInt()
        var totalPage = count / 5
        val lastPage = count % 5
//        l.d("EVENT_MEMO_LIST_TOTAL_COUNT_QUERY: $count")

        removeCommand()
        //commandSelectMemo(resources.getString(R.string.select))

        if(totalPage == 0){
            binding.tvNowPage.text = "1"
            dataUser?.page = 1
            totalPage = 1
        }

        if(count >5){
            if(lastPage > 0)
                totalPage += 1
            binding.tvMovePage.text = "1 - $totalPage"

            if(Locale.getDefault() == Locale.KOREA){
                for(i in 1..totalPage){
                    commandSelectPage(i.toString() + resources.getString(R.string.go_to_page), i)
//                    l.d("pageNum : $i")
                }
            }else{
                for(i in 1..totalPage){
                    commandSelectPage(resources.getString(R.string.go_to_page) + i.toString(), i)
//                    l.d("pageNum : $i")
                }
            }
        }else{
            if(Locale.getDefault() == Locale.KOREA){
                binding.tvMovePage.text = "1"
                commandSelectPage("1 ${resources.getString(R.string.go_to_page)}", 1)
            }else{
                binding.tvMovePage.text = "1"
                commandSelectPage("${resources.getString(R.string.go_to_page)} 1", 1)
            }
        }

        val page = binding.tvNowPage.text.toString()
        if(page.toInt() > totalPage){
//            l.d("page : $totalPage")
//            l.d("count : $count")
            binding.tvNowPage.text = totalPage.toString()
            dataUser?.page = totalPage
            SignalingSendData.selectMemoList(
                (totalPage * 5) - 5,
                5,
                dataUser?.userId!!,
                dataUser?.enSeq!!,
                orderBy,
                dataUser?.sort!!
            ){
                handleSelectMemoList(it)
            }
        }else{
            SignalingSendData.selectMemoList(
                page.toInt() * 5 - 5,
                5,
                dataUser?.userId!!,
                dataUser?.enSeq!!,
                orderBy,
                dataUser?.sort!!
            ){
                handleSelectMemoList(it)
            }
        }
    }


    private fun handleSelectMemoList(result: Any?){
        mMemoDataList.clear()
        mMemoDataList.addAll((result as ArrayList<*>).filterIsInstance<MemoTblData>() as ArrayList<MemoTblData>)
        binding.ivSelectBox.visibility = View.VISIBLE
        if (display?.widthPixels == 1280 && display?.heightPixels == 720){
            var params = binding.ivSelectBox.getLayoutParams() as ViewGroup.LayoutParams
            params.width = 748
            params.height = 450
            binding.ivSelectBox.setLayoutParams(params)
        }
//        else {
//            var params = binding.ivSelectBox.getLayoutParams() as ViewGroup.LayoutParams
//            params.width = 510
//            params.height = 310
//            binding.ivSelectBox.setLayoutParams(params)
//        }

        binding.tvSelect.visibility = View.VISIBLE
        memoDataDrawing()
//        l.d("EVENT_MEMO_LIST_QUERY")
    }



    private fun dbDataNotFound(){
        toast(R.string.data_not_found)
        binding.ivSelectBox.visibility = View.GONE
        binding.tvSelect.visibility = View.GONE
        mMemoDataList.clear()
        memoDataDrawing()
    }


    // 가져온 메모를 화면에 표시
    private fun memoDataDrawing() {
        val recyclerViewAdapter = RecyclerViewAdapter(this, mMemoDataList){ memoSeq, enSeq, hqSeq, brSeq, userId, userName, memoContents, saveTime, position->
            binding.recycler.smoothScrollToPosition(position)
            start<InputMemoActivity> {
                val memoTableData = MemoTableData(
                    memoSeq,
                    enSeq,
                    hqSeq,
                    brSeq,
                    userId,
                    userName,
                    memoContents,
                    saveTime
                )
                putExtra(this, memoTableData)
            }
            finish()
        }
        recyclerViewAdapter.notifyDataSetChanged()
        binding.recycler.smoothScrollToPosition(mMemoDataList.size / 2)
        binding.recycler.adapter = recyclerViewAdapter
    }



    private fun commandSelectMemo(name: String){
        val createTextView = CreateTextView(this)
        createTextView.setText(name){ view ->
//            l.d("command select tag : ${view.tag}")
            start<InputMemoActivity> {
                scrollZoomLayoutManager?.currentPosition?.let{
                    val memoTableData = MemoTableData(
                        mMemoDataList[it].dbMemoSeq ?: 0,
                        mMemoDataList[it].dbEnSeq,
                        mMemoDataList[it].dbHqSeq,
                        mMemoDataList[it].dbBrSeq,
                        mMemoDataList[it].dbUserID ?: "",
                        mMemoDataList[it].dbUserName ?: "",
                        mMemoDataList[it].dbMemoContents ?: "",
                        mMemoDataList[it].dbSaveTime ?: 0
                    )
                    putExtra(this, memoTableData)
                }
            }
            finish()
        }
        binding.llCommand.addView(createTextView)
    }

    // 페이지 이동 음성명령 등록
    private fun commandSelectPage(name: String, tag: Int){
        val createTextView = CreateTextView(this)
        createTextView.setTag(tag)
        createTextView.setText(name){ view ->
//            l.d(".commands: " + view.tag)
            if(view.tag != null){
                var orderBy = "asc"
                if(dataUser?.sort == 2 || dataUser?.sort == 4)
                    orderBy = "desc"
                SignalingSendData.selectMemoList(
                    view.tag.toString().toInt() * 5 - 5,
                    5,
                    dataUser?.userId!!,
                    dataUser?.enSeq!!,
                    orderBy,
                    dataUser?.sort!!
                ){ result ->
                    handleSelectMemoList(result)
                }
                binding.tvNowPage.text = view.tag.toString()
                dataUser?.page = view.tag.toString().toInt()
            }
        }
        binding.llCommand.addView(createTextView)
    }


    private fun removeCommand(){
        val createTextView = CreateTextView(this)
        createTextView.removeAllViews()
        binding.llCommand.removeAllViews()
    }


    private fun showSortDialog(){
        if(sortDialog == null){
            sortDialog = SortDialog(this){
                dataUser?.sort = it
                when(it){
                    1 -> binding.tvSortContents.text = resources.getString(R.string.all_memo_asc)
                    2 -> binding.tvSortContents.text = resources.getString(R.string.all_memo_desc)
                    3 -> binding.tvSortContents.text = resources.getString(R.string.my_memo_asc)
                    4 -> binding.tvSortContents.text = resources.getString(R.string.my_memo_desc)
                }
                SignalingSendData.selectMemoListTotalCount(
                    dataUser?.userId!!,
                    dataUser?.enSeq!!,
                    dataUser?.sort!!
                ){ result ->
                    onSelectMemoListTotalCount(result)
                }
            }
            val window = sortDialog?.window
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        sortDialog?.let{ dialog ->
            dialog.setOnDismissListener {
                sortDialog = null
//                l.d("onDismiss")
            }
            if(!dialog.isShowing)
                dialog.show()
        }
    }


}