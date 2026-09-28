package com.tnfisheries.ornamentalfish

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import java.io.OutputStream

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQ_CREATE_DOC = 3001
        private const val REQ_POST_NOTIF = 3002
    }

    private lateinit var speech: SpeechController
    private var pendingExport: String? = null
    private var pendingReminder: Triple<Int, String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speech = SpeechController(this)
        val content = NativeContentRepository(this).load()
        setContent {
            OrnamentalFishNativeApp(
                contentResult = content,
                speak = { text, tamil -> speech.speak(text, tamil) },
                share = { text -> NativeServices.share(this, text) },
                exportBookmarks = ::exportBookmarks,
                scheduleReminder = ::scheduleReminder
            )
        }
    }

    private fun scheduleReminder(hours: Int, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingReminder = Triple(hours, title, body)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_POST_NOTIF)
            return
        }
        NativeServices.armReminder(this, hours, title, body)
    }

    private fun exportBookmarks(text: String) {
        pendingExport = text.take(200_000)
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "ornamental-fish-bookmarks.txt")
        }
        runCatching { startActivityForResult(intent, REQ_CREATE_DOC) }
            .onFailure { Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show() }
    }

    @Deprecated("Deprecated in Android API; retained for minSdk-compatible document export")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_CREATE_DOC) return
        val payload = pendingExport
        val uri = data?.data
        if (resultCode != Activity.RESULT_OK || payload == null || uri == null) {
            pendingExport = null
            return
        }
        try {
            contentResolver.openOutputStream(uri).use { out: OutputStream? ->
                out?.write(payload.toByteArray(Charsets.UTF_8))
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show()
        } finally {
            pendingExport = null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_POST_NOTIF) return
        val pending = pendingReminder
        pendingReminder = null
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED && pending != null) {
            NativeServices.armReminder(this, pending.first, pending.second, pending.third)
        } else {
            Toast.makeText(this, R.string.notif_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        speech.shutdown()
        super.onDestroy()
    }
}
