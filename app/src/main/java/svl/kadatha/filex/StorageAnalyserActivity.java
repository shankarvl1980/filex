package svl.kadatha.filex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class StorageAnalyserActivity extends BaseActivity implements MediaMountReceiver.MediaMountListener, DetailFragmentListener, DeleteFileAlertDialog.OKButtonClickListener {
    public static final String ACTIVITY_NAME = "STORAGE_ANALYSER_ACTIVITY";
    static LinkedList<FilePOJO> RECENT = new LinkedList<>();
    final ArrayList<ListPopupWindowPOJO> list_popupwindowpojos = new ArrayList<>();
    public FragmentManager fm;
    public PackageManager pm;
    public TextView current_dir, file_number;
    public RecentDialogListener recentDialogListener;
    public boolean clear_cache;
    public List<FilePOJO> storage_filePOJO_list;
    public Toolbar bottom_toolbar, actionmode_toolbar;
    public String toolbar_shown = "bottom";
    public FloatingActionButton floatingActionButton;
    public EditText search_edittext;
    public boolean search_toolbar_visible;
    private Context context;
    private LocalBroadcastManager localBroadcastManager;
    private MediaMountReceiver mediaMountReceiver;
    private LocalBroadcastReceiver localBroadcastReceiver;
    private ImageButton all_select, interval_select;
    private int countBackPressed = 0;
    private Group search_toolbar;
    private KeyBoardUtil keyBoardUtil;
    private InputMethodManager imm;
    private RepositoryClass repositoryClass;
    private StorageAnalyserActivityViewModel viewModel;
    private PopupWindow listPopWindow;

    // Helpers
    static int resolveAttrColor(Context c, int attr) {
        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(attr, tv, true);
        return tv.resourceId != 0 ? ContextCompat.getColor(c, tv.resourceId) : tv.data;
    }

    static int darken(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.85f; // 15% darker
        return Color.HSVToColor(Color.alpha(color), hsv);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        repositoryClass = RepositoryClass.getRepositoryClass();
        viewModel = new ViewModelProvider(this).get(StorageAnalyserActivityViewModel.class); //required to clear hashmap internal and external storage details on final finish of activity
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
        localBroadcastReceiver = new LocalBroadcastReceiver();
        IntentFilter localBroadcastIntentFilter = new IntentFilter();
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_OTHER_ACTIVITY_DELETE_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION);

        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_CLEAR_CACHE_REFRESH_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_CUT_COPY_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_ARCHIVE_UNARCHIVE_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_COPY_TO_FILE_ACTION);
        localBroadcastManager.registerReceiver(localBroadcastReceiver, localBroadcastIntentFilter);

        fm = getSupportFragmentManager();
        pm = getPackageManager();
        setContentView(R.layout.activity_storage_analyser);
        Window w = getWindow();
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

// Resolve your toolbar color from the current theme and darken it slightly
        int toolbar = resolveAttrColor(this, R.attr.toolbar_background);
        w.setStatusBarColor(darken(toolbar));

        ConstraintLayout root_layout = findViewById(R.id.storage_analyser_root_layout);
        ImageButton back_btn = findViewById(R.id.storage_analyser_back_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        current_dir = findViewById(R.id.storage_analyser_current_dir_tv);
        file_number = findViewById(R.id.storage_analyser_file_number); //initiate here before adding fragment
        Intent intent = getIntent();
        if (intent != null) {
            if (savedInstanceState == null) {
                createFragmentTransaction(Global.GET_INTERNAL_STORAGE_FILE_POJO_STORAGE_DIR().getPath(), Global.GET_INTERNAL_STORAGE_FILE_POJO_STORAGE_DIR().getFileObjectType());
            }
        }

        current_dir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileSelectorRecentDialog fileSelectorRecentDialog = new FileSelectorRecentDialog();
                fileSelectorRecentDialog.show(fm, "storage_analyser_recent_file_dialog");
            }
        });

        ImageButton overflow_img_btn = findViewById(R.id.storage_analyser_overflow);
        overflow_img_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listPopWindow.showAsDropDown(v, 0, Global.LIST_POPUP_WINDOW_DROP_DOWN_OFFSET);
            }
        });
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.clean_icon, getString(R.string.clean_storage), 1));
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
                if (p1 == 0) {
                    StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                    actionModeFinish(storageAnalyserFragment, storageAnalyserFragment.fileclickselected);
                    CleanStorageDialog cleanMemoryDialog = new CleanStorageDialog();
                    cleanMemoryDialog.show(fm, "clean_storage_dialog");
                }
                listPopWindow.dismiss();
            }
        });

        all_select = findViewById(R.id.storage_analyser_all_select);
        all_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if (storageAnalyserFragment.adapter == null || storageAnalyserFragment.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }

                if (storageAnalyserFragment.viewModel.mselecteditems.size() < storageAnalyserFragment.filePOJO_list.size()) {
                    all_select.setImageResource(R.drawable.deselect_icon);
                    storageAnalyserFragment.adapter.selectAll();
                } else {
                    all_select.setImageResource(R.drawable.select_icon);
                    storageAnalyserFragment.adapter.deselectAll();
                }
            }
        });

        interval_select = findViewById(R.id.storage_analyser_interval_select);
        interval_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if (storageAnalyserFragment.adapter == null || storageAnalyserFragment.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }

                interval_select.setEnabled(false);
                interval_select.setAlpha(Global.DISABLE_ALFA);
                storageAnalyserFragment.adapter.selectInterval();
            }
        });

        int imageview_dimension;
        if (Global.RECYCLER_VIEW_FONT_SIZE_FACTOR == 0) {
            imageview_dimension = Global.IMAGEVIEW_DIMENSION_SMALL_LIST;
        } else if (Global.RECYCLER_VIEW_FONT_SIZE_FACTOR == 2) {
            imageview_dimension = Global.IMAGEVIEW_DIMENSION_LARGE_LIST;
        } else {
            imageview_dimension = Global.IMAGEVIEW_DIMENSION_MEDIUM_LIST;
        }

        LinearLayout size_description_layout = findViewById(R.id.storage_analyser_size_description);
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) size_description_layout.getLayoutParams();
        layoutParams.setMargins(imageview_dimension + Global.TEN_DP, 0, 0, 0);

        keyBoardUtil = new KeyBoardUtil(root_layout);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        search_toolbar = findViewById(R.id.storage_analyser_search_toolbar);
        search_edittext = findViewById(R.id.storage_analyser_search_view_edit_text);
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
                StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if (storageAnalyserFragment != null && storageAnalyserFragment.adapter != null) {
                    storageAnalyserFragment.adapter.getFilter().filter(s.toString());
                }
            }
        });

        ImageButton search_cancel_btn = findViewById(R.id.storage_analyser_search_view_cancel_button);
        search_cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSearchBarVisibility(false);
            }
        });

        floatingActionButton = findViewById(R.id.storage_analyser_floating_action_button_back);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onbackpressed(false);
            }
        });

        EquallyDistributedButtonsWithTextLayout tb_layout = new EquallyDistributedButtonsWithTextLayout(this, 4, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] bottom_drawables = {R.drawable.search_icon, R.drawable.refresh_icon, R.drawable.sort_icon, R.drawable.cancel_icon};
        String[] titles = new String[]{getString(R.string.search), getString(R.string.refresh), getString(R.string.sort), getString(R.string.close)};
        tb_layout.setResourceImageDrawables(bottom_drawables, titles);

        bottom_toolbar = findViewById(R.id.storage_analyser_bottom_toolbar);
        bottom_toolbar.addView(tb_layout);

        Button search_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        search_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if (storageAnalyserFragment.progress_bar.getVisibility() == View.VISIBLE) {
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


        Button refresh_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        refresh_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if (storageAnalyserFragment.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }
                fm.beginTransaction().detach(storageAnalyserFragment).commit();
                fm.beginTransaction().attach(storageAnalyserFragment).commit();
            }
        });

        Button sort_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        sort_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StorageAnalyserSortDialog storageAnalyserSortDialog = new StorageAnalyserSortDialog();
                storageAnalyserSortDialog.show(fm, "storage_analyser_sort_dialog");
            }
        });

        Button exit_btn = bottom_toolbar.findViewById(R.id.toolbar_btn_4);
        exit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (search_toolbar_visible) {
                    setSearchBarVisibility(false);
                }
                finish();
            }
        });

        tb_layout = new EquallyDistributedButtonsWithTextLayout(this, 2, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] action_mode_drawables = {R.drawable.delete_icon, R.drawable.properties_icon};
        titles = new String[]{getString(R.string.delete), getString(R.string.properties)};
        tb_layout.setResourceImageDrawables(action_mode_drawables, titles);

        actionmode_toolbar = findViewById(R.id.storage_analyser_action_mode_toolbar);
        actionmode_toolbar.addView(tb_layout);
        Button delete_btn = actionmode_toolbar.findViewById(R.id.toolbar_btn_1);
        delete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if (storageAnalyserFragment.viewModel.mselecteditems.isEmpty()) {
                    Global.print(context, getString(R.string.could_not_perform_action));
                    DeselectAllAndAdjustToolbars(storageAnalyserFragment);
                    return;
                }
                final ArrayList<String> files_selected_array = new ArrayList<>();
                int size = storageAnalyserFragment.viewModel.mselecteditems.size();
                for (int i = 0; i < size; ++i) {
                    files_selected_array.add(storageAnalyserFragment.viewModel.mselecteditems.getValueAtIndex(i));
                }

                DeleteFileAlertDialog deleteFileAlertDialog = DeleteFileAlertDialog.getInstance(files_selected_array, storageAnalyserFragment.fileObjectType, storageAnalyserFragment.fileclickselected, true);
                deleteFileAlertDialog.show(fm, "delete_dialog");
                setSearchBarVisibility(false);
            }
        });

        Button properties_btn = actionmode_toolbar.findViewById(R.id.toolbar_btn_2);
        properties_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if (storageAnalyserFragment.viewModel.mselecteditems.isEmpty()) {
                    Global.print(context, getString(R.string.could_not_perform_action));
                    DeselectAllAndAdjustToolbars(storageAnalyserFragment);
                    return;
                }
                final ArrayList<String> files_selected_array = new ArrayList<>();
                int size = storageAnalyserFragment.viewModel.mselecteditems.size();
                for (int i = 0; i < size; ++i) {
                    files_selected_array.add(storageAnalyserFragment.viewModel.mselecteditems.getValueAtIndex(i));
                }
                PropertiesDialog propertiesDialog = PropertiesDialog.getInstance(files_selected_array, storageAnalyserFragment.fileObjectType);
                propertiesDialog.show(fm, "properties_dialog");
            }
        });

        storage_filePOJO_list = getFilePOJO_list();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onbackpressed(true);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        clear_cache = true;
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("clear_cache", clear_cache);
        outState.putString("toolbar_shown", toolbar_shown);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        clear_cache = savedInstanceState.getBoolean("clear_cache");
        toolbar_shown = savedInstanceState.getString("toolbar_shown");
        switch (toolbar_shown) {
            case "bottom":
                bottom_toolbar.setVisibility(View.VISIBLE);
                actionmode_toolbar.setVisibility(View.GONE);
                break;
            case "action_mode":
                actionmode_toolbar.setVisibility(View.VISIBLE);
                bottom_toolbar.setVisibility(View.GONE);
                break;
        }

        StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
        int size = storageAnalyserFragment.viewModel.mselecteditems.size();
        if (size > 1) {
            interval_select.setVisibility(View.VISIBLE);
            int last_key = storageAnalyserFragment.viewModel.mselecteditems.getKeyAtIndex(size - 1);
            int previous_to_last_key = storageAnalyserFragment.viewModel.mselecteditems.getKeyAtIndex(size - 2);
            if (last_key - previous_to_last_key < -1 || last_key - previous_to_last_key > 1) {
                interval_select.setAlpha(Global.ENABLE_ALFA);
                interval_select.setEnabled(true);
            } else {
                interval_select.setAlpha(Global.DISABLE_ALFA);
                interval_select.setEnabled(false);
            }
        }

        if (size == storageAnalyserFragment.file_list_size && size != 0) {
            all_select.setImageResource(R.drawable.deselect_icon);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (search_toolbar_visible) {
            setSearchBarVisibility(false);
        }

        if (!isFinishing() && !isChangingConfigurations() && clear_cache) {
            clearCache();
        }
    }

    public void setSearchBarVisibility(boolean visible) {
        StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
        if (storageAnalyserFragment.progress_bar.getVisibility() == View.VISIBLE && visible) {
            Global.print(context, getString(R.string.please_wait));
            return;
        }

        search_toolbar_visible = visible;
        if (search_toolbar_visible) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            search_toolbar.setVisibility(View.VISIBLE);
            search_edittext.requestFocus();
        } else {
            imm.hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
            search_toolbar.setVisibility(View.GONE);
            search_edittext.setText("");
            search_edittext.clearFocus();
            DeselectAllAndAdjustToolbars(storageAnalyserFragment);
            if (storageAnalyserFragment.adapter != null) {
                storageAnalyserFragment.adapter.getFilter().filter(null);
            }
        }
    }


    public void clearCache() {
        Global.CLEAR_CACHE();
    }

    @Override
    public void onScrollRecyclerView(boolean showToolBar) {
        if (showToolBar) {
            switch (toolbar_shown) {
                case "bottom":
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    break;
                case "action_mode":
                    actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    break;
            }

        } else {
            switch (toolbar_shown) {
                case "bottom":
                    bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    break;
                case "action_mode":
                    actionmode_toolbar.animate().translationY(actionmode_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    break;
            }
        }
    }

    @Override
    public void actionModeFinish(Fragment fragment, String fileclickeselected) {
        if (fragment instanceof StorageAnalyserFragment) {
            DeselectAllAndAdjustToolbars((StorageAnalyserFragment) fragment);
        }
    }

    @Override
    public void onLongClickItem(int size) {
        StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
        if (storageAnalyserFragment == null) {
            return;
        }
        if (size >= 1) {
            bottom_toolbar.setVisibility(View.GONE);
            actionmode_toolbar.setVisibility(View.VISIBLE);
            toolbar_shown = "action_mode";
            actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
            if (size == 1) {
                interval_select.setAlpha(Global.DISABLE_ALFA);
                interval_select.setEnabled(false);
            } else {
                interval_select.setVisibility(View.VISIBLE);
                int last_key = storageAnalyserFragment.viewModel.mselecteditems.getKeyAtIndex(size - 1);
                int previous_to_last_key = storageAnalyserFragment.viewModel.mselecteditems.getKeyAtIndex(size - 2);
                if (last_key - previous_to_last_key < -1 || last_key - previous_to_last_key > 1) {
                    interval_select.setAlpha(Global.ENABLE_ALFA);
                    interval_select.setEnabled(true);
                } else {
                    interval_select.setAlpha(Global.DISABLE_ALFA);
                    interval_select.setEnabled(false);
                }
            }
            if (size == storageAnalyserFragment.file_list_size) {
                all_select.setImageResource(R.drawable.deselect_icon);
                interval_select.setAlpha(Global.DISABLE_ALFA);
                interval_select.setEnabled(false);
            } else {
                all_select.setImageResource(R.drawable.select_icon);
            }

        } else {
            DeselectAllAndAdjustToolbars(storageAnalyserFragment);
        }
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
        current_dir.setText(current_dir_name);
    }

    @Override
    public void enableParentDirImageButton(boolean enable) {

    }

    @Override
    public void rescanLargeDuplicateFilesLibrary(String type) {
        StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
        if (storageAnalyserFragment == null) {
            return;
        }
        if (storageAnalyserFragment.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
            if (storageAnalyserFragment.progress_bar.getVisibility() == View.VISIBLE) {
                Global.print(context, getString(R.string.please_wait));
            } else {
                actionModeFinish(storageAnalyserFragment, storageAnalyserFragment.fileclickselected);
                if (type.equals("large")) {
                    rescan_large_files_library();
                } else if (type.equals("duplicate")) {
                    rescan_duplicate_files_library();
                }
            }
        } else {
            if (type.equals("large")) {
                rescan_large_files_library();
            } else if (type.equals("duplicate")) {
                rescan_duplicate_files_library();
            }
        }
    }

    @Override
    public void onCreateView(String fileclickselected, FileObjectType fileObjectType) {

    }

    @Override
    public void onDeselectAll(Fragment fragment) {
        if (fragment instanceof StorageAnalyserFragment) {
            DeselectAllAndAdjustToolbars((StorageAnalyserFragment) fragment);
        }
    }

    public void rescan_large_files_library() {
        Global.print(context, getString(R.string.scanning_started));
        ExecutorService executorService = MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<String, List<FilePOJO>>> iterator = repositoryClass.hashmap_file_pojo.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, List<FilePOJO>> entry = iterator.next();
                    if (entry.getKey().equals(FileObjectType.SEARCH_LIBRARY_TYPE + "Large Files")) {
                        iterator.remove();
                        break;
                    }
                }

                //get methods kept below instead of in if block above to avoid likely concurrent modification exception
                viewModel.getLargeFileList(false);
                StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if (storageAnalyserFragment == null) {
                    return;
                }
                if (storageAnalyserFragment.fileclickselected.equals("Large Files")) {
                    fm.beginTransaction().detach(storageAnalyserFragment).commit();
                    fm.beginTransaction().attach(storageAnalyserFragment).commit();
                }
            }
        });
    }


    public void rescan_duplicate_files_library() {
        Global.print(context, getString(R.string.scanning_started));
        ExecutorService executorService = MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<String, List<FilePOJO>>> iterator = repositoryClass.hashmap_file_pojo.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, List<FilePOJO>> entry = iterator.next();
                    if (entry.getKey().equals(FileObjectType.SEARCH_LIBRARY_TYPE + "Duplicate Files")) {
                        iterator.remove();
                        break;
                    }
                }

                //get methods kept below instead of in if block above to avoid likely concurrent modification exception
                viewModel.getDuplicateFileList(false);
                StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if (storageAnalyserFragment == null) {
                    return;
                }
                if (storageAnalyserFragment.fileclickselected.equals("Duplicate Files")) {
                    fm.beginTransaction().detach(storageAnalyserFragment).commit();
                    fm.beginTransaction().attach(storageAnalyserFragment).commit();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaMountReceiver.removeMediaMountListener(this);
        localBroadcastManager.unregisterReceiver(localBroadcastReceiver);
        context.unregisterReceiver(mediaMountReceiver);
        listPopWindow.dismiss(); // to avoid memory leak on orientation change
    }

    private boolean isStale(StorageAnalyserFragment storageAnalyserFragment) {
        if (storageAnalyserFragment == null) return true;
        if (storageAnalyserFragment.fileObjectType == null) return true;
        return storageAnalyserFragment.navSession != NavSessionStore.current(storageAnalyserFragment.fileObjectType);
    }

    private void onbackpressed(boolean onBackPressed) {

        StorageAnalyserFragment saf =
                (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);

        if (saf == null) {
            finish();
            return;
        }

        if (keyBoardUtil.getKeyBoardVisibility()) {
            imm.hideSoftInputFromWindow(search_edittext.getWindowToken(), 0);
            return;
        }

        if (saf.viewModel != null && !saf.viewModel.mselecteditems.isEmpty()) {
            DeselectAllAndAdjustToolbars(saf);
            return;
        }

        if (search_toolbar_visible) {
            setSearchBarVisibility(false);
            return;
        }

        switch (toolbar_shown) {
            case "bottom":
                bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                bottom_toolbar.setVisibility(View.VISIBLE);
                saf.is_toolbar_visible = true;
                break;
            case "action_mode":
                actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                actionmode_toolbar.setVisibility(View.VISIBLE);
                saf.is_toolbar_visible = true;
                break;
        }

        if (fm.getBackStackEntryCount() > 1) {

            // If not safe to pop now, do nothing (no defer)
            if (fm.isStateSaved()) return;

            try {
                fm.popBackStackImmediate(); // normal back step
            } catch (Throwable ignored) {
                return;
            }

            while (fm.getBackStackEntryCount() > 1) {
                Fragment top = fm.findFragmentById(R.id.storage_analyser_container);
                if (!(top instanceof StorageAnalyserFragment)) break;

                StorageAnalyserFragment topFrag = (StorageAnalyserFragment) top;
                String tag = topFrag.getTag();

                // Exempt screens: never auto-pop even if stale
                boolean exempt = "Large Files".equals(tag) || "Duplicate Files".equals(tag);

                if (exempt) break;

                // Missing path -> invalid (for storage analyser you were checking plain File.exists)
                boolean missingPath = (tag == null || !new File(tag).exists());

                // Stale based on NAV_SESSION mismatch
                boolean stale = isStale(topFrag);

                // Pop if stale OR missing path (but not exempt)
                if (stale || missingPath) {
                    if (fm.isStateSaved()) break;
                    try {
                        if (!fm.popBackStackImmediate()) break;
                    } catch (Throwable ignored) {
                        break;
                    }
                    continue;
                }
                break; // valid + not stale
            }

            countBackPressed = 0;
            return;
        }

        // Exit logic
        if (onBackPressed) {
            countBackPressed++;
            if (countBackPressed == 1) {
                Global.print(context, getString(R.string.press_again_to_close_activity));
            } else {
                finish();
            }
        } else {
            Global.print(context, context.getString(R.string.click_close_button_to_exit));
        }
    }


    public List<FilePOJO> getFilePOJO_list() {
        List<FilePOJO> filePOJOS = new ArrayList<>();
        for (FilePOJO filePOJO : repositoryClass.storage_dir) {
            if (filePOJO.getFileObjectType() == FileObjectType.FILE_TYPE) {
                filePOJOS.add(filePOJO);
            }
        }
        return filePOJOS;
    }

    public void createFragmentTransaction(String file_path, FileObjectType fileObjectType) {
        String existingFilePOJOkey = "";
        long existingSession = -1;

        StorageAnalyserFragment saf = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);

        if (saf != null) {
            String fragment_tag = saf.getTag(); // still path
            existingFilePOJOkey = saf.fileObjectType + fragment_tag;
            DeselectAllAndAdjustToolbars(saf);

            Bundle ab = saf.getArguments();
            if (ab != null) existingSession = ab.getLong("NAV_SESSION", -1);
        }

        long currentSession = NavSessionStore.current(fileObjectType);

        if (!(fileObjectType + file_path).equals(existingFilePOJOkey) || existingSession != currentSession) {
            StorageAnalyserFragment safNew = StorageAnalyserFragment.getInstance(fileObjectType);
            Bundle b = safNew.getArguments();
            if (b == null) b = new Bundle();
            b.putLong("NAV_SESSION", currentSession);
            b.putString("FILE_PATH", file_path);
            safNew.setArguments(b);

            fm.beginTransaction()
                    .replace(R.id.storage_analyser_container, safNew, file_path)
                    .addToBackStack(file_path)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commitAllowingStateLoss();
        }
    }


    public void DeselectAllAndAdjustToolbars(StorageAnalyserFragment sad) {
        bottom_toolbar.setVisibility(View.VISIBLE);
        actionmode_toolbar.setVisibility(View.GONE);
        toolbar_shown = "bottom";
        viewModel.tool_bar_shown = "bottom";
        bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
        if (sad != null) {
            sad.clearSelectionAndNotifyDataSetChanged();
            sad.is_toolbar_visible = true;
            all_select.setImageResource(R.drawable.select_icon);
            interval_select.setVisibility(View.GONE);
        }
    }

    @Override
    public void deleteDialogOKButtonClick() {
        final StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
        DeselectAllAndAdjustToolbars(storageAnalyserFragment);
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
                StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if (storageAnalyserFragment != null) {
                    storageAnalyserFragment.clearSelectionAndNotifyDataSetChanged();
                }

                break;
        }
    }

    interface RecentDialogListener {
        void onMediaAttachedAndRemoved();
    }

    public static class StorageAnalyserAdapter extends AbstractStorageAnalyserAdapter {
        final StorageAnalyserFragment storageAnalyserFragment;

        StorageAnalyserAdapter(Context context, StorageAnalyserFragment storageAnalyserFragment) {
            super(context, storageAnalyserFragment);
            this.storageAnalyserFragment = storageAnalyserFragment;
        }

        @Override
        public void onBindViewHolder(ViewHolder p1, int p2) {
            {
                FilePOJO file = storageAnalyserFragment.filePOJO_list.get(p2);
                boolean selected = storageAnalyserFragment.viewModel.mselecteditems.containsKey(p2);
                p1.v.setData(file, selected);
                p1.v.setSelected(selected);
            }
        }
    }

    public static class StorageAnalyserAdapterDivider extends AbstractStorageAnalyserAdapter {
        final StorageAnalyserFragment storageAnalyserFragment;

        StorageAnalyserAdapterDivider(Context context, StorageAnalyserFragment storageAnalyserFragment) {
            super(context, storageAnalyserFragment);
            this.storageAnalyserFragment = storageAnalyserFragment;
        }

        @Override
        public void onBindViewHolder(ViewHolder p1, int p2) {
            {
                FilePOJO file = storageAnalyserFragment.filePOJO_list.get(p2);
                boolean selected = storageAnalyserFragment.viewModel.mselecteditems.containsKey(p2);
                p1.v.setData(file, selected);
                p1.v.setWhetherExternal(file);
                p1.v.setSelected(selected);
                String next_file_name = "";
                String next_file_checksum = "";

                try {
                    next_file_name = storageAnalyserFragment.filePOJO_list.get(p2 + 1).getName();
                    next_file_checksum = storageAnalyserFragment.filePOJO_list.get(p2 + 1).getChecksum();
                } catch (IndexOutOfBoundsException e) {

                }
                boolean whetherToShowDivider = !(next_file_name.equals(file.getName()) && next_file_checksum.equals(file.getChecksum()));
                p1.v.setDivider(whetherToShowDivider);
            }
        }
    }

    public static abstract class AbstractStorageAnalyserAdapter extends RecyclerView.Adapter<AbstractStorageAnalyserAdapter.ViewHolder> implements Filterable {
        final StorageAnalyserFragment storageAnalyserFragment;
        final Context context;

        AbstractStorageAnalyserAdapter(Context context, StorageAnalyserFragment storageAnalyserFragment) {
            this.context = context;
            this.storageAnalyserFragment = storageAnalyserFragment;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            return new ViewHolder(new StorageAnalyserRecyclerViewLayout(context, false));
        }

        @Override
        public abstract void onBindViewHolder(final AbstractStorageAnalyserAdapter.ViewHolder p1, int p2);

        @Override
        public int getItemCount() {
            return storageAnalyserFragment.filePOJO_list.size();
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
                    storageAnalyserFragment.filePOJO_list = new ArrayList<>();
                    if (constraint == null || constraint.length() == 0) {
                        storageAnalyserFragment.filePOJO_list = storageAnalyserFragment.totalFilePOJO_list;
                    } else {
                        String pattern = constraint.toString().toLowerCase().trim();
                        for (int i = 0; i < storageAnalyserFragment.totalFilePOJO_list_Size; ++i) {
                            FilePOJO filePOJO = storageAnalyserFragment.totalFilePOJO_list.get(i);
                            if (filePOJO.getLowerName().contains(pattern)) {
                                storageAnalyserFragment.filePOJO_list.add(filePOJO);
                            }
                        }
                    }

                    storageAnalyserFragment.file_list_size = storageAnalyserFragment.filePOJO_list.size();

                    if (!storageAnalyserFragment.viewModel.mselecteditems.isEmpty()) {
                        storageAnalyserFragment.adapter.deselectAll();
                    } else {
                        notifyDataSetChanged();
                    }

                    if (storageAnalyserFragment.file_list_size > 0) {
                        storageAnalyserFragment.recycler_view.setVisibility(View.VISIBLE);
                        storageAnalyserFragment.folder_empty_textview.setVisibility(View.GONE);
                    }

                    if (storageAnalyserFragment.detailFragmentListener != null) {
                        storageAnalyserFragment.detailFragmentListener.setFileNumberView(storageAnalyserFragment.viewModel.mselecteditems.size() + "/" + storageAnalyserFragment.file_list_size);
                    }
                }
            };
        }

        public void selectAll() {
            storageAnalyserFragment.viewModel.mselecteditems = new IndexedLinkedHashMap<>();
            int size = storageAnalyserFragment.filePOJO_list.size();

            for (int i = 0; i < size; ++i) {
                storageAnalyserFragment.viewModel.mselecteditems.put(i, storageAnalyserFragment.filePOJO_list.get(i).getPath());
            }

            int s = storageAnalyserFragment.viewModel.mselecteditems.size();

            if (storageAnalyserFragment.detailFragmentListener != null) {
                storageAnalyserFragment.detailFragmentListener.setFileNumberView(s + "/" + storageAnalyserFragment.file_list_size);
                storageAnalyserFragment.detailFragmentListener.onLongClickItem(size);
            }
            notifyDataSetChanged();
        }

        public void selectInterval() {
            int size = storageAnalyserFragment.viewModel.mselecteditems.size();
            if (size < 2) {
                return;
            }
            int last_key = storageAnalyserFragment.viewModel.mselecteditems.getKeyAtIndex(size - 1);
            int previous_to_last_key = storageAnalyserFragment.viewModel.mselecteditems.getKeyAtIndex(size - 2);
            if (last_key == previous_to_last_key) {
                return;
            }
            int min = Math.min(last_key, previous_to_last_key);
            int max = Math.max(last_key, previous_to_last_key);
            if (max - min == 1) {
                return;
            }
            for (int i = min + 1; i < max; ++i) {
                storageAnalyserFragment.viewModel.mselecteditems.put(i, storageAnalyserFragment.filePOJO_list.get(i).getPath());
            }
            int s = storageAnalyserFragment.viewModel.mselecteditems.size();

            if (storageAnalyserFragment.detailFragmentListener != null) {
                storageAnalyserFragment.detailFragmentListener.setFileNumberView(s + "/" + storageAnalyserFragment.file_list_size);
                storageAnalyserFragment.detailFragmentListener.onLongClickItem(s);
            }
            notifyDataSetChanged();
        }

        public void deselectAll() {
            if (storageAnalyserFragment.detailFragmentListener != null) {
                storageAnalyserFragment.detailFragmentListener.onDeselectAll(storageAnalyserFragment);
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
            final StorageAnalyserRecyclerViewLayout v;
            FileObjectType fileObjectType;
            int pos;

            ViewHolder(StorageAnalyserRecyclerViewLayout v) {
                super(v);
                this.v = v;
                v.setOnClickListener(this);
                v.setOnLongClickListener(this);
            }

            @Override
            public void onClick(View p1) {
                pos = getBindingAdapterPosition();
                int size = storageAnalyserFragment.viewModel.mselecteditems.size();
                if (size > 0) {
                    longClickMethod(p1, size);
                } else {
                    FilePOJO filePOJO = storageAnalyserFragment.filePOJO_list.get(pos);
                    storageAnalyserFragment.clicked_filepojo = filePOJO;
                    fileObjectType = filePOJO.getFileObjectType();
                    if (filePOJO.getIsDirectory()) {
                        if (storageAnalyserFragment.detailFragmentListener != null) {
                            storageAnalyserFragment.detailFragmentListener.createFragmentTransaction(filePOJO.getPath(), fileObjectType);
                        }
                    } else {
                        storageAnalyserFragment.file_open_intent_dispatch(filePOJO.getPath(), filePOJO.getFileObjectType(), filePOJO.getName(), false, filePOJO.getSizeLong());
                    }
                    FileSelectorRecentDialog.ADD_FILE_POJO_TO_RECENT(filePOJO, FileSelectorRecentDialog.STORAGE_ANALYSER);
                }
            }

            @Override
            public boolean onLongClick(View view) {
                longClickMethod(view, storageAnalyserFragment.viewModel.mselecteditems.size());
                return true;
            }

            private void longClickMethod(View v, int size) {
                pos = getBindingAdapterPosition();
                if (storageAnalyserFragment.viewModel.mselecteditems.containsKey(pos)) {
                    storageAnalyserFragment.viewModel.mselecteditems.remove(pos);
                    v.setSelected(false);
                    ((StorageAnalyserRecyclerViewLayout) v).set_selected(false);
                    --size;
                } else {
                    storageAnalyserFragment.viewModel.mselecteditems.put(pos, storageAnalyserFragment.filePOJO_list.get(pos).getPath());
                    v.setSelected(true);
                    ((StorageAnalyserRecyclerViewLayout) v).set_selected(true);
                    ++size;
                }
                if (storageAnalyserFragment.detailFragmentListener != null) {
                    storageAnalyserFragment.detailFragmentListener.onLongClickItem(size);
                    storageAnalyserFragment.detailFragmentListener.setFileNumberView(size + "/" + storageAnalyserFragment.file_list_size);
                }
            }
        }
    }

    private class LocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
            Bundle bundle = intent.getExtras();
            switch (intent.getAction()) {
                case Global.LOCAL_BROADCAST_OTHER_ACTIVITY_DELETE_FILE_ACTION:
                    if (storageAnalyserFragment != null) {
                        storageAnalyserFragment.local_activity_delete = true;
                    }
                    break;
                case Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION:
                    if (storageAnalyserFragment != null) {
                        storageAnalyserFragment.modification_observed = true;
                    }
                    break;
                case Global.LOCAL_BROADCAST_DELETE_FILE_ACTION:
                    if (bundle != null) {
                        String source_folder = bundle.getString("source_folder");
                        FileObjectType sourceFileObjectType = (FileObjectType) bundle.getSerializable("sourceFileObjectType");
                        String parent_source_folder = new File(source_folder).getParent();
                        if (parent_source_folder == null) {
                            parent_source_folder = source_folder;
                        }

                        if (storageAnalyserFragment != null && storageAnalyserFragment.fileObjectType == sourceFileObjectType) {
                            String tag = storageAnalyserFragment.getTag();
                            if (Global.IS_CHILD_FILE(tag, parent_source_folder)) {
                                storageAnalyserFragment.clearSelectionAndNotifyDataSetChanged();
                            }
                        }
                    }
                    break;
                case Global.LOCAL_BROADCAST_CLEAR_CACHE_REFRESH_ACTION:
                    if (bundle != null) {
                        String file_path = bundle.getString("file_path");
                        FileObjectType sourceFileObjectType = (FileObjectType) bundle.getSerializable("sourceFileObjectType");
                        if (storageAnalyserFragment != null) {
                            storageAnalyserFragment.clear_cache_and_refresh(file_path, sourceFileObjectType);
                        }
                    }
                    break;
                case Global.LOCAL_BROADCAST_CUT_COPY_FILE_ACTION:
                    if (bundle != null) {
                        String dest_folder = bundle.getString("dest_folder");
                        String source_folder = bundle.getString("source_folder");
                        FileObjectType destFileObjectType = (FileObjectType) bundle.getSerializable("destFileObjectType");
                        FileObjectType sourceFileObjectType = (FileObjectType) bundle.getSerializable("sourceFileObjectType");
                        FilePOJO filePOJO = bundle.getParcelable("filePOJO");
                        String parent_dest_folder = new File(dest_folder).getParent();
                        if (parent_dest_folder == null) {
                            parent_dest_folder = dest_folder;
                        }

                        String parent_source_folder = new File(source_folder).getParent();
                        if (parent_source_folder == null) {
                            parent_source_folder = source_folder;
                        }

                        if (storageAnalyserFragment != null) {
                            String tag = storageAnalyserFragment.getTag();

                            if (Global.IS_CHILD_FILE(tag, parent_dest_folder) && storageAnalyserFragment.fileObjectType == destFileObjectType) {
                                storageAnalyserFragment.clearSelectionAndNotifyDataSetChanged();
                            }

                            // in case of cut, to take care of instances of dest_folder is also parent of source folder, it is put in separate if block
                            if (Global.IS_CHILD_FILE(tag, parent_source_folder) && storageAnalyserFragment.fileObjectType == sourceFileObjectType) {
                                storageAnalyserFragment.clearSelectionAndNotifyDataSetChanged();
                            }
                        }
                    }
                    break;
                case Global.LOCAL_BROADCAST_ARCHIVE_UNARCHIVE_FILE_ACTION:
                    if (bundle != null) {
                        String dest_folder = bundle.getString("dest_folder");
                        FileObjectType destFileObjectType = (FileObjectType) bundle.getSerializable("destFileObjectType");
                        FilePOJO filePOJO = bundle.getParcelable("filePOJO");
                        String parent_dest_folder = new File(dest_folder).getParent();
                        if (parent_dest_folder == null) {
                            parent_dest_folder = dest_folder;
                        }
                        if (storageAnalyserFragment != null && storageAnalyserFragment.fileObjectType == destFileObjectType) {
                            String tag = storageAnalyserFragment.getTag();
                            if (Global.IS_CHILD_FILE(tag, parent_dest_folder)) {
                                storageAnalyserFragment.clearSelectionAndNotifyDataSetChanged();
                            }
                        }
                    }
                    break;
                case Global.LOCAL_BROADCAST_COPY_TO_FILE_ACTION:
                    if (bundle != null) {
                        String dest_folder = bundle.getString("dest_folder");
                        FileObjectType destFileObjectType = (FileObjectType) bundle.getSerializable("destFileObjectType");
                        FilePOJO filePOJO = bundle.getParcelable("filePOJO");
                        String parent_dest_folder = new File(dest_folder).getParent();
                        if (parent_dest_folder == null) {
                            parent_dest_folder = dest_folder;
                        }

                        if (storageAnalyserFragment != null) {
                            String tag = storageAnalyserFragment.getTag();
                            if (Global.IS_CHILD_FILE(tag, parent_dest_folder) && storageAnalyserFragment.fileObjectType == destFileObjectType) {
                                storageAnalyserFragment.clearSelectionAndNotifyDataSetChanged();
                            }
                        }
                    }
                    break;
            }
        }
    }
}
