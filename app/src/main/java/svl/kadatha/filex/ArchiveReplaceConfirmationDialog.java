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

import java.util.ArrayList;

public class ArchiveReplaceConfirmationDialog extends DialogFragment
{

    private String zip_folder_name;
	private final ArrayList<String> files_selected_array=new ArrayList<>();
	private String archive_action;
	//private ArchiveReplaceDialogListener archiveReplaceDialogListener;
	private Bundle bdl;
	private String request_code;



    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setCancelable(false);
		Bundle bundle=getArguments();
        //String dest_folder = bundle.getString("dest_folder");
        //String zip_file_path = bundle.getString("zip_file_path");
        //String zip_file_name = bundle.getString("zip_file_name");
		request_code=bundle.getString("request_code");
		bdl=bundle.getBundle("bdl");
		zip_folder_name=bdl.getString("zip_folder_name");
		//files_selected_array.addAll(bundle.getStringArrayList("files_selected_array"));
		archive_action=bdl.getString("archive_action");

	}

	public static ArchiveReplaceConfirmationDialog getInstance(String request_code,Bundle bdl)
	{
		ArchiveReplaceConfirmationDialog archiveReplaceConfirmationDialog=new ArchiveReplaceConfirmationDialog();
		Bundle bundle=new Bundle();
//		bundle.getString("dest_folder");
//		bundle.getString("zip_file_path");
//		bundle.getString("zip_file_name");
		bundle.putString("request_code",request_code);
		bundle.putBundle("bdl",bdl);
//		bundle.putStringArrayList("files_selected_array",files_selected_array);
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
		if(archive_action.equals(ArchiveSetUpDialog.ARCHIVE_ACTION_ZIP))
		{
			confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it)+" '"+zip_folder_name+".zip'");
		}
		else
		{
			confirmation_message_textview.setText(getString(R.string.a_folder_with_same_already_exists_do_you_want_to_overwrite_it)+" '"+zip_folder_name+"'");
		}
		yes_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{

				/*
				if(archiveReplaceDialogListener!=null)
				{
					archiveReplaceDialogListener.onYes();
				}

				 */
				((AppCompatActivity)context).getSupportFragmentManager().setFragmentResult(request_code,bdl);
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

	
	/*
	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();

	}

	 */

	/*
	public void setArchiveReplaceDialogListener(ArchiveReplaceDialogListener listener)
	{
		archiveReplaceDialogListener=listener;
	}
	
	interface ArchiveReplaceDialogListener
	{
		
		void onYes();
	}

	 */
	
}
