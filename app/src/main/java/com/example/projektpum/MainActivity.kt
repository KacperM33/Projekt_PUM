package com.example.projektpum

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {
    private val camera_permission = 100
    private val storage_permission = 101

    private var stepCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.layout)

        val serviceIntent = Intent(this, StepCounter::class.java)
        startService(serviceIntent)

        registerReceiver(stepReceiver, IntentFilter("step_count_updated"))

        findViewById<Button>(R.id.music_button).setOnClickListener {
            val musicIntent = Intent(applicationContext, MusicPlayer::class.java)
            startActivity(musicIntent)
        }

        findViewById<Button>(R.id.camera_button).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), camera_permission)
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), storage_permission)
            }

            val intentCapture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intentCapture, 123)
        }
    }

    //  PERMISSIONS
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == camera_permission) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Uprawnienia do kamery są wymagane.", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == storage_permission) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Uprawnienia do folderu są wymagane.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //  CAMERA
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && requestCode == 123) {
            val bmp = data.extras?.get("data") as Bitmap

            bmp?.let {
                saveImageToMediaStore(it)
            }
        }
    }

    // SAVE PHOTOS
    private fun saveImageToMediaStore(imageBitmap: Bitmap) {
        val displayName = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        val mimeType = "image/jpeg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
            }
        }
    }

    // STEP COUNTER
    private val stepReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "step_count_updated") {
                stepCount = intent.getIntExtra("step_count", stepCount)
                updateStepCountOnUI()
            }
        }
    }
    private fun updateStepCountOnUI() {
        val stepCountTextView = findViewById<TextView>(R.id.steps)
        stepCountTextView.text = stepCount.toString()
    }
}
