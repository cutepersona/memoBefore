package com.peng.power.memo.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.peng.power.memo.R
import com.peng.power.memo.data.FileTblData
import com.peng.power.memo.data.SignalingSendData
import com.peng.power.memo.util.l
import java.io.File

class FileRecyclerViewAdapter(
    private val context: Context,
    private val dataList:ArrayList<FileTblData>,
    private val callback: (Int, FileTblData)->Unit
) :RecyclerView.Adapter<FileRecyclerViewAdapter.FileViewHolder>() {

    private var fileTitleCount = 0


    inner class FileViewHolder(itemView:View):RecyclerView.ViewHolder(itemView) {
        val tvFileSeq: TextView = itemView.findViewById(R.id.tv_file_number)
        val ivFileContents:ImageView = itemView.findViewById(R.id.iv_file_contents)
        val ivVideoIcon:ImageView = itemView.findViewById(R.id.iv_video_icon)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val holderView: View =
            LayoutInflater.from(parent.context).inflate(R.layout.file_list_item, parent, false)
        return FileViewHolder(holderView)
    }

    // 실제 각 뷰 홀더에 데이터를 연결해주는 함수
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(fileViewHolder: FileViewHolder, position: Int) {
        val data: FileTblData = dataList[position]
        l.d("onFileBindViewHolder:|||||" + position + ":::::::::" + data.dbFile_type)
        val thumbnailPath = (SignalingSendData.getRootPath()
                + SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER + data.dbThumbnail_name)
        val imgFile = File(thumbnailPath)

        if(!imgFile.exists()){
            l.d("imgFile not exists")
        }else{
            l.d("imgFile exists :: Success")
        }

        Glide.with(context)
            .load(imgFile)
            .asBitmap()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .dontAnimate()
            .into(object : SimpleTarget<Bitmap?>() {
                override fun onResourceReady(arg0: Bitmap?, arg1: GlideAnimation<in Bitmap?>?) {
                    // TODO Auto-generated method stub
                    fileViewHolder.ivFileContents.setImageBitmap(arg0)
                }
            })

        fileTitleCount = position + 1
        fileViewHolder.tvFileSeq.text = context.resources.getString(R.string.file) + " " + fileTitleCount

        if (dataList[position].dbFile_type == "V") {
            fileViewHolder.ivVideoIcon.setVisibility(View.VISIBLE)
        } else {
            fileViewHolder.ivVideoIcon.setVisibility(View.GONE)
        }
        fileViewHolder.tvFileSeq.setOnClickListener{
            callback(position, data)
        }
    }

    override fun getItemCount(): Int {
        //l.d("getItemCount: :::" + dataList.size)
        return dataList.size
    }


}