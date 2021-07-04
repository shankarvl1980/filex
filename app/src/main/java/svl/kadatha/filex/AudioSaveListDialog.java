package svl.kadatha.filex;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class AudioSaveListDialog extends DialogFragment
{
    private Context context;
	private final ArrayList<String> saved_audio_list=new ArrayList<>();
    private final ArrayList<String> create_add_array=new ArrayList<>();
	private SaveAudioListListener saveAudioListListener;
	//private static SparseBooleanArray MSELECTEDITEMS;
	//private ArrayList<String> SAVED_AUDIO_LIST_SELECTED_ARRAY=new ArrayList<>();
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		
		super.onCreate(savedInstanceState);
		saved_audio_list.addAll(AudioPlayerActivity.AUDIO_SAVED_LIST);
		setRetainInstance(true);
		create_add_array.add(getString(R.string.create_new_list));
		create_add_array.add(getString(R.string.add_to_current_play_list));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		context=getContext();
		View v=inflater.inflate(R.layout.audio_save_list_dialog,container,false);
        RecyclerView create_list_view = v.findViewById(R.id.audio_save_list_create_add_recyclerview);
		create_list_view.setAdapter(new CreateAddListRecyclerAdapter(create_add_array));
		create_list_view.setLayoutManager(new LinearLayoutManager(context));
		create_list_view.addItemDecoration(Global.DIVIDERITEMDECORATION);
        RecyclerView audio_list_view = v.findViewById(R.id.audio_save_list_savedlist_recyclerview);
		audio_list_view.setAdapter(new AudioSavedListRecyclerAdapter(saved_audio_list));
		audio_list_view.setLayoutManager(new LinearLayoutManager(context));
		audio_list_view.addItemDecoration(Global.DIVIDERITEMDECORATION);

        ViewGroup button_layout = v.findViewById(R.id.dialog_audio_save_list_button_layout);
		button_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.PORTRAIT_DIALOG_WIDTH,Global.LANDSCAPE_DIALOG_WIDTH));
        Button cancel_buton = button_layout.findViewById(R.id.first_button);
		cancel_buton.setText(R.string.cancel);
		cancel_buton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View vi)
			{
				dismissAllowingStateLoss();
			}
		});
		return v;
	}


	@Override
	public void onResume()
	{
		// TODO: Implement this method
		super.onResume();

		Window window=getDialog().getWindow();
		if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
		{
			window.setLayout(Global.LANDSCAPE_DIALOG_WIDTH,Global.LANDSCAPE_DIALOG_WIDTH);
		}
		else
		{
			window.setLayout(Global.PORTRAIT_DIALOG_WIDTH,Global.PORTRAIT_DIALOG_HEIGHT);
		}
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


		/*
		Window window=getDialog().getWindow();
		WindowManager.LayoutParams params=window.getAttributes();
		int height=params.height;
		if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
		{
			window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_WIDTH);

		}
		else
		{
			window.setLayout(Global.DIALOG_WIDTH,Math.min(height,Global.DIALOG_HEIGHT));

		}

		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		 */
		
	}
	
	interface SaveAudioListListener
	{
		void save_audio_list(String list_name);
		
	}
	
	public void setSaveAudioListListener(SaveAudioListListener listener)
	{
		saveAudioListListener=listener;
	}
	
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
	
	private class CreateAddListRecyclerAdapter extends RecyclerView.Adapter<CreateAddListRecyclerAdapter.ViewHolder>
	{
		final List<String> audio_list;
		CreateAddListRecyclerAdapter(List<String>list)
		{
			audio_list=list;
		}
		class ViewHolder extends RecyclerView.ViewHolder
		{
			final View view;
			final TextView textView;
			int pos;

			ViewHolder(View v)
			{
				super(v);
				this.view=v;
				textView=view.findViewById(R.id.working_dir_name);
				textView.setGravity(Gravity.CENTER);
				view.setOnClickListener(new View.OnClickListener()
					{

						public void onClick(View p)
						{
							pos=getBindingAdapterPosition();

							ProgressBarFragment pbf=ProgressBarFragment.getInstance();
							pbf.show(((AudioPlayerActivity)context).getSupportFragmentManager(),"");
							if(saveAudioListListener!=null)
							{
								if(pos==0)
								{
									saveAudioListListener.save_audio_list(null);
								}
								else if(pos==1)
								{
									saveAudioListListener.save_audio_list("");
								}


							}
							pbf.dismissAllowingStateLoss();
							dismissAllowingStateLoss();
							
						}

					});


				view.setOnLongClickListener(new View.OnLongClickListener()
					{
						public boolean onLongClick(View p)
						{
							pos=getBindingAdapterPosition();
							return true;

						}
					});

			}

		}

		@Override
		public CreateAddListRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
		{
			// TODO: Implement this method

			View itemview=LayoutInflater.from(p1.getContext()).inflate(R.layout.working_dir_recyclerview_layout,p1,false);
			return new ViewHolder(itemview);
		}

		@Override
		public void onBindViewHolder(CreateAddListRecyclerAdapter.ViewHolder p1, int p2)
		{
			// TODO: Implement this method
			p1.textView.setText(audio_list.get(p2));

		}

		@Override
		public int getItemCount()
		{
			// TODO: Implement this method
			return audio_list.size();
		}

	}
	
	private class AudioSavedListRecyclerAdapter extends RecyclerView.Adapter<AudioSavedListRecyclerAdapter.ViewHolder>
	{
		final List<String> audio_list;
		AudioSavedListRecyclerAdapter(List<String>list)
		{
			audio_list=list;
		}
		class ViewHolder extends RecyclerView.ViewHolder
		{
			final View view;
			final TextView textView;
			int pos;

			ViewHolder(View v)
			{
				super(v);
				this.view=v;
				textView=view.findViewById(R.id.working_dir_name);
				
				view.setOnClickListener(new View.OnClickListener()
					{

						public void onClick(View p)
						{
							pos=getBindingAdapterPosition();
							ProgressBarFragment pbf=ProgressBarFragment.getInstance();
							pbf.show(((AudioPlayerActivity)context).getSupportFragmentManager(),"");
							if(saveAudioListListener!=null)
							{
								saveAudioListListener.save_audio_list(audio_list.get(pos));
							}
							pbf.dismissAllowingStateLoss();
							dismissAllowingStateLoss();
						}

					});
				
				
				view.setOnLongClickListener(new View.OnLongClickListener()
					{
						public boolean onLongClick(View p)
						{
							pos=getBindingAdapterPosition();
							return true;

						}
					});

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
			p1.textView.setText(audio_list.get(p2));

		}

		@Override
		public int getItemCount()
		{
			// TODO: Implement this method
			return audio_list.size();
		}

	}
	
}
