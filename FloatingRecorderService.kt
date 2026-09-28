package com.example.floatingrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.IOException

class FloatingRecorderService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var layoutParams: WindowManager.LayoutParams

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String? = null

    private var isRecording = false
    private var isPaused = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.overlay_recorder_bar, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        setupDragListener()
        setupOverlayButtons()

        windowManager.addView(floatingView, layoutParams)
    }

    private fun setupOverlayButtons() {
        val btnRecord = floatingView.findViewById<Button>(R.id.btn_record)
        val btnPause = floatingView.findViewById<Button>(R.id.btn_pause)
        val btnFinishPreview = floatingView.findViewById<Button>(R.id.btn_finish_preview)
        val tvStatus = floatingView.findViewById<TextView>(R.id.tv_status_pill)
        val btnClose = floatingView.findViewById<View>(R.id.btn_close_overlay)

        // 1. Record Button
        btnRecord.setOnClickListener {
            stopPlaybackIfPlaying()
            if (!isRecording) {
                startRecording()
                tvStatus.text = "● REC"
                btnPause.isEnabled = true
                btnPause.text = "Pause"
                btnFinishPreview.isEnabled = true
            } else {
                // Restart recording if already recording
                stopRecordingQuietly()
                startRecording()
                tvStatus.text = "● REC"
                btnPause.text = "Pause"
            }
        }

        // 2. Pause Button
        btnPause.setOnClickListener {
            if (isRecording) {
                if (!isPaused) {
                    pauseRecording()
                    btnPause.text = "Resume"
                    tvStatus.text = "❚❚ PAUSED"
                } else {
                    resumeRecording()
                    btnPause.text = "Pause"
                    tvStatus.text = "● REC"
                }
            }
        }

        // 3. Finish / Preview Button
        btnFinishPreview.setOnClickListener {
            if (isRecording) {
                finishRecordingAndPlayPreview(tvStatus)
            } else if (audioFilePath != null) {
                // Replay preview if already finished
                playRecordedPreview(tvStatus)
            } else {
                Toast.makeText(this, "Press Record first", Toast.LENGTH_SHORT).show()
            }
        }

        // Close button (x) to remove floating bar
        btnClose?.setOnClickListener {
            stopSelf()
        }
    }

    private fun startRecording() {
        try {
            audioFilePath = "${cacheDir.absolutePath}/rec_${System.currentTimeMillis()}.m4a"
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
            isRecording = true
            isPaused = false
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Record error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && !isPaused) {
            try {
                mediaRecorder?.pause()
                isPaused = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && isPaused) {
            try {
                mediaRecorder?.resume()
                isPaused = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun finishRecordingAndPlayPreview(tvStatus: TextView) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        isRecording = false
        isPaused = false

        // Immediately play back the recorded audio for preview!
        playRecordedPreview(tvStatus)
    }

    private fun playRecordedPreview(tvStatus: TextView) {
        val path = audioFilePath ?: return
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(this, "Audio file not found", Toast.LENGTH_SHORT).show()
            return
        }

        stopPlaybackIfPlaying()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                tvStatus.text = "▶ Playing Preview"
                setOnCompletionListener {
                    tvStatus.text = "✓ Preview Finished"
                    release()
                    mediaPlayer = null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Playback error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlaybackIfPlaying() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
    }

    private fun stopRecordingQuietly() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {}
        mediaRecorder = null
        isRecording = false
        isPaused = false
    }

    private fun setupDragListener() {
        val dragHandle = floatingView.findViewById<View>(R.id.drag_handle)
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, layoutParams)
                    true
                }
                else -> false
            }
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "floating_recorder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating Recorder Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Floating Recorder Active")
            .setContentText("Overlay bar drawing over other apps")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlaybackIfPlaying()
        stopRecordingQuietly()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}