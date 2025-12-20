package svl.kadatha.filex;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import me.jahnen.libaums.core.fs.UsbFile;
import svl.kadatha.filex.usb.ReadAccess;
import svl.kadatha.filex.usb.UsbFileRootSingleton;

public class DetailFragment extends Fragment implements FileModifyObserver.FileObserverListener {
    public static final String USB_FILE_PREFIX = "usb:";
    public static final String FTP_FILE_PREFIX = "ftp:";
    public static final String SFTP_FILE_PREFIX = "sftp:";
    public static final String WEBDAV_FILE_PREFIX = "webdav:";
    public static final String SMB_FILE_PREFIX = "smb:";
    static final String SEARCH_RESULT = "Search";
    private static final String CANCEL_PROGRESS_REQUEST_CODE = "search_cancel_progress_request_code";
    private final static String SAF_PERMISSION_REQUEST_CODE = "detail_fragment_saf_permission_request_code";
    private final static String ALBUM_SELECT_REQUEST_CODE = "detail_fragment_album_select_request_code";
    private static final List<String> LIBRARY_CATEGORIES = new ArrayList<>(Arrays.asList("Download", "Document", "Image", "Audio", "Video", "Archive", "APK"));
    public static boolean CUT_SELECTED;
    public static boolean COPY_SELECTED;
    public static FileObjectType CUT_COPY_FILE_OBJECT_TYPE;
    public static String CUT_COPY_FILE_CLICK_SELECTED = "";
    public static ArrayList<String> FILE_SELECTED_FOR_CUT_COPY = new ArrayList<>();
    public static FilePOJO TO_BE_MOVED_TO_FILE_POJO;
    public static String search_file_name;
    public List<FilePOJO> filePOJO_list, totalFilePOJO_list;
    public int totalFilePOJO_list_Size;
    public RecyclerView filepath_recyclerview;
    public RecyclerView recyclerView;
    public ImageView layout_image_view, filter_image_view, time_image_view, size_image_view;
    public boolean grid_layout;
    public FilePathRecyclerViewAdapter filepath_adapter;
    public String fileclickselected = "";
    public String file_click_selected_name = "";
    public UsbFile currentUsbFile;
    public Set<FilePOJO> search_in_dir = new HashSet<>();
    public String search_file_type;
    public boolean search_whole_word, search_case_sensitive, search_regex;
    public FileObjectType fileObjectType;
    public int file_list_size;
    public Uri tree_uri;
    public String tree_uri_path = "";
    public boolean local_activity_delete, modification_observed;
    public FrameLayout progress_bar;
    public FilePOJOViewModel viewModel;
    public DetailFragmentListener detailFragmentListener;
    public LinearLayoutManager llm;
    public GridLayoutManager glm;
    public TextView folder_empty;
    public DetailRecyclerViewAdapter adapter;
    public boolean is_toolbar_visible = true;
    private TinyDB tinyDB;
    private long search_lower_limit_size = 0;
    private long search_upper_limit_size = 0;
    private Context context;
    private FileModifyObserver fileModifyObserver;
    private FilePOJO clicked_filepojo;

    public static DetailFragment getInstance(FileObjectType fileObjectType) {
        DetailFragment df = new DetailFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("fileObjectType", fileObjectType);
        df.setArguments(bundle);
        return df;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        tinyDB = new TinyDB(context);

        AppCompatActivity activity = (AppCompatActivity) context;
        if (activity instanceof DetailFragmentListener) {
            detailFragmentListener = (DetailFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        detailFragmentListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        fileObjectType = (FileObjectType) bundle.getSerializable("fileObjectType");
        fileclickselected = getTag();
        if (fileObjectType == FileObjectType.ROOT_TYPE) {
            if (FileUtil.isFromInternal(FileObjectType.FILE_TYPE, fileclickselected) || FileUtil.isFilePathFromExternalStorage(FileObjectType.FILE_TYPE, fileclickselected)) {
                fileObjectType = FileObjectType.FILE_TYPE;
            }
        } else if (fileObjectType == FileObjectType.FILE_TYPE) {
            RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
            for (String path : repositoryClass.internal_storage_path_list) {
                if (Global.IS_CHILD_FILE(new File(path).getParent(), fileclickselected)) {
                    fileObjectType = FileObjectType.ROOT_TYPE;
                    break;
                }
            }
        }

        if (fileclickselected.equals(File.separator)) {
            if (fileObjectType == FileObjectType.USB_TYPE) {
                file_click_selected_name = USB_FILE_PREFIX + fileclickselected;
            } else if (fileObjectType == FileObjectType.FTP_TYPE) {
                file_click_selected_name = FTP_FILE_PREFIX + fileclickselected;
            } else if (fileObjectType == FileObjectType.SFTP_TYPE) {
                file_click_selected_name = SFTP_FILE_PREFIX + fileclickselected;
            } else if (fileObjectType == FileObjectType.WEBDAV_TYPE) {
                file_click_selected_name = WEBDAV_FILE_PREFIX + fileclickselected;
            } else if (fileObjectType == FileObjectType.SMB_TYPE) {
                file_click_selected_name = SMB_FILE_PREFIX + fileclickselected;
            } else {
                file_click_selected_name = fileclickselected;
            }
        } else {
            file_click_selected_name = new File(fileclickselected).getName();
        }

        if (fileObjectType == FileObjectType.USB_TYPE) {
            try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
                UsbFile usbFileRoot = access.getUsbFile();
                try {
                    currentUsbFile = usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(fileclickselected));
                } catch (Exception e) {

                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_detail, container, false);
        fileModifyObserver = FileModifyObserver.getInstance(fileclickselected);
        fileModifyObserver.setFileObserverListener(this);
        filepath_recyclerview = v.findViewById(R.id.fragment_detail_filepath_container);
        time_image_view = v.findViewById(R.id.fragment_detail_time_image);
        time_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewModel.library_time_desc) {
                    Collections.sort(filePOJO_list, FileComparator.FilePOJOComparate(Global.SORT, false));
                    viewModel.library_time_desc = false;
                } else {
                    Collections.sort(filePOJO_list, FileComparator.FilePOJOComparate("d_date_desc", false));
                    viewModel.library_time_desc = true;
                }
                time_image_view.setSelected(viewModel.library_time_desc);
                adapter.notifyDataSetChanged();
            }
        });

        size_image_view = v.findViewById(R.id.fragment_detail_size_image);
        size_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewModel.library_size_desc) {
                    Collections.sort(filePOJO_list, FileComparator.FilePOJOComparate(Global.SORT, false));
                    viewModel.library_size_desc = false;
                } else {
                    Collections.sort(filePOJO_list, FileComparator.FilePOJOComparate("d_size_desc", false));
                    viewModel.library_size_desc = true;
                }
                size_image_view.setSelected(viewModel.library_size_desc);
                adapter.notifyDataSetChanged();
            }
        });

        filter_image_view = v.findViewById(R.id.fragment_detail_filter_image);
        filter_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewModel.filePOJOS.isEmpty()) {
                    return;
                }
                if (detailFragmentListener != null) {
                    detailFragmentListener.setSearchBarVisibility(false);
                }
                LibraryAlbumSelectDialog libraryAlbumSelectDialog = LibraryAlbumSelectDialog.getInstance(ALBUM_SELECT_REQUEST_CODE, file_click_selected_name);
                libraryAlbumSelectDialog.show(getParentFragmentManager(), "");
            }
        });

        layout_image_view = v.findViewById(R.id.fragment_detail_layout_image);
        layout_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (file_click_selected_name.equals("Image") || file_click_selected_name.equals("Video")) {
                    Global.IMAGE_VIDEO_GRID_LAYOUT = !Global.IMAGE_VIDEO_GRID_LAYOUT;
                    layout_image_view.setImageResource(Global.IMAGE_VIDEO_GRID_LAYOUT ? R.drawable.list_layout_icon : R.drawable.grid_layout_icon);
                    tinyDB.putBoolean("image_video_grid_layout", Global.IMAGE_VIDEO_GRID_LAYOUT);
                }
                getParentFragmentManager().beginTransaction().detach(DetailFragment.this).commit();
                getParentFragmentManager().beginTransaction().attach(DetailFragment.this).commit();
            }
        });

        progress_bar = v.findViewById(R.id.fragment_detail_progressbar);
        recyclerView = v.findViewById(R.id.fragment_detail_container);
        DividerItemDecoration itemdecor = new DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL);
        itemdecor.setDrawable(ContextCompat.getDrawable(context, R.drawable.right_private_icon));
        filepath_recyclerview.addItemDecoration(itemdecor);
        LinearLayoutManager file_path_lm = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        filepath_recyclerview.setLayoutManager(file_path_lm);

        if (fileObjectType.equals(FileObjectType.SEARCH_LIBRARY_TYPE) && (file_click_selected_name.equals("Image") || file_click_selected_name.equals("Video"))) {
            grid_layout = Global.IMAGE_VIDEO_GRID_LAYOUT;
        } else {
            grid_layout = Global.FILE_GRID_LAYOUT;
        }

        if (grid_layout) {
            glm = new GridLayoutManager(context, Global.GRID_COUNT);
            recyclerView.setLayoutManager(glm);
            int top_padding = recyclerView.getPaddingTop();
            int bottom_padding = recyclerView.getPaddingBottom();
            recyclerView.setPadding(Global.RECYCLERVIEW_ITEM_SPACING, top_padding, Global.RECYCLERVIEW_ITEM_SPACING, bottom_padding);
        } else {
            llm = new LinearLayoutManager(context);
            recyclerView.setLayoutManager(llm);
        }

        ItemSeparatorDecoration dividerItemDecoration = new ItemSeparatorDecoration(context, 1, recyclerView);
        recyclerView.addItemDecoration(dividerItemDecoration);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            final int threshold = 5;
            int scroll_distance = 0;

            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (scroll_distance > threshold && is_toolbar_visible) {
                    if (detailFragmentListener != null) {
                        detailFragmentListener.onScrollRecyclerView(false);
                    }

                    is_toolbar_visible = false;
                    scroll_distance = 0;
                } else if (scroll_distance < -threshold && !is_toolbar_visible) {
                    if (detailFragmentListener != null) {
                        detailFragmentListener.onScrollRecyclerView(true);
                    }

                    is_toolbar_visible = true;
                    scroll_distance = 0;
                }

                if ((is_toolbar_visible && dy > 0) || (!is_toolbar_visible && dy < 0)) {
                    scroll_distance += dy;
                }
            }
        });

        FastScrollerView fastScroller = v.findViewById(R.id.fastScroller_detail_fragment);
        fastScroller.setRecyclerView(recyclerView);
        folder_empty = v.findViewById(R.id.empty_folder);
        filepath_adapter = new FilePathRecyclerViewAdapter(fileclickselected);
        viewModel = new ViewModelProvider(this).get(FilePOJOViewModel.class);
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        if (!repositoryClass.hashmap_file_pojo.containsKey(fileObjectType + fileclickselected)) {
            if (fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                if (detailFragmentListener != null) {
                    MainActivity.SearchParameters searchParameters = detailFragmentListener.getSearchParameters();
                    search_file_name = searchParameters.search_file_name;
                    search_in_dir = searchParameters.search_in_dir;
                    search_file_type = searchParameters.search_file_type;
                    search_whole_word = searchParameters.search_whole_word;
                    search_case_sensitive = searchParameters.search_case_sensitive;
                    search_regex = searchParameters.search_regex;
                    search_lower_limit_size = searchParameters.search_lower_limit_size;
                    search_upper_limit_size = searchParameters.search_upper_limit_size;
                }

                if (LIBRARY_CATEGORIES.contains(fileclickselected) || fileclickselected.equals("Large Files") || fileclickselected.equals("Duplicate Files")) {
                    viewModel.getLibraryList(fileclickselected);
                } else {
                    viewModel.populateLibrarySearchFilePOJO(fileObjectType, search_in_dir, file_click_selected_name, fileclickselected, search_file_name, search_file_type, search_whole_word, search_case_sensitive, search_regex, search_lower_limit_size, search_upper_limit_size);
                }
            } else {
                viewModel.populateFilePOJO(fileObjectType, fileclickselected, currentUsbFile, false, false);
            }
        } else {
            viewModel.filePOJOS = repositoryClass.hashmap_file_pojo.get(fileObjectType + fileclickselected);
            viewModel.filePOJOS_filtered = repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType + fileclickselected);
            after_filledFilePojos_procedure();
        }

        viewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    after_filledFilePojos_procedure();
                }
            }
        });

        viewModel.subFileCountUpdate.observe(getViewLifecycleOwner(), pojo -> {
            try {
                int idx = filePOJO_list.indexOf(pojo);
                if (idx < 0 || idx >= adapter.getItemCount()) return;
                adapter.notifyItemChanged(idx);
            } catch (IndexOutOfBoundsException ignored) {
                // log and move on – indicates a data/adapter desync
            }
        });


        getParentFragmentManager().setFragmentResultListener(CANCEL_PROGRESS_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(CANCEL_PROGRESS_REQUEST_CODE)) {
                    viewModel.cancel(true);
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(SAF_PERMISSION_REQUEST_CODE)) {
                    tree_uri = result.getParcelable("tree_uri");
                    tree_uri_path = result.getString("tree_uri_path");
                }
            }
        });

        getParentFragmentManager().setFragmentResultListener(ALBUM_SELECT_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(ALBUM_SELECT_REQUEST_CODE)) {
                    if (progress_bar.getVisibility() != View.VISIBLE && result.getString("library_type").equals(fileclickselected)) {
                        String parent_file_name = result.getString("parent_file_name");
                        if (parent_file_name != null) {
                            filepath_adapter = new FilePathRecyclerViewAdapter(fileclickselected + File.separator + result.getString("parent_file_name"));
                        } else {
                            filepath_adapter = new FilePathRecyclerViewAdapter(fileclickselected);
                        }
                        filepath_recyclerview.setAdapter(filepath_adapter);
                        viewModel.library_filter_path = result.getString("parent_file_path");
                        adapter.getFilter().filter(null);
                    }
                }
            }
        });

        if (detailFragmentListener != null) {
            detailFragmentListener.onCreateView(fileclickselected, fileObjectType);
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (local_activity_delete) {
            clearSelectionAndNotifyDataSetChanged();
        } else if (modification_observed) {
            if (detailFragmentListener != null) {
                detailFragmentListener.actionModeFinish(this, fileclickselected);
            }

            modification_observed = false;
            local_activity_delete = false;
            progress_bar.setVisibility(View.VISIBLE);
            if (fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                if (LIBRARY_CATEGORIES.contains(fileclickselected)) {
                    viewModel.getLibraryList(fileclickselected);
                } else {
                    viewModel.populateLibrarySearchFilePOJO(fileObjectType, search_in_dir, file_click_selected_name, fileclickselected, search_file_name, search_file_type, search_whole_word, search_case_sensitive, search_regex, search_lower_limit_size, search_upper_limit_size);
                }
            } else {
                viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                viewModel.populateFilePOJO(fileObjectType, fileclickselected, currentUsbFile, false, false);
            }

            ExecutorService executorService = MyExecutorService.getExecutorService();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    FilePOJOUtil.UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(fileclickselected, fileObjectType);
                }
            });
        }
    }

    private void after_filledFilePojos_procedure() {
        if (MainActivity.SHOW_HIDDEN_FILE) {
            filePOJO_list = viewModel.filePOJOS;
            totalFilePOJO_list = viewModel.filePOJOS;
        } else {
            filePOJO_list = viewModel.filePOJOS_filtered;
            totalFilePOJO_list = viewModel.filePOJOS_filtered;
        }
        totalFilePOJO_list_Size = totalFilePOJO_list.size();
        file_list_size = filePOJO_list.size();
        if (fileclickselected.equals("Large Files")) {
            Collections.sort(filePOJO_list, viewModel.library_size_desc ? FileComparator.FilePOJOComparate("d_size_desc", false) : FileComparator.FilePOJOComparate(Global.SORT, false));
        } else if (fileclickselected.equals("Duplicate Files")) {
            Collections.sort(filePOJO_list, Global.SORT.equals("d_name_desc") ? FileComparator.FilePOJOComparate(Global.SORT, false) : FileComparator.FilePOJOComparate("d_name_asc", false));
        } else {
            Collections.sort(filePOJO_list, viewModel.library_time_desc ? FileComparator.FilePOJOComparate("d_date_desc", false) : FileComparator.FilePOJOComparate(Global.SORT, false));
        }
        boolean show_file_path = false;
        if (fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
            if (DetailFragment.SEARCH_RESULT.equals(fileclickselected) || fileclickselected.equals("Large Files")) {
                show_file_path = true;
            } else {
                show_file_path = Global.SHOW_FILE_PATH;
            }
        }

        if (grid_layout) {
            if (fileclickselected.equals("Duplicate Files")) {
                adapter = new DetailRecyclerViewDuplicateAdapterGrid(context, this, show_file_path);
            } else {
                adapter = new DetailRecyclerViewAdapterGrid(context, this, show_file_path);
            }
        } else {
            if (fileclickselected.equals("Duplicate Files")) {
                adapter = new DetailRecyclerViewDuplicateAdapterList(context, this, show_file_path);
            } else {
                adapter = new DetailRecyclerViewAdapterList(context, this, show_file_path);
            }
        }

        set_adapter();
        progress_bar.setVisibility(View.GONE);
        viewModel.ensureSubFileCount();

        if (TO_BE_MOVED_TO_FILE_POJO != null) {
            int idx = filePOJO_list.indexOf(TO_BE_MOVED_TO_FILE_POJO);
            if (llm != null) {
                llm.scrollToPositionWithOffset(idx, 0);
            } else if (glm != null) {
                glm.scrollToPositionWithOffset(idx, 0);
            }
            TO_BE_MOVED_TO_FILE_POJO = null;
        }
        if (detailFragmentListener != null) {
            detailFragmentListener.setFileNumberView(viewModel.mselecteditems.size() + "/" + file_list_size);
        }
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
        if (adapter != null) {
            adapter.setCardViewClickListener(null);
        }
    }

    @Override
    public void onFileModified() {
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION, LocalBroadcastManager.getInstance(context), null);
    }

    public void file_open_intent_dispatch(final String file_path, final FileObjectType fileObjectType, String file_name, boolean select_app, long file_size) {
        int idx = file_name.lastIndexOf(".");
        String file_ext = "";
        if (idx > 0) {
            file_ext = file_name.substring(idx + 1);
        }

        if (file_ext.isEmpty() || Global.NO_APPS_FOR_RECOGNISED_FILE_EXT(context, file_ext)) {
            FileTypeSelectDialog fileTypeSelectDialog = FileTypeSelectDialog.getInstance(file_path, fileObjectType, tree_uri, tree_uri_path, select_app, file_size);
            fileTypeSelectDialog.show(getParentFragmentManager(), "");
        } else {
            if (file_ext.matches("(?i)apk")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!context.getPackageManager().canRequestPackageInstalls()) {
                        Intent unknown_package_install_intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                        unknown_package_install_intent.setData(Uri.parse(String.format("package:%s", Global.FILEX_PACKAGE)));
                        activityResultLauncher_unknown_package_install_permission.launch(unknown_package_install_intent);
                        return;
                    }
                }
            }

            if (fileObjectType == FileObjectType.USB_TYPE) {
                if (file_size > Global.CACHE_FILE_MAX_LIMIT) {
                    Global.print(context, context.getString(R.string.file_is_large_copy_to_device_storage));
                    return;
                }

                if (!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(fileObjectType, null)) {
                    Global.print(context, context.getString(R.string.wait_till_completion_on_going_operation_on_usb));
                    return;
                }
                FileIntentDispatch.openFile(context, file_path, "", fileObjectType, select_app, file_size, false);

            } else if (Global.whether_file_cached(fileObjectType)) {
                if (file_size > Global.CACHE_FILE_MAX_LIMIT) {
                    Global.print(context, context.getString(R.string.file_is_large_copy_to_device_storage));
                    return;
                }
                FileIntentDispatch.openFile(context, file_path, "", fileObjectType, select_app, file_size, false);
            } else if (fileObjectType == FileObjectType.FILE_TYPE) {
                FileIntentDispatch.openFile(context, file_path, "", fileObjectType, select_app, file_size, false);
            }
        }
    }

    public void set_adapter() {
        if (fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE && (file_click_selected_name.equals("Download") || file_click_selected_name.equals("Document") || file_click_selected_name.equals("Image")
                || file_click_selected_name.equals("Audio") || file_click_selected_name.equals("Video")
                || file_click_selected_name.equals("Large Files"))
        ) {
            if (fileclickselected.equals("Download")) {
                filter_image_view.setVisibility(View.GONE);
            } else {
                filter_image_view.setVisibility(View.VISIBLE);
            }

            if (fileclickselected.equals("Large Files")) {
                size_image_view.setVisibility(View.VISIBLE);
                size_image_view.setSelected(viewModel.library_size_desc);
            } else {
                time_image_view.setVisibility(View.VISIBLE);
                time_image_view.setSelected(viewModel.library_time_desc);
            }

            if (viewModel.library_filter_path != null) {
                adapter.getFilter().filter(null);
                filepath_adapter = new FilePathRecyclerViewAdapter(file_click_selected_name + File.separator + new File(viewModel.library_filter_path).getName());
            }

            if (file_click_selected_name.equals("Image") || file_click_selected_name.equals("Video")) {
                layout_image_view.setVisibility(View.VISIBLE);
                layout_image_view.setImageResource(Global.IMAGE_VIDEO_GRID_LAYOUT ? R.drawable.list_layout_icon : R.drawable.grid_layout_icon);
            }
        }

        filepath_recyclerview.setAdapter(filepath_adapter);
        filepath_recyclerview.scrollToPosition(filepath_adapter.getItemCount() - 1);
        recyclerView.setAdapter(adapter);
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof androidx.recyclerview.widget.SimpleItemAnimator) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) animator)
                    .setSupportsChangeAnimations(false);
        }
        if (file_list_size == 0) {
            recyclerView.setVisibility(View.GONE);
            folder_empty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            folder_empty.setVisibility(View.GONE);
        }

        adapter.setCardViewClickListener(new DetailRecyclerViewAdapter.CardViewClickListener() {
            public void onClick(FilePOJO filePOJO) {
                clicked_filepojo = filePOJO;
                if (filePOJO.getIsDirectory()) {
                    if (detailFragmentListener != null) {
                        detailFragmentListener.createFragmentTransaction(filePOJO.getPath(), filePOJO.getFileObjectType());
                    }
                } else {
                    file_open_intent_dispatch(filePOJO.getPath(), filePOJO.getFileObjectType(), filePOJO.getName(), false, filePOJO.getSizeLong());
                }
                RecentDialog.ADD_FILE_POJO_TO_RECENT(filePOJO);
            }

            public void onLongClick(FilePOJO filePOJO, int size) {
                if (detailFragmentListener != null) {
                    detailFragmentListener.onLongClickItem(size);
                }
                is_toolbar_visible = true;
            }
        });
    }

    public void clearSelectionAndNotifyDataSetChanged() {
        viewModel.mselecteditems = new IndexedLinkedHashMap<>();
        if (adapter != null) {
            modification_observed = false;
            local_activity_delete = false;
            RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
            List<FilePOJO> fjos = repositoryClass.hashmap_file_pojo.get(fileObjectType + fileclickselected);
            if (fjos != null) {
                viewModel.filePOJOS = fjos;
                viewModel.filePOJOS_filtered = repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType + fileclickselected);
            }

            if (MainActivity.SHOW_HIDDEN_FILE) {
                filePOJO_list = viewModel.filePOJOS;
                totalFilePOJO_list = viewModel.filePOJOS;
            } else {
                filePOJO_list = viewModel.filePOJOS_filtered;
                totalFilePOJO_list = viewModel.filePOJOS_filtered;
            }
            totalFilePOJO_list_Size = totalFilePOJO_list.size();
            file_list_size = filePOJO_list.size();
            if (detailFragmentListener != null) {
                detailFragmentListener.setFileNumberView(viewModel.mselecteditems.size() + "/" + file_list_size);
            }
            if (fileclickselected.equals("Large Files")) {
                Collections.sort(filePOJO_list, viewModel.library_size_desc ? FileComparator.FilePOJOComparate("d_size_desc", false) : FileComparator.FilePOJOComparate(Global.SORT, false));
            } else if (fileclickselected.equals("Duplicate Files")) {
                Collections.sort(filePOJO_list, Global.SORT.equals("d_name_desc") ? FileComparator.FilePOJOComparate(Global.SORT, false) : FileComparator.FilePOJOComparate("d_name_asc", false));
            } else {
                Collections.sort(filePOJO_list, viewModel.library_time_desc ? FileComparator.FilePOJOComparate("d_date_desc", false) : FileComparator.FilePOJOComparate(Global.SORT, false));
            }
            time_image_view.setSelected(viewModel.library_time_desc);
            size_image_view.setSelected(viewModel.library_size_desc);
            adapter.notifyDataSetChanged();

            if (file_list_size == 0) {
                recyclerView.setVisibility(View.GONE);
                folder_empty.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                folder_empty.setVisibility(View.GONE);
            }
        }
    }

    private boolean check_SAF_permission(String new_file_path, FileObjectType fileObjectType) {
        UriPOJO uriPOJO = Global.CHECK_AVAILABILITY_URI_PERMISSION(new_file_path, fileObjectType);
        if (uriPOJO != null) {
            tree_uri_path = uriPOJO.get_path();
            tree_uri = uriPOJO.get_uri();
        }

        if (uriPOJO == null || tree_uri_path.isEmpty()) {
            SAFPermissionHelperDialog safpermissionhelper = SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE, new_file_path, fileObjectType);
            safpermissionhelper.show(getParentFragmentManager(), "saf_permission_dialog");
            return false;
        } else {
            return true;
        }
    }

    public boolean check_availability_USB_SAF_permission(String file_path, FileObjectType fileObjectType) {
        if (!UsbFileRootSingleton.getInstance().isUsbFileRootSet()) {
            return false;
        }
        UriPOJO uriPOJO = Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path, fileObjectType);
        if (uriPOJO != null) {
            tree_uri_path = uriPOJO.get_path();
            tree_uri = uriPOJO.get_uri();
        }

        if (uriPOJO == null || tree_uri_path.isEmpty()) {
            SAFPermissionHelperDialog safpermissionhelper = SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE, file_path, fileObjectType);
            safpermissionhelper.show(getParentFragmentManager(), "saf_permission_dialog");
            return false;
        } else {
            return true;
        }
    }

    public class FilePathRecyclerViewAdapter extends RecyclerView.Adapter<FilePathRecyclerViewAdapter.ViewHolder> {
        final String[] filepath_string_array;
        final String display_path;
        final String truncated_path;

        FilePathRecyclerViewAdapter(String p) {
            truncated_path = p;
            display_path = p;

            if (p.equals(File.separator)) {
                filepath_string_array = new String[]{""};
            } else {
                filepath_string_array = display_path.split("/");
            }
        }

        @Override
        public DetailFragment.FilePathRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            return new ViewHolder((FrameLayout) LayoutInflater.from(context).inflate(R.layout.filepath_recyclerview_layout, p1, false));
        }

        @Override
        public void onBindViewHolder(DetailFragment.FilePathRecyclerViewAdapter.ViewHolder p1, int p2) {
            if (filepath_string_array[p2].isEmpty()) {
                p1.file_path_string_tv.setText(File.separator);
            } else {
                p1.file_path_string_tv.setText(filepath_string_array[p2]);
            }
        }

        @Override
        public int getItemCount() {
            return filepath_string_array.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final FrameLayout fl;
            final TextView file_path_string_tv;

            ViewHolder(FrameLayout fl) {
                super(fl);
                this.fl = fl;
                file_path_string_tv = fl.findViewById(R.id.filepath_recyclerview_TextView);
                this.fl.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        int p2 = getBindingAdapterPosition();
                        StringBuilder file_path;
                        file_path = new StringBuilder();
                        for (int i = 1; i <= p2; ++i) {
                            file_path.append(File.separator).append(filepath_string_array[i]);
                        }

                        if (fileObjectType == FileObjectType.FILE_TYPE) {
                            String fp = file_path.toString();
                            File f = new File(fp);
                            if (f.exists() && f.canRead()) {
                                if (detailFragmentListener != null) {
                                    detailFragmentListener.createFragmentTransaction(fp, fileObjectType);
                                }
                            }
                        } else if (fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                            if (p2 == 0) {
                                if (progress_bar.getVisibility() != View.VISIBLE && viewModel.library_filter_path != null) {
                                    viewModel.library_filter_path = null;
                                    filepath_adapter = new FilePathRecyclerViewAdapter(fileclickselected);
                                    filepath_recyclerview.setAdapter(filepath_adapter);
                                    adapter.getFilter().filter(null);
                                }
                            }
                        } else if (fileObjectType == FileObjectType.USB_TYPE) {
                            if (!UsbFileRootSingleton.getInstance().isUsbFileRootSet()) {
                                return;
                            }
                            if (p2 == 0) {
                                file_path.append(File.separator);
                            }
                            String fp = file_path.toString();
                            if (detailFragmentListener != null) {
                                detailFragmentListener.createFragmentTransaction(fp, fileObjectType);
                            }
                        } else if (fileObjectType == FileObjectType.FTP_TYPE || fileObjectType == FileObjectType.SFTP_TYPE || fileObjectType == FileObjectType.WEBDAV_TYPE || fileObjectType == FileObjectType.SMB_TYPE) {
                            String fp = file_path.toString();
                            if (fp.isEmpty()) {
                                fp = File.separator;
                            }
                            if (detailFragmentListener != null) {
                                detailFragmentListener.createFragmentTransaction(fp, fileObjectType);
                            }
                        }
                    }
                });
            }
        }
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher_unknown_package_install_permission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (clicked_filepojo != null) {
                    file_open_intent_dispatch(clicked_filepojo.getPath(), clicked_filepojo.getFileObjectType(), clicked_filepojo.getName(), false, clicked_filepojo.getSizeLong());
                }
                clicked_filepojo = null;
            } else {
                Global.print(context, getString(R.string.permission_not_granted));
            }
        }
    });
}

