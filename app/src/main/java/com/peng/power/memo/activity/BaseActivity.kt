package com.peng.power.memo.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.peng.power.memo.data.MemoTableData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

// gradle에서 ViewBding 및 Coroutine 설정 해줘야 함.
abstract class BaseActivity<T:ViewBinding> : AppCompatActivity(), CoroutineScope{
    protected lateinit var job:Job

    protected lateinit var binding:T

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        job = SupervisorJob()
        super.onCreate(savedInstanceState)
        initViewBinding()
        setContentView(binding.root)
        initAfterBinding()
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    /**
     * 첫번째 호출.
     * 데이터 바인딩 설정.
     */
    abstract fun initViewBinding()

    /**
     * 데이터 바인딩 직후 호출
     * 뷰나 액티비티의 속성 등을 초기화.
     * ex) 리사이클러뷰, 툴바, 드로어뷰..
     */
    abstract fun initAfterBinding()

    protected val jsonMemoDataName = "jsonMemoInfo"

//    protected fun getJsonStringFromMemoTableInfo(memo_seq:Int?, en_seq:Int?, hq_seq:Int?, br_seq:Int?, user_id:String?, user_name:String?, memo_contents:String?, save_time:String?):String{
//        val memoTableInfo = MemoTableInfo(
//            memo_seq?:0,
//            en_seq?:-1,
//            hq_seq?:-1,
//            br_seq?:-1,
//            user_id?:"",
//            user_name?:"",
//            memo_contents?:"",
//            save_time?:""
//        )
//        return Json.encodeToString(memoTableInfo)
//    }

//    protected fun getJsonStringFromMemoTableInfo(memoTableInfo: MemoTableInfo):String{
//        return Json.encodeToString(memoTableInfo)
//    }

//    protected fun getJsonStringFromMemoTableInfo(memo_seq:Int?, data_user:DataUser?, memo_contents:String, save_time:String):String{
//        val memoTableInfo = MemoTableInfo(
//            memo_seq?:0,
//            data_user?.enSeq?:-1,
//            data_user?.hqSeq?:-1,
//            data_user?.brSeq?:-1,
//            data_user?.userId?:"",
//            data_user?.userName?:"",
//            memo_contents,
//            save_time
//        )
//        return Json.encodeToString(memoTableInfo)
//    }

    protected fun putExtra(intent:Intent, memoTableData: MemoTableData){
        intent.putExtra(jsonMemoDataName, memoTableData.toJson())
    }

    protected fun getMemoTableDataFromJsonString(intent:Intent): MemoTableData {
        return Json.decodeFromString(intent.getStringExtra(jsonMemoDataName)!!)
    }

}