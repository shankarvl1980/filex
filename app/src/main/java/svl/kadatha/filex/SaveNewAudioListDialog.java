package svl.kadatha.filex;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.*;
import android.widget.AbsListView.*;
import android.view.inputmethod.*;
import android.graphics.drawable.*;
import android.graphics.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class SaveNewAudioListDialog extends DialogFragment
{
    private EditText new_file_name_edittext;
    private Context context;
	private InputMethodManager imm;
    private OnSaveAudioListListener onSaveAudioListListener;


	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		imm=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v=inflater.inflate(R.layout.fragment_create_rename_delete,container,false);
        TextView dialog_heading_textview = v.findViewById(R.id.dialog_fragment_rename_delete_title);
        TextView file_label_textview = v.findViewById(R.id.dialog_fragment_rename_delete_message);
		new_file_name_edittext=v.findViewById(R.id.dialog_fragment_rename_delete_newfilename);
        TextView no_of_files_textview = v.findViewById(R.id.dialog_fragment_rename_delete_no_of_files);
        TextView files_size_textview = v.findViewById(R.id.dialog_fragment_rename_delete_total_size);
		no_of_files_textview.setVisibility(View.GONE);
		files_size_textview.setVisibility(View.GONE);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_create_rename_delete_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button okbutton = buttons_layout.findViewById(R.id.first_button);
		okbutton.setText(R.string.ok);
        Button cancelbutton = buttons_layout.findViewById(R.id.second_button);
		cancelbutton.setText(R.string.cancel);


	
		dialog_heading_textview.setText(R.string.enter_name);
		file_label_textview.setText(R.string.list_name_colon);
		
		okbutton.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{


					String new_name=new_file_name_edittext.getText().toString().trim();
					if(new_name.equals("") || new_name.equals(null))
					{
						print("Name field cannot be empty");
						return;
					}
					if(CheckStringForSpecialCharacters.whetherStringContains(new_name))
					{
						print("Avoid name involving characters '\\*:?/'");
						return;
					}
					
					if(AudioPlayerActivity.AUDIO_SAVED_LIST.contains(new_name))
					{
						print("'"+new_name+"' can not be created. A file with the same name already exists");
						imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
						dismissAllowingStateLoss();
						return;
					}
				
					
					if(onSaveAudioListListener!=null)
					{
						onSaveAudioListListener.save_audio_list(new_name);
					}
					

					imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
					
					dismissAllowingStateLoss();
			

				}	


			});

		cancelbutton.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
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
		window.setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		
		new_file_name_edittext.requestFocus();
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

	}


	@Override
	public void onCancel(DialogInterface dialog)
	{
		// TODO: Implement this method

		imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
		super.onCancel(dialog);
	}



	@Override
	public void onDismiss(DialogInterface dialog)
	{
		// TODO: Implement this method
		imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
		super.onDismiss(dialog);

	}


	@Override
	public void onDestroyView() 
	{
		if (getDialog() != null && getRetainInstance()) 
		{
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

	interface OnSaveAudioListListener
	{
		void save_audio_list(String list_name);
	}
	
	public void setOnSaveAudioListener(OnSaveAudioListListener listener)
	{
		onSaveAudioListListener=listener;
	}
	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}

}
