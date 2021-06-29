package svl.kadatha.filex;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AudioPlayerActivity extends BaseActivity
{

	private Group search_toolbar;
	public EditText search_edittext;
    private ViewPager view_pager;
	private final List<AudioCompletionListener> audioCompletionListeners=new ArrayList<>();
	private final List<SearchFilterListener> searchFilterListeners=new ArrayList<>();
    TinyDB tinyDB;
	static AudioPOJO AUDIO_FILE;
	public FragmentManager fm;
    static final String AUDIO_LIST_PREFERENCE_NAME="AudioList";
	static final int WRITE_SETTINGS_PERMISSION_REQUEST_CODE=59;
	static ArrayList<String> AUDIO_SAVED_LIST=new ArrayList<>();
	private Context context;
	private AudioPlayFragment apf;
	private AllAudioListFragment aalf;
	private AlbumListFragment albumlf;
	private AudioSavedListFragment aslf;
	static String AUDIO_NOTIFICATION_INTENT_ACTION;
	public static final String CURRENT_PLAY_LIST="Current play list";
	public static List<Integer> EXISTING_AUDIOS_ID;
    public boolean search_toolbar_visible;
    public KeyBoardUtil keyBoardUtil;
	public boolean fromArchiveView;
    AudioDatabaseHelper audioDatabaseHelper;
	SQLiteDatabase db;
	Uri data;
	FileObjectType fileObjectType;
	String file_path;
	public static final String ACTIVITY_NAME="AUDIO_PLAYER_ACTIVITY";


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
 		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio_player);
		context=this;
		tinyDB=new TinyDB(context);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		
		AUDIO_NOTIFICATION_INTENT_ACTION=getPackageName()+".AUDIO_NOTIFICATION";

		IntentFilter intent_filter=new IntentFilter();
		intent_filter.addAction(AUDIO_NOTIFICATION_INTENT_ACTION);
		Intent intent=getIntent();
		if(savedInstanceState==null)
		{
			on_intent(intent);

		}


		audioDatabaseHelper=new AudioDatabaseHelper(context);
		db=audioDatabaseHelper.getDatabase();

		fm=getSupportFragmentManager();
        View containerLayout = findViewById(R.id.activity_audio_container_layout);
		keyBoardUtil=new KeyBoardUtil(containerLayout);
		search_toolbar=findViewById(R.id.all_audio_search_toolbar);
		search_edittext=findViewById(R.id.search_view);
		search_edittext.setMaxWidth(Integer.MAX_VALUE);
		search_edittext.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if(!search_toolbar_visible)
				{
					return;
				}
				for(SearchFilterListener listener:searchFilterListeners)
				{
					if(listener!=null)
					{
						listener.onSearchFilter(s.toString());
					}
				}
			}
		});

        ImageButton search_cancel_btn = findViewById(R.id.search_view_cancel_button);
		search_cancel_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				set_visibility_searchbar(false);
			}
		});

        TabLayout tab_layout = findViewById(R.id.activity_audio_player_tab_layout);
		view_pager=findViewById(R.id.activity_audio_player_viewpager);
        FloatingActionButton floating_back_button = findViewById(R.id.floating_action_audio_player);
		floating_back_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View p)
			{
				onBackPressed();
			}
		});
        ViewPagerFragmentAdapter adapter = new ViewPagerFragmentAdapter(fm);
		view_pager.setAdapter(adapter);
		view_pager.setOffscreenPageLimit(3);

		view_pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
		{
			public void onPageSelected(int p1)
			{
				if(p1==1)
				{
					aalf.whether_audios_set_to_current_list=false;
				}
			}
			
			public void onPageScrolled(int p1, float p2, int p3)
			{
				
			}
			
			public void onPageScrollStateChanged(int p1)
			{
				
			}
		});
		
		adapter.startUpdate(view_pager);
		apf=(AudioPlayFragment) adapter.instantiateItem(view_pager,0);
		aalf=(AllAudioListFragment) adapter.instantiateItem(view_pager,1);
		albumlf=(AlbumListFragment) adapter.instantiateItem(view_pager,2);
		aslf=(AudioSavedListFragment) adapter.instantiateItem(view_pager,3);
		adapter.finishUpdate(view_pager);

		if(savedInstanceState==null)
		{
			if(AUDIO_FILE==null)
			{
				view_pager.setCurrentItem(1);
				apf.setTitleArt("",null,null);

			}
		}
		tab_layout.setupWithViewPager(view_pager);
		
		AUDIO_SAVED_LIST=audioDatabaseHelper.getTables();

		aalf.setAudioSelectListener(new AllAudioListFragment.AudioSelectListener()
		{
				public void onAudioSelect(Uri data, AudioPOJO audio)
				{
					android.content.Intent service_intent=new android.content.Intent(context,AudioPlayerService.class);
					service_intent.setData(data);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
					{
						startForegroundService(service_intent);
					}
					else 
					{
						startService(service_intent);
					}

					if(apf!=null)
					{
						apf.setTitleArt(audio.getTitle(),audio.getData(),audio.getAlbumArt());
						apf.audio_player_service.current_audio=audio;

					}

					AUDIO_FILE=audio;
				}
		});
		
		albumlf.setAudioSelectListener(new AlbumListFragment.AudioSelectListener()
		{
				public void onAudioSelect(Uri data, AudioPOJO audio)
				{
					android.content.Intent service_intent=new android.content.Intent(context,AudioPlayerService.class);
					service_intent.setData(data);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
					{
						startForegroundService(service_intent);
					}
					else 
					{
						startService(service_intent);
					}
					
					if(apf!=null)
					{
						apf.setTitleArt(audio.getTitle(),audio.getData(),audio.getAlbumArt());
						apf.audio_player_service.current_audio=audio;
			
					}
					AUDIO_FILE=audio;
				}
		});
		
		aslf.setAudioSelectListener(new AudioSavedListFragment.AudioSelectListener()
			{
				public void onAudioSelect(Uri data, AudioPOJO audio)
				{
					if(data==null)
					{
						data=AudioPlayerActivity.this.data;
					}
					android.content.Intent service_intent=new android.content.Intent(context,AudioPlayerService.class);
					service_intent.setData(data);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
					{
						startForegroundService(service_intent);
					}
					else 
					{
						startService(service_intent);
					}

					if(apf!=null)
					{
						apf.setTitleArt(audio.getTitle(),audio.getData(),audio.getAlbumArt());
						apf.audio_player_service.current_audio=audio;

					}

					AUDIO_FILE=audio;
				}
			});
	}

	private void on_intent(Intent intent)
	{
		data=intent.getData();
		fromArchiveView = intent.getBooleanExtra(FileIntentDispatch.EXTRA_FROM_ARCHIVE, false);
		fileObjectType = (FileObjectType) intent.getSerializableExtra(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
		file_path=intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_PATH);


		if(data!=null)
		{
			if(file_path==null) file_path=PathUtil.getPath(context,data);
			String name=new File(file_path).getName();
			AUDIO_FILE=new AudioPOJO(0,file_path,name,name.toLowerCase(),null,null,"0",null,(fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE) ? FileObjectType.FILE_TYPE : fileObjectType);

		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		on_intent(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// TODO: Implement this method
		super.onActivityResult(requestCode, resultCode, data);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if(requestCode==WRITE_SETTINGS_PERMISSION_REQUEST_CODE && Settings.System.canWrite(this))
			{
				print(getString(R.string.now_ringtone_can_be_set));
			}
			else
			{
				print(getString(R.string.permission_not_granted));
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if(requestCode==WRITE_SETTINGS_PERMISSION_REQUEST_CODE && grantResults[0]== PackageManager.PERMISSION_GRANTED)
		{
			print(getString(R.string.now_ringtone_can_be_set));
		}
		else
		{
			print(getString(R.string.permission_not_granted));
		}
	}


	public void set_visibility_searchbar(boolean visible)
	{
		if(!AllAudioListFragment.FULLY_POPULATED)
		{
			print(getString(R.string.please_wait));
			return;
		}
		search_toolbar_visible=visible;
		if(search_toolbar_visible)
		{
			((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
			search_toolbar.setVisibility(View.VISIBLE);
			search_edittext.requestFocus();
			aalf.clear_selection();
			albumlf.clear_selection();
		}
		else
		{
			((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(),0);
			search_toolbar.setVisibility(View.GONE);
			search_edittext.setText("");
			search_edittext.clearFocus();
			aalf.clear_selection();
			albumlf.clear_selection();
			for(SearchFilterListener listener:searchFilterListeners)
			{
				if(listener!=null)
				{
					listener.onSearchFilter(null);
				}
			}
		}

	}

	public static Bitmap getAlbumArt(Context context,String data)
	{

		Bitmap albumart=null;

		if(data !=null && new File(data).exists()) // String data check for null is necessary when archived audio file is accessed
		{
			MediaMetadataRetriever mmr=new MediaMetadataRetriever();
			try
			{
				mmr.setDataSource(data);
				byte [] art_array=mmr.getEmbeddedPicture();
				if(art_array!=null)
				{
					albumart=BitmapFactory.decodeByteArray(art_array,0,art_array.length);
				}

			}
			catch(Exception e)
			{

			}
			finally
			{
				mmr.release();
				return albumart;
			}
		}
		return albumart;
	}
	
	private class ViewPagerFragmentAdapter extends FragmentPagerAdapter
	{
		ViewPagerFragmentAdapter(FragmentManager fm)
		{
			super(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		}
		@Override
		public Fragment getItem(int p1)
		{
			// TODO: Implement this method
			
			switch(p1)
			{
				case 0:
					return new AudioPlayFragment();

				case 2:
					return new AlbumListFragment();
				
				case 3:
					return new AudioSavedListFragment();

				default:
					return new AllAudioListFragment();
			}
		}

		@Override
		public int getCount()
		{
			// TODO: Implement this method
			return 4;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			// TODO: Implement this method

			switch(position)
			{
				case 0:
					return getString(R.string.current_play);

				case 2:
					return getString(R.string.album);

				case 3:
					return getString(R.string.audio_list);

				default:
					return getString(R.string.all_songs);
			}
		}
	}

	@Override
	public void onBackPressed()
	{
		// TODO: Implement this method
		//super.onBackPressed();
		if(keyBoardUtil.getKeyBoardVisibility())
		{
			((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(),0);
		}
		else if(search_toolbar_visible)
		{
			set_visibility_searchbar(false);
		}
		else
		 {
			int current_item=view_pager.getCurrentItem();
			switch (current_item)
			{
				case 1:
					if(aalf.mselecteditems.size()>0)
					{
						aalf.clear_selection();
						albumlf.clear_selection();
						aslf.clear_selection();
					}
					else
					{
						aalf.clear_selection();
						albumlf.clear_selection();
						aslf.clear_selection();

						finish();
					}
					break;
				case 2:
					if(albumlf.mselecteditems.size()>0)
					{
						aalf.clear_selection();
						albumlf.clear_selection();
						aslf.clear_selection();

					}
					else
					{

						aalf.clear_selection();
						albumlf.clear_selection();
						aslf.clear_selection();
						finish();
					}
					break;
				case 3:
					if(aslf.mselecteditems.size()>0)
					{
						aalf.clear_selection();
						albumlf.clear_selection();
						aslf.clear_selection();

					}
					else
					{
						aalf.clear_selection();
						albumlf.clear_selection();
						aslf.clear_selection();

						finish();
					}

					break;
				default:

					aalf.clear_selection();
					albumlf.clear_selection();
					aslf.clear_selection();
					finish();
					break;
			}
		}

	}


	interface AudioCompletionListener
	{
		void onAudioCompletion();
	}
	
	public void addAudioCompletionListener(AudioCompletionListener listener)
	{
		audioCompletionListeners.add(listener);
	}

	public void removeListener(AudioCompletionListener listener)
	{
		audioCompletionListeners.remove(listener);
	}



	interface SearchFilterListener
	{
		void onSearchFilter(String constraint);
	}

	public void addSearchFilterListener(SearchFilterListener listener)
	{
		searchFilterListeners.add(listener);
	}

	public void removeSearchFilterListener(SearchFilterListener listener)
	{
		searchFilterListeners.remove(listener);
	}
	
	public void trigger_audio_list_saved_listener()
	{
		aslf.onSaveAudioList();
	}
	
	
	public void trigger_enable_disable_previous_next_btns()
	{
		apf.enable_disable_previous_next_btn();
	}
	
	public void update_all_audio_list_and_audio_queued_array_and_current_play_number(ArrayList<AudioPOJO> list)
	{
		aalf.remove_audio(list);
		int size=list.size();
		for(int i=0;i<size;++i)
		{
			String path=list.get(i).getData();
			for(AudioPOJO audio : AudioPlayerService.AUDIO_QUEUED_ARRAY)
			{
				if(audio.getData().equals(path))
				{
					AudioPlayerService.AUDIO_QUEUED_ARRAY.remove(audio);
					break;
				}
			}
		}

		if(AudioPlayerService.AUDIO_QUEUED_ARRAY.size()==0)
		{
			AudioPlayerService.CURRENT_PLAY_NUMBER=0;
		}
		else if(AudioPlayerService.CURRENT_PLAY_NUMBER>AudioPlayerService.AUDIO_QUEUED_ARRAY.size()-1)
		{
			AudioPlayerService.CURRENT_PLAY_NUMBER=AudioPlayerService.AUDIO_QUEUED_ARRAY.size()-1;
		}

	}



	public void on_completion_audio()
	{
		if(audioCompletionListeners.size()>0)
		{
			for(AudioCompletionListener listener:audioCompletionListeners)
			{
				listener.onAudioCompletion();
			}
		}
	}



	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}

}
