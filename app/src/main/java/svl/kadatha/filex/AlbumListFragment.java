package svl.kadatha.filex;

import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.AttributeSet;
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
import android.widget.ImageView;
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

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;



public class AlbumListFragment extends Fragment
{
	private Context context;
	private List<AlbumPOJO> album_list, total_album_list;
	private Button play_btn;
	private Button save_btn;
	private Button all_select_btn;
	private TextView file_number_view;
	private AlbumListRecyclerViewAdapter albumListRecyclerViewAdapter;
	private RecyclerView recyclerview;
	private Toolbar bottom_toolbar;
	private AudioSelectListener audioSelectListener;
	private AudioFragmentListener audioFragmentListener;

	private boolean toolbar_visible=true;
	private int scroll_distance;
	private FrameLayout progress_bar;
	private TextView empty_tv;
	private int num_all_album;
	private AudioPlayerActivity.SearchFilterListener searchFilterListener;
	public AudioListViewModel audioListViewModel;
	private static final String SAVE_AUDIO_LIST_REQUEST_CODE="album_list_save_audio_request_code";
	private static final String AUDIO_SELECT_REQUEST_CODE="album_details_audio_select_request_code";
	private AppCompatActivity activity;
	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		activity= (AppCompatActivity) context;
		if(activity instanceof svl.kadatha.filex.AudioSelectListener)
		{
			audioSelectListener= (AudioSelectListener) activity;
		}
		if(activity instanceof AudioFragmentListener)
		{
			audioFragmentListener=(AudioFragmentListener) activity;
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		audioSelectListener=null;
		audioFragmentListener=null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v=inflater.inflate(R.layout.fragment_album_list,container,false);
		file_number_view=v.findViewById(R.id.album_list_file_number);
		bottom_toolbar=v.findViewById(R.id.album_list_bottom_toolbar);
		EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(context,4,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
		int[] bottom_drawables ={R.drawable.search_icon,R.drawable.play_icon,R.drawable.add_list_icon,R.drawable.select_icon};
		String [] titles={getString(R.string.search),getString(R.string.play),getString(R.string.list),getString(R.string.select)};
		tb_layout.setResourceImageDrawables(bottom_drawables,titles);

		bottom_toolbar.addView(tb_layout);
		Button search_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
		play_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_2);
		save_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_3);
		all_select_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_4);

		ToolbarClickListener toolbarClickListener = new ToolbarClickListener();
		search_btn.setOnClickListener(toolbarClickListener);
		play_btn.setOnClickListener(toolbarClickListener);
		save_btn.setOnClickListener(toolbarClickListener);
		all_select_btn.setOnClickListener(toolbarClickListener);

		recyclerview=v.findViewById(R.id.fragment_album_list_container);
		ItemSeparatorDecoration itemSeparatorDecoration =new ItemSeparatorDecoration(context,1, recyclerview);
		recyclerview.addItemDecoration(itemSeparatorDecoration);
		FastScrollerView fastScrollerView=v.findViewById(R.id.fastScroller_album_list);
		fastScrollerView.setRecyclerView(recyclerview);
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
		empty_tv=v.findViewById(R.id.album_list_empty);
		progress_bar=v.findViewById(R.id.album_list_progressbar);

		audioListViewModel=new ViewModelProvider(this).get(AudioListViewModel.class);
		audioListViewModel.listAlbum();
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
					album_list=audioListViewModel.album_list;
					total_album_list=audioListViewModel.album_list;
					albumListRecyclerViewAdapter=new AlbumListRecyclerViewAdapter();
					recyclerview.setAdapter(albumListRecyclerViewAdapter);
					num_all_album=total_album_list.size();
					if(num_all_album<=0)
					{
						recyclerview.setVisibility(View.GONE);
						empty_tv.setVisibility(View.VISIBLE);
						enable_disable_buttons(false);
					}

					file_number_view.setText(audioListViewModel.album_pojo_selected_items.size()+"/"+num_all_album);
				}
			}
		});


		audioListViewModel.isAudioFetchingFromAlbumFinished.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
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
					if(audioListViewModel.action.equals("p"))
					{
						if (audioSelectListener != null && !AudioPlayerService.AUDIO_QUEUED_ARRAY.isEmpty()) {
							AudioPlayerService.CURRENT_PLAY_NUMBER = 0;
							AudioPOJO audio = AudioPlayerService.AUDIO_QUEUED_ARRAY.get(AudioPlayerService.CURRENT_PLAY_NUMBER);
							Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
							Uri data = Uri.withAppendedPath(uri, String.valueOf(audio.getId()));
							audioSelectListener.onAudioSelect(data, audio);
						}
						clear_selection();
						if(audioFragmentListener!=null)
						{
							audioFragmentListener.refreshAudioPlayNavigationButtons();
						}

					}
					else if(audioListViewModel.action.equals("q") || audioListViewModel.action.equals("s"))
					{
						if(audioFragmentListener!=null)
						{
							audioFragmentListener.onAudioSave();
							audioFragmentListener.refreshAudioPlayNavigationButtons();
						}
						clear_selection();
					}

					audioListViewModel.isAudioFetchingFromAlbumFinished.setValue(AsyncTaskStatus.NOT_YET_STARTED);
				}
			}
		});

		int size=audioListViewModel.album_pojo_selected_items.size();
		enable_disable_buttons(size != 0);
		file_number_view.setText(size+"/"+num_all_album);

		getParentFragmentManager().setFragmentResultListener(SAVE_AUDIO_LIST_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(SAVE_AUDIO_LIST_REQUEST_CODE))
				{
					String list_name=result.getString("list_name");
					progress_bar.setVisibility(View.VISIBLE);
					audioListViewModel.listAudio(new ArrayList<>(audioListViewModel.album_pojo_selected_items.values()),list_name.equals("") ? "q" : "s",list_name);
				}
			}
		});

		getParentFragmentManager().setFragmentResultListener(AUDIO_SELECT_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(AUDIO_SELECT_REQUEST_CODE))
				{
					AudioPOJO audio=result.getParcelable("audio");
					int id=audio.getId();
					Uri uri= MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
					Uri data=Uri.withAppendedPath(uri,String.valueOf(id));
					if(audioSelectListener!=null)
					{
						audioSelectListener.onAudioSelect(data,audio);
					}
				}
			}
		});

		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		searchFilterListener=new AudioPlayerActivity.SearchFilterListener() {
			@Override
			public void onSearchFilter(String constraint) {
				albumListRecyclerViewAdapter.getFilter().filter(constraint);
			}
		};
		if(activity instanceof AudioPlayerActivity)
		{
			((AudioPlayerActivity)activity).addSearchFilterListener(searchFilterListener);
		}

	}

	@Override
	public void onPause() {
		super.onPause();
		if(audioFragmentListener!=null)
		{
			if(audioFragmentListener.getSearchBarVisibility())
			{
				audioFragmentListener.setSearchBarVisibility(false);
			}
		}
		if(activity instanceof AudioPlayerActivity)
		{
			((AudioPlayerActivity)activity).removeSearchFilterListener(searchFilterListener);
		}

	}

	private void enable_disable_buttons(boolean enable)
	{
		if(enable)
		{
			play_btn.setAlpha(Global.ENABLE_ALFA);
			save_btn.setAlpha(Global.ENABLE_ALFA);
		}
		else
		{
			play_btn.setAlpha(Global.DISABLE_ALFA);
			save_btn.setAlpha(Global.DISABLE_ALFA);
		}
		play_btn.setEnabled(enable);
		save_btn.setEnabled(enable);

	}
	private class ToolbarClickListener implements View.OnClickListener
	{
		@Override
		public void onClick(View p1)
		{
			if(progress_bar.getVisibility()==View.VISIBLE)
			{
				Global.print(context,getString(R.string.please_wait));
				return;
			}
			int id = p1.getId();
			if (id == R.id.toolbar_btn_1) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
				if(audioFragmentListener!=null)
				{
					if(!audioFragmentListener.getSearchBarVisibility())
					{
						audioFragmentListener.setSearchBarVisibility(true);
					}
				}

			} else if (id == R.id.toolbar_btn_2) {
				if(audioFragmentListener!=null)
				{
					audioFragmentListener.hideKeyBoard();
				}

				if (audioListViewModel.album_pojo_selected_items.isEmpty()) {
					return;
				}
				progress_bar.setVisibility(View.VISIBLE);
				audioListViewModel.listAudio(new ArrayList<>(audioListViewModel.album_pojo_selected_items.values()),"p","");

			} else if (id == R.id.toolbar_btn_3) {
				if(audioFragmentListener!=null)
				{
					audioFragmentListener.hideKeyBoard();
				}

				if (audioListViewModel.album_pojo_selected_items.isEmpty()) {
					return;
				}

				AudioSaveListDialog audioSaveListDialog = AudioSaveListDialog.getInstance(SAVE_AUDIO_LIST_REQUEST_CODE);
				audioSaveListDialog.show(getParentFragmentManager(), "");

			} else if (id == R.id.toolbar_btn_4) {
				if(audioFragmentListener!=null)
				{
					audioFragmentListener.hideKeyBoard();
				}

				int size = album_list.size();
				if (audioListViewModel.album_pojo_selected_items.size() < size) {
					audioListViewModel.album_pojo_selected_items = new IndexedLinkedHashMap<>();

					for (int i = 0; i < size; ++i) {
						audioListViewModel.album_pojo_selected_items.put(i, album_list.get(i));
					}
					all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.deselect_icon,0,0);
					albumListRecyclerViewAdapter.notifyDataSetChanged();
				} else {
					clear_selection();
				}
				int s=audioListViewModel.album_pojo_selected_items.size();
				if (s >= 1) {
					bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
					toolbar_visible = true;
					scroll_distance = 0;
					enable_disable_buttons(true);
				}
				file_number_view.setText(s + "/" + num_all_album);
			}

		}

	}

	public void clear_selection()
	{
		audioListViewModel.album_pojo_selected_items =new IndexedLinkedHashMap<>();
		if(albumListRecyclerViewAdapter!=null)albumListRecyclerViewAdapter.notifyDataSetChanged();
		enable_disable_buttons(false);
		file_number_view.setText(audioListViewModel.album_pojo_selected_items.size()+"/"+num_all_album);
		all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.select_icon,0,0);
	} 
	

	private class AlbumListRecyclerViewAdapter extends RecyclerView.Adapter <AlbumListRecyclerViewAdapter.ViewHolder> implements Filterable
	{
		class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, AdapterView.OnLongClickListener
		{
			final AlbumListRecyclerViewItem view;
			int pos;
			ViewHolder (AlbumListRecyclerViewItem view)
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
				int size=audioListViewModel.album_pojo_selected_items.size();
				if(size>0)
				{
					onLongClickProcedure(p1,size);
				}
				else 
				{
					AlbumPOJO album=album_list.get(pos);
					AlbumDetailsDialog albumDetailsDialog=AlbumDetailsDialog.getInstance(AUDIO_SELECT_REQUEST_CODE, album.getId(), album.getAlbumName());
					albumDetailsDialog.show(getParentFragmentManager(),"");
					
				}
			}


			@Override
			public boolean onLongClick(View p1)
			{
				onLongClickProcedure(p1,audioListViewModel.album_pojo_selected_items.size());
				return true;
			}

			
			private void onLongClickProcedure(View v, int size)
			{
				pos=getBindingAdapterPosition();

				if(audioListViewModel.album_pojo_selected_items.containsKey(pos))
				{
					audioListViewModel.album_pojo_selected_items.remove(pos);
					v.setSelected(false);
					((AlbumListRecyclerViewItem)v).set_selected(false);
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
					audioListViewModel.album_pojo_selected_items.put(pos,album_list.get(pos));
					v.setSelected(true);
					((AlbumListRecyclerViewItem)v).set_selected(true);
			
					bottom_toolbar.setVisibility(View.VISIBLE);
					bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
					toolbar_visible=true;
					scroll_distance=0;
					enable_disable_buttons(true);
					++size;
					if(size==num_all_album)
					{
						all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.deselect_icon,0,0);
					}

				}
				file_number_view.setText(size+"/"+num_all_album);
			}
		}
		
	

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
		{
			return new ViewHolder(new AlbumListRecyclerViewItem(context));
		}

		@Override
		public void onBindViewHolder(AlbumListRecyclerViewAdapter.ViewHolder p1, int p2)
		{
			AlbumPOJO album=album_list.get(p2);
			String album_id=album.getId();
			String album_name=album.getAlbumName();
			String no_of_songs=getString(R.string.tracks)+" "+album.getNoOfSongs();
			String artist=getString(R.string.artists_colon)+" "+album.getArtist();
			boolean item_selected=audioListViewModel.album_pojo_selected_items.containsKey(p2);
			p1.view.setData(album_id,album_name,no_of_songs,artist,item_selected);
			p1.view.setSelected(item_selected);

		}

		@Override
		public int getItemCount()
		{
			return album_list.size();
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
					album_list=new ArrayList<>();
					if(constraint==null || constraint.length()==0)
					{
						album_list=total_album_list;
					}
					else
					{
						String pattern=constraint.toString().toLowerCase().trim();
						for(int i=0;i<num_all_album;++i)
						{
							AlbumPOJO albumPOJO=total_album_list.get(i);
							if(albumPOJO.getLowerAlbumName().contains(pattern))
							{
								album_list.add(albumPOJO);
							}
						}
					}

					int t=album_list.size();
					if(!audioListViewModel.album_pojo_selected_items.isEmpty())
					{
						clear_selection();
					}
					else
					{
						notifyDataSetChanged();
					}
					file_number_view.setText(audioListViewModel.album_pojo_selected_items.size()+"/"+t);

				}
			};
		}
	}
	

	private static class AlbumListRecyclerViewItem extends ViewGroup
	{
		private final Context context;
		private ImageView albumimageview,album_select_indicator;
		private TextView albumtextview, no_of_songs_textview,artisttextview;
		private int itemWidth,itemHeight;
		int imageview_dimension;
		private final int select_indicator_offset=Global.TEN_DP*4;

		AlbumListRecyclerViewItem(Context context)
		{
			super(context);
			this.context=context;
			init();
		}

		AlbumListRecyclerViewItem(Context context, AttributeSet attr)
		{
			super(context,attr);
			this.context=context;
			init();
		}

		AlbumListRecyclerViewItem(Context context, AttributeSet attr, int defStyle)
		{
			super(context,attr,defStyle);
			this.context=context;
			init();
		}

		private void init()
		{

			setBackground(ContextCompat.getDrawable(context,R.drawable.select_detail_recyclerview));
			View view = LayoutInflater.from(context).inflate(R.layout.audiolist_recyclerview_layout, this, true);
			albumimageview= view.findViewById(R.id.audio_image);
			album_select_indicator=view.findViewById(R.id.audio_select_indicator);
			albumtextview= view.findViewById(R.id.audio_file_title);
			no_of_songs_textview= view.findViewById(R.id.audio_file_duration);
			artisttextview= view.findViewById(R.id.audio_file_artist);

			int second_line_font_size;
			int first_line_font_size;
			if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==0)
			{
				first_line_font_size =Global.FONT_SIZE_SMALL_FIRST_LINE;
				second_line_font_size =Global.FONT_SIZE_SMALL_DETAILS_LINE;
				imageview_dimension =Global.IMAGEVIEW_DIMENSION_SMALL_LIST;

			}
			else if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==2)
			{
				first_line_font_size =Global.FONT_SIZE_LARGE_FIRST_LINE;
				second_line_font_size =Global.FONT_SIZE_LARGE_DETAILS_LINE;
				imageview_dimension =Global.IMAGEVIEW_DIMENSION_LARGE_LIST;
			}
			else
			{
				first_line_font_size =Global.FONT_SIZE_MEDIUM_FIRST_LINE;
				second_line_font_size =Global.FONT_SIZE_SMALL_DETAILS_LINE;
				imageview_dimension =Global.IMAGEVIEW_DIMENSION_MEDIUM_LIST;
			}
	
			albumtextview.setTextSize(first_line_font_size);
			no_of_songs_textview.setTextSize(second_line_font_size);
			artisttextview.setTextSize(second_line_font_size);
			
			albumimageview.getLayoutParams().width= imageview_dimension;
			albumimageview.getLayoutParams().height= imageview_dimension;

			if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
			{
				itemWidth =Global.SCREEN_HEIGHT;
			}
			else
			{
				itemWidth =Global.SCREEN_WIDTH;
			}
		}


		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
		{

			int iconheight,maxHeight=0;
			int usedWidth=Global.FOURTEEN_DP;

			measureChildWithMargins(album_select_indicator,widthMeasureSpec,0,heightMeasureSpec,0);
			measureChildWithMargins(albumimageview,widthMeasureSpec,usedWidth,heightMeasureSpec,0);
			usedWidth+=imageview_dimension;
			iconheight=imageview_dimension;


			measureChildWithMargins(albumtextview,widthMeasureSpec,usedWidth+Global.FOUR_DP+(Global.TEN_DP*2),heightMeasureSpec,0);
			maxHeight+=albumtextview.getMeasuredHeight();

			measureChildWithMargins(no_of_songs_textview,widthMeasureSpec,usedWidth+Global.FOUR_DP+(Global.TEN_DP*2),heightMeasureSpec,0);
			maxHeight+=no_of_songs_textview.getMeasuredHeight();


			measureChildWithMargins(artisttextview,widthMeasureSpec,usedWidth+Global.FOUR_DP+(Global.TEN_DP*2),heightMeasureSpec,0);
			maxHeight+=artisttextview.getMeasuredHeight();

			maxHeight=Math.max(iconheight,maxHeight);
			maxHeight+=Global.RECYCLERVIEW_ITEM_SPACING*2;
			itemHeight=maxHeight;
			setMeasuredDimension(widthMeasureSpec,maxHeight);

		}

		@Override
		protected void onLayout(boolean p1, int l, int t, int r, int b)
		{
			int x=Global.FOURTEEN_DP,y=Global.RECYCLERVIEW_ITEM_SPACING;

			View v=albumimageview;
			int measuredHeight = v.getMeasuredHeight();
			int measuredWidth = v.getMeasuredWidth();
			int d=(itemHeight-imageview_dimension)/2;
			v.layout(x,d,x+ measuredWidth,d+ measuredHeight);
			x+= measuredWidth +Global.TEN_DP;

			v=album_select_indicator;
			measuredHeight =v.getMeasuredHeight();
			measuredWidth =v.getMeasuredWidth();
			int a=itemWidth-select_indicator_offset;
			int file_select_indicator_height= measuredHeight;
			int c=(itemHeight-file_select_indicator_height)/2;
			v.layout(a,c,a+ measuredWidth,c+file_select_indicator_height);

			v=albumtextview;
			measuredHeight =v.getMeasuredHeight();
			measuredWidth =v.getMeasuredWidth();
			y=(itemHeight- measuredHeight -no_of_songs_textview.getMeasuredHeight()-artisttextview.getMeasuredHeight())/2;
			v.layout(x,y,x+ measuredWidth,y+ measuredHeight);
			y+= measuredHeight;


			v=no_of_songs_textview;
			measuredHeight =v.getMeasuredHeight();
			measuredWidth =v.getMeasuredWidth();
			v.layout(x,y,x+ measuredWidth,y+ measuredHeight);
			y+= measuredHeight;

			v=artisttextview;
			measuredHeight =v.getMeasuredHeight();
			measuredWidth =v.getMeasuredWidth();
			v.layout(x,y,x+ measuredWidth,y+ measuredHeight);

		}


		@Override
		protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
			return p instanceof MarginLayoutParams;
		}

		/**
		 * @return A set of default layout parameters when given a child with no layout parameters.
		 */
		@Override
		protected LayoutParams generateDefaultLayoutParams() {
			return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}

		/**
		 * @return A set of layout parameters created from attributes passed in XML.
		 */
		@Override
		public LayoutParams generateLayoutParams(AttributeSet attrs) {
			return new MarginLayoutParams(context, attrs);
		}

		/**
		 * Called when {@link #checkLayoutParams(LayoutParams)} fails.
		 *
		 * @return A set of valid layout parameters for this ViewGroup that copies appropriate/valid
		 * attributes from the supplied, not-so-good-parameters.
		 */
		@Override
		protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
			return generateDefaultLayoutParams();
		}


		public void setData(String album_id,String album,String duration,String artist, boolean item_selected)
		{
			GlideApp.with(context).load(Global.GET_ALBUM_ART_URI(album_id)).placeholder(R.drawable.audio_album_icon).error(R.drawable.audio_album_icon).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).dontAnimate().into(albumimageview);
			albumtextview.setText(album);
			album_select_indicator.setVisibility(item_selected  ? VISIBLE : INVISIBLE);
			no_of_songs_textview.setText(duration);
			artisttextview.setText(artist);
		}

		public void set_selected(boolean item_selected)
		{
			album_select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);
		}

	}

}
