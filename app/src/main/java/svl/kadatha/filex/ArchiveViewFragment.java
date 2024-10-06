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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ArchiveViewFragment extends Fragment implements FileModifyObserver.FileObserverListener {
    private final static String SAF_PERMISSION_REQUEST_CODE = "archive_detail_fragment_saf_permission_request_code";
    public List<FilePOJO> filePOJO_list, totalFilePOJO_list;
    public int totalFilePOJO_list_Size;
    public RecyclerView filepath_recyclerview;
    public RecyclerView recyclerView;
    public ImageView time_image_view;
    public String fileclickselected = "";
    public String file_click_selected_name = "";
    public ArchiveViewActivity archiveViewActivity;
    public FileObjectType fileObjectType;
    public int file_list_size;
    public boolean local_activity_delete, modification_observed;
    public FrameLayout progress_bar;
    public FilePOJOViewModel viewModel;
    LinearLayoutManager llm;
    GridLayoutManager glm;
    TextView folder_empty;
    ArchiveViewActivity.ArchiveDetailRecyclerViewAdapter adapter;
    boolean is_toolbar_visible = true;
    private FilePathRecyclerViewAdapter filepath_adapter;
    private Context context;
    private Uri tree_uri;
    private String tree_uri_path = "";
    private FileModifyObserver fileModifyObserver;
    private FilePOJO clicked_filepojo;
    private ExtractZipFileViewModel extractZipFileViewModel;

    public static ArchiveViewFragment getInstance(FileObjectType fileObjectType) {
        ArchiveViewFragment archiveViewFragment = new ArchiveViewFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("fileObjectType", fileObjectType);
        archiveViewFragment.setArguments(bundle);
        return archiveViewFragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        archiveViewActivity = (ArchiveViewActivity) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        archiveViewActivity = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        fileObjectType = (FileObjectType) bundle.getSerializable("fileObjectType");
        fileclickselected = getTag();

        file_click_selected_name = new File(fileclickselected).getName();

        if (Global.ARCHIVE_EXTRACT_DIR == null)
            Global.ARCHIVE_EXTRACT_DIR = new File(context.getFilesDir(), "Archive");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_archive_viewer, container, false);
        fileModifyObserver = FileModifyObserver.getInstance(fileclickselected);
        fileModifyObserver.setFileObserverListener(this);
        filepath_recyclerview = v.findViewById(R.id.fragment_archive_filepath_container);

        progress_bar = v.findViewById(R.id.fragment_archive_detail_progressbar);

        recyclerView = v.findViewById(R.id.fragment_archive_detail_container);
        DividerItemDecoration itemdecor = new DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL);
        itemdecor.setDrawable(ContextCompat.getDrawable(context, R.drawable.right_private_icon));
        filepath_recyclerview.addItemDecoration(itemdecor);
        LinearLayoutManager file_path_lm = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        filepath_recyclerview.setLayoutManager(file_path_lm);

        if (Global.FILE_GRID_LAYOUT) {
            glm = new GridLayoutManager(context, Global.GRID_COUNT);
            recyclerView.setLayoutManager(glm);
            int top_padding = recyclerView.getPaddingTop();
            int bottom_padding = recyclerView.getPaddingBottom();
            recyclerView.setPadding(Global.RECYCLERVIEW_ITEM_SPACING, top_padding, Global.RECYCLERVIEW_ITEM_SPACING, bottom_padding);
        } else {
            llm = new LinearLayoutManager(context);
            recyclerView.setLayoutManager(llm);
        }
        ItemSeparatorDecoration itemSeparatorDecoration = new ItemSeparatorDecoration(context, 1, recyclerView);
        recyclerView.addItemDecoration(itemSeparatorDecoration);
        FastScrollerView fastScroller = v.findViewById(R.id.fastScroller_archive_detail_fragment);
        fastScroller.setRecyclerView(recyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            final int threshold = 5;
            int scroll_distance = 0;

            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (scroll_distance > threshold && is_toolbar_visible) {
                    archiveViewActivity.bottom_toolbar.animate().translationY(archiveViewActivity.bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    is_toolbar_visible = false;
                    scroll_distance = 0;
                } else if (scroll_distance < -threshold && !is_toolbar_visible) {
                    archiveViewActivity.bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    is_toolbar_visible = true;
                    scroll_distance = 0;
                }

                if ((is_toolbar_visible && dy > 0) || (!is_toolbar_visible && dy < 0)) {
                    scroll_distance += dy;
                }
            }
        });

        folder_empty = v.findViewById(R.id.fragment_archive_empty_folder);
        filepath_adapter = new FilePathRecyclerViewAdapter(fileclickselected);
        viewModel = new ViewModelProvider(this).get(FilePOJOViewModel.class);
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        if (!repositoryClass.hashmap_file_pojo.containsKey(fileObjectType + fileclickselected)) {
            viewModel.populateFilePOJO(fileObjectType, fileclickselected, null, true, false);
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

        extractZipFileViewModel = new ViewModelProvider(ArchiveViewFragment.this).get(ExtractZipFileViewModel.class);
        extractZipFileViewModel.asyncTaskStatus.observe(getViewLifecycleOwner(), new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (extractZipFileViewModel.isZipExtracted) {
                        file_open_intent_dispatch(extractZipFileViewModel.filePOJO.getPath(), extractZipFileViewModel.filePOJO.getFileObjectType(), extractZipFileViewModel.filePOJO.getName(), false, extractZipFileViewModel.filePOJO.getSizeLong());
                    }
                    extractZipFileViewModel.isZipExtracted = false;
                    extractZipFileViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (local_activity_delete) {
            modification_observed = false;
            local_activity_delete = false;
            if (MainActivity.SHOW_HIDDEN_FILE) {
                filePOJO_list = viewModel.filePOJOS;
                totalFilePOJO_list = viewModel.filePOJOS;
            } else {
                filePOJO_list = viewModel.filePOJOS_filtered;
                totalFilePOJO_list = viewModel.filePOJOS_filtered;
            }
            totalFilePOJO_list_Size = totalFilePOJO_list.size();
            file_list_size = totalFilePOJO_list_Size;
            archiveViewActivity.file_number_view.setText(viewModel.mselecteditems.size() + "/" + file_list_size);
            Collections.sort(filePOJO_list, viewModel.library_time_desc ? FileComparator.FilePOJOComparate("d_date_desc", false) : FileComparator.FilePOJOComparate(Global.SORT, false));
            time_image_view.setSelected(viewModel.library_time_desc);
            adapter.notifyDataSetChanged();
        } else if (modification_observed) {
            archiveViewActivity.actionmode_finish(this, fileclickselected);
            modification_observed = false;
            local_activity_delete = false;
            progress_bar.setVisibility(View.VISIBLE);
            viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
            viewModel.populateFilePOJO(fileObjectType, fileclickselected, null, true, false);
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
        file_list_size = totalFilePOJO_list_Size;
        Collections.sort(filePOJO_list, viewModel.library_time_desc ? FileComparator.FilePOJOComparate("d_date_desc", false) : FileComparator.FilePOJOComparate(Global.SORT, false));
        adapter = new ArchiveViewActivity.ArchiveDetailRecyclerViewAdapter(context);
        set_adapter();
        progress_bar.setVisibility(View.GONE);
        archiveViewActivity.file_number_view.setText(viewModel.mselecteditems.size() + "/" + file_list_size);
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
        if (adapter != null) adapter.setCardViewClickListener(null);
    }

    @Override
    public void onFileModified() {
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION, LocalBroadcastManager.getInstance(context), ArchiveViewActivity.ACTIVITY_NAME);
    }

    public void file_open_intent_dispatch(final String file_path, final FileObjectType fileObjectType, String file_name, boolean select_app, long file_size) {
        int idx = file_name.lastIndexOf(".");
        String file_ext = "";
        if (idx != -1) {
            file_ext = file_name.substring(idx + 1);
        }

        if (file_ext.matches("(?i)zip")) {
            Global.print(context, getString(R.string.can_not_open_file));
            return;
        }

        if (file_ext.isEmpty() || !Global.CHECK_APPS_FOR_RECOGNISED_FILE_EXT(context, file_ext)) {
            FileTypeSelectDialog fileTypeSelectDialog = FileTypeSelectDialog.getInstance(file_path, null, tree_uri, tree_uri_path, select_app, file_size);
            fileTypeSelectDialog.show(archiveViewActivity.fm, "");
        } else {
            if (file_ext.matches("(?i)apk")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!archiveViewActivity.getPackageManager().canRequestPackageInstalls()) {
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
                FileIntentDispatch.openFile(context, file_path, "", fileObjectType, false, file_size,true);
            } else if (Global.whether_file_cached(fileObjectType)) {
                if (file_size > Global.CACHE_FILE_MAX_LIMIT) {
                    Global.print(context, context.getString(R.string.file_is_large_copy_to_device_storage));
                    return;
                }
                FileIntentDispatch.openFile(context, file_path, "",  fileObjectType, false, file_size,true);
            } else if (fileObjectType == FileObjectType.FILE_TYPE) {
                FileIntentDispatch.openFile(context, file_path, "", fileObjectType, false, file_size,true);
            }
        }
    }

    public void set_adapter() {
        filepath_recyclerview.setAdapter(filepath_adapter);
        filepath_recyclerview.scrollToPosition(filepath_adapter.getItemCount() - 1);
        recyclerView.setAdapter(adapter);
        if (file_list_size == 0) {
            recyclerView.setVisibility(View.GONE);
            folder_empty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            folder_empty.setVisibility(View.GONE);
        }

        adapter.setCardViewClickListener(new ArchiveViewActivity.ArchiveDetailRecyclerViewAdapter.CardViewClickListener() {
            public void onClick(FilePOJO filePOJO) {
                clicked_filepojo = filePOJO;
                if (filePOJO.getIsDirectory()) {
                    archiveViewActivity.createFragmentTransaction(filePOJO.getPath(), filePOJO.getFileObjectType());
                } else {
                    int idx = filePOJO.getName().lastIndexOf(".");
                    if (idx != -1) {
                        String file_ext = filePOJO.getName().substring(idx + 1);
                        if (file_ext.matches("(?i)zip")) {
                            Global.print(context, getString(R.string.can_not_open_file));
                            return;
                        }
                    }

                    try {
                        ZipFile zipfile = new ZipFile(ArchiveViewActivity.ZIP_FILE);
                        ZipEntry zip_entry = zipfile.getEntry(filePOJO.getPath().substring(Global.ARCHIVE_CACHE_DIR_LENGTH + 1));
                        if (zip_entry == null) {
                            Global.print(context, getString(R.string.can_not_open_file));
                            return;
                        }

                        if (zip_entry.getSize() > Global.CACHE_FILE_MAX_LIMIT) {
                            Global.print(context, getString(R.string.file_is_large_please_extract_to_view));
                            return;
                        }

                        progress_bar.setVisibility(View.VISIBLE);
                        extractZipFileViewModel.extractZip(filePOJO, zipfile, zip_entry);
                    } catch (IOException e) {
                    }
                }
            }

            public void onLongClick(FilePOJO filePOJO) {
                archiveViewActivity.bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                is_toolbar_visible = true;
            }
        });
    }

    public void clearSelectionAndNotifyDataSetChanged() {
        viewModel.mselecteditems = new IndexedLinkedHashMap<>();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            file_list_size = filePOJO_list.size();
            archiveViewActivity.file_number_view.setText(viewModel.mselecteditems.size() + "/" + file_list_size);
            totalFilePOJO_list_Size = totalFilePOJO_list.size();

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
    }    private final ActivityResultLauncher<Intent> activityResultLauncher_unknown_package_install_permission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (clicked_filepojo != null)
                    file_open_intent_dispatch(clicked_filepojo.getPath(), clicked_filepojo.getFileObjectType(), clicked_filepojo.getName(), false, clicked_filepojo.getSizeLong());
                clicked_filepojo = null;
            } else {
                Global.print(context, getString(R.string.permission_not_granted));
            }
        }
    });

    private class FilePathRecyclerViewAdapter extends RecyclerView.Adapter<FilePathRecyclerViewAdapter.ViewHolder> {
        final String[] filepath_string_array;
        String display_path;
        String truncated_path;

        FilePathRecyclerViewAdapter(String p) {
            truncated_path = p;
            display_path = p;
            display_path = display_path.substring(Global.ARCHIVE_CACHE_DIR_LENGTH);
            display_path = "Archive" + display_path;
            truncated_path = truncated_path.substring(0, Global.ARCHIVE_CACHE_DIR_LENGTH - "Archive/".length());//number added to archive_cache_dir_length is length of "Archive/"

            if (p.equals(File.separator)) {
                filepath_string_array = new String[]{""};
            } else {
                filepath_string_array = display_path.split(File.separator);
            }
        }

        @Override
        public FilePathRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            return new ViewHolder((FrameLayout) LayoutInflater.from(context).inflate(R.layout.filepath_recyclerview_layout, p1, false));
        }

        @Override
        public void onBindViewHolder(FilePathRecyclerViewAdapter.ViewHolder p1, int p2) {
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
                        file_path = new StringBuilder(truncated_path);
                        for (int i = 0; i <= p2; ++i) {
                            file_path.append(File.separator).append(filepath_string_array[i]);
                        }


                        if (fileObjectType == FileObjectType.FILE_TYPE) {
                            String fp = file_path.toString();
                            File f = new File(fp);
                            if (f.exists() && f.list() != null) {
                                archiveViewActivity.createFragmentTransaction(fp, fileObjectType);
                            }
                        }
                    }
                });
            }
        }
    }




}

