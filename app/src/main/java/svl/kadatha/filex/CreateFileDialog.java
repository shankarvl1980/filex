package svl.kadatha.filex;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import me.jahnen.libaums.core.fs.UsbFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	private final List<String> dest_file_names=new ArrayList<>();
	private List<FilePOJO> destFilePOJOs;


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
		//this.setRetainInstance(true);
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			file_type=bundle.getInt("file_type");
			parent_folder=bundle.getString("parent_folder");
			fileObjectType=(FileObjectType)bundle.getSerializable("fileObjectType");
		}


		destFilePOJOs=Global.HASHMAP_FILE_POJO.get(fileObjectType+parent_folder);

/*
		if(destFilePOJOs==null) //first start of app after installation, hashmap size is zero
		{
			if(fileObjectType==FileObjectType.FILE_TYPE)
			{
				String [] file_names_array;
				if((file_names_array=new File(parent_folder).list())!=null)
				{
					dest_file_names.addAll(Arrays.asList(file_names_array));
				}

			}
		}
		else
		{
			for(FilePOJO filePOJO:destFilePOJOs)
			{
				dest_file_names.add(filePOJO.getName());
			}
		}

 */

		other_file_permission=Global.GET_OTHER_FILE_PERMISSION(parent_folder);

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
		okbutton= buttons_layout.findViewById(R.id.first_button);
		okbutton.setText(R.string.ok);
        Button cancelbutton = buttons_layout.findViewById(R.id.second_button);
		cancelbutton.setText(R.string.cancel);
		df=(DetailFragment)((AppCompatActivity)context).getSupportFragmentManager().findFragmentById(R.id.detail_fragment);

		if(file_type==0)
		{
			dialog_heading_textview.setText(R.string.enter_file_name);
			file_label_textview.setText(R.string.file_name_colon);
		}
		else
		{
			dialog_heading_textview.setText(R.string.enter_folder_name);
			file_label_textview.setText(R.string.folder_name_colon);
		}

		ViewModelCreateRename viewModel=new ViewModelProvider.AndroidViewModelFactory(this.getActivity().getApplication()).create(ViewModelCreateRename.class);
		MutableLiveData<FilePOJO> createdFilePOJO=viewModel.createdFilePOJO;
		createdFilePOJO.observe(CreateFileDialog.this, new Observer<FilePOJO>() {
			final String new_name=new_file_name_edittext.getText().toString().trim();
			@Override
			public void onChanged(FilePOJO filePOJO) {
				if(filePOJO!=null)
				{
					Collections.sort(df.filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));
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
		});



		okbutton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				boolean file_created=false;
				String new_name=new_file_name_edittext.getText().toString().trim();
				if(new_name.equals(""))
				{
					Global.print(context,getString(R.string.enter_file_name));
					return;
				}
				if(CheckStringForSpecialCharacters.whetherStringContains(new_name))
				{
					Global.print(context,getString(R.string.avoid_name_involving_special_characters));
					return;
				}

				String new_file_path =(parent_folder.endsWith(File.separator)) ? parent_folder+new_name : parent_folder+File.separator+new_name;
				File file=new File(new_file_path);

				boolean isWritable=FileUtil.isWritable(fileObjectType,new_file_path);
				if(!check_name_availability(new_file_path, isWritable,fileObjectType))
				{
					imm.hideSoftInputFromWindow(new_file_name_edittext.getWindowToken(),0);
					return;
				}
				//new FileCreateAsyncTask(file,isWritable).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
		return v;
	}

	/*
	private class FileCreateAsyncTask extends AsyncTask<Void,Void,Boolean>
	{
		final File file;
		final boolean isWritable;
		boolean file_created;
		final String new_file_path;
		final String new_name;
		FilePOJO filePOJO;
		FileCreateAsyncTask(File file,boolean isWritable)
		{
			this.file=file;
			this.isWritable=isWritable;
			new_file_path=file.getAbsolutePath();
			new_name=file.getName();
		}
		@Override
		protected Boolean doInBackground(Void... voids) {

			if(file_type==0)
			{
				if(isWritable)
				{
					file_created=FileUtil.createNativeNewFile(file);
				}
				else if(fileObjectType== FileObjectType.FILE_TYPE)
				{
					file_created=FileUtil.createSAFNewFile(context,parent_folder,new_name,tree_uri,tree_uri_path);
				}
				else if(fileObjectType== FileObjectType.USB_TYPE)
				{
					UsbFile parentUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,parent_folder);
					file_created=FileUtil.createUsbFile(parentUsbFile,new_name);

				}
				else if(fileObjectType==FileObjectType.ROOT_TYPE)
				{
					//file_created=RootUtils.EXECUTE(Arrays.asList(">",new_file_path));
					if(Global.SET_OTHER_FILE_PERMISSION("rwx",parent_folder))
					{
						file_created=FileUtil.createNativeNewFile(file);
					}
				}
				else if(fileObjectType==FileObjectType.FTP_TYPE)
				{
					if(Global.CHECK_FTP_SERVER_CONNECTED())
					{
						InputStream bin = new ByteArrayInputStream(new byte[0]);
						try {
							file_created=MainActivity.FTP_CLIENT.storeFile(new_file_path, bin);
						} catch (IOException e) {
							file_created=false;
						}
					}
					else
					{
						file_created=false;
					}

				}
			}
			else if(file_type==1)
			{
				if(isWritable)
				{
					file_created=FileUtil.mkdirNative(file);
				}
				else if(fileObjectType== FileObjectType.FILE_TYPE)
				{
					file_created=FileUtil.mkdirSAF(context,parent_folder,new_name,tree_uri,tree_uri_path);
				}
				else if(fileObjectType== FileObjectType.USB_TYPE)
				{
					UsbFile parentUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,parent_folder);
					file_created=FileUtil.mkdirUsb(parentUsbFile,new_name);
				}
				else if(fileObjectType==FileObjectType.ROOT_TYPE)
				{
					//file_created=RootUtils.EXECUTE(Arrays.asList("mkdir","-p",new_file_path));
					if(Global.SET_OTHER_FILE_PERMISSION("rwx",parent_folder))
					{
						file_created=FileUtil.mkdirNative(file);

					}

				}
				else if(fileObjectType==FileObjectType.FTP_TYPE)
				{
					if(Global.CHECK_FTP_SERVER_CONNECTED())
					{
						try {
							file_created=MainActivity.FTP_CLIENT.makeDirectory(new_file_path);
						} catch (IOException e) {
						}
					}
					else
					{
						file_created=false;
					}

				}

			}
			if(file_created)
			{
				filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(parent_folder, Collections.singletonList(new_name),fileObjectType,null);
				Collections.sort(df.filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));

			}

			return null;
		}

		@Override
		protected void onPostExecute(Boolean aBoolean) {
			super.onPostExecute(aBoolean);
			if(file_created)
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

	 */

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

	private boolean check_name_availability(String new_file_path, boolean isWritable, FileObjectType fileObjectType)
	{
		File new_file=new File(new_file_path);
		String new_file_name=new_file.getName();
		if(destFilePOJOs==null) //first start of app after installation, hashmap size is zero
		{
			if(fileObjectType==FileObjectType.FILE_TYPE)
			{
				String [] file_names_array;
				if((file_names_array=new File(parent_folder).list())!=null)
				{
					for(String name:file_names_array)
					{
						if(name.equals(new_file_name))
						{
							Global.print(context,getString(R.string.new_file_can_not_be_created_a_file_with_the_specified_name_exists));
							return false;
						}
					}
				}

			}
		}
		else
		{
			for(FilePOJO filePOJO:destFilePOJOs)
			{
				if(filePOJO.getName().equals(new_file_name))
				{
					Global.print(context,getString(R.string.new_file_can_not_be_created_a_file_with_the_specified_name_exists));
					return false;
				}

			}
		}

		/*
		if(dest_file_names.contains(new_file.getName()))
		{
			print(getString(R.string.new_file_can_not_be_created_a_file_with_the_specified_name_exists));
			return false;
		}

		 */
		if(fileObjectType== FileObjectType.FILE_TYPE)
		{
			if(isWritable)
			{
				return true;
			}
			else
			{
				return check_SAF_permission(new_file_path,fileObjectType);
			}

		}
		else if(fileObjectType== FileObjectType.USB_TYPE)
		{

		}
		else if(fileObjectType==FileObjectType.ROOT_TYPE)
		{
			Global.print(context,getString(R.string.root_access_not_avaialable));
			return false;
		}
		else if(fileObjectType==FileObjectType.FTP_TYPE)
		{
			return true;
		}
		else
		{
			return check_SAF_permission(new_file_path,fileObjectType);

		}
		Global.print(context,getString(R.string.could_not_create));
		return false;
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

	private boolean check_SAF_permission(String new_file_path,FileObjectType fileObjectType)
	{
		UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(new_file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();

		}

		if(tree_uri_path.equals(""))
		{
			SAFPermissionHelperDialog safpermissionhelper=new SAFPermissionHelperDialog();
			safpermissionhelper.set_safpermissionhelperlistener(new SAFPermissionHelperDialog.SafPermissionHelperListener()
			{
				public void onOKBtnClicked()
				{
					seekSAFPermission();
				}

				public void onCancelBtnClicked()
				{
				}
			});
			safpermissionhelper.show(((AppCompatActivity)context).getSupportFragmentManager(),"saf_permission_dialog");
			//saf_permission_requested=true;

			return false;
		}
		else
		{
			return true;
		}
	}

	public void seekSAFPermission()
	{
		((MainActivity)context).clear_cache=false;
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		activityResultLauncher.launch(intent);
	}

	private final ActivityResultLauncher<Intent> activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
	@Override
	public void onActivityResult(ActivityResult result) {
		if (result.getResultCode()== Activity.RESULT_OK)
		{
			Uri treeUri;
			treeUri = result.getData().getData();
			Global.ON_REQUEST_URI_PERMISSION(context,treeUri);

			//saf_permission_requested=false;
			okbutton.callOnClick();
		}
		else
		{
			Global.print(context,getString(R.string.permission_not_granted));
		}
	}
});


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
	
/*
	@Override
	public void onDestroyView() 
	{
		if (getDialog() != null && getRetainInstance()) 
		{
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

 */


}
