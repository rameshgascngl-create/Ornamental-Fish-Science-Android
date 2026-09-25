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

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class WebAppInterface {

    private final MainActivity activity;
    private TextToSpeech tts;
    private boolean ttsReady;
    private String pendingExport;

    WebAppInterface(MainActivity activity) {
        this.activity = activity;
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
                int perm = activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS);
                if (perm != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        MainActivity.REQ_POST_NOTIF);
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
                activity.startActivityForResult(intent, MainActivity.REQ_CREATE_DOC);
            } catch (ActivityNotFoundException e) {
                toast(activity.getString(R.string.export_failed));
            }
        });
    }

    @JavascriptInterface
    public void onTabChanged(String tab) {
        activity.selectTabFromJs(tab);
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != MainActivity.REQ_CREATE_DOC) return;
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null
                || pendingExport == null) {
            return;
        }
        try (OutputStream out = activity.getContentResolver().openOutputStream(data.getData())) {
            if (out != null) {
                out.write(pendingExport.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            toast(activity.getString(R.string.export_failed));
        } finally {
            pendingExport = null;
        }
    }

    void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode != MainActivity.REQ_POST_NOTIF) return;
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            toast(activity.getString(R.string.notif_denied));
        }
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
