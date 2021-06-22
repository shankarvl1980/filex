package svl.kadatha.filex;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.util.Collections;

public class RenameFileDialog extends DialogFragment
{

    private EditText new_file_name_edittext;
    private DetailFragment df;
	private InputMethodManager imm;
	private Context context;
	private final int request_code=76;
	private boolean saf_permission_requested;
	private String tree_uri_path="";
	private Uri tree_uri;
    private String parent_file_path,existing_name;
	private boolean isDirectory;
	private FileObjectType fileObjectType;
	private Button okbutton;
    private boolean overwriting;
	private String filePOJOHashmapKeyPath;
	private boolean isWritable;
	private RenameFileDialog(){}
	private String other_file_permission,existing_file_path,new_file_path;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			parent_file_path=bundle.getString("parent_file_path");
			existing_name=bundle.getString("existing_name");
			isDirectory=bundle.getBoolean("isDirectory",false);
			fileObjectType=(FileObjectType)bundle.getSerializable("fileObjectType");
			filePOJOHashmapKeyPath=bundle.getString("filePOJOHashmapKeyPath");
		}
		existing_file_path=parent_file_path.endsWith(File.separator) ? parent_file_path+existing_name : parent_file_path+File.separator+existing_name;
		other_file_permission=Global.GET_OTHER_FILE_PERMISSION(existing_file_path);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		context=getContext();
		View v=inflater.inflate(R.layout.fragment_create_rename_delete,container,false);
        TextView dialog_heading_textview = v.findViewById(R.id.dialog_fragment_rename_delete_title);
        TextView dialog_message_textview = v.findViewById(R.id.dialog_fragment_rename_delete_message);
		dialog_message_textview.setVisibility(View.GONE);
		new_file_name_edittext=v.findViewById(R.id.dialog_fragment_rename_delete_newfilename);
        TextView no_of_files_textview = v.findViewById(R.id.dialog_fragment_rename_delete_no_of_files);
        TextView files_size_textview = v.findViewById(R.id.dialog_fragment_rename_delete_total_size);
		no_of_files_textview.setVisibility(View.GONE);
		files_size_textview.setVisibility(View.GONE);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_create_rename_delete_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        okbutton = buttons_layout.findViewById(R.id.first_button);
		okbutton.setText(R.string.ok);
        Button cancelbutton = v.findViewById(R.id.second_button);
		cancelbutton.setText(R.string.cancel);

		dialog_heading_textview.setText(R.string.rename);
		new_file_name_edittext.setText(existing_name);
		int l=existing_name.lastIndexOf(".");
		if(l==-1)
		{
			l=existing_name.length();
		}
		new_file_name_edittext.setSelection(0,l);
		df=(DetailFragment)MainActivity.FM.findFragmentById(R.id.detail_fragment);
		imm=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		
		okbutton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{

				final String new_name=new_file_name_edittext.getText().toString().trim();
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
					print(getString(R.string.could_not_be_renamed));
					return;
				}
				if(CheckStringForSpecialCharacters.whetherStringContains(new_name))
				{
					print(getString(R.string.avoid_name_involving_special_characters));
					return;
				}
				if(new_name.equals(""))
				{
					print(getString(R.string.enter_file_name));
				}
				new_file_path =(parent_file_path.endsWith(File.separator)) ? parent_file_path+new_name : parent_file_path+File.separator+new_name;

				overwriting= whether_file_already_exists(new_file_path, fileObjectType);
				isWritable=FileUtil.isWritable(new_file_path);

				if(overwriting)
				{
					if(fileObjectType==FileObjectType.FILE_TYPE)
					{
						if(isWritable)
						{
							RenameReplaceConfirmationDialog renameReplaceConfirmationDialog=RenameReplaceConfirmationDialog.getInstance(new_name);
							renameReplaceConfirmationDialog.setRenameReplaceDialogListener(new RenameReplaceConfirmationDialog.RenameReplaceDialogListener() {
								@Override
								public void rename_file() {

									rename_process(new_file_path,new_name);
								}
							});
							renameReplaceConfirmationDialog.show(MainActivity.FM,"");

						}
						else
						{
							if(isDirectory || new File(new_file_path).isDirectory())
							{
								print(getString(R.string.a_file_with_given_name_already_exists));
							}
							else
							{
								rename_process(new_file_path,new_name);
							}
						}
					}
					else if(fileObjectType==FileObjectType.USB_TYPE)
					{
						print(getString(R.string.a_file_with_given_name_already_exists));
					}
					else if(fileObjectType==FileObjectType.ROOT_TYPE)
					{
						RenameReplaceConfirmationDialog renameReplaceConfirmationDialog=RenameReplaceConfirmationDialog.getInstance(new_name);
						renameReplaceConfirmationDialog.setRenameReplaceDialogListener(new RenameReplaceConfirmationDialog.RenameReplaceDialogListener() {
							@Override
							public void rename_file() {

								rename_process(new_file_path,new_name);
							}
						});
						renameReplaceConfirmationDialog.show(MainActivity.FM,"");
					}
				}
				else
				{
					rename_process(new_file_path,new_name);
				}

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

	public static RenameFileDialog getInstance(String parent_file_path,String existing_name,boolean isDirectory,FileObjectType fileObjectType,String filePOJOHashmapKeyPath)
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

	public void rename_process(String new_file_path, String new_name)
	{
		boolean fileNameChanged = false;
		File existing_file=new File(parent_file_path,existing_name);
		File new_file=new File(new_file_path);
		if(fileObjectType==FileObjectType.FILE_TYPE)
		{
			if(isWritable)
			{
				fileNameChanged=FileUtil.renameNativeFile(existing_file,new_file);
				onRenameResult(fileNameChanged,new_name);
			}
			else
			{
				if(check_SAF_permission(parent_file_path,fileObjectType))
				{
					if(whether_file_already_exists(new_file_path,fileObjectType)) //to overwrite file name
					{
						boolean isDir=new File(new_file_path).isDirectory();
						if(!isDir && !isDirectory)
						{
							new RenameAsyncTask(context,parent_file_path,existing_name,new_file_path,new_name).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						}
						else
						{
							onRenameResult(false,new_name);
						}
					}
					else
					{
						fileNameChanged=FileUtil.renameSAFFile(context,parent_file_path+File.separator+existing_name,new_name,tree_uri,tree_uri_path);
						onRenameResult(fileNameChanged,new_name);
					}
				}

			}
		}
		else if(fileObjectType== FileObjectType.USB_TYPE)
		{
			if(whether_file_already_exists(new_file_path,fileObjectType))
			{
				print(getString(R.string.a_file_with_given_name_already_exists));
				onRenameResult(false,new_name);
			}
			else
			{
				UsbFile existingUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,existing_file.getAbsolutePath());
				fileNameChanged=FileUtil.renameUsbFile(existingUsbFile,new_name);
				onRenameResult(fileNameChanged,new_name);
			}
		}
		else if (fileObjectType==FileObjectType.ROOT_TYPE)
		{
			if(RootUtils.CAN_RUN_ROOT_COMMANDS())
			{
				//fileNameChanged=RootUtils.EXECUTE(Arrays.asList("mv",existing_file.getAbsolutePath(),new_file_path));
				if(Global.SET_OTHER_FILE_PERMISSION("rwx",existing_file_path))
				{
					fileNameChanged=FileUtil.renameNativeFile(existing_file,new_file);
				}


			}
			else
			{
				print(getString(R.string.root_access_not_avaialable));
				fileNameChanged=false;
			}
			onRenameResult(fileNameChanged,new_name);
		}

	}

	private void onRenameResult(boolean fileNameChanged, final String new_name)
	{
		if(fileNameChanged)
		{
			//use filePOJOHashmapKeyPath to remove from Search Library also
			final FilePOJO filePOJO;

			//String new_file_path=parent_file_path.endsWith(File.separator) ? parent_file_path+new_name : parent_file_path+File.separator+new_name;
			if(df.fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
			{
				filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO_ON_ADD_SEARCH_LIBRARY(filePOJOHashmapKeyPath,Collections.singletonList(new_file_path),fileObjectType, Collections.singletonList(existing_file_path));
			}
			else
			{
				if(overwriting)
				{
					FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(filePOJOHashmapKeyPath, Collections.singletonList(new_name),fileObjectType);
				}

				filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(filePOJOHashmapKeyPath, Collections.singletonList(new_name),fileObjectType, Collections.singletonList(existing_file_path));
			}
			Collections.sort(df.filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {

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


				}
			},500);
			print(getString(R.string.renamed)+" '"+existing_name+"' "+getString(R.string.at)+" '"+new_name+"'");
		}
		else
		{
			print(getString(R.string.could_not_be_renamed));
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
			safpermissionhelper.show(MainActivity.FM,"saf_permission_dialog");
			saf_permission_requested=true;
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
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
	}
	
	public void seekSAFPermission()
	{
		((MainActivity)context).clear_cache=false;
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		startActivityForResult(intent, request_code);
	}

	// @TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) 
	{
		if (requestCode == this.request_code && resultCode== Activity.RESULT_OK)
		{
			Uri treeUri;
			// Get Uri from Storage Access Framework.
			treeUri = resultData.getData();
			Global.ON_REQUEST_URI_PERMISSION(context,treeUri);

			saf_permission_requested=false;
			okbutton.callOnClick();
		}
		else
		{
			print(getString(R.string.permission_not_granted));
		}
	}

	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
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


	private class RenameAsyncTask extends svl.kadatha.filex.AsyncTask<Void,Void,Boolean>
	{
		final Context context;
		final String parent_file_path;
        final String existing_name;
        final String new_file_path;
        final String new_name;
		ProgressBarFragment pbf;
		boolean fileNameChanged;
		RenameAsyncTask(Context context,String parent_file_path,String existing_name,String new_file_path,String new_name)
		{
			this.context=context;
			this.parent_file_path=parent_file_path;
			this.existing_name=existing_name;
			this.new_file_path=new_file_path;
			this.new_name=new_name;
		}
		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			pbf=ProgressBarFragment.getInstance();
			pbf.show(MainActivity.FM,"progressbar_dialog");

		}

		@Override
		protected Boolean doInBackground(Void[] p1)
		{
			// TODO: Implement this method
			if(FileUtil.deleteSAFDirectory(context,new_file_path,tree_uri,tree_uri_path))
			{
				fileNameChanged=FileUtil.renameSAFFile(context,parent_file_path+File.separator+existing_name,new_name,tree_uri,tree_uri_path);
			}
			return fileNameChanged;
		}


		@Override
		protected void onProgressUpdate(Void[] values)
		{
			// TODO: Implement this method
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			pbf.dismissAllowingStateLoss();
			onRenameResult(result,new_name);
		}
	}

	private boolean whether_file_already_exists(String new_file_path,FileObjectType fileObjectType)
	{
		if(fileObjectType== FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
		{
			File new_file=new File(new_file_path);
			return new_file.exists();

		}
		else if(fileObjectType== FileObjectType.USB_TYPE)
		{
			UsbFile usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,new_file_path);
			return usbFile != null;

		}
		else if(fileObjectType==FileObjectType.ROOT_TYPE)
		{
			return RootUtils.WHETHER_FILE_EXISTS(new_file_path);
		}
		else
		{
			if(check_SAF_permission(new_file_path,fileObjectType))
			{
				return FileUtil.exists(context, new_file_path, tree_uri, tree_uri_path);
			}
			else
			{
				return false;
			}
		}

	}


	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}
}
