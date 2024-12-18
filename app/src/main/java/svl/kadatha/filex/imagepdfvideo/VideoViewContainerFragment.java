package svl.kadatha.filex.imagepdfvideo;

import android.content.Context;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import svl.kadatha.filex.AsyncTaskStatus;
import svl.kadatha.filex.CopyToActivity;
import svl.kadatha.filex.DeleteFileAlertDialogOtherActivity;
import svl.kadatha.filex.DeleteFileOtherActivityViewModel;
import svl.kadatha.filex.FileIntentDispatch;
import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FilePOJO;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.IndexedLinkedHashMap;
import svl.kadatha.filex.ListPopupWindowPOJO;
import svl.kadatha.filex.PropertiesDialog;
import svl.kadatha.filex.R;


public class VideoViewContainerFragment extends Fragment implements VideoViewActivity.VideoControlListener {
    public static final String REFRESH_VIDEO_CODE = "video_play_refresh_code";
    private static final String DELETE_FILE_REQUEST_CODE = "video_file_delete_request_code";
    public FrameLayout progress_bar;
    public FilteredFilePOJOViewModel viewModel;
    private Context context;
    private LinearLayout toolbar_container;
    private TextView title, video_number_tv;
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
    private AppCompatActivity activity;
    private ViewPager viewpager;


    public static VideoViewContainerFragment getNewInstance(String file_path, FileObjectType fileObjectType, boolean fromArchive) {
        VideoViewContainerFragment frag = new VideoViewContainerFragment();
        Bundle bundle = new Bundle();
        bundle.putString("file_path", file_path);
        bundle.putSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE, fileObjectType);
        bundle.putBoolean("fromArchive", fromArchive);
        frag.setArguments(bundle);
        return frag;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        activity = ((AppCompatActivity) context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        list_popupwindowpojos = new ArrayList<>();
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon, getString(R.string.delete), 1));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon, getString(R.string.send), 2));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.copy_icon, getString(R.string.copy_to), 3));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon, getString(R.string.properties), 4));

        float height = getResources().getDimension(R.dimen.floating_button_margin_bottom) + 56;
        floating_button_height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, context.getResources().getDisplayMetrics());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View v;
        v = inflater.inflate(R.layout.fragment_video_view_container, container, false);
        handler = new Handler();
        viewpager = v.findViewById(R.id.activity_video_view_viewpager);
        toolbar_container = v.findViewById(R.id.activity_video_toolbar_container);
        title = v.findViewById(R.id.activity_video_name);
        ImageView overflow = v.findViewById(R.id.activity_video_overflow);
        overflow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                is_menu_opened = true;
                listPopWindow.showAsDropDown(v, 0, Global.LIST_POPUP_WINDOW_DROP_DOWN_OFFSET);
            }
        });

        video_number_tv = v.findViewById(R.id.video_view_current_view_number);

        listPopWindow = new PopupWindow(context);
        ListView listView = new ListView(context);
        listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapter(context, list_popupwindowpojos));
        listPopWindow.setContentView(listView);
        listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popup_window_width));
        listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopWindow.setFocusable(true);
        listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.list_popup_background));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterview, View v, int p1, long p2) {
                final ArrayList<String> files_selected_array = new ArrayList<>();
                switch (p1) {
                    case 0:
                        if (viewModel.fromArchive || viewModel.fromThirdPartyApp || Global.whether_file_cached(viewModel.fileObjectType)) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        files_selected_array.add(viewModel.currently_shown_file.getPath());
                        DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity = DeleteFileAlertDialogOtherActivity.getInstance(DELETE_FILE_REQUEST_CODE, files_selected_array, viewModel.fileObjectType);
                        deleteFileAlertDialogOtherActivity.show(getParentFragmentManager(), "deletefilealertotheractivity");
                        break;
                    case 1:
                        Uri src_uri;
                        if (viewModel.fromThirdPartyApp) {
                            src_uri = data;
                        } else {
                            src_uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", new File(viewModel.currently_shown_file.getPath()));
                        }

                        if (src_uri == null) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        ArrayList<Uri> uri_list = new ArrayList<>();
                        uri_list.add(src_uri);
                        FileIntentDispatch.sendUri(context, uri_list);
                        break;
                    case 2:
                        Uri copy_uri;
                        if (viewModel.fromThirdPartyApp) {
                            copy_uri = data;
                        } else {
                            copy_uri = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(viewModel.currently_shown_file.getPath()));
                        }
                        if (copy_uri == null) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        if (activity instanceof VideoViewActivity) {
                            ((VideoViewActivity) activity).clear_cache = false;
                        }

                        Intent copy_intent = new Intent(context, CopyToActivity.class);
                        copy_intent.setAction(Intent.ACTION_SEND);
                        copy_intent.putExtra(Intent.EXTRA_STREAM, copy_uri);
                        copy_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        copy_intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        copy_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            startActivity(copy_intent);
                        } catch (Exception e) {
                            Global.print(context, getString(R.string.could_not_perform_action));
                        }
                        break;
                    case 3:
                        if (viewModel.fromArchive || viewModel.fromThirdPartyApp || Global.whether_file_cached(viewModel.fileObjectType)) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        files_selected_array.add(viewModel.currently_shown_file.getPath());
                        PropertiesDialog propertiesDialog = PropertiesDialog.getInstance(files_selected_array, viewModel.fileObjectType);
                        propertiesDialog.show(getParentFragmentManager(), "properties_dialog");
                        break;
                    default:
                        break;
                }
                listPopWindow.dismiss();
            }
        });

        listPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            public void onDismiss() {
                is_menu_opened = false;
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
            }
        });
        progress_bar = v.findViewById(R.id.activity_video_progressbar);
        floating_back_button = v.findViewById(R.id.floating_button_video_fragment);
        floating_back_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View p) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
        viewModel = new ViewModelProvider(requireActivity()).get(FilteredFilePOJOViewModel.class);
        if (activity instanceof VideoViewActivity) {
            data = ((VideoViewActivity) activity).data;
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            viewModel.file_path = bundle.getString("file_path");
            viewModel.fileObjectType = (FileObjectType) bundle.getSerializable(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE);
            viewModel.fromArchive = bundle.getBoolean("fromArchive");
            if (viewModel.fileObjectType == null || viewModel.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                viewModel.fromThirdPartyApp = true;
                viewModel.fileObjectType = FileObjectType.FILE_TYPE;
            }
        }

        viewModel.getAlbumFromCurrentFolder(Global.VIDEO_REGEX, true);
        viewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (Global.whether_file_cached(viewModel.fileObjectType)) {
                        if (activity instanceof VideoViewActivity) {
                            ((VideoViewActivity) activity).data = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(viewModel.currently_shown_file.getPath()));
                        }
                    }
                    adapter = new VideoViewPagerAdapter(getChildFragmentManager(), viewModel.video_list);
                    viewpager.setAdapter(adapter);
                    viewpager.setCurrentItem(viewModel.file_selected_idx);
                    if (viewModel.file_selected_idx == 0) {
                        if (activity instanceof VideoViewActivity) {
                            ((VideoViewActivity) activity).current_page_idx = 0;
                        }
                        video_number_tv.setText("1/" + viewModel.video_list.size());
                    }
                }
            }
        });

        DeleteFileOtherActivityViewModel deleteFileOtherActivityViewModel = new ViewModelProvider(VideoViewContainerFragment.this).get(DeleteFileOtherActivityViewModel.class);
        deleteFileOtherActivityViewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (!deleteFileOtherActivityViewModel.deleted_files.isEmpty()) {
                        Iterator<Map.Entry<FilePOJO, Integer>> iterator = viewModel.video_list.entrySet().iterator();
                        for (FilePOJO filePOJO : deleteFileOtherActivityViewModel.deleted_files) {
                            while (iterator.hasNext()) {
                                Map.Entry<FilePOJO, Integer> entry = iterator.next();
                                if (entry.getKey().getPath().equals(filePOJO.getPath()) && entry.getKey().getFileObjectType() == filePOJO.getFileObjectType()) {
                                    viewModel.video_list.removeIndex(filePOJO); //remove will not index, hence separately removing index
                                    iterator.remove();
                                    break;
                                }
                            }
                        }

                        adapter.notifyDataSetChanged();
                        if (activity instanceof VideoViewActivity) {
                            video_number_tv.setText(((VideoViewActivity) activity).current_page_idx + 1 + "/" + viewModel.video_list.size());
                        }
                        if (viewModel.video_list.isEmpty()) {
                            getActivity().finish();
                        }
                    }
                    deleteFileOtherActivityViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        viewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageSelected(int p) {
                if (activity instanceof VideoViewActivity) {
                    ((VideoViewActivity) activity).current_page_idx = p;
                }
                video_number_tv.setText(p + 1 + "/" + viewModel.video_list.size());
            }

            public void onPageScrollStateChanged(int p) {

            }

            public void onPageScrolled(int p1, float p2, int p3) {
                viewModel.file_selected_idx = p1;
                viewModel.currently_shown_file = viewModel.video_list.getKeyAtIndex(p1);
                title.setText(viewModel.currently_shown_file.getName());
            }
        });

        runnable = new Runnable() {
            public void run() {
                if (!is_menu_opened) {
                    VideoViewFragment currentFragment = getCurrentVideoViewFragment();
                    if (currentFragment != null) {
                        if (currentFragment.isPlaying()) {
                            hideControls();
                        }
                    }
                }
            }
        };


        getParentFragmentManager().setFragmentResultListener(DELETE_FILE_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(DELETE_FILE_REQUEST_CODE)) {
                    progress_bar.setVisibility(View.VISIBLE);
                    Uri tree_uri = result.getParcelable("tree_uri");
                    String tree_uri_path = result.getString("tree_uri_path");
                    String source_folder = result.getString("source_folder");
                    files_selected_for_delete = new ArrayList<>();
                    files_selected_for_delete.add(viewModel.currently_shown_file);
                    deleteFileOtherActivityViewModel.deleteFilePOJO(source_folder, files_selected_for_delete, viewModel.fileObjectType, tree_uri, tree_uri_path);
                }
            }
        });

        getActivity().getSupportFragmentManager().setFragmentResultListener(REFRESH_VIDEO_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(REFRESH_VIDEO_CODE)) {
                    adapter.notifyDataSetChanged();
                    viewModel.video_refreshed = true;
                }
            }
        });
        return v;
    }


    @Override
    public void showControls(boolean autoHide) {
        toolbar_container.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));
        floating_back_button.animate().translationY(0).setInterpolator(new AccelerateInterpolator(1));

        VideoViewFragment currentFragment = getCurrentVideoViewFragment();
        if (currentFragment != null) {
            currentFragment.showBottomControls();
        }

        if (autoHide) {
            handler.postDelayed(runnable, Global.LIST_POPUP_WINDOW_DISAPPEARANCE_DELAY);
        }
    }

    @Override
    public void hideControls() {
        toolbar_container.animate().translationY(-Global.ACTION_BAR_HEIGHT).setInterpolator(new DecelerateInterpolator(1));
        floating_back_button.animate().translationY(floating_button_height).setInterpolator(new DecelerateInterpolator(1));
        handler.removeCallbacks(runnable);

        VideoViewFragment currentFragment = getCurrentVideoViewFragment();
        if (currentFragment != null) {
            currentFragment.hideBottomControls();
        }
    }

    private VideoViewFragment getCurrentVideoViewFragment() {
        if (adapter != null) {
            VideoViewFragment fragment = adapter.getCurrentFragment();
            if (fragment != null && fragment.isAdded() && fragment.getView() != null) {
                return fragment;
            }
        }
        return null;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        listPopWindow.dismiss(); // to avoid memory leak on orientation change
    }

    private class VideoViewPagerAdapter extends FragmentStatePagerAdapter {
        final IndexedLinkedHashMap<FilePOJO, Integer> list;
        private VideoViewFragment currentFragment;

        VideoViewPagerAdapter(FragmentManager fm, IndexedLinkedHashMap<FilePOJO, Integer> l) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.list = l;
            if (viewModel.currently_shown_file == null) {
                getActivity().finish();
            } else {
                title.setText(viewModel.currently_shown_file.getName());
            }
        }

        @Override
        public Fragment getItem(int position) {
            boolean isFirstStart = (position == viewModel.file_selected_idx);
            FilePOJO filePOJO = viewModel.video_list.getKeyAtIndex(position);
            String filePath = filePOJO.getPath();
            Integer filePosition = viewModel.video_list.get(filePOJO);

            VideoViewFragment fragment = VideoViewFragment.getNewInstance(
                    viewModel.fileObjectType,
                    viewModel.fromThirdPartyApp,
                    filePath,
                    filePosition,
                    position,
                    isFirstStart
            );

            fragment.setVideoPositionListener(new VideoViewFragment.VideoPositionListener() {
                @Override
                public void setPosition(Integer idx, Integer pos) {
                    if (viewModel.video_list.size() > idx) {
                        viewModel.video_list.put(viewModel.video_list.getKeyAtIndex(idx), pos);
                    }
                }
            });
            return fragment;
        }

        @Override
        public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            if (getCurrentFragment() != object) {
                currentFragment = (VideoViewFragment) object;
            }
            super.setPrimaryItem(container, position, object);
        }

        public VideoViewFragment getCurrentFragment() {
            return currentFragment;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }
}
