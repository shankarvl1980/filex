package svl.kadatha.filex;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotifManager
{
	
	private final NotificationCompat.Builder notification_builder;
    private final NotificationManager nm;
	private final Context context;
    //int paste_notification_id,delete_notification_id,archive_notification_id;
	//NotificationCompat.InboxStyle inbox_style=new NotificationCompat.InboxStyle();
	
	NotifManager(Context context)
	{
		
		this.context=context;
		nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channel_id = "nc";
        notification_builder=new NotificationCompat.Builder(context, channel_id);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
		{
            NotificationChannel notification_channel = new NotificationChannel(channel_id, "notification_channel", NotificationManager.IMPORTANCE_LOW);
			notification_channel.enableLights(true);
			notification_channel.setDescription("notification_channel");
			notification_channel.setSound(null,null);
			nm.createNotificationChannel(notification_channel);

		}
		notification_builder
			.setSmallIcon(R.drawable.app_icon)
			.setAutoCancel(true);
		
	}
	

	public void notify(String notification_content_line, int paste_notification_id)
	{
		notification_builder.setContentText(notification_content_line)
			.setContentIntent(null);
		nm.notify(paste_notification_id,notification_builder.build());
	}

	public Notification build1(String action,String notification_content_line, int notification_id)
	{
		Intent intent=new Intent(context,ArchiveDeletePasteProgressActivity1.class);
		intent.setAction(action);
		PendingIntent pi=PendingIntent.getActivity(context,notification_id,intent,PendingIntent.FLAG_CANCEL_CURRENT);
		notification_builder.setContentIntent(pi);
		notification_builder.setContentText(notification_content_line);
		return notification_builder.build();
	}
	
	public Notification build2(String action,String notification_content_line, int notification_id)
	{
		Intent intent=new Intent(context,ArchiveDeletePasteProgressActivity2.class);
		intent.setAction(action);
		PendingIntent pi=PendingIntent.getActivity(context,notification_id,intent,PendingIntent.FLAG_CANCEL_CURRENT);
		notification_builder.setContentIntent(pi);
		notification_builder.setContentText(notification_content_line);
		return notification_builder.build();
	}
	
	public Notification build3(String action,String notification_content_line, int notification_id)
	{
		Intent intent=new Intent(context,ArchiveDeletePasteProgressActivity3.class);
		intent.setAction(action);
		PendingIntent pi=PendingIntent.getActivity(context,notification_id,intent,PendingIntent.FLAG_CANCEL_CURRENT);
		notification_builder.setContentIntent(pi);
		notification_builder.setContentText(notification_content_line);
		return notification_builder.build();
	}
	
	public Notification build(String notification_content_line, int notification_id)
	{
		notification_builder.setContentText(notification_content_line);
		return notification_builder.build();
	}
		
	
}
