package svl.kadatha.filex;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;



public class AlbumListFragment extends Fragment//implements LoaderManager.LoaderCallbacks<Cursor>
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

	public SparseBooleanArray mselecteditems=new SparseBooleanArray();
	public List<AlbumPOJO> album_selected_array=new ArrayList<>();
	public static Bitmap SELECTED_ALBUM_ART;
	private List<AlbumPOJO> album_selected_pojo_copy;
	private boolean toolbar_visible=true;
	private int scroll_distance;
	private AsyncTaskStatus asyncTaskStatus;
	private FrameLayout progress_bar;
	private TextView empty_tv;
	private int num_all_album;
	private AudioPlayerActivity.SearchFilterListener searchFilterListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		asyncTaskStatus=AsyncTaskStatus.NOT_YET_STARTED;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		context=getContext();
		Handler handler = new Handler();

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


		int size=mselecteditems.size();
		enable_disable_buttons(size != 0);
		file_number_view.setText(size+"/"+num_all_album);
		if(asyncTaskStatus!=AsyncTaskStatus.STARTED)
		{
			new AlbumListExtractor().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}

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

	private class AlbumListExtractor extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
	{
		final Uri album_uri=MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
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
			if(album_list!=null)
			{
				return null;
			}
			album_list=new ArrayList<>();
			total_album_list=new ArrayList<>();
			Cursor cursor=context.getContentResolver().query(album_uri,null,null,null,null);
			if(cursor!=null && cursor.getCount()>0)
			{
				while(cursor.moveToNext())
				{

					String id=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
					String album_name=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
					String artist=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
					String no_of_songs=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
					String album_path=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
					Bitmap art=BitmapFactory.decodeFile(album_path);
					
					album_list.add(new AlbumPOJO(id,album_name,album_name.toLowerCase(),artist,no_of_songs,art));
				}
				total_album_list=album_list;
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
			albumListRecyclerViewAdapter=new AlbumListRecyclerViewAdapter();
			recyclerview.setAdapter(albumListRecyclerViewAdapter);
			num_all_album=total_album_list.size();
			if(num_all_album<=0)
			{
				recyclerview.setVisibility(View.GONE);
				empty_tv.setVisibility(View.VISIBLE);
				enable_disable_buttons(false);
			}
			
			file_number_view.setText(mselecteditems.size()+"/"+num_all_album);
			progress_bar.setVisibility(View.GONE);
			asyncTaskStatus=AsyncTaskStatus.COMPLETED;
		}
	}
	
	private class ToolbarClickListener implements View.OnClickListener
	{
		@Override
		public void onClick(View p1)
		{
			// TODO: Implement this method
			int id = p1.getId();
			if (id == R.id.toolbar_btn_1) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
				if(!((AudioPlayerActivity)context).search_toolbar_visible)
				{
					((AudioPlayerActivity) context).set_visibility_searchbar(true);
				}
			} else if (id == R.id.toolbar_btn_2) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity) context).search_edittext.getWindowToken(),0);
				if (album_selected_array.size() < 1) {
					return;
				}
				new AlbumListDetailsExtractor(album_selected_array, 'p', "").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				clear_selection();
			} else if (id == R.id.toolbar_btn_3) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity) context).search_edittext.getWindowToken(),0);
				if (album_selected_array.size() < 1) {
					return;
				}
				album_selected_pojo_copy = new ArrayList<>();
				album_selected_pojo_copy.addAll(album_selected_array);
				AudioSaveListDialog audioSaveListDialog = new AudioSaveListDialog();
				audioSaveListDialog.setSaveAudioListListener(new AudioSaveListDialog.SaveAudioListListener() {
					public void save_audio_list(String list_name) {
						if (list_name == null) {

							SaveNewAudioListDialog saveNewAudioListDialog = new SaveNewAudioListDialog();
							saveNewAudioListDialog.setOnSaveAudioListener(new SaveNewAudioListDialog.OnSaveAudioListListener() {
								public void save_audio_list(String list_name) {

									new AlbumListDetailsExtractor(album_selected_pojo_copy, 's', list_name).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

								}

							});
							saveNewAudioListDialog.show(((AudioPlayerActivity) context).getSupportFragmentManager(), "saveaudiolist_dialog");

						} else if (list_name.equals("")) {
							new AlbumListDetailsExtractor(album_selected_pojo_copy, 'q', "").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						} else {

							new AlbumListDetailsExtractor(album_selected_pojo_copy, 's', list_name).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


						}
					}
				});
				audioSaveListDialog.show(((AudioPlayerActivity) context).getSupportFragmentManager(), "");
				clear_selection();
			} else if (id == R.id.toolbar_btn_4) {
				((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((AudioPlayerActivity) context).search_edittext.getWindowToken(),0);
				int size = album_list.size();
				if (mselecteditems.size() < size) {
					mselecteditems = new SparseBooleanArray();
					album_selected_array = new ArrayList<>();
					for (int i = 0; i < size; ++i) {
						mselecteditems.put(i, true);
						album_selected_array.add(album_list.get(i));
					}
					all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.deselect_icon,0,0);
					albumListRecyclerViewAdapter.notifyDataSetChanged();
				} else {
					clear_selection();
				}
				int s=mselecteditems.size();
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
		album_selected_array=new ArrayList<>();
		mselecteditems=new SparseBooleanArray();
		if(albumListRecyclerViewAdapter!=null)albumListRecyclerViewAdapter.notifyDataSetChanged();
		enable_disable_buttons(false);
		file_number_view.setText(mselecteditems.size()+"/"+num_all_album);
		all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.select_icon,0,0);
	} 
	
	
	private class AlbumListDetailsExtractor extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
	{
		final char action;
		final String list_name;
		final List<AudioPOJO> extracted_audio_list=new ArrayList<>();
		final List<AlbumPOJO> albumList;
		final ProgressBarFragment pbf=ProgressBarFragment.getInstance();
		boolean list_created;
		
		AlbumListDetailsExtractor(List<AlbumPOJO> list,char action, String list_name)
		{
			this.action=action;
			this.list_name=list_name;
			this.albumList=list;
		}
		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			pbf.show(((AudioPlayerActivity)context).fm,"");
		}

		@Override
		protected Void doInBackground(Void[] p1)
		{
			// TODO: Implement this method
			for(AlbumPOJO Album:albumList)
			{
				String album_id=Album.getId();
				Uri uri=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				String where=MediaStore.Audio.Media.ALBUM_ID+"="+album_id;
				Cursor cursor=context.getContentResolver().query(uri,null,where,null,null);
				if(cursor!=null && cursor.getCount()>0)
				{
					while(cursor.moveToNext())
					{
						int id=cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
						String data=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
						String title=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
						String album=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
						String artist=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
						String duration=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

						if(new File(data).exists())
						{
							extracted_audio_list.add(new AudioPOJO(id,data,title,title.toLowerCase(),album,artist,duration, null,FileObjectType.FILE_TYPE));
						}
					}
				}
				assert cursor != null;
				cursor.close();
			}
			if(action=='q')
			{
				AudioPlayerService.AUDIO_QUEUED_ARRAY.addAll(extracted_audio_list);
				
			}
			else if(action=='s')
			{
				
				if(AudioPlayerActivity.AUDIO_SAVED_LIST.contains(list_name))
				{
					((AudioPlayerActivity)context).audioDatabaseHelper.insert(list_name,extracted_audio_list);
				}
				else
				{
					((AudioPlayerActivity)context).audioDatabaseHelper.createTable(list_name);
					((AudioPlayerActivity)context).audioDatabaseHelper.insert(list_name,extracted_audio_list);
					AudioPlayerActivity.AUDIO_SAVED_LIST.add(list_name);
					list_created=true;
				}
			}
			else
			{
				AudioPlayerService.AUDIO_QUEUED_ARRAY=new ArrayList<>();
				AudioPlayerService.AUDIO_QUEUED_ARRAY=extracted_audio_list;
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void[] values)
		{
			// TODO: Implement this method
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Void result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			if(action=='p')
			{
				if(audioSelectListener!=null && AudioPlayerService.AUDIO_QUEUED_ARRAY.size()!=0)
				{
					AudioPlayerService.CURRENT_PLAY_NUMBER=0;
					AudioPOJO audio=AudioPlayerService.AUDIO_QUEUED_ARRAY.get(AudioPlayerService.CURRENT_PLAY_NUMBER);
					Uri uri=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
					Uri data=Uri.withAppendedPath(uri,String.valueOf(audio.getId()));
					audioSelectListener.onAudioSelect(data,audio);
				}
			}
			else if(action=='s')
			{
				if(list_created)
				{
					print("'"+list_name+ "' "+getString(R.string.audio_list_created));
				}
				
				((AudioPlayerActivity)context).trigger_audio_list_saved_listener();
			}
			pbf.dismissAllowingStateLoss();
			((AudioPlayerActivity)context).trigger_enable_disable_previous_next_btns();
		
		}
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
				int size=mselecteditems.size();
				if(size>0)
				{
					onLongClickProcedure(p1,size);
				}
				else 
				{
					AlbumPOJO album=album_list.get(pos);
					SELECTED_ALBUM_ART=null;
					SELECTED_ALBUM_ART=album.getAlbumArt();
					Bundle bundle=new Bundle();
					bundle.putString("albumID",album.getId());
					bundle.putString("album_name",album.getAlbumName());
				
					AlbumDetailsDialog albumDetailsDialog=new AlbumDetailsDialog();
					albumDetailsDialog.setAudioSelectListener(new AlbumDetailsDialog.AudioSelectListener()
						{
							public void onAudioSelect(Uri data, AudioPOJO audio)
							{
								if(audioSelectListener!=null)
								{
									audioSelectListener.onAudioSelect(data,audio);
								}
							}
						});
					
					albumDetailsDialog.setArguments(bundle);
					albumDetailsDialog.show(((AudioPlayerActivity)context).fm,"");
					
				}

			}


			@Override
			public boolean onLongClick(View p1)
			{
				onLongClickProcedure(p1,mselecteditems.size());
				return true;
			}

			
			private void onLongClickProcedure(View v, int size)
			{
				pos=getBindingAdapterPosition();

				if(mselecteditems.get(pos,false))
				{
					mselecteditems.delete(pos);
					v.setSelected(false);
					((AlbumListRecyclerViewItem)v).set_selected(false);
					album_selected_array.remove(album_list.get(pos));
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
					mselecteditems.put(pos,true);
					v.setSelected(true);
					((AlbumListRecyclerViewItem)v).set_selected(true);
					album_selected_array.add(album_list.get(pos));
			
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
			String album_name=album.getAlbumName();
			String no_of_songs=getString(R.string.tracks_colon)+" "+album.getNoOfSongs();
			String artist=getString(R.string.artists_colon)+" "+album.getArtist();
			Bitmap album_art=album.getAlbumArt();
			boolean item_selected=mselecteditems.get(p2,false);
			p1.view.setData(album_art,album_name,no_of_songs,artist,item_selected);
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
					return new FilterResults();
				}

				@Override
				protected void publishResults(CharSequence constraint, FilterResults results) {
					int t=album_list.size();
					if(mselecteditems.size()>0)
					{
						clear_selection();
					}
					else
					{
						notifyDataSetChanged();
					}
					file_number_view.setText(mselecteditems.size()+"/"+t);

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
	

	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}
		
	private static class AlbumListRecyclerViewItem extends ViewGroup
	{
		private final Context context;
		private ImageView albumimageview,album_select_indicator;
		private TextView albumtextview, no_of_songs_textview,artisttextview;
		private int itemWidth,itemHeight;
		int imageview_dimension;

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

		}


		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
		{

			int iconheight,maxHeight=0;
			int usedWidth=Global.TEN_DP;


			if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
			{
				itemWidth =Global.SCREEN_HEIGHT;
			}
			else
			{
				itemWidth =Global.SCREEN_WIDTH;
			}

			measureChildWithMargins(album_select_indicator,widthMeasureSpec,0,heightMeasureSpec,0);
			measureChildWithMargins(albumimageview,widthMeasureSpec,usedWidth,heightMeasureSpec,0);
			usedWidth+=imageview_dimension;
			iconheight=imageview_dimension;


			measureChildWithMargins(albumtextview,widthMeasureSpec,usedWidth+Global.TEN_DP*2,heightMeasureSpec,0);
			maxHeight+=albumtextview.getMeasuredHeight();

			measureChildWithMargins(no_of_songs_textview,widthMeasureSpec,usedWidth+Global.TEN_DP*2,heightMeasureSpec,0);
			maxHeight+=no_of_songs_textview.getMeasuredHeight();


			measureChildWithMargins(artisttextview,widthMeasureSpec,usedWidth+Global.TEN_DP*2,heightMeasureSpec,0);
			maxHeight+=artisttextview.getMeasuredHeight();

			maxHeight=Math.max(iconheight,maxHeight);
			maxHeight+=Global.RECYCLERVIEW_ITEM_SPACING*2;
			itemHeight=maxHeight;
			setMeasuredDimension(widthMeasureSpec,maxHeight);

		}

		@Override
		protected void onLayout(boolean p1, int l, int t, int r, int b)
		{
			// TODO: Implement this method
			int x=Global.TEN_DP,y=Global.RECYCLERVIEW_ITEM_SPACING;

			View v=albumimageview;
			v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
			x+=v.getMeasuredWidth()+Global.TEN_DP;

			v=album_select_indicator;
			int a=itemWidth-Global.THIRTY_FOUR_DP;
			int file_select_indicator_height=v.getMeasuredHeight();
			int c=(itemHeight-file_select_indicator_height)/2;
			v.layout(a,c,a+v.getMeasuredWidth(),c+file_select_indicator_height);

			v=albumtextview;
			v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
			y+=v.getMeasuredHeight();


			v=no_of_songs_textview;
			v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
			y+=v.getMeasuredHeight();

			v=artisttextview;
			v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());

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


		public void setData(Bitmap art,String album,String duration,String artist, boolean item_selected)
		{
			if(art==null)
			{
				albumimageview.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.audio_file_icon));
			}
			else
			{
				albumimageview.setImageBitmap(art);
			}
			
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
