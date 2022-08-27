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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class AudioSaveListDialog extends DialogFragment
{
    private Context context;
	private final ArrayList<String> saved_audio_list=new ArrayList<>();
    private final ArrayList<String> create_add_array=new ArrayList<>();
	private String request_code;
	private Bundle bundle;
	private FragmentManager fragmentManager;

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
		bundle=getArguments();
		request_code=bundle.getString("request_code");
		fragmentManager=((AppCompatActivity)context).getSupportFragmentManager();
		saved_audio_list.addAll(AudioPlayerActivity.AUDIO_SAVED_LIST);
		setCancelable(false);
		create_add_array.add(getString(R.string.create_new_list));
		create_add_array.add(getString(R.string.add_to_current_play_list));
	}

	public static AudioSaveListDialog getInstance(String request_code)
	{
		AudioSaveListDialog audioSaveListDialog=new AudioSaveListDialog();
		Bundle bundle=new Bundle();
		bundle.putString("request_code",request_code);
		audioSaveListDialog.setArguments(bundle);
		return audioSaveListDialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
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
		button_layout.addView(new EquallyDistributedDialogButtonsLayout(context,1,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
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
			window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_WIDTH);
		}
		else
		{
			window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_HEIGHT);
		}
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

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
							if(pos==0)
							{
								SaveNewAudioListDialog saveNewAudioListDialog=SaveNewAudioListDialog.getInstance(request_code);
								saveNewAudioListDialog.show(fragmentManager,"");
							}
							else if(pos==1)
							{
								bundle.putString("list_name","");
								fragmentManager.setFragmentResult(request_code,bundle);
							}

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

			View itemview=LayoutInflater.from(context).inflate(R.layout.working_dir_recyclerview_layout,p1,false);
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
							bundle.putString("list_name",audio_list.get(pos));
							fragmentManager.setFragmentResult(request_code,bundle);
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
			
			View itemview=LayoutInflater.from(context).inflate(R.layout.working_dir_recyclerview_layout,p1,false);
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
