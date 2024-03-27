package com.peng.power.memo.manager

import com.peng.power.memo.model.FileUpDown
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


interface MemoCallAPI {


    @Multipart
//    @POST("https://dev.watttalk.kr:8221/PowerManagerBackend/fileupload/wattmemo")
    @POST("PowerManagerBackend/fileupload/wattmemo")
    fun FileUpload(@Part upload_file: MultipartBody.Part, @PartMap save_folder: HashMap<String, RequestBody>): Call<FileUpDown>

}