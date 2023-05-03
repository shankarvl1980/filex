package svl.kadatha.filex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import me.jahnen.libaums.core.fs.UsbFile;

public class DetailFragment extends Fragment implements MainActivity.DetailFragmentCommunicationListener, FileModifyObserver.FileObserverListener
{
	public List<FilePOJO> filePOJO_list,totalFilePOJO_list;
	public int totalFilePOJO_list_Size;
	public RecyclerView filepath_recyclerview;
	public RecyclerView recyclerView;
	public ImageView layout_image_view,filter_image_view,time_image_view;
	LinearLayoutManager llm;
	GridLayoutManager glm;
	public boolean grid_layout;
	private TinyDB tinyDB;
	TextView folder_empty;
	DetailRecyclerViewAdapter adapter;
	public FilePathRecyclerViewAdapter filepath_adapter;
	public String fileclickselected="";
	public String file_click_selected_name="";
	public UsbFile currentUsbFile;
	public static final String USB_FILE_PREFIX="usb:";
	public static final String FTP_FILE_PREFIX="ftp:";

	public static boolean CUT_SELECTED;
	public static boolean COPY_SELECTED;
	public static FileObjectType CUT_COPY_FILE_OBJECT_TYPE;
	public static String CUT_COPY_FILECLICKSELECTED="";
	public static ArrayList<String> FILE_SELECTED_FOR_CUT_COPY=new ArrayList<>();

	public  String search_file_name;
	public  Set<FilePOJO> search_in_dir=new HashSet<>();
	public  String search_file_type;
	public  boolean search_whole_word,search_case_sensitive,search_regex;
	private long search_lower_limit_size=0;
	private long search_upper_limit_size=0;
	
	static final String SEARCH_RESULT="Search";

	public MainActivity mainActivity;
	private Context context;

	public FileObjectType fileObjectType;
	public int file_list_size;
	boolean is_toolbar_visible=true;
	public Uri tree_uri;
	public String tree_uri_path="";
	public boolean local_activity_delete,modification_observed;
	private FileModifyObserver fileModifyObserver;
	public static FilePOJO TO_BE_MOVED_TO_FILE_POJO;
	private FilePOJO clicked_filepojo;
	public FrameLayout progress_bar;
	public FilePOJOViewModel viewModel;
	private static final String CANCEL_PROGRESS_REQUEST_CODE="search_cancel_progress_request_code";
	private final static String SAF_PERMISSION_REQUEST_CODE="detail_fragment_saf_permission_request_code";
	private final static String ALBUM_SELECT_REQUEST_CODE="detail_fragment_album_select_request_code";
	private static final List<String> LIBRARY_CATEGORIES=new ArrayList<>(Arrays.asList("Download","Document","Image","Audio","Video", "Archive","APK"));

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		mainActivity=(MainActivity)context;
		mainActivity.addFragmentCommunicationListener(this);
		tinyDB=new TinyDB(context);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mainActivity.removeFragmentCommunicationListener(this);
		mainActivity=null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		Bundle bundle=getArguments();
		fileObjectType=(FileObjectType)bundle.getSerializable("fileObjectType");
		fileclickselected=getTag();
		if(fileObjectType==FileObjectType.ROOT_TYPE)
		{
			if(FileUtil.isFromInternal(FileObjectType.FILE_TYPE,fileclickselected) || (Global.EXTERNAL_STORAGE_PATH!=null && !Global.EXTERNAL_STORAGE_PATH.equals("") && Global.IS_CHILD_FILE (fileclickselected,Global.EXTERNAL_STORAGE_PATH)))
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

		if(fileclickselected.equals(File.separator))
		{
			if(fileObjectType==FileObjectType.USB_TYPE)
			{
				file_click_selected_name=USB_FILE_PREFIX+fileclickselected;
			}
			else if(fileObjectType==FileObjectType.FTP_TYPE)
			{
				file_click_selected_name=FTP_FILE_PREFIX+fileclickselected;
			}
			else
			{
				file_click_selected_name=fileclickselected;
			}
		}
		else
		{
			file_click_selected_name=new File(fileclickselected).getName();
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
		View v=inflater.inflate(R.layout.fragment_detail,container,false);
		fileModifyObserver=FileModifyObserver.getInstance(fileclickselected);
		fileModifyObserver.setFileObserverListener(this);
		filepath_recyclerview=v.findViewById(R.id.fragment_detail_filepath_container);
		time_image_view=v.findViewById(R.id.fragment_detail_time_image);
		time_image_view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(viewModel.library_time_desc)
				{
					Collections.sort(filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));
					viewModel.library_time_desc=false;
				}
				else
				{
					Collections.sort(filePOJO_list,FileComparator.FilePOJOComparate("f_date_desc",false));
					viewModel.library_time_desc=true;
				}
				time_image_view.setSelected(viewModel.library_time_desc);
				adapter.notifyDataSetChanged();
			}
		});
		filter_image_view=v.findViewById(R.id.fragment_detail_filter_image);
		filter_image_view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(viewModel.filePOJOS.size()==0)
				{
					//Global.print(context,"No files to filter");
					return;
				}
				mainActivity.set_visibility_searchbar(false);
				LibraryAlbumSelectDialog libraryAlbumSelectDialog=LibraryAlbumSelectDialog.getInstance(ALBUM_SELECT_REQUEST_CODE,file_click_selected_name);
				libraryAlbumSelectDialog.show(mainActivity.fm,"");

			}
		});

		layout_image_view=v.findViewById(R.id.fragment_detail_layout_image);
		layout_image_view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(file_click_selected_name.equals("Image") || file_click_selected_name.equals("Video"))
				{
					Global.IMAGE_VIDEO_GRID_LAYOUT=!Global.IMAGE_VIDEO_GRID_LAYOUT;
					layout_image_view.setImageResource(Global.IMAGE_VIDEO_GRID_LAYOUT ? R.drawable.list_layout_icon : R.drawable.grid_layout_icon);
					tinyDB.putBoolean("image_video_grid_layout",Global.IMAGE_VIDEO_GRID_LAYOUT);

				}
				mainActivity.fm.beginTransaction().detach(DetailFragment.this).commit();
				mainActivity.fm.beginTransaction().attach(DetailFragment.this).commit();
			}
		});
		progress_bar=v.findViewById(R.id.fragment_detail_progressbar);

		recyclerView=v.findViewById(R.id.fragment_detail_container);
		DividerItemDecoration itemdecor=new DividerItemDecoration(context,DividerItemDecoration.HORIZONTAL);
		itemdecor.setDrawable(ContextCompat.getDrawable(context,R.drawable.right_private_icon));
		filepath_recyclerview.addItemDecoration(itemdecor );
		LinearLayoutManager file_path_lm=new LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false);
		filepath_recyclerview.setLayoutManager(file_path_lm);

		if(file_click_selected_name.equals("Image") || file_click_selected_name.equals("Video"))
		{
			grid_layout=Global.IMAGE_VIDEO_GRID_LAYOUT;
		}
		else
		{
			grid_layout=Global.FILE_GRID_LAYOUT;
		}

		if(grid_layout)
		{
			glm=new GridLayoutManager(context,Global.GRID_COUNT);
			recyclerView.setLayoutManager(glm);
			int top_padding=recyclerView.getPaddingTop();
			int bottom_padding=recyclerView.getPaddingBottom();
			recyclerView.setPadding(Global.RECYCLERVIEW_ITEM_SPACING,top_padding,Global.RECYCLERVIEW_ITEM_SPACING,bottom_padding);
		}
		else
		{
			llm=new LinearLayoutManager(context);
			recyclerView.setLayoutManager(llm);
		}

		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener()
		{
			int scroll_distance=0;
			final int threshold=5;
		
			public void onScrolled(RecyclerView rv, int dx, int dy)
			{
				super.onScrolled(rv,dx,dy);
				if(scroll_distance>threshold && is_toolbar_visible)
				{
					switch (mainActivity.viewModel.toolbar_shown) {
						case "bottom":
							mainActivity.bottom_toolbar.animate().translationY(mainActivity.bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
							break;
						case "actionmode":
							mainActivity.actionmode_toolbar.animate().translationY(mainActivity.actionmode_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
							break;
						case "paste":
							mainActivity.paste_toolbar.animate().translationY(mainActivity.paste_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
							break;

					}

					is_toolbar_visible=false;
					scroll_distance=0;
				}
				else if(scroll_distance<-threshold && !is_toolbar_visible)
				{
					switch (mainActivity.viewModel.toolbar_shown) {
						case "bottom":
							mainActivity.bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
							break;
						case "actionmode":
							mainActivity.actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
							break;
						case "paste":
							mainActivity.paste_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
							break;

					}
					is_toolbar_visible=true;
					scroll_distance=0;
				}

				if((is_toolbar_visible && dy>0) || (!is_toolbar_visible && dy<0))
				{
					scroll_distance+=dy;
				}
			}

		});

		folder_empty=v.findViewById(R.id.empty_folder);
		filepath_adapter=new FilePathRecyclerViewAdapter(fileclickselected);
		viewModel=new ViewModelProvider(this).get(FilePOJOViewModel.class);
		if (!Global.HASHMAP_FILE_POJO.containsKey(fileObjectType+fileclickselected))
		{
			if (fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE)
			{
				if (mainActivity == null)
				{
					context = getContext();
					mainActivity = (MainActivity) context;
				}
				search_file_name = mainActivity.search_file_name;
				search_in_dir = mainActivity.search_in_dir;
				search_file_type = mainActivity.search_file_type;
				search_whole_word = mainActivity.search_whole_word;
				search_case_sensitive = mainActivity.search_case_sensitive;
				search_regex = mainActivity.search_regex;
				search_lower_limit_size = mainActivity.search_lower_limit_size;
				search_upper_limit_size = mainActivity.search_upper_limit_size;

				if (LIBRARY_CATEGORIES.contains(fileclickselected))
				{
					viewModel.getLibraryList(fileclickselected);
				}
				else
				{
					viewModel.populateLibrarySearchFilePOJO(fileObjectType, search_in_dir, file_click_selected_name, fileclickselected, search_file_name, search_file_type, search_whole_word, search_case_sensitive, search_regex, search_lower_limit_size, search_upper_limit_size);
				}
			}
			else
			{
				viewModel.populateFilePOJO(fileObjectType, fileclickselected, currentUsbFile, false, false);
			}
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

		viewModel.copyFtpAsyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
			@Override
			public void onChanged(AsyncTaskStatus asyncTaskStatus) {
				if(asyncTaskStatus==AsyncTaskStatus.STARTED)
				{
					progress_bar.setVisibility(View.VISIBLE);
				}
				else if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
				{
					progress_bar.setVisibility(View.GONE);
					if(viewModel.ftp_cached_file_path!=null)
					{
						FileIntentDispatch.openFile(context,viewModel.ftp_cached_file_path,"",false,viewModel.ftp_cached_file_fileObjectType,false,Global.CACHE_FILE_MAX_LIMIT);
					}

					viewModel.copyFtpAsyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
				}

			}
		});

		mainActivity.fm.setFragmentResultListener(CANCEL_PROGRESS_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(CANCEL_PROGRESS_REQUEST_CODE))
				{
					viewModel.cancel(true);
				}
			}
		});

		mainActivity.fm.setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(SAF_PERMISSION_REQUEST_CODE))
				{
					tree_uri=result.getParcelable("tree_uri");
					tree_uri_path=result.getString("tree_uri_path");
				}
			}
		});

		mainActivity.fm.setFragmentResultListener(ALBUM_SELECT_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(ALBUM_SELECT_REQUEST_CODE))
				{
					if(progress_bar.getVisibility()!=View.VISIBLE && result.getString("library_type").equals(fileclickselected))
					{
						String parent_file_name=result.getString("parent_file_name");
						if(parent_file_name!=null)
						{
							filepath_adapter=new FilePathRecyclerViewAdapter(fileclickselected+File.separator+result.getString("parent_file_name"));

						}
						else
						{
							filepath_adapter=new FilePathRecyclerViewAdapter(fileclickselected);
						}
						filepath_recyclerview.setAdapter(filepath_adapter);
						viewModel.library_filter_path=result.getString("parent_file_path");
						adapter.getFilter().filter(null);

					}
				}
			}
		});
		return v;
	}


	@Override
	public void onResume() {
		super.onResume();
		if(local_activity_delete)
		{
			modification_observed=false;
			local_activity_delete=false;
			if(MainActivity.SHOW_HIDDEN_FILE)
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
			file_list_size=totalFilePOJO_list_Size;
			mainActivity.file_number_view.setText(viewModel.mselecteditems.size()+"/"+file_list_size);
			Collections.sort(filePOJO_list, viewModel.library_time_desc ? FileComparator.FilePOJOComparate("f_date_desc", false) : FileComparator.FilePOJOComparate(Global.SORT, false));
			time_image_view.setSelected(viewModel.library_time_desc);
			adapter.notifyDataSetChanged();
		}

		else if(modification_observed)
		{
			mainActivity.actionmode_finish(this,fileclickselected);
			modification_observed=false;
			local_activity_delete=false;
			progress_bar.setVisibility(View.VISIBLE);
			if(fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
			{
				viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
				if(LIBRARY_CATEGORIES.contains(fileclickselected))
				{
					viewModel.getLibraryList(fileclickselected);
				}
				else
				{
					viewModel.populateLibrarySearchFilePOJO(fileObjectType,search_in_dir,file_click_selected_name,fileclickselected,search_file_name,search_file_type,search_whole_word,search_case_sensitive,search_regex,search_lower_limit_size,search_upper_limit_size);
				}
			}
			else
			{
				viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
				viewModel.populateFilePOJO(fileObjectType,fileclickselected,currentUsbFile,false,false);
			}

			ExecutorService executorService=MyExecutorService.getExecutorService();
			executorService.execute(new Runnable() {
				@Override
				public void run() {
					FilePOJOUtil.UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(fileclickselected,fileObjectType);
				}
			});
		}
	}

	private void after_filledFilePojos_procedure()
	{
		if(MainActivity.SHOW_HIDDEN_FILE)
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
		file_list_size=totalFilePOJO_list_Size;
		Collections.sort(filePOJO_list, viewModel.library_time_desc ? FileComparator.FilePOJOComparate("f_date_desc", false) : FileComparator.FilePOJOComparate(Global.SORT, false));
		adapter=new DetailRecyclerViewAdapter(context);
		set_adapter();
		progress_bar.setVisibility(View.GONE);

		if(TO_BE_MOVED_TO_FILE_POJO!=null)
		{
			int idx=filePOJO_list.indexOf(TO_BE_MOVED_TO_FILE_POJO);
			if(llm!=null)
			{
				llm.scrollToPositionWithOffset(idx,0);
			}
			else if(glm!=null)
			{
				glm.scrollToPositionWithOffset(idx,0);
			}

			TO_BE_MOVED_TO_FILE_POJO=null;
		}
		mainActivity.file_number_view.setText(viewModel.mselecteditems.size()+"/"+file_list_size);
	}


	@Override
	public void onStop() {
		super.onStop();
		fileModifyObserver.startWatching();
	
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		fileModifyObserver.stopWatching();
		fileModifyObserver.setFileObserverListener(null);
		if(adapter!=null)adapter.setCardViewClickListener(null);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mainActivity.removeFragmentCommunicationListener(this);
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
		else if(this.fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
		{
			//cache_cleared=true;
		}

	}

	@Override
	public void setUsbFileRootNull() {
		currentUsbFile=null;
	}


	@Override
	public void onFileModified() {
		Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION,LocalBroadcastManager.getInstance(context),MainActivity.ACTIVITY_NAME);
	}


	public static DetailFragment getInstance(FileObjectType fileObjectType)
	{
		DetailFragment df=new DetailFragment();
		Bundle bundle=new Bundle();
		bundle.putSerializable("fileObjectType",fileObjectType);
		df.setArguments(bundle);
		return df;
	}

	private final ActivityResultLauncher<Intent> activityResultLauncher_unknown_package_install_permission=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
	@Override
	public void onActivityResult(ActivityResult result) {
		if (result.getResultCode()== Activity.RESULT_OK)
		{
			if(clicked_filepojo!=null)file_open_intent_despatch(clicked_filepojo.getPath(),clicked_filepojo.getFileObjectType(),clicked_filepojo.getName(),false,clicked_filepojo.getSizeLong());
			clicked_filepojo=null;
		}
		else
		{
			Global.print(context,getString(R.string.permission_not_granted));
		}
	}
});


	public void file_open_intent_despatch(final String file_path, final FileObjectType fileObjectType, String file_name,boolean select_app,long file_size)
	{
		int idx=file_name.lastIndexOf(".");
		String file_ext="";
		if(idx!=-1)
		{
			file_ext=file_name.substring(idx+1);
		}

		if(file_ext.equals("") || !Global.CHECK_APPS_FOR_RECOGNISED_FILE_EXT(context,file_ext))
		{
			FileTypeSelectDialog fileTypeSelectDialog=FileTypeSelectDialog.getInstance(file_path,fileObjectType,tree_uri,tree_uri_path,select_app,file_size);
			fileTypeSelectDialog.show(mainActivity.fm,"");
		}
		else
		 {
		 	if(file_ext.matches("(?i)apk"))
		 	{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					if (!mainActivity.getPackageManager().canRequestPackageInstalls()) {
						Intent unknown_package_install_intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
						unknown_package_install_intent.setData(Uri.parse(String.format("package:%s", Global.FILEX_PACKAGE)));
						activityResultLauncher_unknown_package_install_permission.launch(unknown_package_install_intent);
						return;
					}
				}
			}
		 	if(fileObjectType==FileObjectType.USB_TYPE)
			 {
				 if(check_availability_USB_SAF_permission(file_path,fileObjectType))
				 {
					 FileIntentDispatch.openUri(context,file_path,"", false,fileObjectType,tree_uri,tree_uri_path,select_app,file_size);
				 }
			 }
			 else if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
			 {
				 FileIntentDispatch.openFile(context,file_path,"",false,fileObjectType,select_app,file_size);
			 }
			 else if(fileObjectType==FileObjectType.FTP_TYPE)
			{
				if(file_size>Global.CACHE_FILE_MAX_LIMIT)
				{
					Global.print(context,context.getString(R.string.file_is_large_copy_to_device_storage));
					return;
				}
				progress_bar.setVisibility(View.VISIBLE);
				viewModel.copyFtpToDevice(file_path,fileObjectType);
			}
		 }
	}

	public void set_adapter()
	{
		if(file_click_selected_name.equals("Document") || file_click_selected_name.equals("Image")
				|| file_click_selected_name.equals("Audio")|| file_click_selected_name.equals("Video")
		)
		{
			filter_image_view.setVisibility(View.VISIBLE);

			time_image_view.setVisibility(View.VISIBLE);
			time_image_view.setSelected(viewModel.library_time_desc);
			if(viewModel.library_filter_path!=null)
			{
				adapter.getFilter().filter(null);
				filepath_adapter=new FilePathRecyclerViewAdapter(file_click_selected_name+File.separator+new File(viewModel.library_filter_path).getName());
			}
			if(file_click_selected_name.equals("Image") || file_click_selected_name.equals("Video"))
			{
				layout_image_view.setVisibility(View.VISIBLE);
				layout_image_view.setImageResource(Global.IMAGE_VIDEO_GRID_LAYOUT ? R.drawable.list_layout_icon : R.drawable.grid_layout_icon);
			}
		}

		filepath_recyclerview.setAdapter(filepath_adapter);
		filepath_recyclerview.scrollToPosition(filepath_adapter.getItemCount()-1);
		recyclerView.setAdapter(adapter);
		if(file_list_size==0)
		{
			recyclerView.setVisibility(View.GONE);
			folder_empty.setVisibility(View.VISIBLE);
		}
		else
		{
			recyclerView.setVisibility(View.VISIBLE);
			folder_empty.setVisibility(View.GONE);
		}

		adapter.setCardViewClickListener(new DetailRecyclerViewAdapter.CardViewClickListener()
			{
				public void onClick(FilePOJO filePOJO)
				{
					clicked_filepojo=filePOJO;
					if(filePOJO.getIsDirectory())
					{
						mainActivity.createFragmentTransaction(filePOJO.getPath(),filePOJO.getFileObjectType());
					}
					else
					{
						file_open_intent_despatch(filePOJO.getPath(),filePOJO.getFileObjectType(),filePOJO.getName(),false,filePOJO.getSizeLong());
					}
					RecentDialog.ADD_FILE_POJO_TO_RECENT(filePOJO);
				}

				public void onLongClick(FilePOJO filePOJO)
				{
					if(!mainActivity.viewModel.toolbar_shown.equals("paste") && !mainActivity.viewModel.toolbar_shown.equals("extract"))
					{
						mainActivity.actionmode_toolbar.setVisibility(View.VISIBLE);
						mainActivity.paste_toolbar.setVisibility(View.GONE);
						mainActivity.bottom_toolbar.setVisibility(View.GONE);
						mainActivity.viewModel.toolbar_shown ="actionmode";
						mainActivity.actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
					}
					else if(mainActivity.viewModel.toolbar_shown.equals("paste"))
					{
						mainActivity.paste_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
					}

					is_toolbar_visible=true;
				}
			});
	}


	public void clearSelectionAndNotifyDataSetChanged()
	{
		viewModel.mselecteditems=new SparseBooleanArray();
		viewModel.mselecteditemsFilePath=new SparseArray<>();
		if(adapter!=null)
		{
			adapter.notifyDataSetChanged();
			file_list_size=filePOJO_list.size();
			mainActivity.file_number_view.setText(viewModel.mselecteditems.size()+"/"+file_list_size);
			totalFilePOJO_list_Size=totalFilePOJO_list.size();

			if(file_list_size==0)
			{
				recyclerView.setVisibility(View.GONE);
				folder_empty.setVisibility(View.VISIBLE);
			}
			else
			{
				recyclerView.setVisibility(View.VISIBLE);
				folder_empty.setVisibility(View.GONE);
			}
		}
		
	}

	private boolean check_SAF_permission(String new_file_path,FileObjectType fileObjectType)
	{
		UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(new_file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();
		}

		if(uriPOJO==null || tree_uri_path.equals(""))
		{
			SAFPermissionHelperDialog safpermissionhelper=SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE,new_file_path,fileObjectType);
			safpermissionhelper.show(((AppCompatActivity)context).getSupportFragmentManager(),"saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
	}

	public boolean check_availability_USB_SAF_permission(String file_path,FileObjectType fileObjectType)
	{
		if(MainActivity.usbFileRoot==null)
		{
			return false;
		}
		UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path,fileObjectType);
		if(uriPOJO!=null)
		{
			tree_uri_path=uriPOJO.get_path();
			tree_uri=uriPOJO.get_uri();
		}

		if(uriPOJO==null || tree_uri_path.equals(""))
		{
			SAFPermissionHelperDialog safpermissionhelper=SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE,file_path,fileObjectType);
			safpermissionhelper.show(mainActivity.fm,"saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
	}

	public class FilePathRecyclerViewAdapter extends RecyclerView.Adapter<FilePathRecyclerViewAdapter.ViewHolder>
	{
		final String[] filepath_string_array;
		final String display_path;
		final String truncated_path;

		FilePathRecyclerViewAdapter(String p)
		{
			truncated_path=p;
			display_path=p;

			if(p.equals(File.separator))
			{
				filepath_string_array= new String[]{""};

			}
			else
			{
				filepath_string_array=display_path.split(File.separator);
			}
		}

		
		class ViewHolder extends RecyclerView.ViewHolder
		{
			final FrameLayout fl;
			final TextView file_path_string_tv;
			ViewHolder(FrameLayout fl)
			{
				super(fl);
				this.fl=fl;
				file_path_string_tv=fl.findViewById(R.id.filepath_recyclerview_TextView);
				this.fl.setOnClickListener(new View.OnClickListener()
				{
					public void onClick(View v)
					{
						int p2=getBindingAdapterPosition();
						StringBuilder file_path;
						file_path = new StringBuilder();
						for(int i=1; i<=p2 ; ++i)
						{
							file_path.append(File.separator).append(filepath_string_array[i]);
						}

						if(fileObjectType==FileObjectType.FILE_TYPE)
						{
							String fp=file_path.toString();
							File f=new File(fp);
							if(f.exists() && f.list()!=null)
							{
								mainActivity.createFragmentTransaction(fp,fileObjectType);
							}
						}
						else if(fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
						{
							if(p2==0)
							{
								if(progress_bar.getVisibility()!=View.VISIBLE && viewModel.library_filter_path!=null)
								{
									viewModel.library_filter_path=null;
									filepath_adapter=new FilePathRecyclerViewAdapter(fileclickselected);
									filepath_recyclerview.setAdapter(filepath_adapter);
									adapter.getFilter().filter(null);

								}
							}
						}
						else if(fileObjectType==FileObjectType.USB_TYPE)
						{
							if(MainActivity.usbFileRoot==null)
							{
								return;
							}
							try {
								if(p2==0) file_path.append(File.separator);
								String fp=file_path.toString();
								UsbFile f=MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(fp));
								mainActivity.createFragmentTransaction(fp,fileObjectType);
							} catch (IOException e) {

							}

						}
						else if(fileObjectType==FileObjectType.FTP_TYPE)
                        {
                            String fp=file_path.toString();
                            mainActivity.createFragmentTransaction(fp,fileObjectType);
                        }


					}
				});
			}
		}

		@Override
		public DetailFragment.FilePathRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
		{
			// TODO: Implement this method
			return new ViewHolder((FrameLayout)LayoutInflater.from(context).inflate(R.layout.filepath_recyclerview_layout,p1,false));
		}

		@Override
		public void onBindViewHolder(DetailFragment.FilePathRecyclerViewAdapter.ViewHolder p1, int p2)
		{
			// TODO: Implement this method
			if(filepath_string_array[p2].equals(""))
			{
				p1.file_path_string_tv.setText(File.separator);
			}
			else
			{
				p1.file_path_string_tv.setText(filepath_string_array[p2]);
			}
		}

		@Override
		public int getItemCount()
		{
			// TODO: Implement this method
			return filepath_string_array.length;
		}
	}


}

