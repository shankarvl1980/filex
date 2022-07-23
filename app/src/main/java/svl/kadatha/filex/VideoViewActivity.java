package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

public class VideoViewActivity extends BaseActivity
{
	
	public Uri data;
	public FragmentManager fm;
	private Context context;
	TinyDB tinyDB;
	private final List<OnPageSelectListener> onPageSelectListeners=new ArrayList<>();
	public int current_page_idx,file_selected_idx;
	public boolean toolbar_visible,fromArchiveView;
	public FileObjectType fileObjectType;
	private VideoViewContainerFragment videoViewContainerFragment;
    //ProgressBarFragment pbf;
	//public IndexedLinkedHashMap<FilePOJO,Integer> video_list=new IndexedLinkedHashMap<>();
	//private AlbumPollFragment albumPollFragment;
	private static final String ALBUM_POLL_FRAGMENT_TAG="album_poll_fragment";
	//private Handler handler;
	//public FilePOJO currently_shown_file;
	public boolean fromThirdPartyApp;
	public String source_folder;
	public static final String ACTIVITY_NAME="VIDEO_VIEW_ACTIVITY";
	public boolean clear_cache;
	private LocalBroadcastManager localBroadcastManager;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
 		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		context=this;
		fm=getSupportFragmentManager();
		setContentView(R.layout.activity_blank_view);
		tinyDB=new TinyDB(context);
		localBroadcastManager= LocalBroadcastManager.getInstance(context);

		//handler=new Handler();
		Intent intent=getIntent();
		if(savedInstanceState==null)
		{
			on_intent(intent);
		}
		toolbar_visible=true;
	}

	private void on_intent(Intent intent)
	{
		data=intent.getData();
		fromArchiveView = intent.getBooleanExtra(FileIntentDispatch.EXTRA_FROM_ARCHIVE, false);
		fileObjectType = Global.GET_FILE_OBJECT_TYPE(intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE));
        String file_path = intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_PATH);
		if(file_path ==null) file_path =PathUtil.getPath(context,data);
		/*
		pbf = ProgressBarFragment.newInstance();
		pbf.show(fm,"");
		albumPollFragment=AlbumPollFragment.getInstance(file_path,fileObjectType,Global.VIDEO_REGEX,fromArchiveView);
		fm.beginTransaction().replace(R.id.activity_blank_view_container, albumPollFragment,"").commit();
		handler.post(new Runnable() {
			@Override
			public void run() {
				if(albumPollFragment.asyncTaskStatus!= AsyncTaskStatus.COMPLETED)
				{
					handler.postDelayed(this, 100);
				}
				else
				{
					fromThirdPartyApp=albumPollFragment.fromThirdPartyApp;
					file_selected_idx=albumPollFragment.file_selected_idx;
					video_list=albumPollFragment.getVideoPollingResult();
					currently_shown_file=albumPollFragment.currently_shown_file;
					source_folder=albumPollFragment.source_folder;
					videoViewContainerFragment= VideoViewContainerFragment.getNewInstance(fileObjectType,fromArchiveView);
					set_listeners_on_video_container_fragment(videoViewContainerFragment);
					handler.removeCallbacks(this);
					pbf.dismissAllowingStateLoss();
				}
			}
		});

		 */
		videoViewContainerFragment= VideoViewContainerFragment.getNewInstance(file_path,fromArchiveView,fileObjectType);
		fm.beginTransaction().replace(R.id.activity_blank_view_container, videoViewContainerFragment,"").commit();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		on_intent(intent);
	}

	private void set_listeners_on_video_container_fragment(VideoViewContainerFragment videoViewContainerFragment)
	{
		videoViewContainerFragment.setOnPageSelectListener(new VideoViewContainerFragment.OnPageSelectListener() {
			@Override
			public void onPageSelect(int x) {

				for(OnPageSelectListener listener:onPageSelectListeners)
				{
					if(listener!=null)
					{
						listener.onPageSelect(x);
					}
				}

			}
		});

		videoViewContainerFragment.setToolBarVisibleListener(new VideoViewContainerFragment.ToolBarVisibleListener() {
			@Override
			public void onToolbarVisible(boolean visible) {
				toolbar_visible=visible;

			}
		});

		videoViewContainerFragment.setAlbumPollCompleteListener(new VideoViewContainerFragment.AlbumPollCompleteListener() {
			@Override
			public void onPollComplete() {

			}
		});
		fm.beginTransaction().replace(R.id.activity_blank_view_container, videoViewContainerFragment,"").commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// TODO: Implement this method
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onStart()
	{
		// TODO: Implement this method
		super.onStart();
		clear_cache=true;
		Global.WORKOUT_AVAILABLE_SPACE();
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("clear_cache",clear_cache);
	}

	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		clear_cache=savedInstanceState.getBoolean("clear_cache");
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(!isFinishing() && !isChangingConfigurations() && clear_cache)
		{
			clearCache();
		}
	}

	public void clearCache()
	{
		Global.HASHMAP_FILE_POJO.clear();
		Global.HASHMAP_FILE_POJO_FILTERED.clear();
		Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_FILE_POJO_CACHE_CLEARED_ACTION,localBroadcastManager,ACTIVITY_NAME);
	}

	@Override
	public void onBackPressed() {
		clear_cache=false;
		super.onBackPressed();
	}

	interface OnPageSelectListener
	{
		void onPageSelect(int x);
	}

	public void addOnPageSelectListener(OnPageSelectListener listener)
	{
		onPageSelectListeners.add(listener);
	}

}
