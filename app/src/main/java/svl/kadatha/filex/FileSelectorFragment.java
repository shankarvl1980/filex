package svl.kadatha.filex;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import me.jahnen.libaums.core.fs.UsbFile;
import svl.kadatha.filex.usb.ReadAccess;
import svl.kadatha.filex.usb.UsbFileRootSingleton;

public class FileSelectorFragment extends Fragment implements FileModifyObserver.FileObserverListener {
    private final static String SAF_PERMISSION_REQUEST_CODE = "file_selector_dialog_saf_permission_request_code";
    public FileSelectorActivity.FileSelectorAdapter adapter;
    public List<FilePOJO> filePOJO_list, totalFilePOJO_list;
    public int totalFilePOJO_list_Size;
    public String fileclickselected;
    public FileObjectType fileObjectType;
    public UsbFile currentUsbFile;
    public TextView folder_selected_textview;
    public boolean local_activity_delete, modification_observed;
    public int file_list_size;
    public FrameLayout progress_bar;
    public FilePOJOViewModel viewModel;
    public DetailFragmentListener detailFragmentListener;
    public GridLayoutManager glm;
    public LinearLayoutManager llm;
    public RecyclerView recycler_view;
    public TextView folder_empty_textview;
    private Context context;
    private FileModifyObserver fileModifyObserver;
    private Uri tree_uri;
    private String tree_uri_path = "";
    private int action_sought_request_code;

    public static FileSelectorFragment getInstance(FileObjectType fileObjectType, int action_sought_request_code) {
        FileSelectorFragment fileSelectorFragment = new FileSelectorFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("fileObjectType", fileObjectType);
        bundle.putInt("action_sought_request_code", action_sought_request_code);
        fileSelectorFragment.setArguments(bundle);
        return fileSelectorFragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
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
        fileclickselected = getTag();
        if (fileclickselected == null) {
            fileclickselected = Global.INTERNAL_PRIMARY_STORAGE_PATH;
        }
        Bundle bundle = getArguments();
        if (bundle != null) {
            fileObjectType = (FileObjectType) bundle.getSerializable("fileObjectType");
            action_sought_request_code = bundle.getInt("action_sought_request_code", 0);
        }

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


        if (fileObjectType == FileObjectType.USB_TYPE) {
            try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
                UsbFile usbFileRoot = access.getUsbFile();
                try {
                    currentUsbFile = usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(fileclickselected));
                } catch (IOException e) {

                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_file_selector, container, false);
        fileModifyObserver = FileModifyObserver.getInstance(fileclickselected);
        fileModifyObserver.setFileObserverListener(this);

        TextView current_folder_label = v.findViewById(R.id.file_selector_current_folder_label);
        current_folder_label.setText(R.string.current_folder);
        folder_selected_textview = v.findViewById(R.id.file_selector_folder_selected);

        recycler_view = v.findViewById(R.id.file_selectorRecyclerView);
        FastScrollerView fastScroller = v.findViewById(R.id.fastScroller_file_selector);
        fastScroller.setRecyclerView(recycler_view);
        folder_empty_textview = v.findViewById(R.id.file_selector_folder_empty);
        progress_bar = v.findViewById(R.id.file_selector_progressbar);

        if (FileSelectorActivity.FILE_GRID_LAYOUT) {
            glm = new GridLayoutManager(context, FileSelectorActivity.GRID_COUNT);
            recycler_view.setLayoutManager(glm);
            int top_padding = recycler_view.getPaddingTop();
            int bottom_padding = recycler_view.getPaddingBottom();
            recycler_view.setPadding(Global.RECYCLERVIEW_ITEM_SPACING, top_padding, Global.RECYCLERVIEW_ITEM_SPACING, bottom_padding);
        } else {
            llm = new LinearLayoutManager(context);
            recycler_view.setLayoutManager(llm);
        }
        ItemSeparatorDecoration dividerItemDecoration = new ItemSeparatorDecoration(context, 1, recycler_view);
        recycler_view.addItemDecoration(dividerItemDecoration);

        folder_selected_textview.setText(fileclickselected);

        viewModel = new ViewModelProvider(this).get(FilePOJOViewModel.class);
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        if (!repositoryClass.hashmap_file_pojo.containsKey(fileObjectType + fileclickselected)) {
            viewModel.populateFilePOJO(fileObjectType, fileclickselected, currentUsbFile, false, false);
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

        getParentFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(SAF_PERMISSION_REQUEST_CODE)) {
                    tree_uri = result.getParcelable("tree_uri");
                    tree_uri_path = result.getString("tree_uri_path");
                }
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (local_activity_delete) {
            clearSelectionAndNotifyDataSetChanged();
        } else if (modification_observed) {
            modification_observed = false;
            local_activity_delete = false;
            progress_bar.setVisibility(View.VISIBLE);
            viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
            viewModel.populateFilePOJO(fileObjectType, fileclickselected, currentUsbFile, false, false);

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
        if (FileSelectorActivity.SHOW_HIDDEN_FILE) {
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


        Collections.sort(filePOJO_list, FileComparator.FilePOJOComparate(FileSelectorActivity.SORT, false));
        if (FileSelectorActivity.FILE_GRID_LAYOUT) {
            adapter = new FileSelectorActivity.FileSelectorAdapterGrid(context, this, action_sought_request_code);
        } else {
            adapter = new FileSelectorActivity.FileSelectorAdapterList(context, this, action_sought_request_code);
        }

        set_adapter();
        progress_bar.setVisibility(View.GONE);
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
    }


    @Override
    public void onFileModified() {
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION, LocalBroadcastManager.getInstance(context), null);
    }

    private void set_adapter() {
        recycler_view.setAdapter(adapter);
        if (file_list_size == 0) {
            recycler_view.setVisibility(View.GONE);
            folder_empty_textview.setVisibility(View.VISIBLE);
        } else {
            recycler_view.setVisibility(View.VISIBLE);
            folder_empty_textview.setVisibility(View.GONE);
        }
    }

    public void clearSelectionAndNotifyDataSetChanged() {
        viewModel.mselecteditems = new IndexedLinkedHashMap<>();
        if (adapter != null) {
            modification_observed = false;
            local_activity_delete = false;
            totalFilePOJO_list_Size = totalFilePOJO_list.size();
            file_list_size = filePOJO_list.size();

            if (detailFragmentListener != null) {
                detailFragmentListener.setFileNumberView(viewModel.mselecteditems.size() + "/" + file_list_size);
            }

            Collections.sort(filePOJO_list, FileComparator.FilePOJOComparate(FileSelectorActivity.SORT, false));
            adapter.notifyDataSetChanged();
            if (file_list_size == 0) {
                recycler_view.setVisibility(View.GONE);
                folder_empty_textview.setVisibility(View.VISIBLE);
            } else {
                recycler_view.setVisibility(View.VISIBLE);
                folder_empty_textview.setVisibility(View.GONE);
            }
        }
    }

    public void clear_cache_and_refresh(String file_path, FileObjectType fileObjectType) {
        if (detailFragmentListener != null) {
            detailFragmentListener.clearCache(file_path, fileObjectType);
        }

        modification_observed = true;
        Global.WORKOUT_AVAILABLE_SPACE();
    }

    private boolean check_availability_USB_SAF_permission(String file_path, FileObjectType fileObjectType) {
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
}



