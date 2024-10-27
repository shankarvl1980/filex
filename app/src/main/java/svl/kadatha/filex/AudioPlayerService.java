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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class AudioPlayerService extends Service {
    public final static String CHANNEL_ID = "svl.kadatha.filex.audio_player_channel_id";
    public final static CharSequence CHANNEL_NAME = "Audio Player";
    static final int INIT_MEDIA_PLAYER = 0;
    static final int START = 1;
    static final int PAUSE = 2;
    static final int STOP = 3;
    static final int GOTO_NEXT = 4;
    static final int GOTO_PREVIOUS = 5;
    static final int MOVE_BACKWARD = 6;
    static final int MOVE_FORWARD = 7;
    static final int SEEK_TO = 8;
    //static final int GET_CURRENT_POSITION=9;
    static final int REQUEST_AUDIO_FOCUS = 10;
    static final int RELEASE_AUDIO_FOCUS = 11;
    static final int BACK = 12;
    static List<AudioPOJO> AUDIO_QUEUED_ARRAY = new ArrayList<>();
    static int CURRENT_PLAY_NUMBER;
    private final int notification_id = 808;
    public boolean prepared, playmode, stopped, completed;
    public int total_duration;
    public int current_position;
    public AudioPOJO current_audio;
    public Handler handler, handler_media_preparation, handler_broadcast;
    public AudioPlayerServiceHandlerThread audioPlayerServiceHandlerThread;
    private Binder binder = new AudioBinder();
    private Context context;
    private boolean isReadPermissionGranted;
    private boolean ongoingcall = false;
    private NotificationPanel nPanel;
    private MediaPlayerServicePrepareListener mediaPlayerServicePrepareListener;
    private AudioPlayerServiceBroadCastListener audioPlayerServiceBroadCastListener;
    private AudioFocusRequest audioFocusRequest;
    private MediaSessionCompat mediaSession;


    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        audioPlayerServiceHandlerThread = new AudioPlayerServiceHandlerThread(this);
        audioPlayerServiceHandlerThread.start();
        audioPlayerServiceHandlerThread.onLooperPreparation();
        handler_media_preparation = new Handler();
        handler_broadcast = new Handler();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setOnAudioFocusChangeListener(audioPlayerServiceHandlerThread).build();

        }
        mediaSession = new MediaSessionCompat(this, "AudioPlayerService");
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Uri data = intent.getData();
        isReadPermissionGranted = isReadPhonePermissionGranted();
        if (!audioPlayerServiceHandlerThread.request_focus()) {
            stopSelf();
        }

        if (data != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("data", data);
            Message message = handler.obtainMessage(INIT_MEDIA_PLAYER);
            message.setData(bundle);
            message.sendToTarget();
            current_audio = AudioPlayerActivity.AUDIO_FILE;
            nPanel = new NotificationPanel(this);
            startForeground(notification_id, nPanel.get_notification());
        } else {
            int action = intent.getIntExtra("DO", 0);
            if (action != 0) {
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

    private boolean isReadPhonePermissionGranted() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public void setMediaPlayerPrepareListener(MediaPlayerServicePrepareListener listener) {
        mediaPlayerServicePrepareListener = listener;
    }

    public void removeMediaPlayerPrepareListener() {
        mediaPlayerServicePrepareListener = null;
    }

    public void setAudioPlayerServiceBroadcastListener(AudioPlayerServiceBroadCastListener listener) {
        audioPlayerServiceBroadCastListener = listener;
    }

    public void removeAudioPlayerServiceBroadcastListener() {
        audioPlayerServiceBroadCastListener = null;
    }

    public int get_duration() {
        return total_duration;
    }

    public int get_current_position() {
        return audioPlayerServiceHandlerThread.get_current_position();
    }

    public void seek_to(int counter) {
        audioPlayerServiceHandlerThread.seek_to(counter);
    }

    @Override
    public IBinder onBind(Intent p1) {
        if (binder == null) {
            binder = new AudioBinder();
        }
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioPlayerServiceHandlerThread.mp != null) {
            handler.obtainMessage(STOP).sendToTarget();
        }
        audioPlayerServiceHandlerThread.releaseAudioFocus();
        audioPlayerServiceHandlerThread.quit();
        mediaSession.release();
    }

    private void updateMediaSessionMetadata(MediaPlayer mp) {
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mp.getDuration());
        // Add other metadata like title, artist, etc.
        mediaSession.setMetadata(metadataBuilder.build());
    }

    interface MediaPlayerServicePrepareListener {
        void onMediaPrepare();
    }

    interface AudioPlayerServiceBroadCastListener {
        void onBroadcast(int number);
    }

    private class AudioPlayerServiceHandlerThread extends HandlerThread implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
            MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {
        private final AudioPlayerService audioPlayerService;
        private AudioManager audio_manager;
        private MediaPlayer mp;

        AudioPlayerServiceHandlerThread(AudioPlayerService audioPlayerService) {
            super("handlerthread");
            this.audioPlayerService = audioPlayerService;

            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (isReadPermissionGranted) {
                        telephonyManager.registerTelephonyCallback(getMainExecutor(), new CustomCallback());
                    }

                } else {
                    telephonyManager.listen(new PhoneStateListener() {
                        public void onCallStateChanged(int state, String phonenumber) {
                            switch (state) {
                                case TelephonyManager.CALL_STATE_OFFHOOK:
                                case TelephonyManager.CALL_STATE_RINGING:
                                    if (mp != null) {
                                        handler.obtainMessage(PAUSE).sendToTarget();
                                        ongoingcall = true;
                                    }
                                    break;

                                case TelephonyManager.CALL_STATE_IDLE:
                                    if (ongoingcall) {
                                        ongoingcall = false;
                                        handler.obtainMessage(START).sendToTarget();

                                    }
                            }
                        }
                    }, PhoneStateListener.LISTEN_CALL_STATE);
                }
            }
        }

        public void onLooperPreparation() {
            handler = new Handler(getLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(@NonNull Message message) {

                    switch (message.what) {
                        case INIT_MEDIA_PLAYER:
                            Bundle bundle = message.getData();
                            if (bundle != null) {
                                initMediaPlayer(bundle.getParcelable("data"), audioPlayerService);
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
                        case SEEK_TO:
                            seek_to(message.arg1);
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

        private void initMediaPlayer(final Uri data, AudioPlayerService audioPlayerService) {
            if (data == null) {
                return;
            }
            if (mp != null) {
                if (prepared) {
                    stopped = true;
                    prepared = false;
                    mp.stop();
                    mp.reset();
                } else {
                    return;
                }
            }

            mp = new MediaPlayer();
            mp.setOnCompletionListener(this);
            mp.setOnErrorListener(this);
            mp.setOnBufferingUpdateListener(this);
            mp.setOnSeekCompleteListener(this);
            mp.setOnInfoListener(this);
            mp.setOnPreparedListener(this);
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);

            try {
                mp.setDataSource(audioPlayerService, data);
            } catch (IOException e) {
                stop_();
                return;
            }
            mp.prepareAsync();
        }

        private void updatePlaybackState() {
            long position = mp != null ? mp.getCurrentPosition() : 0;
            PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                            PlaybackStateCompat.ACTION_SEEK_TO)
                    .setState(playmode ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                            position, 1.0f);
            mediaSession.setPlaybackState(stateBuilder.build());
        }

        private void start_() {
            if (prepared) {
                if (request_focus()) {
                    mp.start();
                    playmode = true;
                    completed = false;
                    nPanel = new NotificationPanel(audioPlayerService);
                    nPanel.notify_notification();
                    handler_broadcast.post(new Runnable() {
                        @Override
                        public void run() {
                            if (audioPlayerServiceBroadCastListener != null) {
                                audioPlayerServiceBroadCastListener.onBroadcast(START);
                            }
                        }
                    });
                }
            }
            updatePlaybackState();
            nPanel.updatePlayPauseAction(true);
        }

        private void pause() {
            if (prepared) {
                mp.pause();
                playmode = false;
                nPanel = new NotificationPanel(audioPlayerService);
                nPanel.notify_notification();
                handler_broadcast.post(new Runnable() {
                    @Override
                    public void run() {
                        if (audioPlayerServiceBroadCastListener != null) {
                            audioPlayerServiceBroadCastListener.onBroadcast(PAUSE);
                        }
                    }
                });
                updatePlaybackState();
                nPanel.updatePlayPauseAction(false);
            }
        }

        private void stop_() {
            stopped = true;
            prepared = false;   //this should before setting mp to null
            if (mp != null) {
                mp.stop();
                mp.release();
            }
            mp = null;

            CURRENT_PLAY_NUMBER = 0;
            AUDIO_QUEUED_ARRAY = new ArrayList<>();
            current_audio = null;
            AudioPlayerActivity.AUDIO_FILE = null;
            handler_broadcast.post(new Runnable() {
                @Override
                public void run() {
                    if (audioPlayerServiceBroadCastListener != null) {
                        audioPlayerServiceBroadCastListener.onBroadcast(AudioPlayerService.STOP);
                    }
                }
            });

            stopForeground(true);
            stopSelf();

        }

        private void goto_next() {
            CURRENT_PLAY_NUMBER++;
            if (AUDIO_QUEUED_ARRAY.isEmpty() || CURRENT_PLAY_NUMBER > AUDIO_QUEUED_ARRAY.size() - 1 || CURRENT_PLAY_NUMBER < 0) {
                CURRENT_PLAY_NUMBER = AUDIO_QUEUED_ARRAY.size() - 1;
                return;
            }

            current_audio = AUDIO_QUEUED_ARRAY.get(CURRENT_PLAY_NUMBER);
            AudioPlayerActivity.AUDIO_FILE = current_audio;
            handler_broadcast.post(new Runnable() {
                @Override
                public void run() {
                    if (audioPlayerServiceBroadCastListener != null) {
                        audioPlayerServiceBroadCastListener.onBroadcast(AudioPlayerService.GOTO_NEXT);
                    }
                }
            });


            Uri data = null;
            File f = new File(current_audio.getData());
            if (f.exists()) {
                data = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", f);
                initMediaPlayer(data, audioPlayerService);
            } else {
                AUDIO_QUEUED_ARRAY.remove(CURRENT_PLAY_NUMBER);
                CURRENT_PLAY_NUMBER--;
                goto_next();
            }

        }

        private void goto_previous() {
            CURRENT_PLAY_NUMBER--;
            if (AudioPlayerService.AUDIO_QUEUED_ARRAY.isEmpty() || CURRENT_PLAY_NUMBER < 0 || CURRENT_PLAY_NUMBER > AUDIO_QUEUED_ARRAY.size() - 1) {
                CURRENT_PLAY_NUMBER = 0;
                return;

            }

            current_audio = AudioPlayerService.AUDIO_QUEUED_ARRAY.get(CURRENT_PLAY_NUMBER);
            AudioPlayerActivity.AUDIO_FILE = current_audio;

            handler_broadcast.post(new Runnable() {
                @Override
                public void run() {
                    if (audioPlayerServiceBroadCastListener != null) {
                        audioPlayerServiceBroadCastListener.onBroadcast(AudioPlayerService.GOTO_PREVIOUS);
                    }
                }
            });


            Uri data = null;
            File f = new File(current_audio.getData());
            if (f.exists()) {
                data = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", f);
                initMediaPlayer(data, audioPlayerService);
            } else {
                AUDIO_QUEUED_ARRAY.remove(CURRENT_PLAY_NUMBER);
                CURRENT_PLAY_NUMBER--;
                goto_previous();
            }
        }

        public void seek_to(int counter) {
            if (prepared) {
                mp.seekTo(counter);
                updatePlaybackState();
            }
        }

        private void move_backward() {
            if (prepared) {
                int backward_pos = mp.getCurrentPosition() - 10000;
                mp.seekTo(Math.max(backward_pos, 0));
            }
        }

        private void move_forward() {
            if (prepared) {
                int forward_pos = mp.getCurrentPosition() + 10000;
                mp.seekTo(Math.min(forward_pos, total_duration));
            }
        }

        public int get_current_position() {
            if (prepared) {
                current_position = mp.getCurrentPosition();
            } else {
                current_position = 0;
            }
            return current_position;
        }

        @Override
        public void onPrepared(MediaPlayer p1) {
            prepared = true;
            stopped = false;
            total_duration = mp.getDuration();
            updateMediaSessionMetadata(mp);
            handler.obtainMessage(START).sendToTarget();
            handler_media_preparation.post(new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayerServicePrepareListener != null) {
                        mediaPlayerServicePrepareListener.onMediaPrepare();
                    }
                }
            });

        }

        @Override
        public void onSeekComplete(MediaPlayer p1) {
            // TODO: Implement this method
        }

        @Override
        public void onAudioFocusChange(int focusState) {
            switch (focusState) {
                case AudioManager.AUDIOFOCUS_GAIN:

                    if (mp != null) {
                        mp.setVolume(1f, 1f);
                    }
                    break;

                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (mp != null) {
                        handler.obtainMessage(PAUSE).sendToTarget();
                    }

                    handler_broadcast.post(new Runnable() {
                        @Override
                        public void run() {
                            if (audioPlayerServiceBroadCastListener != null) {
                                audioPlayerServiceBroadCastListener.onBroadcast(AudioPlayerService.PAUSE);
                            }
                        }
                    });

                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (mp != null) {
                        mp.setVolume(0.2f, 0.2f);
                    }
                    break;
            }

        }

        @Override
        public void onBufferingUpdate(MediaPlayer p1, int p2) {
            // TODO: Implement this method
        }

        @Override
        public boolean onError(MediaPlayer p1, int p2, int p3) {
            switch (p2) {
                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:

                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:

                case MediaPlayer.MEDIA_ERROR_UNKNOWN:

                    break;

            }

            handler.obtainMessage(STOP).sendToTarget();
            return false;
        }

        @Override
        public void onCompletion(MediaPlayer p1) {
            completed = true;
            playmode = false;
            nPanel = new NotificationPanel(audioPlayerService);
            nPanel.notify_notification();
            handler.obtainMessage(GOTO_NEXT).sendToTarget();

        }

        @Override
        public boolean onInfo(MediaPlayer p1, int p2, int p3) {
            return false;
        }

        public boolean request_focus() {
            audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                result = audio_manager.requestAudioFocus(audioFocusRequest);
            } else {
                result = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }


            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }

        public void releaseAudioFocus() {
            if (audio_manager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audio_manager.abandonAudioFocusRequest(audioFocusRequest);
                } else {
                    audio_manager.abandonAudioFocus(this);
                }

            }

        }

        @RequiresApi(api = Build.VERSION_CODES.S)
        private class CustomCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {

            @Override
            public void onCallStateChanged(int i) {
                switch (i) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mp != null) {
                            handler.obtainMessage(PAUSE).sendToTarget();
                            ongoingcall = true;
                        }
                        break;

                    case TelephonyManager.CALL_STATE_IDLE:
                        if (ongoingcall) {
                            ongoingcall = false;
                            handler.obtainMessage(START).sendToTarget();

                        }
                }
            }
        }

    }

    class AudioBinder extends Binder {
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

    public class NotificationPanel {

        final int pending_intent_flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
        private final Context parent;
        private final android.app.NotificationManager nManager;
        private final androidx.core.app.NotificationCompat.Builder nBuilder;


        public NotificationPanel(Context parent) {
            this.parent = parent;
            nManager = (android.app.NotificationManager) parent.getSystemService(Context.NOTIFICATION_SERVICE);
            int priority = Notification.PRIORITY_HIGH;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String description = "This is to operate the audio player controls";
                NotificationChannel notification_channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                notification_channel.setDescription(description);
                notification_channel.setSound(null, null);
                nManager.createNotificationChannel(notification_channel);
            }

            Intent previous = new Intent(parent, AudioPlayerService.class);
            previous.putExtra("DO", GOTO_PREVIOUS);
            PendingIntent previousPendingIntent = PendingIntent.getService(parent, 0, previous, pending_intent_flag);

            Intent next = new Intent(parent, AudioPlayerService.class);
            next.putExtra("DO", GOTO_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getService(parent, 2, next, pending_intent_flag);

            int playPauseIcon = playmode ? R.drawable.dark_pause_icon : R.drawable.dark_play_icon;
            String playPauseTitle = playmode ? "Pause" : "Play";
            PendingIntent playPausePendingIntent = PendingIntent.getService(parent, 1,
                    new Intent(parent, AudioPlayerService.class).putExtra("DO", PAUSE),
                    pending_intent_flag);

            NotificationCompat.Action playPauseAction = new NotificationCompat.Action(
                    playPauseIcon, playPauseTitle, playPausePendingIntent);


            Intent intent = new Intent(parent, AudioPlayerActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(parent, 0, intent, pending_intent_flag);

            Bitmap albumArt = null;
            if (current_audio != null && current_audio.getAlbumId() != null) {
                Uri albumArtUri = Global.GET_ALBUM_ART_URI(current_audio.getAlbumId());
                try {
                    albumArt = GlideApp.with(context)
                            .asBitmap()
                            .load(albumArtUri)
                            .error(R.drawable.woofer_icon)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .submit()
                            .get();
                } catch (Exception e) {
                    // Handle exception
                }
            }

            if (albumArt == null) {
                Drawable wooferDrawable = ContextCompat.getDrawable(context, R.drawable.woofer_icon);
                albumArt = Bitmap.createBitmap(wooferDrawable.getIntrinsicWidth(),
                        wooferDrawable.getIntrinsicHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(albumArt);
                wooferDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                wooferDrawable.draw(canvas);
            }


            nBuilder = new NotificationCompat.Builder(parent, CHANNEL_ID)
                    .setSmallIcon(R.drawable.app_icon_notification)
                    .setContentTitle(current_audio.getTitle())
                    .setContentText(current_audio.getArtist())
                    .setLargeIcon(albumArt)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(0, 1, 2))
                    .addAction(R.drawable.dark_previous_icon, "Previous", previousPendingIntent)
                    .addAction(playPauseAction)
                    .addAction(R.drawable.dark_next_icon, "Next", nextPendingIntent)
                    .setContentIntent(pIntent)
                    .setOngoing(true)


                    .setAutoCancel(false)
                    .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        }

        public void updatePlayPauseAction(boolean isPlaying) {
            int icon = isPlaying ? R.drawable.dark_pause_icon : R.drawable.dark_play_icon;
            String title = isPlaying ? "Pause" : "Play";

            Intent pause = new Intent(parent, AudioPlayerService.class);
            pause.putExtra("DO", PAUSE);
            PendingIntent playPausePendingIntent = PendingIntent.getService(parent, 1, pause, pending_intent_flag);

            Intent previous = new Intent(parent, AudioPlayerService.class);
            previous.putExtra("DO", GOTO_PREVIOUS);
            PendingIntent previousPendingIntent = PendingIntent.getService(parent, 0, previous, pending_intent_flag);

            Intent next = new Intent(parent, AudioPlayerService.class);
            next.putExtra("DO", GOTO_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getService(parent, 2, next, pending_intent_flag);


            NotificationCompat.Action action = new NotificationCompat.Action(icon, title, playPausePendingIntent);

            nBuilder.clearActions()
                    .addAction(R.drawable.dark_previous_icon, "Previous", previousPendingIntent)
                    .addAction(action)
                    .addAction(R.drawable.dark_next_icon, "Next", nextPendingIntent);

            Notification notification = nBuilder.build();
            nManager.notify(notification_id, notification);
        }

        public Notification get_notification() {
            return nBuilder.build();
        }

        public void notify_notification() {
            nManager.notify(notification_id, nBuilder.build());
        }


        public void notificationCancel() {
            nManager.cancel(notification_id);
        }
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            handler.obtainMessage(START).sendToTarget();
        }

        @Override
        public void onPause() {
            handler.obtainMessage(PAUSE).sendToTarget();
        }

        @Override
        public void onSkipToNext() {
            handler.obtainMessage(GOTO_NEXT).sendToTarget();
        }

        @Override
        public void onSkipToPrevious() {
            handler.obtainMessage(GOTO_PREVIOUS).sendToTarget();
        }

        @Override
        public void onSeekTo(long pos) {
            handler.obtainMessage(SEEK_TO, (int) pos, 0).sendToTarget();
        }
    }


}
