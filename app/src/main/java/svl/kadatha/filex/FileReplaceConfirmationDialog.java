package svl.kadatha.filex;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
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
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
	ArrayList<Uri>data_list;


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
			data_list=bundle.getParcelableArrayList("data_list");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
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
					progress_bar.setVisibility(View.VISIBLE);
					fileDuplicationViewModel.filterFileSelectedArray(context,true,true,data_list);
				}
				else
				{
					fileDuplicationViewModel.source_duplicate_file_path_array.remove(0);
					fileDuplicationViewModel.overwritten_file_path_list.add(fileDuplicationViewModel.destination_duplicate_file_path_array.remove(0));
					if(fileDuplicationViewModel.source_duplicate_file_path_array.isEmpty())
					{
						progress_bar.setVisibility(View.VISIBLE);
						fileDuplicationViewModel.filterFileSelectedArray(context,true,false,data_list);
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
					progress_bar.setVisibility(View.VISIBLE);
					fileDuplicationViewModel.filterFileSelectedArray(context,false,true,data_list);
				}
				else
				{
					progress_bar.setVisibility(View.VISIBLE);
					fileDuplicationViewModel.filterFileSelectedArray(context,false,false,data_list);

				}

			}
			
		});

		fileDuplicationViewModel=new ViewModelProvider(this).get(FileDuplicationViewModel.class);
		fileDuplicationViewModel.checkForExistingFileWithSameName(source_folder,sourceFileObjectType,dest_folder,destFileObjectType,files_selected_array,cut,true,false);
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
					if(!fileDuplicationViewModel.source_duplicate_file_path_array.isEmpty())
					{
						confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it)+" '"+new File(fileDuplicationViewModel.source_duplicate_file_path_array.get(0)).getName()+"'");
					}
				}

			}
		});

		fileDuplicationViewModel.filterSelectedArrayAsyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
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
					if(getActivity() instanceof CopyToActivity){
						if(!fileDuplicationViewModel.yes && !fileDuplicationViewModel.apply_to_all){
							if(fileDuplicationViewModel.source_duplicate_file_path_array.isEmpty())
							{
								Bundle bundle=new Bundle();
								bundle.putParcelableArrayList("data_list",data_list);
								bundle.putStringArrayList("overwritten_file_path_list",fileDuplicationViewModel.overwritten_file_path_list);
								getParentFragmentManager().setFragmentResult(CopyToActivity.DUPLICATE_FILE_NAMES_REQUEST_CODE,bundle);
							}
							else
							{
								confirmation_message_textview.setText(getString(R.string.a_file_with_same_already_exists_do_you_want_to_replace_it)+" '"+new File(fileDuplicationViewModel.source_duplicate_file_path_array.get(0)).getName()+"'");
								fileDuplicationViewModel.filterSelectedArrayAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
								return;
							}
						}else{
							Bundle bundle=new Bundle();
							bundle.putParcelableArrayList("data_list",data_list);
							bundle.putStringArrayList("overwritten_file_path_list",fileDuplicationViewModel.overwritten_file_path_list);
							getParentFragmentManager().setFragmentResult(CopyToActivity.DUPLICATE_FILE_NAMES_REQUEST_CODE,bundle);
						}

					}
					else{
						PasteSetUpDialog pasteSetUpDialog = PasteSetUpDialog.getInstance(source_folder,sourceFileObjectType,dest_folder,destFileObjectType,
								fileDuplicationViewModel.files_selected_array, fileDuplicationViewModel.overwritten_file_path_list,cut);
						pasteSetUpDialog.show(getParentFragmentManager(), "paste_dialog");
					}

					dismissAllowingStateLoss();
				}

			}
		});

		return v;
	}


	public static FileReplaceConfirmationDialog getInstance(String source_folder, FileObjectType sourceFileObjectType,String dest_folder,FileObjectType destFileObjectType,
															ArrayList<String> files_selected_array,List<Uri> data_list,boolean cut_selected)
	{
		FileReplaceConfirmationDialog fileReplaceConfirmationDialog=new FileReplaceConfirmationDialog();
		Bundle bundle=new Bundle();
		bundle.putString("source_folder", source_folder);
		bundle.putStringArrayList("files_selected_array", files_selected_array);
		bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);
		bundle.putSerializable("destFileObjectType", destFileObjectType);
		bundle.putString("dest_folder", dest_folder);
		bundle.putParcelableArrayList("data_list", (ArrayList<? extends Parcelable>) data_list);
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
