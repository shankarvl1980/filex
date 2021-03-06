package svl.kadatha.filex;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.widget.RelativeLayout.*;
import android.graphics.drawable.*;
import android.graphics.*;
import android.content.*;

import androidx.fragment.app.DialogFragment;

public class FileReplaceConfirmationDialog extends DialogFragment
{

    private String duplicate_file_name;
	private CheckBox apply_all_checkbox;
	private FileReplaceListener fileReplaceListener;

    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setCancelable(false);
		
		Bundle bundle=getArguments();
		duplicate_file_name=bundle.getString("duplicate_file_name");
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		//return super.onCreateView(inflater, container, savedInstanceState);
        Context context = getContext();
		View v=inflater.inflate(R.layout.fragment_replace_confirmation,container,false);
        TextView confirmation_message_textview = v.findViewById(R.id.dialog_fragment_replace_message);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_replace_confirmation_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button yes_button = buttons_layout.findViewById(R.id.first_button);
		yes_button.setText(R.string.yes);
        Button no_button = buttons_layout.findViewById(R.id.second_button);
		no_button.setText(R.string.no);
		confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it)+" '"+duplicate_file_name+"'");
		apply_all_checkbox=v.findViewById(R.id.dialog_fragment_applyall_confirmationCheckBox);
		
		yes_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				
				if(fileReplaceListener!=null)
				{
					fileReplaceListener.onReplaceClick(true,apply_all_checkbox.isChecked());
				}
				dismissAllowingStateLoss();
			}
			
		});
		
		no_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{			
				
				if(fileReplaceListener!=null)
				{
					fileReplaceListener.onReplaceClick(false,apply_all_checkbox.isChecked());
				}
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
	
	public void setReplaceListener(FileReplaceListener fileReplaceListener)
	{
		this.fileReplaceListener=fileReplaceListener;
	}


	interface FileReplaceListener
	{
		void onReplaceClick(boolean replace, boolean replaceall);

	}
	
}
