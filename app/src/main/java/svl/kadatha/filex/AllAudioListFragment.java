package svl.kadatha.filex;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
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
	private boolean toolbar_visible=true;
	private int scroll_distance;
	static boolean FULLY_POPULATED;
	private FrameLayout progress_bar;
	private TextView empty_tv;
	private int num_all_audio;
	public boolean whether_audios_set_to_current_list, img_btns_enabled;
	private AudioPlayerActivity.SearchFilterListener searchFilterListener;
	public AudioListViewModel audioListViewModel;
	private static final String SAVE_AUDIO_LIST_REQUEST_CODE="all_audio_save_audio_request_code";
	private static final String DELETE_FILE_REQUEST_CODE="all_audio_file_delete_request_code";


	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		AppCompatActivity activity= (AppCompatActivity) context;
		if(activity instanceof AudioSelectListener)
		{
			audioSelectListener= (AudioSelectListener) activity;
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		audioSelectListener=null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		list_popupwindowpojos=new ArrayList<>();
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon,getString(R.string.delete),1));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon,getString(R.string.send),2));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties),3));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v=inflater.inflate(R.layout.fragment_all_audio_list,container,false);
		file_number_view=v.findViewById(R.id.all_audio_file_number);
		FloatingActionButton refresh_btn = v.findViewById(R.id.floating_action_all_audio_refresh);
		refresh_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(progress_bar.getVisibility()==View.VISIBLE || !FULLY_POPULATED)
				{
					Global.print(context,getString(R.string.please_wait));
					return;
				}

				progress_bar.setVisibility(View.VISIBLE);
				clear_selection();
				RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
				repositoryClass.audio_pojo_hashmap.clear();
				FULLY_POPULATED=false;
				audioListViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
				audioListViewModel.listAudio();
			}
		});
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
		listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapter(context,list_popupwindowpojos));
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

		audioListViewModel=new ViewModelProvider(this).get(AudioListViewModel.class);
		audioListViewModel.listAudio();
		audioListViewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
			@Override
			public void onChanged(AsyncTaskStatus asyncTaskStatus) {
				if(asyncTaskStatus==AsyncTaskStatus.STARTED)
				{
					progress_bar.setVisibility(View.VISIBLE);
				}
				else if (asyncTaskStatus==AsyncTaskStatus.COMPLETED)
				{
					progress_bar.setVisibility(View.GONE);
				}
				if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
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
					file_number_view.setText(audioListViewModel.audio_pojo_selected_items.size()+"/"+num_all_audio);
				}
			}
		});

		int size=audioListViewModel.audio_pojo_selected_items.size();
		enable_disable_buttons(size != 0);
		file_number_view.setText(size+"/"+num_all_audio);

		DeleteAudioViewModel deleteAudioViewModel=new ViewModelProvider(AllAudioListFragment.this).get(DeleteAudioViewModel.class);
		deleteAudioViewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
			@Override
			public void onChanged(AsyncTaskStatus asyncTaskStatus) {
				if(asyncTaskStatus==AsyncTaskStatus.STARTED)
				{
					progress_bar.setVisibility(View.VISIBLE);
				}
				else if (asyncTaskStatus==AsyncTaskStatus.COMPLETED)
				{
					progress_bar.setVisibility(View.GONE);
				}
				if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
				{
					if(deleteAudioViewModel.deleted_audio_files.size()>0)
					{
						audio_list.removeAll(deleteAudioViewModel.deleted_audio_files);
						total_audio_list.removeAll(deleteAudioViewModel.deleted_audio_files);
						num_all_audio = total_audio_list.size();
						((AudioPlayerActivity) context).update_all_audio_list_and_audio_queued_array_and_current_play_number(deleteAudioViewModel.deleted_audio_files);
						((AudioPlayerActivity) context).trigger_enable_disable_previous_next_btns();
					}

					deleteAudioViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
				}
			}
		});

		audioListViewModel.isSavingAudioFinished.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
			@Override
			public void onChanged(AsyncTaskStatus asyncTaskStatus) {
				if(asyncTaskStatus==AsyncTaskStatus.STARTED)
				{
					progress_bar.setVisibility(View.VISIBLE);
				}
				else if (asyncTaskStatus==AsyncTaskStatus.COMPLETED)
				{
					progress_bar.setVisibility(View.GONE);
				}

				if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
				{
					((AudioPlayerActivity) context).trigger_audio_list_saved_listener();
					((AudioPlayerActivity) context).trigger_enable_disable_previous_next_btns();
					clear_selection();
					audioListViewModel.isSavingAudioFinished.setValue(AsyncTaskStatus.NOT_YET_STARTED);
				}
			}
		});


		((AudioPlayerActivity)context).getSupportFragmentManager().setFragmentResultListener(SAVE_AUDIO_LIST_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(SAVE_AUDIO_LIST_REQUEST_CODE))
				{
					progress_bar.setVisibility(View.VISIBLE);
					String list_name=result.getString("list_name");
					audioListViewModel.save_audio(list_name.equals("") ? "q" : "s",list_name);
				}
			}
		});


		((AppCompatActivity)context).getSupportFragmentManager().setFragmentResultListener(DELETE_FILE_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(DELETE_FILE_REQUEST_CODE))
				{
					progress_bar.setVisibility(View.VISIBLE);
					Uri tree_uri=result.getParcelable("tree_uri");
					String tree_uri_path=result.getString("tree_uri_path");
					String source_folder=result.getString("source_folder");
					deleteAudioViewModel.deleteAudioPOJO(false,audioListViewModel.audios_selected_for_delete,tree_uri,tree_uri_path);

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

	private class ToolBarClickListener implements View.OnClickListener
	{
		@Override
		public void onClick(View p1)
		{
			// TODO: Implement this method
			if(progress_bar.getVisibility()==View.VISIBLE)
			{
				Global.print(context,getString(R.string.please_wait));
				return;
			}

			int id = p1.getId();
			if (id == R.id.toolbar_btn_1) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
				if(!((AudioPlayerActivity)context).search_toolbar_visible)
				{
					((AudioPlayerActivity) context).set_visibility_searchbar(true);
				}

			}  else if (id == R.id.toolbar_btn_2) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity) context).search_edittext.getWindowToken(),0);
				if (audioListViewModel.audio_pojo_selected_items.size() < 1) {
					return;
				}
				AudioPlayerService.AUDIO_QUEUED_ARRAY=new ArrayList<>(audioListViewModel.audio_pojo_selected_items.values());
				Iterator<AudioPOJO> iterator=AudioPlayerService.AUDIO_QUEUED_ARRAY.iterator();
				while(iterator.hasNext())
				{
					AudioPOJO audioPOJO=iterator.next();
					if(new File(audioPOJO.getData()).exists())
					{
						break;
					}
					else
					{
						iterator.remove();
					}
				}

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
				if (audioListViewModel.audio_pojo_selected_items.size() < 1) {
					return;
				}

				AudioSaveListDialog audioSaveListDialog = AudioSaveListDialog.getInstance(SAVE_AUDIO_LIST_REQUEST_CODE);
				audioSaveListDialog.show(((AudioPlayerActivity) context).getSupportFragmentManager(), "");

			} else if (id == R.id.toolbar_btn_4) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity) context).search_edittext.getWindowToken(),0);
				if (audioListViewModel.audio_pojo_selected_items.size() < 1) {
					return;
				}
				//listPopWindow.showAsDropDown(p1,0,-(Global.ACTION_BAR_HEIGHT+listview_height+Global.FOUR_DP));
				listPopWindow.showAtLocation(bottom_toolbar,Gravity.BOTTOM|Gravity.END,0,  (Global.NAVIGATION_STATUS_BAR_HEIGHT-Global.GET_STATUS_BAR_HEIGHT(context)+Global.FOUR_DP));


			} else if (id == R.id.toolbar_btn_5) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity) context).search_edittext.getWindowToken(),0);
				int size = audio_list.size();

				if (audioListViewModel.audio_pojo_selected_items.size() < size) {
					audioListViewModel.audio_pojo_selected_items = new IndexedLinkedHashMap<>();
					//audioListViewModel.audio_selected_array = new ArrayList<>();

					for (int i = 0; i < size; ++i) {
						audioListViewModel.audio_pojo_selected_items.put(i, audio_list.get(i));
						//audioListViewModel.audio_selected_array.add(audio_list.get(i));
					}
					all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.deselect_icon,0,0);
					audioListRecyclerViewAdapter.notifyDataSetChanged();
				} else {
					clear_selection();
				}

				int s=audioListViewModel.audio_pojo_selected_items.size();
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


	public void clear_selection()
	{
		if(total_audio_list!=null)
		{
			num_all_audio=total_audio_list.size();
		}

		//audioListViewModel.audio_selected_array=new ArrayList<>();
		audioListViewModel.audio_pojo_selected_items =new IndexedLinkedHashMap<>();
		if (audioListRecyclerViewAdapter!=null) audioListRecyclerViewAdapter.notifyDataSetChanged();
		enable_disable_buttons(false);
		if(num_all_audio<=0)
		{
			recyclerview.setVisibility(View.GONE);
			empty_tv.setVisibility(View.VISIBLE);
		}
		file_number_view.setText(audioListViewModel.audio_pojo_selected_items.size()+"/"+num_all_audio);
		all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.select_icon,0,0);
	}

	
	private class ListPopupWindowClickListener implements AdapterView.OnItemClickListener
	{
		@Override
		public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
		{
			// TODO: Implement this method
			final ArrayList<String> files_selected_array=new ArrayList<>();
			if (audioListViewModel.audio_pojo_selected_items.size() < 1) {
				return;
			}
			switch(p3)
			{
				case 0:
					audioListViewModel.audios_selected_for_delete = new ArrayList<>();
					int size=audioListViewModel.audio_pojo_selected_items.size();
					for(int i=0;i<size;++i)
					{
						AudioPOJO audio=audioListViewModel.audio_pojo_selected_items.getValueAtIndex(i);
						files_selected_array.add(audio.getData());
						audioListViewModel.audios_selected_for_delete.add(audio);

					}
					final DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity = DeleteFileAlertDialogOtherActivity.getInstance(DELETE_FILE_REQUEST_CODE,files_selected_array,FileObjectType.SEARCH_LIBRARY_TYPE);
					deleteFileAlertDialogOtherActivity.show(((AudioPlayerActivity) context).fm, "deletefilealertdialog");
					break;
				case 1:
					ArrayList<File> file_list=new ArrayList<>();
					for(AudioPOJO audio:audioListViewModel.audio_pojo_selected_items.values())
					{
						file_list.add(new File(audio.getData()));
					}
					FileIntentDispatch.sendFile(context,file_list);
					clear_selection();
					break;
					
				case 2:
					for(AudioPOJO audio:audioListViewModel.audio_pojo_selected_items.values())
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
				int size=audioListViewModel.audio_pojo_selected_items.size();
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
				onLongClickProcedure(p1,audioListViewModel.audio_pojo_selected_items.size());
				return true;
			}

			private void onLongClickProcedure(View v, int size)
			{
				pos=getBindingAdapterPosition();
				if(audioListViewModel.audio_pojo_selected_items.containsKey(pos))
				{
					audioListViewModel.audio_pojo_selected_items.remove(pos);
					v.setSelected(false);
					((AudioListRecyclerViewItem)v).set_selected(false);
					//audioListViewModel.audio_selected_array.remove(audio_list.get(pos));
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
					audioListViewModel.audio_pojo_selected_items.put(pos,audio_list.get(pos));
					v.setSelected(true);
					((AudioListRecyclerViewItem)v).set_selected(true);
					//audioListViewModel.audio_selected_array.add(audio_list.get(pos));
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
			String album_id=audio.getAlbumId();
			String title=audio.getTitle();
			String album=getString(R.string.album_colon)+" "+audio.getAlbum();
			long duration=0L;
			String duration_string=audio.getDuration();
			if(duration_string!=null) duration=Long.parseLong(duration_string);
			String duration_str=getString(R.string.duration)+" "+ (String.format("%d:%02d",duration/1000/60,duration/1000%60));
			String artist=getString(R.string.artists_colon)+" "+audio.getArtist();
			boolean item_selected=audioListViewModel.audio_pojo_selected_items.containsKey(p2);
			p1.view.setData(album_id,title,album,duration_str,artist,item_selected);
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
					return new FilterResults();
				}

				@Override
				protected void publishResults(CharSequence constraint, FilterResults results) {
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

					int t=audio_list.size();
					if(audioListViewModel.audio_pojo_selected_items.size()>0)
					{
						clear_selection();
					}
					else
					{
						notifyDataSetChanged();
					}
					file_number_view.setText(audioListViewModel.audio_pojo_selected_items.size()+"/"+t);
				}
			};
		}


	}

}
