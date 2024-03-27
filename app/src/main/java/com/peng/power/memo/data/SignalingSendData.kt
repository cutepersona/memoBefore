package com.peng.power.memo.data

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import com.peng.power.memo.BuildConfig
import com.peng.power.memo.SocketIO
import com.peng.power.memo.util.l
import kotlinx.serialization.Serializable
import java.util.HashMap

// singleton
object SignalingSendData {

    private val socketIO = SocketIO

    enum class SftpStatus {
        EVENT_SFTP_CONNECT_OK,
        EVENT_SFTP_CONNECT_FAIL,
    }

    const val EVENT_SFTP_CREATE_ROOT_FOLDER = "PowerMemo/"
    const val EVENT_SFTP_CREATE_ORIGINAL_FOLDER = "Original/"
    const val EVENT_SFTP_CREATE_THUMBNAIL_FOLDER = "Thumbnail/"
    const val EVENT_CREATE_VIDEO_DATA_FOLDER = "VideoData/"
    const val EVENT_CREATE_TEMP_FOLDER = "Temp/"

    private var rootPath:String = ""

    fun initRootPath(context: Context){
        rootPath = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + "/" + EVENT_SFTP_CREATE_ROOT_FOLDER
        }else{
            (Environment.getExternalStorageDirectory().absolutePath + "/"
                    + EVENT_SFTP_CREATE_ROOT_FOLDER)
        }
        l.d("initRootPath : $rootPath")
    }

    fun getRootPath():String{
        if(rootPath.isEmpty()){
            rootPath = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                "/storage/emulated/0/Android/data/com.peng.power.memo/files/Pictures/$EVENT_SFTP_CREATE_ROOT_FOLDER"
            }else{
                (Environment.getExternalStorageDirectory().absolutePath + "/"
                        + EVENT_SFTP_CREATE_ROOT_FOLDER)
            }
            l.e("rootPath is empty --> alt root path : $rootPath")
        }
        return rootPath
    }


    @Serializable
    data class SftpData(var socket_url: String = "", var sftp_host: String = "", var sftp_user: String = ""
                        , var sftp_user_pw: String = "", var sftp_port: Int = -1, var sftp_root_folder: String = ""
                        , var sftp_url:String = "", var thermal:Int = -1)

    var sftpData:SftpData? =null


    fun signalingSendData(callback: () -> Unit){
        socketIO.initSocketIoClient(callback)
        // SocketIO 통신에서 해당 자료가 DB에 없을때의 콜백
        socketIO.registObserveDbDataNotFound {
            broadcastCallback()
        }
    }

    fun socketDisconnect(){
        socketIO.socketDisconnected()
    }


    private val callbackPool = HashMap<String, ()->Unit>()

    // callbackPool에 등록된 관찰자들(Activities)에게 알림을 준다.
    private fun broadcastCallback(){
        if(callbackPool.size >0){
            for(element in callbackPool){
                element.value()
            }
        }
    }
    

    fun addObserveDbDataNotFound(activity:Activity, listener: () -> Unit){
        if(callbackPool.containsKey(activity.localClassName))
            return
        else
            callbackPool[activity.localClassName] = listener
        l.d("regist callback in pool (localClassName : ${activity.localClassName} -- callbackpool size = ${callbackPool.size}")
    }

    fun removeObserveDbDataNotFound(activity:Activity){
        l.e("unregist db data not found - acitivity name : ${activity.localClassName}")
        if(callbackPool.containsKey(activity.localClassName)){
            callbackPool.remove(activity.localClassName)
        }
    }





    // 입력된 메모번호 중 제일 마지막 번호를 가져온다.
    fun selectLastNumberQuery(
            user_id: String,
            en_seq: Int,
            callback: (Any?) -> Unit
    ) {
        val cmd = "memo_last_number_query"
        var queryContents = "select top 1 {memo_seq} from Powermemo_MemoList where user_id = N'$user_id' order by memo_seq desc"
        if (en_seq >= 0) {
            queryContents = "select top 1 {memo_seq} from Powermemo_MemoList where user_id = N'$user_id' and en_seq = $en_seq order by memo_seq desc"
        }
        l.d("selectLastNumberQuery:::::$queryContents")

        socketIO.sendSelectQuery(cmd, queryContents, callback)
    }


    // 총 메모의 개수가 몇 개인지 가져온다.
    fun selectMemoListTotalCount(
            userId: String,
            enSeq: Int,
            sort: Int,
            callback: (Any?) -> Unit
    ) {
        var whereContentsWithID = "where mt.user_id = N'$userId' and mt.user_id != N'-'"
        var whereContents = ""
        if (enSeq >= 0) {
            whereContentsWithID = "where mt.user_id = N'$userId' and mt.en_seq = $enSeq and mt.user_id != N'-'"
            whereContents = "where mt.en_seq = $enSeq and mt.user_id != N'-'"
        }
        var queryContents = ""
        val cmd = "memo_list_total_count_query"
        //data 1 : 전체메모(오름차순), 2 : 전체메모(내림차순), 3 : 내메모(오름차순), 4 : 내메모(내림차순)

        Log.d("Noah ", "Noah ==== " + sftpData?.sftp_root_folder )

        if (sftpData?.sftp_root_folder.equals("hanil/") || sftpData?.sftp_root_folder.equals("sftpuser01/")){
            Log.d("Noah ", "Noah ==== hanil ================================================= selectMemoListTotalCount")
            if (sort == 1 || sort == 2) {
                queryContents = "select {COUNT(*) as total} from Powermemo_MemoList as mt INNER JOIN tblUser as ut ON mt.user_id = ut.user_id $whereContents"
            } else if (sort == 3 || sort == 4) {
                queryContents = "select {COUNT(*) as total} from Powermemo_MemoList as mt INNER JOIN tblUser as ut ON mt.user_id = ut.user_id $whereContentsWithID"
            }
        } else {
            if (sort == 1 || sort == 2) {
                queryContents = "select {COUNT(*) as total} from Powermemo_MemoList as mt INNER JOIN tblUser as ut ON mt.user_id = ut.id $whereContents"
            } else if (sort == 3 || sort == 4) {
                queryContents = "select {COUNT(*) as total} from Powermemo_MemoList as mt INNER JOIN tblUser as ut ON mt.user_id = ut.id $whereContentsWithID"
            }
        }

        l.d("memo_list_total_count_query:::::$queryContents")
        socketIO.sendSelectQuery(cmd, queryContents, callback)
    }


    // 해당 메모에 포함된 파일의 리스트를 가져온다.
    fun selectFileListTotalCount(
            memoSeq: Int,
            callback: (Any?) -> Unit
    ) {
        
        val cmd = "file_list_total_count_query"
        val queryContents =
            "select {COUNT(*) as total} from Powermemo_FileList where memo_seq = $memoSeq"
        l.d("file_list_total_count_query:::::$queryContents")
        socketIO.sendSelectQuery(cmd, queryContents, callback)
    }



    // 지정한 범위(startLine~lastLine)의 메모를 가져온다.
    fun selectMemoList(
        startLine: Int,
        lastLine: Int,
        userId: String,
        enSeq: Int,
        order_by: String,
        sort: Int,
        callback: (Any?) -> Unit
    ) {
        var queryContents = ""
        var whereContentsWithID = "where mt.user_id = N'$userId' and mt.user_id != N'-'"
        var whereContents = ""
        if(sftpData?.sftp_root_folder.equals("hanil/") || sftpData?.sftp_root_folder.equals("sftpuser01/")){
            Log.d("Noah ", "Noah ==== hanil ================================================= selectMemoList")
            queryContents = "select {mt.memo_seq, ut.user_id, ut.user_name, mt.memo_contents, mt.save_time, (select count(memo_seq) from Powermemo_FileList where memo_seq = mt.memo_seq and file_type = 'P') as picture, (select count(memo_seq) from Powermemo_FileList where memo_seq = mt.memo_seq and file_type = 'V') as video} from Powermemo_MemoList as mt INNER JOIN tblUser as ut ON mt.user_id = ut.user_id"
        } else {
            queryContents = "select {mt.memo_seq, ut.id, ut.name, mt.memo_contents, mt.save_time, (select count(memo_seq) from Powermemo_FileList where memo_seq = mt.memo_seq and file_type = 'P') as picture, (select count(memo_seq) from Powermemo_FileList where memo_seq = mt.memo_seq and file_type = 'V') as video} from Powermemo_MemoList as mt INNER JOIN tblUser as ut ON mt.user_id = ut.id"
        }
        if (enSeq >= 0) {
            whereContentsWithID = "where mt.user_id = N'$userId' and mt.en_seq = $enSeq and mt.user_id != N'-'"
            whereContents = "where mt.en_seq = $enSeq and mt.user_id != N'-'"

            if(sftpData?.sftp_root_folder.equals("hanil/")  || sftpData?.sftp_root_folder.equals("sftpuser01/")){
                queryContents = "select {mt.memo_seq, mt.en_seq, mt.hq_seq, mt.br_seq, ut.user_id, ut.user_name, mt.memo_contents, mt.save_time, (select count(memo_seq) from Powermemo_FileList where memo_seq = mt.memo_seq and file_type = 'P') as picture, (select count(memo_seq) from Powermemo_FileList where memo_seq = mt.memo_seq and file_type = 'V') as video} from Powermemo_MemoList as mt INNER JOIN tblUser as ut ON mt.user_id = ut.user_id"
            } else {
                queryContents = "select {mt.memo_seq, mt.en_seq, mt.hq_seq, mt.br_seq, ut.id, ut.name, mt.memo_contents, mt.save_time, (select count(memo_seq) from Powermemo_FileList where memo_seq = mt.memo_seq and file_type = 'P') as picture, (select count(memo_seq) from Powermemo_FileList where memo_seq = mt.memo_seq and file_type = 'V') as video} from Powermemo_MemoList as mt INNER JOIN tblUser as ut ON mt.user_id = ut.id"
            }
        }
        val cmd = "memo_list_query"

        //data 1 : 전체메모(오름차순), 2 : 전체메모(내림차순), 3 : 내메모(오름차순), 4 : 내메모(내림차순)
        if (sort == 1 || sort == 2) {
//            queryContents = "select {memo_seq, user_id, user_name, memo_contents, save_time, (select count(memo_seq) from tblFileList3 where memo_seq = t1.memo_seq and file_type = 'P') as picture, " +
            queryContents =
                "$queryContents $whereContents order by memo_seq $order_by offset $startLine rows fetch next $lastLine rows only"
//                    "(select count(memo_seq) from tblFileList3 where memo_seq = t1.memo_seq and file_type = 'V') as video} " +
//                    "from tblMemoList3 as t1 order by memo_seq " + order_by + " offset " + startLine + " rows fetch next " + lastLine + " rows only";
        } else if (sort == 3 || sort == 4) {
//            queryContents = "select {memo_seq, user_id, user_name, memo_contents, save_time, (select count(memo_seq) from tblFileList3 where memo_seq = t1.memo_seq and file_type = 'P') as picture, " +
            queryContents =
                "$queryContents $whereContentsWithID order by memo_seq $order_by offset $startLine rows fetch next $lastLine rows only"
//                    "(select count(memo_seq) from tblFileList3 where memo_seq = t1.memo_seq and file_type = 'V') as video} " +
//                    "from tblMemoList3 as t1 " + whereContents + " order by memo_seq " + order_by + " offset " + startLine + " rows fetch next " + lastLine + " rows only";
        }
        l.d("selectMemoList:::::$queryContents")
        socketIO.sendSelectQuery(cmd, queryContents, callback)
    }

    // 해당 메모의 FileTblData을 가져온다.
    fun selectFileList(
            memoSeq: Int,
            startLine: Int,
            lastLine: Int,
            callback: (Any?) -> Unit
    ) {
        val cmd = "file_list_query"
        val queryContents =
            "select {file_seq, memo_seq, file_type, original_name, thumbnail_name} from Powermemo_FileList where memo_seq = $memoSeq order by file_seq desc offset $startLine rows fetch next $lastLine rows only"
        l.d("selectMemoList:::::$queryContents")
        socketIO.sendSelectQuery(cmd, queryContents, callback)
    }


    // 해당 메모의 작성자(user_id)를 가져온다. userId가 없으면 Pc에서 지운 메모
    fun selectUserIdFromMemoSeq(memoSeq:Int, callback: (Any?) -> Unit){
        val cmd = "user_id_query"
        val queryContents = "select {user_id} from Powermemo_MemoList where memo_seq = $memoSeq"
        l.d("select user id from memo_seq ::: $queryContents")
        socketIO.sendSelectQuery(cmd, queryContents, callback)
    }


    // 해당 파일의 메모번호를 가져온다. 메모번호가 없으면 Pc에서 지운 파일
    fun selectMemoSeqFromFileName(fileName:String, callback: (Any?) -> Unit){
        val cmd = "memo_seq_query"
        val queryContents =  "select {memo_seq} from Powermemo_FileList where original_name = N'$fileName'"
        l.d("selectMemoSeqFromFileName:::::$queryContents")
        socketIO.sendSelectQuery(cmd, queryContents, callback)
    }

    // 지울 파일이름들을 가져온다.
    fun selectDeleteFileName(memoSeq: Int, callback: (Any?) -> Unit) {
        val cmd = "delete_file_name_query"
        //        String queryContents = "select {file_seq, memo_seq, file_type, original_name, thumbnail_name} from tblFileList3 where memo_seq = " + memoSeq;
        val queryContents =
            "select {file_seq, memo_seq, file_type, original_name, thumbnail_name} from Powermemo_FileList where memo_seq = $memoSeq"
        l.d("selectDetailMemo:::::$queryContents")
        socketIO.sendSelectQuery(cmd, queryContents, callback)
    }

    // 메모 입력
    fun insertMemoData(memoTblData: MemoTblData, callback: (Any?) -> Unit) {
        val convertMemoContents: String? = memoTblData.dbMemoContents?.replace("'", "''")
        Log.d("Noah ", " ---- insertMemoData ---- $convertMemoContents")
        l.d("insertMemoData: :::::::$convertMemoContents")
        val cmd = "insert_memo_table"
        //        String queryContents = "INSERT INTO tblMemoList3(user_id, user_name, memo_contents, save_time) values( N'" + memoTblData.dbUserID + "', " +
//                String queryContents = "INSERT INTO tblMemoList2(user_id, user_name, memo_contents, save_time) values( N'" + memoTblData.dbUserID + "', " +
        //        String queryContents = "INSERT INTO tblMemoList3(user_id, user_name, memo_contents, save_time) values( N'" + memoTblData.dbUserID + "', " +
//                String queryContents = "INSERT INTO tblMemoList2(user_id, user_name, memo_contents, save_time) values( N'" + memoTblData.dbUserID + "', " +

        var queryContents = "INSERT INTO Powermemo_MemoList(en_seq, hq_seq, br_seq, user_id, memo_contents, save_time, upload_status) values(${memoTblData.dbEnSeq}, ${memoTblData.dbHqSeq}, ${memoTblData.dbBrSeq}, N'${memoTblData.dbUserID}', N'$convertMemoContents', N'${memoTblData.dbSaveTime}', 0)"

        if(!BuildConfig.UTC_TIME_MODE){
            val saveTime = ConvertDate.getLocalDateTime(memoTblData.dbSaveTime!!)
            queryContents = "INSERT INTO Powermemo_MemoList(en_seq, hq_seq, br_seq, user_id, memo_contents, save_time, upload_status) values(${memoTblData.dbEnSeq}, ${memoTblData.dbHqSeq}, ${memoTblData.dbBrSeq}, N'${memoTblData.dbUserID}', N'$convertMemoContents', N'${saveTime}', 0)"
        }

        l.d("insert query ::::: $queryContents")
        socketIO.sendInsertQuery(cmd, queryContents, callback)
    }

    // 파일정보 입력
    fun insertFileData(
            memoSeq: Int,
            fileType: String,
            originalFileName: String,
            thumbnailName: String,
            callback: (Any?) -> Unit
    ) {
        val cmd = "insert_file_table"
        //        String queryContents = "INSERT INTO tblFileList3(memo_seq, file_type, original_name, thumbnail_name) values( " + memoSeq + ", N'" + fileType
        val queryContents =
            ("INSERT INTO Powermemo_FileList(memo_seq, file_type, original_name, thumbnail_name) values( " + memoSeq + ", N'" + fileType
                    + "', " + "N'" + originalFileName + "',  N'" + thumbnailName + "')")
        l.d("insertMemoData:====== $queryContents")
        socketIO.sendInsertQuery(cmd, queryContents, callback)
    }
//    =================================================================


    //  기존메모의 메모내용 수정
    fun updateQuery(memo_seq: Int, contents: String, callback: (Any?) -> Unit) {
        val cmd = "update_query"
        //        String queryContents = "update tblMemoList3 set memo_contents = N'" + contents + "' where memo_seq = " + memo_seq + "";
        val convertMemoContents = contents.replace("'".toRegex(), "''")
        l.d("insertMemoData: :::::::$convertMemoContents")
        val queryContents =
            "update Powermemo_MemoList set memo_contents = N'$convertMemoContents' where memo_seq = $memo_seq"
        l.d("updateQuery:====== $queryContents")
        socketIO.sendUpdateQuery(cmd, queryContents, callback)
    }

    // 업로드 상태값 변경(-1:실패, 0:업로드중, 1:업로드성공)
    // 각 메모에는 업로드 상태값이 존재하는데 글라스에서 업로드 중일 때 pc에서 메모를 삭제하는 일을 방지하기 위함.
    fun updateUploadStatusQuery(memo_seq: Int, upload_status:Int, callback: (Any?) -> Unit){
        val cmd = "update_upload_status_query"
        //        String queryContents = "update tblMemoList3 set memo_contents = N'" + contents + "' where memo_seq = " + memo_seq + "";
        l.d("update upload status: :::::::$upload_status")
        val queryContents =
                "update Powermemo_MemoList set upload_status = $upload_status where memo_seq = $memo_seq"
        l.d("updateUploadStatusQuery:====== $queryContents")
        socketIO.sendUpdateQuery(cmd, queryContents, callback)
    }

    // DB의 Powermemo_FileList테이블에서 file_seq이 해당되는 부분 삭제
    fun sendDeleteQuery(fileSeq: Int, callback: (Any?) -> Unit) {
        Log.d("JeonSeonIk", "FragmentMemoInput:::fileDelete()::::")
        val cmd = "delete_query"
        //        String queryContents = "delete from tblFileList3 where file_seq = " + fileSeq;
        val queryContents = "delete from Powermemo_FileList where file_seq = $fileSeq"
        l.d("sendDeleteQuery:====== $queryContents")
        socketIO.sendDeleteQuery(cmd, queryContents, callback)
    }

    // DB의 Powermemo_FileList테이블에서 memo_seq에 해당되는 부분 삭제
    fun sendAllDeleteQuery(memoSeq: Int, callback: (Any?) -> Unit) {
        Log.d("JeonSeonIk", "FragmentMemoInput:::fileDelete()::::")
        val cmd = "all_delete_query"
        //        String queryContents = "delete from tblFileList3 where memo_seq = " + memoSeq;
        val queryContents = "delete from Powermemo_FileList where memo_seq = $memoSeq"
        l.d("sendDeleteQuery:====== $queryContents")
        socketIO.sendDeleteQuery(cmd, queryContents, callback)
    }

    // DB의 Powermemo_MemoList에서 memo_seq에 해당되는 부분 삭제
    fun sendDeleteMemoQuery(memoSeq: Int, callback: (Any?) -> Unit) {
        Log.d("JeonSeonIk", "FragmentMemoInput:::fileDelete()::::")
        val cmd = "delete_memo_query"
        //        String queryContents = "delete from tblMemoList3 where memo_seq = " + memoSeq;
        val queryContents = "delete from Powermemo_MemoList where memo_seq = $memoSeq"
        l.d("sendDeleteQuery:====== $queryContents")
        socketIO.sendDeleteQuery(cmd, queryContents, callback)
    }


}

