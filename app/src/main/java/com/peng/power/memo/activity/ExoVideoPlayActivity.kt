package com.peng.power.memo.activity

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.peng.power.memo.R
import com.peng.power.memo.databinding.ActivityExoVideoPlayBinding
import com.peng.power.memo.dialog.UserDialog
import com.peng.power.memo.sftp.SftpProgressDialog
import com.peng.power.memo.util.l
import kotlinx.coroutines.*
import kotlin.properties.Delegates

class ExoVideoPlayActivity : BaseActivity<ActivityExoVideoPlayBinding>() {

    private lateinit var progressDialog: SftpProgressDialog
    private var jobProgressBar: Job? = null
    private var duration:Long = -1
    private var userDialog: UserDialog? = null

    private var simpleExoPlayer:SimpleExoPlayer?=null

    var display: DisplayMetrics? = null
    var fontSize: Float = 21f

    override fun initViewBinding() {
        binding = ActivityExoVideoPlayBinding.inflate(layoutInflater)
    }

    override fun initAfterBinding() {
        val url = intent.getStringExtra("url") ?: ""
        l.d(url)

        userDialog = UserDialog(this)
        progressDialog = SftpProgressDialog(this, 0)

        // 2023-02-06    Navigate520으로 인해 DP로 구분짓기 위함
        display = this.applicationContext?.resources?.displayMetrics

        initExoPlayer(url)
        initUi()
    }

    override fun onPause() {
        super.onPause()
        stopProgressBar()
        setStatus(PlayerStatus.Pause)
    }


    override fun onDestroy() {
        super.onDestroy()
        stopProgressBar()
        setStatus(PlayerStatus.Stop)
    }

    private fun initExoPlayer(url:String){
        Log.d("Noah ", "initExoPlayer ==== " + url)
        simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
        binding.playerView.player = simpleExoPlayer

        // MediaItem을 만들고
        val mediaItem = MediaItem.fromUri(Uri.parse(url))

        // MediaSource를 만들고
        val userAgent = Util.getUserAgent(this, this.applicationInfo.name)
        val factory = DefaultDataSourceFactory(this, userAgent)
        val progressiveMediaSource = ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem)
        // 만들어진 MediaSource를 연결
        simpleExoPlayer?.setMediaSource(progressiveMediaSource)

        simpleExoPlayer?.addListener(object: Player.EventListener{
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                handlePlayerStateChanged(playWhenReady, playbackState)
            }
        })
        
        simpleExoPlayer?.prepare()
    }
    

    private enum class PlayerStatus{
        Play,
        Pause,
        Stop
    }

    private var playerStatus by Delegates.observable(PlayerStatus.Stop){ _,_,newValue->
        simpleExoPlayer?.let{ exoPlayer ->
            when(newValue){
                PlayerStatus.Play->{
                    addKeepScreenOn()
                    showStopPauseButton()
                    exoPlayer.play()
                    startProgressBar()
                    hideBlur()
                    setButtonOutLine(binding.playBtn)
                }
                PlayerStatus.Stop->{
                    clearKeepScreenOn()
                    exoPlayer.pause()
                    exoPlayer.seekTo(0)
                    stopProgressBar()
                    showPlayIcon()
                    setButtonOriginColor()
                    hideStopPauseButton()
                    binding.progressBar.progress = 0
                    binding.tvPastTime.text = "00:00"
                }
                PlayerStatus.Pause->{
                    exoPlayer.pause()

                    stopProgressBar()
                    showPauseIcon()
                    setButtonOutLine(binding.pauseBtn)
                }
            }
        }
    }

    private fun setStatus(status:PlayerStatus){
        playerStatus = status
    }

    private fun initUi(){
        binding.run { 
            pauseBtn.setOnClickListener {
                setStatus(PlayerStatus.Pause)
            }
            
            playBtn.setOnClickListener { 
                simpleExoPlayer?.let{
                    if(it.isPlaying)
                        return@setOnClickListener
                    setStatus(PlayerStatus.Play)
                }
            }
            
            stopBtn.setOnClickListener { 
                setStatus(PlayerStatus.Stop)
            }
            
            tvBack.setOnClickListener { 
                setStatus(PlayerStatus.Stop)
                finish()
            }

            setVolumeButton()

            if(duration <= 0){
                showProgressDialog()
            }

            // 2023-02-06   Navigate520
            if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
                Log.d("Noah", " Navigate520 ")
                binding.tvBack.setTextSize(fontSize)
                binding.playBtn.setTextSize(fontSize)
                binding.stopBtn.setTextSize(fontSize)
                binding.pauseBtn.setTextSize(fontSize)
                binding.tvPastTime.setTextSize(fontSize)
                binding.tvTotalTime.setTextSize(fontSize)
            }


        }
    }



    private fun startProgressBar(){
        if(jobProgressBar == null || !jobProgressBar?.isActive!!){
            jobProgressBar = launch {
                while (isActive){
                    delay(300)
                    if(duration > 0){
                        val current = simpleExoPlayer?.currentPosition ?:0
                        val percent:Double = (current.toDouble() / duration.toDouble()) * 1000.0
                        l.d("percent : $percent")
                        binding.progressBar.progress = percent.toInt()
                        binding.tvPastTime.text = convertPastTimeMillsToHHMMSSColon(current/1000)
                    }
                }
            }
        }
    }


    private fun stopProgressBar(){
        jobProgressBar?.cancel()
    }


    private fun addKeepScreenOn(){
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    
    private fun clearKeepScreenOn(){
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    private fun handlePlayerStateChanged(playWhenReady: Boolean, playbackState: Int){
        simpleExoPlayer?.let { exoPlayer->
            when (playbackState) {
                Player.STATE_IDLE -> { //재생실패
                    progressDialog.dismiss()
                    userDialog?.showDialogWithOkBtn("불러오기 실패","동영상을 불러오는데 문제가 생겼습니다.\n인터넷 연결을 확인해 주세요"){
                        l.d("finish")
                        userDialog?.dismiss()
                        finish()
                    }
                }
                Player.STATE_BUFFERING -> {
                    l.d("buffering")
                }
                Player.STATE_READY -> {
                    duration = exoPlayer.duration
                    l.d("duration in ready : $duration")
                    if(progressDialog.isShowing)
                        progressDialog.dismiss()
                    if(userDialog?.isShowing!!)
                        userDialog?.dismiss()
                    binding.tvTotalTime.text = convertPastTimeMillsToHHMMSSColon(duration/1000)
                }
                Player.STATE_ENDED -> {
                    setStatus(PlayerStatus.Stop)
                }
                else -> {
                    l.e("else on handlePlayerStateChanged")
                }
            }
        }

    }


    private fun hideStopPauseButton(){
        binding.run {
            stopBtn.visibility = View.GONE
            pauseBtn.visibility = View.GONE

        }
    }

    private fun showStopPauseButton(){
        binding.run {
            stopBtn.visibility = View.VISIBLE
            pauseBtn.visibility = View.VISIBLE
        }
    }

    private fun showPlayIcon(){
        binding.llBlur.visibility = View.VISIBLE
        binding.iconPlay.visibility = View.VISIBLE
        binding.iconPause.visibility = View.GONE
        binding.iconStop.visibility = View.GONE
    }

    private fun hideBlur(){
        binding.llBlur.visibility = View.GONE
    }

    private fun showPauseIcon(){
        binding.llBlur.visibility = View.VISIBLE
        binding.iconPlay.visibility = View.GONE
        binding.iconPause.visibility = View.VISIBLE
        binding.iconStop.visibility = View.GONE
    }


    private fun setButtonOutLine(tv: TextView){
        setButtonOriginColor()
        tv.setBackgroundResource(R.drawable.btn_bg_round_line)
    }

    private fun setButtonOriginColor(){
        binding.playBtn.setBackgroundResource(R.drawable.btn_bg_round)
        binding.pauseBtn.setBackgroundResource(R.drawable.btn_bg_round)
        binding.stopBtn.setBackgroundResource(R.drawable.btn_bg_round)
    }
    
    private fun setVolumeButton(){
        binding.run {
            setClickVolume(volume1, 1)
            setClickVolume(volume2, 2)
            setClickVolume(volume3, 3)
            setClickVolume(volume4, 5)
            setClickVolume(volume5, 7)
            setClickVolume(volume6, 9)
            setClickVolume(volume7, 11)
            setClickVolume(volume8, 13)
            setClickVolume(volume9, 14)
            setClickVolume(volume10, 15)
            setClickVolume(mute, 0)
        }
    }

    private fun setClickVolume(tv: TextView, volume:Int){
        tv.setOnClickListener {
            setStreamVolume(volume)
        }
    }

    private fun setStreamVolume(volume:Int){
        val am: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        l.d("set volume : $volume")
        if(volume == 0){
            am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND)
        }else{
            am.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND)
        }
    }

    private fun showProgressDialog(){
        progressDialog.setMessage(resources.getString(R.string.gallery_saving_check))
        progressDialog.setCancelable(true)
        progressDialog.show()
    }


    private fun convertPastTimeMillsToHHMMSSColon(pastTime:Long):String{
        var minutes = 0
        var seconds = 0

        if(pastTime == 3600L){
            return "60:00"
        }

        minutes = (pastTime / 60).toInt()
        seconds = (pastTime % 60).toInt()
        minutes %= 60

        var availableRecordTime = ""

        availableRecordTime += if(minutes < 10){
            "0${minutes}:"
        }else{
            "${minutes}:"
        }

        availableRecordTime += if(seconds<10){
            "0${seconds}"
        }else{
            "${seconds}"
        }

        return availableRecordTime
    }
}