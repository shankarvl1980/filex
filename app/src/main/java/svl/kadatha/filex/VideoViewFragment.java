package svl.kadatha.filex;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.IOException;

public class VideoViewFragment extends Fragment implements SurfaceHolder.Callback,MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,MediaPlayer.OnSeekCompleteListener,
		MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener,AudioManager.OnAudioFocusChangeListener
{

	private SurfaceView surfaceView;
	private MediaPlayer mp;
	public boolean prepared,playmode,stopped,completed;
	public int total_duration;
	private int position;
	private Context context;
	private String file_path;
	private Integer idx;
	private boolean firststart;
	private boolean wasPlaying,isPlaying;
	private VideoPositionListener videoPositionListener;
	private AudioManager audio_manager;
	private Handler handler, handler_seekbar_updation;
	private ImageButton play_pause_img_button, orientation_change_img_button;
	private Runnable runnable;
	private TextView current_progress_tv;
	private SeekBar seekbar;
	private String current_progress,total_time;
	private ConstraintLayout bottom_butt;
	private boolean bottom_butt_visible;
	private SurfaceHolder surfaceHolder;
	private int orientation;
	private boolean orientation_change;
	private boolean isDurationMoreThanHour;
	private AudioFocusRequest audioFocusRequest;
	private int toolbar_height;
	private FileObjectType fileObjectType;
	private boolean fromThirdPartyApp;
	private VideoViewActivity videoViewActivity;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		videoViewActivity=(VideoViewActivity)context;
		audio_manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			audioFocusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setOnAudioFocusChangeListener(this).build();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		Bundle bundle=getArguments();
		fileObjectType= (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
		fromThirdPartyApp=bundle.getBoolean("fromThirdPartyApp");
		file_path=bundle.getString("file_path");
		if(savedInstanceState==null)
		{
			position=bundle.getInt("position");
			idx=bundle.getInt("idx");
			firststart=bundle.getBoolean("firststart");
		}
		else
		{
			position=savedInstanceState.getInt("position");
			idx=savedInstanceState.getInt("idx");
			firststart=savedInstanceState.getBoolean("firststart");
			isPlaying=savedInstanceState.getBoolean("isPlaying");
			orientation=savedInstanceState.getInt("orientation");
		}
	}


	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("firststart",firststart);
		outState.putInt("position",position);
		outState.putInt("idx",idx);
		outState.putBoolean("isPlaying",isPlaying);
		outState.putInt("orientation",orientation);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method

		View v = inflater.inflate(R.layout.fragment_video_view, container, false);
		handler=new Handler();
		handler_seekbar_updation=new Handler();
		surfaceView=v.findViewById(R.id.surface_view);
		surfaceHolder=surfaceView.getHolder();
		surfaceHolder.addCallback(this);

		v.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View vi)
			{
				if(playmode)
				{
					if(bottom_butt_visible)
					{
						bottom_butt.animate().translationY(toolbar_height).setInterpolator(new DecelerateInterpolator(1));
						bottom_butt_visible=false;
						handler.removeCallbacks(runnable);
					}
					else
					{
						bottom_butt.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));
						bottom_butt_visible=true;
						handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);

					}
					play_pause_img_button.setVisibility(bottom_butt_visible ? View.VISIBLE : View.INVISIBLE);

				}

				videoViewActivity.onClickFragment();

			}
		});


		bottom_butt=v.findViewById(R.id.video_view_bottom_butt);
		ConstraintLayout toolbar_background = v.findViewById(R.id.video_play_toolbar_background);
		play_pause_img_button=v.findViewById(R.id.video_play_pause_img_btn);
		play_pause_img_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if(prepared && !playmode)
				{
					start();

				}
				else if(prepared && playmode)
				{
					pause();
				}
			}
		});


		ImageButton backward_img_button = v.findViewById(R.id.video_player_backward);
		backward_img_button.setOnTouchListener(new RepeatListener(400, 101, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				move_backward();
				handler.removeCallbacks(runnable);
				handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
			}
		}));


		ImageButton forward_img_button = v.findViewById(R.id.video_player_forward);
		forward_img_button.setOnTouchListener(new RepeatListener(400, 101, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				move_forward();
				handler.removeCallbacks(runnable);
				handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
			}
		}));


		current_progress_tv=v.findViewById(R.id.video_player_current_progress);
		ConstraintLayout.LayoutParams layoutParams=(ConstraintLayout.LayoutParams) toolbar_background.getLayoutParams();
		layoutParams.height=Global.ACTION_BAR_HEIGHT;
		toolbar_height=Global.ACTION_BAR_HEIGHT+(Global.ONE_DP*4);
		seekbar=v.findViewById(R.id.video_player_seekbar);

		seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			public void onProgressChanged(SeekBar sb, int progress, boolean fromUser)
			{
				if(fromUser)
				{
					if(prepared)
					{
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							mp.seekTo(progress,MediaPlayer.SEEK_CLOSEST_SYNC);
						}
						else
						{
							mp.seekTo(progress);
						}
						current_progress=convertSecondsToHMmSs(progress);
						current_progress_tv.setText(current_progress+"/"+total_time);
						handler.removeCallbacks(runnable);
						handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);

					}
				}

			}

			public void onStartTrackingTouch(SeekBar sb)
			{

			}
			public void onStopTrackingTouch(SeekBar sb)
			{

			}

		});

		runnable=new Runnable() {
			@Override
			public void run() {
				if(playmode)
				{
					bottom_butt.animate().translationY(toolbar_height).setInterpolator(new DecelerateInterpolator(1));
					bottom_butt_visible=false;
					play_pause_img_button.setVisibility(bottom_butt_visible ? View.VISIBLE : View.INVISIBLE);
				}

			}
		};
		bottom_butt_visible=true;
		orientation_change_img_button=v.findViewById(R.id.video_player_orientation_change);
		orientation_change_img_button.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view) {
				if(Global.ORIENTATION==Configuration.ORIENTATION_LANDSCAPE)
				{
					((VideoViewActivity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
				}
				else
				{
					((VideoViewActivity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				}
			}
		});
		return v;
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		if(mp!=null)
		{
			mp.setDisplay(holder);
			try {
				Uri data=((VideoViewActivity)context).data;
				if(fromThirdPartyApp)
				{
					mp.setDataSource(context,data);
				}
				else if(fileObjectType==FileObjectType.FILE_TYPE)
				{
					mp.setDataSource(file_path);
				}
				else if(fileObjectType==FileObjectType.USB_TYPE || fileObjectType==FileObjectType.FTP_TYPE)
				{
					mp.setDataSource(context,data);
				}

				mp.prepareAsync();
			}
			catch (IOException e)
			{

			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{

	}

	private void setSurfaceViewSize() {

		// // Get the dimensions of the video
		int videoWidth = mp.getVideoWidth();
		int videoHeight = mp.getVideoHeight();
		float videoProportion = (float) videoWidth / (float) videoHeight;

		// Get the width of the screen

		int screenWidth = (Global.ORIENTATION==Configuration.ORIENTATION_LANDSCAPE) ? Global.SCREEN_HEIGHT : Global.SCREEN_WIDTH;
		int screenHeight = (Global.ORIENTATION==Configuration.ORIENTATION_LANDSCAPE) ? Global.SCREEN_WIDTH : Global.SCREEN_HEIGHT;
		float screenProportion = (float) screenWidth / (float) screenHeight;

		android.view.ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
		if (videoProportion > screenProportion) {
			lp.width = screenWidth;
			lp.height = (int) ((float) screenWidth / videoProportion);
		} else {
			lp.width = (int) (videoProportion * (float) screenHeight);
			lp.height = screenHeight;
		}

		surfaceView.setLayoutParams(lp);
	}

	
	public static VideoViewFragment getNewInstance(FileObjectType fileObjectType, boolean fromThirdPartyApp,  String file_path,Integer position, Integer idx,boolean firststart)
	{
		VideoViewFragment frag=new VideoViewFragment();
		Bundle bundle=new Bundle();
		bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE,fileObjectType);
		bundle.putBoolean("fromThirdPartyApp",fromThirdPartyApp);
		bundle.putString("file_path",file_path);
		bundle.putInt("position",position);
		bundle.putInt("idx",idx);
		bundle.putBoolean("firststart",firststart);
		frag.setArguments(bundle);
		return frag;
	}

	private void update_position()
	{
		handler_seekbar_updation.post(new Runnable()
		{
			public void run()
			{
				if(mp!=null)
				{
					int current_position=mp.getCurrentPosition();
					seekbar.setProgress(current_position);
					current_progress=convertSecondsToHMmSs(current_position);
					current_progress_tv.setText(current_progress+"/"+total_time);
				}


				if(completed)
				{
					play_pause_img_button.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.play_icon1));
					current_progress=isDurationMoreThanHour ? String.format("%d:%d:%d",0, 0, 0) : String.format("%d:%d", 0, 0);
					current_progress_tv.setText(current_progress+"/"+total_time);
					seekbar.setProgress(0);
					handler_seekbar_updation.removeCallbacks(this);
				}
				handler_seekbar_updation.postDelayed(this,1000);

			}

		});

	}


private String convertSecondsToHMmSs(int milliseconds)
{
	int seconds=milliseconds/1000;
	int s = seconds % 60;
	int m = (seconds / 60) % 60;

	if(isDurationMoreThanHour)
	{
		int h = (seconds / (60 * 60)) % 24;
		return String.format("%d:%02d:%02d", h,m,s);
	}
	else
	{
		return String.format("%02d:%02d", m,s);
	}

}

	@Override
	public void onStart() {
		super.onStart();
		mp=new MediaPlayer();
		mp.setOnCompletionListener(this);
		mp.setOnErrorListener(this);
		mp.setOnBufferingUpdateListener(this);
		mp.setOnSeekCompleteListener(this);
		mp.setOnInfoListener(this);
		mp.setOnPreparedListener(this);
		mp.setScreenOnWhilePlaying(true);
		if(orientation!=0)
		{
			orientation_change= orientation != Global.ORIENTATION;
		}
		orientation=Global.ORIENTATION;
		if(Global.ORIENTATION==Configuration.ORIENTATION_LANDSCAPE)
		{
			orientation_change_img_button.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.full_screen_exit_icon));
		}
		else
		{
			orientation_change_img_button.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.full_screen_icon));
		}

		if(orientation_change)
		{
			wasPlaying=isPlaying;
		}
	}


	@Override
	public void onPause() {
		super.onPause();
		if(mp!=null)
		{

			if(idx!=((VideoViewActivity)context).current_page_idx)
			{
				isPlaying=false;
			}
			else
			{
				isPlaying=mp.isPlaying();
			}

			position=mp.getCurrentPosition();
			 if(prepared && playmode)
			{
				pause();
				play_pause_img_button.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.play_icon1));
			}
			if (videoPositionListener != null)
			{
				videoPositionListener.setPosition(idx, position);
			}
		}

	}

	@Override
	public void onStop() {
		super.onStop();
		stop();
	}


	@Override
	public void onPrepared(MediaPlayer mp)
	{
		// TODO: Implement this method
		prepared=true;
		stopped=false;
		total_duration=mp.getDuration();
		isDurationMoreThanHour=(total_duration/1000)>3599;
		seekbar.setMax(total_duration);
		if(position<51)
		{
			current_progress=isDurationMoreThanHour ? String.format("%d:%d:%d",0, 0, 0) : String.format("%d:%d", 0, 0);
		}
		else
		{
			current_progress=convertSecondsToHMmSs(position);
			seekbar.setProgress(position);
		}

		total_time=convertSecondsToHMmSs(total_duration);
		current_progress_tv.setText(current_progress+"/"+total_time);

		setSurfaceViewSize();

		//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		//	mp.seekTo(Math.max(position,50),MediaPlayer.SEEK_PREVIOUS_SYNC);
		//}
		//else
		{
			mp.seekTo(Math.max(position,50));
		}

		if(firststart || wasPlaying )
		{
			start();

		}
		firststart=false;

	}
	public void start()
	{
		if(prepared)
		{
			if(request_focus())
			{
				mp.start();
				playmode=true;
				completed=false;
				update_position();
				play_pause_img_button.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.dark_pause_icon));
				handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
			}

		}
	}
	public void pause()
	{
		if(prepared)
		{
			if(mp.isPlaying())mp.pause();
			playmode=false;
			play_pause_img_button.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.play_icon1));
			bottom_butt.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));
			bottom_butt_visible=true;
			play_pause_img_button.setVisibility(bottom_butt_visible ? View.VISIBLE : View.INVISIBLE);
		}
	}
	public void stop()
	{
		stopped=true;
		prepared=false;
		if(mp!=null)
		{
			mp.stop();
			mp.reset();
			mp.release();
		}
		wasPlaying=false;
		mp=null;
		releaseAudioFocus();
	}

	public void move_backward()
	{
		if(prepared)
		{
			int backward_pos=mp.getCurrentPosition()-5000;
			//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				//mp.seekTo(Math.min(backward_pos, total_duration),MediaPlayer.SEEK_PREVIOUS_SYNC);
			//}
			//else
			{
				mp.seekTo(Math.min(backward_pos, total_duration));
			}
			seekbar.setProgress(backward_pos);
			current_progress=convertSecondsToHMmSs(backward_pos);
			current_progress_tv.setText(current_progress+"/"+total_time);
		}
	}

	public void move_forward()
	{
		if(prepared)
		{
			int forward_pos=mp.getCurrentPosition()+5000;

			//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			//	mp.seekTo(Math.min(forward_pos, total_duration),MediaPlayer.SEEK_NEXT_SYNC);
			//}
			//else
			{
				mp.seekTo(Math.min(forward_pos, total_duration));
			}
			seekbar.setProgress(forward_pos);
			current_progress=convertSecondsToHMmSs(forward_pos);
			current_progress_tv.setText(current_progress+"/"+total_time);
		}
	}


	private boolean request_focus()
	{
		if(audio_manager==null)
		{
			audio_manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		}

		int result;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			result=audio_manager.requestAudioFocus(audioFocusRequest);
		}
		else
		{
			result =audio_manager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
		}

		return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
	}




	private void releaseAudioFocus()
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
					pause();
				}

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

		if(surfaceHolder!=null)
		{
			mp.reset();
			mp.release();
			onStart();
		}


		return true;
	}

	@Override
	public void onCompletion(MediaPlayer p1)
	{
		// TODO: Implement this method
		completed=true;
		playmode=false;
		if(!bottom_butt_visible)
		{
			bottom_butt.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));
			bottom_butt_visible=true;
			handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
		}
		play_pause_img_button.setVisibility(bottom_butt_visible ? View.VISIBLE : View.INVISIBLE);

	}

	@Override
	public boolean onInfo(MediaPlayer p1, int p2, int p3)
	{
		// TODO: Implement this method
		return false;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		handler.removeCallbacksAndMessages(null);
		handler_seekbar_updation.removeCallbacksAndMessages(null);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		videoPositionListener=null;
	}


interface VideoPositionListener
{
	void setPosition(Integer idx, Integer position);
}

public void setVideoPositionListener(VideoPositionListener listener)
{
	videoPositionListener=listener;
}

}
