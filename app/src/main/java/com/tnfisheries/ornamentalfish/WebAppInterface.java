package com.tnfisheries.ornamentalfish;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    static final String PREFS = "of_reminders";
    static final String KEY_WHEN = "when_utc";
    static final String KEY_TITLE = "title";
    static final String KEY_BODY = "body";
    static final String KEY_PENDING_HOURS = "pending_hours";

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
        if (text.length() > 400) text = text.substring(0, 400);
        final String spoken = text.trim();
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
            tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "of-pronounce");
        });
    }

    @JavascriptInterface
    public void share(String text) {
        if (text == null) text = "";
        if (text.length() > 8000) text = text.substring(0, 8000);
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
            SharedPreferences p = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit().putInt(KEY_PENDING_HOURS, hours <= 0 ? 24 : hours)
                .putString(KEY_TITLE, title)
                .putString(KEY_BODY, body)
                .apply();
            if (Build.VERSION.SDK_INT >= 33) {
                int perm = activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS);
                if (perm != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        MainActivity.REQ_POST_NOTIF);
                    return;
                }
            }
            armReminder(activity, hours, title, body);
        });
    }

    @JavascriptInterface
    public void exportBookmarks(String text) {
        pendingExport = text == null ? "" : text;
        if (pendingExport.length() > 200000) {
            pendingExport = pendingExport.substring(0, 200000);
        }
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
        if (tab == null) return;
        switch (tab) {
            case "home":
            case "atlas":
            case "book":
            case "quiz":
            case "tools":
                activity.selectTabFromJs(tab);
                break;
            default:
                break;
        }
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
            return;
        }
        SharedPreferences p = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        armReminder(
            activity,
            p.getInt(KEY_PENDING_HOURS, 24),
            p.getString(KEY_TITLE, null),
            p.getString(KEY_BODY, null)
        );
    }

    static void armReminder(Context context, int hours, String title, String body) {
        int safeHours = hours <= 0 ? 24 : Math.min(hours, 24 * 30);
        long whenUtc = System.currentTimeMillis() + safeHours * 3600000L;
        String t = (title == null || title.isEmpty())
            ? context.getString(R.string.reminder_default_title) : title;
        String b = (body == null || body.isEmpty())
            ? context.getString(R.string.reminder_default_body) : body;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_WHEN, whenUtc)
            .putString(KEY_TITLE, t)
            .putString(KEY_BODY, b)
            .apply();
        enqueueAlarm(context, whenUtc, t, b);
        Toast.makeText(context, R.string.reminder_set, Toast.LENGTH_SHORT).show();
    }

    static void restoreScheduledReminder(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long whenUtc = p.getLong(KEY_WHEN, 0L);
        if (whenUtc <= System.currentTimeMillis()) return;
        enqueueAlarm(
            context,
            whenUtc,
            p.getString(KEY_TITLE, context.getString(R.string.reminder_default_title)),
            p.getString(KEY_BODY, context.getString(R.string.reminder_default_body))
        );
    }

    private static void enqueueAlarm(Context context, long whenUtc, String title, String body) {
        Intent i = new Intent(context, ReminderReceiver.class);
        i.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        i.putExtra(ReminderReceiver.EXTRA_BODY, body);
        PendingIntent pi = PendingIntent.getBroadcast(
            context, 2401, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long delay = Math.max(5000L, whenUtc - System.currentTimeMillis());
        am.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + delay,
            pi);
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
        return lang.toLowerCase(Locale.ROOT).startsWith("ta");
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
