package svl.kadatha.filex;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import java.io.File;


public class ImageViewActivity extends BaseActivity
{
	
	public Uri data;
	public FileObjectType fileObjectType;
	public FragmentManager fm;
	private Context context;
	TinyDB tinyDB;
	public File CacheDir;
	public static final String ACTIVITY_NAME="IMAGE_VIEW_ACTIVITY";
	public boolean clear_cache;

    @Override
	protected void onCreate(Bundle savedInstanceState)
	{
 		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		context=this;
		fm=getSupportFragmentManager();
		CacheDir=getExternalCacheDir();
		setContentView(R.layout.activity_blank_view);
		tinyDB=new TinyDB(context);
        //LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);

		Intent intent=getIntent();
		on_intent(intent,savedInstanceState);
	}

	private void on_intent(Intent intent, Bundle savedInstanceState)
	{
		if(intent!=null)
		{

			data=intent.getData();
			boolean fromArchiveView = intent.getBooleanExtra(FileIntentDispatch.EXTRA_FROM_ARCHIVE, false);
			fileObjectType = Global.GET_FILE_OBJECT_TYPE(intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE));
			String file_path = intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_PATH);
			if(file_path ==null) file_path =PathUtil.getPath(context,data);
			if(savedInstanceState==null)
			{
				fm.beginTransaction().replace(R.id.activity_blank_view_container, ImageViewFragment.getNewInstance(file_path, fromArchiveView,fileObjectType),"picture_fragment").commit();
			}

		}
	}

	@Override	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		on_intent(intent,null);
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
		Global.CLEAR_CACHE();
	}

	@Override
	public void onBackPressed() {
		clear_cache=false;
    	super.onBackPressed();
	}
}
