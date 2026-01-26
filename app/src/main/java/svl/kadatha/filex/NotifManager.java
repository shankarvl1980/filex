package svl.kadatha.filex;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotifManager {

    public final static String CHANNEL_ID = "svl.kadatha.filex.file_activities_channel_id";
    public final static CharSequence CHANNEL_NAME = "File Management";

    final int pending_intent_flag;
    private final NotificationCompat.Builder notification_builder; // keep for your existing usage
    private final NotificationManager nm;
    private final Context context;

    public NotifManager(Context context) {
        this.context = context;
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notification_builder = new NotificationCompat.Builder(context, CHANNEL_ID);

        int priority = Notification.PRIORITY_LOW;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String description = "This notification indicates long running file activities";
            NotificationChannel notification_channel =
                    new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            notification_channel.setDescription(description);
            notification_channel.setSound(null, null);
            nm.createNotificationChannel(notification_channel);
        }

        // Keep your defaults for "normal" notifications
        notification_builder
                .setSmallIcon(R.drawable.app_icon_notification)
                .setAutoCancel(true)
                .setPriority(priority)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);

        // BUGFIX: you were using CANCEL_CURRENT for < M which can break updates; UPDATE_CURRENT is better
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pending_intent_flag = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            pending_intent_flag = PendingIntent.FLAG_UPDATE_CURRENT;
        }
    }

    // ---------------- existing functions (unchanged signature) ----------------

    public void notify(String notification_content_line, int paste_notification_id) {
        notification_builder.setContentText(notification_content_line)
                .setContentIntent(null)
                .setProgress(0, 0, false)     // clear progress if previously set
                .setOngoing(false);           // clear ongoing if previously set
        nm.notify(paste_notification_id, notification_builder.build());
    }

    public Notification buildADPPActivity1(String action, String notification_content_line, int notification_id) {
        Intent intent = new Intent(context, ArchiveDeletePasteProgressActivity1.class);
        intent.setAction(action);
        PendingIntent pi = PendingIntent.getActivity(context, notification_id, intent, pending_intent_flag);

        // IMPORTANT: ADPP foreground should be ongoing + onlyAlertOnce
        notification_builder
                .setContentIntent(pi)
                .setContentText(notification_content_line)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false) // only for this foreground notification
                .setProgress(0, 0, true); // start indeterminate; will update later

        return notification_builder.build();
    }

    public Notification buildADPPActivity2(String action, String notification_content_line, int notification_id) {
        Intent intent = new Intent(context, ArchiveDeletePasteProgressActivity2.class);
        intent.setAction(action);
        PendingIntent pi = PendingIntent.getActivity(context, notification_id, intent, pending_intent_flag);

        notification_builder
                .setContentIntent(pi)
                .setContentText(notification_content_line)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setProgress(0, 0, true);

        return notification_builder.build();
    }

    public Notification buildADPPActivity3(String action, String notification_content_line, int notification_id) {
        Intent intent = new Intent(context, ArchiveDeletePasteProgressActivity3.class);
        intent.setAction(action);
        PendingIntent pi = PendingIntent.getActivity(context, notification_id, intent, pending_intent_flag);

        notification_builder
                .setContentIntent(pi)
                .setContentText(notification_content_line)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setProgress(0, 0, true);

        return notification_builder.build();
    }

    public Notification build(String notification_content_line, int notification_id) {
        notification_builder
                .setContentText(notification_content_line)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true);
        return notification_builder.build();
    }

    // ---------------- NEW: progress update methods for ADPP 1/2/3 ----------------
    // Call these from the Service on progress updates

    public void updateADPPActivity1Progress(int notification_id, String action,
                                            String title, String line,
                                            int percentOrMinus1,
                                            String sizeLineOrNull) {
        updateADPPProgress(notification_id, ArchiveDeletePasteProgressActivity1.class,
                action, title, line, percentOrMinus1, sizeLineOrNull);
    }

    public void updateADPPActivity2Progress(int notification_id, String action,
                                            String title, String line,
                                            int percentOrMinus1,
                                            String sizeLineOrNull) {
        updateADPPProgress(notification_id, ArchiveDeletePasteProgressActivity2.class,
                action, title, line, percentOrMinus1, sizeLineOrNull);
    }

    public void updateADPPActivity3Progress(int notification_id, String action,
                                            String title, String line,
                                            int percentOrMinus1,
                                            String sizeLineOrNull) {
        updateADPPProgress(notification_id, ArchiveDeletePasteProgressActivity3.class,
                action, title, line, percentOrMinus1, sizeLineOrNull);
    }

    private void updateADPPProgress(int notification_id, Class<?> activityCls,
                                    String action, String title, String line,
                                    int percentOrMinus1,
                                    String sizeLineOrNull) {

        Intent intent = new Intent(context, activityCls);
        intent.setAction(action);
        PendingIntent pi = PendingIntent.getActivity(context, notification_id, intent, pending_intent_flag);

        boolean determinate = percentOrMinus1 >= 0;

        String collapsedTitle = title;
        if (determinate) collapsedTitle = title + " • " + percentOrMinus1 + "%";

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(Notification.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pi)
                .setContentTitle(collapsedTitle)
                .setContentText(line); // filename / current item (collapsed)

        // Expanded body (big text)
        if (sizeLineOrNull != null && !sizeLineOrNull.isEmpty()) {
            String expanded = line + "\n" + sizeLineOrNull;
            b.setStyle(new NotificationCompat.BigTextStyle().bigText(expanded));
        } else {
            // keep expanded same as collapsed if you don't have sizeLine
            b.setStyle(new NotificationCompat.BigTextStyle().bigText(line));
        }

        if (determinate) {
            b.setProgress(100, percentOrMinus1, false);
        } else {
            b.setProgress(0, 0, true);
        }

        nm.notify(notification_id, b.build());
    }
}
