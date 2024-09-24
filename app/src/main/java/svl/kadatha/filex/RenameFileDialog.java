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

public class RenameFileDialog extends DialogFragment
{
    private EditText new_file_name_edittext;
    private DetailFragment df;
	private InputMethodManager imm;
	private Context context;
	private String tree_uri_path="";
	private Uri tree_uri;
    private String parent_file_path,existing_name;
	private boolean isDirectory;
	private FileObjectType fileObjectType;
	private Button okbutton;
    private boolean overwriting;
	private String filePOJOHashmapKeyPath;
	private boolean isWritable;
	private String other_file_permission,existing_file_path,new_file_path;
	public static final String REPLACEMENT_CONFIRMATION="replacement_confirmation";
	private String new_name;
	private Handler handler;
	private final static String SAF_PERMISSION_REQUEST_CODE="rename_file_saf_permission_request_code";
	private FrameLayout progress_bar;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setCancelable(false);
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			parent_file_path=bundle.getString("parent_file_path");
			existing_name=bundle.getString("existing_name");
			isDirectory=bundle.getBoolean("isDirectory",false);
			fileObjectType=(FileObjectType)bundle.getSerializable("fileObjectType");
			filePOJOHashmapKeyPath=bundle.getString("filePOJOHashmapKeyPath");
		}
		existing_file_path=Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,existing_name);
		other_file_permission=Global.GET_OTHER_FILE_PERMISSION(existing_file_path);
		handler=new Handler(Looper.getMainLooper());

		if(savedInstanceState!=null)
		{
			new_file_path =savedInstanceState.getString("new_file_path");
			overwriting= savedInstanceState.getBoolean("overwriting");
			isWritable=savedInstanceState.getBoolean("isWritable");
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method

		View v=inflater.inflate(R.layout.fragment_create_rename_delete,container,false);
        TextView dialog_heading_textview = v.findViewById(R.id.dialog_fragment_rename_delete_title);
        TextView dialog_message_textview = v.findViewById(R.id.dialog_fragment_rename_delete_message);
		dialog_message_textview.setVisibility(View.GONE);
		new_file_name_edittext=v.findViewById(R.id.dialog_fragment_rename_delete_newfilename);
        TextView no_of_files_textview = v.findViewById(R.id.dialog_fragment_rename_delete_no_of_files);
        TextView files_size_textview = v.findViewById(R.id.dialog_fragment_rename_delete_total_size);
		no_of_files_textview.setVisibility(View.GONE);
		files_size_textview.setVisibility(View.GONE);
		progress_bar=v.findViewById(R.id.fragment_create_rename_delete_progressbar);
		ViewGroup buttons_layout = v.findViewById(R.id.fragment_create_rename_delete_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
		okbutton = buttons_layout.findViewById(R.id.first_button);
		okbutton.setText(R.string.ok);
        Button cancelbutton = buttons_layout.findViewById(R.id.second_button);
		cancelbutton.setText(R.string.cancel);

		dialog_heading_textview.setText(R.string.rename);
		new_file_name_edittext.setText(existing_name);
		int l=existing_name.lastIndexOf(".");
		if(l==-1)
		{
			l=existing_name.length();
		}
		new_file_name_edittext.setSelection(0,l);
		df=(DetailFragment)getParentFragmentManager().findFragmentById(R.id.detail_fragment);
		imm=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);


		ViewModelCreateRename viewModel=new ViewModelProvider(this).get(ViewModelCreateRename.class);
		RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
		viewModel.destFilePOJOs=repositoryClass.hashmap_file_pojo.get(fileObjectType+parent_file_path);
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
					onRenameResult(viewModel.fileNameChanged,new_name,viewModel.filePOJO);
				}
			}
		});

		getParentFragmentManager().setFragmentResultListener(REPLACEMENT_CONFIRMATION, RenameFileDialog.this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(REPLACEMENT_CONFIRMATION) && result!=null)
				{
					String new_name=result.getString("rename_file_name");
					if (fileObjectType == FileObjectType.FILE_TYPE && !isWritable)
					{
						if (check_SAF_permission(parent_file_path, fileObjectType)) {
						viewModel.renameFile(parent_file_path,existing_file_path,existing_name,new_file_path,new_name,isWritable,fileObjectType,isDirectory,overwriting,tree_uri_path,tree_uri,filePOJOHashmapKeyPath,df.fileObjectType);
					}
					} else
					{
						viewModel.renameFile(parent_file_path,existing_file_path,existing_name,new_file_path,new_name,isWritable,fileObjectType,isDirectory,overwriting,tree_uri_path,tree_uri,filePOJOHashmapKeyPath,df.fileObjectType);
					}

				}
			}
		});

		getParentFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
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

		okbutton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{

				new_name=new_file_name_edittext.getText().toString().trim();
				if(new_name.equals(existing_name))
				{
					imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
					dismissAllowingStateLoss();
					return;
				}
				if(new_name.equalsIgnoreCase(existing_name))
				{
					imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
					dismissAllowingStateLoss();
					Global.print(context,getString(R.string.could_not_be_renamed));
					return;
				}
				if(CheckString.whetherStringContainsSpecialCharacters(new_name))
				{
					Global.print(context,getString(R.string.avoid_name_involving_special_characters));
					return;
				}
				if(new_name.isEmpty())
				{
					Global.print(context,getString(R.string.enter_file_name));
					return;
				}

				if(!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(fileObjectType,null))
				{
					Global.print(context,getString(R.string.wait_till_completion_on_going_operation_on_usb));
					return;
				}

				new_file_path =Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,new_name);

				overwriting= Global.WHETHER_FILE_ALREADY_EXISTS(fileObjectType,new_file_path,viewModel.destFilePOJOs);
				isWritable=FileUtil.isWritable(fileObjectType,new_file_path);

				if(overwriting)
				{
					if(fileObjectType==FileObjectType.FILE_TYPE)
					{
						if(isWritable)
						{
							RenameReplaceConfirmationDialog renameReplaceConfirmationDialog=RenameReplaceConfirmationDialog.getInstance(new_name);
							renameReplaceConfirmationDialog.show(getParentFragmentManager(),"");
						}
						else
						{
							if(isDirectory || new File(new_file_path).isDirectory())
							{
								Global.print(context,getString(R.string.a_file_with_given_name_already_exists));
							}
							else
							{
								if(check_SAF_permission(parent_file_path,fileObjectType))
								{
									RenameReplaceConfirmationDialog renameReplaceConfirmationDialog=RenameReplaceConfirmationDialog.getInstance(new_name);
									renameReplaceConfirmationDialog.show(getParentFragmentManager(),"");
								}

							}
						}
					}
					else if(fileObjectType==FileObjectType.USB_TYPE)
					{
						Global.print(context,getString(R.string.a_file_with_given_name_already_exists));
					}
					else {
						RenameReplaceConfirmationDialog renameReplaceConfirmationDialog=RenameReplaceConfirmationDialog.getInstance(new_name);
						renameReplaceConfirmationDialog.show(getParentFragmentManager(),"");
					}
				}
				else if (fileObjectType == FileObjectType.FILE_TYPE && !isWritable)
				{
					if (check_SAF_permission(parent_file_path, fileObjectType)) {
						viewModel.renameFile(parent_file_path,existing_file_path,existing_name,new_file_path,new_name,isWritable,fileObjectType,isDirectory,overwriting,tree_uri_path,tree_uri,filePOJOHashmapKeyPath,df.fileObjectType);
					}
				} else
				{
					viewModel.renameFile(parent_file_path,existing_file_path,existing_name,new_file_path,new_name,isWritable,fileObjectType,isDirectory,overwriting,tree_uri_path,tree_uri,filePOJOHashmapKeyPath,df.fileObjectType);
				}
				imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
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
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString("new_file_path",new_file_path);
		outState.putBoolean("overwriting",overwriting);
		outState.putBoolean("isWritable",isWritable);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		handler.removeCallbacksAndMessages(null);
	}

	public static RenameFileDialog getInstance(String parent_file_path, String existing_name, boolean isDirectory, FileObjectType fileObjectType, String filePOJOHashmapKeyPath)
	{
		RenameFileDialog renameFileDialog=new RenameFileDialog();
		Bundle bundle=new Bundle();
		bundle.putString("parent_file_path",parent_file_path);
		bundle.putString("existing_name",existing_name);
		bundle.putBoolean("isDirectory",isDirectory);
		bundle.putSerializable("fileObjectType",fileObjectType);
		bundle.putString("filePOJOHashmapKeyPath",filePOJOHashmapKeyPath);
		renameFileDialog.setArguments(bundle);
		return renameFileDialog;
	}

	private void onRenameResult(boolean fileNameChanged, final String new_name, FilePOJO filePOJO)
	{
		if(fileNameChanged)
		{
			df.clearSelectionAndNotifyDataSetChanged();
			int idx=df.filePOJO_list.indexOf(filePOJO);
			if(df.llm!=null)
			{
				df.llm.scrollToPositionWithOffset(idx,0);
			}
			else if(df.glm!=null)
			{
				df.glm.scrollToPositionWithOffset(idx,0);
			}


			Global.print(context,getString(R.string.renamed)+" '"+existing_name+"' "+getString(R.string.at)+" '"+new_name+"'");
		}
		else
		{
			Global.print(context,getString(R.string.could_not_be_renamed));
		}
		Global.SET_OTHER_FILE_PERMISSION(other_file_permission,new_file_path);
		imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
		dismissAllowingStateLoss();

	}

	private boolean check_SAF_permission(String parent_file_path, FileObjectType fileObjectType)
	{
		UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(parent_file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();
		}

		if(uriPOJO==null || tree_uri_path.isEmpty())
		{
			SAFPermissionHelperDialog safpermissionhelper=SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE,parent_file_path,fileObjectType);
			safpermissionhelper.show(getParentFragmentManager(),"saf_permission_dialog");
			imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
			return false;
		}
		else
		{
			return true;
		}
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

	@Override
	public void onCancel(DialogInterface dialog)
	{
		// TODO: Implement this method
		super.onCancel(dialog);
		imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
	}

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		// TODO: Implement this method
		imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
		super.onDismiss(dialog);
	}


}
