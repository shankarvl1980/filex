package svl.kadatha.filex;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import me.jahnen.libaums.core.fs.UsbFile;

public class FileSelectorActivity extends BaseActivity implements MediaMountReceiver.MediaMountListener, DetailFragmentListener {
    public static final int FOLDER_SELECT_REQUEST_CODE = 1564;
    public static final int MOVE_COPY_REQUEST_CODE = 351;
    public static final int PICK_FILE_REQUEST_CODE = 0;
    public static final int FILE_PATH_REQUEST_CODE = 56;
    public static final String ACTION_SOUGHT = "action_sought";
    public static final String ACTIVITY_NAME = "FILE_SELECTOR_ACTIVITY";
    public static FragmentManager FM;
    public static boolean FILE_GRID_LAYOUT, SHOW_HIDDEN_FILE;
    public static int RECYCLER_VIEW_FONT_SIZE_FACTOR, GRID_COUNT;
    public static String SORT;
    static LinkedList<FilePOJO> RECENT = new LinkedList<>();
    public FragmentManager fm;
    public boolean clear_cache;
    public List<FilePOJO> storage_filePOJO_list;
    public TextView file_number;
    public int action_sought_request_code;
    public RecentDialogListener recentDialogListener;
    public FloatingActionButton floatingActionButton;
    public boolean search_toolbar_visible;
    public KeyBoardUtil keyBoardUtil;
    public EditText search_edittext;
    private Context context;
    private OtherActivityBroadcastReceiver otherActivityBroadcastReceiver;
    private USBReceiver usbReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private MediaMountReceiver mediaMountReceiver;
    private PopupWindow listPopWindow;
    private Bundle bundle;
    private Group search_toolbar;
    private int countBackPressed = 0;
    private InputMethodManager imm;
    private RepositoryClass repositoryClass;
    private NetworkStateReceiver networkStateReceiver;
    private ImageButton parent_dir_btn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        repositoryClass = RepositoryClass.getRepositoryClass();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    activityResultLauncher_all_files_access_permission.launch(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activityResultLauncher_all_files_access_permission.launch(intent);
                }
            }
        }
        PermissionsUtil permissionUtil = new PermissionsUtil(context, this);
        permissionUtil.check_permission();
        mediaMountReceiver = new MediaMountReceiver();
        mediaMountReceiver.addMediaMountListener(this);
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addDataScheme("file");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(mediaMountReceiver, intentFilter, RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(mediaMountReceiver, intentFilter);
        }

        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        otherActivityBroadcastReceiver = new OtherActivityBroadcastReceiver();
        IntentFilter localBroadcastIntentFilter = new IntentFilter();
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_REFRESH_STORAGE_DIR_ACTION);
        localBroadcastManager.registerReceiver(otherActivityBroadcastReceiver, localBroadcastIntentFilter);

        usbReceiver = new USBReceiver();
        IntentFilter usbIntentFilter = new IntentFilter();
        usbIntentFilter.addAction(UsbDocumentProvider.USB_ATTACH_BROADCAST);
        localBroadcastManager.registerReceiver(usbReceiver, usbIntentFilter);

        networkStateReceiver = new NetworkStateReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);

        TinyDB tinyDB = new TinyDB(context);
        fm = getSupportFragmentManager();
        FM = fm;

        setContentView(R.layout.activity_file_selector);
        Toolbar toolbar = findViewById(R.id.activity_file_selector_toolbar);
        file_number = findViewById(R.id.file_selector_file_number); //initiate here before adding fragment

        ImageButton sort_btn = findViewById(R.id.file_selector_sort_btn);
        sort_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewDialog viewDialog = new ViewDialog();
                viewDialog.show(fm, "view_dialog");
            }
        });

        parent_dir_btn = findViewById(R.id.file_selector_parent_dir_btn);
        parent_dir_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentById(R.id.file_selector_container);
                if (fileSelectorFragment != null) {
                    String parent_file_path = Global.getParentPath(fileSelectorFragment.fileclickselected);
                    if (fileSelectorFragment.fileObjectType == FileObjectType.FILE_TYPE) {
                        File parent_file = new File(parent_file_path);
                        if (parent_file.list() != null) {
                            createFragmentTransaction(parent_file.getAbsolutePath(), FileObjectType.FILE_TYPE);
                        }
                    } else if (fileSelectorFragment.fileObjectType == FileObjectType.USB_TYPE) {
                        if (MainActivity.usbFileRoot == null) {
                            return;
                        }
                        try {
                            UsbFile usbFile = MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(parent_file_path));
                            createFragmentTransaction(usbFile.getAbsolutePath(), FileObjectType.USB_TYPE);
                        } catch (IOException ignored) {

                        }
                    } else {
                        createFragmentTransaction(parent_file_path, fileSelectorFragment.fileObjectType);
                    }
                }
            }
        });

        View containerLayout = findViewById(R.id.file_selector_container_layout);
        keyBoardUtil = new KeyBoardUtil(containerLayout);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        search_toolbar = findViewById(R.id.file_selector_search_toolbar);
        search_edittext = findViewById(R.id.file_selector_search_view_edit_text);
        search_edittext.setMaxWidth(Integer.MAX_VALUE);
        search_edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (!search_toolbar_visible) {
                    return;
                }
                FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentById(R.id.file_selector_container);
                if (fileSelectorFragment != null && fileSelectorFragment.adapter != null) {
                    fileSelectorFragment.adapter.getFilter().filter(s.toString());
                }
            }
        });

        ImageButton search_cancel_btn = findViewById(R.id.file_selector_search_view_cancel_button);
        search_cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSearchBarVisibility(false);
            }
        });


        Intent intent = getIntent();
        on_intent(intent, savedInstanceState);
        TextView heading = findViewById(R.id.file_selector_heading);
        if ((action_sought_request_code == FOLDER_SELECT_REQUEST_CODE) || action_sought_request_code == MOVE_COPY_REQUEST_CODE) {
            heading.setText(getString(R.string.choose_folder));
        } else {
            heading.setText(getString(R.string.pick_file));
        }

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileSelectorRecentDialog fileSelectorRecentDialog = new FileSelectorRecentDialog();
                fileSelectorRecentDialog.show(fm, "file_selector_recent_file_dialog");
            }
        });

        storage_filePOJO_list = getFilePOJO_list();
        listPopWindow = new PopupWindow(context);
        ListView listView = new ListView(context);
        PopupWindowAdapter popupWindowAdapter = new PopupWindowAdapter(context, storage_filePOJO_list);
        listView.setAdapter(popupWindowAdapter);
        listPopWindow.setContentView(listView);
        listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popup_window_width));
        listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopWindow.setFocusable(true);
        listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.list_popup_background));

        SHOW_HIDDEN_FILE = tinyDB.getBoolean("file_selector_show_hidden_file");

        floatingActionButton = findViewById(R.id.file_selector_floating_action_button_back);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onbackpressed(false);
            }
        });


        Toolbar bottom_toolbar = findViewById(R.id.file_selector_bottom_toolbar);
        EquallyDistributedButtonsWithTextLayout tb_layout = new EquallyDistributedButtonsWithTextLayout(context, 5, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] bottom_drawables = {R.drawable.search_icon, R.drawable.document_add_icon, R.drawable.refresh_icon, R.drawable.yes_icon, R.drawable.cancel_icon};
        String[] titles = {getString(R.string.search), getString(R.string.new_), getString(R.string.refresh), getString(R.string.ok), getString(R.string.cancel)};
        tb_layout.setResourceImageDrawables(bottom_drawables, titles);


        bottom_toolbar.addView(tb_layout);
        Button search_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        Button add_folder_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        Button refresh_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        Button ok_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_4);
        Button cancel_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_5);

        if (action_sought_request_code == PICK_FILE_REQUEST_CODE) {
            add_folder_btn.setVisibility(View.GONE);
            ok_btn.setVisibility(View.GONE);
        }


        search_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentById(R.id.file_selector_container);
                if (fileSelectorFragment.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }
                if (!search_toolbar_visible) {
                    setSearchBarVisibility(true);
                } else {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }
        });

        add_folder_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (search_toolbar_visible) {
                    setSearchBarVisibility(false);
                }

                FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentById(R.id.file_selector_container);
                if (fileSelectorFragment.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }

                CreateFileDialog createFileDialog = CreateFileDialog.getInstance(1, fileSelectorFragment.fileclickselected, fileSelectorFragment.fileObjectType);
                createFileDialog.show(fm, null);
            }
        });

        refresh_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentById(R.id.file_selector_container);
                if (fileSelectorFragment.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }

                fm.beginTransaction().detach(fileSelectorFragment).commit();
                fm.beginTransaction().attach(fileSelectorFragment).commit();
                Global.WORKOUT_AVAILABLE_SPACE();
            }
        });


        ok_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentById(R.id.file_selector_container);
                if (fileSelectorFragment.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }

                switch (action_sought_request_code) {
                    case FOLDER_SELECT_REQUEST_CODE:
                        if (fileSelectorFragment.fileclickselected == null) {
                            setResult(Activity.RESULT_CANCELED);
                        } else {
                            Intent intent = new Intent();
                            intent.putExtra("folderclickselected", fileSelectorFragment.folder_selected_textview.getText().toString());
                            intent.putExtra("destFileObjectType", fileSelectorFragment.fileObjectType);
                            setResult(Activity.RESULT_OK, intent);
                        }
                        break;
                    case MOVE_COPY_REQUEST_CODE:
                        if (fileSelectorFragment.fileclickselected == null) {
                            setResult(Activity.RESULT_CANCELED);
                        } else {
                            bundle.putString("dest_folder", fileSelectorFragment.folder_selected_textview.getText().toString());
                            bundle.putSerializable("destFileObjectType", fileSelectorFragment.fileObjectType);
                            Intent intent = new Intent();
                            intent.putExtra("bundle", bundle);
                            setResult(Activity.RESULT_OK, intent);
                        }
                        break;
                    default:
                        setResult(Activity.RESULT_CANCELED);
                        break;
                }
                setSearchBarVisibility(false);
                finish();
            }
        });


        cancel_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onbackpressed(true);
            }
        });
    }

    private void on_intent(Intent intent, Bundle savedInstanceState) {
        if (intent != null) {
            action_sought_request_code = intent.getIntExtra(ACTION_SOUGHT, PICK_FILE_REQUEST_CODE);
            bundle = intent.getBundleExtra("bundle");
        }
        if (savedInstanceState == null) {
            createFragmentTransaction(Global.GET_INTERNAL_STORAGE_FILE_POJO_STORAGE_DIR().getPath(), Global.GET_INTERNAL_STORAGE_FILE_POJO_STORAGE_DIR().getFileObjectType());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        on_intent(intent, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        final List<String> permission_not_granted_list = new ArrayList<>();
        if (requestCode == PermissionsUtil.PERMISSIONS_REQUEST_CODE && grantResults.length > 0) {
            for (int i = 0; i < permissions.length; ++i) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    permission_not_granted_list.add(permissions[i]);
                } else if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    repositoryClass.storage_dir.clear();
                    repositoryClass.hashmap_file_pojo.clear();
                    repositoryClass.hashmap_file_pojo_filtered.clear();
                    Intent in = getIntent();
                    finish();
                    startActivity(in);
                    return;
                }
            }
        }

        if (grantResults.length == 0 || !permission_not_granted_list.isEmpty()) {
            for (String permission : permission_not_granted_list) {
                if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permission)) {
                            showDialogOK(getString(R.string.read_and_write_permissions_are_must_for_the_app_to_work_please_grant_permissions), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case DialogInterface.BUTTON_POSITIVE:
                                            new PermissionsUtil(context, FileSelectorActivity.this).check_permission();
                                            break;
                                        case DialogInterface.BUTTON_NEGATIVE:
                                            Global.print(context, getString(R.string.permission_not_granted));
                                            finish();
                                            break;
                                    }
                                }
                            });
                        } else {
                            Global.print(context, getString(R.string.seems_permissions_were_not_granted_goto_settings_grant_permissions_to_app));
                            finish();
                        }
                    }
                }
            }
        }
    }

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), okListener)
                .setNegativeButton(getString(R.string.cancel), okListener)
                .create()
                .show();
    }


    @Override
    protected void onStart() {
        super.onStart();
        clear_cache = true;
        Global.WORKOUT_AVAILABLE_SPACE();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("clear_cache", clear_cache);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        clear_cache = savedInstanceState.getBoolean("clear_cache");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isFinishing() && !isChangingConfigurations() && clear_cache) {
            clearCache();
        }
    }

    public void setSearchBarVisibility(boolean visible) {
        FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentById(R.id.file_selector_container);
        if (fileSelectorFragment.progress_bar.getVisibility() == View.VISIBLE && visible) {
            Global.print(context, getString(R.string.please_wait));
            return;
        }

        search_toolbar_visible = visible;
        if (search_toolbar_visible) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            search_toolbar.setVisibility(View.VISIBLE);
            search_edittext.requestFocus();
            fileSelectorFragment.clearSelectionAndNotifyDataSetChanged();
        } else {
            imm.hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
            search_toolbar.setVisibility(View.GONE);
            search_edittext.setText("");
            search_edittext.clearFocus();
            fileSelectorFragment.clearSelectionAndNotifyDataSetChanged();
            if (fileSelectorFragment.adapter != null)
                fileSelectorFragment.adapter.getFilter().filter(null);
        }
    }


    public void clearCache() {
        Global.CLEAR_CACHE();
    }

    @Override
    public void onScrollRecyclerView(boolean showToolBar) {

    }

    @Override
    public void actionModeFinish(Fragment fragment, String fileclickeselected) {

    }

    @Override
    public void onLongClickItem(int size) {

    }

    @Override
    public void setFileNumberView(String file_number_string) {
        file_number.setText(file_number_string);
    }

    @Override
    public MainActivity.SearchParameters getSearchParameters() {
        return null;
    }

    public void clearCache(String file_path, FileObjectType fileObjectType) {
        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(file_path), fileObjectType); //no need of broad cast here, as the method includes broadcast
    }

    @Override
    public void setCurrentDirText(String current_dir_name) {

    }

    @Override
    public void enableParentDirImageButton(boolean enable) {
        parent_dir_btn.setEnabled(enable);
        if (enable) {
            parent_dir_btn.setAlpha(Global.ENABLE_ALFA);
        } else {
            parent_dir_btn.setAlpha(Global.DISABLE_ALFA);
        }
    }

    @Override
    public void rescanLargeDuplicateFilesLibrary(String type) {

    }

    @Override
    public void onCreateView(String fileclickselected, FileObjectType fileObjectType) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaMountReceiver.removeMediaMountListener(this);
        localBroadcastManager.unregisterReceiver(otherActivityBroadcastReceiver);
        localBroadcastManager.unregisterReceiver(usbReceiver);
        unregisterReceiver(networkStateReceiver);
        context.unregisterReceiver(mediaMountReceiver);
    }

    private void onbackpressed(boolean onBackPressed) {
        if (keyBoardUtil.getKeyBoardVisibility()) {
            imm.hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
        } else if (search_toolbar_visible) {
            setSearchBarVisibility(false);
        } else {
            if (fm.getBackStackEntryCount() > 1) {
                fm.popBackStack();
                int frag = 2, entry_count = fm.getBackStackEntryCount();
                FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentByTag(fm.getBackStackEntryAt(entry_count - frag).getName());
                String tag = fileSelectorFragment.getTag();

                while (tag != null && !new File(tag).exists() && fileSelectorFragment.currentUsbFile == null
                        && !Global.WHETHER_FILE_OBJECT_TYPE_NETWORK_TYPE_AND_CONTAINED_IN_STORAGE_DIR(fileSelectorFragment.fileObjectType)) {
                    fm.popBackStack();
                    ++frag;
                    if (frag > entry_count) break;
                    fileSelectorFragment = (FileSelectorFragment) fm.findFragmentByTag(fm.getBackStackEntryAt(entry_count - frag).getName());
                    tag = fileSelectorFragment.getTag();
                }
                countBackPressed = 0;
            } else {
                if (onBackPressed) {
                    countBackPressed++;
                    if (countBackPressed == 1) {
                        Global.print(context, getString(R.string.press_again_to_close_activity));
                    } else {
                        finish();
                    }
                } else {
                    Global.print(context, getString(R.string.click_OK_cancel_button_to_exit));
                }
            }
        }
    }

    public List<FilePOJO> getFilePOJO_list() {
        List<FilePOJO> filePOJOS = new ArrayList<>();
        for (FilePOJO filePOJO : repositoryClass.storage_dir) {
            if (filePOJO == null) continue;
            if (filePOJO.getFileObjectType() == FileObjectType.FILE_TYPE || filePOJO.getFileObjectType() == FileObjectType.FTP_TYPE
                    || filePOJO.getFileObjectType() == FileObjectType.USB_TYPE || filePOJO.getFileObjectType() == FileObjectType.SFTP_TYPE
                    || filePOJO.getFileObjectType() == FileObjectType.WEBDAV_TYPE || filePOJO.getFileObjectType() == FileObjectType.SMB_TYPE) {
                filePOJOS.add(filePOJO);
            }
        }
        return filePOJOS;
    }

    public void createFragmentTransaction(String file_path, FileObjectType fileObjectType) {
        String fragment_tag;
        String existingFilePOJOkey = "";

        FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentById(R.id.file_selector_container);
        if (fileSelectorFragment != null) {
            fragment_tag = fileSelectorFragment.getTag();
            existingFilePOJOkey = fileSelectorFragment.fileObjectType + fragment_tag;
            setSearchBarVisibility(false);
        }


        if (!(fileObjectType + file_path).equals(existingFilePOJOkey)) {
            FileSelectorFragment ff = FileSelectorFragment.getInstance(fileObjectType);
            fm.beginTransaction().replace(R.id.file_selector_container, ff, file_path).addToBackStack(file_path)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commitAllowingStateLoss();
        }

    }

    public void createNewFragmentTransaction(String file_path, FileObjectType fileObjectType) {
        String fragment_tag;
        String existingFilePOJOkey = "";

        FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentById(R.id.file_selector_container);
        if (fileSelectorFragment != null) {
            fragment_tag = fileSelectorFragment.getTag();
            existingFilePOJOkey = fileSelectorFragment.fileObjectType + fragment_tag;
            setSearchBarVisibility(false);
        }


        //this will force create new transaction//if(!(fileObjectType+file_path).equals(existingFilePOJOkey))
        {
            FileSelectorFragment ff = FileSelectorFragment.getInstance(fileObjectType);
            fm.beginTransaction().replace(R.id.file_selector_container, ff, file_path).addToBackStack(file_path)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commitAllowingStateLoss();
        }
    }

    @Override
    public void onMediaMount(String action) {
        switch (action) {
            case "android.intent.action.MEDIA_MOUNTED":
                repositoryClass.storage_dir.clear();
                repositoryClass.storage_dir.addAll(new ArrayList<>(StorageUtil.getSdCardPaths(context, true)));
                Global.WORKOUT_AVAILABLE_SPACE();
                storage_filePOJO_list = getFilePOJO_list();
                if (recentDialogListener != null) {
                    recentDialogListener.onMediaAttachedAndRemoved();
                }
                break;

            case "android.intent.action.MEDIA_EJECT":
            case "android.intent.action.MEDIA_REMOVED":
            case "android.intent.action.MEDIA_BAD_REMOVAL":
                repositoryClass.storage_dir.clear();
                repositoryClass.storage_dir.addAll(new ArrayList<>(StorageUtil.getSdCardPaths(context, true)));
                Global.WORKOUT_AVAILABLE_SPACE();
                storage_filePOJO_list = getFilePOJO_list();

                if (recentDialogListener != null) {
                    recentDialogListener.onMediaAttachedAndRemoved();
                }
                FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(repositoryClass.external_storage_path_list, FileObjectType.FILE_TYPE);
                FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentById(R.id.file_selector_container);
                if (fileSelectorFragment != null)
                    fileSelectorFragment.clearSelectionAndNotifyDataSetChanged();
                break;
        }
    }    private final ActivityResultLauncher<Intent> activityResultLauncher_all_files_access_permission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    repositoryClass.storage_dir.clear();
                    repositoryClass.hashmap_file_pojo.clear();
                    repositoryClass.hashmap_file_pojo_filtered.clear();
                    Intent in = getIntent();
                    finish();
                    startActivity(in);
                } else {
                    showDialogOK(getString(R.string.read_and_write_permissions_are_must_for_the_app_to_work_please_grant_permissions), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    try {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                        intent.addCategory("android.intent.category.DEFAULT");
                                        intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                                        activityResultLauncher_all_files_access_permission.launch(intent);
                                    } catch (Exception e) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                        activityResultLauncher_all_files_access_permission.launch(intent);
                                    }
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    Global.print(context, getString(R.string.permission_not_granted));
                                    finish();
                                    break;
                            }
                        }
                    });
                }
            }
        }
    });

    interface RecentDialogListener {
        void onMediaAttachedAndRemoved();
    }

    public static class PopupWindowAdapter extends ArrayAdapter<FilePOJO> {
        final Context context;
        final List<FilePOJO> list;

        PopupWindowAdapter(Context context, List<FilePOJO> list) {
            super(context, R.layout.list_popupwindow_layout, list);
            this.context = context;
            this.list = list;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable final View convertView, @NonNull ViewGroup parent) {
            View v;
            PopupWindowAdapter.ViewHolder vh;
            if (convertView == null) {
                v = LayoutInflater.from(context).inflate(R.layout.list_popupwindow_layout, parent, false);
                vh = new PopupWindowAdapter.ViewHolder();
                vh.imageView = v.findViewById(R.id.list_popupwindow_layout_iv);
                vh.textView = v.findViewById(R.id.list_popupwindow_tv);
                v.setTag(vh);
            } else {
                v = convertView;
                vh = (PopupWindowAdapter.ViewHolder) convertView.getTag();
            }
            final FilePOJO filePOJO = list.get(position);
            FileObjectType fileObjectType = filePOJO.getFileObjectType();
            if (fileObjectType == FileObjectType.FILE_TYPE) {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                if (repositoryClass.internal_storage_path_list.contains(filePOJO.getPath())) {
                    vh.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.device_icon));
                } else {
                    vh.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.sdcard_icon));
                }
                vh.textView.setText(filePOJO.getName());
            } else if (fileObjectType == FileObjectType.USB_TYPE) {
                vh.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.usb_icon));
                vh.textView.setText(DetailFragment.USB_FILE_PREFIX + filePOJO.getName());
            } else if (fileObjectType == FileObjectType.FTP_TYPE) {
                vh.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ftp_file_icon));
                vh.textView.setText(DetailFragment.FTP_FILE_PREFIX + filePOJO.getName());
            } else if (fileObjectType == FileObjectType.SFTP_TYPE) {
                vh.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ftp_file_icon));
                vh.textView.setText(DetailFragment.SFTP_FILE_PREFIX + filePOJO.getName());
            } else if (fileObjectType == FileObjectType.ROOT_TYPE) {
                vh.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.device_icon));
                vh.textView.setText(R.string.root_directory);
            }

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((FileSelectorActivity) context).createFragmentTransaction(filePOJO.getPath(), fileObjectType);
                    ((FileSelectorActivity) context).listPopWindow.dismiss();
                }
            });
            return v;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        public static class ViewHolder {
            ImageView imageView;
            TextView textView;
        }
    }

    public abstract static class FileSelectorAdapter extends RecyclerView.Adapter<FileSelectorAdapter.ViewHolder> implements Filterable {
        final Context context;
        final FileSelectorFragment fileSelectorFragment;

        FileSelectorAdapter(Context context, FileSelectorFragment fileSelectorFragment) {
            this.context = context;
            this.fileSelectorFragment = fileSelectorFragment;
            if (fileSelectorFragment != null) {
                if (fileSelectorFragment.fileObjectType == FileObjectType.FILE_TYPE) {
                    File f = new File(fileSelectorFragment.fileclickselected);
                    File parent_file = f.getParentFile();
                    if (parent_file != null) {
                        fileSelectorFragment.detailFragmentListener.enableParentDirImageButton(true);
                    } else {
                        fileSelectorFragment.detailFragmentListener.enableParentDirImageButton(false);
                    }
                } else {
                    String parent_path = fileSelectorFragment.fileclickselected;
                    if (parent_path.equals("/")) {
                        fileSelectorFragment.detailFragmentListener.enableParentDirImageButton(false);
                    } else {
                        fileSelectorFragment.detailFragmentListener.enableParentDirImageButton(true);
                    }
                }
            }
        }

        @Override
        public abstract FileSelectorAdapter.ViewHolder onCreateViewHolder(ViewGroup p1, int p2);

        @Override
        public void onBindViewHolder(final FileSelectorAdapter.ViewHolder p1, int p2) {
            FilePOJO file = fileSelectorFragment.filePOJO_list.get(p2);
            p1.v.setData(file, false);
        }

        @Override
        public int getItemCount() {
            return fileSelectorFragment.filePOJO_list.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    return new FilterResults();
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    fileSelectorFragment.filePOJO_list = new ArrayList<>();
                    if (constraint == null || constraint.length() == 0) {
                        fileSelectorFragment.filePOJO_list = fileSelectorFragment.totalFilePOJO_list;
                    } else {
                        String pattern = constraint.toString().toLowerCase().trim();
                        for (int i = 0; i < fileSelectorFragment.totalFilePOJO_list_Size; ++i) {
                            FilePOJO filePOJO = fileSelectorFragment.totalFilePOJO_list.get(i);
                            if (filePOJO.getLowerName().contains(pattern)) {
                                fileSelectorFragment.filePOJO_list.add(filePOJO);
                            }
                        }
                    }

                    int t = fileSelectorFragment.filePOJO_list.size();
                    fileSelectorFragment.clearSelectionAndNotifyDataSetChanged();
                    if (t > 0) {
                        fileSelectorFragment.recycler_view.setVisibility(View.VISIBLE);
                        fileSelectorFragment.folder_empty_textview.setVisibility(View.GONE);
                    }

                    if (fileSelectorFragment.detailFragmentListener != null) {
                        fileSelectorFragment.detailFragmentListener.setFileNumberView("" + t);
                    }
                }
            };
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final RecyclerViewLayout v;
            FileObjectType fileObjectType;

            ViewHolder(RecyclerViewLayout v) {
                super(v);
                this.v = v;
                v.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        int pos = getBindingAdapterPosition();
                        FilePOJO filePOJO = fileSelectorFragment.filePOJO_list.get(pos);
                        fileObjectType = filePOJO.getFileObjectType();
                        if (filePOJO.getIsDirectory()) {
                            if (fileSelectorFragment.detailFragmentListener != null) {
                                fileSelectorFragment.detailFragmentListener.createFragmentTransaction(filePOJO.getPath(), fileObjectType);
                            }
                            FileSelectorRecentDialog.ADD_FILE_POJO_TO_RECENT(filePOJO, FileSelectorRecentDialog.FILE_SELECTOR);
                        } else {
                            AppCompatActivity activity = (AppCompatActivity) context;
                            if (!(activity instanceof FileSelectorActivity)) return;
                            if (((FileSelectorActivity) activity).action_sought_request_code == FileSelectorActivity.PICK_FILE_REQUEST_CODE) {
                                Uri uri = null;
                                if (Global.whether_file_cached(fileObjectType)) {
                                    Global.print(context, context.getString(R.string.not_supported));
                                } else if (fileObjectType == FileObjectType.FILE_TYPE) {
                                    File file = new File(filePOJO.getPath());
                                    uri = FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", file);
                                }

                                if (uri != null) {
                                    String file_extn = "";
                                    String file_path = filePOJO.getPath();
                                    int file_extn_idx = file_path.lastIndexOf(".");
                                    if (file_extn_idx != -1) {
                                        file_extn = file_path.substring(file_extn_idx + 1);
                                    }
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    FileIntentDispatch.SET_INTENT_FOR_VIEW(intent, null, filePOJO.getPath(), file_extn, filePOJO.getFileObjectType(), false, uri);
                                    fileSelectorFragment.getActivity().setResult(Activity.RESULT_OK, intent);
                                } else {
                                    fileSelectorFragment.getActivity().setResult(Activity.RESULT_CANCELED);
                                }
                                fileSelectorFragment.getActivity().finish();
                            } else if (((FileSelectorActivity) activity).action_sought_request_code == FileSelectorActivity.FILE_PATH_REQUEST_CODE) {
                                if (fileObjectType != FileObjectType.FILE_TYPE) {
                                    fileSelectorFragment.getActivity().setResult(Activity.RESULT_CANCELED);
                                }
                                Intent intent = new Intent();
                                intent.putExtra("filepathclickselected", filePOJO.getPath());
                                intent.putExtra("destFileObjectType", fileObjectType);
                                fileSelectorFragment.getActivity().setResult(Activity.RESULT_OK, intent);
                                fileSelectorFragment.getActivity().finish();
                            }
                        }
                    }
                });
            }
        }
    }

    public static class FileSelectorAdapterGrid extends FileSelectorAdapter {

        FileSelectorAdapterGrid(Context context, FileSelectorFragment fileSelectorFragment) {
            super(context, fileSelectorFragment);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            return new FileSelectorAdapter.ViewHolder(new FileSelectorRecyclerViewLayoutGrid(context, false));
        }
    }

    public static class FileSelectorAdapterList extends FileSelectorAdapter {

        FileSelectorAdapterList(Context context, FileSelectorFragment fileSelectorFragment) {
            super(context, fileSelectorFragment);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            return new FileSelectorAdapter.ViewHolder(new FileSelectorRecyclerViewLayoutList(context, false));
        }
    }

    private class OtherActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentById(R.id.file_selector_container);
            String activity_name = intent.getStringExtra("activity_name");
            String file_path = intent.getStringExtra("file_path");
            FileObjectType fileObjectType = (FileObjectType) intent.getSerializableExtra("fileObjectType");
            switch (intent.getAction()) {
                case Global.LOCAL_BROADCAST_DELETE_FILE_ACTION:
                    if (fileSelectorFragment != null)
                        fileSelectorFragment.local_activity_delete = true;
                    break;
                case Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION:
                    if (fileSelectorFragment != null)
                        fileSelectorFragment.modification_observed = true;
                    break;
                case Global.LOCAL_BROADCAST_REFRESH_STORAGE_DIR_ACTION:
                    break;
            }
        }
    }

    private class USBReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context p1, Intent intent) {
            String action = intent.getAction();
            if (UsbDocumentProvider.USB_ATTACH_BROADCAST.equals(action)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                if (!MainActivity.USB_ATTACHED) {
                    FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(""), FileObjectType.USB_TYPE);
                    FileSelectorFragment fileSelectorFragment = (FileSelectorFragment) fm.findFragmentById(R.id.file_selector_container);
                    if (fileSelectorFragment != null && fileSelectorFragment.fileObjectType == FileObjectType.USB_TYPE) {
                        fileSelectorFragment.progress_bar.setVisibility(View.VISIBLE);
                        onbackpressed(false);
                    }

                    int entry_count = fm.getBackStackEntryCount();
                    for (int i = 0; i < entry_count; ++i) {
                        Fragment frag = fm.findFragmentByTag(fm.getBackStackEntryAt(i).getName());
                        if (frag instanceof FileSelectorFragment) {
                            fileSelectorFragment = (FileSelectorFragment) frag;
                            fileSelectorFragment.currentUsbFile = null;
                        }
                    }

                    Iterator<FilePOJO> iterator1 = RECENT.iterator();
                    while (iterator1.hasNext()) {
                        if (iterator1.next().getFileObjectType() == FileObjectType.USB_TYPE) {
                            iterator1.remove();
                        }
                    }
                    Global.REMOVE_USB_URI_PERMISSIONS();
                }
                storage_filePOJO_list = getFilePOJO_list();
                //usb_heading.setVisibility(USB_ATTACHED ? View.VISIBLE : View.GONE);
            }
            if (recentDialogListener != null) {
                recentDialogListener.onMediaAttachedAndRemoved();
            }
        }
    }




}
