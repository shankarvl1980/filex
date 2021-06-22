package svl.kadatha.filex;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileSelectorDialog extends Fragment implements FileSelectorActivity.DetailFragmentCommunicationListener, FileModifyObserver.FileObserverListener
{
	static private final SimpleDateFormat SDF=new SimpleDateFormat("dd-MM-yyyy");
	private RecyclerView recycler_view;
    private TextView folder_empty_textview;
    private Context context;
	public FileSelectorAdapter adapter;
	public List<FilePOJO> filePOJO_list,totalFilePOJO_list;
	public int totalFilePOJO_list_Size;
	private FileSelectorActivity fileSelectorActivity;
	public String fileclickselected;
	public FileObjectType fileObjectType;
	public UsbFile currentUsbFile;
	private ProgressBarFragment pbf_polling;
	public TextView folder_selected_textview;
	private List<FilePOJO> filePOJOS=new ArrayList<>(), filePOJOS_filtered=new ArrayList<>();
	private FileModifyObserver fileModifyObserver;
	public boolean local_activity_delete,modification_observed,cleared_cache;
	private boolean filled_filePOJOs;
	private Uri tree_uri;
	private String tree_uri_path="";
	private final int request_code=5678;
	public int file_list_size;

	private FileSelectorDialog(){}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		AsyncTaskStatus asyncTaskStatus = AsyncTaskStatus.NOT_YET_STARTED;
		context=getContext();
		fileclickselected=getTag();
		if(fileclickselected==null)
		{
			fileclickselected=Global.GET_INTERNAL_STORAGE_PATH_STORAGE_DIR();
		}
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			fileObjectType=(FileObjectType)bundle.getSerializable("fileObjectType");
		}

		if(fileObjectType==FileObjectType.ROOT_TYPE)
		{
			if(fileclickselected.startsWith(Global.GET_INTERNAL_STORAGE_PATH_STORAGE_DIR()) || (Global.EXTERNAL_STORAGE_PATH!=null && !Global.EXTERNAL_STORAGE_PATH.equals("") && fileclickselected.startsWith(Global.EXTERNAL_STORAGE_PATH)))
			{
				fileObjectType=FileObjectType.FILE_TYPE;
			}

		}
		else if(fileObjectType==FileObjectType.FILE_TYPE)
		{
			if(new File(Global.GET_INTERNAL_STORAGE_PATH_STORAGE_DIR()).getParent().startsWith(fileclickselected))
			{
				fileObjectType=FileObjectType.ROOT_TYPE;
			}
		}


		if(fileObjectType==FileObjectType.USB_TYPE)
		{
			if(MainActivity.usbFileRoot!=null)
			{
				try {
					currentUsbFile=MainActivity.usbFileRoot.search(fileclickselected);

				} catch (IOException e) {

				}
			}
		}

		if (!Global.HASHMAP_FILE_POJO.containsKey(fileObjectType+fileclickselected)) {

			pbf_polling=ProgressBarFragment.getInstance();
			pbf_polling.show(FileSelectorActivity.FM,""); // don't show when archive view to avoid double pbf
			new Thread(new Runnable() {

				@Override
				public void run() {
					filled_filePOJOs=false;
					filled_filePOJOs=FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,fileclickselected,currentUsbFile,false);
				}
			}).start();
		}
		else
		{
			filePOJOS=Global.HASHMAP_FILE_POJO.get(fileObjectType+fileclickselected);
			filePOJOS_filtered=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+fileclickselected);
			filled_filePOJOs=true;
		}

	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		context=getContext();
		if(cleared_cache)
		{
			cleared_cache=false;
			local_activity_delete=false;
			modification_observed=false;
			pbf_polling=ProgressBarFragment.getInstance();
			pbf_polling.show(FileSelectorActivity.FM,""); // don't show when archive view to avoid double pbf
			new Thread(new Runnable() {

				@Override
				public void run() {
					filled_filePOJOs=false;
					filled_filePOJOs=FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,fileclickselected,currentUsbFile,false);
				}
			}).start();

		}
		View v=inflater.inflate(R.layout.fragment_file_selector,container,false);
		fileSelectorActivity=(FileSelectorActivity)context;
		fileSelectorActivity.addFragmentCommunicationListener(this);

		fileModifyObserver=FileModifyObserver.getInstance(fileclickselected);
		fileModifyObserver.setFileObserverListener(this);

		TextView current_folder_label=v.findViewById(R.id.file_selector_current_folder_label);
		current_folder_label.setText(R.string.current_folder_colon);
        folder_selected_textview = v.findViewById(R.id.file_selector_folder_selected);
		recycler_view=v.findViewById(R.id.file_selectorRecyclerView);
		folder_empty_textview=v.findViewById(R.id.file_selector_folder_empty);

		if(Global.FILE_GRID_LAYOUT)
		{
			GridLayoutManager glm = new GridLayoutManager(context, Global.GRID_COUNT - 1);
			SpacesItemDecoration spacesItemDecoration=new SpacesItemDecoration(Global.ONE_DP);
			recycler_view.addItemDecoration(spacesItemDecoration);
			recycler_view.setLayoutManager(glm);
		}
		else
		{
			LinearLayoutManager llm = new LinearLayoutManager(context);
			recycler_view.setLayoutManager(llm);
		}


		folder_selected_textview.setText(fileclickselected);
		after_filledFilePojos_procedure();

		return v;
		
	}


	public static FileSelectorDialog getInstance(FileObjectType fileObjectType)
	{
		FileSelectorDialog fileSelectorDialog=new FileSelectorDialog();
		Bundle bundle=new Bundle();
		bundle.putSerializable("fileObjectType",fileObjectType);
		fileSelectorDialog.setArguments(bundle);
		return fileSelectorDialog;
	}


	@Override
	public void onResume() {
		super.onResume();
		if(local_activity_delete)
		{
			cleared_cache=false;
			modification_observed=false;
			local_activity_delete=false;
			after_filledFilePojos_procedure();
		}
		else if(modification_observed && ArchiveDeletePasteFileService1.SERVICE_COMPLETED && ArchiveDeletePasteFileService2.SERVICE_COMPLETED && ArchiveDeletePasteFileService3.SERVICE_COMPLETED)
		{
			cleared_cache=false;
			modification_observed=false;
			local_activity_delete=false;
			pbf_polling=ProgressBarFragment.getInstance();
			pbf_polling.show(FileSelectorActivity.FM,""); // don't show when archive view to avoid double pbf
			new Thread(new Runnable() {

				@Override
				public void run() {
					filled_filePOJOs=false;
					filled_filePOJOs=FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,fileclickselected,currentUsbFile,false);
				}
			}).start();
			//FilePOJOUtil.UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(fileclickselected,fileObjectType); //update parent filepojohashmap
			Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION, LocalBroadcastManager.getInstance(context)); //as file observer is triggered only once, not being trigger on default fragment
			after_filledFilePojos_procedure();
		}
	}

	private void after_filledFilePojos_procedure()
	{
		final Handler handler_inter=new Handler();
		handler_inter.post(new Runnable() {
			@Override
			public void run() {
				if(filled_filePOJOs)
				{
					if(MainActivity.SHOW_HIDDEN_FILE)
					{
						filePOJO_list=filePOJOS;
						totalFilePOJO_list=filePOJOS;
					}
					else
					{
						filePOJO_list=filePOJOS_filtered;
						totalFilePOJO_list=filePOJOS_filtered;
					}
					totalFilePOJO_list_Size=totalFilePOJO_list.size();
					file_list_size=filePOJO_list.size();
					fileSelectorActivity.file_number.setText(""+file_list_size);

					Collections.sort(filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));
					adapter=new FileSelectorAdapter();
					set_adapter();
					if(pbf_polling!=null && pbf_polling.getDialog()!=null)
					{
						pbf_polling.dismissAllowingStateLoss();
					}

					handler_inter.removeCallbacks(this);
				}
				else
				{
					handler_inter.postDelayed(this,50);
				}
			}
		});


	}


	@Override
	public void onStop() {
		super.onStop();
		fileModifyObserver.startWatching();
		if(pbf_polling!=null && pbf_polling.getDialog()!=null)
		{
			pbf_polling.dismissAllowingStateLoss();
		}
	}


	@Override
	public void onDestroyView()
	{
		super.onDestroyView();
		fileModifyObserver.stopWatching();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		fileSelectorActivity.removeFragmentCommunicationListener(this);
	}

	@Override
	public void onFragmentCacheClear() {
		cleared_cache=true;
	}

	@Override
	public void onSettingUsbFileRootNull() {
		currentUsbFile=null;
	}

	@Override
	public void onModificationObserved() {
		modification_observed=true;
	}

	@Override
	public void onFileModified() {
		modification_observed=true;
	}



	private void set_adapter()
	{
		recycler_view.setAdapter(adapter);
		if(file_list_size==0)
		{
			recycler_view.setVisibility(View.GONE);
			folder_empty_textview.setVisibility(View.VISIBLE);
		}
		else
		{
			recycler_view.setVisibility(View.VISIBLE);
			folder_empty_textview.setVisibility(View.GONE);
		}
	}

	public class FileSelectorAdapter extends RecyclerView.Adapter<FileSelectorAdapter.ViewHolder>
	{
		@Override
		public FileSelectorAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
		{
			// TODO: Implement this method
			return new FileSelectorAdapter.ViewHolder(new RecyclerViewLayout(context,false));
		}

		@Override
		public void onBindViewHolder(final FileSelectorDialog.FileSelectorAdapter.ViewHolder p1, int p2)
		{
			// TODO: Implement this method
			FilePOJO file=filePOJO_list.get(p2);
			p1.v.setData(file,false);
		}

		@Override
		public int getItemCount()
		{
			// TODO: Implement this method
			return filePOJO_list.size();
		}

		class ViewHolder extends RecyclerView.ViewHolder
		{
			final RecyclerViewLayout v;
			FileObjectType fileObjectType;
			ViewHolder(RecyclerViewLayout v)
			{
				super(v);
				this.v=v;
				v.setOnClickListener(new View.OnClickListener()
				{

					public void onClick(View v)
					{
						int pos=getBindingAdapterPosition();
						FilePOJO filePOJO=filePOJO_list.get(pos);
						fileObjectType=filePOJO.getFileObjectType();
						if(filePOJO.getIsDirectory())
						{
							fileSelectorActivity.createFileSelectorFragmentTransaction(filePOJO);
							FileSelectorRecentDialog.ADD_FILE_POJO_TO_RECENT(filePOJO);
						}
						else
						{
							if(fileSelectorActivity.action_sought_request_code==FileSelectorActivity.PICK_FILE_REQUEST_CODE)
							{
								Uri uri = null;
								if(fileObjectType==FileObjectType.USB_TYPE)
								{
									if(check_availability_USB_SAF_permission(filePOJO.getPath(),fileObjectType))
									{
										uri=FileUtil.getDocumentUri(filePOJO.getPath(),tree_uri,tree_uri_path);
									}
								}
								else if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
								{
									File file=new File(filePOJO.getPath());
									if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
									{
										uri = FileProvider.getUriForFile(context,context.getPackageName()+".provider",file);
									}
									else
									{
										uri=Uri.fromFile(file);
									}
								}

								if(uri!=null)
								{
									String file_extn="";
									String file_path=filePOJO.getPath();
									int file_extn_idx=file_path.lastIndexOf(".");
									if(file_extn_idx!=-1)
									{
										file_extn=file_path.substring(file_extn_idx+1);
									}
									Intent intent=new Intent(Intent.ACTION_VIEW);
									FileIntentDispatch.SET_INTENT_FOR_VIEW(intent,null,filePOJO.getPath(),file_extn,filePOJO.getFileObjectType(),false,false,uri);
									fileSelectorActivity.setResult(Activity.RESULT_OK,intent);
								}
								else
								{
									fileSelectorActivity.setResult(Activity.RESULT_CANCELED);
								}
								fileSelectorActivity.finish();

							}
						}

					}
				});
			}
		}
	}

	public void seekSAFPermission()
	{
		fileSelectorActivity.clear_cache=false;
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

	public void clearSelectionAndNotifyDataSetChanged()
	{
		if(adapter!=null)
		{
			adapter.notifyDataSetChanged();
			file_list_size=filePOJO_list.size();
			fileSelectorActivity.file_number.setText(""+file_list_size);
			totalFilePOJO_list_Size=totalFilePOJO_list.size();

			if(file_list_size==0)
			{
				recycler_view.setVisibility(View.GONE);
				folder_empty_textview.setVisibility(View.VISIBLE);
			}
			else
			{
				recycler_view.setVisibility(View.VISIBLE);
				folder_empty_textview.setVisibility(View.GONE);
			}
		}

	}


	public void clear_cache_and_refresh()
	{
		fileSelectorActivity.clearCache();
		Global.WORKOUT_AVAILABLE_SPACE();
	}

	private boolean check_availability_USB_SAF_permission(String file_path,FileObjectType fileObjectType)
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
			safpermissionhelper.show(FileSelectorActivity.FM,"saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
	}



	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}

}



