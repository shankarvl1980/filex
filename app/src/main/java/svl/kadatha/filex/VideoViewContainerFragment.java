package svl.kadatha.filex;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class VideoViewContainerFragment extends Fragment
{
	private Context context;
	private Toolbar toolbar;
	private TextView title;
	private PopupWindow listPopWindow;
	private ArrayList<ListPopupWindowPOJO> list_popupwindowpojos;
	private List<FilePOJO> files_selected_for_delete;
	private Handler handler;
	private Runnable runnable;
	private boolean is_menu_opened;
	private Uri data;

	private VideoViewPagerAdapter adapter;

	private int floating_button_height;
	private FloatingActionButton floating_back_button;
	private boolean toolbar_visible;

	private VideoViewActivity videoViewActivity;
	public FrameLayout progress_bar;
	public FilteredFilePOJOViewModel viewModel;
	private static final String DELETE_FILE_REQUEST_CODE="video_file_delete_request_code";

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context=context;
		videoViewActivity=((VideoViewActivity)context);
	}


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onCreate(savedInstanceState);
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
							if(viewModel.fromArchiveView || viewModel.fromThirdPartyApp)
							{
								Global.print(context,getString(R.string.not_able_to_process));
								break;
							}
							files_selected_array.add(viewModel.currently_shown_file.getPath());
							DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity=DeleteFileAlertDialogOtherActivity.getInstance(DELETE_FILE_REQUEST_CODE,files_selected_array,viewModel.fileObjectType);
							deleteFileAlertDialogOtherActivity.show(((VideoViewActivity)context).fm,"deletefilealertotheractivity");
							break;
						case 1:
							Uri src_uri=null;
							if(viewModel.fromThirdPartyApp)
							{
								src_uri=data;

							}
							else if(viewModel.fileObjectType==FileObjectType.FILE_TYPE)
							{
								src_uri= FileProvider.getUriForFile(context, context.getPackageName()+".provider",new File(viewModel.currently_shown_file.getPath()));
							}
							if(src_uri==null)
							{
								Global.print(context,getString(R.string.not_able_to_process));
								break;
							}
							ArrayList<Uri> uri_list=new ArrayList<>();
							uri_list.add(src_uri);
							FileIntentDispatch.sendUri(context,uri_list);

							break;
						case 2:
							if(viewModel.fromThirdPartyApp)
							{
								Global.print(context,getString(R.string.not_able_to_process));
								break;
							}
							files_selected_array.add(viewModel.currently_shown_file.getPath());
							PropertiesDialog propertiesDialog=PropertiesDialog.getInstance(files_selected_array,viewModel.fileObjectType);
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
		progress_bar=v.findViewById(R.id.activity_video_progressbar);
		floating_back_button=v.findViewById(R.id.floating_button_video_fragment);
		floating_back_button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View p)
			{
				((VideoViewActivity)context).onBackPressed();
			}
			
		});
		viewModel=new ViewModelProvider(this).get(FilteredFilePOJOViewModel.class);
		data=videoViewActivity.data;
		Bundle bundle=getArguments();
		if(bundle!=null)
		{
			viewModel.file_path=bundle.getString("file_path");
			viewModel.fileObjectType = (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
			viewModel.fromArchiveView=bundle.getBoolean(FileIntentDispatch.EXTRA_FROM_ARCHIVE);

			if(viewModel.fileObjectType ==null || viewModel.fileObjectType ==FileObjectType.SEARCH_LIBRARY_TYPE)
			{
				viewModel.fromThirdPartyApp=true;
				viewModel.fileObjectType =FileObjectType.FILE_TYPE;
			}

		}


		viewModel.getAlbumFromCurrentFolder(Global.VIDEO_REGEX,true);
		viewModel.isFinished.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean aBoolean) {
				if(aBoolean)
				{
					adapter=new VideoViewPagerAdapter(((VideoViewActivity)context).fm,viewModel.video_list);
					viewpager.setAdapter(adapter);
					viewpager.setCurrentItem(viewModel.file_selected_idx);
					if(viewModel.file_selected_idx==0)
					{
						((VideoViewActivity)context).current_page_idx=0;
					}
					progress_bar.setVisibility(View.GONE);
				}
			}
		});

		DeleteFileOtherActivityViewModel deleteFileOtherActivityViewModel=new ViewModelProvider(VideoViewContainerFragment.this).get(DeleteFileOtherActivityViewModel.class);
		deleteFileOtherActivityViewModel.isFinished.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean aBoolean) {
				if(aBoolean)
				{
					if(deleteFileOtherActivityViewModel.deleted_files.size()>0)
					{
						Iterator<Map.Entry<FilePOJO, Integer>> iterator=viewModel.video_list.entrySet().iterator();
						for(FilePOJO filePOJO:deleteFileOtherActivityViewModel.deleted_files)
						{
							while(iterator.hasNext())
							{
								Map.Entry<FilePOJO,Integer> entry=iterator.next();
								if(entry.getKey().getPath().equals(filePOJO.getPath()) && entry.getKey().getFileObjectType()==filePOJO.getFileObjectType())
								{
									viewModel.video_list.removeIndex(filePOJO);
									iterator.remove();
									break;
								}
							}

						}

						adapter.notifyDataSetChanged();
						if(viewModel.video_list.size()<1)
						{
							((VideoViewActivity)context).finish();
						}

					}
					progress_bar.setVisibility(View.GONE);
					deleteFileOtherActivityViewModel.isFinished.setValue(false);
				}
			}
		});



		viewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
			{
				public void onPageSelected(int p)
				{
					((VideoViewActivity)context).current_page_idx=p;
				}

				public void onPageScrollStateChanged(int p)
				{

				}

				public void onPageScrolled(int p1, float p2, int p3)
				{

					viewModel.file_selected_idx=p1;
					viewModel.currently_shown_file=viewModel.video_list.getKeyAtIndex(p1);
					title.setText(viewModel.currently_shown_file.getName());

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
				}
			}
		};

		handler.postDelayed(runnable,Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);


		((AppCompatActivity)context).getSupportFragmentManager().setFragmentResultListener(DELETE_FILE_REQUEST_CODE, this, new FragmentResultListener() {
			@Override
			public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
				if(requestKey.equals(DELETE_FILE_REQUEST_CODE))
				{
					progress_bar.setVisibility(View.VISIBLE);
					Uri tree_uri=result.getParcelable("tree_uri");
					String tree_uri_path=result.getString("tree_uri_path");
					String source_folder=result.getString("source_folder");
					files_selected_for_delete=new ArrayList<>();
					files_selected_for_delete.add(viewModel.currently_shown_file);
					deleteFileOtherActivityViewModel.deleteFilePOJO(source_folder,files_selected_for_delete,viewModel.fileObjectType,tree_uri,tree_uri_path);
				}
			}
		});

		return v;
	}


	public static VideoViewContainerFragment getNewInstance(String file_path, boolean fromArchiveView, FileObjectType fileObjectType)
	{
		VideoViewContainerFragment frag=new VideoViewContainerFragment();
		Bundle bundle=new Bundle();
		bundle.putString("file_path",file_path);
		bundle.putBoolean("fromArchiveView",fromArchiveView);
		bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE,fileObjectType);
		frag.setArguments(bundle);
		return frag;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		handler.removeCallbacksAndMessages(null);
		listPopWindow.dismiss(); // to avoid memory leak on orientation change
	}

	private class VideoViewPagerAdapter extends FragmentStatePagerAdapter
	{
		final IndexedLinkedHashMap<FilePOJO,Integer> list;
		VideoViewPagerAdapter(FragmentManager fm, IndexedLinkedHashMap<FilePOJO,Integer> l)
		{
			super(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			this.list=l;
			if(viewModel.currently_shown_file==null)
			{
				getActivity().finish();
			}
			else
			{
				title.setText(viewModel.currently_shown_file.getName());
			}

		}

		@Override
		public Fragment getItem(int p1)
		{
			// TODO: Implement this method

			final VideoViewFragment frag;
			boolean b=viewModel.firststart;
			FilePOJO filePOJO=viewModel.video_list.getKeyAtIndex(p1);
			final String file_path=filePOJO.getPath();
			int position=viewModel.video_list.get(filePOJO);
			frag=VideoViewFragment.getNewInstance(viewModel.fileObjectType,viewModel.fromThirdPartyApp,file_path,position,p1,b);
			viewModel.firststart=false;

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
							is_menu_opened=false;
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

					}
				});

			frag.setVideoPositionListener(new VideoViewFragment.VideoPositionListener()
			{
				public void setPosition(Integer idx, Integer position)
				{
					if(viewModel.video_list.size()>idx) // condition is required, otherwise app crashes, when last video is removed
					{
						viewModel.video_list.put(viewModel.video_list.getKeyAtIndex(idx),position);
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

}
