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
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

public class ArchiveReplaceConfirmationDialog extends DialogFragment
{

    private String zip_folder_name;
	private String archive_action;
	private Bundle bundle;
	private String request_code;
	private String new_name;


    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setCancelable(false);
		bundle=getArguments();
		request_code=bundle.getString("request_code");
		zip_folder_name=bundle.getString("zip_folder_name");
		archive_action=bundle.getString("archive_action");
		new_name=bundle.getString("new_name");

	}

	public static ArchiveReplaceConfirmationDialog getInstance(String request_code,Bundle bundle)
	{
		ArchiveReplaceConfirmationDialog archiveReplaceConfirmationDialog=new ArchiveReplaceConfirmationDialog();
		bundle.putString("request_code",request_code);
		archiveReplaceConfirmationDialog.setArguments(bundle);
		return archiveReplaceConfirmationDialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
        Context context = getContext();
		View v=inflater.inflate(R.layout.fragment_archivereplace_confirmation,container,false);
        TextView confirmation_message_textview = v.findViewById(R.id.dialog_fragment_archive_replace_message);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_archivereplace_confirmation_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button yes_button = buttons_layout.findViewById(R.id.first_button);
		yes_button.setText(R.string.yes);
        Button no_button = buttons_layout.findViewById(R.id.second_button);
		no_button.setText(R.string.no);
		if(archive_action!=null)
		{
			if(archive_action.equals(ArchiveSetUpDialog.ARCHIVE_ACTION_ZIP))
			{
				confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it)+" '"+zip_folder_name+".zip'");
			}
			else
			{
				confirmation_message_textview.setText(getString(R.string.a_folder_with_same_already_exists_do_you_want_to_overwrite_it)+" '"+zip_folder_name+"'");
			}
		}
		else
		{
			confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it)+" '"+new_name+"'");
		}

		yes_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				getParentFragmentManager().setFragmentResult(request_code,bundle);
				dismissAllowingStateLoss();
			}
			
		});
		
		no_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
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
		window.setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	}
}
