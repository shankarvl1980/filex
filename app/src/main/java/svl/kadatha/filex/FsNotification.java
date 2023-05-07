/*
Copyright 2011-2013 Pieter Pareit

This file is part of SwiFTP.

SwiFTP is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SwiFTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
*/

package svl.kadatha.filex;

import static android.content.Context.NOTIFICATION_SERVICE;

import static svl.kadatha.filex.Global.TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;



import java.net.InetAddress;

import svl.kadatha.filex.ftpserver.ftp.FsService;
import svl.kadatha.filex.ftpserver.ftp.FsSettings;
import timber.log.Timber;


public class FsNotification {

    public static final int NOTIFICATION_ID = 7890;
    public static final String CHANNEL_ID = "svl.kadatha.filex.ftp_server_channel_id";
    public final static CharSequence CHANNEL_NAME="FTP Server";
    public static Notification setupNotification(Context context) {
        Timber.tag(TAG).d("Setting up the notification");
        // Get NotificationManager reference
        NotificationManager nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        // get ip address
        InetAddress address = FsService.getLocalInetAddress();
        String ipText;
        if (address == null) {
            ipText = "-";
        } else {
            ipText = "ftp://" + address.getHostAddress() + ":"
                    + FsSettings.getPortNumber() + "/";
        }

        // Instantiate a Notification
//        int icon = R.mipmap.notification;
        CharSequence tickerText = String.format(context.getString(R.string.notification_server_starting), ipText);
        long when = System.currentTimeMillis();

        // Define Notification's message and Intent
        CharSequence contentTitle = context.getString(R.string.notification_title);
        CharSequence contentText = ipText;

        int pending_intent_flag=(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_CANCEL_CURRENT;

        Intent notificationIntent = new Intent(context, FtpServerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, pending_intent_flag);

        int stopIcon = android.R.drawable.ic_menu_close_clear_cancel;
        CharSequence stopText = context.getString(R.string.notification_stop_text);
        Intent stopIntent = new Intent(context, FsService.class);
        stopIntent.setAction(FsService.ACTION_REQUEST_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(context, 0,
                stopIntent, PendingIntent.FLAG_ONE_SHOT | pending_intent_flag);

//        int preferenceIcon = android.R.drawable.ic_menu_preferences;
//        CharSequence preferenceText = context.getString(R.string.notif_settings_text);
//        Intent preferenceIntent = new Intent(context, FtpServerActivity.class);
//        preferenceIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        PendingIntent preferencePendingIntent = PendingIntent.getActivity(context, 0,
//                preferenceIntent, pending_intent_flag);

        int priority = Notification.PRIORITY_LOW;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String description = "This notification shows the current state of the FTP Server";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription(description);
            channel.setSound(null,null);
            nm.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(context,CHANNEL_ID)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.app_icon)
                .setTicker(tickerText)
                .setWhen(when)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(priority)
                .addAction(stopIcon, stopText, stopPendingIntent)
                .setShowWhen(false)
                .build();

        // Pass Notification to NotificationManager
        return notification;
    }

}
