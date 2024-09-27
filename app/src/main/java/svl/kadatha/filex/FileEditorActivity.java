package svl.kadatha.filex;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.os.EnvironmentCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import timber.log.Timber;


public class FileEditorActivity extends BaseActivity implements FileEditorSettingsDialog.EOL_ChangeListener {
    public static final int EOL_N = 0;
    public static final int EOL_R = 1;
    public static final int EOL_RN = 2;
    public static final String ACTIVITY_NAME = "FILE_EDITOR_ACTIVITY";
    private static final String DELETE_FILE_REQUEST_CODE = "text_file_delete_request_code";
    private final static String SAF_PERMISSION_REQUEST_CODE = "file_editor_saf_permission_request_code";
    private final static String SAVE_CONFIRMATION_REQUEST_CODE = "file_editor_save_confirmation_request_code";
    static boolean NOT_WRAP = true;
    static float FILE_EDITOR_TEXT_SIZE;
    static int LINE_NUMBER_SIZE;
    public FragmentManager fm;
    public boolean clear_cache;
    public FileEditorViewModel viewModel;
    FileSaveServiceConnection serviceConnection;
    LineNumberedEditText filetext_container_edittext;
    TinyDB tinyDB;
    private List<FilePOJO> files_selected_for_delete;
    private String tree_uri_path = "";
    private Uri tree_uri;
    private Button edit_button, undo_button, redo_button, save_button, up_button, down_button;
    private TextView file_name, page_number;
    private SaveFileConfirmationDialog saveConfirmationAlertDialog;
    private svl.kadatha.filex.ObservableScrollView scrollview;
    private FileEditorSettingsDialog fileEditorSettingsDialog;
    private Context context;
    private Class emptyService;
    private KeyBoardUtil keyBoardUtil;
    private PopupWindow listPopWindow;
    private LocalBroadcastManager localBroadcastManager;
    private InputMethodManager imm;
    private FrameLayout progress_bar;

    static Class getEmptyService() {
        Class emptyService = null;
        if (FileSaveService1.SERVICE_COMPLETED) {
            emptyService = FileSaveService1.class;
        } else if (FileSaveService2.SERVICE_COMPLETED) {
            emptyService = FileSaveService2.class;
        }
        return emptyService;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        fm = getSupportFragmentManager();
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        tinyDB = new TinyDB(context);

        NOT_WRAP = tinyDB.getBoolean("file_editor_not_wrap");
        if (NOT_WRAP) {
            setContentView(R.layout.activity_file_editor_horizontal_scroll);
        } else {
            setContentView(R.layout.activity_file_editor);
        }
        viewModel = new ViewModelProvider(this).get(FileEditorViewModel.class);

        FILE_EDITOR_TEXT_SIZE = tinyDB.getFloat("file_editor_text_size");
        if (FILE_EDITOR_TEXT_SIZE <= 0 || FILE_EDITOR_TEXT_SIZE > FileEditorSettingsDialog.MAX_TEXT_SIZE) {
            FILE_EDITOR_TEXT_SIZE = 16F;
            tinyDB.putFloat("file_editor_text_size", FILE_EDITOR_TEXT_SIZE);
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        LINE_NUMBER_SIZE = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());

        scrollview = findViewById(R.id.file_editor_scrollview);
        keyBoardUtil = new KeyBoardUtil(scrollview);

        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        progress_bar = findViewById(R.id.file_editor_progressbar);
        FloatingActionButton floating_back_button = findViewById(R.id.file_editor_floating_action_button_back);
        floating_back_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        EquallyDistributedButtonsWithTextLayout tb_layout = new EquallyDistributedButtonsWithTextLayout(this, 6, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] bottom_drawables = {R.drawable.edit_icon, R.drawable.undo_icon, R.drawable.redo_icon, R.drawable.save_icon, R.drawable.up_caret_icon, R.drawable.down_caret_icon};
        String[] titles = {getString(R.string.edit), getString(R.string.undo), getString(R.string.redo), getString(R.string.save), getString(R.string.up), getString(R.string.down)};
        tb_layout.setResourceImageDrawables(bottom_drawables, titles);

        Toolbar bottom_toolbar = findViewById(R.id.file_editor_bottom_toolbar);
        bottom_toolbar.addView(tb_layout);
        edit_button = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        undo_button = bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        redo_button = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        save_button = bottom_toolbar.findViewById(R.id.toolbar_btn_4);
        up_button = bottom_toolbar.findViewById(R.id.toolbar_btn_5);
        down_button = bottom_toolbar.findViewById(R.id.toolbar_btn_6);
        file_name = findViewById(R.id.file_editor_file_name_textview);
        ImageButton overflow = findViewById(R.id.file_editor_overflow_btn);
        overflow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View p1) {
                if (progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }
                listPopWindow.showAsDropDown(p1, 0, Global.LIST_POPUP_WINDOW_DROP_DOWN_OFFSET);
            }
        });

        ArrayList<ListPopupWindowPOJO> list_popupwindowpojos = new ArrayList<>();
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.delete_icon, getString(R.string.delete), 1));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon, getString(R.string.send), 2));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.copy_icon, getString(R.string.copy_to), 3));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon, getString(R.string.properties), 4));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.settings_icon, getString(R.string.settings), 5));

        listPopWindow = new PopupWindow(context);
        ListView listView = new ListView(context);
        listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapter(context, list_popupwindowpojos));
        listPopWindow.setContentView(listView);
        listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popup_window_width));
        listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopWindow.setFocusable(true);
        listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.list_popup_background));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View v, int p1, long p2) {
                final ArrayList<String> files_selected_array = new ArrayList<>();
                switch (p1) {
                    case 0:
                        if (viewModel.fromThirdPartyApp || Global.whether_file_cached(viewModel.fileObjectType)) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        files_selected_array.add(viewModel.currently_shown_file.getPath());
                        DeleteFileAlertDialogOtherActivity deleteFileAlertDialogOtherActivity = DeleteFileAlertDialogOtherActivity.getInstance(DELETE_FILE_REQUEST_CODE, files_selected_array, viewModel.fileObjectType);
                        deleteFileAlertDialogOtherActivity.show(fm, "deletefilealertotheractivity");
                        break;

                    case 1:
                        Uri src_uri = null;
                        if (viewModel.fromThirdPartyApp) {
                            src_uri = viewModel.data;
                        } else if (Global.whether_file_cached(viewModel.fileObjectType)) {
                            src_uri = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(viewModel.currently_shown_file.getPath()));
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
                        Uri copy_uri = null;
                        if (viewModel.fromThirdPartyApp) {
                            copy_uri = viewModel.data;
                        } else if (Global.whether_file_cached(viewModel.fileObjectType)) {
                            copy_uri = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", new File(viewModel.currently_shown_file.getPath()));
                        }
                        if (copy_uri == null) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        clear_cache = false;
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
                        if (viewModel.fromThirdPartyApp || Global.whether_file_cached(viewModel.fileObjectType)) {
                            Global.print(context, getString(R.string.not_able_to_process));
                            break;
                        }
                        files_selected_array.add(viewModel.currently_shown_file.getPath());
                        PropertiesDialog propertiesDialog = PropertiesDialog.getInstance(files_selected_array, viewModel.fileObjectType);
                        propertiesDialog.show(fm, "properties_dialog");
                        break;
                    case 4:
                        fileEditorSettingsDialog = FileEditorSettingsDialog.getInstance(viewModel.eol);
                        fileEditorSettingsDialog.show(fm, "file_editor_overflow");
                        break;
                    default:
                        break;
                }
                listPopWindow.dismiss();
            }
        });

        scrollview.setScrollViewListener(new ObservableScrollView.ScrollViewListener() {
            final int threshold = 5;
            boolean visible = true;
            int scroll_distance = 0;
            int dy = 0;

            public void onScrollChange(ObservableScrollView v, int old_scrollX, int old_scrollY, int scrollX, int scrollY) {
                dy = scrollY - old_scrollY;

                if (scroll_distance > threshold && !visible) {
                    visible = true;
                    scroll_distance = 0;
                } else if (scroll_distance < -threshold && visible) {
                    scroll_distance = 0;
                }
                if ((visible && dy < 0) || (!visible && dy > 0)) {
                    scroll_distance += dy;
                }
            }
        });

        BottomToolbarListener bottomToolbarListener = new BottomToolbarListener();
        edit_button.setOnClickListener(bottomToolbarListener);
        undo_button.setOnTouchListener(new RepeatListener(400, 101, bottomToolbarListener));
        redo_button.setOnTouchListener(new RepeatListener(400, 101, bottomToolbarListener));
        save_button.setOnClickListener(bottomToolbarListener);
        up_button.setOnClickListener(bottomToolbarListener);
        down_button.setOnClickListener(bottomToolbarListener);

        filetext_container_edittext = findViewById(R.id.textfile_edittext);
        filetext_container_edittext.setTextSize(FILE_EDITOR_TEXT_SIZE);

        page_number = findViewById(R.id.file_editor_page_number);

        viewModel.textViewUndoRedo = new TextViewUndoRedoBatch(filetext_container_edittext.getEditText());
        viewModel.textViewUndoRedo.setEditTextUndoRedoListener(new TextViewUndoRedoBatch.EditTextRedoUndoListener() {
            @Override
            public void onEditTextChange() {
                undo_button.setEnabled(true);
                undo_button.setAlpha(Global.ENABLE_ALFA);
                save_button.setEnabled(true);
                save_button.setAlpha(Global.ENABLE_ALFA);
                viewModel.updated = false;
                redo_button.setEnabled(false);
                redo_button.setAlpha(Global.DISABLE_ALFA);
            }
        });

        viewModel.initializedSetUp.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (viewModel.data != null) {
                        if (viewModel.file != null) {
                            file_name.setText(viewModel.file.getName());
                        }

                        if (!openFile(0, 1)) {
                            viewModel.textViewUndoRedo.disconnect();
                            clear_cache = false;
                            finish();
                        }

                        if (viewModel.file.exists()) {
                            long internal_available_space, external_available_space, file_size;
                            file_size = viewModel.file.length();
                            RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                            for (FilePOJO filePOJO : repositoryClass.storage_dir) {
                                if (filePOJO.getFileObjectType() != FileObjectType.FILE_TYPE) {
                                    continue;
                                }
                                if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(new File(filePOJO.getPath())))) {
                                    continue;
                                }

                                //if(filePOJO.getPath().endsWith("/0"))
                                {
                                    internal_available_space = new File(Global.INTERNAL_PRIMARY_STORAGE_PATH).getUsableSpace();
                                    if (file_size * 2.5 > internal_available_space) {
                                        viewModel.isFileBig = true;
                                    } else {
                                        //viewModel.temporary_file_for_save=getExternalFilesDir("file_save_temp");
                                        viewModel.isFileBig = false;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    viewModel.initializedSetUp.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        viewModel.isReadingFinished.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (!viewModel.fileRead) {
                        viewModel.file_start = viewModel.file_end = true;
                        Global.print(context, getString(R.string.file_could_not_be_opened));
                    }
                    viewModel.file_format_supported = viewModel.fileRead;
                    viewModel.textViewUndoRedo.clearHistory();
                    viewModel.textViewUndoRedo.stopListening();

                    edit_button.setSelected(viewModel.edit_mode);

                    undo_button.setEnabled(false);
                    undo_button.setAlpha(Global.DISABLE_ALFA);
                    redo_button.setEnabled(false);
                    redo_button.setAlpha(Global.DISABLE_ALFA);

                    if (viewModel.file_start) {
                        up_button.setEnabled(false);
                        up_button.setAlpha(Global.DISABLE_ALFA);
                    } else {
                        up_button.setEnabled(true);
                        up_button.setAlpha(Global.ENABLE_ALFA);
                    }

                    if (viewModel.file_end) {
                        down_button.setEnabled(false);
                        down_button.setAlpha(Global.DISABLE_ALFA);
                    } else {
                        down_button.setEnabled(true);
                        down_button.setAlpha(Global.ENABLE_ALFA);
                    }
                    viewModel.updated = true;
                    page_number.setText(String.valueOf(viewModel.current_page));
                    filetext_container_edittext.setContent(viewModel.stringBuilder.toString(), viewModel.current_page, FileEditorViewModel.MAX_LINES_TO_DISPLAY);
                    scrollview.smoothScrollTo(0, 0);
                    filetext_container_edittext.setEditable(false);
                    onClick_edit_button();
                    viewModel.textViewUndoRedo.startListening();
                }
            }
        });

        viewModel.saveContentInTempFile.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (viewModel.whether_temp_content_saved) {
                        viewModel.edit_mode = false;
                        start_file_save_service();
                    }
                    viewModel.saveContentInTempFile.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        DeleteFileOtherActivityViewModel deleteFileOtherActivityViewModel = new ViewModelProvider(FileEditorActivity.this).get(DeleteFileOtherActivityViewModel.class);
        deleteFileOtherActivityViewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    progress_bar.setVisibility(View.GONE);
                    if (!deleteFileOtherActivityViewModel.deleted_files.isEmpty()) {
                        viewModel.textViewUndoRedo.disconnect();
                        clear_cache = false;
                        finish();
                    }
                    deleteFileOtherActivityViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        fm.setFragmentResultListener(DELETE_FILE_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(DELETE_FILE_REQUEST_CODE)) {
                    progress_bar.setVisibility(View.VISIBLE);
                    Uri tree_uri = result.getParcelable("tree_uri");
                    String tree_uri_path = result.getString("tree_uri_path");
                    files_selected_for_delete = new ArrayList<>();
                    files_selected_for_delete.add(viewModel.currently_shown_file);
                    deleteFileOtherActivityViewModel.deleteFilePOJO(viewModel.source_folder, files_selected_for_delete, viewModel.fileObjectType, tree_uri, tree_uri_path);
                }
            }
        });

        fm.setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(SAF_PERMISSION_REQUEST_CODE)) {
                    tree_uri = result.getParcelable("tree_uri");
                    tree_uri_path = result.getString("tree_uri_path");
                    start_file_save_service();
                }
            }
        });

        fm.setFragmentResultListener(SAVE_CONFIRMATION_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (requestKey.equals(SAVE_CONFIRMATION_REQUEST_CODE)) {
                    boolean whether_closing = result.getBoolean("whether_closing");
                    if (whether_closing) {
                        boolean to_close = result.getBoolean("to_close");
                        on_being_closed(to_close);
                    } else {
                        boolean next_action = result.getBoolean("next_action");
                        next_action(next_action);
                    }
                }
            }
        });

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            on_intent(intent, savedInstanceState);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                file_close_procedure();
            }
        });
    }

    private void on_intent(Intent intent, @Nullable Bundle savedInstanceState) {
        if (intent != null) {
            viewModel.data = intent.getData();
            viewModel.fileObjectType = Global.GET_FILE_OBJECT_TYPE(intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_OBJECT_TYPE));
            viewModel.file_path = intent.getStringExtra(FileIntentDispatch.EXTRA_FILE_PATH);
            if (viewModel.file_path == null)
                viewModel.file_path = RealPathUtil.getLastSegmentPath(viewModel.data);

            if (viewModel.fileObjectType == null || viewModel.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                viewModel.fileObjectType = FileObjectType.FILE_TYPE;
                viewModel.fromThirdPartyApp = true;
            }

            if (savedInstanceState == null) {
                viewModel.setUpInitialization(viewModel.fileObjectType, viewModel.file_path);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        on_intent(intent, null);
    }

    private void lockScreen(boolean lock) {
        if (lock) {
            if (Global.ORIENTATION == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (emptyService != null && serviceConnection != null) {
            Intent file_save_service_intent = new Intent(context, emptyService);
            bindService(file_save_service_intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        clear_cache = true;
        Global.WORKOUT_AVAILABLE_SPACE();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }

        if (!isFinishing() && !isChangingConfigurations() && clear_cache) {
            clearCache();
        }
    }

    public void clearCache() {
        Global.CLEAR_CACHE();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.textViewUndoRedo.setEditTextUndoRedoListener(null);
        scrollview.setScrollViewListener(null);
        listPopWindow.dismiss(); // to avoid memory leak on orientation change
    }

    private boolean check_SAF_permission(String file_path, FileObjectType fileObjectType) {
        UriPOJO uriPOJO = Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path, fileObjectType);
        if (uriPOJO != null) {
            tree_uri_path = uriPOJO.get_path();
            tree_uri = uriPOJO.get_uri();
        }

        if (uriPOJO == null || tree_uri_path.isEmpty()) {
            SAFPermissionHelperDialog safpermissionhelper = SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE, file_path, fileObjectType);
            safpermissionhelper.show(fm, "saf_permission_dialog");
            return false;
        } else {
            return true;
        }
    }

    private void go_previous() {
        if (viewModel.current_page <= 1) {
            return;
        }
        viewModel.edit_mode = false;
        onClick_edit_button();
        int previousPage = viewModel.current_page - 1;
        FileEditorViewModel.PagePointer prevPagePointer = viewModel.page_pointer_hashmap.get(previousPage);

        if (prevPagePointer != null) {
            openFile(prevPagePointer.getStartPoint(), previousPage);
        }
    }

    private void go_next() {
        if (viewModel.file_end) {
            return;
        }
        viewModel.edit_mode = false;
        onClick_edit_button();
        int nextPage = viewModel.current_page + 1;
        openFile(viewModel.current_page_end_point, nextPage);
    }

    private boolean openFile(long pointer, int pageNumber) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(viewModel.data, "r");
            FileDescriptor fd = pfd.getFileDescriptor();
            progress_bar.setVisibility(View.VISIBLE);
            viewModel.isReadingFinished.setValue(AsyncTaskStatus.NOT_YET_STARTED);
            viewModel.openFile(new FileInputStream(fd), pointer, pageNumber);
            return true;
        } catch (FileNotFoundException e) {
            Global.print(context, getString(R.string.file_not_found));
            return false;
        } catch (IllegalArgumentException e) {
            Global.print(context, getString(R.string.file_could_not_be_opened));
            return false;
        }
    }

    @Override
    public void onEOLchanged(int eol) {
        if (!viewModel.file_format_supported) {
            return;
        }
        if (viewModel.eol != eol) {
            save_button.setEnabled(true);
            save_button.setAlpha(Global.ENABLE_ALFA);
            viewModel.updated = false;
        }
    }

    public void next_action(boolean save) {
        if (save) {
            progress_bar.setVisibility(View.VISIBLE);
            viewModel.saveContentInTempFile(filetext_container_edittext.getContent());
        } else {
            save_button.setEnabled(false);
            save_button.setAlpha(Global.DISABLE_ALFA);
            viewModel.updated = true;
            if (viewModel.action_after_save.equals("go_previous")) {
                go_previous();
            } else if (viewModel.action_after_save.equals("go_next")) {
                go_next();
            }
        }
    }

    public void on_being_closed(boolean to_close_after_save) {
        if (to_close_after_save) {
            viewModel.to_be_closed_after_save = to_close_after_save;
            progress_bar.setVisibility(View.VISIBLE);
            viewModel.saveContentInTempFile(filetext_container_edittext.getContent());
        } else {
            viewModel.textViewUndoRedo.disconnect();
            clear_cache = false;
            finish();
        }
    }

    private void file_close_procedure() {
        if (keyBoardUtil.getKeyBoardVisibility()) {
            imm.hideSoftInputFromWindow(filetext_container_edittext.getWindowToken(), 0);
        } else if (!FileSaveService1.SERVICE_COMPLETED || !FileSaveService2.SERVICE_COMPLETED || !FileSaveService3.SERVICE_COMPLETED) {
            Global.print(context, getString(R.string.please_wait));
        } else if (!viewModel.updated) {
            saveConfirmationAlertDialog = SaveFileConfirmationDialog.getInstance(SAVE_CONFIRMATION_REQUEST_CODE, true);
            saveConfirmationAlertDialog.show(fm, "saveconfirmationalert_dialog");
        } else {
            viewModel.textViewUndoRedo.disconnect();
            clear_cache = false;
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("file_name", file_name.getText().toString());
        outState.putBoolean("clear_cache", clear_cache);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        file_name.setText(savedInstanceState.getString("file_name"));
        onClick_edit_button();
        if (viewModel.file_start) {
            up_button.setEnabled(false);
            up_button.setAlpha(Global.DISABLE_ALFA);
        }
        if (viewModel.file_end) {
            down_button.setEnabled(false);
            down_button.setAlpha(Global.DISABLE_ALFA);
        }
        clear_cache = savedInstanceState.getBoolean("clear_cache");
    }

    private void onClick_edit_button() {
        if (!viewModel.file_format_supported) {
            save_button.setEnabled(false);
            setAlfaFileEditMenuItem();
            return;
        }

        if (viewModel.edit_mode) {
            if (viewModel.textViewUndoRedo.getCanUndo()) {
                undo_button.setEnabled(true);
                save_button.setEnabled(!viewModel.updated);
            }
            if (viewModel.textViewUndoRedo.getCanRedo()) {
                redo_button.setEnabled(true);
            }

            // API 21
            //filetext_container_edittext.setShowSoftInputOnFocus(true);
            filetext_container_edittext.setEditable(true);
            filetext_container_edittext.requestFocus();
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            viewModel.textViewUndoRedo.startListening();
        } else {
            filetext_container_edittext.clearFocus();
            save_button.setEnabled(false);
            undo_button.setEnabled(false);
            redo_button.setEnabled(false);

            // API 21
            //filetext_container_edittext.setShowSoftInputOnFocus(false);
            filetext_container_edittext.setEditable(false);
            imm.hideSoftInputFromWindow(filetext_container_edittext.getWindowToken(), 0);
            viewModel.textViewUndoRedo.stopListening();
        }
        edit_button.setSelected(viewModel.edit_mode);
        setAlfaFileEditMenuItem();
        lockScreen(viewModel.edit_mode);
        filetext_container_edittext.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent me) {
                return !viewModel.edit_mode;
            }
        });

    }

    private void setAlfaFileEditMenuItem() {
        edit_button.setAlpha(edit_button.isEnabled() ? Global.ENABLE_ALFA : Global.DISABLE_ALFA);
        save_button.setAlpha(save_button.isEnabled() ? Global.ENABLE_ALFA : Global.DISABLE_ALFA);
        undo_button.setAlpha(undo_button.isEnabled() ? Global.ENABLE_ALFA : Global.DISABLE_ALFA);
        redo_button.setAlpha(redo_button.isEnabled() ? Global.ENABLE_ALFA : Global.DISABLE_ALFA);
    }


    private void start_file_save_service() {
        if (!viewModel.file.exists())// || viewModel.temporary_file_for_save == null) {
        {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putBoolean("isWritable", viewModel.isWritable);
        bundle.putString("file_path", viewModel.file_path);
        bundle.putString("temp_file_path", new File(getExternalCacheDir(), FileEditorViewModel.temp_content_file_name).getAbsolutePath());
        bundle.putInt("eol", viewModel.eol);
        bundle.putInt("altered_eol", viewModel.altered_eol);
        bundle.putSerializable("page_pointer_hashmap", new LinkedHashMap<>(viewModel.page_pointer_hashmap));
        bundle.putInt("current_page", viewModel.current_page);

        if (!viewModel.isWritable) {
            if (!check_SAF_permission(viewModel.file_path, viewModel.fileObjectType)) {
                return;
            }
        }
        bundle.putString("tree_uri_path", tree_uri_path);
        bundle.putParcelable("tree_uri", tree_uri);
        progress_bar.setVisibility(View.VISIBLE);
        emptyService = getEmptyService();
        if (emptyService == null) {
            Global.print(context, getString(R.string.maximum_2_services_only_be_processed_at_a_time));
            return;
        }
        serviceConnection = new FileSaveServiceConnection(emptyService);
        Intent file_save_service_intent = new Intent(context, emptyService);
        bindService(file_save_service_intent, serviceConnection, Context.BIND_AUTO_CREATE);

        file_save_service_intent.putExtra("bundle", bundle);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(file_save_service_intent);
        } else {
            startService(file_save_service_intent);
        }
    }

    private void reloadCurrentChunk() {
        long startPoint = viewModel.page_pointer_hashmap.get(viewModel.current_page).getStartPoint();
        openFile(startPoint, viewModel.current_page);
    }

    private class BottomToolbarListener implements View.OnClickListener {
        @Override
        public void onClick(View p1) {
            // TODO: Implement this method
            if (progress_bar.getVisibility() == View.VISIBLE) {
                Global.print(context, getString(R.string.please_wait));
                return;
            }

            int id = p1.getId();
            if (id == R.id.toolbar_btn_1) {
                if (viewModel.fromThirdPartyApp) {
                    Global.print(context, getString(R.string.cant_edit_this_file));
                    return;
                }

                if (viewModel.isFileBig) {
                    Global.print(context, getString(R.string.file_is_large) + ", " + getString(R.string.cant_edit_this_file));
                    return;
                }
                viewModel.edit_mode = !viewModel.edit_mode;
                onClick_edit_button();
            } else if (id == R.id.toolbar_btn_2) {
                if (viewModel.textViewUndoRedo.getCanUndo()) {
                    viewModel.textViewUndoRedo.undo();
                    save_button.setEnabled(true);
                    save_button.setAlpha(Global.ENABLE_ALFA);
                    viewModel.updated = false;
                    if (!viewModel.textViewUndoRedo.getCanUndo()) {
                        undo_button.setEnabled(false);
                        undo_button.setAlpha(Global.DISABLE_ALFA);

                        save_button.setEnabled(false);
                        save_button.setAlpha(Global.DISABLE_ALFA);
                        viewModel.updated = true;
                    }

                    if (viewModel.textViewUndoRedo.getCanRedo()) {
                        redo_button.setEnabled(true);
                        redo_button.setAlpha(Global.ENABLE_ALFA);
                    }
                }
            } else if (id == R.id.toolbar_btn_3) {
                if (viewModel.textViewUndoRedo.getCanRedo()) {
                    viewModel.textViewUndoRedo.redo();
                    viewModel.updated = false;
                    if (!viewModel.textViewUndoRedo.getCanRedo()) {
                        redo_button.setEnabled(false);
                        redo_button.setAlpha(Global.DISABLE_ALFA);
                    }

                    if (viewModel.textViewUndoRedo.getCanUndo()) {
                        undo_button.setEnabled(true);
                        undo_button.setAlpha(Global.ENABLE_ALFA);
                        save_button.setEnabled(true);
                        save_button.setAlpha(Global.ENABLE_ALFA);
                    }
                }
            } else if (id == R.id.toolbar_btn_4) {
                progress_bar.setVisibility(View.VISIBLE);
                viewModel.saveContentInTempFile(filetext_container_edittext.getContent());

            } else if (id == R.id.toolbar_btn_5) {
                if (!viewModel.updated) {
                    viewModel.action_after_save = "go_previous";
                    saveConfirmationAlertDialog = SaveFileConfirmationDialog.getInstance(SAVE_CONFIRMATION_REQUEST_CODE, false);
                    saveConfirmationAlertDialog.show(fm, "saveconfirmationalert_dialog");
                } else {
                    viewModel.action_after_save = "";
                    go_previous();
                }
            } else if (id == R.id.toolbar_btn_6) {
                if (!viewModel.updated) {
                    viewModel.action_after_save = "go_next";
                    saveConfirmationAlertDialog = SaveFileConfirmationDialog.getInstance(SAVE_CONFIRMATION_REQUEST_CODE, false);
                    saveConfirmationAlertDialog.show(fm, "saveconfirmationalert_dialog");

                } else {
                    viewModel.action_after_save = "";
                    go_next();
                }
            }
        }
    }

    private class FileSaveServiceConnection implements ServiceConnection {
        Class service;
        FileSaveService1 fileSaveService1;
        FileSaveService2 fileSaveService2;

        FileSaveServiceConnection(Class service) {
            this.service = service;
        }

        @Override
        public void onServiceConnected(ComponentName p1, IBinder binder) {
            switch (service.getName()) {
                case "svl.kadatha.filex.FileSaveService1":
                    fileSaveService1 = ((FileSaveService1.FileSaveServiceBinder) binder).getService();
                    if (fileSaveService1 != null) {
                        fileSaveService1.setFileSaveServiceCompletionListener(new FileSaveService1.FileSaveServiceCompletionListener() {
                            public void onServiceCompletion(FileSaveHelper.SaveResult saveResult) {
                                if (saveResult.success) {
                                    viewModel.page_pointer_hashmap = saveResult.pagePointerHashmap;
                                    viewModel.current_page_end_point = viewModel.page_pointer_hashmap.get(viewModel.current_page).getEndPoint();
                                    viewModel.updated = true;
                                    viewModel.eol = viewModel.altered_eol;

                                    save_button.setEnabled(false);
                                    save_button.setAlpha(Global.DISABLE_ALFA);

                                    Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION, localBroadcastManager, ACTIVITY_NAME);

                                    Timber.tag(Global.TAG).d("File saved successfully");
                                    reloadCurrentChunk();
                                } else {
                                    viewModel.updated = false;
                                    Global.print(context, getString(R.string.file_could_not_be_saved) + ": " + saveResult.errorMessage);
                                    Timber.tag(Global.TAG).e("File save failed: %s", saveResult.errorMessage);
                                }

                                if (viewModel.to_be_closed_after_save) {
                                    viewModel.textViewUndoRedo.disconnect();
                                    clear_cache = false;
                                    finish();
                                } else if (viewModel.action_after_save.equals("go_previous")) {
                                    go_previous();
                                } else if (viewModel.action_after_save.equals("go_next")) {
                                    go_next();
                                }

                                progress_bar.setVisibility(View.GONE);
                            }
                        });
                    }
                    break;
                case "svl.kadatha.filex.FileSaveService2":
                    final FileSaveService2 fileSaveService2 = ((FileSaveService2.FileSaveServiceBinder) binder).getService();
                    if (fileSaveService2 != null) {
                        fileSaveService2.setFileSaveServiceCompletionListener(new FileSaveService2.FileSaveServiceCompletionListener() {
                            public void onServiceCompletion(FileSaveHelper.SaveResult saveResult) {
                                if (saveResult.success) {
                                    viewModel.page_pointer_hashmap = saveResult.pagePointerHashmap;
                                    viewModel.current_page_end_point = viewModel.page_pointer_hashmap.get(viewModel.current_page).getEndPoint();
                                    viewModel.updated = true;
                                    viewModel.eol = viewModel.altered_eol;

                                    save_button.setEnabled(false);
                                    save_button.setAlpha(Global.DISABLE_ALFA);

                                    Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION, localBroadcastManager, ACTIVITY_NAME);

                                    Timber.tag(Global.TAG).d("File saved successfully");
                                    reloadCurrentChunk();
                                } else {
                                    viewModel.updated = false;
                                    Global.print(context, getString(R.string.file_could_not_be_saved) + ": " + saveResult.errorMessage);
                                    Timber.tag(Global.TAG).e("File save failed: %s", saveResult.errorMessage);
                                }

                                if (viewModel.to_be_closed_after_save) {
                                    viewModel.textViewUndoRedo.disconnect();
                                    clear_cache = false;
                                    finish();
                                } else if (viewModel.action_after_save.equals("go_previous")) {
                                    go_previous();
                                } else if (viewModel.action_after_save.equals("go_next")) {
                                    go_next();
                                }
                                progress_bar.setVisibility(View.GONE);
                            }
                        });
                    }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName p1) {
            if (service != null) service = null;
            if (fileSaveService1 != null)
                fileSaveService1.setFileSaveServiceCompletionListener(null);
            if (fileSaveService2 != null)
                fileSaveService2.setFileSaveServiceCompletionListener(null);
        }
    }
}
