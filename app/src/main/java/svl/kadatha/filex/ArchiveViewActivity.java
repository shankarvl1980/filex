package svl.kadatha.filex;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipFile;

public class ArchiveViewActivity extends BaseActivity implements DetailFragmentListener {

    public static final String ACTIVITY_NAME = "ARCHIVE_VIEW_ACTIVITY";
    private static final boolean[] alreadyNotificationWarned = new boolean[1];
    public static PackageManager PM;
    public static FragmentManager FM;
    static File ZIP_FILE;
    public ImageButton back_button, parent_dir_image_button, all_select, interval_select;
    public TinyDB tinyDB;
    public PackageManager pm;
    public FragmentManager fm;
    public EditText search_edittext;
    public boolean search_toolbar_visible;
    public boolean clear_cache;
    public FloatingActionButton floating_button_back;
    public ArchiveViewerViewModel viewModel;
    public FrameLayout activity_progress_bar;
    TextView file_number_view;
    Toolbar bottom_toolbar;
    TextView current_dir_textview;
    Context context = this;
    private int countBackPressed = 0;
    private Group search_toolbar;
    private KeyBoardUtil keyBoardUtil;
    private LocalBroadcastManager localBroadcastManager;
    private LocalBroadcastReceiver localBroadcastReceiver;
    private InputMethodManager imm;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    activityResultLauncher_all_file_access_permission.launch(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activityResultLauncher_all_file_access_permission.launch(intent);
                }
            }
        }

        PermissionsUtil permissionUtil = new PermissionsUtil(context, ArchiveViewActivity.this);
        permissionUtil.check_permission();
        tinyDB = new TinyDB(context);
        setContentView(R.layout.activity_archive_viewer);
        ConstraintLayout root_layout = findViewById(R.id.archive_root_layout);
        viewModel = new ViewModelProvider(this).get(ArchiveViewerViewModel.class);
        fm = getSupportFragmentManager();
        FM = fm;
        pm = getPackageManager();
        PM = pm;
        localBroadcastManager = LocalBroadcastManager.getInstance(context);

        localBroadcastReceiver = new LocalBroadcastReceiver();
        IntentFilter localBroadcastIntentFilter = new IntentFilter();
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_OTHER_ACTIVITY_DELETE_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION);
        localBroadcastManager.registerReceiver(localBroadcastReceiver, localBroadcastIntentFilter);

        activity_progress_bar = findViewById(R.id.activity_archive_detail_progressbar);
        keyBoardUtil = new KeyBoardUtil(root_layout);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        search_toolbar = findViewById(R.id.archive_viewer_search_toolbar);
        search_edittext = findViewById(R.id.archive_viewer_search_view_edit_text);
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
                ArchiveViewFragment archiveViewFragment = (ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
                if (archiveViewFragment != null && archiveViewFragment.adapter != null) {
                    archiveViewFragment.adapter.getFilter().filter(s.toString());
                }
            }
        });

        ImageButton search_cancel_btn = findViewById(R.id.archive_viewer_search_view_cancel_button);
        search_cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                set_visibility_searchbar(false);
            }
        });

        file_number_view = findViewById(R.id.archive_detail_fragment_file_number);
        back_button = findViewById(R.id.archive_top_toolbar_back_button);
        back_button.setVisibility(View.GONE);
        parent_dir_image_button = findViewById(R.id.archive_top_toolbar_parent_dir_image_button);
        current_dir_textview = findViewById(R.id.archive_top_toolbar_current_dir_label);
        all_select = findViewById(R.id.archive_detail_fragment_all_select);
        interval_select = findViewById(R.id.archive_detail_fragment_interval_select);

        TopToolbarClickListener topToolbarClickListener = new TopToolbarClickListener();
        back_button.setOnClickListener(topToolbarClickListener);
        parent_dir_image_button.setOnClickListener(topToolbarClickListener);
        current_dir_textview.setOnClickListener(topToolbarClickListener);
        all_select.setOnClickListener(topToolbarClickListener);
        interval_select.setOnClickListener(topToolbarClickListener);

        floating_button_back = findViewById(R.id.archive_floating_action_button_back);
        floating_button_back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View p1) {
                onbackpressed(false);
            }
        });

        EquallyDistributedButtonsWithTextLayout tb_layout = new EquallyDistributedButtonsWithTextLayout(this, 4, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] drawables = {R.drawable.search_icon, R.drawable.refresh_icon, R.drawable.extract_icon, R.drawable.cancel_icon};
        String[] titles = new String[]{getString(R.string.search), getString(R.string.refresh), getString(R.string.extract), getString(R.string.close)};
        tb_layout.setResourceImageDrawables(drawables, titles);
        bottom_toolbar = findViewById(R.id.archive_bottom_toolbar);
        bottom_toolbar.addView(tb_layout);
        Button search = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        Button refresh = bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        Button extract = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        Button close = bottom_toolbar.findViewById(R.id.toolbar_btn_4);

        BottomToolbarClickListener bottomToolbarClickListener = new BottomToolbarClickListener();
        search.setOnClickListener(bottomToolbarClickListener);
        refresh.setOnClickListener(bottomToolbarClickListener);
        extract.setOnClickListener(bottomToolbarClickListener);
        close.setOnClickListener(bottomToolbarClickListener);

        Global.WARN_NOTIFICATIONS_DISABLED(context, NotifManager.CHANNEL_ID, alreadyNotificationWarned);

        viewModel.isExtractionCompleted.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                ArchiveViewFragment archiveViewFragment = (ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    activity_progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    activity_progress_bar.setVisibility(View.GONE);
                    if (viewModel.zipFileExtracted) {
                        createFragmentTransaction(Global.ARCHIVE_EXTRACT_DIR.getAbsolutePath(), FileObjectType.FILE_TYPE);
                        viewModel.zipFileExtracted = false;
                    }
                }
            }
        });

        viewModel.isArchiveEntriesPopulated.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    activity_progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    activity_progress_bar.setVisibility(View.GONE);
                    ArrayList<String> files_selected_array = new ArrayList<>();
                    files_selected_array.add(ZIP_FILE.getAbsolutePath());
                    ArchiveSetUpDialog archiveSetUpDialog = ArchiveSetUpDialog.getInstance(files_selected_array, viewModel.zip_entries_array, viewModel.base_path, viewModel.fileObjectType, ArchiveSetUpDialog.ARCHIVE_ACTION_UNZIP);
                    archiveSetUpDialog.show(fm, null);
                    ArchiveViewFragment archiveViewFragment = (ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
                    if (archiveViewFragment != null) {
                        archiveViewFragment.clearSelectionAndNotifyDataSetChanged();
                    }
                    viewModel.isArchiveEntriesPopulated.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent != null) {
                onInitiate(intent);
            }
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onbackpressed(true);
            }
        });
    }

    public void set_visibility_searchbar(boolean visible) {
        ArchiveViewFragment archiveViewFragment = (ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
        if (archiveViewFragment.progress_bar.getVisibility() == View.VISIBLE) {
            Global.print(context, getString(R.string.please_wait));
            return;
        }
        search_toolbar_visible = visible;
        if (search_toolbar_visible) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            search_toolbar.setVisibility(View.VISIBLE);
            search_edittext.requestFocus();
        } else {
            action_mode_finish(archiveViewFragment, archiveViewFragment.fileclickselected);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        final List<String> permission_not_granted_list = new ArrayList<>();
        if (requestCode == PermissionsUtil.PERMISSIONS_REQUEST_CODE && grantResults.length > 0) {
            for (int i = 0; i < permissions.length; ++i) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    permission_not_granted_list.add(permissions[i]);
                } else if (permissions[i].equals(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
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
                switch (permission) {
                    case android.Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(permission)) {
                                showDialogOK(getString(R.string.read_and_write_permissions_are_must_for_the_app_to_work_please_grant_permissions), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                new PermissionsUtil(context, ArchiveViewActivity.this).check_permission();
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
                                break;
                            }
                        }
                        break;

                    case android.Manifest.permission.READ_PHONE_STATE:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(permission)) {
                                showDialogOK(getString(R.string.permission_required_to_regulate_audio_play_when_phone_rings), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                new PermissionsUtil(context, ArchiveViewActivity.this).check_permission();
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                Global.print(context, getString(R.string.permission_not_granted));
                                                break;
                                        }
                                    }
                                });
                            }

                        }
                        break;

                    case Manifest.permission.POST_NOTIFICATIONS:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(permission)) {
                                showDialogOK(getString(R.string.permission_rationale_for_notification), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                new PermissionsUtil(context, ArchiveViewActivity.this).check_permission();
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                Global.print(context, getString(R.string.permission_not_granted));
                                                break;
                                        }
                                    }
                                });
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

    private void onInitiate(Intent intent) {
        String receivedAction = intent.getAction();
        Uri uri = intent.getData();
        if (receivedAction != null && receivedAction.equals(Intent.ACTION_VIEW) && uri != null) {
            String path = RealPathUtil.getRealPath(context, uri);
            if (path == null) {
                Global.print(context, getString(R.string.could_not_open_zipe_file));
                return;
            }
            ZIP_FILE = new File(path);
            ZipFile zipfile;
            try {
                zipfile = new ZipFile(ZIP_FILE);
            } catch (IOException e) {
                viewModel.isExtractionCompleted.setValue(AsyncTaskStatus.COMPLETED);
                activity_progress_bar.setVisibility(View.GONE);
                Global.print(context, getString(R.string.could_not_open_zipe_file));
                return;
            }
            viewModel.extractArchive(zipfile);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        clear_cache = true;
        Global.WORKOUT_AVAILABLE_SPACE();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (search_toolbar_visible) {
            set_visibility_searchbar(false);
        }

        if (!isChangingConfigurations() && clear_cache) {
            clearCache();
        }
    }

    public void clearCache() {
        Global.CLEAR_CACHE();
    }

    public void clearCache(String file_path, FileObjectType fileObjectType) {
        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(file_path), fileObjectType); //no need of broad cast here, as the method includes broadcast
    }

    @Override
    public void setCurrentDirText(String current_dir_name) {
        current_dir_textview.setText(current_dir_name);
    }

    @Override
    public void enableParentDirImageButton(boolean enable) {
        parent_dir_image_button.setEnabled(enable);
        if (enable) {
            parent_dir_image_button.setAlpha(Global.ENABLE_ALFA);
        } else {
            parent_dir_image_button.setAlpha(Global.DISABLE_ALFA);
        }
    }

    @Override
    public void rescanLargeDuplicateFilesLibrary(String type) {

    }

    @Override
    public void onCreateView(String fileclickselected, FileObjectType fileObjectType) {

    }

    @Override
    public void onDeselectAll(Fragment fragment) {
        DeselectAllAndAdjustToolbars((ArchiveViewFragment) fragment, ((ArchiveViewFragment) fragment).fileclickselected);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("clear_cache", clear_cache);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        clear_cache = savedInstanceState.getBoolean("clear_cache");
        ArchiveViewFragment archiveViewFragment = (ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
        int size = archiveViewFragment.viewModel.mselecteditems.size();
        if (size > 1) {
            interval_select.setVisibility(View.VISIBLE);
            int last_key = archiveViewFragment.viewModel.mselecteditems.getKeyAtIndex(size - 1);
            int previous_to_last_key = archiveViewFragment.viewModel.mselecteditems.getKeyAtIndex(size - 2);
            if (last_key - previous_to_last_key < -1 || last_key - previous_to_last_key > 1) {
                interval_select.setAlpha(Global.ENABLE_ALFA);
                interval_select.setEnabled(true);
            } else {
                interval_select.setAlpha(Global.DISABLE_ALFA);
                interval_select.setEnabled(false);
            }
        }

        if (size == archiveViewFragment.file_list_size) {
            all_select.setImageResource(R.drawable.deselect_icon);
        }
    }

    @Override
    public void onScrollRecyclerView(boolean showToolBar) {
        if (showToolBar) {
            bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
        } else {
            bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
        }
    }

    @Override
    public void actionModeFinish(Fragment fragment, String fileclickeselected) {
        action_mode_finish((ArchiveViewFragment) fragment, fileclickeselected);
    }

    @Override
    public void onLongClickItem(int size) {
        ArchiveViewFragment archiveViewFragment = (ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
        if (size == 1) {
            interval_select.setAlpha(Global.DISABLE_ALFA);
            interval_select.setEnabled(false);
        } else if (size > 1) {
            interval_select.setVisibility(View.VISIBLE);
            int last_key = archiveViewFragment.viewModel.mselecteditems.getKeyAtIndex(size - 1);
            int previous_to_last_key = archiveViewFragment.viewModel.mselecteditems.getKeyAtIndex(size - 2);
            if (last_key - previous_to_last_key < -1 || last_key - previous_to_last_key > 1) {
                interval_select.setAlpha(Global.ENABLE_ALFA);
                interval_select.setEnabled(true);
            } else {
                interval_select.setAlpha(Global.DISABLE_ALFA);
                interval_select.setEnabled(false);
            }
        }

        if (size == archiveViewFragment.file_list_size) {
            all_select.setImageResource(R.drawable.deselect_icon);
            interval_select.setAlpha(Global.DISABLE_ALFA);
            interval_select.setEnabled(false);
        } else {
            all_select.setImageResource(R.drawable.select_icon);
        }

        if (size == 0) {
            DeselectAllAndAdjustToolbars(archiveViewFragment, archiveViewFragment.fileclickselected);
        }
    }

    @Override
    public void setFileNumberView(String file_number_string) {
        file_number_view.setText(file_number_string);
    }

    public void createFragmentTransaction(String file_path, FileObjectType fileObjectType) {
        String fragment_tag;
        String existingFilePOJOkey = "";
        ArchiveViewFragment archiveViewFragment = (ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
        if (archiveViewFragment != null) {
            fragment_tag = archiveViewFragment.getTag();
            existingFilePOJOkey = archiveViewFragment.fileObjectType + fragment_tag;
            action_mode_finish(archiveViewFragment, file_path); //string provided to action_mode_finish method is file_path (which is clicked, not the existing file_path) to be created of fragemnttransaction
        }

        if (!(fileObjectType + file_path).equals(existingFilePOJOkey)) {
            fm.beginTransaction().replace(R.id.archive_detail_fragment, ArchiveViewFragment.getInstance(fileObjectType), file_path)
                    .addToBackStack(file_path).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commitAllowingStateLoss();
        }
    }

    @Override
    public void setSearchBarVisibility(boolean visible) {

    }

    @Override
    public MainActivity.SearchParameters getSearchParameters() {
        return null;
    }

    private void onbackpressed(boolean onBackPressed) {
        ArchiveViewFragment archiveViewFragment = (ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
        if (archiveViewFragment == null) {
            if (keyBoardUtil.getKeyBoardVisibility()) {
                imm.hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
            }
            finish();
        } else if (keyBoardUtil.getKeyBoardVisibility()) {
            imm.hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
        } else if (!archiveViewFragment.viewModel.mselecteditems.isEmpty()) {
            action_mode_finish(archiveViewFragment, archiveViewFragment.fileclickselected);
        } else if (search_toolbar_visible) {
            set_visibility_searchbar(false);
        } else {
            bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
            bottom_toolbar.setVisibility(View.VISIBLE);
            archiveViewFragment.is_toolbar_visible = true;

            int entry_count;
            if ((entry_count = fm.getBackStackEntryCount()) > 1) {
                fm.popBackStack();
                int frag = 2;
                archiveViewFragment = (ArchiveViewFragment) fm.findFragmentByTag(fm.getBackStackEntryAt(entry_count - frag).getName());
                String df_tag = archiveViewFragment.getTag();
                while (!new File(df_tag).exists()) //!df_tag.equals(DetailFragment.SEARCH_RESULT) &&
                {
                    fm.popBackStack();
                    ++frag;
                    if (frag > entry_count) {
                        break;
                    }
                    archiveViewFragment = (ArchiveViewFragment) fm.findFragmentByTag(fm.getBackStackEntryAt(entry_count - frag).getName());
                    df_tag = archiveViewFragment.getTag();
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
                    Global.print(context, getString(R.string.click_close_button_to_exit));
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        imm.hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(localBroadcastReceiver);
    }

    public void DeselectAllAndAdjustToolbars(ArchiveViewFragment archiveViewFragment, String detailfrag_tag) {
        bottom_toolbar.setVisibility(View.VISIBLE);
        bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
        if (archiveViewFragment != null) {
            archiveViewFragment.clearSelectionAndNotifyDataSetChanged();
            archiveViewFragment.is_toolbar_visible = true;
            all_select.setImageResource(R.drawable.select_icon);
            interval_select.setVisibility(View.GONE);
        }
    }

    public void action_mode_finish(ArchiveViewFragment archiveViewFragment, String archive_view_fragment_tag) {
        if (archiveViewFragment.adapter != null) {
            archiveViewFragment.adapter.getFilter().filter(null);
        }
        DeselectAllAndAdjustToolbars(archiveViewFragment, archive_view_fragment_tag);
        imm.hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
        search_edittext.setText("");
        search_edittext.clearFocus();
        search_toolbar.setVisibility(View.GONE); //no need to call adapter.filter with null to refill filepjos as calling datasetchanged replenished archiveViewFragment.adapter.filepojo listUri
        search_toolbar_visible = false;
    }

    public static class ArchiveDetailRecyclerViewAdapter extends RecyclerView.Adapter<ArchiveDetailRecyclerViewAdapter.ViewHolder> implements Filterable {
        private final Context context;
        private final ArchiveViewFragment archiveViewFragment;

        private CardViewClickListener cardViewClickListener;
        private boolean show_file_path;

        ArchiveDetailRecyclerViewAdapter(Context context, ArchiveViewFragment archiveViewFragment) {
            this.context = context;
            this.archiveViewFragment = archiveViewFragment;
            if (archiveViewFragment.detailFragmentListener != null) {
                archiveViewFragment.detailFragmentListener.setCurrentDirText(archiveViewFragment.file_click_selected_name);
                archiveViewFragment.detailFragmentListener.setFileNumberView(archiveViewFragment.viewModel.mselecteditems.size() + "/" + archiveViewFragment.file_list_size);
                if (archiveViewFragment.fileObjectType == FileObjectType.FILE_TYPE) {
                    File f = new File(archiveViewFragment.fileclickselected);
                    File parent_file = f.getParentFile();
                    if (parent_file != null) {

                    } else {
                        archiveViewFragment.detailFragmentListener.setCurrentDirText(context.getString(R.string.root_directory));
                        archiveViewFragment.detailFragmentListener.enableParentDirImageButton(false);
                    }
                }
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            if (Global.FILE_GRID_LAYOUT) {
                return new ViewHolder(new RecyclerViewLayoutGrid(context, show_file_path));
            } else {
                return new ViewHolder(new RecyclerViewLayoutList(context, show_file_path));
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder p1, int p2) {
            FilePOJO file = archiveViewFragment.filePOJO_list.get(p2);
            boolean selected = archiveViewFragment.viewModel.mselecteditems.containsKey(p2);
            p1.view.setData(file, selected);
            p1.view.setSelected(selected);
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
                    archiveViewFragment.filePOJO_list = new ArrayList<>();
                    if (constraint == null || constraint.length() == 0) {
                        archiveViewFragment.filePOJO_list = archiveViewFragment.totalFilePOJO_list;
                    } else {
                        String pattern = constraint.toString().toLowerCase().trim();
                        for (int i = 0; i < archiveViewFragment.totalFilePOJO_list_Size; ++i) {
                            FilePOJO filePOJO = archiveViewFragment.totalFilePOJO_list.get(i);
                            if (filePOJO.getLowerName().contains(pattern)) {
                                archiveViewFragment.filePOJO_list.add(filePOJO);
                            }
                        }
                    }

                    archiveViewFragment.file_list_size = archiveViewFragment.filePOJO_list.size();
                    if (!archiveViewFragment.viewModel.mselecteditems.isEmpty()) {
                        archiveViewFragment.adapter.deselectAll();
                    } else {
                        notifyDataSetChanged();
                    }
                    if (archiveViewFragment.file_list_size > 0) {
                        archiveViewFragment.recyclerView.setVisibility(View.VISIBLE);
                        archiveViewFragment.folder_empty.setVisibility(View.GONE);
                    }

                    if (archiveViewFragment.detailFragmentListener != null) {
                        archiveViewFragment.detailFragmentListener.setFileNumberView(archiveViewFragment.viewModel.mselecteditems.size() + "/" + archiveViewFragment.file_list_size);
                    }
                }
            };
        }

        @Override
        public int getItemCount() {
            return archiveViewFragment.filePOJO_list.size();
        }

        public void selectAll() {
            archiveViewFragment.viewModel.mselecteditems = new IndexedLinkedHashMap<>();
            int size = archiveViewFragment.filePOJO_list.size();

            for (int i = 0; i < size; ++i) {
                archiveViewFragment.viewModel.mselecteditems.put(i, archiveViewFragment.filePOJO_list.get(i).getPath());
            }

            int s = archiveViewFragment.viewModel.mselecteditems.size();
            archiveViewFragment.detailFragmentListener.setFileNumberView(s + "/" + archiveViewFragment.file_list_size);
            archiveViewFragment.detailFragmentListener.onLongClickItem(size);
            archiveViewFragment.detailFragmentListener.onScrollRecyclerView(true);
            notifyDataSetChanged();
        }

        public void selectInterval() {
            int size = archiveViewFragment.viewModel.mselecteditems.size();
            if (size < 2) {
                return;
            }
            int last_key = archiveViewFragment.viewModel.mselecteditems.getKeyAtIndex(size - 1);
            int previous_to_last_key = archiveViewFragment.viewModel.mselecteditems.getKeyAtIndex(size - 2);
            if (last_key == previous_to_last_key) {
                return;
            }
            int min = Math.min(last_key, previous_to_last_key);
            int max = Math.max(last_key, previous_to_last_key);
            if (max - min == 1) {
                return;
            }
            for (int i = min + 1; i < max; ++i) {
                archiveViewFragment.viewModel.mselecteditems.put(i, archiveViewFragment.filePOJO_list.get(i).getPath());
            }
            int s = archiveViewFragment.viewModel.mselecteditems.size();
            archiveViewFragment.detailFragmentListener.setFileNumberView(s + "/" + archiveViewFragment.file_list_size);
            archiveViewFragment.detailFragmentListener.onLongClickItem(s);
            archiveViewFragment.detailFragmentListener.onScrollRecyclerView(true);
            notifyDataSetChanged();
        }

        public void deselectAll() {
            archiveViewFragment.detailFragmentListener.onDeselectAll(archiveViewFragment);
        }

        public void setCardViewClickListener(CardViewClickListener listener) {
            this.cardViewClickListener = listener;
        }

        interface CardViewClickListener {
            void onClick(FilePOJO filePOJO);

            void onLongClick(FilePOJO filePOJO, int size);
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, AdapterView.OnLongClickListener {
            final RecyclerViewLayout view;
            int pos;

            ViewHolder(RecyclerViewLayout view) {
                super(view);
                this.view = view;
                this.view.setOnClickListener(this);
                this.view.setOnLongClickListener(this);
            }

            @Override
            public void onClick(View p1) {
                pos = getBindingAdapterPosition();
                int size = archiveViewFragment.viewModel.mselecteditems.size();
                if (size > 0) {
                    longClickMethod(p1, size);
                } else {
                    if (cardViewClickListener != null) {
                        FilePOJO filePOJO = archiveViewFragment.filePOJO_list.get(pos);
                        cardViewClickListener.onClick(filePOJO);
                    }
                }
            }

            private void longClickMethod(View v, int size) {
                pos = getBindingAdapterPosition();
                if (archiveViewFragment.viewModel.mselecteditems.containsKey(pos)) {
                    archiveViewFragment.viewModel.mselecteditems.remove(pos);
                    v.setSelected(false);
                    ((RecyclerViewLayout) v).set_selected(false);
                    --size;
                    if (cardViewClickListener != null) {
                        FilePOJO filePOJO = archiveViewFragment.filePOJO_list.get(pos);
                        cardViewClickListener.onLongClick(filePOJO, size);
                    }
                } else {
                    archiveViewFragment.viewModel.mselecteditems.put(pos, archiveViewFragment.filePOJO_list.get(pos).getPath());
                    v.setSelected(true);
                    ((RecyclerViewLayout) v).set_selected(true);
                    ++size;

                    if (cardViewClickListener != null) {
                        FilePOJO filePOJO = archiveViewFragment.filePOJO_list.get(pos);
                        cardViewClickListener.onLongClick(filePOJO, size);
                    }
                }
                archiveViewFragment.detailFragmentListener.setFileNumberView(size + "/" + archiveViewFragment.file_list_size);
            }

            @Override
            public boolean onLongClick(View p1) {
                longClickMethod(p1, archiveViewFragment.viewModel.mselecteditems.size());
                return true;
            }
        }
    }

    private class TopToolbarClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            ArchiveViewFragment archiveViewFragment = (ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
            if (archiveViewFragment == null) {
                return;
            }
            int id = v.getId();
            if (id == R.id.archive_top_toolbar_back_button) {

            } else if (id == R.id.archive_top_toolbar_parent_dir_image_button) {
                if (Global.IS_CHILD_FILE(Global.ARCHIVE_EXTRACT_DIR.getAbsolutePath(), archiveViewFragment.fileclickselected)) {
                    return;
                }
                File f = new File(archiveViewFragment.fileclickselected);
                String parent_file_path = f.getParent();
                if (parent_file_path == null) {
                    return;
                }

                if (archiveViewFragment.fileObjectType == FileObjectType.FILE_TYPE) {

                    File parent_file = new File(parent_file_path);
                    if (parent_file != null && parent_file.list() != null) {
                        createFragmentTransaction(parent_file.getAbsolutePath(), FileObjectType.FILE_TYPE);
                    }
                }
            } else if (id == R.id.archive_top_toolbar_current_dir_label) {

            } else if (id == R.id.archive_detail_fragment_all_select) {
                if (archiveViewFragment.adapter == null || archiveViewFragment.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }

                if (archiveViewFragment.viewModel.mselecteditems.size() < archiveViewFragment.filePOJO_list.size()) {
                    all_select.setImageResource(R.drawable.deselect_icon);
                    archiveViewFragment.adapter.selectAll();
                } else {
                    all_select.setImageResource(R.drawable.select_icon);
                    archiveViewFragment.adapter.deselectAll();
                }
            } else if (id == R.id.archive_detail_fragment_interval_select) {
                if (archiveViewFragment.adapter == null || archiveViewFragment.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }
                interval_select.setEnabled(false);
                interval_select.setAlpha(Global.DISABLE_ALFA);
                archiveViewFragment.adapter.selectInterval();
            }
        }
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher_all_file_access_permission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
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
                                        activityResultLauncher_all_file_access_permission.launch(intent);
                                    } catch (Exception e) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                        activityResultLauncher_all_file_access_permission.launch(intent);
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

    private class BottomToolbarClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            ArchiveViewFragment archiveViewFragment = (ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
            int id = v.getId();
            if (id == R.id.toolbar_btn_1) {
                if (archiveViewFragment == null) {
                    return;
                }
                if (!search_toolbar_visible) {
                    set_visibility_searchbar(true);
                } else {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }

            } else if (id == R.id.toolbar_btn_2) {
                if (archiveViewFragment == null) {
                    return;
                }
                if (archiveViewFragment.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }
                DeselectAllAndAdjustToolbars(archiveViewFragment, archiveViewFragment.fileclickselected);
                fm.beginTransaction().detach(archiveViewFragment).commit();
                fm.beginTransaction().attach(archiveViewFragment).commit();
                Global.WORKOUT_AVAILABLE_SPACE();
            } else if (id == R.id.toolbar_btn_3) {
                if (archiveViewFragment == null) {
                    return;
                }

                if (archiveViewFragment.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }

                if (!Global.ARCHIVE_EXTRACT_DIR.exists()) {
                    Global.print(context, getString(R.string.could_not_perform_action));
                    return;
                }

                if (ZIP_FILE != null) {
                    activity_progress_bar.setVisibility(View.VISIBLE);
                    viewModel.populateArchiveEntries(archiveViewFragment);
                } else {
                    Global.print(context, getString(R.string.could_not_perform_action));
                    onbackpressed(false);
                }
            } else if (id == R.id.toolbar_btn_4) {
                if (archiveViewFragment == null) {
                    finish();
                }
                if (search_toolbar_visible) {
                    set_visibility_searchbar(false);
                }
                finish();
            }
        }
    }

    private class LocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArchiveViewFragment archiveViewFragment = (ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
            String activity_name = intent.getStringExtra("activity_name");
            String file_path = intent.getStringExtra("file_path");
            FileObjectType fileObjectType = (FileObjectType) intent.getSerializableExtra("fileObjectType");
            switch (intent.getAction()) {

                case Global.LOCAL_BROADCAST_OTHER_ACTIVITY_DELETE_FILE_ACTION:
                    if (archiveViewFragment != null) {
                        archiveViewFragment.local_activity_delete = true;
                    }
                    break;
                case Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION:
                    if (archiveViewFragment != null) {
                        archiveViewFragment.modification_observed = true;
                    }
                    break;
            }
        }
    }


}
