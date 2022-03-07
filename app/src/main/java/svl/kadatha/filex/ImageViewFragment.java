package svl.kadatha.filex;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.mjdev.libaums.fs.UsbFile;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ImageViewFragment extends Fragment
{

	private ViewPager view_pager;
	private Context context;
	private ImageViewPagerAdapter image_view_adapter;
	private int file_selected_idx=0;
    private Toolbar toolbar;
	private LinearLayoutManager lm;
	private PictureSelectorAdapter picture_selector_adapter;
	private final List<FilePOJO> album_file_pojo_list=new ArrayList<>();
	private SparseBooleanArray selected_item_sparseboolean;
	private int preview_image_offset;
	private Handler handler,polling_handler;
	private Runnable runnable;
	private boolean is_menu_opened;
	private PopupWindow listPopWindow;
	private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
	private TextView title;
	private FilePOJO currently_shown_file;
	private List<FilePOJO> files_selected_for_delete;
	private List<FilePOJO> deleted_files;
	private String tree_uri_path="";
	private Uri tree_uri;
	private final int saf_request_code=234;
	private final int crop_request_code=890;
	private DeleteFileAsyncTask delete_file_async_task;
	private boolean asynctask_running;
	private Uri data;
	private int floating_button_height;
	private int recyclerview_height;
	private FloatingActionButton floating_back_button;
	private boolean toolbar_visible;
	private LinearLayout image_view_selector_butt;
	private TextView current_image_tv;
	private int total_images;
	private boolean fromArchiveView;
	private AsyncTaskStatus asyncTaskStatus=AsyncTaskStatus.NOT_YET_STARTED;
	private FileObjectType fileObjectType;
	private String source_folder,file_path;
	private boolean fromThirdPartyApp;
	private LocalBroadcastManager localBroadcastManager;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		localBroadcastManager=LocalBroadcastManager.getInstance(context);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		FilenameFilter file_name_filter = new FilenameFilter() {
			public boolean accept(File fi, String na) {
				if (MainActivity.SHOW_HIDDEN_FILE) {
					return true;
				} else {
					return !na.startsWith(".");
				}
			}
		};

		data=((ImageViewActivity)context).data;

		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			file_path = bundle.getString("file_path");
			fromArchiveView = bundle.getBoolean(FileIntentDispatch.EXTRA_FROM_ARCHIVE);
			fileObjectType= (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
		}

		if(fileObjectType==null || fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
		{
			fileObjectType=FileObjectType.FILE_TYPE;
			fromThirdPartyApp=true;
		}

		source_folder=new File(file_path).getParent();
		if(fileObjectType==FileObjectType.USB_TYPE)
		{
			if(MainActivity.usbFileRoot!=null)
			{
				try {
					currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(file_path)),false);

				} catch (IOException e) {

				}
			}
		}
		else
		{
			currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,false,fileObjectType);
		}
		new AlbumPollingAsyncTask(album_file_pojo_list,Global.IMAGE_REGEX,source_folder,fileObjectType,fromArchiveView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		list_popupwindowpojos=new ArrayList<>();
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon,getString(R.string.delete)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon,getString(R.string.send)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon,getString(R.string.properties)));
		list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.wallpaper_icon,getString(R.string.set_as_wallpaper)));
		DisplayMetrics displayMetrics=context.getResources().getDisplayMetrics();
		floating_button_height=(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,146,displayMetrics);
		recyclerview_height= (int) getResources().getDimension(R.dimen.image_preview_dimen)+((int)+getResources().getDimension(R.dimen.layout_margin)*2);

	}
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// TODO: Implement this method
		View v=inflater.inflate(R.layout.fragment_image_view,container,false);
		toolbar_visible=true;
		handler=new Handler();
		polling_handler=new Handler();
		toolbar=v.findViewById(R.id.activity_picture_toolbar);
		title=v.findViewById(R.id.activity_picture_name);
		ImageView overflow = v.findViewById(R.id.activity_picture_overflow);
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
							deleteFileAlertDialogOtherActivity.show(((ImageViewActivity)context).fm,"deletefilealertotheractivity");
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
							propertiesDialog.show(((ImageViewActivity)context).fm,"properties_dialog");
							break;
							
						case 3:
							Uri uri=null;
							if(fromThirdPartyApp)
							{
								uri=data;

							}
							else if(fileObjectType==FileObjectType.FILE_TYPE)
							{
								uri=Uri.fromFile(new File(currently_shown_file.getPath()));
							}
							if(uri==null)
							{
								print(getString(R.string.not_able_to_process));
								break;
							}
							float aspect_ratio=image_view_adapter.getAspectRatio();
							File tempFile=new File(((ImageViewActivity)getContext()).CacheDir,currently_shown_file.getName());
							Intent intent=InstaCropperActivity.getIntent(context,uri,Uri.fromFile(tempFile),currently_shown_file.getName(),Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT,100);
							//startActivityForResult(intent,crop_request_code);
							activityResultLauncher_crop_request.launch(intent);
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
		view_pager=v.findViewById(R.id.activity_picture_view_viewpager);
		floating_back_button=v.findViewById(R.id.floating_button_picture_fragment);
		floating_back_button.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					((ImageViewActivity)context).onBackPressed();
				}

			});

		image_view_selector_butt=v.findViewById(R.id.image_view_selector_recyclerview_group);
		current_image_tv=v.findViewById(R.id.image_view_current_view);

        final RecyclerView recyclerview = v.findViewById(R.id.activity_picture_view_recyclerview);
		recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener()
			{
				public void onScrolled(RecyclerView rv, int dx,int dy)
				{
					handler.removeCallbacks(runnable);
					handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
				}
			});
		int recyclerview_image_width = (int) getResources().getDimension(R.dimen.image_preview_dimen);
		if(Global.ORIENTATION== Configuration.ORIENTATION_LANDSCAPE)
		{
			recyclerview.setPadding(Global.SCREEN_HEIGHT/2- recyclerview_image_width /2,0,Global.SCREEN_HEIGHT/2- recyclerview_image_width /2,0);
		}
		else
		{
			recyclerview.setPadding(Global.SCREEN_WIDTH/2- recyclerview_image_width /2,0,Global.SCREEN_WIDTH/2- recyclerview_image_width /2,0);
		}

		runnable=new Runnable()
		{
			public void run()
			{
				if(!is_menu_opened)
				{
					toolbar.animate().translationY(-Global.ACTION_BAR_HEIGHT).setInterpolator(new DecelerateInterpolator(1));
					floating_back_button.animate().translationY(floating_button_height).setInterpolator(new DecelerateInterpolator(1));
					image_view_selector_butt.animate().translationY(recyclerview_height).setInterpolator(new DecelerateInterpolator(1));

					//toolbar.setVisibility(View.GONE);
					//recyclerview.setVisibility(View.GONE);
					//floating_back_button.setVisibility(View.GONE);
					toolbar_visible=false;
				}

			}
		};
		final ProgressBarFragment pbf=ProgressBarFragment.getInstance();
		pbf.show(getActivity().getSupportFragmentManager(),"");
		polling_handler.post(new Runnable() {
			@Override
			public void run() {
				if(asyncTaskStatus!=AsyncTaskStatus.COMPLETED)
				{
					polling_handler.postDelayed(this,100);
				}
				else
				{
					preview_image_offset=(int)getResources().getDimension(R.dimen.layout_margin);
					selected_item_sparseboolean=new SparseBooleanArray();
					image_view_adapter=new ImageViewPagerAdapter(album_file_pojo_list);
					view_pager.setAdapter(image_view_adapter);
					view_pager.setCurrentItem(file_selected_idx);
					current_image_tv.setText(file_selected_idx+1+"/"+total_images);
					selected_item_sparseboolean.put(file_selected_idx,true);
					picture_selector_adapter=new PictureSelectorAdapter(album_file_pojo_list);
					lm=new LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false);
					recyclerview.setLayoutManager(lm);
					recyclerview.setAdapter(picture_selector_adapter);
					lm.scrollToPositionWithOffset(file_selected_idx,-preview_image_offset);
					view_pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
					{
						public void onPageSelected(int i)
						{
							lm.scrollToPositionWithOffset(i,-preview_image_offset);
							selected_item_sparseboolean=new SparseBooleanArray();
							selected_item_sparseboolean.put(i,true);
							picture_selector_adapter.notifyDataSetChanged();
						}

						public void onPageScrollStateChanged(int i)
						{

						}

						public void onPageScrolled(int i,float p2, int p3)
						{
							file_selected_idx=i;
							current_image_tv.setText(file_selected_idx+1+"/"+total_images);
							currently_shown_file=album_file_pojo_list.get(i);
							title.setText(currently_shown_file.getName());
						}
					});
					pbf.dismissAllowingStateLoss();
					handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
					polling_handler.removeCallbacks(this);
				}

			}
		});



		return v;
	}
	
	
	public static ImageViewFragment getNewInstance(String file_path, boolean fromArchiveView, FileObjectType fileObjectType)
	{
		ImageViewFragment frag=new ImageViewFragment();
		Bundle bundle=new Bundle();
		bundle.putString("file_path",file_path);
		bundle.putBoolean("fromArchiveView",fromArchiveView);
		bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE,fileObjectType);
		frag.setArguments(bundle);
		return frag;
	}

	 public void seekSAFPermission()
	 {
	 	Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
	 	//startActivityForResult(intent, saf_request_code);
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

				 boolean permission_requested = false;
				 delete_file_async_task=new DeleteFileAsyncTask(files_selected_for_delete,fileObjectType);
				 delete_file_async_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

			 }
			 else
			 {
				 print(getString(R.string.permission_not_granted));
			 }
		 }
	 });

	private final ActivityResultLauncher<Intent>activityResultLauncher_crop_request=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
		@RequiresApi(api = Build.VERSION_CODES.N)
		@Override
		public void onActivityResult(ActivityResult result) {
			if(result.getResultCode()== Activity.RESULT_OK)
			{
				ProgressBarFragment pbf = ProgressBarFragment.getInstance();
				pbf.show(((ImageViewActivity)context).fm,"");
				Uri uri=result.getData().getData();
				String file_name=result.getData().getStringExtra(InstaCropperActivity.EXTRA_FILE_NAME);
				File f=new File(((ImageViewActivity)context).CacheDir,file_name);
				WallpaperManager wm= WallpaperManager.getInstance(context);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					if(wm.isWallpaperSupported() && wm.isSetWallpaperAllowed())
						try
						{
							wm.setStream(context.getContentResolver().openInputStream(uri));
							print(getString(R.string.set_as_wallpaper));
						}
						catch(IOException e){}
						finally
						{
							if(f.exists())
							{
								f.delete();
							}
						}
					else
					{

						if(f.exists())
						{
							f.delete();
						}

					}
				}
				pbf.dismissAllowingStateLoss();
			}
			else
			{
				print(getString(R.string.could_not_be_set_as_wallpaper));
			}
		}
	});

	/*
	 @RequiresApi(api = Build.VERSION_CODES.N)
	 @Override
	 public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) 
	 {
	 	super.onActivityResult(requestCode,resultCode,resultData);
		switch(requestCode)
		{
			
			case saf_request_code:
				 if (resultCode== Activity.RESULT_OK)
				 {
					 Uri treeUri;
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
				break;
			case crop_request_code:
				 if(resultCode== Activity.RESULT_OK)
				 {
                     ProgressBarFragment pbf = ProgressBarFragment.getInstance();
					 pbf.show(((ImageViewActivity)context).fm,"");
					 Uri uri=resultData.getData();
					 String file_name=resultData.getStringExtra(InstaCropperActivity.EXTRA_FILE_NAME);
					 File f=new File(((ImageViewActivity)context).CacheDir,file_name);
					 WallpaperManager wm= WallpaperManager.getInstance(context);

					 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						 if(wm.isWallpaperSupported() && wm.isSetWallpaperAllowed())
							 try
							 {
								 wm.setStream(context.getContentResolver().openInputStream(uri));
								 print(getString(R.string.set_as_wallpaper));
							 }
							 catch(IOException e){}
							 finally
							 {
								 if(f.exists())
								 {
									 f.delete();
								 }
							 }
						 else
						 {

							 if(f.exists())
							 {
								 f.delete();
							 }

						 }
					 }
					 pbf.dismissAllowingStateLoss();
				 }
				 else
				 {
					 print(getString(R.string.could_not_be_set_as_wallpaper));
				 }
				break;

		}
	 }

	 */

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
			safpermissionhelper.show(((ImageViewActivity)context).fm, "saf_permission_dialog");
			return false;
		}
		else
		{
			return true;
		}
	}


	private class ImageViewPagerAdapter extends PagerAdapter
	{
		final List<FilePOJO> albumList;
		TouchImageView image_view;
		FilePOJO f;
		ImageViewPagerAdapter(List<FilePOJO> albumList)
		{
			this.albumList=albumList;
			title.setText(currently_shown_file.getName());
		}

		@Override
		public int getCount()
		{
			// TODO: Implement this method
			return albumList.size();

		}

		@Override
		public boolean isViewFromObject(View p1, Object p2)
		{
			// TODO: Implement this method
			return p1.equals(p2);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position)
		{
			// TODO: Implement this method


			View v=LayoutInflater.from(context).inflate(R.layout.image_viewpager_layout,container,false);
			image_view=v.findViewById(R.id.picture_viewpager_layout_imageview);
			image_view.setMaxZoom(6);
			image_view.setOnClickListener(new View.OnClickListener()
				{
					public void onClick(View v)
					{
						//if(toolbar.getGlobalVisibleRect(new Rect()))
						if(toolbar_visible)
						{
							//disappear
							toolbar.animate().translationY(-Global.ACTION_BAR_HEIGHT).setInterpolator(new DecelerateInterpolator(1));
							floating_back_button.animate().translationY(floating_button_height).setInterpolator(new DecelerateInterpolator(1));
							image_view_selector_butt.animate().translationY(recyclerview_height).setInterpolator(new DecelerateInterpolator(1));


							//toolbar.setVisibility(View.GONE);
							//recyclerview.setVisibility(View.GONE);
							//floating_back_button.setVisibility(View.GONE);
							is_menu_opened=false;
							toolbar_visible=false;
							handler.removeCallbacks(runnable);

						}
						else
						{
							//appear
							toolbar.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));
							floating_back_button.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));
							image_view_selector_butt.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));

							//toolbar.setVisibility(View.VISIBLE);
							//recyclerview.setVisibility(View.VISIBLE);
							//floating_back_button.setVisibility(View.VISIBLE);
							toolbar_visible=true;
							handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
						}

					}
				});

			f=albumList.get(position);
			if(fromThirdPartyApp)
			{
				GlideApp.with(context).load(data).placeholder(R.drawable.picture_icon).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(image_view);
			}
			else if(f.getFileObjectType()==FileObjectType.FILE_TYPE)
			{
				GlideApp.with(context).load(new File(f.getPath())).placeholder(R.drawable.picture_icon).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(image_view);
			}
			else if(f.getFileObjectType()==FileObjectType.USB_TYPE)
			{
				GlideApp.with(context).load(data).placeholder(R.drawable.picture_icon).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(image_view);
			}

			container.addView(v);
			return v;
		}

		@Override
		public int getItemPosition(Object object)
		{
			// TODO: Implement this method
			return POSITION_NONE;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object)
		{
			// TODO: Implement this method
			container.removeView((View)object);
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			// TODO: Implement this method
			return albumList.get(position).getName();
		}

		public float getAspectRatio()
		{
			BitmapFactory.Options options=new BitmapFactory.Options();
			options.inJustDecodeBounds=true;
			int width=options.outWidth;
			int height=options.outHeight;
			return (float) (width/height);
		}
	}


	private class PictureSelectorAdapter extends RecyclerView.Adapter<PictureSelectorAdapter.VH>
	{
		final List<FilePOJO> picture_list;
		PictureSelectorAdapter(List<FilePOJO>list)
		{
			picture_list=list;
		}

		@Override
		public PictureSelectorAdapter.VH onCreateViewHolder(ViewGroup parent, int p2)
		{
			// TODO: Implement this method
			View v=LayoutInflater.from(context).inflate(R.layout.image_selector_recyclerview_layout,parent,false);
			return new VH(v);
		}

		@Override
		public void onBindViewHolder(PictureSelectorAdapter.VH p1, int p2)
		{
			// TODO: Implement this method
			FilePOJO f=picture_list.get(p2);
			if(fromThirdPartyApp)
			{
				GlideApp.with(context).load(data).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(p1.imageview);
			}
			else if(f.getFileObjectType()==FileObjectType.FILE_TYPE)
			{
				GlideApp.with(context).load(new File(f.getPath())).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(p1.imageview);
			}
			else if(f.getFileObjectType()==FileObjectType.USB_TYPE)
			{
				GlideApp.with(context).load(data).error(R.drawable.picture_icon).diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate().into(p1.imageview);
			}

			p1.v.setSelected(selected_item_sparseboolean.get(p2,false));

		}

		@Override
		public int getItemCount()
		{
			// TODO: Implement this method
			return picture_list.size();
		}

		private class VH extends RecyclerView.ViewHolder implements View.OnClickListener
		{
			final View v;
			final ImageView imageview;
			VH(View view)
			{
				super(view);
				v=view;
				imageview=v.findViewById(R.id.picture_viewpager_layout_imageview);
				v.setOnClickListener(this);
			}

			@Override
			public void onClick(View p1)
			{
				// TODO: Implement this method
				view_pager.setCurrentItem(getBindingAdapterPosition());
			}
		}
	}

	private class AlbumPollingAsyncTask extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
	{
		final List<FilePOJO> album_list;
		final FileObjectType fileObjectType;
		final String source_folder;
		final boolean fromArchiveView;
		final String regex;

		AlbumPollingAsyncTask(List<FilePOJO> album_list, String regex ,String source_folder ,FileObjectType fileObjectType,boolean fromArchiveView)
		{
			this.album_list=album_list;
			this.regex=regex;
			this.source_folder=source_folder;
			this.fileObjectType=fileObjectType;
			this.fromArchiveView=fromArchiveView;
		}
		@Override
		protected void onPreExecute() {
			asyncTaskStatus=AsyncTaskStatus.STARTED;
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... voids) {
			List<FilePOJO> filePOJOS=new ArrayList<>(), filePOJOS_filtered=new ArrayList<>();
			if (!Global.HASHMAP_FILE_POJO.containsKey(fileObjectType+source_folder))
			{
				FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,source_folder,null,false);
			}
			else
			{
				if(MainActivity.SHOW_HIDDEN_FILE)
				{
					filePOJOS=Global.HASHMAP_FILE_POJO.get(fileObjectType+source_folder) ;
				}
				else
				{
					filePOJOS=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+source_folder);
				}
			}

			// limiting to the selected only, in case of file selected from usb storage by adding condition below
			if(fromArchiveView || fromThirdPartyApp || fileObjectType==FileObjectType.USB_TYPE)
			{
				album_list.add(currently_shown_file);
			}
			else
			{
				if(Global.SORT==null)
				{
					Global.GET_PREFERENCES(new TinyDB(getContext()));
				}
				Collections.sort(filePOJOS,FileComparator.FilePOJOComparate(Global.SORT,false));
				int size=filePOJOS.size();
				int count=0;
				for(int i=0; i<size;++i)
				{
					FilePOJO filePOJO=filePOJOS.get(i);
					if(!filePOJO.getIsDirectory())
					{
						String file_ext;
						int idx=filePOJO.getName().lastIndexOf(".");
						if(idx!=-1)
						{
							file_ext=filePOJO.getName().substring(idx+1);
							if(file_ext.matches(regex))
							{

								album_list.add(filePOJO);
								if(filePOJO.getName().equals(currently_shown_file.getName())) file_selected_idx=count;
								count++;

							}
							else if(filePOJO.getName().equals(currently_shown_file.getName()))
							{
								album_list.add(currently_shown_file);
								file_selected_idx=count;
								count++;
							}

						}
						else if(filePOJO.getName().equals(currently_shown_file.getName()))
						{
							album_list.add(currently_shown_file);
							file_selected_idx=count;
							count++;
						}

					}
				}

			}
			total_images=album_list.size();
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			asyncTaskStatus=AsyncTaskStatus.COMPLETED;
		}
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
			pbf.show(((ImageViewActivity)context).fm,"progressbar_dialog");

		}

		@Override
		protected void onCancelled(Boolean result)
		{
			// TODO: Implement this method
			super.onCancelled(result);

			if(deleted_files.size()>0)
			{
				album_file_pojo_list.removeAll(deleted_files);
				total_images=album_file_pojo_list.size();
				image_view_adapter.notifyDataSetChanged();
				picture_selector_adapter.notifyDataSetChanged();
				FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_name_list,fileObjectType);
				Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,ImageViewActivity.ACTIVITY_NAME);
				if(album_file_pojo_list.size()<1)
				{
					((ImageViewActivity)context).finish();
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
				album_file_pojo_list.removeAll(deleted_files);
				total_images=album_file_pojo_list.size();
				image_view_adapter.notifyDataSetChanged();
				picture_selector_adapter.notifyDataSetChanged();
				FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_name_list,fileObjectType);
				Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION,localBroadcastManager,ImageViewActivity.ACTIVITY_NAME);
				if(album_file_pojo_list.size()<1)
				{
					((ImageViewActivity)context).finish();
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
