package svl.kadatha.filex;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.Collections;

public class CreateFileDialog extends DialogFragment
{
    private EditText new_file_name_edittext;
	private Button okbutton;
    private DetailFragment df;
	private int file_type;
	private Context context;
	private InputMethodManager imm;
	private String tree_uri_path="";
	private Uri tree_uri;
	private String parent_folder;
	private FileObjectType fileObjectType;
	private String other_file_permission;
	private Handler handler;
	private final static String SAF_PERMISSION_REQUEST_CODE="create_file_saf_permission_request_code";
	private FrameLayout progress_bar;

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
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			file_type=bundle.getInt("file_type");
			parent_folder=bundle.getString("parent_folder");
			fileObjectType=(FileObjectType)bundle.getSerializable("fileObjectType");
		}

		other_file_permission=Global.GET_OTHER_FILE_PERMISSION(parent_folder);
		handler=new Handler(Looper.getMainLooper());

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
		progress_bar=v.findViewById(R.id.fragment_create_rename_delete_progressbar);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_create_rename_delete_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
		okbutton= buttons_layout.findViewById(R.id.first_button);
		okbutton.setText(R.string.ok);
        Button cancelbutton = buttons_layout.findViewById(R.id.second_button);
		cancelbutton.setText(R.string.cancel);
		df=(DetailFragment)getParentFragmentManager().findFragmentById(R.id.detail_fragment);

		if(file_type==0)
		{
			dialog_heading_textview.setText(R.string.enter_file_name);
			file_label_textview.setText(R.string.file_name);
		}
		else
		{
			dialog_heading_textview.setText(R.string.enter_folder_name);
			file_label_textview.setText(R.string.folder_name);
		}


		ViewModelCreateRename viewModel=new ViewModelProvider(this).get(ViewModelCreateRename.class);
		RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
		viewModel.destFilePOJOs=repositoryClass.hashmap_file_pojo.get(fileObjectType+parent_folder);
		viewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
			@Override
			public void onChanged(AsyncTaskStatus asyncTaskStatus) {
				if(asyncTaskStatus==AsyncTaskStatus.STARTED)
				{
					progress_bar.setVisibility(View.VISIBLE);
				}
				else if (asyncTaskStatus==AsyncTaskStatus.COMPLETED)
				{
					progress_bar.setVisibility(View.GONE);
					final String new_name=new_file_name_edittext.getText().toString().trim();
					if(viewModel.file_created)
					{
						Collections.sort(df.filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));
						df.clearSelectionAndNotifyDataSetChanged();
						int idx=df.filePOJO_list.indexOf(viewModel.filePOJO);
						if(df.llm!=null)
						{
							df.llm.scrollToPositionWithOffset(idx,0);
						}
						else if(df.glm!=null)
						{
							df.glm.scrollToPositionWithOffset(idx,0);
						}

						Global.print(context,"'"+new_name+ "' "+getString(R.string.created));
					}
					else
					{
						Global.print(context,getString(R.string.could_not_create));
					}
					Global.SET_OTHER_FILE_PERMISSION(other_file_permission,parent_folder);
					imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
					dismissAllowingStateLoss();
				}

			}
		});


		okbutton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				String new_name=new_file_name_edittext.getText().toString().trim();
				if(new_name.equals(""))
				{
					Global.print(context,getString(R.string.enter_file_name));
					return;
				}
				if(CheckString.whetherStringContainsSpecialCharacters(new_name))
				{
					Global.print(context,getString(R.string.avoid_name_involving_special_characters));
					return;
				}
				if(!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(fileObjectType,null))
				{
					Global.print(context,getString(R.string.wait_till_completion_on_going_operation_on_usb));
					return;
				}

				if(!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_FTP(fileObjectType,null))
				{
					Global.print(context,getString(R.string.wait_till_current_service_on_ftp_finishes));
					return;
				}

				String new_file_path =Global.CONCATENATE_PARENT_CHILD_PATH(parent_folder,new_name);
				File file=new File(new_file_path);

				boolean isWritable=FileUtil.isWritable(fileObjectType,new_file_path);
				if(Global.WHETHER_FILE_ALREADY_EXISTS(fileObjectType,new_file_path,viewModel.destFilePOJOs))
				{
					Global.print(context,getString(R.string.new_file_can_not_be_created_a_file_with_the_specified_name_exists));
					return;
				}

				if(!is_file_writable(new_file_path, fileObjectType))
				{
					imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
					return;
				}

				viewModel.createFile(file,fileObjectType,isWritable,file_type,parent_folder,tree_uri_path,tree_uri);
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

		getParentFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, CreateFileDialog.this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(SAF_PERMISSION_REQUEST_CODE))
				{
					tree_uri=result.getParcelable("tree_uri");
					tree_uri_path=result.getString("tree_uri_path");
					okbutton.callOnClick();
				}

			}
		});
		return v;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		handler.removeCallbacksAndMessages(null);
	}


	public static CreateFileDialog getInstance(int file_type, String parent_folder, FileObjectType fileObjectType)
	{
		CreateFileDialog createFileDialog=new CreateFileDialog();
		Bundle bundle=new Bundle();
		bundle.putInt("file_type",file_type);
		bundle.putString("parent_folder",parent_folder);
		bundle.putSerializable("fileObjectType",fileObjectType);
		createFileDialog.setArguments(bundle);
		return createFileDialog;
	}

	private boolean is_file_writable(String file_path,FileObjectType fileObjectType)
	{
		if(fileObjectType==FileObjectType.FILE_TYPE)
		{
			boolean isWritable;
			isWritable=FileUtil.isWritable(fileObjectType,file_path);
			if(isWritable)
			{
				return true;
			}
			else
			{
				return check_SAF_permission(file_path,fileObjectType);
			}
		}
		return true;
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
	}

	private boolean check_SAF_permission(String new_file_path,FileObjectType fileObjectType)
	{
		UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(new_file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();
		}

		if(uriPOJO==null || tree_uri_path.equals(""))
		{
			SAFPermissionHelperDialog safpermissionhelper=SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE,new_file_path,fileObjectType);
			safpermissionhelper.show(getParentFragmentManager(),"saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
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
