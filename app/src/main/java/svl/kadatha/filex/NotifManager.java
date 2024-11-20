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
    private final NotificationCompat.Builder notification_builder;
    private final NotificationManager nm;
    private final Context context;

    public NotifManager(Context context) {

        this.context = context;
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notification_builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        int priority = Notification.PRIORITY_LOW;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String description = "This notification indicates long running file activities";
            NotificationChannel notification_channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            notification_channel.setDescription(description);
            notification_channel.setSound(null, null);
            nm.createNotificationChannel(notification_channel);

        }
        notification_builder
                .setSmallIcon(R.drawable.app_icon_notification)
                .setAutoCancel(true)
                .setPriority(priority)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);

        pending_intent_flag = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_CANCEL_CURRENT;

    }


    public void notify(String notification_content_line, int paste_notification_id) {
        notification_builder.setContentText(notification_content_line)
                .setContentIntent(null);
        nm.notify(paste_notification_id, notification_builder.build());
    }

    public Notification buildADPPActivity1(String action, String notification_content_line, int notification_id) {
        Intent intent = new Intent(context, ArchiveDeletePasteProgressActivity1.class);
        intent.setAction(action);
        PendingIntent pi = PendingIntent.getActivity(context, notification_id, intent, pending_intent_flag);
        notification_builder.setContentIntent(pi);
        notification_builder.setContentText(notification_content_line);
        return notification_builder.build();
    }

    public Notification buildADPPActivity2(String action, String notification_content_line, int notification_id) {
        Intent intent = new Intent(context, ArchiveDeletePasteProgressActivity2.class);
        intent.setAction(action);
        PendingIntent pi = PendingIntent.getActivity(context, notification_id, intent, pending_intent_flag);
        notification_builder.setContentIntent(pi);
        notification_builder.setContentText(notification_content_line);
        return notification_builder.build();
    }

    public Notification buildADPPActivity3(String action, String notification_content_line, int notification_id) {
        Intent intent = new Intent(context, ArchiveDeletePasteProgressActivity3.class);
        intent.setAction(action);
        PendingIntent pi = PendingIntent.getActivity(context, notification_id, intent, pending_intent_flag);
        notification_builder.setContentIntent(pi);
        notification_builder.setContentText(notification_content_line);
        return notification_builder.build();
    }

    public Notification build(String notification_content_line, int notification_id) {
        notification_builder.setContentText(notification_content_line);
        return notification_builder.build();
    }
}
