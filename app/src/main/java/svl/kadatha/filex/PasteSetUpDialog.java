package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.ArrayList;

import me.jahnen.libaums.core.fs.UsbFile;

public class PasteSetUpDialog extends DialogFragment
{
	private Context context;
	private Bundle bundle;
	private boolean isWritable, isSourceFromInternal,cut;
	private String tree_uri_path="",source_uri_path="",source_folder,dest_folder;
    private Uri tree_uri,source_uri;
	private ArrayList<String> files_selected_array=new ArrayList<>();
	private FileObjectType sourceFileObjectType,destFileObjectType;
	private int size;
	private final static String SAF_PERMISSION_REQUEST_CODE_SOURCE="paste_set_up_saf_permission_request_code_source";
	private final static String SAF_PERMISSION_REQUEST_CODE_DEST="paste_set_up_saf_permission_request_code_dest";

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

		bundle=getArguments();
		if(bundle!=null)
		{
			files_selected_array=bundle.getStringArrayList("files_selected_array");
			cut=bundle.getBoolean("cut");
			source_folder=bundle.getString("source_folder");
			dest_folder=bundle.getString("dest_folder");
			sourceFileObjectType=(FileObjectType)bundle.getSerializable("sourceFileObjectType");
			destFileObjectType=(FileObjectType)bundle.getSerializable("destFileObjectType");
			size=files_selected_array.size();
		}
		else
		{
			dismissAllowingStateLoss();
		}
		if(files_selected_array.size()==0)dismissAllowingStateLoss();
	}

	public static PasteSetUpDialog getInstance(String source_folder,FileObjectType sourceFileObjectType,String dest_folder,FileObjectType destFileObjectType,
											   ArrayList<String>files_selected_array,ArrayList<String>overwritten_file_path_list,boolean cut_selected)
	{
		PasteSetUpDialog pasteSetUpDialog=new PasteSetUpDialog();
		Bundle bundle=new Bundle();
		bundle.putString("source_folder", source_folder);
		bundle.putStringArrayList("files_selected_array", files_selected_array);
		bundle.putStringArrayList("overwritten_file_path_list",overwritten_file_path_list);
		bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);
		bundle.putSerializable("destFileObjectType", destFileObjectType);
		bundle.putString("dest_folder", dest_folder);
		bundle.putBoolean("cut", cut_selected);
		pasteSetUpDialog.setArguments(bundle);
		return pasteSetUpDialog;
	}

	public static PasteSetUpDialog getInstance(Bundle bundle)
	{
		PasteSetUpDialog pasteSetUpDialog=new PasteSetUpDialog();
		pasteSetUpDialog.setArguments(bundle);
		return pasteSetUpDialog;
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		MainActivityViewModel viewModel=new ViewModelProvider(this).get(MainActivityViewModel.class);
		if(!viewModel.checkedSAFPermissionPasteSetUp)
		{
			if(check_permission_for_source(source_folder,sourceFileObjectType))
			{
				if(check_permission_for_destination(dest_folder,destFileObjectType))
				{
					initiate_startActivity();
				}
			}
			viewModel.setSAFCheckedBoolean();
		}

		((AppCompatActivity)context).getSupportFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE_DEST, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(SAF_PERMISSION_REQUEST_CODE_DEST))
				{
					tree_uri=result.getParcelable("tree_uri");
					tree_uri_path=result.getString("tree_uri_path");
					if(check_permission_for_destination(dest_folder,destFileObjectType) && check_permission_for_source(source_folder,sourceFileObjectType))
					{
						initiate_startActivity();
					}

				}

			}
		});

		((AppCompatActivity)context).getSupportFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE_SOURCE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(SAF_PERMISSION_REQUEST_CODE_SOURCE))
				{
					source_uri=result.getParcelable("tree_uri");
					source_uri_path=result.getString("tree_uri_path");
					if(check_permission_for_destination(dest_folder,destFileObjectType) && check_permission_for_source(source_folder,sourceFileObjectType))
					{
						initiate_startActivity();
					}

				}

			}
		});

		((AppCompatActivity)context).getSupportFragmentManager().setFragmentResultListener(SAFPermissionHelperDialog.SAF_PERMISSION_CANCEL_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(SAFPermissionHelperDialog.SAF_PERMISSION_CANCEL_REQUEST_CODE))
				{
					Global.print(context,getString(R.string.permission_not_granted));
					dismissAllowingStateLoss();
				}

			}
		});



		return super.onCreateView(inflater, container, savedInstanceState);
	}

	private boolean check_permission_for_source(String file_path,FileObjectType fileObjectType)
	{

		if(!cut)
		{
			return true;
		}

		if(fileObjectType==FileObjectType.FILE_TYPE)
		{
			isSourceFromInternal=FileUtil.isFromInternal(fileObjectType,file_path);
			if(isSourceFromInternal)
			{
				return true;
			}
			else
			{
				return check_SAF_permission_source(file_path,fileObjectType);
			}
		}
		else if(fileObjectType==FileObjectType.USB_TYPE)
		{
			return true;
		}
		else if(fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
		{
			for(int n=0;n<size;++n)
			{
				file_path=files_selected_array.get(n);
				if(!FileUtil.isFromInternal(FileObjectType.FILE_TYPE,file_path))
				{
					if(!check_SAF_permission_source(file_path,FileObjectType.FILE_TYPE)) return false;
				}
			}
			return true;
		}
		else if(fileObjectType==FileObjectType.FTP_TYPE)
		{
			return true;
			/*
			if(Global.CHECK_FTP_SERVER_CONNECTED())
			{
				return true;
			}
			else
			{
				print(getString(R.string.ftp_server_is_not_connected));
				return false;
			}

			 */
		}
		return true; //this needs to be true, after success checking of permission of in searchlibrarytype to return true

	}

	private boolean check_permission_for_destination(String file_path,FileObjectType fileObjectType)
	{
		if(fileObjectType==FileObjectType.FILE_TYPE)
		{
			isWritable=FileUtil.isWritable(fileObjectType,file_path);
			if(isWritable)
			{
				return true;
			}
			else
			{
				return check_SAF_permission_destination(file_path,fileObjectType);
			}
		}
		else if(fileObjectType == FileObjectType.USB_TYPE)
		{
			return true;
		}
		else if(fileObjectType==FileObjectType.FTP_TYPE)
		{
			return true;
//			if(Global.CHECK_FTP_SERVER_CONNECTED())
//			{
//				return true;
//			}
//			else
//			{
//				Global.print(context,getString(R.string.ftp_server_is_not_connected));
//				return false;
//			}

		}
		else if(fileObjectType ==FileObjectType.ROOT_TYPE)
		{
			if(!RootUtils.CAN_RUN_ROOT_COMMANDS())
			{
				Global.print(context,getString(R.string.root_access_not_avaialable));
				dismissAllowingStateLoss();
				return false;
			}
			else
			{
				return true;
			}

		}
		return true;
	}

	private boolean check_SAF_permission_destination(String parent_file_path, FileObjectType fileObjectType)
	{
		UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(parent_file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();
		}


		if(uriPOJO==null || tree_uri_path.equals(""))
		{
			SAFPermissionHelperDialog safpermissionhelper=SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE_DEST,parent_file_path,fileObjectType);
			safpermissionhelper.show(((AppCompatActivity)context).getSupportFragmentManager(),"saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
	}

	private boolean check_SAF_permission_source(String parent_file_path, FileObjectType fileObjectType)
	{
		UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(parent_file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			source_uri_path=uriPOJO.get_path();
			source_uri=uriPOJO.get_uri();
		}


		if(source_uri_path.equals(""))
		{
			SAFPermissionHelperDialog safpermissionhelper=SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE_SOURCE,parent_file_path,fileObjectType);
			safpermissionhelper.show(((AppCompatActivity)context).getSupportFragmentManager(),"saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
	}



	private void initiate_startActivity()
	{
		if(files_selected_array.size()==0)
		{
			Global.print(context,getString(R.string.could_not_perform_action));
			dismissAllowingStateLoss();
			return;
		}
		if(!whether_file_already_exists(dest_folder,destFileObjectType))
		{
			Global.print(context,getString(R.string.directory_not_exist_not_valid));
			dismissAllowingStateLoss();
			return;
		}
		Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
		if(emptyService==null)
		{
			Global.print(context,getString(R.string.maximum_3_services_processed));
			dismissAllowingStateLoss();
			return;
		}

		if(!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(sourceFileObjectType,destFileObjectType))
		{
			Global.print(context,getString(R.string.wait_till_completion_on_going_operation_on_usb));
			dismissAllowingStateLoss();
			return;
		}

		Intent intent=new Intent(context,emptyService);
		intent.setAction(cut ? "paste-cut" : "paste-copy");
		bundle.putString("tree_uri_path",tree_uri_path);
		bundle.putString("source_uri_path",source_uri_path);
		bundle.putParcelable("tree_uri",tree_uri);
		bundle.putParcelable("source_uri",source_uri);
		bundle.putBoolean("isWritable",isWritable);
		bundle.putBoolean("isSourceFromInternal",isSourceFromInternal);
		intent.putExtra("bundle",bundle);
		context.startActivity(intent);
		dismissAllowingStateLoss();
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
		else if(fileObjectType==FileObjectType.FTP_TYPE)
		{
			return true;
//			final boolean[] result_came = new boolean[1];
//			final FTPFile[] ftpFile = new FTPFile[1];
//
//			ExecutorService executorService=MyExecutorService.getExecutorService();
//			executorService.execute(new Runnable() {
//				@Override
//				public void run() {
//					ftpFile[0] =FileUtil.getFTPFile(new_file_path);
//					result_came[0] =true;
//				}
//			});
//
//			while(!result_came[0])
//			{
//				if(ftpFile[0]!=null)
//				{
//					return true;
//				}
//			}
		}
		else
		{
			if(check_SAF_permission_destination(new_file_path,fileObjectType))
			{
				return FileUtil.exists(context, new_file_path, tree_uri, tree_uri_path);
			}
			else
			{
				return false;
			}
		}

	}

}
