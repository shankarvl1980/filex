package svl.kadatha.filex;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class AudioSavedListFragment extends Fragment
{

	private Context context;

	private AudioSavedListRecyclerAdapter audio_saved_list_adapter;
	private List<String> saved_audio_list;
	private Button play_btn,remove_btn;
	private Button all_select_btn;
	private TextView file_number_view;
	private Toolbar bottom_toolbar;
	private AudioSavedListDetailsDialog audioSavedListDetailsDialog;
	private AudioSelectListener audioSelectListener;
	public List<String> audio_list_selected_array=new ArrayList<>();
	public SparseBooleanArray mselecteditems=new SparseBooleanArray();
	private boolean toolbar_visible=true;
	private int scroll_distance;
	private boolean AsyncExtractIsInProgress;
	private int num_all_audio_list;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		context=getContext();
		Handler handler = new Handler();

		View v=inflater.inflate(R.layout.fragment_audio_saved_list,container,false);
		bottom_toolbar=v.findViewById(R.id.audio_saved_list_bottom_toolbar);
		EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(context,3,Global.PORTRAIT_SCREEN_WIDTH,Global.LANDSCAPE_SCREEN_HEIGHT);
		int[] bottom_drawables ={R.drawable.play_icon,R.drawable.remove_list_icon,R.drawable.select_icon};
		String [] titles={getString(R.string.play),getString(R.string.remove),getString(R.string.select)};
		tb_layout.setResourceImageDrawables(bottom_drawables,titles);

		bottom_toolbar.addView(tb_layout);
		play_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_1);
		remove_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_2);
		all_select_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_3);

		ToolbarClickListener toolbarClickListener = new ToolbarClickListener();
		play_btn.setOnClickListener(toolbarClickListener);
		remove_btn.setOnClickListener(toolbarClickListener);
		all_select_btn.setOnClickListener(toolbarClickListener);

		file_number_view=v.findViewById(R.id.audio_saved_list_file_number);
		RecyclerView audio_recycler_view = v.findViewById(R.id.fragment_audio_saved_list_recyclerview);
		audio_recycler_view.addItemDecoration(Global.DIVIDERITEMDECORATION);
		audio_recycler_view.setLayoutManager(new LinearLayoutManager(context));
		audio_recycler_view.addOnScrollListener(new RecyclerView.OnScrollListener()
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
		
		saved_audio_list=new ArrayList<>();
		saved_audio_list.add(AudioPlayerActivity.CURRENT_PLAY_LIST);
		saved_audio_list.addAll(AudioPlayerActivity.AUDIO_SAVED_LIST);
		audio_saved_list_adapter=new AudioSavedListRecyclerAdapter();
		audio_recycler_view.setAdapter(audio_saved_list_adapter);
		
		num_all_audio_list=saved_audio_list.size();
		int size=mselecteditems.size();
		enable_disable_buttons(size != 0);
		
		file_number_view.setText(size+"/"+num_all_audio_list);
		return v;
	}

	@Override
	public void onAttach(Context context)
	{
		// TODO: Implement this method
		super.onAttach(context);

		 ((AudioPlayerActivity)context).addAudioCompletionListener(new AudioPlayerActivity.AudioCompletionListener()
		 {

			 @Override
			 public void onAudioCompletion() {
				 if(audioSavedListDetailsDialog!=null)
				 {
					 audioSavedListDetailsDialog.onAudioChange();
				 }
			 }



		 });
	}

	private void enable_disable_buttons(boolean enable)
	{

		if(enable)
		{
			play_btn.setAlpha(Global.ENABLE_ALFA);
			remove_btn.setAlpha(Global.ENABLE_ALFA);
		}
		else
		{
			play_btn.setAlpha(Global.DISABLE_ALFA);
			remove_btn.setAlpha(Global.DISABLE_ALFA);
		}
		play_btn.setEnabled(enable);
		remove_btn.setEnabled(enable);

	}
	
	
	public void toolbarSlideDown()
	{
		if(mselecteditems.size()==0)
		{
			bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
			toolbar_visible=false;
			scroll_distance=0;
		}
	}

	
	public void onSaveAudioList()
	{
		// TODO: Implement this method
		saved_audio_list=new ArrayList<>();
		saved_audio_list.add(AudioPlayerActivity.CURRENT_PLAY_LIST);
		saved_audio_list.addAll(AudioPlayerActivity.AUDIO_SAVED_LIST);
		clear_selection();
	
	}

	
	private class ToolbarClickListener implements View.OnClickListener
	{

		@Override
		public void onClick(View p1)
		{
			// TODO: Implement this method
			int id=p1.getId();
			if(id==R.id.toolbar_btn_1)
			{
				if (audio_list_selected_array.size() >= 1) {
					if(!AsyncExtractIsInProgress)
					{
						ExtractAudioFromSavedListAsyncTask extractAudioFromSavedListAsyncTask = new ExtractAudioFromSavedListAsyncTask(audio_list_selected_array);
						extractAudioFromSavedListAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					}
					clear_selection();
				}

			}
			else if(id==R.id.toolbar_btn_2)
			{
				if (audio_list_selected_array.size() >= 1) {
					if(audio_list_selected_array.contains(AudioPlayerActivity.CURRENT_PLAY_LIST))
					{
						AudioPlayerService.AUDIO_QUEUED_ARRAY=new ArrayList<>();
						audio_list_selected_array.remove(AudioPlayerActivity.CURRENT_PLAY_LIST);
					}
					for(String list_name:audio_list_selected_array)
					{
						AudioPlayerActivity.AUDIO_SAVED_LIST.remove(list_name);
						saved_audio_list.remove(list_name);
						((AudioPlayerActivity)context).audioDatabaseHelper.deleteTable(list_name);
					}
					((AudioPlayerActivity)context).tinyDB.putListString("audio_saved_list",AudioPlayerActivity.AUDIO_SAVED_LIST);
					num_all_audio_list=saved_audio_list.size();
					clear_selection();
				}

			}
			else if(id==R.id.toolbar_btn_3)
			{
				if(mselecteditems.size()<num_all_audio_list)
				{
					mselecteditems=new SparseBooleanArray();
					audio_list_selected_array=new ArrayList<>();

					for(int i=0;i<num_all_audio_list;++i)
					{
						mselecteditems.put(i,true);
						audio_list_selected_array.add(saved_audio_list.get(i));
					}
					all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.deselect_icon,0,0);
					audio_saved_list_adapter.notifyDataSetChanged();
				}
				else
				{
					clear_selection();

				}

				int s=mselecteditems.size();
				if (s >= 1) {
					bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
					toolbar_visible = true;
					scroll_distance = 0;
					enable_disable_buttons(true);
				}
				file_number_view.setText(s+"/"+num_all_audio_list);
			}
			else
			{
				clear_selection();
			}


			((AudioPlayerActivity)context).trigger_enable_disable_previous_next_btns();
		}

	}
	

	private List<AudioPOJO> fetch_audio_list(String list_name)
	{
		List<AudioPOJO> clicked_audio_list=new ArrayList<>();
		if(list_name.equals(AudioPlayerActivity.CURRENT_PLAY_LIST))
		{
			clicked_audio_list=AudioPlayerService.AUDIO_QUEUED_ARRAY;
		}

		else if(AudioPlayerActivity.AUDIO_SAVED_LIST.contains(list_name))
		{
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
		return clicked_audio_list;

	}
	

	public void clear_selection()
	{
		mselecteditems=new SparseBooleanArray();
		audio_list_selected_array=new ArrayList<>();
		if(audio_saved_list_adapter!=null)audio_saved_list_adapter.notifyDataSetChanged();
		enable_disable_buttons(false);
		file_number_view.setText(mselecteditems.size()+"/"+num_all_audio_list);
		all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.select_icon,0,0);
	}
	
	
	private class ExtractAudioFromSavedListAsyncTask extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
	{
		final List<String> audio_selected_list;
		final List<AudioPOJO> extracted_audio_list=new ArrayList<>();
		final ProgressBarFragment pbf;
		ExtractAudioFromSavedListAsyncTask(List<String> audio_selected_list)
		{
			this.audio_selected_list=audio_selected_list;
			pbf=ProgressBarFragment.getInstance();
		}
		
		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			AsyncExtractIsInProgress=true;
			pbf.show(((AudioPlayerActivity)context).fm,"");
		}

		@Override
		protected Void doInBackground(Void[] p1)
		{
			// TODO: Implement this method
			for(String list_name:audio_selected_list)
			{
				extracted_audio_list.addAll(fetch_audio_list(list_name));
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
			AudioPlayerService.AUDIO_QUEUED_ARRAY=new ArrayList<>();
			AudioPlayerService.AUDIO_QUEUED_ARRAY=extracted_audio_list;
			if(audioSelectListener!=null && AudioPlayerService.AUDIO_QUEUED_ARRAY.size()!=0)
			{
				AudioPlayerService.CURRENT_PLAY_NUMBER=0;
				AudioPOJO audio=AudioPlayerService.AUDIO_QUEUED_ARRAY.get(AudioPlayerService.CURRENT_PLAY_NUMBER);
				Uri data=null;
				File f=new File(audio.getData());
				if(f.exists())
				{
					data=Uri.fromFile(f);
				}

                audioSelectListener.onAudioSelect(data,audio);

			}
			pbf.dismissAllowingStateLoss();
			((AudioPlayerActivity)context).trigger_enable_disable_previous_next_btns();
			AsyncExtractIsInProgress=false;
		}
		
	}
	

	private class AudioSavedListRecyclerAdapter extends RecyclerView.Adapter<AudioSavedListRecyclerAdapter.ViewHolder>
	{
		int first_line_font_size,second_line_font_size;

		class ViewHolder extends RecyclerView.ViewHolder
		{
			final View view;
			final TextView textView;
			final ImageView select_indicator;
			int pos;

			ViewHolder(View view)
			{
				super(view);
				this.view=view;
				textView=view.findViewById(R.id.working_dir_name);
				select_indicator=view.findViewById(R.id.working_dir_select_indicator);
				select_indicator.setVisibility(View.INVISIBLE);
				view.setOnClickListener(new View.OnClickListener()
					{

						public void onClick(View p)
						{
							pos=getBindingAdapterPosition();
							int size=mselecteditems.size();
							if(size>0)
							{

								onLongClickProcedure(p,size);
							}
							else
							{
								ProgressBarFragment pbf=ProgressBarFragment.getInstance();
								pbf.show(((AudioPlayerActivity)context).fm,"");

								Bundle bundle=new Bundle();
								bundle.putInt("pos",pos);
								bundle.putString("list_name",saved_audio_list.get(pos));
								audioSavedListDetailsDialog=new AudioSavedListDetailsDialog();
								audioSavedListDetailsDialog.setAudioSelectListener(new AudioSavedListDetailsDialog.AudioSelectListener()
									{

										public void onAudioSelect(Uri data, AudioPOJO audio)
										{

											if(audioSelectListener!=null)
											{
												audioSelectListener.onAudioSelect(data,audio);
											}

										}

									});
								audioSavedListDetailsDialog.setArguments(bundle);
								audioSavedListDetailsDialog.show(((AudioPlayerActivity)context).fm,"");

								pbf.dismissAllowingStateLoss();
							}
						}

					});
				
				
				view.setOnLongClickListener(new View.OnLongClickListener()
					{
						public boolean onLongClick(View p)
						{
							onLongClickProcedure(p,mselecteditems.size());
							return true;

						}
					});
			}
			

			private void onLongClickProcedure(View v, int size)
			{
				pos=getBindingAdapterPosition();
				if(mselecteditems.get(pos,false))
				{
					v.setSelected(false);
					select_indicator.setVisibility(View.INVISIBLE);
					audio_list_selected_array.remove(saved_audio_list.get(pos));
					mselecteditems.delete(pos);
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
					v.setSelected(true);
					select_indicator.setVisibility(View.VISIBLE);
					audio_list_selected_array.add(saved_audio_list.get(pos));
					mselecteditems.put(pos,true);

					bottom_toolbar.setVisibility(View.VISIBLE);
					bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
					toolbar_visible=true;
					scroll_distance=0;
					enable_disable_buttons(true);
					++size;
					if(size==num_all_audio_list)
					{
						all_select_btn.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.deselect_icon,0,0);
					}

				}
				file_number_view.setText(size+"/"+num_all_audio_list);
			}

		}

		@Override
		public AudioSavedListRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
		{
			// TODO: Implement this method
			View itemview=LayoutInflater.from(p1.getContext()).inflate(R.layout.working_dir_recyclerview_layout,p1,false);
			return new ViewHolder(itemview);
		}

		@Override
		public void onBindViewHolder(AudioSavedListRecyclerAdapter.ViewHolder p1, int p2)
		{
			// TODO: Implement this method

			if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==0)
			{
				first_line_font_size=Global.FONT_SIZE_SMALL_FIRST_LINE;
				second_line_font_size=Global.FONT_SIZE_SMALL_DETAILS_LINE;

			}
			else if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==2)
			{
				first_line_font_size=Global.FONT_SIZE_LARGE_FIRST_LINE;
				second_line_font_size=Global.FONT_SIZE_LARGE_DETAILS_LINE;
			}
			else
			{
				first_line_font_size=Global.FONT_SIZE_MEDIUM_FIRST_LINE;
				second_line_font_size=Global.FONT_SIZE_SMALL_DETAILS_LINE;
			}
			
			
			p1.textView.setTextSize(first_line_font_size);
			p1.textView.setText(saved_audio_list.get(p2));
			boolean item_selected=mselecteditems.get(p2,false);
			p1.view.setSelected(item_selected);
			p1.select_indicator.setVisibility(item_selected ? View.VISIBLE : View.INVISIBLE);

		}

		@Override
		public int getItemCount()
		{
			// TODO: Implement this method
			num_all_audio_list=saved_audio_list.size();
			return num_all_audio_list;
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

}
