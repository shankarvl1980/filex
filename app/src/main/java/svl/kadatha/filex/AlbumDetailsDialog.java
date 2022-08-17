 package svl.kadatha.filex;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlbumDetailsDialog extends DialogFragment
{
	private static final String SAVE_AUDIO_LIST_REQUEST_CODE="album_details_save_audio_request_code";
	private static final String DELETE_FILE_REQUEST_CODE="album_details_file_delete_request_code";
	private Context context;
	private AudioListRecyclerViewAdapter audioListRecyclerViewAdapter;
	private RecyclerView selected_album_recyclerview;
	private List<AudioPOJO> audio_list,total_audio_list;
	private ImageButton all_select_btn;
	private TextView file_number_view;
	private TextView empty_audio_list_tv;
	private Toolbar bottom_toolbar;
	private Button delete_btn,play_btn,add_list_btn,overflow_btn;
	private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
	private boolean toolbar_visible;
	private int scroll_distance;
	private PopupWindow listPopWindow;
	private String albumID,album_name;
	private AudioSelectListener audioSelectListener;
	private int num_all_audio;
	private boolean whether_audios_set_to_current_list;
	private ConstraintLayout search_toolbar;
	private EditText search_edittext;
	private boolean search_toolbar_visible;
	private AudioListViewModel audioListViewModel;
	private FrameLayout progress_bar;
	private LocalBroadcastManager localBroadcastManager;


	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		localBroadcastManager= LocalBroadcastManager.getInstance(context);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setCancelable(false);
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			albumID=bundle.getString("albumID");
			album_name=bundle.getString("album_name");
		}
		
		list_popupwindowpojos=new ArrayList<>();
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon,getString(R.string.send)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties)));

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method

		View v;
		v=inflater.inflate(R.layout.fragment_album_details,container,false);
		TextView dialog_title = v.findViewById(R.id.album_details_panel_title_TextView);
		dialog_title.setText(album_name);

		ImageButton search_btn = v.findViewById(R.id.album_details_search_img_btn);
		search_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(progress_bar.getVisibility()==View.VISIBLE)
				{
					Global.print(context,getString(R.string.please_wait));
					return;
				}
				if(!search_toolbar_visible)
				{
					set_visibility_searchbar(true);
				}

			}
		});
		all_select_btn=v.findViewById(R.id.album_details_all_select);
		all_select_btn.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View p1)
				{
					if(progress_bar.getVisibility()==View.VISIBLE)
					{
						Global.print(context,getString(R.string.please_wait));
						return;
					}
					int size=audio_list.size();
					if(audioListViewModel.mselecteditems.size()<size)
					{
						audioListViewModel.mselecteditems=new SparseBooleanArray();
						audioListViewModel.audio_selected_array=new ArrayList<>();
						for(int i=0;i<size;++i)
						{
							audioListViewModel.mselecteditems.put(i,true);
							audioListViewModel.audio_selected_array.add(audio_list.get(i));
						}
			
						audioListRecyclerViewAdapter.notifyDataSetChanged();
						bottom_toolbar.setVisibility(View.VISIBLE);
						bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
						toolbar_visible=true;
						scroll_distance=0;
						all_select_btn.setImageResource(R.drawable.deselect_icon);
					}
					else
					{
						clear_selection();
					}

					file_number_view.setText(audioListViewModel.mselecteditems.size()+"/"+num_all_audio);
				}
			});

		file_number_view=v.findViewById(R.id.album_details_file_number);
		search_toolbar=v.findViewById(R.id.album_details_search_toolbar);
		search_edittext=v.findViewById(R.id.album_details_search_view);
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
				audioListRecyclerViewAdapter.getFilter().filter(s.toString());
			}
		});

		ImageButton search_cancel_btn = v.findViewById(R.id.album_details_search_view_cancel_button);
		search_cancel_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				set_visibility_searchbar(false);
			}
		});

		empty_audio_list_tv=v.findViewById(R.id.album_details_empty_list_tv);
		selected_album_recyclerview=v.findViewById(R.id.album_details_recyclerview);
		selected_album_recyclerview.setLayoutManager(new LinearLayoutManager(context));
		selected_album_recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener()
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
						if(audioListViewModel.mselecteditems.size()>0)
						{
							bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
							toolbar_visible=true;
							scroll_distance=0;
						}

					}
					if((toolbar_visible && dy>0) || (!toolbar_visible && dy<0))
					{
						scroll_distance+=dy;
					}
				}

		});

		FloatingActionButton floating_back_button = v.findViewById(R.id.album_details_floating_action_button);
		floating_back_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View p1)
			{
				if(audioListViewModel.mselecteditems.size()>0)
				{
					clear_selection();
				}
				else if(((AudioPlayerActivity)context).keyBoardUtil.getKeyBoardVisibility())
				{
					((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(),0);
				}
				else if(search_toolbar_visible)
				{
					set_visibility_searchbar(false);
				}
				else
				{
					dismissAllowingStateLoss();
				}
			}
			
		});

		progress_bar=v.findViewById(R.id.album_details_progressbar);

		EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(context,4,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH);
		int[] drawables ={R.drawable.delete_icon,R.drawable.play_icon,R.drawable.add_list_icon,R.drawable.overflow_icon};
		String [] titles={getString(R.string.delete),getString(R.string.play),getString(R.string.list),getString(R.string.more)};
		tb_layout.setResourceImageDrawables(drawables,titles);
		bottom_toolbar=v.findViewById(R.id.album_details_bottom_toolbar);
		
		bottom_toolbar.addView(tb_layout);
		delete_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_1);
		play_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_2);
		add_list_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_3);
		overflow_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_4);

		ToolbarButtonClickListener toolbarButtonClickListener=new ToolbarButtonClickListener();
		delete_btn.setOnClickListener(toolbarButtonClickListener);
		play_btn.setOnClickListener(toolbarButtonClickListener);
		add_list_btn.setOnClickListener(toolbarButtonClickListener);
		overflow_btn.setOnClickListener(toolbarButtonClickListener);


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

		audioListViewModel=new ViewModelProvider(this).get(AudioListViewModel.class);
		AlbumPOJO albumPOJO=new AlbumPOJO(albumID,"",null,null,null);
		audioListViewModel.listAudio(Collections.singletonList(albumPOJO),null,null);
		audioListViewModel.isAudioFetchingFromAlbumFinished.observe(this, new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean aBoolean) {
				if(aBoolean)
				{
					audio_list=audioListViewModel.audio_list;
					total_audio_list=audioListViewModel.audio_list;
					audioListRecyclerViewAdapter=new AudioListRecyclerViewAdapter();
					selected_album_recyclerview.setAdapter(audioListRecyclerViewAdapter);

					num_all_audio=(total_audio_list==null) ? 0 : total_audio_list.size();
					if(num_all_audio==0)
					{
						selected_album_recyclerview.setVisibility(View.GONE);
						empty_audio_list_tv.setVisibility(View.VISIBLE);
					}
					file_number_view.setText(audioListViewModel.mselecteditems.size()+"/"+num_all_audio);
					progress_bar.setVisibility(View.GONE);
				}
			}
		});



		int size=audioListViewModel.mselecteditems.size();
		if(size==0)
		{
			bottom_toolbar.setVisibility(View.GONE);
			bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
			toolbar_visible=false;
		}
		else
		{
			toolbar_visible=true;
		}
		file_number_view.setText(size+"/"+num_all_audio);

		//hide keyboard when coming from search list of albumlist dialog
		((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity)context).search_edittext.getWindowToken(),0);

		DeleteAudioViewModel deleteAudioViewModel=new ViewModelProvider(AlbumDetailsDialog.this).get(DeleteAudioViewModel.class);
		deleteAudioViewModel.isFinished.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean aBoolean) {
				if(aBoolean) {
					if (deleteAudioViewModel.deleted_audio_files.size() > 0) {
						audio_list.removeAll(deleteAudioViewModel.deleted_audio_files);
						total_audio_list.removeAll(deleteAudioViewModel.deleted_audio_files);
						num_all_audio = total_audio_list.size();
						if (num_all_audio == 0) {
							selected_album_recyclerview.setVisibility(View.GONE);
							empty_audio_list_tv.setVisibility(View.VISIBLE);
						}

						((AudioPlayerActivity) context).update_all_audio_list_and_audio_queued_array_and_current_play_number(deleteAudioViewModel.deleted_audio_files);
						((AudioPlayerActivity) context).trigger_enable_disable_previous_next_btns();
						//Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION, localBroadcastManager, AudioPlayerActivity.ACTIVITY_NAME);
					}
					clear_selection();
					progress_bar.setVisibility(View.GONE);
					deleteAudioViewModel.isFinished.setValue(false);
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



		((AppCompatActivity)context).getSupportFragmentManager().setFragmentResultListener(DELETE_FILE_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(DELETE_FILE_REQUEST_CODE))
				{
					progress_bar.setVisibility(View.VISIBLE);
					Uri tree_uri=result.getParcelable("tree_uri");
					String tree_uri_path=result.getString("tree_uri_path");
					String source_folder=result.getString("source_folder");


					deleteAudioViewModel.deleteAudioPOJO(true,audioListViewModel.audios_selected_for_delete,tree_uri,tree_uri_path);

				}

			}
		});

		return v;
	}
	

	public void clear_selection()
	{
		audioListViewModel.audio_selected_array=new ArrayList<>();
		audioListViewModel.mselecteditems=new SparseBooleanArray();
		if(audioListRecyclerViewAdapter!=null)audioListRecyclerViewAdapter.notifyDataSetChanged();
		bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
		toolbar_visible=false;
		scroll_distance=0;
		file_number_view.setText(audioListViewModel.mselecteditems.size()+"/"+num_all_audio);
		all_select_btn.setImageResource(R.drawable.select_icon);
	}

	private void enable_disable_buttons(boolean enable)
	{
		if(enable)
		{
			delete_btn.setAlpha(Global.ENABLE_ALFA);
			play_btn.setAlpha(Global.ENABLE_ALFA);
			add_list_btn.setAlpha(Global.ENABLE_ALFA);
			overflow_btn.setAlpha(Global.ENABLE_ALFA);
		}
		else
		{
			delete_btn.setAlpha(Global.DISABLE_ALFA);
			play_btn.setAlpha(Global.DISABLE_ALFA);
			add_list_btn.setAlpha(Global.DISABLE_ALFA);
			overflow_btn.setAlpha(Global.DISABLE_ALFA);
		}
		delete_btn.setEnabled(enable);
		play_btn.setEnabled(enable);
		add_list_btn.setEnabled(enable);
		overflow_btn.setEnabled(enable);

	}

	private void set_visibility_searchbar(boolean visible)
	{
		search_toolbar_visible=visible;
		if(search_toolbar_visible)
		{
			((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
			search_toolbar.setVisibility(View.VISIBLE);
			search_edittext.requestFocus();
			clear_selection();
		}
		else
		{
			((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(),0);
			search_edittext.setText("");
			search_edittext.clearFocus();
			search_toolbar.setVisibility(View.GONE);
			clear_selection();
			audioListRecyclerViewAdapter.getFilter().filter(null);
		}

	}


	@Override
	public void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		Window window=getDialog().getWindow();
		if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
		{
			window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_WIDTH);
		}
		else
		{
			window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_HEIGHT);
		}
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

	}

	@Override
	public void onPause() {
		super.onPause();
		if(search_toolbar_visible)
		{
			set_visibility_searchbar(false);
		}
	}

	public void setAudioSelectListener(AudioSelectListener listener)
	{
		audioSelectListener=listener;
	}

	interface AudioSelectListener
	{
		void onAudioSelect(Uri data, AudioPOJO audio);
	}

	private class ListPopupWindowClickListener implements AdapterView.OnItemClickListener
	{
		@Override
		public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4)
		{
			// TODO: Implement this method
			//final Bundle bundle=new Bundle();
			final ArrayList<String> files_selected_array=new ArrayList<>();

			switch(p3)
			{
				case 0:
					if(audioListViewModel.audio_selected_array.size()<1)
					{
						break;
					}
					ArrayList<File> file_list=new ArrayList<>();
					for(AudioPOJO audio:audioListViewModel.audio_selected_array)
					{
						file_list.add(new File(audio.getData()));
					}
					FileIntentDispatch.sendFile(context,file_list);
					clear_selection();
					break;

				case 1:
					if(audioListViewModel.audio_selected_array.size()<1)
					{
						break;
					}
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

	private class AudioListRecyclerViewAdapter extends RecyclerView.Adapter <AudioListRecyclerViewAdapter.ViewHolder> implements Filterable
	{
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
		{
			return new ViewHolder(new AudioListRecyclerViewItem(context,true));
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
						bottom_toolbar.setVisibility(View.VISIBLE);
						bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
						toolbar_visible=true;
						scroll_distance=0;
					}

					if(size==0)
					{
						bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
						toolbar_visible=false;
						scroll_distance=0;
						all_select_btn.setImageResource(R.drawable.select_icon);
					}
				}
				else
				{
					audioListViewModel.mselecteditems.put(pos,true);
					v.setSelected(true);
					((AudioListRecyclerViewItem)v).set_selected(true);
					audioListViewModel.audio_selected_array.add(audio_list.get(pos));
					bottom_toolbar.setVisibility(View.VISIBLE);
					bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
					toolbar_visible=true;
					scroll_distance=0;
					++size;
					if(size==num_all_audio)
					{
						all_select_btn.setImageResource(R.drawable.deselect_icon);
					}

				}
				file_number_view.setText(size+"/"+num_all_audio);
			}

		}

	}
	
	private class ToolbarButtonClickListener implements View.OnClickListener
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
			((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search_edittext.getWindowToken(),0);
			if (audioListViewModel.audio_selected_array.size() < 1) {
				return;
			}
			final Bundle bundle=new Bundle();
			final ArrayList<String> files_selected_array=new ArrayList<>();

			int id = p1.getId();
			if (id == R.id.toolbar_btn_1) {

				if (!AllAudioListFragment.FULLY_POPULATED) {
					Global.print(context,getString(R.string.wait_till_all_audios_populated));
					return;
				}

				audioListViewModel.audios_selected_for_delete = new ArrayList<>();
				int size=audioListViewModel.audio_selected_array.size();
				for(int i=0;i<size;++i)
				{
					AudioPOJO audio=audioListViewModel.audio_selected_array.get(i);
					files_selected_array.add(audio.getData());
					audioListViewModel.audios_selected_for_delete.add(audio);

				}
				final DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity = DeleteFileAlertDialogOtherActivity.getInstance(DELETE_FILE_REQUEST_CODE,files_selected_array,FileObjectType.FILE_TYPE);
				deleteFileAlertDialogOtherActivity.show(((AudioPlayerActivity) context).fm, "deletefilealertdialog");

			} else if (id == R.id.toolbar_btn_2) {
				AudioPlayerService.AUDIO_QUEUED_ARRAY = audioListViewModel.audio_selected_array;
				if (audioSelectListener != null && AudioPlayerService.AUDIO_QUEUED_ARRAY.size() != 0) {
					AudioPlayerService.CURRENT_PLAY_NUMBER = 0;
					AudioPOJO audio = AudioPlayerService.AUDIO_QUEUED_ARRAY.get(AudioPlayerService.CURRENT_PLAY_NUMBER);
					Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
					Uri data = Uri.withAppendedPath(uri, String.valueOf(audio.getId()));
					audioSelectListener.onAudioSelect(data, audio);

				}
				((AudioPlayerActivity) context).trigger_enable_disable_previous_next_btns();
				clear_selection();
			} else if (id == R.id.toolbar_btn_3) {

				AudioSaveListDialog audioSaveListDialog = AudioSaveListDialog.getInstance(SAVE_AUDIO_LIST_REQUEST_CODE);
				audioSaveListDialog.show(((AudioPlayerActivity) context).getSupportFragmentManager(), "");
			} else if (id == R.id.toolbar_btn_4) {
				//listPopWindow.showAsDropDown(p1,0,-(Global.ACTION_BAR_HEIGHT+listview_height+Global.FOUR_DP));
				listPopWindow.showAtLocation(bottom_toolbar,Gravity.BOTTOM|Gravity.END,0,Global.ACTION_BAR_HEIGHT+Global.FOUR_DP);

			}
		}

	}
	


}
