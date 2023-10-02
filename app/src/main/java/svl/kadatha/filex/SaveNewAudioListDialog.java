package svl.kadatha.filex;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class SaveNewAudioListDialog extends DialogFragment
{
    private EditText new_file_name_edittext;
    private Context context;
	private InputMethodManager imm;
	private String request_code;
	Bundle bundle;


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
		setCancelable(false);
		bundle=getArguments();
		request_code=bundle.getString("request_code");
	}

	public static SaveNewAudioListDialog getInstance(String request_code)
	{
		SaveNewAudioListDialog saveNewAudioListDialog=new SaveNewAudioListDialog();
		Bundle bundle=new Bundle();
		bundle.putString("request_code",request_code);
		saveNewAudioListDialog.setArguments(bundle);
		return saveNewAudioListDialog;
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
						Global.print(context,getString(R.string.name_field_cannot_be_empty));
						return;
					}

					if(!CheckString.isStringOnlyAlphabet(new_name))
					{
						Global.print(context,getString(R.string.name_should_contain_only_alphabets_without_spaces));
						return;
					}

					if(CheckString.whetherStringContainsSpecialCharacters(new_name))
					{
						Global.print(context,getString(R.string.avoid_name_involving_special_characters));
						return;
					}


					if(!new_name.matches("\\S+"))
					{
						Global.print(context,getString(R.string.name_contains_space));
						return;
					}
					
					if(AudioPlayerActivity.AUDIO_SAVED_LIST.contains(new_name))
					{
						Global.print(context,getString(R.string.a_list_exists_with_given_name));
						return;
					}

					bundle.putString("list_name",new_name);
					getParentFragmentManager().setFragmentResult(request_code,bundle);
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

}
