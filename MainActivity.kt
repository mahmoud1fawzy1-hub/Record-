package com.example.floatingrecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val REQ_CODE_OVERLAY = 1001
    private val REQ_CODE_PERMISSIONS = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btn_start_overlay)
        val btnStop = findViewById<Button>(R.id.btn_stop_overlay)
        val tvStatus = findViewById<TextView>(R.id.tv_status)

        btnStart.setOnClickListener {
            checkPermissionsAndStart()
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, FloatingRecorderService::class.java))
            tvStatus.text = "Floating overlay stopped."
        }

        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        // 1. Overlay Drawing Permission Check
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant 'Draw over other apps' permission", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQ_CODE_OVERLAY)
            return
        }

        // 2. Microphone & Notification Permissions Check
        val neededPermissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = neededPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQ_CODE_PERMISSIONS)
            return
        }

        // All granted, start service
        startFloatingOverlay()
    }

    private fun startFloatingOverlay() {
        val intent = Intent(this, FloatingRecorderService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Floating Recorder active! Move to other apps.", Toast.LENGTH_SHORT).show()
        // Minimize activity so user is on home screen or previous app
        moveTaskToBack(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                checkPermissionsAndStart()
            } else {
                Toast.makeText(this, "Overlay permission is required to float over other apps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startFloatingOverlay()
            } else {
                Toast.makeText(this, "Microphone permission is required to record audio", Toast.LENGTH_SHORT).show()
            }
        }
    }
}