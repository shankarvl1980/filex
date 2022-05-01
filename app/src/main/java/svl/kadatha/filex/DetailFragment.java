package svl.kadatha.filex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.jahnen.libaums.core.fs.UsbFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DetailFragment extends Fragment implements MainActivity.DetailFragmentCommunicationListener, FileModifyObserver.FileObserverListener
{
	
	public List<FilePOJO> filePOJO_list,totalFilePOJO_list;
	public int totalFilePOJO_list_Size;
	public RecyclerView filepath_recyclerview;
	public RecyclerView recyclerView;
	LinearLayoutManager llm;
	GridLayoutManager glm;
	TextView folder_empty;
	DetailRecyclerViewAdapter adapter;
	private FilePathRecyclerViewAdapter filepath_adapter;
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

	public static String SEARCH_FILE_NAME;
	public static Set<FilePOJO> SEARCH_IN_DIR=new HashSet<>();
	public static String SEARCH_FILE_TYPE;
	public static boolean SEARCH_WHOLE_WORD,SEARCH_CASE_SENSITIVE,SEARCH_REGEX;
	
	static final String SEARCH_RESULT="Search";
	public SparseBooleanArray mselecteditems=new SparseBooleanArray();
	public SparseArray<String> mselecteditemsFilePath=new SparseArray<>();
	public MainActivity mainActivity;
	private Context context;
	AsyncTaskLibrarySearch asyncTaskLibrarySearch;
	AsyncTaskFilePopulate asyncTaskFilePopulate;
	public boolean archive_view;

	private AsyncTaskStatus asynctask_status;
	public FileObjectType fileObjectType;
	public int file_list_size;
	//ViewPager viewPager;
	boolean is_toolbar_visible=true;
	private final int request_code=487;
	private Uri tree_uri;
	private String tree_uri_path="";
	private static ProgressBarFragment pbf_polling;
	public boolean filled_filePOJOs;
	public boolean local_activity_delete,modification_observed,cache_cleared;
	private List<FilePOJO> filePOJOS=new ArrayList<>(), filePOJOS_filtered=new ArrayList<>();
	private FileModifyObserver fileModifyObserver;
	public static FilePOJO TO_BE_MOVED_TO_FILE_POJO;
	public static final int UNKNOWN_PACKAGE_REQUEST_CODE=214;
	private FilePOJO clicked_filepojo;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		mainActivity=(MainActivity)context;
		mainActivity.addFragmentCommunicationListener(this);

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
		setRetainInstance(true);
		asynctask_status=AsyncTaskStatus.NOT_YET_STARTED;
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

		if(Global.ARCHIVE_EXTRACT_DIR==null) Global.ARCHIVE_EXTRACT_DIR=new File(context.getFilesDir(),"Archive");
		archive_view=(fileObjectType==FileObjectType.FILE_TYPE) && Global.IS_CHILD_FILE(fileclickselected,Global.ARCHIVE_EXTRACT_DIR.getAbsolutePath()) && mainActivity.archive_view;

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

		if(pbf_polling!=null)
		{
			pbf_polling.dismissAllowingStateLoss();
		}



		if (!Global.HASHMAP_FILE_POJO.containsKey(fileObjectType+fileclickselected)) {
			if(fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
			{
				asyncTaskLibrarySearch=new AsyncTaskLibrarySearch(file_click_selected_name);
				asyncTaskLibrarySearch.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			else
			{
				/*
			    if(mainActivity.fm==null)
				{
					context=getContext();
					mainActivity=(MainActivity)context;
					mainActivity.fm=mainActivity.getSupportFragmentManager();
				}
				else
				{
					if(!archive_view)
					{
						if(pbf_polling==null || pbf_polling.getDialog()==null)
						{
							pbf_polling=ProgressBarFragment.newInstance();
							pbf_polling.show(mainActivity.fm,""); // don't show when archive view to avoid double pbf
						}

					}

				}

				new Thread(new Runnable() {

					@Override
					public void run() {
						filled_filePOJOs=false;
						filled_filePOJOs=FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,fileclickselected,currentUsbFile,archive_view);

					}
				}).start();

			     */
                asyncTaskFilePopulate=new AsyncTaskFilePopulate();
                asyncTaskFilePopulate.executeOnExecutor(svl.kadatha.filex.AsyncTask.THREAD_POOL_EXECUTOR);
			}
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
		if(cache_cleared)
		{
			//Log.d("shankar","in createview, cache_cleared is true");
			cache_cleared=false;
			local_activity_delete=false;
			modification_observed=false;
			if(fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
			{
				asyncTaskLibrarySearch=new AsyncTaskLibrarySearch(file_click_selected_name);
				asyncTaskLibrarySearch.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			else
			{
                /*
			    pbf_polling=ProgressBarFragment.newInstance();
                if(!archive_view)
				{
					pbf_polling.show(mainActivity.fm,""); // don't show when archive view to avoid double pbf
				}

			    new Thread(new Runnable() {

					@Override
					public void run() {
						filled_filePOJOs=false;
						filled_filePOJOs=FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,fileclickselected,currentUsbFile,archive_view);
					}
				}).start();

                 */
                asyncTaskFilePopulate=new AsyncTaskFilePopulate();
                asyncTaskFilePopulate.executeOnExecutor(svl.kadatha.filex.AsyncTask.THREAD_POOL_EXECUTOR);
			}

		}

		View v=inflater.inflate(R.layout.fragment_detail,container,false);
		fileModifyObserver=FileModifyObserver.getInstance(fileclickselected);
		fileModifyObserver.setFileObserverListener(this);
		filepath_recyclerview=v.findViewById(R.id.fragment_detail_filepath_container);


		recyclerView=v.findViewById(R.id.fragment_detail_container);
		DividerItemDecoration itemdecor=new DividerItemDecoration(context,DividerItemDecoration.HORIZONTAL);
		itemdecor.setDrawable(ContextCompat.getDrawable(context,R.drawable.right_private_icon));
		filepath_recyclerview.addItemDecoration(itemdecor );
		LinearLayoutManager file_path_lm=new LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false);
		filepath_recyclerview.setLayoutManager(file_path_lm);

		if(Global.FILE_GRID_LAYOUT)
		{
			glm=new GridLayoutManager(context,Global.GRID_COUNT);
			SpacesItemDecoration spacesItemDecoration=new SpacesItemDecoration(Global.ONE_DP);
			recyclerView.addItemDecoration(spacesItemDecoration);
			recyclerView.setLayoutManager(glm);
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
					switch (mainActivity.toolbar_shown) {
						case "bottom":
							mainActivity.bottom_toolbar.animate().translationY(mainActivity.bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
							break;
						case "actionmode":
							mainActivity.actionmode_toolbar.animate().translationY(mainActivity.actionmode_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
							break;
						case "paste":
							mainActivity.paste_toolbar.animate().translationY(mainActivity.paste_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
							break;
						case "extract":
							mainActivity.extract_toolbar.animate().translationY(mainActivity.extract_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
							break;
					}

					is_toolbar_visible=false;
					scroll_distance=0;
				}
				else if(scroll_distance<-threshold && !is_toolbar_visible)
				{
					switch (mainActivity.toolbar_shown) {
						case "bottom":
							mainActivity.bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
							break;
						case "actionmode":
							mainActivity.actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
							break;
						case "paste":
							mainActivity.paste_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
							break;
						case "extract":
							mainActivity.extract_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
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
		after_filledFilePojos_procedure();
		return v;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		if(modification_observed)
		{
			mainActivity.actionmode_finish(this,fileclickselected);
			cache_cleared=false;
			modification_observed=false;
			local_activity_delete=false;
			if(fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
			{
				asyncTaskLibrarySearch=new AsyncTaskLibrarySearch(file_click_selected_name);
				asyncTaskLibrarySearch.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			else
			{
                /*
			    pbf_polling=ProgressBarFragment.newInstance();
                if(!archive_view)pbf_polling.show(mainActivity.fm,""); // don't show when archive view to avoid double pbf
			    new Thread(new Runnable() {

					@Override
					public void run() {
						filled_filePOJOs=false;
						filled_filePOJOs=FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,fileclickselected,currentUsbFile,archive_view);
					}
				}).start();

                 */
                asyncTaskFilePopulate=new AsyncTaskFilePopulate();
                asyncTaskFilePopulate.executeOnExecutor(svl.kadatha.filex.AsyncTask.THREAD_POOL_EXECUTOR);
			}
			new Thread(new Runnable() {
				@Override
				public void run() {
					FilePOJOUtil.UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(fileclickselected,fileObjectType);
				}
			}).start();
			after_filledFilePojos_procedure();
		}
		else if(local_activity_delete)
		{
			cache_cleared=false;
			modification_observed=false;
			local_activity_delete=false;
			if(fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
			{
				asyncTaskLibrarySearch=new AsyncTaskLibrarySearch(file_click_selected_name);
				asyncTaskLibrarySearch.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
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
                    mainActivity.file_number_view.setText(mselecteditems.size()+"/"+file_list_size);

                    Collections.sort(filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));
					adapter=new DetailRecyclerViewAdapter(context,archive_view);
					set_adapter();


					if(pbf_polling!=null && pbf_polling.getDialog()!=null)
					{
						pbf_polling.dismissAllowingStateLoss();
					}

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
	
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		fileModifyObserver.stopWatching();
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
			cache_cleared=true;
		}
		else if(Global.IS_CHILD_FILE(this.fileObjectType+fileclickselected,fileObjectType+file_path))
		{
			cache_cleared=true;
		}
		else if((this.fileObjectType+fileclickselected).equals(fileObjectType+new File(file_path).getParent()))
		{
			//Log.d("shankar","fragment cache cleared in "+this.fileObjectType.toString());
			cache_cleared=true;
		}
		else if(this.fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
		{
			cache_cleared=true;
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

	public void seekSAFPermission()
	{
		mainActivity.clear_cache=false;
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		activityResultLauncher_SAF_permission.launch(intent);
	}


	private final ActivityResultLauncher<Intent> activityResultLauncher_SAF_permission=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
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
			print(getString(R.string.permission_not_granted));
		}
	}
});

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
			print(getString(R.string.permission_not_granted));
		}
	}
});


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
						if(fileObjectType==FileObjectType.USB_TYPE)
						{
							if(check_availability_USB_SAF_permission(file_path,fileObjectType))
							{
								FileIntentDispatch.openUri(context,file_path,mime_type,false,archive_view,fileObjectType,tree_uri,tree_uri_path);
							}
						}
						else if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
						{
							FileIntentDispatch.openFile(context,file_path,mime_type,false,archive_view,fileObjectType);
						}

					}
				});
			fileTypeSelectFragment.show(mainActivity.fm,"");
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
					 FileIntentDispatch.openUri(context,file_path,"", file_ext.matches("(?i)zip"),archive_view,fileObjectType,tree_uri,tree_uri_path);
				 }
			 }
			 else if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
			 {
				 FileIntentDispatch.openFile(context,file_path,"",file_ext.matches("(?i)zip"),archive_view,fileObjectType);
			 }
		 }
	}

	public void set_adapter()
	{
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
						if(archive_view)
						{
							int idx=filePOJO.getName().lastIndexOf(".");
							if(idx!=-1)
							{
								String file_ext=filePOJO.getName().substring(idx+1);
								if(file_ext.matches("(?i)zip"))
								{
									print(getString(R.string.can_not_open_file));
									return;
								}
							}

							ZipFile zipfile=null;
							try
							{
								zipfile=new ZipFile(MainActivity.ZIP_FILE);
							}
							catch(IOException e){}
							ZipEntry zip_entry=zipfile.getEntry(filePOJO.getPath().substring(Global.ARCHIVE_CACHE_DIR_LENGTH+1));
							if(zip_entry==null)
							{
								print(getString(R.string.can_not_open_file));
								return;
							}


							if(zip_entry.getSize()>5000000)
							{
								print(getString(R.string.file_is_big_please_extract_to_view));
								return;
							}


							ZipFile finalZipfile = zipfile;
							new svl.kadatha.filex.AsyncTask<Void,Void,Boolean>()
							{
								ProgressBarFragment pbf;
								@Override
								protected void onPreExecute() {
									super.onPreExecute();
									pbf=ProgressBarFragment.newInstance();
									pbf.show(mainActivity.fm,null);
								}

								@Override
								protected Boolean doInBackground(Void... voids) {
									return ExtractZipFile.read_zipentry(context, finalZipfile,zip_entry,Global.ARCHIVE_EXTRACT_DIR);
								}

								@Override
								protected void onPostExecute(Boolean aBoolean) {
									super.onPostExecute(aBoolean);
									pbf.dismissAllowingStateLoss();
									if(aBoolean)
									{
										file_open_intent_despatch(filePOJO.getPath(),filePOJO.getFileObjectType(),filePOJO.getName());
									}

								}
							}.executeOnExecutor(svl.kadatha.filex.AsyncTask.THREAD_POOL_EXECUTOR);

						}
						else
						{
							file_open_intent_despatch(filePOJO.getPath(),filePOJO.getFileObjectType(),filePOJO.getName());
						}

					}
					if(!archive_view)RecentDialog.ADD_FILE_POJO_TO_RECENT(filePOJO);
				}

				public void onLongClick(FilePOJO filePOJO)
				{
					if(!mainActivity.toolbar_shown.equals("paste") && !mainActivity.toolbar_shown.equals("extract"))
					{
						mainActivity.actionmode_toolbar.setVisibility(View.VISIBLE);
						mainActivity.paste_toolbar.setVisibility(View.GONE);
						mainActivity.bottom_toolbar.setVisibility(View.GONE);
						mainActivity.toolbar_shown ="actionmode";
						mainActivity.actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
					}
					else if(mainActivity.toolbar_shown.equals("paste"))
					{
						mainActivity.paste_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
					}
					else if(mainActivity.toolbar_shown.equals("extract"))
					{
						mainActivity.extract_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
					}
					is_toolbar_visible=true;
				}
			});
	}
	



	public void clearSelectionAndNotifyDataSetChanged()
	{
		mselecteditems=new SparseBooleanArray();
		mselecteditemsFilePath=new SparseArray<>();
		if(adapter!=null)
		{
			adapter.notifyDataSetChanged();
			file_list_size=filePOJO_list.size();
			mainActivity.file_number_view.setText(mselecteditems.size()+"/"+file_list_size);
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
			safpermissionhelper.show(mainActivity.fm,"saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
	}

	private class AsyncTaskFilePopulate extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
    {

    	@Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(asynctask_status==AsyncTaskStatus.STARTED)
			{
				cancel(true);
			}
            else
			{
				asynctask_status=AsyncTaskStatus.STARTED;
				pbf_polling=ProgressBarFragment.newInstance();
				if(mainActivity.fm==null)
				{
					context=getContext();
					mainActivity=(MainActivity)context;
					mainActivity.fm=mainActivity.getSupportFragmentManager();
				}
				pbf_polling.show(mainActivity.fm,"");
			}


        }

        @Override
        protected Void doInBackground(Void... voids) {
            filled_filePOJOs=false;
            filled_filePOJOs=FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,fileclickselected,currentUsbFile,archive_view);
            return null;
        }


		@Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            asynctask_status=AsyncTaskStatus.COMPLETED;
            pbf_polling.dismissAllowingStateLoss();
        }
    }

	private class AsyncTaskLibrarySearch extends svl.kadatha.filex.AsyncTask<Void, Integer,Void>
	{
		final String library_or_search;
		String what_to_find=null;
		String media_category=null;
		final List<FilePOJO> path=new ArrayList<>();
		String file_type="f";
		int count=0;
		final CancelableProgressBarDialog cancelableProgressBarDialog=new CancelableProgressBarDialog();
		
		AsyncTaskLibrarySearch(String library_or_search)
		{
			this.library_or_search=library_or_search;
			cancelableProgressBarDialog.set_title(getString(R.string.searching));
			cancelableProgressBarDialog.setProgressBarCancelListener(new CancelableProgressBarDialog.ProgresBarFragmentCancelListener() {
				@Override
				public void on_cancel_progress() {
					cancel(true);
				}
			});
		}
		
		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			super.onPreExecute();
			if(asynctask_status==AsyncTaskStatus.STARTED)
			{
				cancel(true);
			}
			else
			{
				filled_filePOJOs=false;
				asynctask_status=AsyncTaskStatus.STARTED;
				if(mainActivity.fm==null)
				{
					context=getContext();
					mainActivity=(MainActivity)context;
					mainActivity.fm=mainActivity.getSupportFragmentManager();
				}
				cancelableProgressBarDialog.show(mainActivity.fm,"");

			}

		}

		@Override
		protected void onCancelled(Void result)
		{
			// TODO: Implement this method
			super.onCancelled(result);
			cancelableProgressBarDialog.dismissAllowingStateLoss();
			asynctask_status=AsyncTaskStatus.COMPLETED;
            filled_filePOJOs=true;
		}

		@Override
		protected Void doInBackground(Void[] p1)
		{
			// TODO: Implement this method

			if(totalFilePOJO_list!=null)
			{
				Iterator<FilePOJO> iterator=filePOJOS.iterator();
				while(iterator.hasNext())
				{
					if(isCancelled())
					{
						filePOJOS_filtered=filePOJOS;
						return null;
					}
					FilePOJO filePOJO=iterator.next();
					if(!new File(filePOJO.getPath()).exists())
					{
						iterator.remove();
					}
				}

				filePOJOS_filtered=filePOJOS;
				return null;
			}


			filePOJOS.clear(); filePOJOS_filtered.clear();
			if(library_or_search.equals(DetailFragment.SEARCH_RESULT))
			{
				for(FilePOJO f : SEARCH_IN_DIR)
				{
					if(f.getFileObjectType()==FileObjectType.FILE_TYPE && Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(new File(f.getPath()))))
					{
						path.add(f);
					}
				}
				if(SEARCH_REGEX)
				{
					what_to_find=SEARCH_FILE_NAME;
				}
				else if(SEARCH_WHOLE_WORD)
				{
					what_to_find="\\Q"+SEARCH_FILE_NAME+"\\E";
					if(!SEARCH_CASE_SENSITIVE)
					{
						what_to_find="(?i)\\Q"+SEARCH_FILE_NAME+"\\E";
					}
				}
				else
				{
					what_to_find=".*(\\Q"+SEARCH_FILE_NAME+"\\E).*";
					if(!SEARCH_CASE_SENSITIVE)
					{
						what_to_find=".*((?i)\\Q"+SEARCH_FILE_NAME+"\\E).*";
					}
				}
				file_type=SEARCH_FILE_TYPE;
			}
			else
			{
				for(FilePOJO f : Global.STORAGE_DIR)
				{
					if(f.getFileObjectType()==FileObjectType.FILE_TYPE && Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(new File(f.getPath()))))
					{
						path.add(f);
					}
				}
				if (getString(R.string.document).equals(library_or_search)) {
					what_to_find = ".*((?i)\\.doc|\\.docx|\\.txt|\\.pdf|\\.java|\\.xml|\\.rtf|\\.cpp|\\.c|\\.h)$";
					media_category="Documents";
				} else if (getString(R.string.image).equals(library_or_search)) {
					what_to_find = ".*((?i)\\.png|\\.jpg|\\.jpeg|\\.gif|\\.tif|\\.svg|\\.webp)$";
					media_category="Images";
				} else if (getString(R.string.audio).equals(library_or_search)) {
					what_to_find = ".*((?i)\\.mp3|\\.ogg|\\.wav|\\.aac|\\.wma|\\.opus)$";
					media_category="Audio";
				} else if (getString(R.string.video).equals(library_or_search)) {
					what_to_find = ".*((?i)\\.3gp|\\.mp4|\\.avi|\\.mov|\\.flv|\\.wmv|\\.webm)$";
					media_category="Video";
				} else if (getString(R.string.archive).equals(library_or_search)) {
					what_to_find = ".*((?i)\\.zip|\\.rar|\\.tar|\\.gz|\\.gzip)$";
				} else if (getString(R.string.apk).equals(library_or_search)) {
					what_to_find = ".*(?i)\\.apk$";
				} else if(getString(R.string.download).equals(library_or_search)){
					what_to_find=".*";
					media_category="Download";
				}

			}



			if(Global.DETAILED_SEARCH_LIBRARY)
			{
				if(media_category!=null && media_category.equals("Download"))
				{
					search_download(filePOJOS,filePOJOS_filtered);
				}
				else
				{
					for(FilePOJO f : path)
					{
						search_file(what_to_find,file_type,f.getPath(),filePOJOS,filePOJOS_filtered);
					}
				}
			}
			else
			{
				if(media_category!=null && media_category.equals("Download"))
				{
					search_download(filePOJOS,filePOJOS_filtered);
				}
				else if(library_or_search.equals(SEARCH_RESULT))
				{
					for(FilePOJO f : path)
					{
						search_file(what_to_find,file_type,f.getPath(),filePOJOS,filePOJOS_filtered);
					}
				}
				else
				{
					search_file(filePOJOS,filePOJOS_filtered);
				}
			}

			return null;
		}
		
		private void search_file(List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered)
		{
			Cursor cursor=null;
			switch (media_category)
			{
				case "Download":
					search_download(f_pojos,f_pojos_filtered);
					break;
				case "Documents":

					cursor=context.getContentResolver().query(MediaStore.Files.getContentUri("external"),new String[]{MediaStore.Files.FileColumns.DATA},
							MediaStore.Files.FileColumns.MIME_TYPE+"!=?" +" AND ("+
									MediaStore.Files.FileColumns.DISPLAY_NAME+" LIKE ?"+" OR "+
									MediaStore.Files.FileColumns.DISPLAY_NAME+" LIKE ?"+" OR "+
									MediaStore.Files.FileColumns.DISPLAY_NAME+" LIKE ?"+" OR "+
									MediaStore.Files.FileColumns.DISPLAY_NAME+" LIKE ?"+" OR "+
									MediaStore.Files.FileColumns.DISPLAY_NAME+" LIKE ?"+" OR "+
									MediaStore.Files.FileColumns.DISPLAY_NAME+" LIKE ?"+" OR "+
									MediaStore.Files.FileColumns.DISPLAY_NAME+" LIKE ?"+" OR "+
									MediaStore.Files.FileColumns.DISPLAY_NAME+" LIKE ?"+" OR "+
									MediaStore.Files.FileColumns.DISPLAY_NAME+" LIKE ?"+" OR "+
									MediaStore.Files.FileColumns.DISPLAY_NAME+" LIKE ?"+")",

							new String[]{DocumentsContract.Document.MIME_TYPE_DIR,"%.doc","%.docx","%.txt","%.pdf","%.java","%.xml","%.rtf","%.cpp","%.c","%.h"},null);

					break;
				case "Images":
					cursor=context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Images.Media.DATA},null,null,null);
					break;
				case "Audio":
					cursor=context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Media.DATA},null,null,null);
					break;
				case "Video":
					cursor=context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Video.Media.DATA},null,null,null);
					break;
			}


			if(cursor!=null && cursor.getCount()>0)
			{
				while(cursor.moveToNext())
				{
					if(isCancelled())
					{
						return;
					}
					String data=cursor.getString(0);
					File f=new File(data);
					if(f.exists())
					{
						FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(f,false,false,FileObjectType.FILE_TYPE);
						f_pojos.add(filePOJO);
						f_pojos_filtered.add(filePOJO);
						count++;
						publishProgress(count);
					}
				}
			}
			if(cursor != null) cursor.close();
		}

		private void search_download(List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered)
		{
			File f=new File("/storage/emulated/0/Download");
			if(f.exists())
			{
				File[] file_list=f.listFiles();
				int size=file_list.length;
				for(int i=0;i<size;++i)
				{
					if(isCancelled())
					{
						return;
					}

					File file=file_list[i];
					FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(file,true,false,FileObjectType.FILE_TYPE);
					f_pojos.add(filePOJO);
					f_pojos_filtered.add(filePOJO);
				}
			}

		}

		private void search_file(String search_name,String file_type, String search_dir, List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered) throws PatternSyntaxException
		{
			File[] list=new File(search_dir).listFiles();
			if(list==null) return;
			int size=list.length;
			for(int i=0;i<size;++i)
			{
				File f=list[i];
				if(isCancelled())
				{
					return;
				}
				try
				{
					if(f.isDirectory())
					{
						if(Pattern.matches(search_name,f.getName()) && (file_type.equals("d")|| file_type.equals("fd")))
						{
							FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(f,false,false,FileObjectType.FILE_TYPE);
							f_pojos.add(filePOJO);
							f_pojos_filtered.add(filePOJO);
							count++;
						}
						search_file(search_name,file_type,f.getPath(),f_pojos,f_pojos_filtered);
					}
					else
					{
						if(Pattern.matches(search_name,f.getName()) && (file_type.equals("f")||file_type.equals("fd")))
						{
							FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(f,true,false,FileObjectType.FILE_TYPE);
							f_pojos.add(filePOJO);
							f_pojos_filtered.add(filePOJO);
							count++;
						}
					}
					publishProgress(count);
				}
				catch(final PatternSyntaxException e)
				{
					mainActivity.runOnUiThread(new Runnable() {
						public void run() {
							print(e.getMessage());
						}
					});
				}

			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			mainActivity.file_number_view.setText(mselecteditems.size()+"/"+(file_list_size=values[0]));
		}

		@Override
		protected void onPostExecute(Void result)
		{
			// TODO: Implement this method
			super.onPostExecute(result);
			cancelableProgressBarDialog.dismissAllowingStateLoss();
			asynctask_status=AsyncTaskStatus.COMPLETED;
			Global.HASHMAP_FILE_POJO.put(fileObjectType+fileclickselected,filePOJOS);
			Global.HASHMAP_FILE_POJO_FILTERED.put(fileObjectType+fileclickselected,filePOJOS_filtered);
            filled_filePOJOs=true;
		}
	}
	
	private class FilePathRecyclerViewAdapter extends RecyclerView.Adapter<FilePathRecyclerViewAdapter.ViewHolder>
	{
		final String[] filepath_string_array;
		String display_path;
		String truncated_path;

		FilePathRecyclerViewAdapter(String p)
		{
			truncated_path=p;
			display_path=p;
			if(archive_view)
			{
				display_path=display_path.substring(Global.ARCHIVE_CACHE_DIR_LENGTH);
				display_path="Archive"+display_path;
				truncated_path=truncated_path.substring(0,Global.ARCHIVE_CACHE_DIR_LENGTH-"Archive/".length());//number added to archive_cache_dir_length is length of "Archive/"
			}

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
						if(archive_view)
						{
							file_path = new StringBuilder(truncated_path);
							for(int i=0; i<=p2 ; ++i)
							{
								file_path.append(File.separator).append(filepath_string_array[i]);
							}

						}
						else
						{
							file_path = new StringBuilder();
							for(int i=1; i<=p2 ; ++i)
							{
								file_path.append(File.separator).append(filepath_string_array[i]);
							}
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


	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}
	
}

