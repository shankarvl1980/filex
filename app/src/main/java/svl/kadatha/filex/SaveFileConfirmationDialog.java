package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class SaveFileConfirmationDialog extends DialogFragment
{
    private SaveFileListener saveFileListener;
	private Context context;
	private boolean whether_closing;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		saveFileListener=((FileEditorActivity)context);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		Bundle bundle=getArguments();
        whether_closing = bundle.getBoolean("whether_closing");
		setCancelable(false);
	}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
        View v = inflater.inflate(R.layout.fragment_create_rename_delete, container, false);
        TextView dialog_heading_textview = v.findViewById(R.id.dialog_fragment_rename_delete_title);
        TextView dialog_message_textview = v.findViewById(R.id.dialog_fragment_rename_delete_message);


        EditText new_file_name_edittext = v.findViewById(R.id.dialog_fragment_rename_delete_newfilename);
        TextView no_files_textview = v.findViewById(R.id.dialog_fragment_rename_delete_no_of_files);
        TextView size_files_textview = v.findViewById(R.id.dialog_fragment_rename_delete_total_size);
        ViewGroup buttons_layout = v.findViewById((R.id.fragment_create_rename_delete_button_layout));
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button okbutton = v.findViewById(R.id.first_button);
		okbutton.setText(R.string.yes);
        Button cancelbutton = v.findViewById(R.id.second_button);
		cancelbutton.setText(R.string.no);

		dialog_heading_textview.setText(R.string.save);
		dialog_message_textview.setText(R.string.file_has_been_modified_do_you_want_save_the_file);
		new_file_name_edittext.setVisibility(View.GONE);
		no_files_textview.setVisibility(View.GONE);
		size_files_textview.setVisibility(View.GONE);
		
		okbutton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if(saveFileListener!=null)
				{
					if(whether_closing)
					{
						saveFileListener.on_being_closed(true);
					}
					else
					{
						saveFileListener.next_action(true);
					}

				}
				dismissAllowingStateLoss();
			}
		});
		
		cancelbutton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if(whether_closing)
				{
					saveFileListener.on_being_closed(false);
				}
				else
				{
					saveFileListener.next_action(false);
				}
				dismissAllowingStateLoss();
			}
		});

		return v;
	}
	
	public static SaveFileConfirmationDialog getInstance(boolean whether_closing)
	{
		SaveFileConfirmationDialog dialog=new SaveFileConfirmationDialog();
		Bundle bundle=new Bundle();
		bundle.putBoolean("whether_closing",whether_closing);
		dialog.setArguments(bundle);
		return dialog;
	}
	

	@Override
	public void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		Window window=getDialog().getWindow();
		window.setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		
	}

	interface SaveFileListener
	{
		void next_action(boolean save);
		void on_being_closed(boolean to_close_after_save);
	}

}
