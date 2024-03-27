package com.peng.power.memo.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.peng.power.memo.R
import com.peng.power.memo.data.ConvertDate
import com.peng.power.memo.data.MemoTblData
import com.peng.power.memo.sftp.SftpUploadManager
import com.peng.power.memo.util.l


class RecyclerViewAdapter(
    private val context: Context, private val dataList: ArrayList<MemoTblData>,
    private val callback: (Int, Int, Int, Int, String, String, String, Long, Int)->Unit
) : RecyclerView.Adapter<RecyclerViewAdapter.Holder>() {

    inner class Holder(itemView: View):RecyclerView.ViewHolder(itemView){
        val tvMemoSeq = itemView.findViewById<TextView>(R.id.tv_memo_number)
        val tvUserID = itemView.findViewById<TextView>(R.id.tv_memo_user_id_contents)
        val tvUserName = itemView.findViewById<TextView>(R.id.tv_memo_user_contents)
        val tvMemoContents = itemView.findViewById<TextView>(R.id.tv_memo_contents)
        val tvSaveTime = itemView.findViewById<TextView>(R.id.tv_memo_date_contents)
        val tvPictureCount = itemView.findViewById<TextView>(R.id.tv_picture_count)
        val tvVideoCount = itemView.findViewById<TextView>(R.id.tv_video_count)
        val llUploading = itemView.findViewById<LinearLayout>(R.id.ll_uploading)
        val llFileCount = itemView.findViewById<LinearLayout>(R.id.ll_filecount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holderView =
            LayoutInflater.from(parent.context).inflate(R.layout.memo_list_item, parent, false)
        return Holder(holderView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        l.d("onBindViewHolder: ^^^^^^^^^^^^^^")
        val data: MemoTblData = dataList[position]
        holder.tvMemoSeq.text = context.resources.getString(R.string.memo) + " " + data.dbMemoSeq
        holder.tvUserID.text = data.dbUserID
        holder.tvUserName.text = data.dbUserName
        holder.tvMemoContents.text = data.dbMemoContents
        holder.tvSaveTime.text = ConvertDate.getLocalDateTime(data.dbSaveTime!!)
        holder.tvPictureCount.text = java.lang.String.valueOf(data.dbPictureCount)
        holder.tvVideoCount.text = java.lang.String.valueOf(data.dbVideoCount)
        holder.tvMemoSeq.setOnClickListener{
            l.d("onClick: :::::::::::::::$position")
            callback(data.dbMemoSeq?:0,
                    data.dbEnSeq?:-1,
                data.dbHqSeq?:-1,
                data.dbBrSeq?:-1,
                data.dbUserID?:"",
                data.dbUserName?:"",
                data.dbMemoContents?:"",
                data.dbSaveTime?:0L,
                position)
        }

        data.dbMemoSeq?.let{
            if(SftpUploadManager.isUploading(it)){
                l.d("is now uploading process")
                holder.llUploading.visibility = View.VISIBLE
                holder.llFileCount.visibility = View.GONE

                SftpUploadManager.setOnDownloadCompleteListener(it){
                    l.d("download complete callback in recycler adapter")
                    holder.llUploading.visibility = View.GONE
                    holder.llFileCount.visibility = View.VISIBLE
                }
            }else{
                holder.llUploading.visibility = View.GONE
                holder.llFileCount.visibility = View.VISIBLE
                l.d("not!!! now uploading process")
            }
        }

    }

    override fun getItemCount(): Int {
        //l.d("getItemCount ::: ${dataList.size}")
        return dataList.size
    }
}