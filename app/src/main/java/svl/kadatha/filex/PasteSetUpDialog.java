package svl.kadatha.filex;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.util.ArrayList;

public class PasteSetUpDialog extends DialogFragment
{
	private Context context;
	private Bundle bundle;
	private boolean isWritable, isSourceFromInternal,cut;
	private final int request_code=103;
	private String tree_uri_path="",source_uri_path="",source_folder,dest_folder;
    private Uri tree_uri,source_uri;
	private final ArrayList<String> files_selected_array=new ArrayList<>();
	private FileObjectType sourceFileObjectType,destFileObjectType;
	private int size;
	private boolean saf_permission_requested;
	
	private PasteSetUpDialog(){}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		context=getContext();
		this.setRetainInstance(true);
		setCancelable(false);

		bundle=getArguments();
		if(bundle!=null)
		{
			files_selected_array.addAll(bundle.getStringArrayList("files_selected_array"));
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

	}

	public static PasteSetUpDialog getInstance(String source_folder,ArrayList<String>files_selected_array,
											   FileObjectType sourceFileObjectType,FileObjectType destFileObjectType,String dest_folder,boolean cut_selected)
	{
		PasteSetUpDialog pasteSetUpDialog=new PasteSetUpDialog();
		Global.REMOVE_RECURSIVE_PATHS(files_selected_array,dest_folder,destFileObjectType,sourceFileObjectType);
		Bundle bundle=new Bundle();
		bundle.putString("source_folder", source_folder);
		bundle.putStringArrayList("files_selected_array", files_selected_array);
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
		context=getContext();
		if(check_permission_for_source(source_folder,sourceFileObjectType))
		{
			if(check_permission_for_destination(dest_folder,destFileObjectType))
			{
				initiate_startActivity();
			}
		}

		if(!saf_permission_requested)
		{
			dismissAllowingStateLoss();
		}
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	
	public void seekSAFPermission()
	{
		((MainActivity)context).clear_cache=false;
		saf_permission_requested=true;
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		startActivityForResult(intent, request_code);
	}

	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) 
	{
		saf_permission_requested=false;
		if (requestCode == this.request_code && resultCode== Activity.RESULT_OK)
		{
			Uri treeUri;
			treeUri = resultData.getData();
			Global.ON_REQUEST_URI_PERMISSION(context,treeUri);


			if(check_permission_for_destination(dest_folder,destFileObjectType) && check_permission_for_source(source_folder,sourceFileObjectType))
			{
				initiate_startActivity();
			}

		}
		else
		{
			print(getString(R.string.permission_not_granted));
			dismissAllowingStateLoss();
		}

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
		else if(fileObjectType ==FileObjectType.ROOT_TYPE)
		{
			if(!RootUtils.CAN_RUN_ROOT_COMMANDS())
			{
				print(getString(R.string.root_access_not_avaialable));
				return false;
				//dismissAllowingStateLoss();
			}
			else
			{
				return true;
			}

		}
		return false;
	}

	private boolean check_SAF_permission_destination(String parent_file_path, FileObjectType fileObjectType)
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
					dismissAllowingStateLoss(); // should be dismissed as this fragment has no view
				}
			});
			safpermissionhelper.show(((AppCompatActivity)context).getSupportFragmentManager(),"saf_permission_dialog");
			saf_permission_requested=true;
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
			SAFPermissionHelperDialog safpermissionhelper=new SAFPermissionHelperDialog();
			safpermissionhelper.set_safpermissionhelperlistener(new SAFPermissionHelperDialog.SafPermissionHelperListener()
			{
				public void onOKBtnClicked()
				{
					seekSAFPermission();
				}

				public void onCancelBtnClicked()
				{
					dismissAllowingStateLoss(); //should be dismissed as this fragment has no view
				}
			});
			safpermissionhelper.show(((AppCompatActivity)context).getSupportFragmentManager(),"saf_permission_dialog");
			saf_permission_requested=true;
			return false;
		}
		else
		{
			return true;
		}
	}



	private void initiate_startActivity()
	{
		if(!whether_file_already_exists(dest_folder,destFileObjectType))
		{
			print(getString(R.string.directory_not_exist_not_valid));
			dismissAllowingStateLoss();
			return;
		}
		Class emptyService=ArchiveDeletePasteServiceUtil.getEmptyService(context);
		if(emptyService==null)
		{
			print(getString(R.string.maximum_3_services_processed));
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



	private void print(String msg)
	{
		android.widget.Toast.makeText(context,msg,android.widget.Toast.LENGTH_SHORT).show();
	}
	
}
