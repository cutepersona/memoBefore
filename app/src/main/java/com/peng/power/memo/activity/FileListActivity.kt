package com.peng.power.memo.activity

import android.annotation.SuppressLint
import android.app.ProgressDialog.STYLE_HORIZONTAL
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Process
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.PagerSnapHelper
import com.peng.plant.powerreceiver.POWERDEFINE
import com.peng.plant.powerreceiver.PowerReceiverData
import com.peng.plant.powerreceiver.PowerStoreManager
import com.peng.power.memo.R
import com.peng.power.memo.adapter.FileRecyclerViewAdapter
import com.peng.power.memo.broadcastreceiver.ConnectionStateMonitor
import com.peng.power.memo.data.*
import com.peng.power.memo.databinding.ActivityFileListBinding
import com.peng.power.memo.dialog.DeleteDialog
import com.peng.power.memo.dialog.EditFileDialog
import com.peng.power.memo.preference.DataUser
import com.peng.power.memo.preference.PreferenceValue
import com.peng.power.memo.sftp.SftpService
import com.peng.power.memo.sftp.SftpProgressDialog
import com.peng.power.memo.sftp.SftpUploadManager
import com.peng.power.memo.util.l
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.activities.start
import splitties.toast.toast
import java.io.File
import java.util.*
import kotlin.math.abs

class FileListActivity : BaseActivity<ActivityFileListBinding>() {

    private var connectionStateMonitor = ConnectionStateMonitor()

    private val fileDataList = ArrayList<FileTblData>()

    private val filePath = (SignalingSendData.getRootPath()
            + SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER)

    private var deleteFileName = ""
    private var deleteThumbnailFileName = ""

    private val sampleMimeType = "image/jpeg"


    private var tiltScrollController: TiltScrollController? = null
    var recyclerViewAdapter: FileRecyclerViewAdapter? = null

    var scrollZoomLayoutManager: ScrollZoomLayoutManager? = null
    var bindService: SftpService? = null

    private lateinit var currentMemoTableData: MemoTableData

//    private var memoSeq = 0
//    private var enSeq = -1
//    private var hqSeq = -1
//    private var brSeq = -1
//    private var userID: String? = null
//    private var userName: String? = null
//    private var memoContents: String? = null
//    private var saveTime: String? = null

    private lateinit var editFileDialog:EditFileDialog
    private lateinit var deleteDialog:DeleteDialog
    private var progressDialog:SftpProgressDialog?=null

    private var dataUser: DataUser? = null
    //private var dataUpload: DataUpload?=null

    var display: DisplayMetrics? = null

    override fun initViewBinding() {
        binding = ActivityFileListBinding.inflate(layoutInflater)
    }

    override fun initAfterBinding() {
        connectionStateMonitor.enable(this)
        powerStoreManager.register()

        // 2023-02-06    Navigate520으로 인해 DP로 구분짓기 위함
        display = this.applicationContext?.resources?.displayMetrics

        connectBindService()

        SignalingSendData.addObserveDbDataNotFound(this) {
            dbDataNotFound()
        }

        dataUser = PreferenceValue.getDataUser(this)
        //dataUpload = PreferenceValue.getDataUpload(this)

        initUI()

        setEmergencyCall()

    }

    private fun dbDataNotFound(){
        toast(R.string.data_not_found)
        fileDataList.clear()
        binding.ivSelectBox.visibility = View.GONE
        binding.tvSelect.visibility = View.GONE
        fileDataDrawing()
    }


    private var powerStoreManager = PowerStoreManager(
        this
    ) { _, o ->
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


    private fun connectBindService() {
        // intent 객체, 서비스와 연결에 대한 정의
        val intent = Intent(baseContext, SftpService::class.java)
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

    private fun dp2px(dp: Float): Int {
        val scale = resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    override fun onResume() {
        super.onResume()
        launch(Dispatchers.Default) {
            delay(1000)
            tiltScrollController?.requestAllSensors()
        }
    }

    override fun onPause() {
        super.onPause()
        tiltScrollController?.releaseAllSensors()
    }

    override fun onDestroy() {
        super.onDestroy()
        SftpUploadManager.saveUploadingInfo(this)
        SignalingSendData.removeObserveDbDataNotFound(this)
        connectionStateMonitor.disable()
        powerStoreManager.unregister()
    }

    override fun onBackPressed() {
        //super.onBackPressed()
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

    private fun initUI(){
        editFileDialog = EditFileDialog(this)
        deleteDialog = DeleteDialog(this)

        currentMemoTableData = getMemoTableDataFromJsonString(intent)

//        memoSeq = intent.getIntExtra("memo_seq", 0)
//        enSeq = intent.getIntExtra("en_seq", -1)
//        userID = intent.getStringExtra("user_id")
//        userName = intent.getStringExtra("user_name")
//        memoContents = intent.getStringExtra("memo_contents")
//        saveTime = intent.getStringExtra("save_time")

        scrollZoomLayoutManager = ScrollZoomLayoutManager(this, dp2px(30f))
        binding.recycler.layoutManager = scrollZoomLayoutManager

        val pagerSnapHelper = PagerSnapHelper()
        pagerSnapHelper.attachToRecyclerView(binding.recycler)

        tiltScrollController = TiltScrollController(this){ x, y, deltaX ->
            //   기존 글라스용 감도
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
//            Log.d("Noah", " abs(deltaX) " +  abs(deltaX))
            if (binding.recycler.adapter != null) {
                when {
                    abs(deltaX) > 1.8 -> {
                        binding.recycler.smoothScrollBy(
                            (x * binding.recycler.adapter!!.itemCount / 0.21 ).toInt(),
                            0
                        )
                    }
                    abs(deltaX) > 1.4 -> {
                        binding.recycler.smoothScrollBy(
                            (x * binding.recycler.adapter!!.itemCount / 0.26 ).toInt(),
                            0
                        )
                    }
                    abs(deltaX) > 1.0 -> {
                        binding.recycler.smoothScrollBy(
                            (x * binding.recycler.adapter!!.itemCount / 0.36 ).toInt(),
                            0
                        )
                    }
                    abs(deltaX) > 0.6 -> {
                        binding.recycler.smoothScrollBy(
                            (x * binding.recycler.adapter!!.itemCount / 0.46 ).toInt(),
                            0
                        )
                    }
                    else -> binding.recycler.smoothScrollBy(
                        x * binding.recycler.adapter!!.itemCount / 3000,
                        0
                    )
                }
            }

        }

        if(Locale.getDefault() == Locale.KOREA){
            binding.tvEnPage.visibility = View.GONE
            binding.tvKrPage.visibility = View.VISIBLE
        }else{
            binding.tvEnPage.visibility = View.VISIBLE
            binding.tvKrPage.visibility = View.GONE
        }

        binding.tvBack.setOnClickListener {
            start<InputMemoActivity> {
                putExtra(this, currentMemoTableData)
            }
            finish()
        }

        binding.tvSelect.setOnClickListener {
            l.d("onClick tvSelect")

            scrollZoomLayoutManager?.let{

                val curPosition = it.currentPosition
                l.d("curposition : $curPosition")
                showEditDialog(curPosition + 1, fileDataList[curPosition])
            }
        }

        // 2023-02-06   Navigate520
        if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
            Log.d("Noah", " Navigate520 FileListActivity")
            binding.tvBack.setTextSize(23f)
            binding.tvFileListTitle.setTextSize(27f)
            binding.tvNowPageTitle.setTextSize(23f)
            binding.tvNowPage.setTextSize(23f)
            binding.tvEnPage.setTextSize(23f)
            binding.tvMovePage.setTextSize(23f)
            binding.tvKrPage.setTextSize(23f)
            binding.tvSelect.setTextSize(23f)
        }


        showSavingDialog()

    }




//    private fun commandSelectFile(name: String){
//        val createTextView = CreateTextView(this)
//        createTextView.setText(name){ view->
//            l.d("command select file tag : ${view.tag}")
//            scrollZoomLayoutManager?.let{
//                val curPosition = it.currentPosition
//                showEditDialog(curPosition + 1, fileDataList[curPosition])
//            }
//        }
//    }

    private fun commandSelectPage(name: String, tag: Int){
        val createTextView = CreateTextView(this)
        createTextView.setTag(tag)
        createTextView.setText(name){ view->
            binding.tvNowPage.text = view.tag.toString()
            SignalingSendData.selectFileList(
                currentMemoTableData.memo_seq,
                view.tag.toString().toInt() * 5 - 5,
                5
            ){ resultArray->
                l.d("EVENT_FILE_LIST_QUERY")
                val array = (resultArray as ArrayList<*>).filterIsInstance<FileTblData>() as ArrayList<FileTblData>
                handleSelectFileList(array)
            }
        }
        binding.llCommand.addView(createTextView)
    }


    private fun removeCommand(){
        val createTextView = CreateTextView(this)
        createTextView.removeAllViews()
        binding.llCommand.removeAllViews()
    }


    @SuppressLint("SetTextI18n")
    private fun handleFileListTotalCount(count: Int){
        launch {
            l.d("EVENT_FILE_LIST_TOTAL_COUNT_QUERY: $count")
            var totalPage = count / 5
            val lastPage = count % 5

            removeCommand()

            //commandSelectFile(resources.getString(R.string.select))

            if (totalPage == 0) {
                binding.tvNowPage.text = "1"
                totalPage = 1
            }

            if (count > 5) {
                if (lastPage == 0) {
                    binding.tvMovePage.text = "1 - $totalPage"
                } else {
                    totalPage += 1
                    binding.tvMovePage.text = "1 - $totalPage"
                }

                if(Locale.getDefault() == Locale.KOREA){
                    for (i in 1..totalPage) {
                        commandSelectPage(
                            i.toString() + resources.getString(R.string.go_to_page),
                            i
                        )
                        l.d("handleMessage: pageNum$i")
                    }
                }else{
                    for (i in 1..totalPage) {
                        commandSelectPage(resources.getString(R.string.go_to_page) + i, i)
                        l.d("handleMessage: pageNum$i")
                    }
                }
            } else {
                if (Locale.getDefault() == Locale.KOREA) {
                    binding.tvMovePage.text = "1"
                    commandSelectPage("1" + resources.getString(R.string.go_to_page), 1)
                } else {
                    binding.tvMovePage.text = "1"
                    commandSelectPage(resources.getString(R.string.go_to_page) + 1, 1)
                }
            }


            val page: String = binding.tvNowPage.text.toString()
            if (page.toInt() > totalPage) {
                l.d("handleMessage: page::::::$totalPage")
                l.d("handleMessage: count::::::$count")
                binding.tvNowPage.text = totalPage.toString()
                SignalingSendData.selectFileList(
                    currentMemoTableData.memo_seq,
                    totalPage * 5 - 5,
                    5
                ){ resultArray ->
                    l.d("EVENT_FILE_LIST_QUERY")
                    val array = (resultArray as ArrayList<*>).filterIsInstance<FileTblData>() as ArrayList<FileTblData>
                    handleSelectFileList(array)
                }
            } else {
                l.d("handleMessage: page:1111:::::$totalPage")
                SignalingSendData.selectFileList(
                    currentMemoTableData.memo_seq,
                    page.toInt() * 5 - 5,
                    5
                ){ resultArray->
                    l.d("EVENT_FILE_LIST_QUERY")
                    val array = (resultArray as ArrayList<*>).filterIsInstance<FileTblData>() as ArrayList<FileTblData>
                    handleSelectFileList(array)
                }
            }
        }

    }


    private fun handleSelectFileList(resultArray: ArrayList<FileTblData>){
        fileDataList.clear()
        fileDataList.addAll(resultArray)
        getServerFile()

        binding.ivSelectBox.visibility = View.VISIBLE
        binding.tvSelect.visibility = View.VISIBLE

        if (display?.widthPixels == 1280 && display?.heightPixels == 720){
            var params = binding.ivSelectBox.getLayoutParams() as ViewGroup.LayoutParams
            params.width = 748
            params.height = 450
            binding.ivSelectBox.setLayoutParams(params)
        }

        fileDataDrawing()
        l.d("handleMessage : EVENT_PICTURE_LIST_QUERY")
    }


    private var jobCheckFileSaving: Job?=null

    private fun selectFileListTotalCount(memoSeq: Int){
        SignalingSendData.selectFileListTotalCount(memoSeq){
            handleFileListTotalCount(it.toString().toInt())
        }
    }



    private fun showSavingDialog() {

        selectFileListTotalCount(currentMemoTableData.memo_seq)

        progressDialog?.dismiss()
        jobCheckFileSaving?.cancel()

//        l.d("showSavingDialog countupload:${dataUpload?.countUpload}, completeupload:${dataUpload?.countUploadComplete}")
//        if(dataUpload?.countUpload == dataUpload?.countUploadComplete){
//            SignalingSendData.selectFileListTotalCount(currentMemoTableData.memo_seq){
//                handleFileListTotalCount(it.toString().toInt())
//            }
//            progressDialog?.dismiss()
//            jobCheckFileSaving?.cancel()
//        }else{
//            progressDialog?.let{
//                it.setMessage(resources.getString(R.string.gallery_saving_check))
//                it.setProgressStyle(android.R.style.Widget_ProgressBar_Horizontal)
//                it.setCancelable(true)
//                it.show()
//            }
//            jobCheckFileSaving = launch {
//                delay(3000)
//                showSavingDialog()
//            }
//        }
    }

    private fun getServerFile(){
        for (saveFile in fileDataList.indices) {
            val thumbNailPath = (SignalingSendData.getRootPath()
                    + SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER + fileDataList.get(
                saveFile
            ).dbThumbnail_name)
            l.d("getServerFile: ::::::::::::" + fileDataList.get(saveFile).dbThumbnail_name)
            val imgFile = File(thumbNailPath)
            if (!imgFile.exists()) {
                l.d("imgFile not exists")
                fileDataList.get(saveFile).dbThumbnail_name?.let{
                    bindService?.sftpConnect?.downloadThumbnailFile(it){
                        l.e("download thumbnail file callback")
                        listSetRefresh()
                    }
                }
            }
        }
    }

    private fun fileDataDrawing(){
        recyclerViewAdapter = FileRecyclerViewAdapter(this, fileDataList){ position, fileTblData ->
            showEditDialog(position, fileTblData)
        }
        recyclerViewAdapter?.notifyDataSetChanged()
        binding.recycler.smoothScrollToPosition(fileDataList.size / 2)
        binding.recycler.adapter = recyclerViewAdapter
    }

    private fun listSetRefresh(){
        binding.recycler.setHasFixedSize(true)
        recyclerViewAdapter?.notifyDataSetChanged()
    }




    private fun showEditDialog(position: Int, fileTblData: FileTblData){

        if(!editFileDialog.isShowing)
            editFileDialog.showDialog(position){
                when(it){
                    0 -> { // show document
                        if (SignalingSendData.sftpData?.sftp_url.isNullOrEmpty()) {
                            toast(resources.getString(R.string.invalid_sftp_url))
                        }else{
                            val url =
                                SignalingSendData.sftpData?.sftp_url + SignalingSendData.EVENT_SFTP_CREATE_ROOT_FOLDER + SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER + fileTblData.dbOriginal_name
                            l.d("show video on url : $url")
                            fileTblData.dbOriginal_name?.let { fileName ->
                                if (fileName.contains(".mp4")) {
//                                    start<VideoPlayActivity> {
//                                        putExtra("url", url)
//                                    }
                                    start<ExoVideoPlayActivity>{
                                        putExtra("url",url)
                                    }

                                } else {
                                    val type = SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER
                                    val progressDialog = SftpProgressDialog(this, 0)
                                    progressDialog.isIndeterminate = false
                                    progressDialog.setProgressStyle(STYLE_HORIZONTAL)
                                    progressDialog.show()
                                    bindService?.sftpConnect?.downloadFile(
                                        fileName,
                                        type,
                                        progressDialog
                                    ) { path ->
                                        if (path == "null") {
                                            editFileDialog.dismiss()
                                            toast(resources.getString(R.string.deleted_file))
                                            selectFileListTotalCount(currentMemoTableData.memo_seq)
                                            return@downloadFile
                                        }
                                        val fileRootPath = filePath + path
                                        androidVersionODocument(fileRootPath)
                                    }
                                }
                            }
                        }




//                        l.d("onFileSelect")
//                        val fileRoot: String = filePath + fileTblData.dbOriginal_name
//                        l.d("onPictureSelect : $fileRoot")
//                        val file = File(fileRoot)
//                        if (file.exists()) {
//                            androidVersionODocument(fileRoot)
//                        } else {
//                            val type = SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER
//                            val progressDialog = SftpProgressDialog(this, 0)
//                            progressDialog.isIndeterminate = false
//                            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
//                            progressDialog.show()
//                            bindService?.sftpConnect?.downloadFile(
//                                fileTblData.dbOriginal_name!!,
//                                type,
//                                progressDialog
//                            ) { path ->
//                                val fileRootPath = filePath + path
//                                androidVersionODocument(fileRootPath)
//                            }
//                        }
                    }
                    1 -> { // delete
                        l.d("onDeleteConfirm")
                        if (currentMemoTableData.user_id == dataUser?.userId) {
                            deleteFileName = fileTblData.dbOriginal_name!!
                            deleteThumbnailFileName = fileTblData.dbThumbnail_name!!
                            if (!deleteDialog.isShowing) {
                                deleteDialog.showDialog(
                                    fileTblData.dbFile_seq,
                                    deleteFileName,
                                    deleteThumbnailFileName
                                ) {
                                    val deleteOriginalFile = filePath + deleteFileName
                                    val originalFile = File(deleteOriginalFile)
                                    if (originalFile.exists()) originalFile.delete()
                                    val deleteThumbFile = SignalingSendData.getRootPath() + SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER + deleteThumbnailFileName
                                    val thumbnailFile = File(deleteThumbFile)
                                    if (thumbnailFile.exists()) thumbnailFile.delete()
                                    SignalingSendData.sendDeleteQuery(fileTblData.dbFile_seq) { deleteResult ->
                                        l.d("EVENT_DELETE_DATA_RESULT")
                                        when (deleteResult.toString().toInt()) {
                                            -1 -> {
                                                toast(resources.getString(R.string.delete_error))
                                            }
                                            0 -> {
                                                toast(resources.getString(R.string.fail))
                                            }
                                            1 -> {
                                                sftpOriginalFileDelete()
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            toast(resources.getString(R.string.user_not_match))
                        }
                    }
                }
            }
    }






    private fun androidVersionODocument(filename: String?) {
        val readFile = File(filename)
        if (readFile == null || !readFile.isFile) {
            return
        }
        val uri = FileProvider.getUriForFile(
            this,
            "com.peng.power.memo.fileprovider",
            readFile
        )
        l.e("uri path : $uri")
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
        Log.d("onLaunchDocument80", "mimeType: $mimeType")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(uri, mimeType)
        intent.putExtra("page", "1") // Open a specific page
        intent.putExtra("zoom", "1") // Open at a specific zoom level
        startActivity(intent)
    }


    private fun sftpOriginalFileDelete(){
        val type = SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER
        bindService?.sftpConnect?.deleteFile(deleteFileName, type){
            l.d("EVENT_DELETE_ORIGINAL_FILE_OK")
            sftpThumbFileDelete()
        }
    }

    private fun sftpThumbFileDelete(){
        val type = SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER //"/Thumbnail/";
        bindService?.sftpConnect?.deleteFile(deleteThumbnailFileName, type){
            l.d("EVENT_DELETE_THUMBNAIL_FILE_OK")
            deleteDialog.dismiss()
            editFileDialog.dismiss()
            SignalingSendData.selectFileListTotalCount(currentMemoTableData.memo_seq){
                handleFileListTotalCount(it.toString().toInt())
            }
        }
    }


}