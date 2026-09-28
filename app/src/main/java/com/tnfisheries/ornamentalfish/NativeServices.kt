package com.tnfisheries.ornamentalfish

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.Locale

internal class SpeechController(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts?.language = Locale.US
    }

    fun speak(text: String, tamil: Boolean) {
        val phrase = text.trim().take(400)
        if (phrase.isEmpty()) return
        val engine = tts
        if (!ready || engine == null) {
            Toast.makeText(context, R.string.tts_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        val locale = if (tamil) Locale("ta", "IN") else Locale("en", "IN")
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (tamil) {
                Toast.makeText(context, R.string.tts_tamil_missing, Toast.LENGTH_SHORT).show()
                return
            }
            engine.language = Locale.US
        }
        engine.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "ornamental-fish-native")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}

internal object NativeServices {
    private const val PREFS = "of_reminders"
    private const val KEY_WHEN = "when_utc"
    private const val KEY_TITLE = "title"
    private const val KEY_BODY = "body"

    fun share(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, text.take(8000))
        }
        runCatching {
            context.startActivity(Intent.createChooser(send, context.getString(R.string.share_title)))
        }.onFailure {
            Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun armReminder(context: Context, hours: Int, title: String, body: String) {
        val safeHours = if (hours <= 0) 24 else hours.coerceAtMost(24 * 30)
        val whenUtc = System.currentTimeMillis() + safeHours * 3_600_000L
        val safeTitle = title.ifBlank { context.getString(R.string.reminder_default_title) }
        val safeBody = body.ifBlank { context.getString(R.string.reminder_default_body) }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_WHEN, whenUtc)
            .putString(KEY_TITLE, safeTitle)
            .putString(KEY_BODY, safeBody)
            .apply()

        enqueueAlarm(context, whenUtc, safeTitle, safeBody)
        Toast.makeText(context, R.string.reminder_set, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun restoreScheduledReminder(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val whenUtc = prefs.getLong(KEY_WHEN, 0L)
        if (whenUtc <= System.currentTimeMillis()) return
        enqueueAlarm(
            context,
            whenUtc,
            prefs.getString(KEY_TITLE, context.getString(R.string.reminder_default_title)).orEmpty(),
            prefs.getString(KEY_BODY, context.getString(R.string.reminder_default_body)).orEmpty()
        )
    }

    private fun enqueueAlarm(context: Context, whenUtc: Long, title: String, body: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2401,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val delay = (whenUtc - System.currentTimeMillis()).coerceAtLeast(5000L)
        alarm.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + delay,
            pendingIntent
        )
    }
}
