package com.tnfisheries.ornamentalfish;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_BODY = "body";
    public static final String CHANNEL_ID = "of_revision";
    private static final int NOTIFY_ID = 2501;

    @Override
    public void onReceive(Context context, Intent intent) {
        ensureChannel(context);

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
            context, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = intent.getStringExtra(EXTRA_TITLE);
        String body = intent.getStringExtra(EXTRA_BODY);
        if (title == null || title.isEmpty()) {
            title = context.getString(R.string.reminder_default_title);
        }
        if (body == null) body = "";

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(context, CHANNEL_ID);
        } else {
            b = new Notification.Builder(context);
        }
        Notification n = b
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new Notification.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build();

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try {
                nm.notify(NOTIFY_ID, n);
            } catch (SecurityException ignored) {
            }
        }
    }

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }
}
