package svl.kadatha.filex;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AllAudioListFragment extends Fragment
{

	private Context context;
	private List<AudioPOJO> audio_list,total_audio_list;
	private AudioListRecyclerViewAdapter audioListRecyclerViewAdapter;
	private RecyclerView recyclerview;
	private Button play_btn;
	private Button save_btn;
	private Button overflow_btn;
	private Button all_select_btn;
	private TextView file_number_view;
	private AudioSelectListener audioSelectListener;
	private Toolbar bottom_toolbar;
	private PopupWindow listPopWindow;
	private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
	private List<AudioPOJO> audios_selected_for_delete;
	private ArrayList<AudioPOJO> deleted_audios=new ArrayList<>();
	private boolean permission_requested;
	private final String tree_uri_path="";
	private Uri tree_uri;
	private final int request_code=982;
	private boolean asynctask_running;
//	public SparseBooleanArray mselecteditems=new SparseBooleanArray();
	//public List<AudioPOJO> audio_selected_array=new ArrayList<>();
	private boolean toolbar_visible=true;
	private int scroll_distance;
	private AsyncTaskStatus asyncTaskStatus;
	static boolean FULLY_POPULATED;
	private FrameLayout progress_bar;
	private TextView empty_tv;
	private int num_all_audio;
	public boolean whether_audios_set_to_current_list, img_btns_enabled;
	private AudioPlayerActivity.SearchFilterListener searchFilterListener;
	public AudioListViewModel audioListViewModel;
	private static final String SAVE_AUDIO_LIST_REQUEST_CODE="all_audio_save_audio_request_code";

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		//setRetainInstance(true);
		asyncTaskStatus=AsyncTaskStatus.NOT_YET_STARTED;
		list_popupwindowpojos=new ArrayList<>();
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon,getString(R.string.delete)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon,getString(R.string.send)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties)));

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v=inflater.inflate(R.layout.fragment_all_audio_list,container,false);
		file_number_view=v.findViewById(R.id.all_audio_file_number);
		bottom_toolbar=v.findViewById(R.id.audio_list_bottom_toolbar);

		EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(context,5,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
		int[] bottom_drawables ={R.drawable.search_icon,R.drawable.play_icon,R.drawable.add_list_icon,R.drawable.overflow_icon,R.drawable.select_icon};
		String [] titles={getString(R.string.search),getString(R.string.play),getString(R.string.list),getString(R.string.more),getString(R.string.select)};
		tb_layout.setResourceImageDrawables(bottom_drawables,titles);

		bottom_toolbar.addView(tb_layout);

		Button search_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
		play_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_2);
		save_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_3);
		overflow_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_4);
		all_select_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_5);

		ToolBarClickListener toolBarClickListener = new ToolBarClickListener();

		search_btn.setOnClickListener(toolBarClickListener);
		play_btn.setOnClickListener(toolBarClickListener);
		save_btn.setOnClickListener(toolBarClickListener);
		overflow_btn.setOnClickListener(toolBarClickListener);
		all_select_btn.setOnClickListener(toolBarClickListener);

		listPopWindow=new PopupWindow(context);
		ListView listView=new ListView(context);
		listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapater(context,list_popupwindowpojos));
		listPopWindow.setContentView(listView);
		listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popupwindow_width));
		listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		listPopWindow.setFocusable(true);
		listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.list_popup_background));
		int listview_height = Global.GET_HEIGHT_LIST_VIEW(listView);
		listView.setOnItemClickListener(new ListPopupWindowClickListener());
		
		recyclerview=v.findViewById(R.id.fragment_audio_list_container);
		recyclerview.setLayoutManager(new LinearLayoutManager(context));
		recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener()
			{
				final int threshold=5;
				public void onScrolled(RecyclerView rv, int dx, int dy)
				{
					super.onScrolled(rv,dx,dy);
					if(scroll_distance>threshold && toolbar_visible)
					{
						bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
						toolbar_visible=false;
						scroll_distance=0;
					}
					else if(scroll_distance<-threshold && !toolbar_visible)
					{
						bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
						toolbar_visible=true;
						scroll_distance=0;
					}

					if((toolbar_visible && dy>0) || (!toolbar_visible && dy<0))
					{
						scroll_distance+=dy;
					}

				}

			});
		
		empty_tv=v.findViewById(R.id.all_audio_list_empty);
		progress_bar=v.findViewById(R.id.all_audio_list_progressbar);


		/*
		if(asyncTaskStatus!=AsyncTaskStatus.STARTED)
		{
			new MediaExtractAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}

		 */
		asyncTaskStatus=AsyncTaskStatus.STARTED;
		audioListViewModel=new ViewModelProvider(this).get(AudioListViewModel.class);
		audioListViewModel.listAudio();
		audioListViewModel.isFinished.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean aBoolean) {
				if(aBoolean)
				{
					audio_list=audioListViewModel.audio_list;
					total_audio_list=audioListViewModel.audio_list;
					audioListRecyclerViewAdapter=new AudioListRecyclerViewAdapter();
					recyclerview.setAdapter(audioListRecyclerViewAdapter);
					num_all_audio=total_audio_list.size();
					if(num_all_audio<=0)
					{
						recyclerview.setVisibility(View.GONE);
						empty_tv.setVisibility(View.VISIBLE);
						enable_disable_buttons(false);
					}

					FULLY_POPULATED=true;
					file_number_view.setText(audioListViewModel.mselecteditems.size()+"/"+num_all_audio);
					progress_bar.setVisibility(View.GONE);
					asyncTaskStatus=AsyncTaskStatus.COMPLETED;
				}
			}
		});



		int size=audioListViewModel.mselecteditems.size();
		enable_disable_buttons(size != 0);
		file_number_view.setText(size+"/"+num_all_audio);
		((AudioPlayerActivity)context).getSupportFragmentManager().setFragmentResultListener(SAVE_AUDIO_LIST_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(SAVE_AUDIO_LIST_REQUEST_CODE))
				{
					progress_bar.setVisibility(View.VISIBLE);
					String list_name=result.getString("list_name");
					audioListViewModel.isSavingAudioFinished.setValue(false);
					audioListViewModel.save_audio(list_name.equals("") ? "q" : "s",list_name);
					audioListViewModel.isSavingAudioFinished.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
						@Override
						public void onChanged(Boolean aBoolean) {
							if(aBoolean)
							{
								progress_bar.setVisibility(View.GONE);
								((AudioPlayerActivity) context).trigger_audio_list_saved_listener();
								((AudioPlayerActivity) context).trigger_enable_disable_previous_next_btns();
								clear_selection();
							}
						}
					});


				}
			}
		});


		return v;
	}


	private void enable_disable_buttons(boolean enable)
	{
		if(enable)
		{
			play_btn.setAlpha(Global.ENABLE_ALFA);
			save_btn.setAlpha(Global.ENABLE_ALFA);
			overflow_btn.setAlpha(Global.ENABLE_ALFA);
		}
		else
		{
			play_btn.setAlpha(Global.DISABLE_ALFA);
			save_btn.setAlpha(Global.DISABLE_ALFA);
			overflow_btn.setAlpha(Global.DISABLE_ALFA);
		}
		img_btns_enabled=enable;
		play_btn.setEnabled(img_btns_enabled);
		save_btn.setEnabled(img_btns_enabled);
		overflow_btn.setEnabled(img_btns_enabled);
	}

/*
	private class MediaExtractAsyncTask extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
	{
		final Uri media_uri=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			asyncTaskStatus=AsyncTaskStatus.STARTED;
		}

		@Override
		protected Void doInBackground(Void[] p1)
		{
			// TODO: Implement this method
			if(audio_list!=null)
			{
				return null;
			}
			audio_list=new ArrayList<>();
			total_audio_list=new ArrayList<>();
			AudioPlayerActivity.EXISTING_AUDIOS_ID=new ArrayList<>();
			Cursor audio_cursor;

			Cursor cursor=context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,null,null,null,null);
			if(cursor!=null && cursor.getCount()>0)
			{
				while(cursor.moveToNext())
				{

					String album_id=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
					String album_path=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
					Bitmap albumart=null;//Global.GET_RESIZED_BITMAP(album_path,Global.IMAGEVIEW_DIMENSION_LARGE_LIST);

					String where=MediaStore.Audio.Media.ALBUM_ID+"="+album_id;
					audio_cursor=context.getContentResolver().query(media_uri,null,where,null,null);
					if(audio_cursor!=null && audio_cursor.getCount()>0)
					{
						while(audio_cursor.moveToNext())
						{
							int id=audio_cursor.getInt(audio_cursor.getColumnIndex(MediaStore.Audio.Media._ID));
							String data=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
							String title=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
							String album=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
							String artist=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
							String duration=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
				

							if(new File(data).exists())
							{
								audio_list.add(new AudioPOJO(id,data,title,album,artist,duration,FileObjectType.FILE_TYPE));
								AudioPlayerActivity.EXISTING_AUDIOS_ID.add(id);
							}
						}
					}
					assert audio_cursor != null;
					audio_cursor.close();
				}
				total_audio_list=audio_list;

			}
			assert cursor != null;
			cursor.close();
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			audioListRecyclerViewAdapter=new AudioListRecyclerViewAdapter();
			recyclerview.setAdapter(audioListRecyclerViewAdapter);
			num_all_audio=total_audio_list.size();
			if(num_all_audio<=0)
			{
				recyclerview.setVisibility(View.GONE);
				empty_tv.setVisibility(View.VISIBLE);
				enable_disable_buttons(false);
			}

			FULLY_POPULATED=true;
			file_number_view.setText(mselecteditems.size()+"/"+num_all_audio);
			progress_bar.setVisibility(View.GONE);
			asyncTaskStatus=AsyncTaskStatus.COMPLETED;
		}

	}

 */

	@Override
	public void onResume() {
		super.onResume();
		searchFilterListener=new AudioPlayerActivity.SearchFilterListener() {
			@Override
			public void onSearchFilter(String constraint) {
				audioListRecyclerViewAdapter.getFilter().filter(constraint);
			}
		};
		((AudioPlayerActivity)context).addSearchFilterListener(searchFilterListener);
	}

	@Override
	public void onPause() {
		super.onPause();
		if(((AudioPlayerActivity)context).search_toolbar_visible)
		{
			((AudioPlayerActivity)context).set_visibility_searchbar(false);
		}

		((AudioPlayerActivity)context).removeSearchFilterListener(searchFilterListener);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		listPopWindow.dismiss(); // to avoid memory leak on orientation change
	}

	interface AudioSelectListener
	{
		void onAudioSelect(Uri data, AudioPOJO audio);
	}
	
	public void setAudioSelectListener(AudioSelectListener listener)
	{
		audioSelectListener=listener;
	}

	private class ToolBarClickListener implements View.OnClickListener
	{
		@Override
		public void onClick(View p1)
		{
			// TODO: Implement this method
			if(progress_bar.getVisibility()==View.VISIBLE) return;
			final Bundle bundle=new Bundle();
			final ArrayList<String> files_selected_array=new ArrayList<>();

			int id = p1.getId();
			if (id == R.id.toolbar_btn_1) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
				if(!((AudioPlayerActivity)context).search_toolbar_visible)
				{
					((AudioPlayerActivity) context).set_visibility_searchbar(true);
				}

			}  else if (id == R.id.toolbar_btn_2) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity) context).search_edittext.getWindowToken(),0);
				if (audioListViewModel.audio_selected_array.size() < 1) {
					return;
				}
				AudioPlayerService.AUDIO_QUEUED_ARRAY = new ArrayList<>();
				AudioPlayerService.AUDIO_QUEUED_ARRAY=audioListViewModel.audio_selected_array;
				if (audioSelectListener != null && AudioPlayerService.AUDIO_QUEUED_ARRAY.size() != 0) {
					AudioPlayerService.CURRENT_PLAY_NUMBER = 0;
					AudioPOJO audio = AudioPlayerService.AUDIO_QUEUED_ARRAY.get(AudioPlayerService.CURRENT_PLAY_NUMBER);
					Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
					Uri data = Uri.withAppendedPath(uri, String.valueOf(audio.getId()));
					audioSelectListener.onAudioSelect(data, audio);
				}
				clear_selection();
				((AudioPlayerActivity) context).trigger_enable_disable_previous_next_btns();
			} else if (id == R.id.toolbar_btn_3) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity) context).search_edittext.getWindowToken(),0);
				if (audioListViewModel.audio_selected_array.size() < 1) {
					return;
				}

				AudioSaveListDialog audioSaveListDialog = AudioSaveListDialog.getInstance(SAVE_AUDIO_LIST_REQUEST_CODE);
				/*
				audioSaveListDialog.setSaveAudioListListener(new AudioSaveListDialog.SaveAudioListListener() {
					public void save_audio_list(String list_name) {
						if (list_name == null) {

							SaveNewAudioListDialog saveNewAudioListDialog = new SaveNewAudioListDialog();

							saveNewAudioListDialog.setOnSaveAudioListener(new SaveNewAudioListDialog.OnSaveAudioListListener() {
								public void save_audio_list(String list_name) {

									((AudioPlayerActivity) context).audioDatabaseHelper.createTable(list_name);
									((AudioPlayerActivity) context).audioDatabaseHelper.insert(list_name, audio_selected_list_copy);
									AudioPlayerActivity.AUDIO_SAVED_LIST.add(list_name);
									((AudioPlayerActivity) context).trigger_audio_list_saved_listener();
									Global.print(context,"'" + list_name + "' " + getString(R.string.audio_list_created));

								}

							});


							saveNewAudioListDialog.show(((AudioPlayerActivity) context).getSupportFragmentManager(), "saveaudiolist_dialog");

						} else if (list_name.equals("")) {
							AudioPlayerService.AUDIO_QUEUED_ARRAY.addAll(audio_selected_list_copy);

						} else {
							if (AudioPlayerActivity.AUDIO_SAVED_LIST.contains(list_name)) {
								((AudioPlayerActivity) context).audioDatabaseHelper.insert(list_name, audio_selected_list_copy);
							}
						}
						((AudioPlayerActivity) context).trigger_enable_disable_previous_next_btns();
					}
				});

				 */


				audioSaveListDialog.show(((AudioPlayerActivity) context).getSupportFragmentManager(), "");

			} else if (id == R.id.toolbar_btn_4) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity) context).search_edittext.getWindowToken(),0);
				if (audioListViewModel.audio_selected_array.size() < 1) {
					return;
				}
				//listPopWindow.showAsDropDown(p1,0,-(Global.ACTION_BAR_HEIGHT+listview_height+Global.FOUR_DP));
				listPopWindow.showAtLocation(bottom_toolbar,Gravity.BOTTOM|Gravity.END,0,Global.ACTION_BAR_HEIGHT+Global.FOUR_DP+Global.NAVIGATION_BAR_HEIGHT);


			} else if (id == R.id.toolbar_btn_5) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity) context).search_edittext.getWindowToken(),0);
				int size = audio_list.size();

				if (audioListViewModel.mselecteditems.size() < size) {
					audioListViewModel.mselecteditems = new SparseBooleanArray();
					audioListViewModel.audio_selected_array = new ArrayList<>();

					for (int i = 0; i < size; ++i) {
						audioListViewModel.mselecteditems.put(i, true);
						audioListViewModel.audio_selected_array.add(audio_list.get(i));
					}
					all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.deselect_icon,0,0);
					audioListRecyclerViewAdapter.notifyDataSetChanged();
				} else {
					clear_selection();
				}

				int s=audioListViewModel.mselecteditems.size();
				if (s >= 1) {
					bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
					toolbar_visible = true;
					scroll_distance = 0;
					enable_disable_buttons(true);
				}
				file_number_view.setText(s + "/" + num_all_audio);
			}

		}
	
	}

	
	public void remove_audio(ArrayList<AudioPOJO> list)
	{
		for(AudioPOJO audio: list)
		{
			String data=audio.getData();
			for(AudioPOJO a:audio_list)
			{
				if(a.getData().equals(data))
				{
					audio_list.remove(a);
					total_audio_list.remove(a);
					break;
				}
			}
		}
		num_all_audio=total_audio_list.size();
		clear_selection();
	}
	

	public void clear_selection()
	{
		audioListViewModel.audio_selected_array=new ArrayList<>();
		audioListViewModel.mselecteditems=new SparseBooleanArray();
		if (audioListRecyclerViewAdapter!=null) audioListRecyclerViewAdapter.notifyDataSetChanged();
		enable_disable_buttons(false);
		if(num_all_audio<=0)
		{
			recyclerview.setVisibility(View.GONE);
			empty_tv.setVisibility(View.VISIBLE);
		}
		file_number_view.setText(audioListViewModel.mselecteditems.size()+"/"+num_all_audio);
		all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.select_icon,0,0);

	}

	
	private class ListPopupWindowClickListener implements AdapterView.OnItemClickListener
	{
		@Override
		public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
		{
			// TODO: Implement this method
			final Bundle bundle=new Bundle();
			final ArrayList<String> files_selected_array=new ArrayList<>();
			if (audioListViewModel.audio_selected_array.size() < 1) {
				return;
			}

			switch(p3)
			{
				case 0:
					audios_selected_for_delete = new ArrayList<>();
					int size=audioListViewModel.audio_selected_array.size();
					for(int i=0;i<size;++i)
					{
						AudioPOJO audio=audioListViewModel.audio_selected_array.get(i);
						files_selected_array.add(audio.getData());
						audios_selected_for_delete.add(audio);

					}
					final DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity = DeleteFileAlertDialogOtherActivity.getInstance(files_selected_array,FileObjectType.SEARCH_LIBRARY_TYPE);
					deleteFileAlertDialogOtherActivity.setDeleteFileDialogListener(new DeleteFileAlertDialogOtherActivity.DeleteFileAlertDialogListener() {
						public void onSelectOK() {

							final DeleteAudioDialog deleteAudioDialog = DeleteAudioDialog.getInstance(files_selected_array,false,deleteFileAlertDialogOtherActivity.tree_uri,deleteFileAlertDialogOtherActivity.tree_uri_path);
							deleteAudioDialog.setDeleteAudioCompleteListener(new DeleteAudioDialog.DeleteAudioCompleteListener() {
								public void onDeleteComplete() {
									deleted_audios = new ArrayList<>();
									int size=audios_selected_for_delete.size();
									for(int i=0;i<size;++i)
									{
										AudioPOJO audio=audios_selected_for_delete.get(i);
										if (!new File(audio.getData()).exists()) {
											deleted_audios.add(audio);
										}

									}

									((AudioPlayerActivity) context).update_all_audio_list_and_audio_queued_array_and_current_play_number(deleted_audios);
									((AudioPlayerActivity) context).trigger_enable_disable_previous_next_btns();
								}

							});
							deleteAudioDialog.show(((AudioPlayerActivity) context).fm, "");
							clear_selection();

						}
					});
					deleteFileAlertDialogOtherActivity.show(((AudioPlayerActivity) context).fm, "deletefilealertdialog");
					break;
				case 1:
					ArrayList<File> file_list=new ArrayList<>();
					for(AudioPOJO audio:audioListViewModel.audio_selected_array)
					{
						file_list.add(new File(audio.getData()));
					}
					FileIntentDispatch.sendFile(context,file_list);
					clear_selection();
					break;
					
				case 2:
					for(AudioPOJO audio:audioListViewModel.audio_selected_array)
					{
						files_selected_array.add(audio.getData());
					}

					PropertiesDialog propertiesDialog=PropertiesDialog.getInstance(files_selected_array,FileObjectType.FILE_TYPE);
					propertiesDialog.show(((AudioPlayerActivity)context).getSupportFragmentManager(),"properties_dialog");
					break;
				default:
					break;

			}

			listPopWindow.dismiss();
		}

	}

	public class AudioListRecyclerViewAdapter extends RecyclerView.Adapter <AudioListRecyclerViewAdapter.ViewHolder> implements Filterable
	{
		class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, AdapterView.OnLongClickListener
		{
			final AudioListRecyclerViewItem view;
			int pos;

			ViewHolder (AudioListRecyclerViewItem view)
			{
				super(view);
				this.view=view;
				view.setOnClickListener(this);
				view.setOnLongClickListener(this);
			}

			@Override
			public void onClick(View p1)
			{
				pos=getBindingAdapterPosition();
				int size=audioListViewModel.mselecteditems.size();
				if(size>0)
				{
					onLongClickProcedure(p1,size);
				}
				else 
				{
					AudioPOJO audio=audio_list.get(pos);
					int id=audio.getId();
					Uri uri=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
					Uri data=Uri.withAppendedPath(uri,String.valueOf(id));

					if(!whether_audios_set_to_current_list)
					{
						AudioPlayerService.AUDIO_QUEUED_ARRAY=new ArrayList<>();
						AudioPlayerService.AUDIO_QUEUED_ARRAY=audio_list;
						whether_audios_set_to_current_list=true;
					}
					AudioPlayerService.CURRENT_PLAY_NUMBER=pos;

					if(audioSelectListener!=null)
					{
						audioSelectListener.onAudioSelect(data,audio);
					}
					((AudioPlayerActivity)context).trigger_enable_disable_previous_next_btns();
				}
			}

			@Override
			public boolean onLongClick(View p1)
			{
				onLongClickProcedure(p1,audioListViewModel.mselecteditems.size());
				return true;
			}

			private void onLongClickProcedure(View v, int size)
			{
				pos=getBindingAdapterPosition();
				if(audioListViewModel.mselecteditems.get(pos,false))
				{
					audioListViewModel.mselecteditems.delete(pos);
					v.setSelected(false);
					((AudioListRecyclerViewItem)v).set_selected(false);
					audioListViewModel.audio_selected_array.remove(audio_list.get(pos));
					--size;
					if(size>=1)
					{
						bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
						toolbar_visible=true;
						scroll_distance=0;
						enable_disable_buttons(true);
					}

					if(size==0)
					{
						enable_disable_buttons(false);
						all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.select_icon,0,0);
					}

				}
				else
				{
					audioListViewModel.mselecteditems.put(pos,true);
					v.setSelected(true);
					((AudioListRecyclerViewItem)v).set_selected(true);
					audioListViewModel.audio_selected_array.add(audio_list.get(pos));
					bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
					toolbar_visible=true;
					scroll_distance=0;
					enable_disable_buttons(true);
					++size;
					if(size==num_all_audio)
					{
						all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.deselect_icon,0,0);
					}

				}
				file_number_view.setText(size+"/"+num_all_audio);
			}
			
		}
		

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
		{
			return new ViewHolder(new AudioListRecyclerViewItem(context,false));
		}

		@Override
		public void onBindViewHolder(AudioListRecyclerViewAdapter.ViewHolder p1, int p2)
		{
			AudioPOJO audio=audio_list.get(p2);
			String title=audio.getTitle();
			String album=getString(R.string.album_colon)+" "+audio.getAlbum();
			long duration=0L;
			String duration_string=audio.getDuration();
			if(duration_string!=null) duration=Long.parseLong(duration_string);
			String duration_str=getString(R.string.duration_colon)+" "+ (String.format("%d:%02d",duration/1000/60,duration/1000%60));
			String artist=getString(R.string.artists_colon)+" "+audio.getArtist();
			boolean item_selected=audioListViewModel.mselecteditems.get(p2,false);
			p1.view.setData(title,album,duration_str,artist,item_selected);
			p1.view.setSelected(item_selected);
		}


		@Override
		public int getItemCount()
		{	
			return audio_list.size();
		}

		@Override
		public Filter getFilter() {
			return new Filter() {
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {

					audio_list=new ArrayList<>();
					if(constraint==null || constraint.length()==0)
					{
						audio_list=total_audio_list;
					}
					else
					{
						String pattern=constraint.toString().toLowerCase().trim();
						for(int i=0;i<num_all_audio;++i)
						{
							AudioPOJO audioPOJO=total_audio_list.get(i);
							if(audioPOJO.getLowerTitle().contains(pattern))
							{
								audio_list.add(audioPOJO);
							}
						}
					}
					return new FilterResults();
				}

				@Override
				protected void publishResults(CharSequence constraint, FilterResults results) {

					int t=audio_list.size();
					if(audioListViewModel.mselecteditems.size()>0)
					{
						clear_selection();
					}
					else
					{
						notifyDataSetChanged();
					}
					file_number_view.setText(audioListViewModel.mselecteditems.size()+"/"+t);
				}
			};
		}


	}

}
