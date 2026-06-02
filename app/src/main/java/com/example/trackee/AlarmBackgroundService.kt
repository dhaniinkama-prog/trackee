package com.example.trackee

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.util.Log

class AlarmBackgroundService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ringtoneUriStr = intent?.getStringExtra("RINGTONE_URI")

        if (mediaPlayer == null) {
            try {
                mediaPlayer = if (!ringtoneUriStr.isNullOrEmpty()) {
                    MediaPlayer.create(this, Uri.parse(ringtoneUriStr))
                } else {
                    // Jika null, gunakan ringtone alarm bawaan HP
                    val defaultAlarm = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    MediaPlayer.create(this, defaultAlarm)
                }

                mediaPlayer?.isLooping = true // Biar bunyi terus-menerus
                mediaPlayer?.start()
                Log.d("TrackeeService", "Suara alarm mulai berdering...")
            } catch (e: Exception) {
                Log.e("TrackeeService", "Gagal memutar audio alarm", e)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d("TrackeeService", "Suara alarm dimatikan.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}