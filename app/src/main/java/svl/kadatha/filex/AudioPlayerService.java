package svl.kadatha.filex;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



public class AudioPlayerService extends Service
{
	private Binder binder=new AudioBinder();
	public boolean prepared,playmode,stopped,completed;
	public int total_duration;
	private Context context;

	private boolean isReadPermissionGranted;

	private boolean ongoingcall=false;
	static List<AudioPOJO> AUDIO_QUEUED_ARRAY=new ArrayList<>();
	static int CURRENT_PLAY_NUMBER;
	private final int notification_id=808;
	public int current_position;


	public AudioPOJO current_audio;
	private NotificationPanel nPanel;
	public Handler handler,handler_media_preparation,handler_broadcast;
	public AudioPlayerServiceHandlerThread audioPlayerServiceHandlerThread;
	private MediaPlayerServicePrepareListener mediaPlayerServicePrepareListener;
	private AudioPlayerServiceBroadCastListener audioPlayerServiceBroadCastListener;
	private AudioFocusRequest audioFocusRequest;

	static final int INIT_MEDIA_PLAYER=0;
	static final int START=1;
	static final int PAUSE=2;
	static final int STOP=3;
	static final int GOTO_NEXT=4;
	static final int GOTO_PREVIOUS=5;
	static final int MOVE_BACKWARD=6;
	static final int MOVE_FORWARD=7;
	//static final int SEEK_TO=8;
	//static final int GET_CURRENT_POSITION=9;
	static final int REQUEST_AUDIO_FOCUS=10;
	static final int RELEASE_AUDIO_FOCUS=11;
	static final int BACK=12;



	@Override
	public void onCreate()
	{
		// TODO: Implement this method
		super.onCreate();
		context=this;
		audioPlayerServiceHandlerThread=new AudioPlayerServiceHandlerThread(this);
		audioPlayerServiceHandlerThread.start();
		audioPlayerServiceHandlerThread.onLooperPreparation();
		handler_media_preparation=new Handler();
		handler_broadcast=new Handler();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			audioFocusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setOnAudioFocusChangeListener(audioPlayerServiceHandlerThread).build();
		}

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// TODO: Implement this method
		Uri data=intent.getData();
		isReadPermissionGranted=isReadPhonePermissionGranted();
		if(!audioPlayerServiceHandlerThread.request_focus())
		{
			stopSelf();
		}

		if(data!=null)
		{
			Bundle bundle=new Bundle();
			bundle.putParcelable("data",data);
			Message message=handler.obtainMessage(INIT_MEDIA_PLAYER);
			message.setData(bundle);
			message.sendToTarget();
			current_audio=AudioPlayerActivity.AUDIO_FILE;
			nPanel=new NotificationPanel(this);
			startForeground(notification_id,nPanel.get_notification());
		}
		else
		{
			int action = intent.getIntExtra("DO",0);
			if(action!=0)
			{
				switch (action) {
					case GOTO_PREVIOUS:
						handler.obtainMessage(GOTO_PREVIOUS).sendToTarget();
						break;
					case PAUSE:
						if (prepared && !playmode) {
							handler.obtainMessage(START).sendToTarget();
						} else if (prepared && playmode) {
							handler.obtainMessage(PAUSE).sendToTarget();
						}
						break;
					case GOTO_NEXT:
						handler.obtainMessage(GOTO_NEXT).sendToTarget();
						break;
					case STOP:
						handler.obtainMessage(STOP).sendToTarget();
						break;
				}

			}

		}

		return START_NOT_STICKY;
	}

	private boolean isReadPhonePermissionGranted()
	{
		return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)== PackageManager.PERMISSION_GRANTED;
	}

	private class AudioPlayerServiceHandlerThread extends HandlerThread implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,MediaPlayer.OnSeekCompleteListener,
			MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener,AudioManager.OnAudioFocusChangeListener
	{
        private AudioManager audio_manager;
		private final AudioPlayerService audioPlayerService;
		private MediaPlayer mp;

        AudioPlayerServiceHandlerThread(AudioPlayerService audioPlayerService) {
			super("handlerthread");
			this.audioPlayerService=audioPlayerService;

            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if(Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP_MR1)
			{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					if(isReadPermissionGranted)
					{
						telephonyManager.registerTelephonyCallback(getMainExecutor(),new CustomCallback());
					}

				}
				else
				{
					telephonyManager.listen(new PhoneStateListener()
					{
						public void onCallStateChanged(int state, String phonenumber)
						{
							switch(state)
							{
								case TelephonyManager.CALL_STATE_OFFHOOK:
								case TelephonyManager.CALL_STATE_RINGING:
									if(mp!=null)
									{
										handler.obtainMessage(PAUSE).sendToTarget();
										ongoingcall=true;
									}
									break;

								case TelephonyManager.CALL_STATE_IDLE:
									if(ongoingcall)
									{
										ongoingcall=false;
										handler.obtainMessage(START).sendToTarget();

									}
							}
						}
					},PhoneStateListener.LISTEN_CALL_STATE);
				}

			}

		}

		@RequiresApi(api = Build.VERSION_CODES.S)
		private class CustomCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener
		{

			@Override
			public void onCallStateChanged(int i) {
				switch(i)
				{
					case TelephonyManager.CALL_STATE_OFFHOOK:
					case TelephonyManager.CALL_STATE_RINGING:
						if(mp!=null)
						{
							handler.obtainMessage(PAUSE).sendToTarget();
							ongoingcall=true;
						}
						break;

					case TelephonyManager.CALL_STATE_IDLE:
						if(ongoingcall)
						{
							ongoingcall=false;
							handler.obtainMessage(START).sendToTarget();

						}
				}
			}
		}

		public void onLooperPreparation()
		{
			handler=new Handler(getLooper(), new Handler.Callback() {
				@Override
				public boolean handleMessage(@NonNull Message message) {

					switch (message.what)
					{
						case INIT_MEDIA_PLAYER:
							Bundle bundle=message.getData();
							if(bundle!=null)
							{
								initMediaPlayer(bundle.getParcelable("data"),audioPlayerService);
							}
							break;
						case START:
							start_();
							break;
						case PAUSE:
							pause();
							break;
						case STOP:
							stop_();
							break;
						case GOTO_NEXT:
							goto_next();
							break;
						case GOTO_PREVIOUS:
							goto_previous();
							break;
						case MOVE_BACKWARD:
							move_backward();
							break;
						case MOVE_FORWARD:
							move_forward();
							break;
						case REQUEST_AUDIO_FOCUS:
							request_focus();
							break;
						case RELEASE_AUDIO_FOCUS:
							releaseAudioFocus();
							break;

					}


					return true;
				}
			});

		}


		private void initMediaPlayer(final Uri data, AudioPlayerService audioPlayerService)
		{
			if(data==null)
			{
				return;
			}
			if(mp!=null)
			{
				if(prepared)
				{
					stopped=true;
					prepared=false;
					mp.stop();
					mp.reset();
				}
				else
				{
					return;
				}
			}

			mp=new MediaPlayer();
			mp.setOnCompletionListener(this);
			mp.setOnErrorListener(this);
			mp.setOnBufferingUpdateListener(this);
			mp.setOnSeekCompleteListener(this);
			mp.setOnInfoListener(this);
			mp.setOnPreparedListener(this);
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);

			try
			{
				mp.setDataSource(audioPlayerService,data);
			}
			catch(IOException e)
			{
				stop_();
				return;
			}
			mp.prepareAsync();
		}



		private void start_()
		{
			if(prepared)
			{
				if(request_focus())
				{
					mp.start();
					playmode=true;
					completed=false;
					nPanel=new NotificationPanel(audioPlayerService);
					nPanel.notify_notification();
					handler_broadcast.post(new Runnable() {
						@Override
						public void run() {
							if(audioPlayerServiceBroadCastListener!=null)
							{
								audioPlayerServiceBroadCastListener.onBroadcast(START);
							}
						}
					});
				}
			}
		}

		private void pause()
		{
			if(prepared)
			{
				mp.pause();
				playmode=false;
				nPanel=new NotificationPanel(audioPlayerService);
				nPanel.notify_notification();
				handler_broadcast.post(new Runnable() {
					@Override
					public void run() {
						if(audioPlayerServiceBroadCastListener!=null)
						{
							audioPlayerServiceBroadCastListener.onBroadcast(PAUSE);
						}
					}
				});
			}
		}

		private void stop_()
		{
			stopped=true;
			prepared=false;   //this should before setting mp to null
			if(mp!=null)
			{
				mp.stop();
				mp.release();
			}
			mp=null;

			CURRENT_PLAY_NUMBER=0;
			AUDIO_QUEUED_ARRAY=new ArrayList<>();
			current_audio=null;
			AudioPlayerActivity.AUDIO_FILE=null;
			handler_broadcast.post(new Runnable() {
				@Override
				public void run() {
					if(audioPlayerServiceBroadCastListener!=null)
					{
						audioPlayerServiceBroadCastListener.onBroadcast(AudioPlayerService.STOP);
					}
				}
			});

			stopForeground(true);
			stopSelf();
		}

		private void goto_next()
		{
			CURRENT_PLAY_NUMBER++;
			if(AUDIO_QUEUED_ARRAY.size()==0 || CURRENT_PLAY_NUMBER>AUDIO_QUEUED_ARRAY.size()-1 || CURRENT_PLAY_NUMBER<0)
			{
				CURRENT_PLAY_NUMBER=AUDIO_QUEUED_ARRAY.size()-1;
				return;
			}

			current_audio=AUDIO_QUEUED_ARRAY.get(CURRENT_PLAY_NUMBER);
			AudioPlayerActivity.AUDIO_FILE=current_audio;
			handler_broadcast.post(new Runnable() {
				@Override
				public void run() {
					if(audioPlayerServiceBroadCastListener!=null)
					{
						audioPlayerServiceBroadCastListener.onBroadcast(AudioPlayerService.GOTO_NEXT);
					}
				}
			});


			Uri data=null;
			File f=new File(current_audio.getData());
			if(f.exists())
			{
				data= FileProvider.getUriForFile(context,Global.FILEX_PACKAGE+".provider",f);
				initMediaPlayer(data,audioPlayerService);
			}
			else
			{
				AUDIO_QUEUED_ARRAY.remove(CURRENT_PLAY_NUMBER);
				CURRENT_PLAY_NUMBER--;
				goto_next();
			}

		}

		private void goto_previous()
		{
			CURRENT_PLAY_NUMBER--;
			if(AudioPlayerService.AUDIO_QUEUED_ARRAY.size()==0 || CURRENT_PLAY_NUMBER<0 || CURRENT_PLAY_NUMBER> AUDIO_QUEUED_ARRAY.size()-1)
			{
				CURRENT_PLAY_NUMBER=0;
				return;

			}

			current_audio=AudioPlayerService.AUDIO_QUEUED_ARRAY.get(CURRENT_PLAY_NUMBER);
			AudioPlayerActivity.AUDIO_FILE=current_audio;

			handler_broadcast.post(new Runnable() {
				@Override
				public void run() {
					if(audioPlayerServiceBroadCastListener!=null)
					{
						audioPlayerServiceBroadCastListener.onBroadcast(AudioPlayerService.GOTO_PREVIOUS);
					}
				}
			});


			Uri data=null;
			File f=new File(current_audio.getData());
			if(f.exists())
			{
				data=FileProvider.getUriForFile(context,Global.FILEX_PACKAGE+".provider",f);
				initMediaPlayer(data,audioPlayerService);
			}
			else
			{
				AUDIO_QUEUED_ARRAY.remove(CURRENT_PLAY_NUMBER);
				CURRENT_PLAY_NUMBER--;
				goto_previous();
			}
		}


		public void seek_to(int counter)
		{
			if(prepared)
			{
				mp.seekTo(counter);
			}
		}

		private void move_backward()
		{
			if(prepared)
			{
				int backward_pos=mp.getCurrentPosition()-5000;
				mp.seekTo(Math.max(backward_pos, 0));
			}

		}

		private void move_forward()
		{
			if(prepared)
			{
				int forward_pos=mp.getCurrentPosition()+5000;
				mp.seekTo(Math.min(forward_pos, total_duration));
			}
		}

		public int get_current_position()
		{
			if(prepared)
			{
				current_position=mp.getCurrentPosition();
			}
			else
			{
				current_position=0;
			}
			return current_position;
		}


		@Override
		public void onPrepared(MediaPlayer p1)
		{
			// TODO: Implement this method
			prepared=true;
			stopped=false;
			total_duration=mp.getDuration();

			handler.obtainMessage(START).sendToTarget();
			handler_media_preparation.post(new Runnable() {
				@Override
				public void run() {
					if(mediaPlayerServicePrepareListener!=null)
					{
						mediaPlayerServicePrepareListener.onMediaPrepare();
					}
				}
			});

		}


		@Override
		public void onSeekComplete(MediaPlayer p1)
		{
			// TODO: Implement this method

		}

		@Override
		public void onAudioFocusChange(int focusState)
		{
			// TODO: Implement this method
			switch(focusState)
			{
				case AudioManager.AUDIOFOCUS_GAIN:

					if(mp!=null)
					{
						mp.setVolume(1f,1f);
					}
					break;

				case AudioManager.AUDIOFOCUS_LOSS:
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					if(mp!=null)
					{
						handler.obtainMessage(PAUSE).sendToTarget();
					}

					handler_broadcast.post(new Runnable() {
						@Override
						public void run() {
							if(audioPlayerServiceBroadCastListener!=null)
							{
								audioPlayerServiceBroadCastListener.onBroadcast(AudioPlayerService.PAUSE);
							}
						}
					});

					break;

				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					if(mp!=null)
					{
						mp.setVolume(0.2f,0.2f);
					}
					break;
			}

		}



		@Override
		public void onBufferingUpdate(MediaPlayer p1, int p2)
		{
			// TODO: Implement this method
		}

		@Override
		public boolean onError(MediaPlayer p1, int p2, int p3)
		{
			// TODO: Implement this method
			switch(p2)
			{
				case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:

				case MediaPlayer.MEDIA_ERROR_SERVER_DIED:

				case MediaPlayer.MEDIA_ERROR_UNKNOWN:

					break;

			}

			handler.obtainMessage(STOP).sendToTarget();
			return false;
		}

		@Override
		public void onCompletion(MediaPlayer p1)
		{
			// TODO: Implement this method
			completed=true;
			playmode=false;
			nPanel=new NotificationPanel(audioPlayerService);
			nPanel.notify_notification();
			handler.obtainMessage(GOTO_NEXT).sendToTarget();

		}

		@Override
		public boolean onInfo(MediaPlayer p1, int p2, int p3)
		{
			// TODO: Implement this method
			return false;
		}

		public boolean request_focus()
		{
			audio_manager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
			int result;
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
			{
				result=audio_manager.requestAudioFocus(audioFocusRequest);
			}
			else
			{
				result=audio_manager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
			}


			return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
		}

		public void releaseAudioFocus()
		{
			if(audio_manager!=null)
			{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					audio_manager.abandonAudioFocusRequest(audioFocusRequest);
				}
				else
				{
					audio_manager.abandonAudioFocus(this);
				}

			}

		}

	}

	interface MediaPlayerServicePrepareListener
	{
		void onMediaPrepare();
	}

	public void setMediaPlayerPrepareListener(MediaPlayerServicePrepareListener listener)
	{
		mediaPlayerServicePrepareListener=listener;
	}

	interface  AudioPlayerServiceBroadCastListener
	{
		void onBroadcast(int number);
	}

	public void setAudioPlayerServiceBroadcastListener(AudioPlayerServiceBroadCastListener listener)
	{
		audioPlayerServiceBroadCastListener=listener;
	}

	public void removeAudioPlayerServiceBroadcastListener()
	{
		audioPlayerServiceBroadCastListener=null;
	}



	public int get_duration()
	{
		return total_duration;
	}

	public int get_current_position()
	{
		return audioPlayerServiceHandlerThread.get_current_position();
	}

	public void seek_to(int counter)
	{
		audioPlayerServiceHandlerThread.seek_to(counter);
	}


	@Override
	public IBinder onBind(Intent p1)
	{
		// TODO: Implement this method
		if(binder==null)
		{
			binder=new AudioBinder();
		}

		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy()
	{
		// TODO: Implement this method
		super.onDestroy();
		if(audioPlayerServiceHandlerThread.mp!=null)
		{
			handler.obtainMessage(STOP).sendToTarget();
		}
		audioPlayerServiceHandlerThread.releaseAudioFocus();
		audioPlayerServiceHandlerThread.quit();

	}

	class AudioBinder extends Binder
	{
		public AudioPlayerService getService()
		{
			return AudioPlayerService.this;
		}
	}

	public class NotificationPanel
	{

		private final Context parent;
		private final android.app.NotificationManager nManager;
		private final androidx.core.app.NotificationCompat.Builder nBuilder;

        public NotificationPanel(Context parent)
		{
			// TODO Auto-generated constructor stub
			this.parent = parent;
			nManager = (android.app.NotificationManager) parent.getSystemService(Context.NOTIFICATION_SERVICE);
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
			{

                NotificationChannel notification_channel = new NotificationChannel("asc", "notification_channel", NotificationManager.IMPORTANCE_LOW);
				notification_channel.enableLights(true);
				notification_channel.setDescription("nc");
				notification_channel.setSound(null,null);
				nManager.createNotificationChannel(notification_channel);
			}
			nBuilder = new NotificationCompat.Builder(parent,"asc");
			nBuilder.setContentTitle(AudioPlayerActivity.AUDIO_FILE==null ? "FileX Manager":AudioPlayerActivity.AUDIO_FILE.getTitle())
				.setSmallIcon(R.drawable.app_icon)
				.setAutoCancel(true)
				.setStyle(new NotificationCompat.DecoratedCustomViewStyle());

            RemoteViews remoteView = new RemoteViews(parent.getPackageName(), R.layout.audio_notification_view);
			if(playmode)
			{
				remoteView.setImageViewResource(R.id.audio_notification_play_pause,R.drawable.dark_pause_icon);
			}
			else
			{
				remoteView.setImageViewResource(R.id.audio_notification_play_pause,R.drawable.dark_play_icon);
			}

			if(CURRENT_PLAY_NUMBER==0)
			{
				remoteView.setBoolean(R.id.audio_notification_previous,"setEnabled",false);
				remoteView.setInt(R.id.audio_notification_previous,"setAlpha",100);
			}
			if(CURRENT_PLAY_NUMBER==AUDIO_QUEUED_ARRAY.size()-1)
			{
				remoteView.setBoolean(R.id.audio_notification_next,"setEnabled",false);
				remoteView.setInt(R.id.audio_notification_next,"setAlpha",100);
			}

			String name="";
			if(current_audio!=null)
			{
				name=current_audio.getTitle();
			}
			else if(AudioPlayerActivity.AUDIO_FILE!=null)
			{
				name=AudioPlayerActivity.AUDIO_FILE.getTitle();
			}

			remoteView.setTextViewText(R.id.audio_notification_audio_name,name);

			//set the button listeners
			setListeners(remoteView);
			nBuilder.setCustomBigContentView(remoteView);
		}

		public Notification get_notification()
		{
			return nBuilder.build();
		}
		public void notify_notification()
		{
			nManager.notify(notification_id, nBuilder.build());
		}

		public void setListeners(RemoteViews view)
		{
			int pending_intent_flag=(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_CANCEL_CURRENT;

			//listener 1
			Intent previous = new Intent(parent,AudioPlayerService.class);
			previous.putExtra("DO", GOTO_PREVIOUS);
			PendingIntent prev = PendingIntent.getService(parent, 0, previous, pending_intent_flag);
			view.setOnClickPendingIntent(R.id.audio_notification_previous, prev);

			//listener 2
			Intent pause = new Intent(parent, AudioPlayerService.class);
			pause.putExtra("DO", PAUSE);
			PendingIntent start = PendingIntent.getService(parent, 1, pause, pending_intent_flag);
			view.setOnClickPendingIntent(R.id.audio_notification_play_pause, start);

			//listener 3
			Intent next = new Intent(parent, AudioPlayerService.class);
			next.putExtra("DO", GOTO_NEXT);
			PendingIntent nxt = PendingIntent.getService(parent, 2, next, pending_intent_flag);
			view.setOnClickPendingIntent(R.id.audio_notification_next, nxt);

			//listener 4
			Intent cancel = new Intent(parent, AudioPlayerService.class);
			cancel.putExtra("DO", STOP);
			PendingIntent close = PendingIntent.getService(parent, 3, cancel, pending_intent_flag);
			view.setOnClickPendingIntent(R.id.audio_notification_close, close);

			Intent back=new Intent(parent, AudioPlayerActivity.class);
			back.putExtra("Do",BACK);
			PendingIntent bck=PendingIntent.getActivity(parent,4,back,pending_intent_flag);
			view.setOnClickPendingIntent(R.id.audio_notification_back,bck);
		}
		public void notificationCancel()
		{
			nManager.cancel(notification_id);
		}
	}


}
