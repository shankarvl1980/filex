package svl.kadatha.filex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.github.mjdev.libaums.fs.UsbFile;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class VideoViewContainerFragment extends Fragment
{
	private Context context;

	private int file_selected_idx=0;
	private Toolbar toolbar;
	private TextView title;
	//private ListPopupWindow listPopWindow;
	private PopupWindow listPopWindow;
	private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
	private List<FilePOJO> files_selected_for_delete,deleted_files;
	private DeleteFileAsyncTask delete_file_async_task;
	private String tree_uri_path="";
	private Uri tree_uri;
	private final int request_code=904;
	private Handler handler;
	private Runnable runnable;
	private boolean is_menu_opened;

	private boolean asynctask_running;
	private IndexedLinkedHashMap<FilePOJO,Integer> video_list;
	private Uri data;
	private FilePOJO currently_shown_file;
	private VideoViewPagerAdapter adapter;
	private boolean firststart;
	private int floating_button_height;
	private FloatingActionButton floating_back_button;
	private OnPageSelectListener onPageSelectListener;
	private ToolBarVisibleListener toolBarVisibleListener;
	private boolean toolbar_visible,fromArchiveView;
	private AlbumPollCompleteListener albumPollCompleteListener;
	private FileObjectType fileObjectType;
	private boolean fromThirdPartyApp;
	private String source_folder;
	private LocalBroadcastManager localBroadcastManager;
	private VideoViewActivity videoViewActivity;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		localBroadcastManager=LocalBroadcastManager.getInstance(context);
		videoViewActivity=((VideoViewActivity)context);
	}


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		firststart=true;

		data=videoViewActivity.data;
		currently_shown_file=videoViewActivity.currently_shown_file;
		video_list=videoViewActivity.video_list;
		file_selected_idx=videoViewActivity.file_selected_idx;
		fromThirdPartyApp=videoViewActivity.fromThirdPartyApp;
		source_folder=videoViewActivity.source_folder;
		source_folder=videoViewActivity.source_folder;
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			fromArchiveView = bundle.getBoolean(FileIntentDispatch.EXTRA_FROM_ARCHIVE);
			fileObjectType= (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
		}


		if(albumPollCompleteListener!=null) albumPollCompleteListener.onPollComplete();
		list_popupwindowpojos=new ArrayList<>();
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon,getString(R.string.delete)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon,getString(R.string.send)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties)));
		floating_button_height=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,146,context.getResources().getDisplayMetrics());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v;
		v=inflater.inflate(R.layout.fragment_video_view_container,container,false);
		toolbar_visible=true;
		handler=new Handler();
		ViewPager viewpager = v.findViewById(R.id.activity_video_view_viewpager);
		toolbar=v.findViewById(R.id.activity_video_toolbar);
		title=v.findViewById(R.id.activity_video_name);
		ImageView overflow = v.findViewById(R.id.activity_video_overflow);
		overflow.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					is_menu_opened=true;
					listPopWindow.showAsDropDown(v,0,Global.SIX_DP);
				}
			});


		listPopWindow=new PopupWindow(context);
		ListView listView=new ListView(context);
		listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapater(context,list_popupwindowpojos));
		listPopWindow.setContentView(listView);
		listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popupwindow_width));
		listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		listPopWindow.setFocusable(true);
		listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.list_popup_background));
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
			{
				public void onItemClick(AdapterView<?> adapterview, View v, int p1,long p2)
				{
					final Bundle bundle=new Bundle();
					final ArrayList<String> files_selected_array=new ArrayList<>();

					switch(p1)
					{
						case 0:
							if(fromArchiveView || fromThirdPartyApp)
							{
								print(getString(R.string.not_able_to_process));
								break;
							}
							files_selected_array.add(currently_shown_file.getPath());
							DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity=DeleteFileAlertDialogOtherActivity.getInstance(files_selected_array,fileObjectType);
							deleteFileAlertDialogOtherActivity.setDeleteFileDialogListener(new DeleteFileAlertDialogOtherActivity.DeleteFileAlertDialogListener()
								{
									public void onSelectOK()
									{
										if(!asynctask_running)
										{
											asynctask_running=true;
											files_selected_for_delete=new ArrayList<>();
											deleted_files=new ArrayList<>();
											files_selected_for_delete.add(currently_shown_file);
											delete_file_async_task=new DeleteFileAsyncTask(files_selected_for_delete,fileObjectType);
											delete_file_async_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
										}

									}
								});
							deleteFileAlertDialogOtherActivity.show(((VideoViewActivity)context).fm,"deletefilealertotheractivity");
							break;
						case 1:
							Uri src_uri=null;
							if(fromThirdPartyApp)
							{
								src_uri=data;

							}
							else if(fileObjectType==FileObjectType.FILE_TYPE)
							{
								src_uri= FileProvider.getUriForFile(context, context.getPackageName()+".provider",new File(currently_shown_file.getPath()));
							}
							if(src_uri==null)
							{
								print(getString(R.string.not_able_to_process));
								break;
							}
							ArrayList<Uri> uri_list=new ArrayList<>();
							uri_list.add(src_uri);
							FileIntentDispatch.sendUri(context,uri_list);

							break;
						case 2:
							if(fromThirdPartyApp)
							{
								print(getString(R.string.not_able_to_process));
								break;
							}
							files_selected_array.add(currently_shown_file.getPath());
							PropertiesDialog propertiesDialog=PropertiesDialog.getInstance(files_selected_array,fileObjectType);
							propertiesDialog.show(((VideoViewActivity)context).fm,"properties_dialog");
							break;

						default:
							break;
					}
					listPopWindow.dismiss();
				}


			});
		listPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener()
		{
			public void onDismiss()
			{
				is_menu_opened=false;
				handler.removeCallbacks(runnable);
				handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
			}

		});
		
		floating_back_button=v.findViewById(R.id.floating_button_video_fragment);
		floating_back_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View p)
			{
				((VideoViewActivity)context).onBackPressed();
			}
			
		});

		adapter=new VideoViewPagerAdapter(((VideoViewActivity)context).fm,video_list);
		viewpager.setAdapter(adapter);
		viewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
			{

				public void onPageSelected(int p)
				{
					if(onPageSelectListener!=null)
					{
						onPageSelectListener.onPageSelect(p);
					}
					((VideoViewActivity)context).current_page_idx=p;
				}

				public void onPageScrollStateChanged(int p)
				{

				}

				public void onPageScrolled(int p1, float p2, int p3)
				{

					file_selected_idx=p1;
					currently_shown_file=video_list.getKeyAtIndex(p1);
					title.setText(currently_shown_file.getName());

				}
			});

		runnable=new Runnable()
		{
			public void run()
			{
				if(!is_menu_opened)
				{
					toolbar.animate().translationY(-Global.ACTION_BAR_HEIGHT).setInterpolator(new DecelerateInterpolator(1));
					floating_back_button.animate().translationY(floating_button_height).setInterpolator(new DecelerateInterpolator(1));
					//frag.toolbar.animate().translationY(toolbar.getHeight()).setInterpolator(new DecelerateInterpolator(1));
					toolbar_visible=false;
					if(toolBarVisibleListener!=null)
					{
						toolBarVisibleListener.onToolbarVisible(toolbar_visible);
					}
				}
			}
		};

		handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
		viewpager.setCurrentItem(file_selected_idx);
		if(file_selected_idx==0)
		{
			((VideoViewActivity)context).current_page_idx=0;
		}
		return v;
	}


	public static VideoViewContainerFragment getNewInstance(FileObjectType fileObjectType,boolean fromArchiveView)
	{
		VideoViewContainerFragment frag=new VideoViewContainerFragment();
		Bundle bundle=new Bundle();
		bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE,fileObjectType);
		bundle.putBoolean("fromArchiveView",fromArchiveView);
		frag.setArguments(bundle);
		return frag;
	}
	
	public void seekSAFPermission()
	{
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		startActivityForResult(intent, request_code);
	}


	// @TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) 
	{
		super.onActivityResult(requestCode,resultCode,resultData);
		if (requestCode == this.request_code && resultCode== Activity.RESULT_OK)
		{
			Uri treeUri;
			// Get Uri from Storage Access Framework.
			treeUri = resultData.getData();
			Global.ON_REQUEST_URI_PERMISSION(context,treeUri);

			boolean permission_requested = false;
			delete_file_async_task=new DeleteFileAsyncTask(files_selected_for_delete,fileObjectType);
			delete_file_async_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
			safpermissionhelper.show(((VideoViewActivity)context).fm, "saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
	}


	private class VideoViewPagerAdapter extends FragmentStatePagerAdapter
	{
		final IndexedLinkedHashMap<FilePOJO,Integer> list;
		VideoViewPagerAdapter(FragmentManager fm, IndexedLinkedHashMap<FilePOJO,Integer> l)
		{
			super(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			this.list=l;
			title.setText(currently_shown_file.getName());
		}

		@Override
		public Fragment getItem(int p1)
		{
			// TODO: Implement this method

			final VideoViewFragment frag;
			boolean b=firststart;
			FilePOJO filePOJO=video_list.getKeyAtIndex(p1);
			final String file_path=filePOJO.getPath();
			int position=video_list.get(filePOJO);
			frag=VideoViewFragment.getNewInstance(fileObjectType,fromThirdPartyApp,file_path,position,p1,b);
			firststart=false;

			frag.setVideoViewClickListener(new VideoViewFragment.VideoViewClickListener()
				{
					public void onVideoViewClick()
					{
						//if(toolbar.getGlobalVisibleRect(new Rect()))
						if(toolbar_visible)
						{
							//disappear
							toolbar.animate().translationY(-Global.ACTION_BAR_HEIGHT).setInterpolator(new DecelerateInterpolator(1));
							floating_back_button.animate().translationY(floating_button_height).setInterpolator(new DecelerateInterpolator(1));
							//frag.toolbar.animate().translationY(frag.toolbar.getHeight()).setInterpolator(new DecelerateInterpolator(1));
							toolbar_visible=false;
							handler.removeCallbacks(runnable);
						}
						else
						{
							//appear
							toolbar.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1)); 
							floating_back_button.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));
							//frag.toolbar.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));
							toolbar_visible=true;
							handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
						}
						if(toolBarVisibleListener!=null)
						{
							toolBarVisibleListener.onToolbarVisible(toolbar_visible);
						}
					}
				});
			frag.setVideoPositionListener(new VideoViewFragment.VideoPositionListener()
			{
				public void setPosition(Integer idx, Integer position)
				{
		
					if(video_list.size()>idx) // condition is required, otherwise app crashes, when last video is removed
					{

						video_list.put(video_list.getKeyAtIndex(idx),position);

					}
				}
			});
			return frag;
		}

		

		@Override
		public int getCount()
		{
			// TODO: Implement this method
			return list.size();
		}

		@Override
		public int getItemPosition(Object object)
		{
			// TODO: Implement this method
			return POSITION_NONE;
		}

	}

	interface AlbumPollCompleteListener
	{
		void onPollComplete();
	}

	public void setAlbumPollCompleteListener(AlbumPollCompleteListener listener)
	{
		albumPollCompleteListener=listener;
	}

	interface OnPageSelectListener
	{
		void onPageSelect(int x);
	}

	public void setOnPageSelectListener(OnPageSelectListener listener)
	{
		onPageSelectListener=listener;
	}

	interface ToolBarVisibleListener
	{
		void onToolbarVisible(boolean visible);
	}

	public void setToolBarVisibleListener(ToolBarVisibleListener listener)
	{
		toolBarVisibleListener=listener;
	}

	private class DeleteFileAsyncTask extends svl.kadatha.filex.AsyncTask<Void,String,Boolean>
	{

		final List<FilePOJO> src_file_list;
		final List<String> deleted_file_name_list=new ArrayList<>();

		int counter_no_files;
		long counter_size_files;
		String current_file_name;
		boolean isFromInternal;
		String size_of_files_format;
		final ProgressBarFragment pbf=ProgressBarFragment.getInstance();
		final FileObjectType fileObjectType;
		DeleteFileAsyncTask(List<FilePOJO> src_file_list, FileObjectType fileObjectType)
		{
			this.src_file_list=src_file_list;
			this.fileObjectType=fileObjectType;
		}

		@Override
		protected void onPreExecute()
		{
			// TODO: Implement this method
			pbf.show(((VideoViewActivity)context).fm,"progressbar_dialog");

		}

		@Override
		protected void onCancelled(Boolean result)
		{
			// TODO: Implement this method
			super.onCancelled(result);

			if(deleted_files.size()>0)
			{

				Iterator<Map.Entry<FilePOJO, Integer>> iterator=video_list.entrySet().iterator();
				for(FilePOJO filePOJO:deleted_files)
				{
					while(iterator.hasNext())
					{
						Map.Entry<FilePOJO,Integer> entry=iterator.next();
						if(entry.getKey().getPath().equals(filePOJO.getPath()) && entry.getKey().getFileObjectType()==filePOJO.getFileObjectType())
						{
							video_list.removeIndex(filePOJO);
							iterator.remove();
							break;
						}
					}

				}

				adapter.notifyDataSetChanged();
				FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_name_list,fileObjectType);
				Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,VideoViewActivity.ACTIVITY_NAME);
				if(video_list.size()<1)
				{
					((VideoViewActivity)context).finish();
				}

			}

			pbf.dismissAllowingStateLoss();
			asynctask_running=false;
		}

		@Override
		protected Boolean doInBackground(Void...p)
		{
			// TODO: Implement this method
			boolean success;

			if(fileObjectType==FileObjectType.FILE_TYPE)
			{
				isFromInternal=FileUtil.isFromInternal(fileObjectType,src_file_list.get(0).getPath());
			}
			success=deleteFromFolder();
			return success;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			// TODO: Implement this method

			super.onPostExecute(result);
			if(deleted_files.size()>0)
			{
				Iterator<Map.Entry<FilePOJO, Integer>> iterator=video_list.entrySet().iterator();
				for(FilePOJO filePOJO:deleted_files)
				{
					while(iterator.hasNext())
					{
						Map.Entry<FilePOJO,Integer> entry=iterator.next();
						if(entry.getKey().getPath().equals(filePOJO.getPath()) && entry.getKey().getFileObjectType()==filePOJO.getFileObjectType())
						{
							video_list.removeIndex(filePOJO);
							iterator.remove();
							break;
						}
					}

				}

				adapter.notifyDataSetChanged();
				FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_name_list,fileObjectType);
				Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,VideoViewActivity.ACTIVITY_NAME);
				if(video_list.size()<1)
				{
					((VideoViewActivity)context).finish();
				}

			}
			pbf.dismissAllowingStateLoss();
			asynctask_running=false;

		}

		private boolean deleteFromFolder()
		{
			boolean success=false;
			int size=src_file_list.size();
			if(fileObjectType==FileObjectType.FILE_TYPE)
			{
				if(isFromInternal)
				{
					for(int i=0;i<size;++i)
					{
						if(isCancelled())
						{
							return false;
						}
						FilePOJO filePOJO=src_file_list.get(i);
						File f=new File(filePOJO.getPath());
						current_file_name=f.getName();
						success=FileUtil.deleteNativeDirectory(f);
						if(success)
						{
							deleted_files.add(filePOJO);
							deleted_file_name_list.add(current_file_name);
						}
						files_selected_for_delete.remove(filePOJO);
					}

				}
				else
				{
					if(check_SAF_permission(src_file_list.get(0).getPath(),fileObjectType))
					{
						for(int i=0;i<size;++i)
						{
							if(isCancelled())
							{
								return false;
							}
							FilePOJO filePOJO=src_file_list.get(i);
							File file=new File(filePOJO.getPath());
							current_file_name=file.getName();
							success=FileUtil.deleteSAFDirectory(context,file.getAbsolutePath(),tree_uri,tree_uri_path);
							if(success)
							{
								deleted_files.add(filePOJO);
								deleted_file_name_list.add(current_file_name);
							}
							files_selected_for_delete.remove(filePOJO);
						}
					}


				}
			}
			else if(fileObjectType==FileObjectType.USB_TYPE)
			{
				for(int i=0;i<size;++i)
				{
					if(isCancelled())
					{
						return false;
					}
					FilePOJO filePOJO=src_file_list.get(i);
					UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,filePOJO.getPath());
					current_file_name=f.getName();
					success=FileUtil.deleteUsbDirectory(f);
					if(success)
					{
						deleted_files.add(filePOJO);
						deleted_file_name_list.add(current_file_name);
					}
					files_selected_for_delete.remove(filePOJO);
				}
			}

			return success;
		}

	}


	private void print(String msg)
	{
		Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
	}
}
