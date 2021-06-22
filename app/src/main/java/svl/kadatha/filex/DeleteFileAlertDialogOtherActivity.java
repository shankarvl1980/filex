package svl.kadatha.filex;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeleteFileAlertDialogOtherActivity extends DialogFragment
{

    private TextView no_files_textview;
    private TextView size_files_textview;

    private ArrayList<String>files_selected_array=new ArrayList<>();
    private int total_no_of_files;
	private String size_of_files_to_be_deleted;
	private Context context;
    private DeleteFileAlertDialogListener deleteFileAlertDialogListener;
    private FileObjectType fileObjectType;
    private final int request_code=421;
	private FileCountSize fileCountSize;
	public String tree_uri_path="";
	public Uri tree_uri;
	private int size;
	private Button okbutton;

	private DeleteFileAlertDialogOtherActivity(){}

    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		this.setRetainInstance(true);
		Bundle bundle=getArguments();
		files_selected_array=bundle.getStringArrayList("files_selected_array");
		fileObjectType= (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
        fileCountSize = new FileCountSize(files_selected_array,fileObjectType);
		fileCountSize.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		size=files_selected_array.size();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		context=getContext();
		View v=inflater.inflate(R.layout.fragment_create_rename_delete,container,false);
        TextView dialog_heading_textview = v.findViewById(R.id.dialog_fragment_rename_delete_title);
        TextView dialog_message_textview = v.findViewById(R.id.dialog_fragment_rename_delete_message);
		if(files_selected_array.size()==1)
		{
			dialog_message_textview.setText(getString(R.string.are_you_sure_to_delete_the_selected_file)+" '"+new File(files_selected_array.get(0)).getName()+"'");
		}
		else
		{
			dialog_message_textview.setText(getString(R.string.are_you_sure_to_delete_the_selected_files)+" "+files_selected_array.size()+" "+getString(R.string.files));
		}

        EditText new_file_name_edittext = v.findViewById(R.id.dialog_fragment_rename_delete_newfilename);
		no_files_textview=v.findViewById(R.id.dialog_fragment_rename_delete_no_of_files);
		size_files_textview=v.findViewById(R.id.dialog_fragment_rename_delete_total_size);
        ViewGroup buttons_layout = v.findViewById(R.id.fragment_create_rename_delete_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        okbutton = buttons_layout.findViewById(R.id.first_button);
		okbutton.setText(R.string.ok);
        Button cancelbutton = buttons_layout.findViewById(R.id.second_button);
		cancelbutton.setText(R.string.cancel);
		dialog_heading_textview.setText(R.string.delete);
		new_file_name_edittext.setVisibility(View.GONE);

		okbutton.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{

					if(fileCountSize!=null)
					{
						fileCountSize.cancel(true);
					}
					if(fileObjectType== FileObjectType.FILE_TYPE)
					{
						String file_path=files_selected_array.get(0);
						if(!FileUtil.isWritable(file_path))
						{
							if (!check_SAF_permission(file_path, fileObjectType)) return;
						}

					}
					else if(fileObjectType== FileObjectType.SEARCH_LIBRARY_TYPE)
					{
						for(int i=0;i<size;++i)
						{
							String file_path=files_selected_array.get(i);
							if (!FileUtil.isFromInternal(file_path))
							{
								if (!check_SAF_permission(file_path, FileObjectType.FILE_TYPE)) return;
							}
						}

					}


					if(deleteFileAlertDialogListener!=null)
					{
						deleteFileAlertDialogListener.onSelectOK();
					}
					dismissAllowingStateLoss();
					

				}

			});

		cancelbutton.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					if(fileCountSize!=null)
					{
						fileCountSize.cancel(true);
					}
					dismissAllowingStateLoss();
					
				}

			});

		if(savedInstanceState!=null)
		{

			no_files_textview.setText(getString(R.string.total_files_colon)+" "+total_no_of_files);
			size_files_textview.setText(getString(R.string.size_colon)+" "+size_of_files_to_be_deleted);
		}
		return v;
	}

	public static DeleteFileAlertDialogOtherActivity getInstance(ArrayList<String>files_selected_array,FileObjectType fileObjectType)
	{
		DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity=new DeleteFileAlertDialogOtherActivity();
		Bundle bundle=new Bundle();
		bundle.putStringArrayList("files_selected_array",files_selected_array);
		bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE,fileObjectType);
		deleteFileAlertDialogOtherActivity.setArguments(bundle);
		return deleteFileAlertDialogOtherActivity;
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
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();

	}

	/*
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		// TODO: Implement this method
		super.onSaveInstanceState(outState);
		outState.putInt("total_no_of_files",total_no_of_files);
		outState.putString("size_of_files_format",size_of_files_to_be_deleted);
	}

	 */

	public void seekSAFPermission()
	{
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		startActivityForResult(intent, request_code);
	}


	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData)
	{
		if (requestCode == this.request_code && resultCode== Activity.RESULT_OK)
		{
			Uri treeUri;
			treeUri = resultData.getData();
			Global.ON_REQUEST_URI_PERMISSION(context,treeUri);

			//saf_permission_requested=false;
			okbutton.callOnClick();
		}
		else
		{
			print(getString(R.string.permission_not_granted));
		}

	}

	private boolean check_SAF_permission(String file_path,FileObjectType fileObjectType)
	{
		UriPOJO  uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();
		}


		if(tree_uri_path.equals("")) {
			SAFPermissionHelperDialog safpermissionhelper = new SAFPermissionHelperDialog();
			safpermissionhelper.set_safpermissionhelperlistener(new SAFPermissionHelperDialog.SafPermissionHelperListener() {
				public void onOKBtnClicked() {
					seekSAFPermission();
				}

				public void onCancelBtnClicked() {

				}
			});
			safpermissionhelper.show(getActivity().getSupportFragmentManager(), "saf_permission_dialog");
			//saf_permission_requested=true;
			return false;
		}
		else
		{
			return true;
		}
	}

	public void setDeleteFileDialogListener(DeleteFileAlertDialogListener listener)
	{
		deleteFileAlertDialogListener=listener;
	}
	
	interface DeleteFileAlertDialogListener
	{
		void onSelectOK();
	}



	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}

	private class FileCountSize extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
	{
		long total_size_of_files;
		final List<String> source_list_files;
		final boolean include_folder;
		final FileObjectType sourceFileObjectType;

		FileCountSize(ArrayList<String> source_list_files, FileObjectType fileObjectType)
		{
			this.source_list_files=source_list_files;
			this.include_folder= true;
			this.sourceFileObjectType=fileObjectType;
		}

		@Override
		protected Void doInBackground(Void[] p1)
		{
			// TODO: Implement this method

			String file_path=source_list_files.get(0);
			if(sourceFileObjectType==FileObjectType.FILE_TYPE)
			{
				if(FileUtil.isFromInternal(file_path))
				{
					File[] f_array=new File[size];
					for(int i=0;i<size;++i)
					{
						File f=new File(source_list_files.get(i));
						f_array[i]=f;
					}
					populate(f_array,include_folder);
				}
				else
				{
					File[] f_array=new File[size];
					for(int i=0;i<size;++i)
					{
						File f=new File(source_list_files.get(i));
						f_array[i]=f;
					}
					populate(f_array,include_folder);

				}
			}
			else if(sourceFileObjectType== FileObjectType.USB_TYPE)
			{
				UsbFile[] f_array=new UsbFile[size];
				for(int i=0;i<size;++i)
				{
					UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,source_list_files.get(i));
					f_array[i]=f;
				}
				populate(f_array,include_folder);
			}

			else if(sourceFileObjectType== FileObjectType.SEARCH_LIBRARY_TYPE)
			{
				File[] f_array=new File[size];
				for(int i=0;i<size;++i)
				{
					File f=new File(source_list_files.get(i));
					f_array[i]=f;
				}
				populate(f_array,include_folder);

			}
			return null;
		}

		private void populate(File[] source_list_files,boolean include_folder)
		{
			int size=source_list_files.length;
			for(int i=0;i<size;++i)
			{
				File f=source_list_files[i];
				if(isCancelled())
				{
					return;
				}
				int no_of_files=0;
				long size_of_files=0L;
				if(f.isDirectory())
				{
					if(f.list()!=null)
					{
						populate(f.listFiles(),include_folder);
					}
					if(include_folder)
					{
						no_of_files++;
					}
				}
				else
				{
					no_of_files++;
					size_of_files+=f.length();
				}
				total_no_of_files+=no_of_files;
				total_size_of_files+=size_of_files;
				size_of_files_to_be_deleted=FileUtil.humanReadableByteCount(total_size_of_files,Global.BYTE_COUNT_BLOCK_1000);
				publishProgress();
			}
		}

		private void populate(UsbFile[] source_list_files, boolean include_folder)
		{
			int size=source_list_files.length;
			for(int i=0;i<size;++i)
			{
				UsbFile f=source_list_files[i];
				if(isCancelled())
				{
					return;
				}
				int no_of_files=0;
				long size_of_files=0L;
				if(f.isDirectory())
				{
					try {
						populate(f.listFiles(),include_folder);
					} catch (IOException e) {

					}
					if(include_folder)
					{
						no_of_files++;
					}
				}
				else
				{
					no_of_files++;
					size_of_files+=f.getLength();
				}
				total_no_of_files+=no_of_files;
				total_size_of_files+=size_of_files;
				size_of_files_to_be_deleted=FileUtil.humanReadableByteCount(total_size_of_files,Global.BYTE_COUNT_BLOCK_1000);
				publishProgress();
			}
		}


		private void populate(List<String> source_list_files,boolean include_folder)
		{
			int size=source_list_files.size();
			for(int i=0;i<size;++i)
			{
				if(isCancelled())
				{
					return;
				}
				int no_of_files=0;
				long size_of_files=0L;
				String parent_file_path=source_list_files.get(i);
				Uri uri=FileUtil.getDocumentUri(parent_file_path,tree_uri,tree_uri_path);
				if(FileUtil.isDirectory(context,uri))
				{
					Uri children_uri=DocumentsContract.buildChildDocumentsUriUsingTree(tree_uri,FileUtil.getDocumentID(parent_file_path,tree_uri,tree_uri_path));
					Cursor cursor=context.getContentResolver().query(children_uri,new String[] {DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null);
					if(cursor!=null && cursor.getCount()>0)
					{
						List<String>inner_source_list_files=new ArrayList<>();
						while(cursor.moveToNext())
						{

							String docID=cursor.getString(0);
							String displayName=cursor.getString(1);
							inner_source_list_files.add(parent_file_path+File.separator+displayName);

						}
						cursor.close();
						populate(inner_source_list_files,include_folder);

					}

					if(include_folder)
					{
						no_of_files++;
					}
				}
				else
				{
					no_of_files++;
					size_of_files+=FileUtil.getSize(context,uri);
				}
				total_no_of_files+=no_of_files;
				total_size_of_files+=size_of_files;
				size_of_files_to_be_deleted=FileUtil.humanReadableByteCount(total_size_of_files,Global.BYTE_COUNT_BLOCK_1000);
				publishProgress();
			}
		}
		@Override
		protected void onProgressUpdate(Void[] values)
		{
			// TODO: Implement this method
			super.onProgressUpdate(values);
			if(no_files_textview!=null)
			{
				no_files_textview.setText(getString(R.string.total_files_colon)+" "+total_no_of_files);
				size_files_textview.setText(getString(R.string.size_colon)+" "+size_of_files_to_be_deleted);
			}
		}

		@Override
		protected void onPostExecute(Void result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
		}

		@Override
		protected void onCancelled(Void result)
		{
			// TODO: Implement this method
			super.onCancelled(result);
		}
	}

}
