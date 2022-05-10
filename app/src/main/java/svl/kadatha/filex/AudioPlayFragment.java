package svl.kadatha.filex;
import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import me.jahnen.libaums.core.fs.UsbFile;
import me.jahnen.libaums.core.fs.UsbFileStreamFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AudioPlayFragment extends Fragment
{
	private ImageView album_art_imageview;
	private ImageButton previous_btn;
	private ImageButton play_pause_btn;
	private ImageButton next_btn;
	private TextView audio_name_tv,audio_album_tv,audio_artists_tv,next_audio_tv,total_time_tv,current_progress_tv;
	private SeekBar seekbar;
	private int total_duration;
	private Handler handler, onserviceconnection_handler,handler_for_art;
	private ServiceConnection service_connection;
	public AudioPlayerService audio_player_service;
	private boolean service_bound;
	private Context context;
	private PopupWindow listPopWindow;
	private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
	private List<AudioPOJO> files_selected_for_delete;
	private ArrayList<AudioPOJO> deleted_files=new ArrayList<>();
	private String tree_uri_path="";
	private Uri tree_uri;
	private final int request_code=982;
	private DeleteFileAsyncTask delete_file_async_task;
	private boolean asynctask_running;
	public String audio_file_name="";
	public Bitmap album_art;
	private AsyncTaskStatus asyncTaskStatus;
	private boolean isDurationMoreThanHour,fromArchiveView;
	private Uri data;
	private FileObjectType fileObjectType;
	private boolean fromThirdPartyApp;
	private LocalBroadcastManager localBroadcastManager;
	private AudioManager audioManager;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		localBroadcastManager=LocalBroadcastManager.getInstance(context);
		audioManager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		FilenameFilter file_name_filter = new FilenameFilter() {
			public boolean accept(File fi, String na) {
				if (MainActivity.SHOW_HIDDEN_FILE) {
					return true;
				} else {
					return !na.startsWith(".");
				}
			}
		};

		AudioPlayerActivity activity=((AudioPlayerActivity)context);
		data=activity.data;

		String file_path = activity.file_path;
		fromArchiveView = activity.fromArchiveView;
		fileObjectType= activity.fileObjectType;

		if(fileObjectType==null || fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
		{
			fileObjectType=FileObjectType.FILE_TYPE;
			fromThirdPartyApp=true;
		}


		if(data!=null)
		{
			String source_folder = new File(file_path).getParent();
			AlbumPolling(source_folder,fileObjectType,fromThirdPartyApp);
			Intent service_intent=new Intent(context,AudioPlayerService.class);
			service_intent.setData(data);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			{
				context.startForegroundService(service_intent);
			}
			else
			{
				context.startService(service_intent);
			}

		}

		onserviceconnection_handler=new Handler();
	
		list_popupwindowpojos=new ArrayList<>();
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon,getString(R.string.delete)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon,getString(R.string.send)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.ringtone_icon,getString(R.string.set_as_ringtone)));
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v=inflater.inflate(R.layout.fragment_current_play,container,false);
		handler=new Handler();
		service_connection=new ServiceConnection()
		{
			public void onServiceConnected(ComponentName component_name, IBinder binder)
			{
				audio_player_service=((AudioPlayerService.AudioBinder)binder).getService();
				audio_player_service.setMediaPlayerPrepareListener(new AudioPlayerService.MediaPlayerServicePrepareListener()
				{
					public void onMediaPrepare()
					{
						total_duration=audio_player_service.get_duration();
						isDurationMoreThanHour=(total_duration/1000)>3599;
						total_time_tv.setText(convertSecondsToHMmSs(total_duration));
						seekbar.setMax(total_duration);
						play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.pause_icon));
					}
				});

				audio_player_service.setAudioPlayerServiceBroadcastListener(new AudioPlayerService.AudioPlayerServiceBroadCastListener() {
					@Override
					public void onBroadcast(int number) {
						switch (number)
						{
							case AudioPlayerService.GOTO_PREVIOUS:
							case AudioPlayerService.GOTO_NEXT:
								if(audio_player_service.current_audio!=null)
								{
									setTitleArt(audio_player_service.current_audio.getTitle(),audio_player_service.current_audio.getData());
								}
								((AudioPlayerActivity)context).on_completion_audio();
								break;
							case AudioPlayerService.START:
							case AudioPlayerService.PAUSE:
								if(audio_player_service.prepared && !audio_player_service.playmode)
								{
									play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.play_icon));
								}
								else if(audio_player_service.prepared && audio_player_service.playmode)
								{
									play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.pause_icon));
								}
								break;
							case AudioPlayerService.STOP:
								setTitleArt("",null);
								total_time_tv.setText("00.00");
								AudioPlayerActivity.AUDIO_FILE=null;
								break;
							default:
								break;
						}

						enable_disable_previous_next_btn();
					}
				});
				service_bound=true;
				
			}
			
			public void onServiceDisconnected(ComponentName component_nane)
			{
				audio_player_service=null;
				service_bound=false;
			}
		};
		Toolbar top_toolbar = v.findViewById(R.id.current_play_toolbar);

		SeekBar volumeControlSeekbar = v.findViewById(R.id.current_play_volume_seekbar);
		volumeControlSeekbar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
		volumeControlSeekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
		volumeControlSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,i,0);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		audio_name_tv=v.findViewById(R.id.current_play_audio_name);
		audio_album_tv=v.findViewById(R.id.current_play_album);
		audio_artists_tv=v.findViewById(R.id.current_play_artists);
		next_audio_tv=v.findViewById(R.id.current_play_next_audio_title);


		ImageButton overflow_btn = v.findViewById(R.id.current_play_overflow);
		overflow_btn.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{

					listPopWindow.showAsDropDown(v,0,Global.SIX_DP);
				}
			});

		ImageButton exit_btn = v.findViewById(R.id.audio_player_exit_btn);
		exit_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(((AudioPlayerActivity)context).keyBoardUtil.getKeyBoardVisibility())
				{
					((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity)context).search_edittext.getWindowToken(),0);
				}
				((AudioPlayerActivity)context).onBackPressed();
				audio_player_service.handler.obtainMessage(AudioPlayerService.STOP).sendToTarget();

			}
		});

		fromArchiveView=((AudioPlayerActivity)context).fromArchiveView;

		listPopWindow=new PopupWindow(context);
		ListView listView=new ListView(context);
		listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapater(context,list_popupwindowpojos));
		listPopWindow.setContentView(listView);
		listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popupwindow_width));
		listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		listPopWindow.setFocusable(true);
		listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.list_popup_background));
		listView.setOnItemClickListener(new ListPopupWindowClickListener());
		
		
		EquallyDistributedImageButtonsLayout tb_layout =new EquallyDistributedImageButtonsLayout(context, Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
		int[] drawables ={R.drawable.previous_icon,R.drawable.backward_icon,R.drawable.play_icon,R.drawable.forward_icon,R.drawable.next_icon};
		tb_layout.setResourceImageDrawables(drawables);

		Toolbar bottom_toolbar = v.findViewById(R.id.current_play_bottom_toolbar);
		bottom_toolbar.addView(tb_layout);
		previous_btn= bottom_toolbar.findViewById(R.id.toolbar_img_btn_1);
		ImageButton backward_btn = bottom_toolbar.findViewById(R.id.toolbar_img_btn_2);
		play_pause_btn= bottom_toolbar.findViewById(R.id.toolbar_img_btn_3);
		ImageButton forward_btn = bottom_toolbar.findViewById(R.id.toolbar_img_btn_4);
		next_btn= bottom_toolbar.findViewById(R.id.toolbar_img_btn_5);
		
		
		album_art_imageview=v.findViewById(R.id.fragment_current_play_albumart);
		//album_art_image_view_dimension=album_art_imageview.getWidth();
		
		total_time_tv=v.findViewById(R.id.audio_player_total_time);
		current_progress_tv=v.findViewById(R.id.audio_player_current_progress);
		seekbar=v.findViewById(R.id.audio_player_seekbar);


		seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
			{
				public void onProgressChanged(SeekBar sb, int progress, boolean fromUser)
				{
					if(fromUser)
					{
						audio_player_service.seek_to(progress);

					}

				}

				public void onStartTrackingTouch(SeekBar sb)
				{

				}
				public void onStopTrackingTouch(SeekBar sb)
				{

				}

			});

		previous_btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				audio_player_service.handler.obtainMessage(AudioPlayerService.GOTO_PREVIOUS).sendToTarget();
			}
		});

		backward_btn.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					audio_player_service.handler.obtainMessage(AudioPlayerService.MOVE_BACKWARD).sendToTarget();
				}
			});

		play_pause_btn.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{

					if(audio_player_service.prepared && !audio_player_service.playmode)
					{
						audio_player_service.handler.obtainMessage(AudioPlayerService.START).sendToTarget();
						play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.pause_icon));
						update_position();

					}
					else if(audio_player_service.prepared && audio_player_service.playmode)
					{
						audio_player_service.handler.obtainMessage(AudioPlayerService.PAUSE).sendToTarget();
						play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.play_icon));

					}

				}
			});

		forward_btn.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					audio_player_service.handler.obtainMessage(AudioPlayerService.MOVE_FORWARD).sendToTarget();
				}
			});
		
		next_btn.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				audio_player_service.handler.obtainMessage(AudioPlayerService.GOTO_NEXT).sendToTarget();
			}
		});

		return v;
	}
	

	private void update_position()
	{
		handler.post(new Runnable()
			{
				public void run()
				{

					int current_pos=audio_player_service.get_current_position(); //audio_player;_service.get_current_position();
					seekbar.setProgress(current_pos);
					current_progress_tv.setText(convertSecondsToHMmSs(current_pos));

					if(audio_player_service.completed)
					{
						play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.play_icon));
						current_progress_tv.setText(isDurationMoreThanHour ? String.format("%d:%d:%d",0, 0, 0) : String.format("%d:%d", 0, 0));
						seekbar.setProgress(0);
						handler.removeCallbacks(this);
					}
					handler.postDelayed(this,1000);

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
	public void enable_disable_previous_next_btn()
	{
		if(AudioPlayerService.AUDIO_QUEUED_ARRAY.size()==0)
		{
			previous_btn.setEnabled(false);
			previous_btn.setAlpha(Global.DISABLE_ALFA);
			next_btn.setEnabled(false);
			next_btn.setAlpha(Global.DISABLE_ALFA);
			audio_album_tv.setText(getString(R.string.album_colon)+" null");
			audio_artists_tv.setText(getString(R.string.artists_colon)+" null");
			next_audio_tv.setText(getString(R.string.next_audio_colon)+" null");

			return;
			
		}
		if(AudioPlayerService.CURRENT_PLAY_NUMBER<=0)
		{
			previous_btn.setEnabled(false);
			previous_btn.setAlpha(Global.DISABLE_ALFA);
		}
		else
		{
			previous_btn.setEnabled(true);
			previous_btn.setAlpha(Global.ENABLE_ALFA);
		}
		if(AudioPlayerService.CURRENT_PLAY_NUMBER>=AudioPlayerService.AUDIO_QUEUED_ARRAY.size()-1)
		{
			next_btn.setEnabled(false);
			next_btn.setAlpha(Global.DISABLE_ALFA);
		}
		else
		{
			next_btn.setEnabled(true);
			next_btn.setAlpha(Global.ENABLE_ALFA);
		}
		// Below is placed here instead of at setTittleArt method because, AudioPlayerService.AUDIO_QUEUED_ARRAY and CURRENT_PLAY not yet updated on selection of audio
		if(audio_player_service !=null && audio_player_service.current_audio!=null)
		{
			audio_album_tv.setText(getString(R.string.album_colon)+" "+audio_player_service.current_audio.getAlbum());
			audio_artists_tv.setText(getString(R.string.artists_colon)+" "+audio_player_service.current_audio.getArtist());
		}
		else
		{
			audio_album_tv.setText(getString(R.string.album_colon)+" null");
			audio_artists_tv.setText(getString(R.string.artists_colon)+" null");

		}
		if(next_btn.isEnabled() && AudioPlayerService.AUDIO_QUEUED_ARRAY.size()>AudioPlayerService.CURRENT_PLAY_NUMBER+1)
		{
			next_audio_tv.setText(getString(R.string.next_audio_colon)+" "+AudioPlayerService.AUDIO_QUEUED_ARRAY.get(AudioPlayerService.CURRENT_PLAY_NUMBER+1).getTitle());

		}
		else
		{
			next_audio_tv.setText(getString(R.string.next_audio_colon)+" null");

		}
		
	}

	@Override
	public void onStart()
	{
		// TODO: Implement this method
		super.onStart();
		Intent service_intent=new Intent(context,AudioPlayerService.class);
		service_bound=context.bindService(service_intent,service_connection,Context.BIND_AUTO_CREATE);
		Runnable runnable=new Runnable()
		{
			public void run()
			{
				if(audio_player_service==null)
				{

					onserviceconnection_handler.postDelayed(this,1000);
				}
				else
				{
					if(AudioPlayerActivity.AUDIO_FILE!=null)
					{
						String path=AudioPlayerActivity.AUDIO_FILE.getData();
						setTitleArt(AudioPlayerActivity.AUDIO_FILE.getTitle(),path); // dont try audio_player_service.current_audio, it may not have been instantiated.

					}
					total_duration=audio_player_service.get_duration();
					isDurationMoreThanHour=(total_duration/1000)>3599;
					current_progress_tv.setText(isDurationMoreThanHour ? String.format("%d:%d:%d",0, 0, 0) : String.format("%d:%d", 0, 0));
					total_time_tv.setText(convertSecondsToHMmSs(total_duration));
					
					seekbar.setMax(total_duration);
					enable_disable_previous_next_btn();
					if(audio_player_service.playmode)
					{
						play_pause_btn.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.pause_icon));
					}

					update_position();
					onserviceconnection_handler.removeCallbacks(this);
				}

			}
		};

		onserviceconnection_handler.post(runnable);

	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		listPopWindow.dismiss(); // to avoid memory leak on orientation change
	}

	@Override
	public void onDestroy()
	{
		// TODO: Implement this method
		audio_player_service.removeAudioPlayerServiceBroadcastListener();
		if(service_bound)
		{
			context.unbindService(service_connection);
			service_bound=false;
		}
		super.onDestroy();
	}
	
	public void setTitleArt(String audiofilename,final String audiofilepath)
	{
		audio_file_name=audiofilename;
		if(audio_name_tv!=null && album_art_imageview!=null)
		{
			set_title_art(audiofilepath);
		}
		else
		{
			handler_for_art=new Handler();
			handler_for_art.post(new Runnable()
			{
				public void run()
				{
					if(audio_name_tv!=null && album_art_imageview!=null)
					{
						set_title_art(audiofilepath);
						handler_for_art.removeCallbacks(this);
					}
					else
					{
						handler_for_art.postDelayed(this,500);
					}
				}
			});
		}
	
	}
	
	private void set_title_art(String audiofilepath)
	{
		audio_name_tv.setText(audio_file_name);
		if(album_art==null)
		{
			album_art= AudioPlayerActivity.getAlbumArt(audiofilepath,Global.SCREEN_WIDTH-Global.FOUR_DP);
			if(album_art==null)
			{
				album_art=BitmapFactory.decodeResource(context.getResources(),R.drawable.woofer_icon);

			}
		}

		GlideApp.with(context).load(album_art).placeholder(R.drawable.woofer_icon).error(R.drawable.woofer_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(album_art_imageview);

	}
	
	public void seekSAFPermission()
	{
		((AudioPlayerActivity)context).clear_cache=false;
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		activityResultLauncher.launch(intent);
	}

	private final ActivityResultLauncher<Intent> activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
		@Override
		public void onActivityResult(ActivityResult result) {
			if (result.getResultCode()== Activity.RESULT_OK)
			{
				Uri treeUri;
				treeUri = result.getData().getData();
				Global.ON_REQUEST_URI_PERMISSION(context,treeUri);


				boolean permission_requested = false;
				delete_file_async_task=new DeleteFileAsyncTask(files_selected_for_delete,AudioPlayerActivity.AUDIO_FILE.getFileObjectType());
				delete_file_async_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

			}
			else
			{
				print(getString(R.string.permission_not_granted));
			}

		}
	});

	private final ActivityResultLauncher<Intent> activityResultLauncher_write_settings=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
		@Override
		public void onActivityResult(ActivityResult result) {
			if(result.getResultCode()==Activity.RESULT_OK)
			{
				set_ring_tone();
			}
			else
			{
				print(getString(R.string.permission_not_granted));
			}
		}
	});


	private boolean check_SAF_permission(String file_path,FileObjectType fileObjectType)
	{
		UriPOJO  uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();
		}


		if(tree_uri_path.equals("")) {
			SAFPermissionHelperDialog safpermissionhelper = new SAFPermissionHelperDialog();
			safpermissionhelper.set_safpermissionhelperlistener(new SAFPermissionHelperDialog.SafPermissionHelperListener() {
				public void onOKBtnClicked() {
					seekSAFPermission();
				}

				public void onCancelBtnClicked() {

				}
			});
			safpermissionhelper.show(((AudioPlayerActivity)context).fm, "saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
	}

	private class ListPopupWindowClickListener implements AdapterView.OnItemClickListener
	{

		@Override
		public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
		{
			// TODO: Implement this method
			final Bundle bundle=new Bundle();
			final ArrayList<String> files_selected_array=new ArrayList<>();
			if(AudioPlayerActivity.AUDIO_FILE==null) return;
			switch(p3)
			{
				
				case 0:
					if(fromArchiveView || AudioPlayerActivity.AUDIO_FILE.getFileObjectType()==null)
					{
						print(getString(R.string.not_able_to_process));
						break;
					}
					if(!AllAudioListFragment.FULLY_POPULATED)
					{
						print(getString(R.string.wait_till_all_audios_populated_in_all_songs_tab));
						break;
					}
					files_selected_array.add(AudioPlayerActivity.AUDIO_FILE.getData());
					DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity=DeleteFileAlertDialogOtherActivity.getInstance(files_selected_array,AudioPlayerActivity.AUDIO_FILE.getFileObjectType());
					deleteFileAlertDialogOtherActivity.setDeleteFileDialogListener(new DeleteFileAlertDialogOtherActivity.DeleteFileAlertDialogListener()
						{
							public void onSelectOK()
							{
								if(!asynctask_running)
								{
									asynctask_running=true;
									files_selected_for_delete=new ArrayList<>();
									deleted_files=new ArrayList<>();
									files_selected_for_delete.add(AudioPlayerActivity.AUDIO_FILE);
									delete_file_async_task=new DeleteFileAsyncTask(files_selected_for_delete,AudioPlayerActivity.AUDIO_FILE.getFileObjectType());
									delete_file_async_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
								}

							}
						});
					deleteFileAlertDialogOtherActivity.show(((AudioPlayerActivity)context).getSupportFragmentManager(),"deletefilealertotheractivity");
					break;
				case 1:
					Uri src_uri=null;
					if(AudioPlayerActivity.AUDIO_FILE.getFileObjectType()==null)
					{
						src_uri=data;

					}
					else if(fileObjectType==FileObjectType.FILE_TYPE)
					{
						src_uri= FileProvider.getUriForFile(context, context.getPackageName()+".provider",new File(AudioPlayerActivity.AUDIO_FILE.getData()));
					}

					if(src_uri==null)
					{
						print(getString(R.string.not_able_to_process));
						break;
					}
					ArrayList<Uri> uri_list=new ArrayList<>();
					uri_list.add(src_uri);
					FileIntentDispatch.sendUri(context,uri_list);

					break;
				case 2:
					if(AudioPlayerActivity.AUDIO_FILE.getFileObjectType()==null)
					{
						print(getString(R.string.not_able_to_process));
						break;
					}
					files_selected_array.add(AudioPlayerActivity.AUDIO_FILE.getData());
					PropertiesDialog propertiesDialog=PropertiesDialog.getInstance(files_selected_array,AudioPlayerActivity.AUDIO_FILE.getFileObjectType());
					propertiesDialog.show(((AudioPlayerActivity)context).getSupportFragmentManager(),"properties_dialog");
					break;
				case 3:
					boolean permission;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						permission = Settings.System.canWrite(context);
					} else {
						permission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
					}
					if (permission) {
						set_ring_tone();
						break;
					}  else {
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
							Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
							intent.setData(Uri.parse("package:" + context.getPackageName()));
							//((AudioPlayerActivity)context).startActivityForResult(intent, AudioPlayerActivity.WRITE_SETTINGS_PERMISSION_REQUEST_CODE);
							activityResultLauncher_write_settings.launch(intent);
						} else {
							ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_SETTINGS}, AudioPlayerActivity.WRITE_SETTINGS_PERMISSION_REQUEST_CODE);
						}
					}
					break;
				default:
					break;
				
			}
			listPopWindow.dismiss();
		}

	}

	private void set_ring_tone()
	{
		if(AudioPlayerActivity.AUDIO_FILE.getFileObjectType()==null)
		{
			print(getString(R.string.not_able_to_process));
			return;
		}
		ContentValues contentValues=new ContentValues();
		contentValues.put(MediaStore.MediaColumns.DATA,AudioPlayerActivity.AUDIO_FILE.getData());
		contentValues.put(MediaStore.MediaColumns.TITLE, AudioPlayerActivity.AUDIO_FILE.getTitle());
		contentValues.put(MediaStore.MediaColumns.SIZE, AudioPlayerActivity.AUDIO_FILE.getDuration());
		contentValues.put(MediaStore.MediaColumns.MIME_TYPE,"audio/*");
		contentValues.put(MediaStore.Audio.Media.ARTIST, "artist");
		contentValues.put(MediaStore.Audio.Media.DURATION, 500);
		contentValues.put(MediaStore.Audio.Media.IS_ALARM, false);
		contentValues.put(MediaStore.Audio.Media.IS_MUSIC, false);
		contentValues.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
		contentValues.put(MediaStore.Audio.Media.IS_RINGTONE, true);
		ContentResolver cr=context.getContentResolver();
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q)
		{
			Uri addedUri=cr.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,contentValues);
			try
			{
				OutputStream outputStream=cr.openOutputStream(addedUri);
				byte [] byte_array=new byte[500];
				BufferedInputStream bufferedInputStream = null;
				if(AudioPlayerActivity.AUDIO_FILE.getFileObjectType()==FileObjectType.FILE_TYPE)
				{
					bufferedInputStream=new BufferedInputStream(new FileInputStream(AudioPlayerActivity.AUDIO_FILE.getData()));
				}
				else if(AudioPlayerActivity.AUDIO_FILE.getFileObjectType()==FileObjectType.USB_TYPE)
				{
					UsbFile usbFile = null;
					if(MainActivity.usbFileRoot!=null)
					{
						usbFile=MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(AudioPlayerActivity.AUDIO_FILE.getData()));
					}

					bufferedInputStream= UsbFileStreamFactory.createBufferedInputStream(usbFile,MainActivity.usbCurrentFs);
				}

				int size=bufferedInputStream.read(byte_array, 0, byte_array.length);
				outputStream.write(byte_array,0,size);
				bufferedInputStream.close();
				outputStream.flush();
				outputStream.close();

			}
			catch (IOException e)
			{

			}
			RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE,addedUri);
		}
		else
		{
			Uri url= MediaStore.Audio.Media.getContentUriForPath(AudioPlayerActivity.AUDIO_FILE.getData());
			Uri addedUri=cr.insert(url,contentValues);
			RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE,addedUri);
		}

		print(getString(R.string.ringtone_set));
	}

	private class DeleteFileAsyncTask extends svl.kadatha.filex.AsyncTask<Void,File,Boolean>
	{
		final List<AudioPOJO> src_file_list;
		final List<String> deleted_file_name_list=new ArrayList<>();

		int counter_no_files;
		long counter_size_files;
		String current_file_name;
		boolean isFromInternal;
		String size_of_files_format;
		final ProgressBarFragment pbf=ProgressBarFragment.newInstance();
		final FileObjectType fileObjectType;
		String source_folder;
		DeleteFileAsyncTask(List<AudioPOJO> src_file_list, FileObjectType fileObjectType)
		{
			this.src_file_list=src_file_list;
			this.fileObjectType=fileObjectType;
		}


		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			pbf.show(((AudioPlayerActivity)context).fm,"progressbar_dialog");
			source_folder=new File(src_file_list.get(0).getData()).getParent();
		}

		@Override
		protected void onCancelled(Boolean result)
		{
			// TODO: Implement this method
			super.onCancelled(result);
			
			if(deleted_files.size()>0)
			{
				if(audio_player_service!=null)
				{
					audio_player_service.handler.obtainMessage(AudioPlayerService.STOP).sendToTarget();
				}
				((AudioPlayerActivity) context).update_all_audio_list_and_audio_queued_array_and_current_play_number(deleted_files);
				FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_name_list,fileObjectType);
				Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,AudioPlayerActivity.ACTIVITY_NAME);
				AudioPlayerActivity.AUDIO_FILE=null;

			}
			pbf.dismissAllowingStateLoss();
			asynctask_running=false;

		}

		@Override
		protected Boolean doInBackground(Void...p)
		{
			// TODO: Implement this method
			boolean success;

			if(fileObjectType==FileObjectType.FILE_TYPE)
			{
				isFromInternal=FileUtil.isFromInternal(fileObjectType,src_file_list.get(0).getData());
			}
			success=deleteFromFolder();
			return success;

		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method

			super.onPostExecute(result);
			if(deleted_files.size()>0)
			{
				if(audio_player_service!=null)
				{
					audio_player_service.handler.obtainMessage(AudioPlayerService.STOP).sendToTarget();
				}
				((AudioPlayerActivity) context).update_all_audio_list_and_audio_queued_array_and_current_play_number(deleted_files);
				FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_name_list,fileObjectType);
				Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,AudioPlayerActivity.ACTIVITY_NAME);
				AudioPlayerActivity.AUDIO_FILE=null;

			}
			pbf.dismissAllowingStateLoss();
			asynctask_running=false;

		}


		private boolean deleteFromFolder()
		{
			boolean success=false;
			int iteration=0;
			int size=src_file_list.size();
			if(fileObjectType==FileObjectType.FILE_TYPE)
			{
				if(isFromInternal)
				{
					for(int i=0;i<size;++i)
					{
						if(isCancelled())
						{
							return false;
						}
						AudioPOJO audioPOJO=src_file_list.get(i);
						File f=new File(audioPOJO.getData());
						current_file_name=f.getName();
						success=FileUtil.deleteNativeDirectory(f);
						if(success)
						{
							deleted_files.add(audioPOJO);
							deleted_file_name_list.add(current_file_name);

						}
						files_selected_for_delete.remove(audioPOJO);
					}
				}
				else
				{
					if(check_SAF_permission(src_file_list.get(0).getData(),FileObjectType.FILE_TYPE))
					{
						for(int i=0;i<size;++i)
						{
							if(isCancelled())
							{
								return false;
							}
							AudioPOJO audioPOJO=src_file_list.get(i);
							File file=new File(audioPOJO.getData());
							current_file_name=file.getName();
							success=FileUtil.deleteSAFDirectory(context,file.getAbsolutePath(),tree_uri,tree_uri_path);
							if(success)
							{
								deleted_files.add(audioPOJO);
								deleted_file_name_list.add(current_file_name);

							}
							files_selected_for_delete.remove(audioPOJO);
						}
					}

				}
			}
			else if(fileObjectType==FileObjectType.USB_TYPE)
			{
				for(int i=0;i<size;++i)
				{
					if(isCancelled())
					{
						return false;
					}
					AudioPOJO audioPOJO=src_file_list.get(i);
					UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,audioPOJO.getData());
					current_file_name=f.getName();
					success=FileUtil.deleteUsbDirectory(f);
					if(success)
					{
						deleted_files.add(audioPOJO);
						deleted_file_name_list.add(current_file_name);

					}
					files_selected_for_delete.remove(audioPOJO);
				}
			}

			return success;
		}

	}


	private void AlbumPolling(String source_folder, FileObjectType fileObjectType, boolean fromThirdPartyApp)
	{
		List<FilePOJO> filePOJOS=new ArrayList<>(), filePOJOS_filtered=new ArrayList<>();
		if (!Global.HASHMAP_FILE_POJO.containsKey(fileObjectType+source_folder))
		{
			FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,source_folder,null,false);
		}
		else
		{
			if(MainActivity.SHOW_HIDDEN_FILE)
			{
				filePOJOS=Global.HASHMAP_FILE_POJO.get(fileObjectType+source_folder) ;
			}
			else
			{
				filePOJOS=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+source_folder);
			}
		}

		AudioPlayerService.AUDIO_QUEUED_ARRAY=new ArrayList<>();
		AudioPlayerService.CURRENT_PLAY_NUMBER=0;

		// limiting to the selected only, in case of file selected from usb storage by adding condition below
		if(fromArchiveView || fromThirdPartyApp || fileObjectType==FileObjectType.USB_TYPE)
		{
			AudioPlayerService.AUDIO_QUEUED_ARRAY.add(AudioPlayerActivity.AUDIO_FILE);
		}
		else
		{
			Collections.sort(filePOJOS,FileComparator.FilePOJOComparate(Global.SORT,false));
			int size=filePOJOS.size();
			int count=0;
			for(int i=0; i<size;++i)
			{
				FilePOJO filePOJO=filePOJOS.get(i);
				if(!filePOJO.getIsDirectory())
				{
					String file_ext;
					int idx=filePOJO.getName().lastIndexOf(".");
					if(idx!=-1)
					{
						file_ext=filePOJO.getName().substring(idx+1);
						if(file_ext.matches(Global.AUDIO_REGEX))
						{

							AudioPOJO audio=new AudioPOJO(0,filePOJO.getPath(),filePOJO.getName(),null,null,"0",fileObjectType);
							AudioPlayerService.AUDIO_QUEUED_ARRAY.add(audio);

							if(AudioPlayerActivity.AUDIO_FILE.getTitle().equals(filePOJO.getName()))AudioPlayerService.CURRENT_PLAY_NUMBER=count;
							count++;

						}
						else if(filePOJO.getName().equals(AudioPlayerActivity.AUDIO_FILE.getTitle()))
						{

							AudioPlayerService.AUDIO_QUEUED_ARRAY.add(AudioPlayerActivity.AUDIO_FILE);
							AudioPlayerService.CURRENT_PLAY_NUMBER=count;
							count++;
						}

					}
					else if(filePOJO.getName().equals(AudioPlayerActivity.AUDIO_FILE.getTitle()))
					{

						AudioPlayerService.AUDIO_QUEUED_ARRAY.add(AudioPlayerActivity.AUDIO_FILE);
						AudioPlayerService.CURRENT_PLAY_NUMBER=count;
						count++;
					}

				}
			}

		}
	}


	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}
}
