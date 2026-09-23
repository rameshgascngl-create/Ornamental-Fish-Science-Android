package com.tnfisheries.ornamentalfish;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class WebAppInterface {

    private final MainActivity activity;
    private TextToSpeech tts;
    private boolean ttsReady;
    private String pendingExport;
    private final ActivityResultLauncher<Intent> createDoc;
    private final ActivityResultLauncher<String> notifPermission;

    WebAppInterface(MainActivity activity) {
        this.activity = activity;

        createDoc = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK
                            || result.getData() == null
                            || result.getData().getData() == null
                            || pendingExport == null) {
                        return;
                    }
                    try (OutputStream out = activity.getContentResolver()
                            .openOutputStream(result.getData().getData())) {
                        if (out != null) {
                            out.write(pendingExport.getBytes(StandardCharsets.UTF_8));
                        }
                    } catch (Exception e) {
                        toast(activity.getString(R.string.export_failed));
                    } finally {
                        pendingExport = null;
                    }
                });

        notifPermission = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        toast(activity.getString(R.string.notif_denied));
                    }
                });

        tts = new TextToSpeech(activity, status -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            if (ttsReady) {
                tts.setLanguage(Locale.US);
            }
        });
    }

    @JavascriptInterface
    public void pronounce(String text, String lang) {
        if (text == null || text.trim().isEmpty()) return;
        activity.runOnUiThread(() -> {
            if (!ttsReady || tts == null) {
                toast(activity.getString(R.string.tts_unavailable));
                return;
            }
            Locale loc = localeFor(lang);
            int ok = tts.setLanguage(loc);
            if (ok == TextToSpeech.LANG_MISSING_DATA
                    || ok == TextToSpeech.LANG_NOT_SUPPORTED) {
                if (isTamil(lang)) {
                    toast(activity.getString(R.string.tts_tamil_missing));
                    return;
                }
                tts.setLanguage(Locale.US);
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "of-pronounce");
        });
    }

    @JavascriptInterface
    public void share(String text) {
        if (text == null) text = "";
        final String body = text;
        activity.runOnUiThread(() -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.app_name));
            send.putExtra(Intent.EXTRA_TEXT, body);
            try {
                activity.startActivity(Intent.createChooser(
                        send, activity.getString(R.string.share_title)));
            } catch (ActivityNotFoundException e) {
                toast(activity.getString(R.string.share_failed));
            }
        });
    }

    @JavascriptInterface
    public void scheduleReminder(int hours, String title, String body) {
        activity.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= 33) {
                int perm = ContextCompat.checkSelfPermission(
                        activity, Manifest.permission.POST_NOTIFICATIONS);
                if (perm != PackageManager.PERMISSION_GRANTED) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return;
                }
            }
            int safeHours = hours <= 0 ? 24 : hours;
            long trigger = SystemClock.elapsedRealtime()
                    + safeHours * 60L * 60L * 1000L;

            Intent i = new Intent(activity, ReminderReceiver.class);
            i.putExtra(ReminderReceiver.EXTRA_TITLE,
                    title == null || title.isEmpty()
                            ? activity.getString(R.string.reminder_default_title)
                            : title);
            i.putExtra(ReminderReceiver.EXTRA_BODY,
                    body == null || body.isEmpty()
                            ? activity.getString(R.string.reminder_default_body)
                            : body);

            PendingIntent pi = PendingIntent.getBroadcast(
                    activity,
                    2401,
                    i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager am = (AlarmManager) activity.getSystemService(Activity.ALARM_SERVICE);
            if (am == null) return;
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi);
            toast(activity.getString(R.string.reminder_set));
        });
    }

    @JavascriptInterface
    public void exportBookmarks(String text) {
        pendingExport = text == null ? "" : text;
        activity.runOnUiThread(() -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, "ornamental-fish-bookmarks.txt");
            try {
                createDoc.launch(intent);
            } catch (ActivityNotFoundException e) {
                toast(activity.getString(R.string.export_failed));
            }
        });
    }

    @JavascriptInterface
    public void onTabChanged(String tab) {
        activity.selectTabFromJs(tab);
    }

    void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    private static boolean isTamil(String lang) {
        if (lang == null) return false;
        String l = lang.toLowerCase(Locale.ROOT);
        return l.startsWith("ta");
    }

    private static Locale localeFor(String lang) {
        if (lang == null || lang.trim().isEmpty()) return Locale.US;
        String[] parts = lang.replace('_', '-').split("-");
        if (parts.length >= 2) return new Locale(parts[0], parts[1]);
        return new Locale(parts[0]);
    }

    private void toast(String msg) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
    }
}
