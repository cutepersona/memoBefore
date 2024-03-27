package com.peng.power.memo

import android.util.Log
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.peng.power.memo.coroutine.BaseCoroutineScope
import com.peng.power.memo.coroutine.ClassCoroutineScope
import com.peng.power.memo.data.ConvertDate
import com.peng.power.memo.util.l
import com.peng.power.memo.data.FileTblData
import com.peng.power.memo.data.MemoTblData
import com.peng.power.memo.data.SignalingSendData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.*

// singleton
object SocketIO : BaseCoroutineScope by ClassCoroutineScope() {


    private var mSocket:Socket?=null

    private val CONNECTION_TIMEOUT = (30 * 1000).toLong()
    private val RECONNECTION_ATTEMPT = 0

    private const val selectQuery = "selectQuery"
    private const val insertQuery = "insertQuery"
    private const val deleteQuery = "deleteQuery"
    private const val updateQuery = "updateQuery"

    private val gson = Gson()

    lateinit var callbackSocketConnectComplete: () -> Unit

    private val callbackPool = HashMap<String, (Any?)->Unit>()

    private var callbackDbDataNotFound:(()->Unit)? = null

    private fun getCallbackFun(cmd:String):((Any?)->Unit)?{
        return if(callbackPool.containsKey(cmd)){
            val callback:(Any?)->Unit = callbackPool[cmd]!!
            callbackPool.remove(cmd)
            callback
        }else{
            null
        }
    }

    fun registObserveDbDataNotFound(listener:()->Unit){
        callbackDbDataNotFound = listener
    }

    // Query 보내는 명령이 들어올때 cmd를 key값으로 callback을 hashMap에 저장해 놓고
    // 해당 query에 대한 응답이 들어오면 cmd key값으로 callback을 찾아서 보낸다.
    private fun addCallbackFun(cmd:String, callback:(Any?)->Unit){
        callbackPool[cmd] = callback
    }

    fun initSocketIoClient(callbackSocketConnectComplete: () -> Unit){
        this.callbackSocketConnectComplete = callbackSocketConnectComplete

        Log.d("Noah ", " ---- initSocketIoClient ---- $mSocket")

        if(mSocket != null){
            callbackSocketConnectComplete()
            return
        }

        try{
            //String CONNECT_ULR = "http://powertalk.co.kr:9500"; //내부 데모용 파워메모
            //해당 옵션은 Https 사용시 적용되는 요소이다.
            val opts: IO.Options = IO.Options() // server url : 45.119.147.108:8080

            opts.timeout = CONNECTION_TIMEOUT
            opts.reconnection = true
            opts.reconnectionAttempts = RECONNECTION_ATTEMPT
            opts.multiplex = false
            opts.reconnectionDelay = 1000
            opts.forceNew = true
            opts.secure = false

            mSocket = IO.socket(SignalingSendData.sftpData?.socket_url, opts)
            Log.d("Noah ", " ---- initSocketIoClient ---- $mSocket")
            Log.d("Noah ", " ---- initSocketIoClient ---- ${SignalingSendData.sftpData?.socket_url}")
            l.d("InitSocketIOClient: :::" + SignalingSendData.sftpData?.socket_url)
            registerConnectionAttributes()

            l.d("InitSocketIOClient : IO.socket is option binding")
            Log.d("Noah ", " ---- initSocketIoClient ---- ${mSocket?.connected()}")
            mSocket?.let{
                if(!it.connected()){
                    it.connect()
                    Log.d("Noah ", " ---- initSocketIoClient ---- ${mSocket?.connected()}")
                }
            }
        }catch (e: URISyntaxException){
            e.printStackTrace()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }


    fun socketDisconnected(){
        try{
            l.d("SocketDisconnected")
            registerDisConnectionAttributes()
            mSocket?.disconnect()
            mSocket = null
        }catch (e: Exception){
            e.printStackTrace()
        }
    }


    private fun registerConnectionAttributes() {
        try {
            if (mSocket != null) {
                mSocket!!.on(Socket.EVENT_CONNECT){
                    l.d("onConnect to Signaling Server")
                    callbackSocketConnectComplete()
                }
                mSocket!!.on(Socket.EVENT_CONNECT_ERROR){
                    for(element in it){
                        l.d("onConnectError $element")
                    }

                }
                mSocket!!.on(Socket.EVENT_CONNECT_TIMEOUT){
                    l.d("onConnectTimeOut : $it")
                }


                //User Event
                mSocket!!.on(selectQuery){
                    try {
                        val obj = JSONObject(it.get(0).toString())
                        val cmd = obj.getString("cmd")
                        val array = obj.getJSONArray("result")
                        l.d("cmd : $cmd, data : ${array.length()}")
                        socketResult(cmd, array)
                    } catch (e: java.lang.Exception) {
                        l.d("onSelectQuery:$e")
                    }
                }


                mSocket!!.on(insertQuery){
                    getCmdAndResult(it[0].toString()){ cmd, result ->
                        sendCallback(cmd, result)
                    }
                }

                mSocket!!.on(updateQuery){
                    getCmdAndResult(it[0].toString()){ cmd, result ->
                        sendCallback(cmd, result)
                    }
                }

                mSocket!!.on(deleteQuery){
                    getCmdAndResult(it[0].toString()){ cmd, result ->
                        sendCallback(cmd, result)
                    }
                }
            }
        } catch (e: Exception) {
            l.d("registerConnectionAttributes:$e")
        }
    }

    private fun getCmdAndResult(jsonString: String, callback: (String, String) -> Unit){
        try{
            val obj = JSONObject(jsonString)
            val cmd = obj.getString("cmd")
            val result = obj.getString("result")
            l.d("cmd : $cmd, result length : $result")
            callback(cmd, result)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun getJsonString(cmd: String, query: String):String{
        val jsonObject = JsonObject()
        jsonObject.addProperty("cmd", cmd)
        jsonObject.addProperty("query", query)
        return gson.toJson(jsonObject)
    }


    fun sendSelectQuery(
        cmd: String,
        queryContents: String,
        callback: (Any?) -> Unit
    ) {
        addCallbackFun(cmd, callback)
        mSocket?.emit(selectQuery, getJsonString(cmd, queryContents))
    }

    fun sendInsertQuery(
        cmd: String,
        queryContents: String,
        callback: (Any?) -> Unit
    ){
        addCallbackFun(cmd, callback)
        mSocket?.emit(insertQuery, getJsonString(cmd, queryContents))
    }

    fun sendUpdateQuery(
        cmd: String,
        queryContents: String,
        callback: (Any?) -> Unit
    ){
        addCallbackFun(cmd, callback)
        mSocket?.emit(updateQuery, getJsonString(cmd, queryContents))
    }



    fun sendDeleteQuery(
        cmd: String,
        queryContents: String,
        callback: (Any?) -> Unit
    ){
        addCallbackFun(cmd, callback)
        mSocket?.emit(deleteQuery, getJsonString(cmd, queryContents))
    }



    private fun registerDisConnectionAttributes() {
        try {
            if (mSocket != null) {
                mSocket!!.off(Socket.EVENT_CONNECT)
                mSocket!!.off(Socket.EVENT_CONNECT_ERROR)
                mSocket!!.off(Socket.EVENT_CONNECT_TIMEOUT)

                //User Event
                mSocket!!.off(selectQuery)
                mSocket!!.off(insertQuery)
                mSocket!!.off(deleteQuery)
                mSocket!!.off(updateQuery)
            }
        } catch (e: java.lang.Exception) {
            l.d("registerDisConnectionAttributes:$e")
        }
    }


    private fun sendCallback(cmd:String, dataForCallback:Any){
        val callbackFun = getCallbackFun(cmd)
        if(callbackFun != null){
            launch(Dispatchers.Main) {
                callbackFun(dataForCallback)
            }
        }
    }


    private fun socketResult(cmd: String, jsonArray: JSONArray){
        try{
            if(jsonArray.length() == 0){
                if(cmd == "memo_seq_query"){
                    l.d("not found memo seq where filename")
                    sendCallback(cmd, -1)
                    return
                }

                if(cmd == "user_id_query"){
                    l.d("not found user id where memo seq")
                    sendCallback(cmd, "-1")
                    return
                }

                //등록된 cmd에 대한 콜백 삭제해주기 위해 호출
                getCallbackFun(cmd)

                if(callbackDbDataNotFound == null){
                    l.e("socket io callbackdbdatanotfound null")
                }
                launch(Dispatchers.Main) {
                    callbackDbDataNotFound?.let { it() }
                }
                l.d("socketResult null")
            }else{
                when(cmd){
                    "memo_last_number_query" -> {
                        val jObj = JSONObject(jsonArray.getString(0))
                        val memoSeq = jObj.getInt("memo_seq")

                        l.d("socketResult: =========::::::$memoSeq")

                        sendCallback(cmd, memoSeq)
                    }
                    "user_id_query"->{
                        l.d("into user id query")
                        val jObj = JSONObject(jsonArray.getString(0))
                        l.json(jObj.toString())
                        val userId = jObj.getString("user_id")
                        sendCallback(cmd, userId)
                    }
                    "memo_seq_query"->{
                        l.d("into memo_seq_query")
                        val jObj = JSONObject(jsonArray.getString(0))
                        l.json(jObj.toString())
                        val memoSeq = jObj.getInt("memo_seq")
                        sendCallback(cmd, memoSeq)
                    }
                    "memo_list_query" -> {
                        val mMemoDataList: ArrayList<MemoTblData> = ArrayList<MemoTblData>()

                        for (i in 0 until jsonArray.length()) {
                            l.json(jsonArray.getString(i))

                            val jObj = JSONObject(jsonArray.getString(i))
                            val memoTblData = MemoTblData()
                            //                            memoTblData.dbSeq = jObj.getInt("seq");
                            memoTblData.dbMemoSeq = jObj.getInt("mt.memo_seq")
                            memoTblData.dbEnSeq =  jObj.optInt("mt.en_seq", -1)
                            memoTblData.dbHqSeq = jObj.optInt("mt.hq_seq", -1)
                            memoTblData.dbBrSeq = jObj.optInt("mt.br_seq", -1)
                            if(SignalingSendData.sftpData?.sftp_root_folder.equals("hanil/")){
                                Log.d("Noah ", "Noah ==== hanil ================================================= memo_list_query")
                                memoTblData.dbUserID = jObj.getString("ut.user_id")
                                memoTblData.dbUserName = jObj.getString("ut.user_name")
                            } else {
                                memoTblData.dbUserID = jObj.getString("ut.id")
                                memoTblData.dbUserName = jObj.getString("ut.name")
                            }
                            memoTblData.dbMemoContents = jObj.getString("mt.memo_contents")
                            memoTblData.dbSaveTime = jObj.optLong("mt.save_time", 0L)

                            if(memoTblData.dbSaveTime == 0L){
                                val saveTime = jObj.optString("mt.save_time", "")
                                if(saveTime.isNullOrEmpty()){
                                    l.e("saveTime is null or empty")
                                }
                                if(saveTime.isNotEmpty() && saveTime != "null"){
                                    memoTblData.dbSaveTime = ConvertDate.getUtcEpochSecondsFromLocalDateTime(saveTime)
                                    l.d("converted save time from local time string : ${memoTblData.dbSaveTime}")
                                }
                            }


                            val picture =
                                "(select count(memo_seq) from Powermemo_FileList where memo_seq = mt.memo_seq and file_type = 'P') as picture"
                            val video =
                                "(select count(memo_seq) from Powermemo_FileList where memo_seq = mt.memo_seq and file_type = 'V') as video"

                            memoTblData.dbPictureCount = jObj.getInt(picture)
                            memoTblData.dbVideoCount = jObj.getInt(video)

                            l.d("picture_list_query not null:::::" + memoTblData.dbMemoContents)
                            mMemoDataList.add(memoTblData)
                        }
                        sendCallback(cmd, mMemoDataList)
                    }
                    "file_list_query" -> {
                        val mFileDataList: ArrayList<FileTblData> = ArrayList<FileTblData>()

                        for (i in 0 until jsonArray.length()) {
                            val jObj = JSONObject(jsonArray.getString(i))
                            val fileTblData = FileTblData()
                            //                            memoTblData.dbSeq = jObj.getInt("seq");
                            fileTblData.dbFile_seq = jObj.getInt("file_seq")
                            fileTblData.dbMemo_seq = jObj.getInt("memo_seq")
                            fileTblData.dbFile_type = jObj.getString("file_type")
                            fileTblData.dbOriginal_name = jObj.getString("original_name")
                            fileTblData.dbThumbnail_name = jObj.getString("thumbnail_name")
                            l.d("file_list_query not null:::::")
                            mFileDataList.add(fileTblData)
                        }

                        sendCallback(cmd, mFileDataList)
                    }
                    "memo_list_total_count_query" -> {
                        val jObj = JSONObject(jsonArray.getString(0))
                        val totalCount = jObj.getInt("COUNT(*) as total")

                        sendCallback(cmd, totalCount)
                    }
                    "file_list_total_count_query" -> {
                        val jObj = JSONObject(jsonArray.getString(0))
                        val fileTotalCount = jObj.getInt("COUNT(*) as total")

                        sendCallback(cmd, fileTotalCount)
                    }
                    "delete_file_name_query" -> {
                        val arrOriginalList = ArrayList<String>()
                        val arrThumbnailList = ArrayList<String>()
                        for (i in 0 until jsonArray.length()) {
                            val jObj = JSONObject(jsonArray.getString(i))
                            arrOriginalList.add(jObj.getString("original_name"))
                            arrThumbnailList.add(jObj.getString("thumbnail_name"))
                        }

                        val lists = arrayListOf(arrOriginalList, arrThumbnailList)

                        sendCallback(cmd, lists)
                    }
                }
            }

        }catch (e: Exception){
            l.d("socketResult Error :: $e")
        }
    }




}