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
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.ArrayList;

public class FileReplaceConfirmationDialog extends DialogFragment
{
	private CheckBox apply_all_checkbox;
	private Context context;
	private FrameLayout progress_bar;
	private boolean cut;
	private String source_folder;
	private String dest_folder;
	private FileObjectType sourceFileObjectType,destFileObjectType;
	FileDuplicationViewModel fileDuplicationViewModel;
	ArrayList<String> files_selected_array;


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
		setCancelable(false);

		Bundle bundle = getArguments();
		if(bundle !=null)
		{
			files_selected_array= bundle.getStringArrayList("files_selected_array");
			cut= bundle.getBoolean("cut");
			source_folder= bundle.getString("source_folder");
			dest_folder= bundle.getString("dest_folder");
			sourceFileObjectType=(FileObjectType) bundle.getSerializable("sourceFileObjectType");
			destFileObjectType=(FileObjectType) bundle.getSerializable("destFileObjectType");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method

		View v=inflater.inflate(R.layout.fragment_replace_confirmation,container,false);
        TextView confirmation_message_textview = v.findViewById(R.id.dialog_fragment_replace_message);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_replace_confirmation_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button yes_button = buttons_layout.findViewById(R.id.first_button);
		yes_button.setText(R.string.yes);
        Button no_button = buttons_layout.findViewById(R.id.second_button);
		no_button.setText(R.string.no);
		apply_all_checkbox=v.findViewById(R.id.dialog_fragment_applyall_confirmationCheckBox);
		progress_bar=v.findViewById(R.id.file_replacement_dialog_progressbar);
		
		yes_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if(progress_bar.getVisibility()==View.VISIBLE)
				{
					Global.print(context,getString(R.string.please_wait));
					return;
				}
				if(apply_all_checkbox.isChecked())
				{
					fileDuplicationViewModel.files_selected_array.removeAll(fileDuplicationViewModel.not_to_be_replaced_files_path_array);
					fileDuplicationViewModel.overwritten_file_path_list.addAll(fileDuplicationViewModel.destination_duplicate_file_path_array);
					PasteSetUpDialog pasteSetUpDialog = PasteSetUpDialog.getInstance(source_folder,sourceFileObjectType,dest_folder,destFileObjectType,
							fileDuplicationViewModel.files_selected_array, fileDuplicationViewModel.overwritten_file_path_list,cut);
					pasteSetUpDialog.show(((AppCompatActivity)context).getSupportFragmentManager(), "paste_dialog");
					dismissAllowingStateLoss();
				}
				else
				{
					fileDuplicationViewModel.source_duplicate_file_path_array.remove(0);
					fileDuplicationViewModel.overwritten_file_path_list.add(fileDuplicationViewModel.destination_duplicate_file_path_array.remove(0));
					if(fileDuplicationViewModel.source_duplicate_file_path_array.size()==0)
					{
						fileDuplicationViewModel.files_selected_array.removeAll(fileDuplicationViewModel.not_to_be_replaced_files_path_array);
						PasteSetUpDialog pasteSetUpDialog = PasteSetUpDialog.getInstance(source_folder,sourceFileObjectType,dest_folder,destFileObjectType,
								fileDuplicationViewModel.files_selected_array, fileDuplicationViewModel.overwritten_file_path_list,cut);
						pasteSetUpDialog.show(((AppCompatActivity)context).getSupportFragmentManager(), "paste_dialog");
						dismissAllowingStateLoss();
					}
					else
					{
						confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it)+" '"+new File(fileDuplicationViewModel.source_duplicate_file_path_array.get(0)).getName()+"'");
					}
				}

			}
			
		});
		
		no_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{

				if(apply_all_checkbox.isChecked())
				{
					fileDuplicationViewModel.files_selected_array.removeAll(fileDuplicationViewModel.source_duplicate_file_path_array);
					PasteSetUpDialog pasteSetUpDialog = PasteSetUpDialog.getInstance(source_folder,sourceFileObjectType,dest_folder,destFileObjectType,
							fileDuplicationViewModel.files_selected_array, fileDuplicationViewModel.overwritten_file_path_list,cut);
					pasteSetUpDialog.show(((AppCompatActivity)context).getSupportFragmentManager(), "paste_dialog");
					dismissAllowingStateLoss();
				}
				else
				{
					fileDuplicationViewModel.not_to_be_replaced_files_path_array.add(fileDuplicationViewModel.source_duplicate_file_path_array.remove(0));
					fileDuplicationViewModel.files_selected_array.removeAll(fileDuplicationViewModel.not_to_be_replaced_files_path_array);
					fileDuplicationViewModel.destination_duplicate_file_path_array.remove(0);
					if(fileDuplicationViewModel.source_duplicate_file_path_array.size()==0)
					{
						PasteSetUpDialog pasteSetUpDialog = PasteSetUpDialog.getInstance(source_folder,sourceFileObjectType,dest_folder,destFileObjectType,
								fileDuplicationViewModel.files_selected_array, fileDuplicationViewModel.overwritten_file_path_list,cut);
						pasteSetUpDialog.show(((AppCompatActivity)context).getSupportFragmentManager(), "paste_dialog");
						dismissAllowingStateLoss();
					}
					else
					{
						confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it)+" '"+new File(fileDuplicationViewModel.source_duplicate_file_path_array.get(0)).getName()+"'");
					}
				}

			}
			
		});

		fileDuplicationViewModel=new ViewModelProvider(this).get(FileDuplicationViewModel.class);
		fileDuplicationViewModel.checkForExistingFileWithSameName(source_folder,sourceFileObjectType,dest_folder,destFileObjectType,files_selected_array,cut,true);
		fileDuplicationViewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
			@Override
			public void onChanged(AsyncTaskStatus asyncTaskStatus) {
				if(asyncTaskStatus==AsyncTaskStatus.STARTED)
				{
					progress_bar.setVisibility(View.VISIBLE);
				}
				else if (asyncTaskStatus==AsyncTaskStatus.COMPLETED)
				{
					progress_bar.setVisibility(View.GONE);
				}
				if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
				{
					if(fileDuplicationViewModel.source_duplicate_file_path_array.size()>0)
					{
						confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it)+" '"+new File(fileDuplicationViewModel.source_duplicate_file_path_array.get(0)).getName()+"'");
					}
				}

			}
		});

		return v;
	}


	public static FileReplaceConfirmationDialog getInstance(String source_folder, FileObjectType sourceFileObjectType,String dest_folder,FileObjectType destFileObjectType,
															ArrayList<String> files_selected_array,boolean cut_selected)
	{
		FileReplaceConfirmationDialog fileReplaceConfirmationDialog=new FileReplaceConfirmationDialog();
		Bundle bundle=new Bundle();
		bundle.putString("source_folder", source_folder);
		bundle.putStringArrayList("files_selected_array", files_selected_array);
		bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);
		bundle.putSerializable("destFileObjectType", destFileObjectType);
		bundle.putString("dest_folder", dest_folder);
		bundle.putBoolean("cut", cut_selected);
		fileReplaceConfirmationDialog.setArguments(bundle);
		return fileReplaceConfirmationDialog;
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
