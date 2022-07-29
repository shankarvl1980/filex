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
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import me.jahnen.libaums.core.fs.UsbFile;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class DeleteFileAlertDialog extends DialogFragment
{

    private TextView no_files_textview;
    private TextView size_files_textview;

    private final ArrayList<String>files_selected_array=new ArrayList<>();
	//private FileCountSize fileCountSize;
	private int total_no_of_files;
	private String size_of_files_to_be_deleted;
	private Context context;
    private String tree_uri_path="";
	private Uri tree_uri;
	private int size=0;
	private boolean whether_native_file_exists;
	private Bundle bundle;
	private FileObjectType sourceFileObjectType;
	private Button okbutton;
    private OKButtonClickListener okButtonClickListener;
	private String other_file_permission;
	private String source_folder;
	private Handler handler;
	private final static String SAF_PERMISSION_REQUEST_CODE="delete_file_saf_permission_request_code";


	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		okButtonClickListener= (OKButtonClickListener) context;
	}


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setCancelable(false);
		//this.setRetainInstance(true);
		bundle=getArguments();

		if(bundle!=null)
		{
			files_selected_array.addAll(bundle.getStringArrayList("files_selected_array"));
			sourceFileObjectType=(FileObjectType)bundle.getSerializable("sourceFileObjectType");
			source_folder=bundle.getString("source_folder");
            boolean storage_analyser_delete = bundle.getBoolean("storage_analyser_delete");
			size=files_selected_array.size();
			//fileCountSize=new FileCountSize(files_selected_array,sourceFileObjectType);
			//fileCountSize.count();
		}
		other_file_permission=Global.GET_OTHER_FILE_PERMISSION(source_folder);
		handler=new Handler(Looper.getMainLooper());
	}

	public static DeleteFileAlertDialog getInstance(ArrayList<String> files_selected_array, FileObjectType sourceFileObjectType, String source_folder,boolean storage_analyser_delete )
	{
		DeleteFileAlertDialog deleteFileAlertDialog=new DeleteFileAlertDialog();
		Bundle bundle=new Bundle();
		bundle.putStringArrayList("files_selected_array",files_selected_array);
		bundle.putSerializable("sourceFileObjectType",sourceFileObjectType);
		bundle.putString("source_folder",source_folder);
		bundle.putBoolean("storage_analyser_delete",storage_analyser_delete);
		deleteFileAlertDialog.setArguments(bundle);
		return deleteFileAlertDialog;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
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

		ViewModelFileCount viewModel=new ViewModelProvider(this).get(ViewModelFileCount.class);
		viewModel.count(source_folder,sourceFileObjectType,files_selected_array,size,true);

		viewModel.total_no_of_files.observe(this, new Observer<Integer>() {
			@Override
			public void onChanged(Integer integer) {
				no_files_textview.setText(getString(R.string.total_files_colon)+" "+integer);
			}
		});

		viewModel.size_of_files_formatted.observe(this, new Observer<String>() {
			@Override
			public void onChanged(String s) {

				size_files_textview.setText(getString(R.string.size_colon)+" "+s);
			}
		});


		okbutton.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					/*
					if(fileCountSize!=null)
					{
						fileCountSize.cancel(true);
					}

					 */
					DeleteFileAlertDialog.this.getViewModelStore().clear();
					if(sourceFileObjectType== FileObjectType.FILE_TYPE)
					{
						String file_path=files_selected_array.get(0);
						if(!FileUtil.isWritable(sourceFileObjectType,file_path))
						{
							if (!check_SAF_permission(file_path, sourceFileObjectType)) return;

						}
						Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
						if(emptyService==null)
						{
							Global.print(context,getString(R.string.maximum_3_services_processed));
							return;
						}
						start_delete_progress_activity(emptyService);

					}
					else if(sourceFileObjectType== FileObjectType.SEARCH_LIBRARY_TYPE)
					{
						for(int i=0;i<size;++i)
						{
							String file_path=files_selected_array.get(i);
							if(FileUtil.isFromInternal(FileObjectType.FILE_TYPE,file_path))
							{
								whether_native_file_exists=true;
							}
							else
							{
								if (!check_SAF_permission(file_path, FileObjectType.FILE_TYPE)) return;
							}

						}

						Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
						if(emptyService==null)
						{
							Global.print(context,getString(R.string.maximum_3_services_processed));
							return;
						}
						start_delete_progress_activity(emptyService);
					}
					else if(sourceFileObjectType== FileObjectType.USB_TYPE)
					{
						Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
						if(emptyService==null)
						{
							Global.print(context,getString(R.string.maximum_3_services_processed));
							return;
						}
						start_delete_progress_activity(emptyService);
					}
					else if(sourceFileObjectType==FileObjectType.ROOT_TYPE)
					{
						if(RootUtils.CAN_RUN_ROOT_COMMANDS())
						{
							Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
							if(emptyService==null)
							{
								Global.print(context,getString(R.string.maximum_3_services_processed));
								return;
							}
							start_delete_progress_activity(emptyService);
						}
						else
						{
							Global.print(context,getString(R.string.root_access_not_avaialable));
							return;
						}
					}
					else if(sourceFileObjectType== FileObjectType.FTP_TYPE)
					{
						Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
						if(emptyService==null)
						{
							Global.print(context,getString(R.string.maximum_3_services_processed));
							return;
						}
						start_delete_progress_activity(emptyService);
						/*
						if(Global.CHECK_FTP_SERVER_CONNECTED())
						{

						}
						else
						{

							print(getString(R.string.ftp_server_is_not_connected));
							return;
						}

						 */

					}
					if(okButtonClickListener!=null) okButtonClickListener.deleteDialogOKButtonClick();
					dismissAllowingStateLoss();
				}

			});

		cancelbutton.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					/*
					if(fileCountSize!=null)
					{
						fileCountSize.cancel(true);
					}

					 */
					DeleteFileAlertDialog.this.getViewModelStore().clear();
					dismissAllowingStateLoss();
				}
			});

		((AppCompatActivity)context).getSupportFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
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
	public void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		Window window=getDialog().getWindow();
		window.setLayout(Global.DIALOG_WIDTH,LayoutParams.WRAP_CONTENT);
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	}
	
	private void start_delete_progress_activity(Class service)
	{
		bundle.putString("source_uri_path",tree_uri_path);
		bundle.putParcelable("source_uri",tree_uri);
		Intent intent=new Intent(context,service);
		intent.setAction("delete");
		intent.putExtra("bundle",bundle);
		context.startActivity(intent);
	}
	
	/*
	public void seekSAFPermission()
	{

		AppCompatActivity appCompatActivity=(AppCompatActivity)context;
		if(appCompatActivity instanceof MainActivity)
		{
			((MainActivity)context).clear_cache=false;
		}
		else if(appCompatActivity instanceof StorageAnalyserActivity)
		{
			((StorageAnalyserActivity)context).clear_cache=false;
		}


		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		//startActivityForResult(intent, request_code);
		activityResultLauncher.launch(intent);
	}

	 */

	/*
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

	 */

/*
	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

 */

	@Override
	public void onDestroy() {
		super.onDestroy();
		//Global.SET_OTHER_FILE_PERMISSION(other_file_permission,source_folder);
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
			SAFPermissionHelperDialog safpermissionhelper = SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE,file_path,fileObjectType);
			/*
			safpermissionhelper.set_safpermissionhelperlistener(new SAFPermissionHelperDialog.SafPermissionHelperListener() {
				public void onOKBtnClicked() {
					seekSAFPermission();
				}

				public void onCancelBtnClicked() {

				}
			});

			 */
			safpermissionhelper.show(getActivity().getSupportFragmentManager(), "saf_permission_dialog");
			//saf_permission_requested=true;
			return false;
		}
		else
		{
			return true;
		}
	}

	private class FileCountSize
	{
		long total_size_of_files;
		final List<String> source_list_files;
		final boolean include_folder;
		final FileObjectType sourceFileObjectType;
		Future<?> future;
		boolean isCancelled;

		FileCountSize(ArrayList<String> source_list_files, FileObjectType fileObjectType)
		{
			this.source_list_files=source_list_files;
			this.include_folder= true;
			this.sourceFileObjectType=fileObjectType;
		}

		public void count()
		{
			Global.SET_OTHER_FILE_PERMISSION("rwx",source_folder);
			ExecutorService executorService=MyExecutorService.getExecutorService();
			future= executorService.submit(new Runnable() {
				@Override
				public void run() {
					String file_path=source_list_files.get(0);
					if(sourceFileObjectType==FileObjectType.FILE_TYPE || sourceFileObjectType==FileObjectType.ROOT_TYPE)
					{
						File[] f_array=new File[size];
						for(int i=0;i<size;++i)
						{
							File f=new File(source_list_files.get(i));
							f_array[i]=f;
						}
						populate(f_array,include_folder);

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
					else if(sourceFileObjectType==FileObjectType.FTP_TYPE)
					{
						FTPFile[] f_array=new FTPFile[size];
						for(int i=0;i<size;++i)
						{

							FTPFile f = FileUtil.getFTPFile(source_list_files.get(i));//MainActivity.FTP_CLIENT.mlistFile(source_list_files.get(i));
							f_array[i]=f;

						}
						populate(f_array,include_folder,source_folder);

					}

				}
			});
		}
		/*
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Global.SET_OTHER_FILE_PERMISSION("rwx",source_folder);
		}

		 */

		private void cancel(boolean mayInterruptRunning){
			if(future!=null)
			{
				future.cancel(mayInterruptRunning);
				isCancelled=true;
			}
		}

		private boolean isCancelled()
		{
			return isCancelled;
		}

/*
		@Override
		protected void onCancelled() {
			super.onCancelled();
			Global.SET_OTHER_FILE_PERMISSION(other_file_permission,source_folder);
		}

 */

		/*
		@Override
		protected Void doInBackground(Void[] p1)
		{
			// TODO: Implement this method

			String file_path=source_list_files.get(0);
			if(sourceFileObjectType==FileObjectType.FILE_TYPE || sourceFileObjectType==FileObjectType.ROOT_TYPE)
			{
				File[] f_array=new File[size];
				for(int i=0;i<size;++i)
				{
					File f=new File(source_list_files.get(i));
					f_array[i]=f;
				}
				populate(f_array,include_folder);

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
			else if(sourceFileObjectType==FileObjectType.FTP_TYPE)
			{
				FTPFile[] f_array=new FTPFile[size];
				for(int i=0;i<size;++i)
				{

					FTPFile f = FileUtil.getFTPFile(source_list_files.get(i));//MainActivity.FTP_CLIENT.mlistFile(source_list_files.get(i));
					f_array[i]=f;

				}
				populate(f_array,include_folder,source_folder);

			}
			return null;
		}
		*/

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

		private void populate(FTPFile[] source_list_files, boolean include_folder, String path)
		{
			int size=source_list_files.length;
			for(int i=0;i<size;++i)
			{
				FTPFile f=source_list_files[i];
				if(isCancelled())
				{
					return;
				}
				int no_of_files=0;
				long size_of_files=0L;
				if(f.isDirectory())
				{
					try {
						String name=f.getName();
						path=(path.endsWith(File.separator)) ? path+name : path+File.separator+name;
						populate(MainActivity.FTP_CLIENT.listFiles(path),include_folder,path);
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
					size_of_files+=f.getSize();
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

		public void publishProgress()
		{
			handler.post(new Runnable() {
				@Override
				public void run() {
					if(no_files_textview!=null)
					{
						no_files_textview.setText(getString(R.string.total_files_colon)+" "+total_no_of_files);
						size_files_textview.setText(getString(R.string.size_colon)+" "+size_of_files_to_be_deleted);
					}
				}
			});
		}

		/*
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

		 */

		/*
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

		 */
	}

	interface OKButtonClickListener
	{
		void deleteDialogOKButtonClick();
	}

	/*
	public void setOKButtonClickListener(OKButtonClickListener listener)
	{
		this.okButtonClickListener=listener;
	}

	 */
}
