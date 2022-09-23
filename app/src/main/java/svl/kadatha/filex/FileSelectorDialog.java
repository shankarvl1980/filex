package svl.kadatha.filex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.jahnen.libaums.core.fs.UsbFile;

public class FileSelectorDialog extends Fragment implements FileSelectorActivity.DetailFragmentCommunicationListener, FileModifyObserver.FileObserverListener
{
	private RecyclerView recycler_view;
    private TextView folder_empty_textview;
    private Context context;
	public FileSelectorAdapter adapter;
	public List<FilePOJO> filePOJO_list,totalFilePOJO_list;
	public int totalFilePOJO_list_Size;
	public FileSelectorActivity fileSelectorActivity;
	public String fileclickselected;
	public FileObjectType fileObjectType;
	public UsbFile currentUsbFile;
	public TextView folder_selected_textview;
	private FileModifyObserver fileModifyObserver;
	public boolean local_activity_delete,modification_observed;
	private Uri tree_uri;
	private String tree_uri_path="";
	public int file_list_size;
	public FrameLayout progress_bar;
	public FilePOJOViewModel viewModel;
	private final static String SAF_PERMISSION_REQUEST_CODE="file_selector_dialog_saf_permission_request_code";

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		fileSelectorActivity=(FileSelectorActivity)context;
		fileSelectorActivity.addFragmentCommunicationListener(this);

	}

	@Override
	public void onDetach() {
		super.onDetach();
		fileSelectorActivity.removeFragmentCommunicationListener(this);
		fileSelectorActivity=null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		fileclickselected=getTag();
		if(fileclickselected==null)
		{
			fileclickselected=Global.INTERNAL_PRIMARY_STORAGE_PATH;
		}
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			fileObjectType=(FileObjectType)bundle.getSerializable("fileObjectType");
		}

		if(fileObjectType==FileObjectType.ROOT_TYPE)
		{
			if(FileUtil.isFromInternal(FileObjectType.FILE_TYPE,fileclickselected) || (Global.EXTERNAL_STORAGE_PATH!=null && !Global.EXTERNAL_STORAGE_PATH.equals("") && Global.IS_CHILD_FILE(fileclickselected,Global.EXTERNAL_STORAGE_PATH)))
			{
				fileObjectType=FileObjectType.FILE_TYPE;
			}
		}
		else if(fileObjectType==FileObjectType.FILE_TYPE)
		{
			for(String path:Global.INTERNAL_STORAGE_PATH_LIST)
			{
				if(Global.IS_CHILD_FILE(new File(path).getParent(),fileclickselected))
				{
					fileObjectType=FileObjectType.ROOT_TYPE;
					break;
				}
			}
		}


		if(fileObjectType==FileObjectType.USB_TYPE)
		{
			if(MainActivity.usbFileRoot!=null)
			{
				try {
					currentUsbFile=MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(fileclickselected));
				} catch (IOException e) {

				}
			}
		}


	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v=inflater.inflate(R.layout.fragment_file_selector,container,false);
		fileModifyObserver=FileModifyObserver.getInstance(fileclickselected);
		fileModifyObserver.setFileObserverListener(this);

		TextView current_folder_label=v.findViewById(R.id.file_selector_current_folder_label);
		current_folder_label.setText(R.string.current_folder);
        folder_selected_textview = v.findViewById(R.id.file_selector_folder_selected);
		recycler_view=v.findViewById(R.id.file_selectorRecyclerView);
		folder_empty_textview=v.findViewById(R.id.file_selector_folder_empty);
		progress_bar=v.findViewById(R.id.file_selector_progressbar);

		if(FileSelectorActivity.FILE_GRID_LAYOUT)
		{
			GridLayoutManager glm = new GridLayoutManager(context, FileSelectorActivity.GRID_COUNT);
			recycler_view.setLayoutManager(glm);
			int top_padding=recycler_view.getPaddingTop();
			int bottom_padding=recycler_view.getPaddingBottom();
			recycler_view.setPadding(Global.RECYCLERVIEW_ITEM_SPACING,top_padding,Global.RECYCLERVIEW_ITEM_SPACING,bottom_padding);
		}
		else
		{
			LinearLayoutManager llm = new LinearLayoutManager(context);
			recycler_view.setLayoutManager(llm);
		}


		folder_selected_textview.setText(fileclickselected);

		viewModel=new ViewModelProvider(this).get(FilePOJOViewModel.class);
		if (!Global.HASHMAP_FILE_POJO.containsKey(fileObjectType+fileclickselected))
		{
			viewModel.populateFilePOJO(fileObjectType,fileclickselected,currentUsbFile,false,false);
		}
		else
		{
			viewModel.filePOJOS=Global.HASHMAP_FILE_POJO.get(fileObjectType+fileclickselected);
			viewModel.filePOJOS_filtered=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+fileclickselected);
			after_filledFilePojos_procedure();
		}

		viewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
			@Override
			public void onChanged(AsyncTaskStatus asyncTaskStatus) {
				if(asyncTaskStatus==AsyncTaskStatus.STARTED)
				{
					progress_bar.setVisibility(View.VISIBLE);
				}
				else if (asyncTaskStatus==AsyncTaskStatus.COMPLETED)
				{
					progress_bar.setVisibility(View.GONE);
				}
				if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
				{
					after_filledFilePojos_procedure();
				}
			}
		});

//		viewModel.mutable_file_count.observe(getViewLifecycleOwner(), new Observer<Integer>() {
//			@Override
//			public void onChanged(Integer integer) {
//				fileSelectorActivity.file_number.setText(""+integer);
//			}
//		});

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
			modification_observed=false;
			local_activity_delete=false;
			if(FileSelectorActivity.SHOW_HIDDEN_FILE)
			{
				filePOJO_list=viewModel.filePOJOS;
				totalFilePOJO_list=viewModel.filePOJOS;
			}
			else
			{
				filePOJO_list=viewModel.filePOJOS_filtered;
				totalFilePOJO_list=viewModel.filePOJOS_filtered;
			}
			totalFilePOJO_list_Size=totalFilePOJO_list.size();
			file_list_size=filePOJO_list.size();
			fileSelectorActivity.file_number.setText(""+file_list_size);
			Collections.sort(filePOJO_list,FileComparator.FilePOJOComparate(FileSelectorActivity.SORT,false));
			adapter.notifyDataSetChanged();
		}
		else if(modification_observed)
		{
			modification_observed=false;
			local_activity_delete=false;
			progress_bar.setVisibility(View.VISIBLE);
			viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
			viewModel.populateFilePOJO(fileObjectType,fileclickselected,currentUsbFile,false,false);

			new Thread(new Runnable() {
				@Override
				public void run() {
					FilePOJOUtil.UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(fileclickselected,fileObjectType);
				}
			}).start();

		}
	}

	private void after_filledFilePojos_procedure()
	{
		if(FileSelectorActivity.SHOW_HIDDEN_FILE)
		{
			filePOJO_list=viewModel.filePOJOS;
			totalFilePOJO_list=viewModel.filePOJOS;
		}
		else
		{
			filePOJO_list=viewModel.filePOJOS_filtered;
			totalFilePOJO_list=viewModel.filePOJOS_filtered;
		}
		totalFilePOJO_list_Size=totalFilePOJO_list.size();
		file_list_size=filePOJO_list.size();
		fileSelectorActivity.file_number.setText(""+file_list_size);

		Collections.sort(filePOJO_list,FileComparator.FilePOJOComparate(FileSelectorActivity.SORT,false));
		adapter=new FileSelectorAdapter();
		set_adapter();
		progress_bar.setVisibility(View.GONE);

	}


	@Override
	public void onStop() {
		super.onStop();
		fileModifyObserver.startWatching();

	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();
		fileModifyObserver.stopWatching();
		fileModifyObserver.setFileObserverListener(null);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		fileSelectorActivity.removeFragmentCommunicationListener(this);
	}

	@Override
	public void onFragmentCacheClear(String file_path, FileObjectType fileObjectType) {
		if(file_path==null || fileObjectType==null)
		{
			//cache_cleared=true;
		}
		else if(Global.IS_CHILD_FILE(this.fileObjectType+fileclickselected,fileObjectType+file_path))
		{
			//cache_cleared=true;
		}
		else if((this.fileObjectType+fileclickselected).equals(fileObjectType+new File(file_path).getParent()))
		{
			//cache_cleared=true;
		}
	}


	@Override
	public void onSettingUsbFileRootNull() {
		currentUsbFile=null;
	}


	@Override
	public void onFileModified() {
		Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION,LocalBroadcastManager.getInstance(context),FileSelectorActivity.ACTIVITY_NAME);
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


	public class FileSelectorAdapter extends RecyclerView.Adapter<FileSelectorAdapter.ViewHolder> implements Filterable
	{
		@Override
		public FileSelectorAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
		{
			// TODO: Implement this method
			return new FileSelectorAdapter.ViewHolder(new RecyclerViewLayout(context,false,true));
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

		@Override
		public Filter getFilter() {
			return new Filter() {
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {

					filePOJO_list = new ArrayList<>();
					if (constraint == null || constraint.length() == 0) {
						filePOJO_list = totalFilePOJO_list;
					} else {
						String pattern = constraint.toString().toLowerCase().trim();
						for (int i = 0; i < totalFilePOJO_list_Size; ++i) {
							FilePOJO filePOJO = totalFilePOJO_list.get(i);
							if (filePOJO.getLowerName().contains(pattern)) {
								filePOJO_list.add(filePOJO);
							}
						}
					}
					return new FilterResults();
				}

				@Override
				protected void publishResults(CharSequence constraint, FilterResults results) {

					int t=filePOJO_list.size();
					/*
					if(mselecteditems.size()>0)
					{
						deselectAll();
					}
					else
					{
						notifyDataSetChanged();
					}

					 */
					clearSelectionAndNotifyDataSetChanged();
					if(t>0)
					{
						recycler_view.setVisibility(View.VISIBLE);
						folder_empty_textview.setVisibility(View.GONE);
					}

					fileSelectorActivity.file_number.setText(""+t);

				}
			};
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
							FileSelectorRecentDialog.ADD_FILE_POJO_TO_RECENT(filePOJO,FileSelectorRecentDialog.FILE_SELECTOR);
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
									uri = FileProvider.getUriForFile(context,context.getPackageName()+".provider",file);
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


	public void clear_cache_and_refresh(String file_path, FileObjectType fileObjectType)
	{
		fileSelectorActivity.clearCache(file_path,fileObjectType);
		modification_observed=true;
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
			SAFPermissionHelperDialog safpermissionhelper=SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE,file_path,fileObjectType);
			safpermissionhelper.show(fileSelectorActivity.fm, "saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
	}

}



