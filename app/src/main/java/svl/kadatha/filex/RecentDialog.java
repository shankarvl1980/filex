package svl.kadatha.filex;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.LinkedList;

import me.jahnen.libaums.core.UsbMassStorageDevice;

public class RecentDialog extends DialogFragment implements MainActivity.RecentDialogListener
{

    private Context context;
	private final LinkedList<FilePOJO> root_dir_linkedlist=new LinkedList<>();
    public static final int RECENT_SIZE=30;
    private Uri tree_uri;
	private String tree_uri_path="";
	private final int request_code=249;
	private RecentRecyclerAdapter rootdirrecycleradapter,recentRecyclerAdapter;
	private RecyclerView recent_recyclerview;
	private TextView recent_label;
	private FilePOJO clicked_filepojo;
	private static final String FILE_TYPE_REQUEST_CODE="recent_file_type_request_code";
	private FragmentManager fragmentManager;
	private final static String SAF_PERMISSION_REQUEST_CODE="recent_dialog_saf_permission_request_code";

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		((MainActivity)context).recentDialogListener=this;
		fragmentManager=((MainActivity)context).getSupportFragmentManager();

	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		//setRetainInstance(true);
		setCancelable(false);
		root_dir_linkedlist.addAll(Global.STORAGE_DIR); ////adding all because root_dir_linkedlist is linkedlist where as Storage_Dir is array list
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v=inflater.inflate(R.layout.fragment_recent,container,false);
		RecyclerView root_dir_recyclerview = v.findViewById(R.id.dialog_recent_root_dir_RecyclerView);
        recent_recyclerview = v.findViewById(R.id.dialog_recent_RecyclerView);
        recent_label=v.findViewById(R.id.recent_label);
		ViewGroup buttons_layout = v.findViewById(R.id.fragment_recent_button_layout);
		buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.DIALOG_WIDTH,Global.DIALOG_WIDTH));
        Button recent_clear_button = buttons_layout.findViewById(R.id.first_button);
		recent_clear_button.setText(R.string.clear);
        Button close_button = buttons_layout.findViewById(R.id.second_button);
		close_button.setText(R.string.close);

        rootdirrecycleradapter = new RecentRecyclerAdapter(root_dir_linkedlist, true);
		root_dir_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
		root_dir_recyclerview.setAdapter(rootdirrecycleradapter);
		root_dir_recyclerview.setLayoutManager(new LinearLayoutManager(context));

		recentRecyclerAdapter=new RecentRecyclerAdapter(MainActivity.RECENTS,false);
		recent_recyclerview.addItemDecoration(Global.DIVIDERITEMDECORATION);
		recent_recyclerview.setAdapter(recentRecyclerAdapter);
		recent_recyclerview.setLayoutManager(new LinearLayoutManager(context));
		
		recent_clear_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				recentRecyclerAdapter.clear_recents();
				MainActivity.RECENTS=new LinkedList<>();
			}
		});
		
		close_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
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

				}

			}
		});
		return v;
		
	}


/*
	public void seekSAFPermission()
	{
		((MainActivity)context).clear_cache=false;
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		activityResultLauncher_SAF_permission.launch(intent);
	}

	private final ActivityResultLauncher<Intent>activityResultLauncher_SAF_permission=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
		@Override
		public void onActivityResult(ActivityResult result) {
			if (result.getResultCode()== Activity.RESULT_OK)
			{
				Uri treeUri;
				treeUri = result.getData().getData();
				Global.ON_REQUEST_URI_PERMISSION(context,treeUri);
			}
			else
			{
				Global.print(context,getString(R.string.permission_not_granted));
			}

		}
	});

 */

	private final ActivityResultLauncher<Intent> activityResultLauncher_unknown_package_install_permission=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
		@Override
		public void onActivityResult(ActivityResult result) {
			if (result.getResultCode()== Activity.RESULT_OK)
			{
				if(clicked_filepojo!=null)file_open_intent_despatch(clicked_filepojo.getPath(),clicked_filepojo.getFileObjectType(),clicked_filepojo.getName());
				clicked_filepojo=null;
			}
			else
			{
				Global.print(context,getString(R.string.permission_not_granted));
			}
		}
	});


	private boolean check_availability_USB_SAF_permission(String file_path, FileObjectType fileObjectType)
	{
		if(fileObjectType==FileObjectType.USB_TYPE && MainActivity.usbFileRoot==null)
		{
			return false;
		}
		UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();
		}

		if(tree_uri_path.equals(""))
		{
			SAFPermissionHelperDialog safpermissionhelper=SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE,file_path,fileObjectType);
			/*
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

			 */
			safpermissionhelper.show(((AppCompatActivity)context).getSupportFragmentManager(),"saf_permission_dialog");
			//saf_permission_requested=true;
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
		if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
		{
			if(!Global.IS_TABLET)
			{
				recent_label.setVisibility(View.GONE);
				recent_recyclerview.setAdapter(null);
			}

			window.setLayout(Global.DIALOG_WIDTH, Global.DIALOG_WIDTH);

		}
		else
		{
			window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_HEIGHT);
		}
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	}

	/*
	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

	 */
	
	private void file_open_intent_despatch(final String file_path, final FileObjectType fileObjectType, String file_name)
	{
		int idx=file_name.lastIndexOf(".");
		String file_ext="";
		if(idx!=-1)
		{
			file_ext=file_name.substring(idx+1);
		}

		if(file_ext.equals("") || !Global.CHECK_APPS_FOR_RECOGNISED_FILE_EXT(context,file_ext))
		{
			FileTypeSelectDialog fileTypeSelectFragment=FileTypeSelectDialog.getInstance(file_path,false,fileObjectType,tree_uri,tree_uri_path);
			fileTypeSelectFragment.show(fragmentManager,"");
		}

		else
		{
			if(file_ext.matches("(?i)apk"))
			{

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					if (!getActivity().getPackageManager().canRequestPackageInstalls()) {
						Intent unknown_package_install_intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
						unknown_package_install_intent.setData(Uri.parse(String.format("package:%s", Global.FILEX_PACKAGE)));
						activityResultLauncher_unknown_package_install_permission.launch(unknown_package_install_intent);
						return;
					}
				}
			}

			if(fileObjectType== FileObjectType.USB_TYPE)
			{
				if(check_availability_USB_SAF_permission(file_path,fileObjectType))
				{
					FileIntentDispatch.openUri(context,file_path,"",file_ext.matches("(?i)zip"),false,fileObjectType,tree_uri,tree_uri_path);
				}
			}
			else if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
			{
				FileIntentDispatch.openFile(context,file_path,"",file_ext.matches("(?i)zip"),false,fileObjectType);
			}

		}
	}


	@Override
	public void onMediaAttachedAndRemoved()
	{
		root_dir_linkedlist.clear();
		root_dir_linkedlist.addAll(Global.STORAGE_DIR); //adding all because root_dir_linkedlist is linkedlist where as Storage_Dir is array list
		rootdirrecycleradapter.notifyDataSetChanged();
		recentRecyclerAdapter.notifyDataSetChanged();
	}


	private class RecentRecyclerAdapter extends RecyclerView.Adapter<RecentRecyclerAdapter.ViewHolder>
	{
		LinkedList<FilePOJO> dir_linkedlist;
		final boolean storage_dir;
		PackageInfo pi;

		RecentRecyclerAdapter(LinkedList<FilePOJO> dir_linkedlist, boolean storage_dir)
		{
			this.dir_linkedlist=dir_linkedlist;
			this.storage_dir=storage_dir;
		}

		class ViewHolder extends RecyclerView.ViewHolder
		{
			final View view;
			final ImageView fileimageview;
			final ImageView overlay_fileimageview;
			final TextView textView_recent_dir;
			int pos;
			
			ViewHolder(View view)
			{
				super(view);
				this.view=view;
				fileimageview=view.findViewById(R.id.image_storage_dir);
				overlay_fileimageview=view.findViewById(R.id.overlay_image_storage_dir);
				textView_recent_dir=view.findViewById(R.id.text_storage_dir_name);

				this.view.setOnClickListener(new View.OnClickListener()
					{

						public void onClick(View p)
						{
							pos=getBindingAdapterPosition();
							final FilePOJO filePOJO=dir_linkedlist.get(pos);
							clicked_filepojo=filePOJO;
							if(filePOJO.getIsDirectory())
							{
								((MainActivity)context).createFragmentTransaction(filePOJO.getPath(),filePOJO.getFileObjectType());
							}
							else
							{
								file_open_intent_despatch(filePOJO.getPath(),filePOJO.getFileObjectType(),filePOJO.getName());
							}

							ADD_FILE_POJO_TO_RECENT(filePOJO);
							dismissAllowingStateLoss();
						}

					});
			}
		}


		@Override
		public RecentRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
		{
			// TODO: Implement this method
			View itemview=LayoutInflater.from(context).inflate(R.layout.storage_dir_recyclerview_layout,p1,false);
			return new ViewHolder(itemview);
		}

		@Override
		public void onBindViewHolder(RecentRecyclerAdapter.ViewHolder p1, int p2)
		{
			// TODO: Implement this method
			FilePOJO filePOJO = dir_linkedlist.get(p2);
			if(storage_dir)
			{
				FileObjectType fileObjectType=filePOJO.getFileObjectType();
				String space="";
				SpacePOJO spacePOJO=Global.SPACE_ARRAY.get(fileObjectType+filePOJO.getPath());
				if(spacePOJO!=null) space=" ("+spacePOJO.getUsedSpaceReadable()+"/"+spacePOJO.getTotalSpaceReadable()+")";
				if(fileObjectType== FileObjectType.FILE_TYPE)
				{
					if(Global.GET_INTERNAL_STORAGE_FILEPOJO_STORAGE_DIR().getPath().equals(filePOJO.getPath()))
					{
						p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.device_icon));
					}
					else
					{
						p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.sdcard_icon));
					}
					p1.textView_recent_dir.setText(filePOJO.getName()+space);

				}

				else if(fileObjectType== FileObjectType.USB_TYPE)
				{
					p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.sdcard_icon));
					p1.textView_recent_dir.setText(DetailFragment.USB_FILE_PREFIX+filePOJO.getName()+space);
				}
				else if(fileObjectType==FileObjectType.ROOT_TYPE)
				{
					if(filePOJO.getPath().equals(File.separator))
					{
						p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.device_icon));
						p1.textView_recent_dir.setText(R.string.root_directory);

					}
				}
				else if(fileObjectType==FileObjectType.FTP_TYPE)
				{
					p1.fileimageview.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ftp_file_icon));
					p1.textView_recent_dir.setText(DetailFragment.FTP_FILE_PREFIX+filePOJO.getName()+space);
				}
				
			}
			else
			{
				RecyclerViewLayout.setIcon(context,filePOJO,p1.fileimageview,p1.overlay_fileimageview);
				if(filePOJO.getFileObjectType()==FileObjectType.USB_TYPE)
				{
					p1.textView_recent_dir.setText(DetailFragment.USB_FILE_PREFIX+ filePOJO.getPath());
				}
				else
				{
					p1.textView_recent_dir.setText(filePOJO.getPath());
				}

			}
		}

		@Override
		public int getItemCount()
		{
			// TODO: Implement this method
			return dir_linkedlist.size();
		}
		
		public void clear_recents()
		{
			dir_linkedlist=new LinkedList<>();
			notifyDataSetChanged();
		}
	}

	public static void ADD_FILE_POJO_TO_RECENT(FilePOJO filePOJO)
	{
		if(MainActivity.RECENTS.size()!=0)
		{
			if((!MainActivity.RECENTS.getFirst().getPath().equals(filePOJO.getPath())))
			{
				if(MainActivity.RECENTS.size()>=RECENT_SIZE)
				{
					MainActivity.RECENTS.removeLast();
				}

				MainActivity.RECENTS.addFirst(filePOJO);
			}
		}
		else
		{
			MainActivity.RECENTS.addFirst(filePOJO);
		}


	}


	private void discoverDevice() {

		UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		for (UsbDevice device : usbManager.getDeviceList().values())
		{
			for (UsbMassStorageDevice massStorageDevice : UsbMassStorageDevice.getMassStorageDevices(getContext()))
			{
				if (device.equals(massStorageDevice.getUsbDevice()))
				{
					PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(
							UsbDocumentProvider.ACTION_USB_PERMISSION), 0);
					usbManager.requestPermission(device, permissionIntent);
					break;

				}
			}
		}
	}
}
