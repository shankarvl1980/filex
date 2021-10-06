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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mjdev.libaums.UsbMassStorageDevice;

import java.io.File;
import java.util.LinkedList;

public class RecentDialog extends DialogFragment implements MainActivity.RecentDialogListener
{

    private Context context;
	private final LinkedList<FilePOJO> root_dir_linkedlist=new LinkedList<>();
    public static final int RECENT_SIZE=30;
    private Uri tree_uri;
	private String tree_uri_path="";
	private final int request_code=249;
	private RecentRecyclerAdapter rootdirrecycleradapter,recentRecyclerAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		root_dir_linkedlist.addAll(Global.STORAGE_DIR); ////adding all because root_dir_linkedlist is linkedlist where as Storage_Dir is array list
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		context=getContext();
		View v=inflater.inflate(R.layout.fragment_recent,container,false);
        RecyclerView root_dir_recyclerview = v.findViewById(R.id.dialog_recent_root_dir_RecyclerView);
        RecyclerView recent_recyclerview = v.findViewById(R.id.dialog_recent_RecyclerView);
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


		return v;
		
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		((MainActivity)context).recentDialogListener=this;
	}

	public void seekSAFPermission()
	{
		((MainActivity)context).clear_cache=false;
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
		}
		else
		{
			print(getString(R.string.permission_not_granted));
		}

	}

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
			SAFPermissionHelperDialog safpermissionhelper=new SAFPermissionHelperDialog(true);
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
	
	@Override
	public void onResume()
	{
		// TODO: Implement this method
		super.onResume();
		Window window=getDialog().getWindow();
		if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
		{
			window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_WIDTH);
		}
		else
		{
			window.setLayout(Global.DIALOG_WIDTH,Global.DIALOG_HEIGHT);
		}
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	}
	
	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}
	
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
			FileTypeSelectDialog fileTypeSelectFragment=new FileTypeSelectDialog();
			fileTypeSelectFragment.setFileTypeSelectListener(new FileTypeSelectDialog.FileTypeSelectListener()
				{
					public void onSelectType(String mime_type)
					{

						if(fileObjectType== FileObjectType.USB_TYPE)
						{
							if(check_availability_USB_SAF_permission(file_path,fileObjectType))
							{
								FileIntentDispatch.openUri(context,file_path,mime_type, false,false,fileObjectType,tree_uri,tree_uri_path);
							}
						}
						else if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
						{
							FileIntentDispatch.openFile(context,file_path,mime_type, false,false,fileObjectType);
						}

					}
				});
			fileTypeSelectFragment.show(((AppCompatActivity)context).getSupportFragmentManager(),"");
		}
		else
		{
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

	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}
}
