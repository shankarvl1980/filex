package svl.kadatha.filex;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
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
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AudioSavedListDetailsDialog extends DialogFragment
{
	private CurrentListRecyclerViewAdapter currentAudioListRecyclerViewAdapter;
	private Context context;
	private RecyclerView CurrentAudioListRecyclerview;
	private TextView file_number_view;
	private ImageButton all_select_btn;
	private TextView empty_audio_list_tv;
	private Toolbar bottom_toolbar;
	private Button remove_btn;
	private Button play_btn;
	private Button overflow_btn;
    //public SparseBooleanArray mselecteditems=new SparseBooleanArray();
	//public List<AudioPOJO> audio_selected_array=new ArrayList<>();
	public List<AudioPOJO> clicked_audio_list,total_audio_list;
	private AudioSelectListener audioSelectListener;
	private String audio_list_clicked_name;
	private boolean whether_saved_play_list;
	private AsyncTaskStatus asyncTaskStatus;
	private int number_button=3;
	private boolean toolbar_visible;
	private int scroll_distance;
	private int num_all_audio;
	private boolean whether_audios_set_to_current_list;
	private ConstraintLayout search_toolbar;
	private EditText search_edittext;
	private boolean search_toolbar_visible;
	private PopupWindow listPopWindow;
	private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
	private int playing_audio_text_color,rest_audio_text_color;
	private int listview_height;
	private AudioListViewModel audioListViewModel;
	private FrameLayout progress_bar;

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

		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			int saved_audio_clicked_pos=bundle.getInt("pos");
			if(saved_audio_clicked_pos!=0)
			{
				whether_saved_play_list=true;
				number_button=4;
			}
			audio_list_clicked_name=bundle.getString("list_name");
		}
		

		list_popupwindowpojos=new ArrayList<>();
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon, getString(R.string.send)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties)));
		playing_audio_text_color=getResources().getColor(R.color.light_item_select_text_color);
		TypedValue typedValue=new TypedValue();
		Resources.Theme theme=context.getTheme();
		theme.resolveAttribute(R.attr.recycler_text_color,typedValue,true);
		rest_audio_text_color=typedValue.data;

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v=inflater.inflate(R.layout.fragment_album_details,container,false);
		TextView dialog_title = v.findViewById(R.id.album_details_panel_title_TextView);
		dialog_title.setText(audio_list_clicked_name);
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
					int size=clicked_audio_list.size();

					if(audioListViewModel.mselecteditems.size()<size)
					{
						audioListViewModel.mselecteditems=new SparseBooleanArray();
						audioListViewModel.audio_selected_array=new ArrayList<>();
						for(int i=0;i<size;++i)
						{
							audioListViewModel.mselecteditems.put(i,true);
							audioListViewModel.audio_selected_array.add(clicked_audio_list.get(i));
						}
						
						currentAudioListRecyclerViewAdapter.notifyDataSetChanged();
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
				currentAudioListRecyclerViewAdapter.getFilter().filter(s.toString());
			}
		});

		ImageButton search_cancel_btn = v.findViewById(R.id.album_details_search_view_cancel_button);
		search_cancel_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				set_visibility_searchbar(false);
			}
		});

		file_number_view=v.findViewById(R.id.album_details_file_number);
		CurrentAudioListRecyclerview=v.findViewById(R.id.album_details_recyclerview);
		CurrentAudioListRecyclerview.setLayoutManager(new LinearLayoutManager(context));
		CurrentAudioListRecyclerview.addOnScrollListener(new RecyclerView.OnScrollListener()
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
		empty_audio_list_tv=v.findViewById(R.id.album_details_empty_list_tv);
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
		
		EquallyDistributedButtonsWithTextLayout tb_layout=new EquallyDistributedButtonsWithTextLayout(context,number_button,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH);

		int[] drawables;
		String[] titles;
		if(number_button==4)
		{

			drawables = new int[]{R.drawable.remove_list_icon, R.drawable.play_icon, R.drawable.add_list_icon,R.drawable.overflow_icon};
			titles = new String[]{getString(R.string.remove), getString(R.string.play), getString(R.string.list),getString(R.string.more)};
		}
		else
		{

			drawables = new int[]{R.drawable.remove_list_icon, R.drawable.play_icon, R.drawable.overflow_icon};
			titles = new String[]{getString(R.string.remove), getString(R.string.play), getString(R.string.more)};
		}
		tb_layout.setResourceImageDrawables(drawables,titles);

		ToolbarButtonClickListener toolbarButtonClickListener=new ToolbarButtonClickListener();
		bottom_toolbar=v.findViewById(R.id.album_details_bottom_toolbar);
		
		bottom_toolbar.addView(tb_layout);

		remove_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_1);
		play_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_2);

		if(number_button==4)
		{
            Button add_to_list_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
			overflow_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_4);
			add_to_list_btn.setOnClickListener(toolbarButtonClickListener);
		}
		else
		{

			overflow_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_3);
		}

		remove_btn.setOnClickListener(toolbarButtonClickListener);
		play_btn.setOnClickListener(toolbarButtonClickListener);
		overflow_btn.setOnClickListener(toolbarButtonClickListener);


		listPopWindow=new PopupWindow(context);
		ListView listView=new ListView(context);
		listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapater(context,list_popupwindowpojos));
		listPopWindow.setContentView(listView);
		listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popupwindow_width));
		listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		listPopWindow.setFocusable(true);
		listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.list_popup_background));
		listview_height=Global.GET_HEIGHT_LIST_VIEW(listView);
		listView.setOnItemClickListener(new ListPopupWindowClickListener());
		


		asyncTaskStatus=AsyncTaskStatus.STARTED;

		audioListViewModel=new ViewModelProvider(this).get(AudioListViewModel.class);
		audioListViewModel.fetch_saved_audio_list(audio_list_clicked_name,whether_saved_play_list);

		audioListViewModel.isFinished.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean aBoolean) {
				if(aBoolean)
				{
					clicked_audio_list=audioListViewModel.audio_list;
					total_audio_list=audioListViewModel.audio_list;
					currentAudioListRecyclerViewAdapter=new CurrentListRecyclerViewAdapter();
					CurrentAudioListRecyclerview.setAdapter(currentAudioListRecyclerViewAdapter);
					num_all_audio=total_audio_list.size();
					if(num_all_audio==0)
					{
						CurrentAudioListRecyclerview.setVisibility(View.GONE);
						empty_audio_list_tv.setVisibility(View.VISIBLE);
					}
					file_number_view.setText(audioListViewModel.mselecteditems.size()+"/"+num_all_audio);
					if(!whether_saved_play_list)CurrentAudioListRecyclerview.scrollToPosition(AudioPlayerService.CURRENT_PLAY_NUMBER);
					progress_bar.setVisibility(View.GONE);
					asyncTaskStatus=AsyncTaskStatus.COMPLETED;
				}

			}
		});


		/*
		if(asyncTaskStatus!=AsyncTaskStatus.STARTED)
		{
			new FetchAudioListDetailsAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}

		 */
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


		return v;
	}


	private void remove_and_save(String list_name,List<AudioPOJO> audio_list_to_be_removed, SparseBooleanArray index)
	{
		
		ProgressBarFragment pbf=ProgressBarFragment.newInstance();
		pbf.show(((AudioPlayerActivity)context).fm,"");
		if(!whether_saved_play_list)
		{
			AudioPlayerService.AUDIO_QUEUED_ARRAY.removeAll(audio_list_to_be_removed);
			int size=audio_list_to_be_removed.size();
			for(int i=0;i<size;++i)
			{
				int key=index.keyAt(i);
				if(AudioPlayerService.CURRENT_PLAY_NUMBER>=key)
				{
					AudioPlayerService.CURRENT_PLAY_NUMBER--;
				}
			}
		}

		clicked_audio_list.removeAll(audio_list_to_be_removed);
		total_audio_list.removeAll(audio_list_to_be_removed);
		num_all_audio=total_audio_list.size();
		if(num_all_audio==0)
		{
			CurrentAudioListRecyclerview.setVisibility(View.GONE);
			empty_audio_list_tv.setVisibility(View.VISIBLE);
		}
		file_number_view.setText(audioListViewModel.mselecteditems.size()+"/"+num_all_audio);


		if(AudioPlayerService.CURRENT_PLAY_NUMBER<0)
		{
			AudioPlayerService.CURRENT_PLAY_NUMBER=0;
		}
		if(whether_saved_play_list)
		{
			((AudioPlayerActivity)context).audioDatabaseHelper.delete(list_name,audio_list_to_be_removed);
		}
		pbf.dismissAllowingStateLoss();
	}
	
	public void onAudioChange()
	{
		currentAudioListRecyclerViewAdapter.notifyDataSetChanged();
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


/*
	@Override
	public void onDestroyView()
	{
		// TODO: Implement this method
		
		if(getDialog()!=null && getRetainInstance())
		{
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

 */
	/*
	private class FetchAudioListDetailsAsyncTask extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
	{
		ProgressBarFragment pbf;
		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			asyncTaskStatus=AsyncTaskStatus.STARTED;
			pbf=ProgressBarFragment.newInstance();
			pbf.show(((AudioPlayerActivity)context).fm,"");
		}

		@Override
		protected void onCancelled(Void result)
		{
			// TODO: Implement this method
			super.onCancelled(result);
			pbf.dismissAllowingStateLoss();
		}

		
		@Override
		protected Void doInBackground(Void[] p1)
		{
			// TODO: Implement this method
			if(clicked_audio_list!=null)
			{
				return null;
			}
			clicked_audio_list=new ArrayList<>();
			fetch_audio_list(audio_list_clicked_name,whether_saved_play_list);
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			currentAudioListRecyclerViewAdapter=new CurrentListRecyclerViewAdapter();
			CurrentAudioListRecyclerview.setAdapter(currentAudioListRecyclerViewAdapter);
			num_all_audio=total_audio_list.size();
			if(num_all_audio==0)
			{
				CurrentAudioListRecyclerview.setVisibility(View.GONE);
				empty_audio_list_tv.setVisibility(View.VISIBLE);
			}
			file_number_view.setText(audioListViewModel.mselecteditems.size()+"/"+num_all_audio);
			if(!whether_saved_play_list)CurrentAudioListRecyclerview.scrollToPosition(AudioPlayerService.CURRENT_PLAY_NUMBER);
			pbf.dismissAllowingStateLoss();
			asyncTaskStatus=AsyncTaskStatus.COMPLETED;
		}
	}

	 */
	/*
	private void fetch_audio_list(String list_name, boolean whether_saved_play_list)
	{

		if(!whether_saved_play_list)
		{
			for(AudioPOJO audio:AudioPlayerService.AUDIO_QUEUED_ARRAY)
			{
				if(new File(audio.getData()).exists())
				{
					clicked_audio_list.add(audio);
				}
			}
		}
		else if(AudioPlayerActivity.AUDIO_SAVED_LIST.contains(list_name))
		{
			audio_list_clicked_name=list_name;
			clicked_audio_list=((AudioPlayerActivity)context).audioDatabaseHelper.getAudioList(list_name);
			Iterator<AudioPOJO> it=clicked_audio_list.iterator();
			while(it.hasNext())
			{
				AudioPOJO audio=it.next();
				if(!new File(audio.getData()).exists())
				{
					((AudioPlayerActivity)context).audioDatabaseHelper.delete(list_name,audio.getId());
					it.remove();
				}
			}

		}
		total_audio_list=clicked_audio_list;
	}

	 */


	public void clear_selection()
	{
		audioListViewModel.audio_selected_array=new ArrayList<>();
		audioListViewModel.mselecteditems=new SparseBooleanArray();
		if(currentAudioListRecyclerViewAdapter!=null)currentAudioListRecyclerViewAdapter.notifyDataSetChanged();
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
			remove_btn.setAlpha(Global.ENABLE_ALFA);
			play_btn.setAlpha(Global.ENABLE_ALFA);
			overflow_btn.setAlpha(Global.ENABLE_ALFA);
		}
		else
		{
			remove_btn.setAlpha(Global.DISABLE_ALFA);
			play_btn.setAlpha(Global.DISABLE_ALFA);
			overflow_btn.setAlpha(Global.DISABLE_ALFA);
		}
		remove_btn.setEnabled(enable);
		play_btn.setEnabled(enable);
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
			currentAudioListRecyclerViewAdapter.getFilter().filter(null);
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
			if(audioListViewModel.audio_selected_array.size()<1)
			{
				return;
			}

			int id = p1.getId();
			if (id == R.id.toolbar_btn_1) {
				remove_and_save(audio_list_clicked_name, audioListViewModel.audio_selected_array, audioListViewModel.mselecteditems);
				Global.print(context,getString(R.string.removed_the_selected_audios));
				clear_selection();
			} else if (id == R.id.toolbar_btn_2) {
				AudioPlayerService.AUDIO_QUEUED_ARRAY = new ArrayList<>();
				AudioPlayerService.AUDIO_QUEUED_ARRAY=audioListViewModel.audio_selected_array;
				if(!whether_saved_play_list)
				{
					clicked_audio_list.clear();
					clicked_audio_list.addAll(audioListViewModel.audio_selected_array);
					currentAudioListRecyclerViewAdapter.notifyDataSetChanged();
				}

				if (AudioPlayerService.AUDIO_QUEUED_ARRAY.size() != 0) {
					AudioPlayerService.CURRENT_PLAY_NUMBER = 0;
					AudioPOJO audio = AudioPlayerService.AUDIO_QUEUED_ARRAY.get(AudioPlayerService.CURRENT_PLAY_NUMBER);
					Uri data = null;
					File f = new File(audio.getData());
					if (f.exists()) {
						data = FileProvider.getUriForFile(context,Global.FILEX_PACKAGE+".provider",f);
					}


					if (audioSelectListener != null) {
						audioSelectListener.onAudioSelect(data, audio);
					}
				}
				clear_selection();
			} else if (id == R.id.toolbar_btn_3) {
				if (number_button == 4) {

					AudioPlayerService.AUDIO_QUEUED_ARRAY.addAll(audioListViewModel.audio_selected_array);
					Global.print(context,getString(R.string.added_audios_current_play_list));
					clear_selection();
				} else {
					//listPopWindow.showAsDropDown(p1,0,-(Global.ACTION_BAR_HEIGHT+listview_height+Global.FOUR_DP));
					listPopWindow.showAtLocation(bottom_toolbar,Gravity.BOTTOM|Gravity.END,0,Global.ACTION_BAR_HEIGHT+Global.FOUR_DP);

				}
			} else if (id == R.id.toolbar_btn_4) {

				listPopWindow.showAsDropDown(p1,0,-(Global.ACTION_BAR_HEIGHT+listview_height));
			}

			((AudioPlayerActivity)context).trigger_enable_disable_previous_next_btns();
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
	private class CurrentListRecyclerViewAdapter extends RecyclerView.Adapter <CurrentListRecyclerViewAdapter.ViewHolder> implements Filterable
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
					AudioPOJO audio=clicked_audio_list.get(pos);
					Uri data;
					File f=new File(audio.getData());
					if(f.exists())
					{
						data=FileProvider.getUriForFile(context,Global.FILEX_PACKAGE+".provider",f);
					}
					else
					{
						data=null;
					}
	

					if(whether_saved_play_list && !whether_audios_set_to_current_list)
					{
						AudioPlayerService.AUDIO_QUEUED_ARRAY=new ArrayList<>();
						AudioPlayerService.AUDIO_QUEUED_ARRAY=clicked_audio_list;
						whether_audios_set_to_current_list=true;
					}
					AudioPlayerService.CURRENT_PLAY_NUMBER=pos;
					currentAudioListRecyclerViewAdapter.notifyDataSetChanged();
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

			private void onLongClickProcedure(View v,int size)
			{
				pos=getBindingAdapterPosition();

				if(audioListViewModel.mselecteditems.get(pos,false))
				{
					audioListViewModel.mselecteditems.delete(pos);

					v.setSelected(false);
					((AudioListRecyclerViewItem)v).set_selected(false);
					audioListViewModel.audio_selected_array.remove(clicked_audio_list.get(pos));
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
					audioListViewModel.audio_selected_array.add(clicked_audio_list.get(pos));
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

	
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
		{
			return new ViewHolder(new AudioListRecyclerViewItem(context,true));
		}

		@Override
		public void onBindViewHolder(CurrentListRecyclerViewAdapter.ViewHolder p1, int p2)
		{
			AudioPOJO audio=clicked_audio_list.get(p2);
			String title=audio.getTitle();
			String album=getString(R.string.album_colon)+" "+audio.getAlbum();
			long duration=0L;
			String duration_string=audio.getDuration();
			if(duration_string!=null) duration=Long.parseLong(duration_string);
			String duration_str=getString(R.string.duration_colon)+" "+ (String.format("%d:%02d",duration/1000/60,duration/1000%60));
			String artist=getString(R.string.artists_colon)+" "+audio.getArtist();
			boolean item_selected=audioListViewModel.mselecteditems.get(p2,false);

			if(!whether_saved_play_list && p2==AudioPlayerService.CURRENT_PLAY_NUMBER)
			{
				p1.view.titletextview.setTextColor(playing_audio_text_color);
				p1.view.albumtextview.setTextColor(playing_audio_text_color);
				p1.view.durationtextview.setTextColor(playing_audio_text_color);
				p1.view.artisttextview.setTextColor(playing_audio_text_color);
			}
			else
			{
				p1.view.titletextview.setTextColor(rest_audio_text_color);
				p1.view.albumtextview.setTextColor(rest_audio_text_color);
				p1.view.durationtextview.setTextColor(rest_audio_text_color);
				p1.view.artisttextview.setTextColor(rest_audio_text_color);
			}

			p1.view.setData(title,album,duration_str,artist,item_selected);
			p1.view.setSelected(item_selected);

		}


		@Override
		public int getItemCount()
		{
			return clicked_audio_list.size();
		}

		@Override
		public Filter getFilter() {
			return new Filter() {
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {

					clicked_audio_list=new ArrayList<>();
					if(constraint==null || constraint.length()==0)
					{
						clicked_audio_list=total_audio_list;
					}
					else
					{
						String pattern=constraint.toString().toLowerCase().trim();
						for(int i=0;i<num_all_audio;++i)
						{
							AudioPOJO audioPOJO=total_audio_list.get(i);
							if(audioPOJO.getLowerTitle().contains(pattern))
							{
								clicked_audio_list.add(audioPOJO);
							}
						}
					}

					return new FilterResults();
				}

				@Override
				protected void publishResults(CharSequence constraint, FilterResults results) {
					int t=clicked_audio_list.size();
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


	 interface AudioSelectListener
	 {
	 	void onAudioSelect(Uri data, AudioPOJO audio);
	 }

	 public void setAudioSelectListener(AudioSelectListener listener)
	 {
	 	audioSelectListener=listener;
	 }


}
