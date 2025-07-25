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
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import me.jahnen.libaums.core.fs.UsbFile;

public class StorageAnalyserFragment extends Fragment implements FileModifyObserver.FileObserverListener {
    private final static String SAF_PERMISSION_REQUEST_CODE = "storage_analyser_dialog_saf_permission_request_code";
    public StorageAnalyserActivity.AbstractStorageAnalyserAdapter adapter;
    public List<FilePOJO> filePOJO_list, totalFilePOJO_list;
    public int totalFilePOJO_list_Size;
    public String fileclickselected;
    public FileObjectType fileObjectType;
    public UsbFile currentUsbFile;
    public TextView folder_selected_textview;
    public boolean local_activity_delete, modification_observed;
    public int file_list_size;
    public boolean is_toolbar_visible = true;
    public FrameLayout progress_bar;
    public FilePOJOViewModel viewModel;
    public DetailFragmentListener detailFragmentListener;
    public RecyclerView recycler_view;
    public TextView folder_empty_textview;
    public FilePOJO clicked_filepojo;
    private Context context;
    private FileModifyObserver fileModifyObserver;
    private Uri tree_uri;
    private String tree_uri_path = "";

    public static StorageAnalyserFragment getInstance(FileObjectType fileObjectType) {
        StorageAnalyserFragment storageAnalyserFragment = new StorageAnalyserFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("fileObjectType", fileObjectType);
        storageAnalyserFragment.setArguments(bundle);
        return storageAnalyserFragment;
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_file_selector, container, false);
        fileModifyObserver = FileModifyObserver.getInstance(fileclickselected);
        fileModifyObserver.setFileObserverListener(this);

        folder_selected_textview = v.findViewById(R.id.file_selector_folder_selected);
        folder_selected_textview.setVisibility(View.GONE);
        recycler_view = v.findViewById(R.id.file_selectorRecyclerView);
        FastScrollerView fastScroller = v.findViewById(R.id.fastScroller_file_selector);
        fastScroller.setRecyclerView(recycler_view);
        folder_empty_textview = v.findViewById(R.id.file_selector_folder_empty);
        progress_bar = v.findViewById(R.id.file_selector_progressbar);

        LinearLayoutManager llm = new LinearLayoutManager(context);
        recycler_view.setLayoutManager(llm);
        recycler_view.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        viewModel = new ViewModelProvider(this).get(FilePOJOViewModel.class);
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        if (!repositoryClass.hashmap_file_pojo.containsKey(fileObjectType + fileclickselected)) {
            if (fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                if (fileclickselected.equals("Large Files") || fileclickselected.equals("Duplicate Files")) {
                    viewModel.getLibraryList(fileclickselected);
                }

            } else {
                viewModel.populateFilePOJO(fileObjectType, fileclickselected, currentUsbFile, false, true);
            }

        } else {
            viewModel.filePOJOS = repositoryClass.hashmap_file_pojo.get(fileObjectType + fileclickselected);
            viewModel.filePOJOS_filtered = repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType + fileclickselected);
            if (fileclickselected.equals("Large Files") || fileclickselected.equals("Duplicate Files")) {
                after_filledFilePojos_procedure();
            } else {
                if (!viewModel.filePOJOS.isEmpty() && (viewModel.filePOJOS.get(0).getTotalSizePercentage() == null || viewModel.filePOJOS.get(viewModel.filePOJOS.size() - 1).getTotalSizePercentage() == null)) {
                    viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                    viewModel.fill_filePOJOs_size(fileObjectType, fileclickselected, currentUsbFile);
                } else {
                    after_filledFilePojos_procedure();
                }
            }
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
            if (detailFragmentListener != null) {
                detailFragmentListener.actionModeFinish(this, fileclickselected);
            }

            modification_observed = false;
            local_activity_delete = false;
            progress_bar.setVisibility(View.VISIBLE);
            if (fileclickselected.equals("Large Files") || fileclickselected.equals("Duplicate Files")) {
                after_filledFilePojos_procedure();
            } else {
                viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                viewModel.populateFilePOJO(fileObjectType, fileclickselected, currentUsbFile, false, true);
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
        totalFilePOJO_list = viewModel.filePOJOS;
        filePOJO_list = viewModel.filePOJOS;
        totalFilePOJO_list_Size = totalFilePOJO_list.size();
        file_list_size = filePOJO_list.size();
        if (detailFragmentListener != null) {
            detailFragmentListener.setCurrentDirText(new File(fileclickselected).getName());
            detailFragmentListener.setFileNumberView(viewModel.mselecteditems.size() + "/" + file_list_size);
        }

        if (fileclickselected.equals("Duplicate Files")) {
            Collections.sort(filePOJO_list, Global.STORAGE_ANALYSER_SORT.equals("d_name_desc") ? FileComparator.FilePOJOComparate(Global.STORAGE_ANALYSER_SORT, false) : FileComparator.FilePOJOComparate("d_name_asc", false));
        } else {
            Collections.sort(filePOJO_list, FileComparator.FilePOJOComparate(StorageAnalyserActivityViewModel.SORT, true));
        }

        if (fileclickselected.equals("Duplicate Files")) {
            adapter = new StorageAnalyserActivity.StorageAnalyserAdapterDivider(context, this);
        } else {
            adapter = new StorageAnalyserActivity.StorageAnalyserAdapter(context, this);
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

    public void notifyDataSetChanged() {
        after_filledFilePojos_procedure();
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


    public void file_open_intent_dispatch(final String file_path, final FileObjectType fileObjectType, String file_name, boolean select_app, long file_size) {
        int idx = file_name.lastIndexOf(".");
        String file_ext = "";
        if (idx > 0) {
            file_ext = file_name.substring(idx + 1);
        }

        if (file_ext.isEmpty() || Global.NO_APPS_FOR_RECOGNISED_FILE_EXT(context, file_ext)) {
            FileTypeSelectDialog fileTypeSelectFragment = FileTypeSelectDialog.getInstance(file_path, fileObjectType, tree_uri, tree_uri_path, select_app, file_size);
            fileTypeSelectFragment.show(getParentFragmentManager(), "");
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
            FileIntentDispatch.openFile(context, file_path, "", fileObjectType, select_app, file_size, false);
        }
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
            if (!viewModel.filePOJOS.isEmpty() && viewModel.filePOJOS.get(0).getTotalSizePercentage() == null && !fileclickselected.equals("Large Files") && !fileclickselected.equals("Duplicate Files")) {
                progress_bar.setVisibility(View.VISIBLE);
                viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                viewModel.fill_filePOJOs_size(fileObjectType, fileclickselected, currentUsbFile);
            } else {
                if (detailFragmentListener != null) {
                    detailFragmentListener.setCurrentDirText(new File(fileclickselected).getName());
                    detailFragmentListener.setFileNumberView(viewModel.mselecteditems.size() + "/" + file_list_size);
                }
                if (fileclickselected.equals("Duplicate Files")) {
                    Collections.sort(filePOJO_list, Global.STORAGE_ANALYSER_SORT.equals("d_name_desc") ? FileComparator.FilePOJOComparate(Global.STORAGE_ANALYSER_SORT, false) : FileComparator.FilePOJOComparate("d_name_asc", false));
                } else {
                    Collections.sort(filePOJO_list, FileComparator.FilePOJOComparate(StorageAnalyserActivityViewModel.SORT, true));
                }

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
    }

    public void clear_cache_and_refresh(String file_path, FileObjectType fileObjectType) {
        viewModel.mselecteditems = new IndexedLinkedHashMap<>();
        if (detailFragmentListener != null) {
            detailFragmentListener.clearCache(file_path, fileObjectType);
        }

        modification_observed = true;
        Global.WORKOUT_AVAILABLE_SPACE();
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



