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
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import me.jahnen.libaums.core.UsbMassStorageDevice;
import me.jahnen.libaums.core.fs.FileSystem;
import me.jahnen.libaums.core.fs.UsbFile;
import svl.kadatha.filex.appmanager.AppManagerActivity;
import svl.kadatha.filex.audio.AudioPlayerActivity;
import svl.kadatha.filex.cloud.CloudAuthActivity;
import svl.kadatha.filex.ftpserver.FtpServerActivity;
import svl.kadatha.filex.network.NetworkAccountsDetailsDialog;
import svl.kadatha.filex.usb.ReadAccess;
import svl.kadatha.filex.usb.UsbDocumentProvider;
import svl.kadatha.filex.usb.UsbFileRootSingleton;


public class MainActivity extends BaseActivity implements MediaMountReceiver.MediaMountListener,
        DeleteFileAlertDialog.OKButtonClickListener, DetailFragmentListener {
    public static final String ACTIVITY_NAME = "MAIN_ACTIVITY";
    private static final boolean[] alreadyNotificationWarned = new boolean[1];
    public static FileSystem usbCurrentFs;
    public static boolean USB_ATTACHED;
    public static String SU = "";
    public static boolean SHOW_HIDDEN_FILE;
    public static LinkedList<FilePOJO> RECENT = new LinkedList<>();
    final ArrayList<ListPopupWindowPOJO> list_popupwindowpojos = new ArrayList<>();
    private final int DRAWER_CLOSE_DELAY = 300;
    public Button rename, working_dir_add_btn, working_dir_remove_btn;
    public ImageButton parent_dir_image_button, all_select, interval_select;
    public TinyDB tinyDB;
    public StorageRecyclerAdapter storageRecyclerAdapter;
    public PackageManager pm;
    public FragmentManager fm;
    public boolean search_toolbar_visible;
    public boolean clear_cache;
    public RecentDialogListener recentDialogListener;
    public FloatingActionButton floating_button_back;
    public long search_lower_limit_size = 0;
    public long search_upper_limit_size = 0;
    public String search_file_name;
    public Set<FilePOJO> search_in_dir;
    public String search_file_type;
    public boolean search_whole_word, search_case_sensitive, search_regex;
    public MainActivityViewModel viewModel;
    DrawerLayout drawerLayout;
    TextView file_number_view;
    Toolbar bottom_toolbar, paste_toolbar, actionmode_toolbar;
    LinearLayout search_toolbar;
    ViewGroup drawer;
    TextView current_dir_textview;
    Context context = this;
    ViewPager viewPager;
    ActionModeListener actionModeListener;
    private List<String> library_categories = new ArrayList<>();
    private FilePOJO drawer_storage_file_pojo_selected;
    private ImageView working_dir_expand_indicator, library_expand_indicator, clean_storage_expand_indicator, network_expand_indicator, cloud_expand_indicator;
    private RecyclerView workingDirListRecyclerView;
    private RecyclerView networkRecyclerView, cloudRecyclerView;
    private int countBackPressed = 0;
    private Group working_dir_button_layout;
    private WorkingDirRecyclerAdapter workingDirRecyclerAdapter;
    private ArrayList<String> working_dir_arraylist = new ArrayList<>();
    private PopupWindow listPopWindow;
    private EditText search_view;
    private KeyBoardUtil keyBoardUtil;
    private LocalBroadcastManager localBroadcastManager;
    private LocalBroadcastReceiver localBroadcastReceiver;
    private USBReceiver usbReceiver;
    private InputMethodManager imm;
    private ListView listView;
    private MediaMountReceiver mediaMountReceiver;
    private FileDuplicationViewModel fileDuplicationViewModel;
    private final ActivityResultLauncher<Intent> activityResultLauncher_file_select = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Bundle bundle = result.getData().getBundleExtra("bundle");
                ArrayList<String> files_selected_array = new ArrayList<>(bundle.getStringArrayList("files_selected_array"));
                boolean cut = bundle.getBoolean("cut");
                String source_folder = bundle.getString("source_folder");
                String dest_folder = bundle.getString("dest_folder");
                FileObjectType sourceFileObjectType = (FileObjectType) bundle.getSerializable("sourceFileObjectType");
                FileObjectType destFileObjectType = (FileObjectType) bundle.getSerializable("destFileObjectType");
                DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                df.progress_bar.setVisibility(View.VISIBLE);
                fileDuplicationViewModel.checkForExistingFileWithSameName(source_folder, sourceFileObjectType, dest_folder, destFileObjectType, files_selected_array, cut, false);
            }
        }
    });
    private ListPopupWindowPOJO extract_listPopupWindowPOJO, open_listPopupWindowPOJO;
    private ListPopupWindowPOJO.PopupWindowAdapter popupWindowAdapter;
    private Group usb_eject_layout_group, library_layout_group, clean_storage_layout_group, network_layout_group, cloud_layout_group;
    private Handler h;
    private NestedScrollView nestedScrollView;
    private RepositoryClass repositoryClass;
    private NetworkStateReceiver networkStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        repositoryClass = RepositoryClass.getRepositoryClass();
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

        PermissionsUtil permissionUtil = new PermissionsUtil(context, MainActivity.this);
        permissionUtil.check_permission();
        tinyDB = new TinyDB(context);
        setContentView(R.layout.main);

        viewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        fm = getSupportFragmentManager();
        pm = getPackageManager();
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        h = new Handler();

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

        localBroadcastReceiver = new LocalBroadcastReceiver();
        IntentFilter localBroadcastIntentFilter = new IntentFilter();
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_OTHER_ACTIVITY_DELETE_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_REFRESH_STORAGE_DIR_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_POP_UP_NETWORK_FILE_TYPE_FRAGMENT);

        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_CLEAR_CACHE_REFRESH_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_CUT_COPY_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_ARCHIVE_UNARCHIVE_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_COPY_TO_FILE_ACTION);

        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_CONNECTED_TO_CLOUD_ACTION);
        localBroadcastManager.registerReceiver(localBroadcastReceiver, localBroadcastIntentFilter);

        usbReceiver = new USBReceiver();
        IntentFilter usbIntentFilter = new IntentFilter();
        usbIntentFilter.addAction(UsbDocumentProvider.USB_ATTACH_BROADCAST);
        localBroadcastManager.registerReceiver(usbReceiver, usbIntentFilter);

        networkStateReceiver = new NetworkStateReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawer = findViewById(R.id.drawer_navigation_layout);
        keyBoardUtil = new KeyBoardUtil(drawerLayout);

        nestedScrollView = findViewById(R.id.drawerScrollView);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        search_toolbar = findViewById(R.id.search_bar);

        ImageButton search_detailed_button = search_toolbar.findViewById(R.id.search_bar_detailed_search_button);
        search_detailed_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSearchBarVisibility(false);
                SearchDialog searchDialog = new SearchDialog();
                searchDialog.show(fm, "search_dialog");
            }
        });

        ImageButton search_cancel_button = search_toolbar.findViewById(R.id.search_bar_cancel_button);
        search_cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSearchBarVisibility(false);
            }
        });

        search_view = search_toolbar.findViewById(R.id.search_bar_view);
        search_view.setMaxWidth(Integer.MAX_VALUE);
        search_view.addTextChangedListener(new TextWatcher() {
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
                DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                if (df != null && df.adapter != null) {
                    df.adapter.getFilter().filter(s.toString());
                }
            }
        });

        file_number_view = findViewById(R.id.detail_fragment_file_number);

        ImageButton home_button = findViewById(R.id.top_toolbar_home_button);
        parent_dir_image_button = findViewById(R.id.top_toolbar_parent_dir_image_button);
        current_dir_textview = findViewById(R.id.top_toolbar_current_dir_label);
        all_select = findViewById(R.id.detail_fragment_all_select);
        interval_select = findViewById(R.id.detail_fragment_interval_select);
        TopToolbarClickListener topToolbarClickListener = new TopToolbarClickListener();
        home_button.setOnClickListener(topToolbarClickListener);
        parent_dir_image_button.setOnClickListener(topToolbarClickListener);
        current_dir_textview.setOnClickListener(topToolbarClickListener);
        all_select.setOnClickListener(topToolbarClickListener);
        interval_select.setOnClickListener(topToolbarClickListener);

        floating_button_back = findViewById(R.id.floating_action_button_back);
        floating_button_back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View p1) {
                onbackpressed(false);
            }
        });


        EquallyDistributedButtonsWithTextLayout tb_layout = new EquallyDistributedButtonsWithTextLayout(this, 5, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] bottom_drawables = {R.drawable.search_icon, R.drawable.document_add_icon, R.drawable.refresh_icon, R.drawable.view_icon, R.drawable.exit_icon};
        String[] titles = new String[]{getString(R.string.search), getString(R.string.new_), getString(R.string.refresh), getString(R.string.view), getString(R.string.exit)};
        tb_layout.setResourceImageDrawables(bottom_drawables, titles);
        bottom_toolbar = findViewById(R.id.bottom_toolbar);
        bottom_toolbar.addView(tb_layout);
        Button search = bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        Button create_file = bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        Button refresh = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        Button view = bottom_toolbar.findViewById(R.id.toolbar_btn_4);
        Button finish = bottom_toolbar.findViewById(R.id.toolbar_btn_5);
        BottomToolbarClickListener bottomToolbarClickListener = new BottomToolbarClickListener();
        search.setOnClickListener(bottomToolbarClickListener);
        create_file.setOnClickListener(bottomToolbarClickListener);
        refresh.setOnClickListener(bottomToolbarClickListener);
        view.setOnClickListener(bottomToolbarClickListener);
        finish.setOnClickListener(bottomToolbarClickListener);

        tb_layout = new EquallyDistributedButtonsWithTextLayout(this, 5, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] paste_drawables = {R.drawable.document_add_icon, R.drawable.refresh_icon, R.drawable.paste_icon, R.drawable.delete_icon, R.drawable.cancel_icon};
        titles = new String[]{getString(R.string.new_), getString(R.string.refresh), getString(R.string.paste), getString(R.string.delete), getString(R.string.cancel)};
        tb_layout.setResourceImageDrawables(paste_drawables, titles);
        paste_toolbar = findViewById(R.id.paste_toolbar);
        paste_toolbar.addView(tb_layout);
        Button paste_toolbar_create_file = paste_toolbar.findViewById(R.id.toolbar_btn_1);
        Button paste_toolbar_refresh = paste_toolbar.findViewById(R.id.toolbar_btn_2);
        Button paste = paste_toolbar.findViewById(R.id.toolbar_btn_3);
        Button paste_toolbar_delete = paste_toolbar.findViewById(R.id.toolbar_btn_4);
        Button paste_cancel = paste_toolbar.findViewById(R.id.toolbar_btn_5);

        PasteToolbarClickListener pasteToolbarClickListener = new PasteToolbarClickListener();
        paste_toolbar_create_file.setOnClickListener(pasteToolbarClickListener);
        paste_toolbar_refresh.setOnClickListener(pasteToolbarClickListener);
        paste.setOnClickListener(pasteToolbarClickListener);
        paste_cancel.setOnClickListener(pasteToolbarClickListener);
        paste_toolbar_delete.setOnClickListener(pasteToolbarClickListener);

        tb_layout = new EquallyDistributedButtonsWithTextLayout(this, 5, Global.SCREEN_WIDTH, Global.SCREEN_HEIGHT);
        int[] action_mode_drawables = {R.drawable.cut_icon, R.drawable.copy_icon, R.drawable.rename_icon, R.drawable.delete_icon, R.drawable.overflow_icon};
        titles = new String[]{getString(R.string.cut), getString(R.string.Copy), getString(R.string.rename), getString(R.string.delete), getString(R.string.more)};
        tb_layout.setResourceImageDrawables(action_mode_drawables, titles);
        actionmode_toolbar = findViewById(R.id.action_mode_toolbar);
        actionmode_toolbar.addView(tb_layout);
        Button cut = actionmode_toolbar.findViewById(R.id.toolbar_btn_1);
        Button copy = actionmode_toolbar.findViewById(R.id.toolbar_btn_2);
        rename = actionmode_toolbar.findViewById(R.id.toolbar_btn_3);
        Button delete = actionmode_toolbar.findViewById(R.id.toolbar_btn_4);
        Button overflow = actionmode_toolbar.findViewById(R.id.toolbar_btn_5);

        extract_listPopupWindowPOJO = new ListPopupWindowPOJO(R.drawable.extract_icon, getString(R.string.extract), 6);
        open_listPopupWindowPOJO = new ListPopupWindowPOJO(R.drawable.open_with_icon, getString(R.string.open_with), 7);

        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.share_icon, getString(R.string.send), 1));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.properties_icon, getString(R.string.properties), 2));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.cut_icon, getString(R.string.move_to), 3));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.copy_icon, getString(R.string.copy_to), 4));
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.compress_popup_list_icon, getString(R.string.compress), 5));
        list_popupwindowpojos.add(extract_listPopupWindowPOJO);
        list_popupwindowpojos.add(open_listPopupWindowPOJO);


        listPopWindow = new PopupWindow(context);
        listView = new ListView(context);
        popupWindowAdapter = new ListPopupWindowPOJO.PopupWindowAdapter(context, list_popupwindowpojos);
        listView.setAdapter(popupWindowAdapter);
        listPopWindow.setContentView(listView);
        listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popup_window_width));
        listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopWindow.setFocusable(true);
        listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.list_popup_background));

        actionModeListener = new ActionModeListener();
        cut.setOnClickListener(actionModeListener);
        copy.setOnClickListener(actionModeListener);
        delete.setOnClickListener(actionModeListener);
        rename.setOnClickListener(actionModeListener);
        overflow.setOnClickListener(actionModeListener);

		/*
		viewPager=(ViewPager)findViewById(R.id.view_pager);
		viewPager.setOffscreenPageLimit(10);
		FragmentViewPager viewPagerAdapter=new FragmentViewPager(getSupportFragmentManager(),context);
		viewPager.setAdapter(viewPagerAdapter);

*/
        Global.WARN_NOTIFICATIONS_DISABLED(context, NotifManager.CHANNEL_ID, alreadyNotificationWarned);

        viewModel.isDeletionCompleted.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                if (df == null) {
                    return;
                }
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    df.progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    df.progress_bar.setVisibility(View.GONE);
                    viewModel.isDeletionCompleted.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        fileDuplicationViewModel = new ViewModelProvider(this).get(FileDuplicationViewModel.class);
        fileDuplicationViewModel.asyncTaskStatus.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                if (asyncTaskStatus == AsyncTaskStatus.STARTED) {
                    df.progress_bar.setVisibility(View.VISIBLE);
                } else if (asyncTaskStatus == AsyncTaskStatus.COMPLETED) {
                    df.progress_bar.setVisibility(View.GONE);
                    if (fileDuplicationViewModel.source_duplicate_file_path_array.isEmpty()) {
                        PasteSetUpDialog pasteSetUpDialog = PasteSetUpDialog.getInstance(fileDuplicationViewModel.source_folder, fileDuplicationViewModel.sourceFileObjectType,
                                fileDuplicationViewModel.dest_folder, fileDuplicationViewModel.destFileObjectType, fileDuplicationViewModel.sourceFileDestNameMap, fileDuplicationViewModel.overwritten_file_path_list,
                                fileDuplicationViewModel.cut);
                        pasteSetUpDialog.show(fm, "paste_dialog");
                    } else {
                        FileReplaceConfirmationDialog fileReplaceConfirmationDialog = FileReplaceConfirmationDialog.getInstance(fileDuplicationViewModel.source_folder, fileDuplicationViewModel.sourceFileObjectType,
                                fileDuplicationViewModel.dest_folder, fileDuplicationViewModel.destFileObjectType, fileDuplicationViewModel.files_selected_array, null, null, fileDuplicationViewModel.cut);
                        fileReplaceConfirmationDialog.show(fm, "paste_dialog");
                    }
                    fileDuplicationViewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        SHOW_HIDDEN_FILE = tinyDB.getBoolean("show_hidden_file");

        RecyclerView storageDirListRecyclerView = findViewById(R.id.drawer_recyclerview);
        storageDirListRecyclerView.addItemDecoration(Global.DIVIDERITEMDECORATION);

        storageDirListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        storageRecyclerAdapter = new StorageRecyclerAdapter(repositoryClass.storage_dir);
        storageDirListRecyclerView.setAdapter(storageRecyclerAdapter);

        usb_eject_layout_group = findViewById(R.id.usb_group);
        View usb_background = findViewById(R.id.usb_background);
        usb_background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final BlankFragment pbf = BlankFragment.newInstance();
                pbf.show(fm, "");
                //drawerLayout.closeDrawer(drawer);
                Global.LOCAL_BROADCAST(UsbDocumentProvider.ACTION_USB_EJECT, LocalBroadcastManager.getInstance(context), null);
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pbf.dismissAllowingStateLoss();
                    }
                }, DRAWER_CLOSE_DELAY);
            }
        });
        usb_eject_layout_group.setVisibility(USB_ATTACHED ? View.VISIBLE : View.GONE);
        View working_dir_heading_layout = findViewById(R.id.working_dir_layout_background);
        working_dir_heading_layout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (workingDirListRecyclerView.getVisibility() == View.GONE) {
                    working_dir_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.right_arrow_drawer_icon));
                    workingDirListRecyclerView.setVisibility(View.VISIBLE);
                    working_dir_button_layout.setVisibility(View.VISIBLE);
                    viewModel.working_dir_open = true;
                } else {
                    working_dir_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.down_arrow_drawer_icon));
                    workingDirListRecyclerView.setVisibility(View.GONE);
                    working_dir_button_layout.setVisibility(View.GONE);
                    viewModel.working_dir_open = false;
                }
            }
        });

        working_dir_expand_indicator = findViewById(R.id.working_dir_expand_indicator);
        workingDirListRecyclerView = findViewById(R.id.working_dir_recyclerview);
        workingDirListRecyclerView.addItemDecoration(Global.DIVIDERITEMDECORATION);
        working_dir_arraylist = tinyDB.getListString("working_dir_arraylist");
        workingDirRecyclerAdapter = new WorkingDirRecyclerAdapter(context, working_dir_arraylist);
        workingDirRecyclerAdapter.setOnItemClickListenerForWorkingDirAdapter(new WorkingDirRecyclerAdapter.ItemClickListener() {
            public void onItemClick(int pos, String item_name) {
                File f = new File(working_dir_arraylist.get(pos));
                drawerLayout.closeDrawer(drawer);
                if (!f.exists()) {
                    Global.print(context, getString(R.string.directory_does_not_exist));
                } else {
                    drawer_storage_file_pojo_selected = new FilePOJO(FileObjectType.FILE_TYPE, f.getName(), null, f.getAbsolutePath(), true, 0L, null, 0L, null, R.drawable.folder_icon, null, 0, 0, 0);
                }
            }
        });
        workingDirListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        workingDirListRecyclerView.setAdapter(workingDirRecyclerAdapter);
        setRecyclerViewHeight(workingDirListRecyclerView);

        working_dir_button_layout = findViewById(R.id.working_dir_button_group);
        working_dir_add_btn = findViewById(R.id.working_dir_add_btn);
        working_dir_add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                workingDirAdd();
            }
        });
        working_dir_remove_btn = findViewById(R.id.working_dir_remove_btn);
        working_dir_remove_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                workingDirRemove();
            }
        });

        library_layout_group = findViewById(R.id.library_layout_group);
        View library_heading_layout = findViewById(R.id.library_layout_background);
        library_heading_layout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (library_layout_group.getVisibility() == View.GONE) {
                    library_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.right_arrow_drawer_icon));
                    library_layout_group.setVisibility(View.VISIBLE);
                    nestedScrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            nestedScrollView.smoothScrollTo(0, library_expand_indicator.getTop());
                        }
                    });
                    viewModel.library_or_search_shown = true;
                } else {
                    library_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.down_arrow_drawer_icon));
                    library_layout_group.setVisibility(View.GONE);
                    viewModel.library_or_search_shown = false;
                }
            }
        });

        library_expand_indicator = findViewById(R.id.library_expand_indicator);
        library_categories = Arrays.asList(getResources().getStringArray(R.array.library_categories));
        RecyclerView libraryRecyclerView = findViewById(R.id.library_recyclerview);
        libraryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        libraryRecyclerView.addItemDecoration(Global.DIVIDERITEMDECORATION);
        int[] icon_image_array = {R.drawable.lib_download_icon, R.drawable.lib_doc_icon, R.drawable.lib_image_icon, R.drawable.lib_audio_icon, R.drawable.lib_video_icon, R.drawable.compress_icon, R.drawable.android_os_outlined_icon};
        libraryRecyclerView.setAdapter(new LibraryRecyclerAdapter(library_categories, icon_image_array));

        View library_scan_heading_layout = findViewById(R.id.library_scan_label_background);
        library_scan_heading_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final BlankFragment pbf = BlankFragment.newInstance();
                pbf.show(fm, "");
                drawerLayout.closeDrawer(drawer);

                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                        if (df.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                            if (df.progress_bar.getVisibility() == View.VISIBLE) {
                                Global.print(context, getString(R.string.please_wait));
                            } else {
                                action_mode_finish(df);
                                rescanLibrary();
                            }
                        } else {
                            rescanLibrary();
                        }
                        pbf.dismissAllowingStateLoss();
                    }
                }, DRAWER_CLOSE_DELAY);
            }
        });

        SwitchCompat switchHideFile = findViewById(R.id.switch_hide_file);
        switchHideFile.setChecked(MainActivity.SHOW_HIDDEN_FILE);
        switchHideFile.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton cb, final boolean checked) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        MainActivity.SHOW_HIDDEN_FILE = checked;
                        tinyDB.putBoolean("show_hidden_file", MainActivity.SHOW_HIDDEN_FILE);
                        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                        action_mode_finish(df);
                        if (df.fileObjectType == FileObjectType.FILE_TYPE || df.fileObjectType == FileObjectType.ROOT_TYPE) {
                            fm.beginTransaction().detach(df).commit();
                            fm.beginTransaction().attach(df).commit();
                        }
                    }
                }, DRAWER_CLOSE_DELAY);
            }
        });

        View audio_player_heading_layout = findViewById(R.id.audio_player_label_background);
        audio_player_heading_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clear_cache = false;
                //final ProgressBarFragment pbf = ProgressBarFragment.newInstance();
                final BlankFragment pbf = BlankFragment.newInstance();
                pbf.show(fm, "");
                drawerLayout.closeDrawer(drawer);

                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                        action_mode_finish(df);
                        Intent intent = new Intent(context, AudioPlayerActivity.class);
                        startActivity(intent);
                        pbf.dismissAllowingStateLoss();
                    }
                }, DRAWER_CLOSE_DELAY);
            }
        });

        View storage_analyser_heading_layout = findViewById(R.id.storage_analyser_label_background);
        storage_analyser_heading_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clear_cache = false;
                //final ProgressBarFragment pbf = ProgressBarFragment.newInstance();
                final BlankFragment pbf = BlankFragment.newInstance();
                pbf.show(fm, "");
                drawerLayout.closeDrawer(drawer);
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                        action_mode_finish(df);
                        Intent intent = new Intent(context, StorageAnalyserActivity.class);
                        startActivity(intent);
                        pbf.dismissAllowingStateLoss();
                    }
                }, DRAWER_CLOSE_DELAY);
            }
        });

        clean_storage_layout_group = findViewById(R.id.clean_storage_layout_group);
        View clean_storage_heading_layout = findViewById(R.id.clean_storage_layout_background);
        clean_storage_heading_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (clean_storage_layout_group.getVisibility() == View.GONE) {
                    clean_storage_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.right_arrow_drawer_icon));
                    clean_storage_layout_group.setVisibility(View.VISIBLE);
                    nestedScrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            //nestedScrollView.smoothScrollTo(0, library_expand_indicator.getTop());
                        }
                    });
                    viewModel.clean_storage_shown = true;
                } else {
                    clean_storage_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.down_arrow_drawer_icon));
                    clean_storage_layout_group.setVisibility(View.GONE);
                    viewModel.clean_storage_shown = false;
                }
            }
        });

        clean_storage_expand_indicator = findViewById(R.id.clean_storage_expand_indicator);
        List<String> clean_storage_categories = Arrays.asList(getResources().getStringArray(R.array.clean_storage_categories));
        RecyclerView clean_storage_recyclerView = findViewById(R.id.clean_storage_recyclerview);
        clean_storage_recyclerView.setLayoutManager(new LinearLayoutManager(this));
        clean_storage_recyclerView.addItemDecoration(Global.DIVIDERITEMDECORATION);
        int[] clean_icon_image_array = {R.drawable.scan_drawer_icon, R.drawable.scan_drawer_icon};
        clean_storage_recyclerView.setAdapter(new CleanStorageRecyclerAdapter(clean_storage_categories, clean_icon_image_array));

        View app_manager_heading_layout = findViewById(R.id.app_manager_label_background);
        app_manager_heading_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clear_cache = false;
                //final ProgressBarFragment pbf = ProgressBarFragment.newInstance();
                final BlankFragment pbf = BlankFragment.newInstance();
                pbf.show(fm, "");
                drawerLayout.closeDrawer(drawer);
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                        action_mode_finish(df);
                        Intent intent = new Intent(context, AppManagerActivity.class);
                        startActivity(intent);
                        pbf.dismissAllowingStateLoss();
                    }
                }, DRAWER_CLOSE_DELAY);
            }
        });

        View search_heading_layout = findViewById(R.id.search_label_background);
        search_heading_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //final ProgressBarFragment pbf = ProgressBarFragment.newInstance();
                final BlankFragment pbf = BlankFragment.newInstance();
                pbf.show(fm, "");
                drawerLayout.closeDrawer(drawer);
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                        action_mode_finish(df);
                        SearchDialog searchDialog = new SearchDialog();
                        searchDialog.show(fm, "search_dialog");
                        pbf.dismissAllowingStateLoss();
                    }
                }, DRAWER_CLOSE_DELAY);
            }
        });

        View access_pc_heading_layout = findViewById(R.id.access_pc_label_background);
        access_pc_heading_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //final ProgressBarFragment pbf = ProgressBarFragment.newInstance();
                final BlankFragment pbf = BlankFragment.newInstance();
                pbf.show(fm, "");
                drawerLayout.closeDrawer(drawer);
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                        action_mode_finish(df);
                        Intent intent = new Intent(context, FtpServerActivity.class);
                        startActivity(intent);
                        pbf.dismissAllowingStateLoss();
                    }
                }, DRAWER_CLOSE_DELAY);
            }
        });

        network_layout_group = findViewById(R.id.network_layout_group);
        View network_heading_layout = findViewById(R.id.network_layout_background);
        network_heading_layout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (network_layout_group.getVisibility() == View.GONE) {
                    network_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.right_arrow_drawer_icon));
                    network_layout_group.setVisibility(View.VISIBLE);
                    nestedScrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            nestedScrollView.smoothScrollTo(0, networkRecyclerView.getBottom());
                        }
                    });
                    viewModel.network_shown = true;
                } else {
                    network_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.down_arrow_drawer_icon));
                    network_layout_group.setVisibility(View.GONE);
                    viewModel.network_shown = false;
                }
            }
        });

        network_expand_indicator = findViewById(R.id.network_expand_indicator);
        List<String> network_types = Arrays.asList(getResources().getStringArray(R.array.network_types));
        networkRecyclerView = findViewById(R.id.network_recyclerview);
        networkRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        networkRecyclerView.addItemDecoration(Global.DIVIDERITEMDECORATION);
        int[] network_icon_image_array = {R.drawable.network_icon, R.drawable.network_icon, R.drawable.network_icon, R.drawable.network_icon};
        networkRecyclerView.setAdapter(new NetworkRecyclerAdapter(network_types, network_icon_image_array));

        cloud_layout_group = findViewById(R.id.cloud_layout_group);
        View cloud_heading_layout = findViewById(R.id.cloud_layout_background);
        cloud_heading_layout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (cloud_layout_group.getVisibility() == View.GONE) {
                    cloud_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.right_arrow_drawer_icon));
                    cloud_layout_group.setVisibility(View.VISIBLE);
                    nestedScrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            nestedScrollView.smoothScrollTo(0, networkRecyclerView.getBottom());
                        }
                    });
                    viewModel.cloud_shown = true;
                } else {
                    cloud_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.down_arrow_drawer_icon));
                    cloud_layout_group.setVisibility(View.GONE);
                    viewModel.cloud_shown = false;
                }
            }
        });

        cloud_expand_indicator = findViewById(R.id.cloud_expand_indicator);
        List<String> cloud_types = Arrays.asList(getResources().getStringArray(R.array.cloud_types));
        cloudRecyclerView = findViewById(R.id.cloud_recyclerview);
        cloudRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cloudRecyclerView.addItemDecoration(Global.DIVIDERITEMDECORATION);
        int[] cloud_icon_image_array = {R.drawable.cloud_icon, R.drawable.cloud_icon, R.drawable.cloud_icon};
        cloudRecyclerView.setAdapter(new CloudRecyclerAdapter(cloud_types, cloud_icon_image_array));


        int drawer_width = (int) getResources().getDimension(R.dimen.drawer_width_with_padding);
        tb_layout = new EquallyDistributedButtonsWithTextLayout(this, 2, drawer_width, drawer_width);
        int[] drawer_end_drawables = {R.drawable.exit_drawer_icon, R.drawable.settings_drawer_icon};
        titles = new String[]{getString(R.string.exit), getString(R.string.settings)};
        tb_layout.setResourceImageDrawables(drawer_end_drawables, titles);
        ViewGroup drawer_end_butt = findViewById(R.id.drawer_end_butt_layout);
        drawer_end_butt.addView(tb_layout);
        int color = getResources().getColor(R.color.light_heading_text_color);
        Button exit = drawer_end_butt.findViewById(R.id.toolbar_btn_1);
        exit.setTextColor(color);
        Button settings = drawer_end_butt.findViewById(R.id.toolbar_btn_2);
        settings.setTextColor(color);
        DrawerEndButtButtonsClickListener drawerEndButtButtonsClickListener = new DrawerEndButtButtonsClickListener();
        exit.setOnClickListener(drawerEndButtButtonsClickListener);
        settings.setOnClickListener(drawerEndButtButtonsClickListener);

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            public void onDrawerOpened(View v) {
                drawer_storage_file_pojo_selected = null;
            }

            public void onDrawerClosed(View v) {
                if (drawer_storage_file_pojo_selected != null) {
                    FileObjectType fileObjectType = drawer_storage_file_pojo_selected.getFileObjectType();
                    createFragmentTransaction(drawer_storage_file_pojo_selected.getPath(), fileObjectType);
                    drawer_storage_file_pojo_selected = null;
                }
            }

            public void onDrawerStateChanged(int p) {
            }

            public void onDrawerSlide(View v, float f) {
            }
        });

        setupUsbDevice();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onbackpressed(true);
            }
        });

        if (savedInstanceState == null) {
            createFragmentTransaction(Global.INTERNAL_PRIMARY_STORAGE_PATH, FileObjectType.FILE_TYPE);
            Intent intent = getIntent();
            if (intent != null) {
                onNewIntent(intent);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SEND_MULTIPLE) || action.equals(Intent.ACTION_SEND)) {
            viewModel.send_intent = intent;
            on_intent(viewModel.send_intent);
        }
    }

    private void on_intent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SEND_MULTIPLE) || action.equals(Intent.ACTION_SEND)) {
                DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                if (df != null) {
                    df.clearSelectionAndNotifyDataSetChanged();
                    paste_pastecancel_view_procedure(df);
                    DetailFragment.COPY_SELECTED = true;
                    action_mode_finish(df);
                }
            }
        }
    }

    private void rescanLibrary() {
        Global.print(context, getString(R.string.scanning_started));
        ExecutorService executorService = MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                boolean download_removed = false, document_removed = false, image_removed = false, audio_removed = false, video_removed = false, archive_removed = false, apk_removed = false;
                Iterator<Map.Entry<String, List<FilePOJO>>> iterator = repositoryClass.hashmap_file_pojo.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, List<FilePOJO>> entry = iterator.next();
                    if (entry.getKey().equals(FileObjectType.SEARCH_LIBRARY_TYPE + "Download")) {
                        iterator.remove();
                        download_removed = true;
                    } else if (entry.getKey().equals(FileObjectType.SEARCH_LIBRARY_TYPE + "Document")) {
                        iterator.remove();
                        document_removed = true;
                    } else if (entry.getKey().equals(FileObjectType.SEARCH_LIBRARY_TYPE + "Image")) {
                        iterator.remove();
                        image_removed = true;
                    } else if (entry.getKey().equals(FileObjectType.SEARCH_LIBRARY_TYPE + "Audio")) {
                        iterator.remove();
                        audio_removed = true;
                    } else if (entry.getKey().equals(FileObjectType.SEARCH_LIBRARY_TYPE + "Video")) {
                        iterator.remove();
                        video_removed = true;
                    } else if (entry.getKey().equals(FileObjectType.SEARCH_LIBRARY_TYPE + "Archive")) {
                        iterator.remove();
                        archive_removed = true;
                    } else if (entry.getKey().equals(FileObjectType.SEARCH_LIBRARY_TYPE + "APK")) {
                        iterator.remove();
                        apk_removed = true;
                    }

                    if (download_removed && document_removed && image_removed && audio_removed && video_removed && archive_removed && apk_removed) {
                        break;
                    }
                }
                //get methods kept below instead of in if block above to avoid likely concurrent modification exception
                viewModel.getDownloadList(false);
                viewModel.getDocumentList(false);
                viewModel.getImageList(false);
                viewModel.getAudioList(false);
                viewModel.getVideoList(false);
                viewModel.getArchiveList(false);
                viewModel.getApkList(false);

                DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                if (df == null) {
                    return;
                }
                if (df.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                    fm.beginTransaction().detach(df).commit();
                    fm.beginTransaction().attach(df).commit();
                }
            }
        });
    }

    public void rescanLargeDuplicateFilesLibrary(String type) {
        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
        if (df == null) {
            return;
        }

        if (df.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
            if (df.progress_bar.getVisibility() == View.VISIBLE) {
                Global.print(context, getString(R.string.please_wait));
            } else {
                action_mode_finish(df);
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
        if (viewModel.send_intent != null) {
            on_intent(viewModel.send_intent);
        }
    }

    @Override
    public void onDeselectAll(Fragment fragment) {
        if (fragment instanceof DetailFragment) {
            DeselectAllAndAdjustToolbars((DetailFragment) fragment);
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
                DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                if (df == null) {
                    return;
                }
                if (df.fileclickselected.equals("Large Files")) {
                    fm.beginTransaction().detach(df).commit();
                    fm.beginTransaction().attach(df).commit();
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
                DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                if (df == null) {
                    return;
                }
                if (df.fileclickselected.equals("Duplicate Files")) {
                    fm.beginTransaction().detach(df).commit();
                    fm.beginTransaction().attach(df).commit();
                }
            }
        });
    }

    private void createLibraryCache() {
        viewModel.getDownloadList(false);
        viewModel.getDocumentList(false);
        viewModel.getImageList(false);
        viewModel.getAudioList(false);
        viewModel.getVideoList(false);
        viewModel.getArchiveList(false);
        viewModel.getApkList(false);
        viewModel.getLargeFileList(false);
        viewModel.getDuplicateFileList(false);

        viewModel.getAppList();

        viewModel.getAudioPOJOList(false);
        viewModel.getAlbumList(false);
    }

    public void setSearchBarVisibility(boolean visible) {
        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
        if (df.progress_bar.getVisibility() == View.VISIBLE) {
            Global.print(context, getString(R.string.please_wait));
            return;
        }
        search_toolbar_visible = visible;
        if (search_toolbar_visible) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            search_toolbar.setVisibility(View.VISIBLE);
            search_view.requestFocus();
        } else {
            action_mode_finish(df);
        }
    }

    @Override
    public SearchParameters getSearchParameters() {
        return new SearchParameters(search_file_name, search_in_dir, search_file_type, search_whole_word, search_case_sensitive, search_regex, search_lower_limit_size, search_upper_limit_size);
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
                switch (permission) {
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(permission)) {
                                showDialogOK(getString(R.string.read_and_write_permissions_are_must_for_the_app_to_work_please_grant_permissions), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                new PermissionsUtil(context, MainActivity.this).check_permission();
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

                    case Manifest.permission.READ_PHONE_STATE:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(permission)) {
                                showDialogOK(getString(R.string.permission_required_to_regulate_audio_play_when_phone_rings), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                new PermissionsUtil(context, MainActivity.this).check_permission();
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
                                                new PermissionsUtil(context, MainActivity.this).check_permission();
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

    @Override
    protected void onStart() {
        super.onStart();
        clear_cache = true;
        Global.WORKOUT_AVAILABLE_SPACE();
        createLibraryCache();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (search_toolbar_visible) {
            setSearchBarVisibility(false);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("custom_dir_selected_hash_map", workingDirRecyclerAdapter.custom_dir_selected_hash_map);
        outState.putStringArrayList("custom_dir_selected_array", workingDirRecyclerAdapter.custom_dir_selected_array);
        outState.putBoolean("clear_cache", clear_cache);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
        switch (viewModel.toolbar_shown) {
            case "action_mode":
                actionmode_toolbar.setVisibility(View.VISIBLE);
                bottom_toolbar.setVisibility(View.GONE);
                paste_toolbar.setVisibility(View.GONE);
                break;
            case "paste":
                paste_toolbar.setVisibility(View.VISIBLE);
                bottom_toolbar.setVisibility(View.GONE);
                actionmode_toolbar.setVisibility(View.GONE);
                break;
        }

        int size = df.viewModel.mselecteditems.size();
        if (size > 1) {
            rename.setEnabled(false);
            rename.setAlpha(Global.DISABLE_ALFA);
            interval_select.setVisibility(View.VISIBLE);
            int last_key = df.viewModel.mselecteditems.getKeyAtIndex(size - 1);
            int previous_to_last_key = df.viewModel.mselecteditems.getKeyAtIndex(size - 2);
            if (last_key - previous_to_last_key < -1 || last_key - previous_to_last_key > 1) {
                interval_select.setAlpha(Global.ENABLE_ALFA);
                interval_select.setEnabled(true);
            } else {
                interval_select.setAlpha(Global.DISABLE_ALFA);
                interval_select.setEnabled(false);
            }

        }
        if (size == df.file_list_size && size != 0) {
            all_select.setImageResource(R.drawable.deselect_icon);
        }

        if (viewModel.working_dir_open) {
            working_dir_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.right_arrow_drawer_icon));
            workingDirListRecyclerView.setVisibility(View.VISIBLE);
            working_dir_button_layout.setVisibility(View.VISIBLE);
        }

        workingDirRecyclerAdapter.custom_dir_selected_hash_map = (HashMap<Integer, Boolean>) savedInstanceState.getSerializable("custom_dir_selected_hash_map");
        workingDirRecyclerAdapter.custom_dir_selected_array = savedInstanceState.getStringArrayList("custom_dir_selected_array");

        if (viewModel.library_or_search_shown) {
            library_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.right_arrow_drawer_icon));
            library_layout_group.setVisibility(View.VISIBLE);
        }

        if (viewModel.clean_storage_shown) {
            clean_storage_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.right_arrow_drawer_icon));
            clean_storage_layout_group.setVisibility(View.VISIBLE);
        }

        if (viewModel.network_shown) {
            network_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.right_arrow_drawer_icon));
            network_layout_group.setVisibility(View.VISIBLE);
        }

        if (viewModel.cloud_shown) {
            cloud_expand_indicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.right_arrow_drawer_icon));
            cloud_layout_group.setVisibility(View.VISIBLE);
        }

        clear_cache = savedInstanceState.getBoolean("clear_cache");
    }

    public void createFragmentTransaction(String file_path, FileObjectType fileObjectType) {
        String fragment_tag;
        String existingFilePOJOkey = "";
        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
        if (df != null) {
            fragment_tag = df.getTag();
            existingFilePOJOkey = df.fileObjectType + fragment_tag;
            action_mode_finish(df); //string provided to action_mode_finish method is file_path (which is clicked, not the existing file_path) to be created of fragemnttransaction
        }

        if (file_path.equals(DetailFragment.SEARCH_RESULT)) {
            fm.beginTransaction().replace(R.id.detail_fragment, DetailFragment.getInstance(fileObjectType), file_path)
                    .addToBackStack(file_path).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commitAllowingStateLoss();

        } else if (DetailFragment.TO_BE_MOVED_TO_FILE_POJO != null && !(fileObjectType + file_path).equals(existingFilePOJOkey)) {
            fm.beginTransaction().replace(R.id.detail_fragment, DetailFragment.getInstance(fileObjectType), file_path)
                    .addToBackStack(file_path).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commitAllowingStateLoss(); //committing allowing state loss becuase it is committed after onsavedinstance

        } else if (!(fileObjectType + file_path).equals(existingFilePOJOkey)) {
            fm.beginTransaction().replace(R.id.detail_fragment, DetailFragment.getInstance(fileObjectType), file_path)
                    .addToBackStack(file_path).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commitAllowingStateLoss();
        }
    }

    private void onbackpressed(boolean onBackPressed) {
        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawer);
        if (drawerOpen) {
            drawerLayout.closeDrawer(drawer);
        } else if (keyBoardUtil.getKeyBoardVisibility()) {
            imm.hideSoftInputFromWindow(search_view.getWindowToken(), 0);
        } else if (!df.viewModel.mselecteditems.isEmpty()) {
            action_mode_finish(df);
        } else if (search_toolbar_visible) {
            setSearchBarVisibility(false);
        } else if (df.viewModel.library_filter_path != null) {
            df.filepath_adapter = df.new FilePathRecyclerViewAdapter(df.fileclickselected);
            df.filepath_recyclerview.setAdapter(df.filepath_adapter);
            df.viewModel.library_filter_path = null;
            df.adapter.getFilter().filter(null);
        } else {
            switch (viewModel.toolbar_shown) {
                case "bottom":
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    bottom_toolbar.setVisibility(View.VISIBLE);
                    df.is_toolbar_visible = true;
                    break;
                case "paste":
                    paste_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    paste_toolbar.setVisibility(View.VISIBLE);
                    df.is_toolbar_visible = true;
                    break;
            }
            int entry_count;
            if ((entry_count = fm.getBackStackEntryCount()) > 1) {
                fm.popBackStack();
                int frag = 2;
                df = (DetailFragment) fm.findFragmentByTag(fm.getBackStackEntryAt(entry_count - frag).getName());
                String df_tag = df.getTag();
                while (!(df.fileObjectType == FileObjectType.FILE_TYPE && new File(df_tag).exists()) && !library_categories.contains(df_tag) && !df_tag.equals("Large Files")
                        && !df_tag.equals("Duplicate Files") && df.currentUsbFile == null && !df_tag.equals(DetailFragment.SEARCH_RESULT)
                        && !Global.WHETHER_FILE_OBJECT_TYPE_NETWORK_OR_CLOUD_TYPE_AND_CONTAINED_IN_STORAGE_DIR(df.fileObjectType)) {
                    fm.popBackStack();
                    ++frag;
                    if (frag > entry_count) {
                        break;
                    }
                    df = (DetailFragment) fm.findFragmentByTag(fm.getBackStackEntryAt(entry_count - frag).getName());
                    df_tag = df.getTag();
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
                    Global.print(context, getString(R.string.click_exit_button_to_exit));
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        imm.hideSoftInputFromWindow(search_view.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        workingDirRecyclerAdapter.setOnItemClickListenerForWorkingDirAdapter(null);
        mediaMountReceiver.removeMediaMountListener(this);
        localBroadcastManager.unregisterReceiver(usbReceiver);
        localBroadcastManager.unregisterReceiver(localBroadcastReceiver);
        unregisterReceiver(networkStateReceiver);
        context.unregisterReceiver(mediaMountReceiver);
        listPopWindow.dismiss(); // to avoid memory leak on orientation change
        h.removeCallbacksAndMessages(null);
    }

    public void DeselectAllAndAdjustToolbars(DetailFragment df) {
        listPopWindow.dismiss();
        if (DetailFragment.CUT_SELECTED || DetailFragment.COPY_SELECTED) {
            paste_toolbar.setVisibility(View.VISIBLE);
            paste_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
            bottom_toolbar.setVisibility(View.GONE);
            viewModel.toolbar_shown = "paste";
            parent_dir_image_button.setEnabled(true);
            parent_dir_image_button.setAlpha(Global.ENABLE_ALFA);
        } else {
            parent_dir_image_button.setEnabled(true);
            parent_dir_image_button.setAlpha(Global.ENABLE_ALFA);
            bottom_toolbar.setVisibility(View.VISIBLE);
            bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
            viewModel.toolbar_shown = "bottom";
            paste_toolbar.setVisibility(View.GONE);
        }
        actionmode_toolbar.setVisibility(View.GONE);
        if (df != null) {
            df.clearSelectionAndNotifyDataSetChanged();
            df.is_toolbar_visible = true;
            all_select.setImageResource(R.drawable.select_icon);
            interval_select.setVisibility(View.GONE);
        }
    }

    public void action_mode_finish(DetailFragment df) {
        if (df.adapter != null) {
            df.adapter.getFilter().filter(null);
        }
        DeselectAllAndAdjustToolbars(df);
        imm.hideSoftInputFromWindow(search_view.getWindowToken(), 0);
        search_view.setText("");
        search_view.clearFocus();
        search_toolbar.setVisibility(View.GONE); //no need to call adapter.filter with null to refill filepjos as calling datasetchanged replenished df.adapter.filepojo listUri
        search_toolbar_visible = false;
    }

    public void workingDirAdd() {
        DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
        if (working_dir_arraylist.size() > 20) {
            Global.print(context, getString(R.string.more_than_20_directories_cannot_be_added));
            return;
        }

        String file_path = df.fileclickselected;
        if (df.fileObjectType == FileObjectType.FILE_TYPE || df.fileObjectType == FileObjectType.ROOT_TYPE) {
            File file = new File(file_path);
            if (file.isDirectory() && !working_dir_arraylist.contains(file_path) && !StorageUtil.STORAGE_DIR.contains(file) && !viewModel.archive_view) {
                int i = workingDirRecyclerAdapter.insert(file_path);
                workingDirListRecyclerView.scrollToPosition(i);
                tinyDB.putListString("working_dir_arraylist", working_dir_arraylist);
                setRecyclerViewHeight(workingDirListRecyclerView);
            }
        }
    }

    public void workingDirRemove() {
        if (working_dir_arraylist == null || working_dir_arraylist.isEmpty()) {
            return;
        }

        if (workingDirRecyclerAdapter.custom_dir_selected_array.isEmpty()) {
            Global.print(context, getString(R.string.select_directories_by_long_pressing));
        } else {
            workingDirRecyclerAdapter.remove(workingDirRecyclerAdapter.custom_dir_selected_array);
            tinyDB.putListString("working_dir_arraylist", working_dir_arraylist);
            setRecyclerViewHeight(workingDirListRecyclerView);
        }
    }

    private void setRecyclerViewHeight(RecyclerView v) {
        int number_items = Math.min(5, v.getAdapter().getItemCount());
        v.getLayoutParams().height = number_items * Global.FOUR_DP * 14;
    }

    public ArrayList<File> iterate_to_attach_file(IndexedLinkedHashMap<Integer, String> file_list) {
        ArrayList<File> file_list_excluding_dir = new ArrayList<>();
        int size = file_list.size();
        for (int i = 0; i < size; ++i) {
            File f = new File(file_list.getValueAtIndex(i));
            if (!f.isDirectory()) {
                file_list_excluding_dir.add(f);
            }
        }
        return file_list_excluding_dir;
    }

    public ArrayList<Uri> iterate_to_attach_usb_file(IndexedLinkedHashMap<Integer, String> file_list, DetailFragment df) {
        ArrayList<Uri> uri_list_excluding_dir = new ArrayList<>();
        int size = file_list.size();
        for (int i = 0; i < size; ++i) {
            String file_path = file_list.getValueAtIndex(i);
            try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
                UsbFile f = FileUtil.getUsbFile(access.getUsbFile(), file_path);
                if (f != null && !f.isDirectory()) {
                    uri_list_excluding_dir.add(FileUtil.getDocumentUri(file_path, df.tree_uri, df.tree_uri_path));
                    break;
                }
            }
        }
        return uri_list_excluding_dir;
    }

    @Override
    public void deleteDialogOKButtonClick() {
        final DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
        action_mode_finish(df);
    }

    @Override
    public void onScrollRecyclerView(boolean showToolBar) {
        if (showToolBar) {
            switch (viewModel.toolbar_shown) {
                case "bottom":
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    break;
                case "action_mode":
                    actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    break;
                case "paste":
                    paste_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    break;
            }
        } else {
            switch (viewModel.toolbar_shown) {
                case "bottom":
                    bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    break;
                case "action_mode":
                    actionmode_toolbar.animate().translationY(actionmode_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    break;
                case "paste":
                    paste_toolbar.animate().translationY(paste_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    break;
            }
        }
    }

    @Override
    public void actionModeFinish(Fragment fragment, String fileclickeselected) {
        if (fragment instanceof DetailFragment) {
            action_mode_finish((DetailFragment) fragment);
        }
    }

    @Override
    public void onLongClickItem(int size) {
        if (!viewModel.toolbar_shown.equals("paste")) {
            actionmode_toolbar.setVisibility(View.VISIBLE);
            paste_toolbar.setVisibility(View.GONE);
            bottom_toolbar.setVisibility(View.GONE);
            viewModel.toolbar_shown = "action_mode";
            actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
        } else {
            paste_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
        }

        final DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
        if (size == 1) {
            rename.setEnabled(true);
            rename.setAlpha(Global.ENABLE_ALFA);
            interval_select.setAlpha(Global.DISABLE_ALFA);
            interval_select.setEnabled(false);
        } else if (size > 1) {
            rename.setEnabled(false);
            rename.setAlpha(Global.DISABLE_ALFA);
            interval_select.setVisibility(View.VISIBLE);
            int last_key = df.viewModel.mselecteditems.getKeyAtIndex(size - 1);
            int previous_to_last_key = df.viewModel.mselecteditems.getKeyAtIndex(size - 2);
            if (last_key - previous_to_last_key < -1 || last_key - previous_to_last_key > 1) {
                interval_select.setAlpha(Global.ENABLE_ALFA);
                interval_select.setEnabled(true);
            } else {
                interval_select.setAlpha(Global.DISABLE_ALFA);
                interval_select.setEnabled(false);
            }
        }

        if (size == df.file_list_size) {
            all_select.setImageResource(R.drawable.deselect_icon);
            interval_select.setAlpha(Global.DISABLE_ALFA);
            interval_select.setEnabled(false);
        } else {
            all_select.setImageResource(R.drawable.select_icon);
        }
        if (size == 0) {
            DeselectAllAndAdjustToolbars(df);
        }
    }

    @Override
    public void setFileNumberView(String file_number_string) {
        file_number_view.setText(file_number_string);
    }

    private void MoveToCopyToProcedure(DetailFragment df, boolean cut) {
        clear_cache = false;
        Bundle bundle = new Bundle();
        ArrayList<String> files_selected_array = new ArrayList<>();
        int size = df.viewModel.mselecteditems.size();
        for (int i = 0; i < size; ++i) {
            files_selected_array.add(df.viewModel.mselecteditems.getValueAtIndex(i));
        }
        bundle.putString("source_folder", df.fileclickselected);
        bundle.putStringArrayList("files_selected_array", files_selected_array);
        bundle.putSerializable("sourceFileObjectType", df.fileObjectType);
        bundle.putBoolean("cut", cut);

        Intent intent = new Intent(context, FileSelectorActivity.class);
        intent.putExtra("bundle", bundle);
        intent.putExtra(FileSelectorActivity.ACTION_SOUGHT, FileSelectorActivity.MOVE_COPY_REQUEST_CODE);
        activityResultLauncher_file_select.launch(intent);
    }

    private void paste_pastecancel_view_procedure(DetailFragment df) {
        DetailFragment.FILE_SELECTED_FOR_CUT_COPY = new ArrayList<>();
        DetailFragment.CUT_SELECTED = false;
        DetailFragment.COPY_SELECTED = false;
        bottom_toolbar.setVisibility(View.VISIBLE);
        bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
        paste_toolbar.setVisibility(View.GONE);
        actionmode_toolbar.setVisibility(View.GONE);
        viewModel.toolbar_shown = "bottom";
        df.is_toolbar_visible = true;
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher_all_file_access_permission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
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

    @Override
    public void onMediaMount(String action) {
        switch (action) {
            case "android.intent.action.MEDIA_MOUNTED":
                repositoryClass.storage_dir.clear();
                repositoryClass.storage_dir.addAll(new ArrayList<>(StorageUtil.getSdCardPaths(context, true)));
                if (USB_ATTACHED && usbCurrentFs != null) {
                    try {
                        UsbFile root = usbCurrentFs.getRootDirectory();
                        if (root != null) {
                            Global.USB_STORAGE_PATH = root.getAbsolutePath();
                            repositoryClass.storage_dir.add(MakeFilePOJOUtil.MAKE_FilePOJO(root, false));
                        }
                    } catch (Exception ignored) {
                    }
                }
                Global.WORKOUT_AVAILABLE_SPACE();
                storageRecyclerAdapter.notifyDataSetChanged();
                if (recentDialogListener != null) {
                    recentDialogListener.onMediaAttachedAndRemoved();
                }
                break;

            case "android.intent.action.MEDIA_EJECT":
            case "android.intent.action.MEDIA_REMOVED":
            case "android.intent.action.MEDIA_BAD_REMOVAL":
                repositoryClass.storage_dir.clear();
                repositoryClass.storage_dir.addAll(new ArrayList<>(StorageUtil.getSdCardPaths(context, true)));
                if (USB_ATTACHED && usbCurrentFs != null) {
                    try {
                        UsbFile root = usbCurrentFs.getRootDirectory();
                        if (root != null) {
                            Global.USB_STORAGE_PATH = root.getAbsolutePath();
                            repositoryClass.storage_dir.add(MakeFilePOJOUtil.MAKE_FilePOJO(root, false));
                        }
                    } catch (Exception ignored) {
                    }
                }
                Global.WORKOUT_AVAILABLE_SPACE();
                storageRecyclerAdapter.notifyDataSetChanged();
                FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(repositoryClass.external_storage_path_list, FileObjectType.FILE_TYPE);
                DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                if (df != null) {
                    df.clearSelectionAndNotifyDataSetChanged();
                }
                if (recentDialogListener != null) {
                    recentDialogListener.onMediaAttachedAndRemoved();
                }
                break;
        }
    }

    private void setupUsbDevice() {
        if (UsbDocumentProvider.USB_MASS_STORAGE_DEVICES.isEmpty()) {
            return;
        }

        UsbMassStorageDevice device = UsbDocumentProvider.USB_MASS_STORAGE_DEVICES.get(0);
        try {
            if (device.getPartitions().isEmpty()) {
                Global.print(this, getString(R.string.error_setting_up_device));
                usb_eject_layout_group.setVisibility(View.GONE);
                return;
            }
            // Save the file system
            usbCurrentFs = device.getPartitions().get(0).getFileSystem();

            // Acquire the write lock by calling setUsbFileRoot (which uses writeLock internally)
            UsbFile usbFileRoot = usbCurrentFs.getRootDirectory();
            UsbFileRootSingleton.getInstance().setUsbFileRoot(usbFileRoot);

            // Set chunk size, etc.
            int chunk = usbCurrentFs.getChunkSize();
            FileUtil.USB_CHUNK_SIZE = (chunk > 0) ? chunk : FileUtil.BUFFER_SIZE;
            usb_eject_layout_group.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            // Handle exception
        }


        boolean usb_path_added = false;
        for (FilePOJO filePOJO : repositoryClass.storage_dir) {
            if (filePOJO.getFileObjectType() == FileObjectType.USB_TYPE
                    && filePOJO.getPath().equals(Global.USB_STORAGE_PATH)) {
                usb_path_added = true;
                break;
            }
        }

        if (!usb_path_added) {
            Global.USB_STORAGE_PATH = usbCurrentFs.getRootDirectory().getAbsolutePath();
            repositoryClass.storage_dir.clear();
            repositoryClass.storage_dir.addAll(new ArrayList<>(StorageUtil.getSdCardPaths(context, true)));
            repositoryClass.storage_dir.add(
                    MakeFilePOJOUtil.MAKE_FilePOJO(usbCurrentFs.getRootDirectory(), false)
            );
            Global.WORKOUT_AVAILABLE_SPACE();
            storageRecyclerAdapter.notifyDataSetChanged();
        }
    }

    public interface RecentDialogListener {
        void onMediaAttachedAndRemoved();
    }

    static class SearchParameters {
        final String search_file_name;
        final Set<FilePOJO> search_in_dir;
        final String search_file_type;
        final boolean search_whole_word;
        final boolean search_case_sensitive;
        final boolean search_regex;
        final long search_lower_limit_size;
        final long search_upper_limit_size;

        SearchParameters(String search_file_name, Set<FilePOJO> search_in_dir, String search_file_type, boolean search_whole_word,
                         boolean search_case_sensitive, boolean search_regex, long search_lower_limit_size, long search_upper_limit_size) {
            this.search_file_name = search_file_name;
            this.search_in_dir = search_in_dir;
            this.search_file_type = search_file_type;
            this.search_whole_word = search_whole_word;
            this.search_case_sensitive = search_case_sensitive;
            this.search_regex = search_regex;
            this.search_lower_limit_size = search_lower_limit_size;
            this.search_upper_limit_size = search_upper_limit_size;
        }
    }

    private class TopToolbarClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
            int id = v.getId();
            if (id == R.id.top_toolbar_home_button) {
                if (drawerLayout.isDrawerOpen(drawer)) {
                    drawerLayout.closeDrawer(drawer);
                } else {
                    drawerLayout.openDrawer(drawer);
                }
            } else if (id == R.id.top_toolbar_parent_dir_image_button) {
                String parent_file_path = Global.getParentPath(df.fileclickselected);
                if (df.fileObjectType == FileObjectType.FILE_TYPE) {
                    File parent_file = new File(parent_file_path);
                    if (parent_file.canRead()) {
                        createFragmentTransaction(parent_file.getAbsolutePath(), FileObjectType.FILE_TYPE);
                    }
                } else if (df.fileObjectType == FileObjectType.USB_TYPE) {
                    try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
                        UsbFile usbFileRoot = access.getUsbFile();
                        if (usbFileRoot == null) {
                            return;
                        }
                        try {
                            UsbFile usbFile = usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(parent_file_path));
                            createFragmentTransaction(usbFile.getAbsolutePath(), FileObjectType.USB_TYPE);
                        } catch (IOException ignored) {

                        }
                    }
                } else {
                    createFragmentTransaction(parent_file_path, df.fileObjectType);
                }
            } else if (id == R.id.top_toolbar_current_dir_label) {
                RecentDialog recentDialogFragment = new RecentDialog();
                recentDialogFragment.show(fm, "recent_file_dialog");
            } else if (id == R.id.detail_fragment_all_select) {
                if (df.adapter == null || df.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }

                if (df.viewModel.mselecteditems.size() < df.filePOJO_list.size()) {
                    all_select.setImageResource(R.drawable.deselect_icon);
                    df.adapter.selectAll();
                } else {
                    all_select.setImageResource(R.drawable.select_icon);
                    df.adapter.deselectAll();
                }
            } else if (id == R.id.detail_fragment_interval_select) {
                if (df.adapter == null || df.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }
                interval_select.setEnabled(false);
                interval_select.setAlpha(Global.DISABLE_ALFA);
                df.adapter.selectInterval();
            }
        }
    }

    private class BottomToolbarClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
            int id = v.getId();
            if (id == R.id.toolbar_btn_1) {
                if (!search_toolbar_visible) {
                    setSearchBarVisibility(true);
                } else {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }

            } else if (id == R.id.toolbar_btn_2) {
                if (search_toolbar_visible) {
                    setSearchBarVisibility(false);
                }
                if (df.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }
                if (df.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                    Global.print(context, getString(R.string.file_can_not_be_created_here));
                    return;
                }
                CreateFileAlertDialog dialog = CreateFileAlertDialog.getInstance(df.fileclickselected, df.fileObjectType);
                dialog.show(fm, "create_file_dialog");
            } else if (id == R.id.toolbar_btn_3) {
                if (df.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }

                if (df.fileObjectType != FileObjectType.SEARCH_LIBRARY_TYPE) //refresh/remove only file object type is not search/library type
                {
                    repositoryClass.hashmap_file_pojo.remove(df.fileObjectType + df.fileclickselected);
                    repositoryClass.hashmap_file_pojo_filtered.remove(df.fileObjectType + df.fileclickselected);
                    df.viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }

                fm.beginTransaction().detach(df).commit();
                fm.beginTransaction().attach(df).commit();
                Global.WORKOUT_AVAILABLE_SPACE();
            } else if (id == R.id.toolbar_btn_4) {
                if (search_toolbar_visible) {
                    setSearchBarVisibility(false);
                }
                ViewDialog viewDialog = new ViewDialog();
                viewDialog.show(fm, "view_dialog");
            } else if (id == R.id.toolbar_btn_5) {
                if (search_toolbar_visible) {
                    setSearchBarVisibility(false);
                }
                finish();
            }
        }
    }

    private class ActionModeListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            imm.hideSoftInputFromWindow(search_view.getWindowToken(), 0);
            final DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
            if (df.viewModel.mselecteditems.isEmpty()) {
                Global.print(context, getString(R.string.could_not_perform_action));
                action_mode_finish(df);
                return;
            }
            final ArrayList<String> files_selected_array = new ArrayList<>();
            int size;
            int id = v.getId();
            if (id == R.id.toolbar_btn_1) {
                DetailFragment.CUT_SELECTED = true;
                DetailFragment.COPY_SELECTED = false;
                DetailFragment.FILE_SELECTED_FOR_CUT_COPY = new ArrayList<>();
                size = df.viewModel.mselecteditems.size();
                for (int i = 0; i < size; ++i) {
                    DetailFragment.FILE_SELECTED_FOR_CUT_COPY.add(df.viewModel.mselecteditems.getValueAtIndex(i));
                }
                DetailFragment.CUT_COPY_FILE_OBJECT_TYPE = df.fileObjectType;
                DetailFragment.CUT_COPY_FILE_CLICK_SELECTED = df.fileclickselected;
                action_mode_finish(df);
            } else if (id == R.id.toolbar_btn_2) {
                DetailFragment.COPY_SELECTED = true;
                DetailFragment.CUT_SELECTED = false;
                DetailFragment.FILE_SELECTED_FOR_CUT_COPY = new ArrayList<>();
                size = df.viewModel.mselecteditems.size();
                for (int i = 0; i < size; ++i) {
                    DetailFragment.FILE_SELECTED_FOR_CUT_COPY.add(df.viewModel.mselecteditems.getValueAtIndex(i));
                }
                DetailFragment.CUT_COPY_FILE_OBJECT_TYPE = df.fileObjectType;
                DetailFragment.CUT_COPY_FILE_CLICK_SELECTED = df.fileclickselected;
                action_mode_finish(df);
            } else if (id == R.id.toolbar_btn_3) {
                FilePOJO filePOJO = df.filePOJO_list.get(df.viewModel.mselecteditems.getKeyAtIndex(0)); //take file pojo from df.adapter.filepojolist, not from df.filepojolist
                String parent_file_path = new File(filePOJO.getPath()).getParent();
                String existing_name = filePOJO.getName();
                String ext = filePOJO.getExt();
                boolean isDirectory = filePOJO.getIsDirectory();
                RenameFileDialog renameFileAlertDialog = RenameFileDialog.getInstance(parent_file_path, existing_name, ext, isDirectory, filePOJO.getFileObjectType(), df.fileclickselected);
                renameFileAlertDialog.show(fm, "rename_dialog");
                action_mode_finish(df);
            } else if (id == R.id.toolbar_btn_4) {
                size = df.viewModel.mselecteditems.size();
                for (int i = 0; i < size; ++i) {
                    files_selected_array.add(df.viewModel.mselecteditems.getValueAtIndex(i));
                }

                DeleteFileAlertDialog deleteFileAlertDialog = DeleteFileAlertDialog.getInstance(files_selected_array, df.fileObjectType, df.fileclickselected, false);
                deleteFileAlertDialog.show(fm, "delete_dialog");
                action_mode_finish(df);
            } else if (id == R.id.toolbar_btn_5) {
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
                        int size;
                        Object idd = listView.getItemAtPosition(p3);
                        int item_id = ((ListPopupWindowPOJO) idd).id;

                        switch (item_id) {
                            case 1:
                                if ((df.fileObjectType == FileObjectType.FILE_TYPE) || (df.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE)) {
                                    ArrayList<File> file_list_excluding_dir;
                                    file_list_excluding_dir = iterate_to_attach_file(df.viewModel.mselecteditems);
                                    if (file_list_excluding_dir.isEmpty()) {
                                        Global.print(context, getString(R.string.directories_can_not_be_sent_select_one_file));
                                        break;
                                    }
                                    FileIntentDispatch.sendFile(MainActivity.this, file_list_excluding_dir);
                                } else {
                                    Global.print(context, getString(R.string.not_supported));
                                }
                                break;
                            case 2:
                                size = df.viewModel.mselecteditems.size();
                                for (int i = 0; i < size; ++i) {
                                    files_selected_array.add(df.viewModel.mselecteditems.getValueAtIndex(i));
                                }

                                PropertiesDialog propertiesDialog = PropertiesDialog.getInstance(files_selected_array, df.fileObjectType);
                                propertiesDialog.show(fm, "properties_dialog");
                                break;

                            case 3:
                                MoveToCopyToProcedure(df, true);
                                break;
                            case 4:
                                MoveToCopyToProcedure(df, false);
                                break;
                            case 5:
                                size = df.viewModel.mselecteditems.size();
                                for (int i = 0; i < size; ++i) {
                                    files_selected_array.add(df.viewModel.mselecteditems.getValueAtIndex(i));
                                }
                                ArchiveSetUpDialog archiveSetUpDialog = ArchiveSetUpDialog.getInstance(files_selected_array, null, "", df.fileObjectType, ArchiveSetUpDialog.ARCHIVE_ACTION_ZIP);
                                archiveSetUpDialog.show(fm, "zip_dialog");
                                break;
                            case 6:
                                if (df.viewModel.mselecteditems.size() != 1) {
                                    Global.print(context, getString(R.string.select_only_a_zip_file));
                                    break;
                                }
                                String path = df.viewModel.mselecteditems.getValueAtIndex(0);
                                String file_name = new File(path).getName();
                                String file_ext = "";
                                int idx = file_name.lastIndexOf(".");
                                if (idx > 0) {
                                    file_ext = file_name.substring(idx + 1);
                                }
                                if (file_ext.matches(("(?i)zip"))) {
                                    size = df.viewModel.mselecteditems.size();
                                    for (int i = 0; i < size; ++i) {
                                        files_selected_array.add(df.viewModel.mselecteditems.getValueAtIndex(i));
                                    }
                                    ArchiveSetUpDialog unarchiveSetUpDialog = ArchiveSetUpDialog.getInstance(files_selected_array, null, "", df.fileObjectType, ArchiveSetUpDialog.ARCHIVE_ACTION_UNZIP);
                                    unarchiveSetUpDialog.show(fm, "zip_dialog");
                                } else {
                                    Global.print(context, getString(R.string.select_only_a_zip_file));
                                }
                                break;
                            case 7:
                                if (df.viewModel.mselecteditems.size() != 1) {
                                    Global.print(context, getString(R.string.select_only_a_file));
                                    break;
                                }
                                FilePOJO filePOJO = df.filePOJO_list.get(df.viewModel.mselecteditems.getKeyAtIndex(0));
                                if (filePOJO.getIsDirectory()) {
                                    Global.print(context, getString(R.string.select_only_a_file));
                                    break;
                                }
                                df.file_open_intent_dispatch(filePOJO.getPath(), filePOJO.getFileObjectType(), filePOJO.getName(), true, filePOJO.getSizeLong());
                                break;
                            default:
                                break;

                        }
                        if (item_id != 2) {
                            action_mode_finish(df);
                        }
                        listPopWindow.dismiss();
                    }
                });

                if (!list_popupwindowpojos.contains(extract_listPopupWindowPOJO)) {
                    list_popupwindowpojos.add(extract_listPopupWindowPOJO);
                }
                if (!list_popupwindowpojos.contains(open_listPopupWindowPOJO)) {
                    list_popupwindowpojos.add(open_listPopupWindowPOJO);
                }

                if (df.viewModel.mselecteditems.size() != 1) {
                    list_popupwindowpojos.remove(extract_listPopupWindowPOJO);
                    list_popupwindowpojos.remove(open_listPopupWindowPOJO);
                } else if (df.viewModel.mselecteditems.size() == 1) {
                    FilePOJO filePOJO = df.filePOJO_list.get(df.viewModel.mselecteditems.getKeyAtIndex(0));
                    if (filePOJO.getIsDirectory()) {
                        list_popupwindowpojos.remove(extract_listPopupWindowPOJO);
                        list_popupwindowpojos.remove(open_listPopupWindowPOJO);
                    } else {
                        String file_ext = filePOJO.getExt();

                        if (file_ext != null && !file_ext.matches(("(?i)zip"))) {
                            list_popupwindowpojos.remove(extract_listPopupWindowPOJO);
                        }
                    }
                }
                popupWindowAdapter.notifyDataSetChanged();
                Global.SHOW_LIST_POPUP_WINDOW_BOTTOM(actionmode_toolbar, listPopWindow, Global.FOUR_DP);
            }
        }
    }

    private class PasteToolbarClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
            ArrayList<String> files_selected_array = new ArrayList<>();

            int size;
            int id = v.getId();
            if (id == R.id.toolbar_btn_1) {
                if (df.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }
                if (df.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                    Global.print(context, getString(R.string.file_can_not_be_created_here));
                    return;
                }
                CreateFileAlertDialog dialog = CreateFileAlertDialog.getInstance(df.fileclickselected, df.fileObjectType);
                dialog.show(fm, "create_file_dialog");
            } else if (id == R.id.toolbar_btn_2) {
                if (df.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }
                if (df.fileObjectType != FileObjectType.SEARCH_LIBRARY_TYPE) //refresh/remove only file object type is not search/library type
                {
                    repositoryClass.hashmap_file_pojo.remove(df.fileObjectType + df.fileclickselected);
                    repositoryClass.hashmap_file_pojo_filtered.remove(df.fileObjectType + df.fileclickselected);
                    df.viewModel.asyncTaskStatus.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }

                fm.beginTransaction().detach(df).commit();
                fm.beginTransaction().attach(df).commit();

            } else if (id == R.id.toolbar_btn_3) {
                if (df.progress_bar.getVisibility() == View.VISIBLE) {
                    Global.print(context, getString(R.string.please_wait));
                    return;
                }
                action_mode_finish(df);
                if (df.fileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE) {
                    Global.print(context, getString(R.string.files_can_not_be_pasted_here));
                    return;
                }

                //if intent is originated from outside
                if (viewModel.send_intent != null) {
                    clear_cache = false;
                    viewModel.send_intent.putExtra("dest_folder", df.fileclickselected);
                    viewModel.send_intent.putExtra("destFileObjectType", df.fileObjectType);
                    viewModel.send_intent.setClass(context, CopyToActivity.class);
                    startActivity(viewModel.send_intent);
                    viewModel.send_intent = null;
                } else {
                    FileObjectType sourceFileObjectType = DetailFragment.CUT_COPY_FILE_OBJECT_TYPE;
                    String source_folder = DetailFragment.CUT_COPY_FILE_CLICK_SELECTED;
                    if (sourceFileObjectType == null) {
                        Global.print(context, getString(R.string.could_not_perform_action));
                    } else {
                        files_selected_array = new ArrayList<>(DetailFragment.FILE_SELECTED_FOR_CUT_COPY);
                        df.progress_bar.setVisibility(View.VISIBLE);
                        fileDuplicationViewModel.checkForExistingFileWithSameName(source_folder, sourceFileObjectType, df.fileclickselected, df.fileObjectType, files_selected_array, DetailFragment.CUT_SELECTED, false);
                    }
                }

                DetailFragment.CUT_COPY_FILE_OBJECT_TYPE = null;
                DetailFragment.CUT_COPY_FILE_CLICK_SELECTED = "";
                paste_pastecancel_view_procedure(df);
            } else if (id == R.id.toolbar_btn_4) {
                if (!df.viewModel.mselecteditems.isEmpty()) {
                    size = df.viewModel.mselecteditems.size();
                    for (int i = 0; i < size; ++i) {
                        files_selected_array.add(df.viewModel.mselecteditems.getValueAtIndex(i));
                    }
                    DeleteFileAlertDialog deleteFileAlertDialog = DeleteFileAlertDialog.getInstance(files_selected_array, df.fileObjectType, df.fileclickselected, false);
                    deleteFileAlertDialog.show(fm, "delete_dialog");
                } else {
                    Global.print(context, getString(R.string.select_files_to_delete));
                }
            } else if (id == R.id.toolbar_btn_5) {
                viewModel.send_intent = null;
                paste_pastecancel_view_procedure(df);
            }
            df.clearSelectionAndNotifyDataSetChanged();
        }
    }

    private class DrawerEndButtButtonsClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.toolbar_btn_1) {
                finish();
            } else if (id == R.id.toolbar_btn_2) {
                final BlankFragment pbf = BlankFragment.newInstance();
                pbf.show(fm, "");
                drawerLayout.closeDrawer(drawer);
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PreferencesDialog preferencesDialog = new PreferencesDialog();
                        preferencesDialog.show(fm, "preferences_dialog");
                        pbf.dismissAllowingStateLoss();
                    }
                }, DRAWER_CLOSE_DELAY);
            }
        }
    }

    public class StorageRecyclerAdapter extends RecyclerView.Adapter<StorageRecyclerAdapter.ViewHolder> {
        final List<FilePOJO> storage_dir_arraylist;

        StorageRecyclerAdapter(List<FilePOJO> storage_dir_arraylist) {
            this.storage_dir_arraylist = storage_dir_arraylist;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            View v = LayoutInflater.from(context).inflate(R.layout.storage_dir_recyclerview_layout, p1, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder p1, int p2) {
            FilePOJO filePOJO = storage_dir_arraylist.get(p2);
            if (filePOJO == null) {
                return;
            }
            String file_path = filePOJO.getPath();
            FileObjectType fileObjectType = filePOJO.getFileObjectType();

            if (fileObjectType == FileObjectType.FILE_TYPE) {
                if (Global.GET_INTERNAL_STORAGE_FILE_POJO_STORAGE_DIR().getPath().equals(file_path)) {
                    p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.device_icon));
                } else {
                    p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.sdcard_icon));
                }
                p1.textView_storage_dir.setText(filePOJO.getName());
            } else if (fileObjectType == FileObjectType.USB_TYPE) {
                p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.usb_icon));
                p1.textView_storage_dir.setText(DetailFragment.USB_FILE_PREFIX + filePOJO.getName());
            } else if (fileObjectType == FileObjectType.FTP_TYPE) {
                p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.network_icon));
                p1.textView_storage_dir.setText(DetailFragment.FTP_FILE_PREFIX + filePOJO.getName());
            } else if (fileObjectType == FileObjectType.SFTP_TYPE) {
                p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.network_icon));
                p1.textView_storage_dir.setText(DetailFragment.SFTP_FILE_PREFIX + filePOJO.getName());
            } else if (fileObjectType == FileObjectType.WEBDAV_TYPE) {
                p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.network_icon));
                p1.textView_storage_dir.setText(DetailFragment.WEBDAV_FILE_PREFIX + filePOJO.getName());
            } else if (fileObjectType == FileObjectType.SMB_TYPE) {
                p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.network_icon));
                p1.textView_storage_dir.setText(DetailFragment.SMB_FILE_PREFIX + filePOJO.getName());
            } else if (fileObjectType == FileObjectType.ROOT_TYPE) {
                p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.device_icon));
                p1.textView_storage_dir.setText(R.string.root_directory);
            } else if (fileObjectType == FileObjectType.GOOGLE_DRIVE_TYPE) {
                p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.cloud_icon));
                p1.textView_storage_dir.setText(DetailFragment.GOOGLE_DRIVE_FILE_PREFIX + filePOJO.getName());
            } else if (fileObjectType == FileObjectType.ONE_DRIVE_TYPE) {
                p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.cloud_icon));
                p1.textView_storage_dir.setText(DetailFragment.ONE_DRIVE_FILE_PREFIX + filePOJO.getName());
            } else if (fileObjectType == FileObjectType.DROP_BOX_TYPE) {
                p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.cloud_icon));
                p1.textView_storage_dir.setText(DetailFragment.DROP_BOX_FILE_PREFIX + filePOJO.getName());
            } else if (fileObjectType == FileObjectType.YANDEX_TYPE) {
                p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.cloud_icon));
                p1.textView_storage_dir.setText(DetailFragment.YANDEX_FILE_PREFIX + filePOJO.getName());
            }
        }

        @Override
        public int getItemCount() {
            return storage_dir_arraylist.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View v;
            final ImageView imageview;
            final TextView textView_storage_dir;

            ViewHolder(View v) {
                super(v);
                this.v = v;
                imageview = v.findViewById(R.id.image_storage_dir);
                textView_storage_dir = v.findViewById(R.id.text_storage_dir_name);

                v.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View p) {
                        drawer_storage_file_pojo_selected = storage_dir_arraylist.get(getBindingAdapterPosition());
                        drawerLayout.closeDrawer(drawer);
                    }
                });
            }
        }
    }

    private class LibraryRecyclerAdapter extends RecyclerView.Adapter<LibraryRecyclerAdapter.ViewHolder> {
        final List<String> library_arraylist;
        final int[] icon_image_list;

        LibraryRecyclerAdapter(List<String> storage_dir_arraylist, int[] icon_image_list) {
            this.library_arraylist = storage_dir_arraylist;
            this.icon_image_list = icon_image_list;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            View v = LayoutInflater.from(context).inflate(R.layout.storage_dir_recyclerview_layout, p1, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder p1, int p2) {
            p1.textView_library.setText(library_arraylist.get(p2));
            p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, icon_image_list[p2]));
        }

        @Override
        public int getItemCount() {
            return library_arraylist.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View v;
            final ImageView imageview;
            final ImageView play_overlay_imageview, pdf_overlay_imageview;
            final TextView textView_library;

            ViewHolder(View v) {
                super(v);
                this.v = v;
                imageview = v.findViewById(R.id.image_storage_dir);
                play_overlay_imageview = v.findViewById(R.id.play_overlay_image_storage_dir);
                pdf_overlay_imageview = v.findViewById(R.id.pdf_overlay_image_storage_dir);
                textView_library = v.findViewById(R.id.text_storage_dir_name);
                play_overlay_imageview.setVisibility(View.GONE);
                final int[] position = new int[1];
                v.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View p) {
                        position[0] = getBindingAdapterPosition();
                        String name = "Download";
                        switch (position[0]) {
                            case 0:
                                name = "Download";
                                break;
                            case 1:
                                name = "Document";
                                break;
                            case 2:
                                name = "Image";
                                break;
                            case 3:
                                name = "Audio";
                                break;
                            case 4:
                                name = "Video";
                                break;
                            case 5:
                                name = "Archive";
                                break;
                            case 6:
                                name = "APK";
                                break;
                        }
                        drawer_storage_file_pojo_selected = new FilePOJO(FileObjectType.SEARCH_LIBRARY_TYPE, name, null, name, false, 0L, null, 0L, null, R.drawable.folder_icon, null, 0, 0, 0);
                        drawerLayout.closeDrawer(drawer);
                    }
                });
            }
        }
    }

    private class CleanStorageRecyclerAdapter extends RecyclerView.Adapter<CleanStorageRecyclerAdapter.ViewHolder> {
        final List<String> clean_storage_arraylist;
        final int[] icon_image_list;

        CleanStorageRecyclerAdapter(List<String> storage_dir_arraylist, int[] icon_image_list) {
            this.clean_storage_arraylist = storage_dir_arraylist;
            this.icon_image_list = icon_image_list;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            View v = LayoutInflater.from(context).inflate(R.layout.clean_storage_recycler_layout, p1, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder p1, int p2) {
            p1.textView_library.setText(clean_storage_arraylist.get(p2));
            p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, icon_image_list[p2]));
        }

        @Override
        public int getItemCount() {
            return clean_storage_arraylist.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View v;
            final ImageView imageview;
            final TextView textView_library;

            ViewHolder(View v) {
                super(v);
                this.v = v;
                imageview = v.findViewById(R.id.clean_storage_label_image);
                textView_library = v.findViewById(R.id.clean_storage_text_label);

                final int[] position = new int[1];
                textView_library.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View p) {
                        position[0] = getBindingAdapterPosition();
                        String name = "Large Files";
                        switch (position[0]) {
                            case 0:
                                name = "Large Files";
                                break;
                            case 1:
                                name = "Duplicate Files";
                                break;
                        }
                        drawer_storage_file_pojo_selected = new FilePOJO(FileObjectType.SEARCH_LIBRARY_TYPE, name, null, name, false, 0L, null, 0L, null, R.drawable.folder_icon, null, 0, 0, 0);
                        drawerLayout.closeDrawer(drawer);
                    }
                });

                imageview.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View p) {
                        position[0] = getBindingAdapterPosition();
                        //final ProgressBarFragment pbf = ProgressBarFragment.newInstance();
                        final BlankFragment pbf = BlankFragment.newInstance();
                        pbf.show(fm, "");
                        drawerLayout.closeDrawer(drawer);

                        Handler h = new Handler();
                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                switch (position[0]) {
                                    case 0:
                                        rescanLargeDuplicateFilesLibrary("large");
                                        break;
                                    case 1:
                                        rescanLargeDuplicateFilesLibrary("duplicate");
                                        break;
                                }
                                pbf.dismissAllowingStateLoss();
                            }
                        }, DRAWER_CLOSE_DELAY);
                    }
                });
            }
        }
    }

    private class NetworkRecyclerAdapter extends RecyclerView.Adapter<NetworkRecyclerAdapter.ViewHolder> {
        final List<String> network_arraylist;
        final int[] icon_image_list;

        NetworkRecyclerAdapter(List<String> network_arraylist, int[] icon_image_list) {
            this.network_arraylist = network_arraylist;
            this.icon_image_list = icon_image_list;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            View v = LayoutInflater.from(context).inflate(R.layout.storage_dir_recyclerview_layout, p1, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder p1, int p2) {
            p1.textView_network.setText(network_arraylist.get(p2));
            p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, icon_image_list[p2]));
        }

        @Override
        public int getItemCount() {
            return network_arraylist.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View v;
            final ImageView imageview;
            final ImageView play_overlay_imageview, pdf_overlay_imageview;
            final TextView textView_network;

            ViewHolder(View v) {
                super(v);
                this.v = v;
                imageview = v.findViewById(R.id.image_storage_dir);
                play_overlay_imageview = v.findViewById(R.id.play_overlay_image_storage_dir);
                pdf_overlay_imageview = v.findViewById(R.id.pdf_overlay_image_storage_dir);
                textView_network = v.findViewById(R.id.text_storage_dir_name);
                play_overlay_imageview.setVisibility(View.GONE);
                final int[] position = new int[1];
                v.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View p) {
                        position[0] = getBindingAdapterPosition();
                        final BlankFragment pbf = BlankFragment.newInstance();
                        pbf.show(fm, "");
                        drawerLayout.closeDrawer(drawer);
                        Handler h = new Handler();
                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                                action_mode_finish(df);
                                if (position[0] == 0) {
                                    NetworkAccountsDetailsDialog networkAccountsDetailsDialog = NetworkAccountsDetailsDialog.getInstance(NetworkAccountsDetailsDialog.FTP);
                                    networkAccountsDetailsDialog.show(fm, "");
                                } else if (position[0] == 1) {
                                    NetworkAccountsDetailsDialog networkAccountsDetailsDialog = NetworkAccountsDetailsDialog.getInstance(NetworkAccountsDetailsDialog.SFTP);
                                    networkAccountsDetailsDialog.show(fm, "");
                                } else if (position[0] == 2) {
                                    NetworkAccountsDetailsDialog networkAccountsDetailsDialog = NetworkAccountsDetailsDialog.getInstance(NetworkAccountsDetailsDialog.WebDAV);
                                    networkAccountsDetailsDialog.show(fm, "");
                                } else if (position[0] == 3) {
                                    NetworkAccountsDetailsDialog networkAccountsDetailsDialog = NetworkAccountsDetailsDialog.getInstance(NetworkAccountsDetailsDialog.SMB);
                                    networkAccountsDetailsDialog.show(fm, "");
                                }
                                pbf.dismissAllowingStateLoss();
                            }
                        }, DRAWER_CLOSE_DELAY);
                    }
                });
            }
        }
    }

    private class CloudRecyclerAdapter extends RecyclerView.Adapter<CloudRecyclerAdapter.ViewHolder> {
        final List<String> cloud_arraylist;
        final int[] icon_image_list;

        CloudRecyclerAdapter(List<String> cloud_arraylist, int[] icon_image_list) {
            this.cloud_arraylist = cloud_arraylist;
            this.icon_image_list = icon_image_list;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2) {
            View v = LayoutInflater.from(context).inflate(R.layout.storage_dir_recyclerview_layout, p1, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder p1, int p2) {
            p1.textView_network.setText(cloud_arraylist.get(p2));
            p1.imageview.setImageDrawable(ContextCompat.getDrawable(context, icon_image_list[p2]));
        }

        @Override
        public int getItemCount() {
            return cloud_arraylist.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View v;
            final ImageView imageview;
            final ImageView play_overlay_imageview, pdf_overlay_imageview;
            final TextView textView_network;

            ViewHolder(View v) {
                super(v);
                this.v = v;
                imageview = v.findViewById(R.id.image_storage_dir);
                play_overlay_imageview = v.findViewById(R.id.play_overlay_image_storage_dir);
                pdf_overlay_imageview = v.findViewById(R.id.pdf_overlay_image_storage_dir);
                textView_network = v.findViewById(R.id.text_storage_dir_name);
                play_overlay_imageview.setVisibility(View.GONE);
                final int[] position = new int[1];
                v.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View p) {
                        position[0] = getBindingAdapterPosition();
                        final BlankFragment pbf = BlankFragment.newInstance();
                        pbf.show(fm, "");
                        drawerLayout.closeDrawer(drawer);
                        Handler h = new Handler();
                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                                action_mode_finish(df);
                                Intent intent = new Intent(context, CloudAuthActivity.class);
                                FileObjectType fileObjectType = null;
                                if (position[0] == 0) {
                                    fileObjectType = FileObjectType.GOOGLE_DRIVE_TYPE;
                                } else if (position[0] == 1) {
                                    fileObjectType = FileObjectType.DROP_BOX_TYPE;
                                } else if (position[0] == 2) {
                                    fileObjectType = FileObjectType.YANDEX_TYPE;
                                }
                                intent.putExtra("fileObjectType", fileObjectType);
                                startActivity(intent);
                                pbf.dismissAllowingStateLoss();
                            }
                        }, DRAWER_CLOSE_DELAY);
                    }
                });
            }
        }
    }

    private class USBReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context p1, Intent intent) {
            String action = intent.getAction();
            if (UsbDocumentProvider.USB_ATTACH_BROADCAST.equals(action)) {
                USB_ATTACHED = intent.getBooleanExtra(UsbDocumentProvider.USB_ATTACHED, false);
                if (USB_ATTACHED) {
                    setupUsbDevice();
                } else {
                    usbCurrentFs = null;

                    // Clear the singleton so no one can use the old reference
                    UsbFileRootSingleton.getInstance().clearUsbFileRoot();
                    usb_eject_layout_group.setVisibility(View.GONE);
                    // The rest of your existing teardown code remains:
                    Iterator<FilePOJO> iterator = repositoryClass.storage_dir.iterator();
                    while (iterator.hasNext()) {
                        if (iterator.next().getFileObjectType() == FileObjectType.USB_TYPE) {
                            iterator.remove();
                        }
                    }
                    storageRecyclerAdapter.notifyDataSetChanged();

                    FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(
                            Collections.singletonList(""),
                            FileObjectType.USB_TYPE
                    );
                    DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
                    if (df != null && df.fileObjectType == FileObjectType.USB_TYPE) {
                        df.progress_bar.setVisibility(View.VISIBLE);
                        viewModel.deleteDirectory(Global.USB_CACHE_DIR);
                        onbackpressed(false);
                    }

                    int entry_count = fm.getBackStackEntryCount();
                    for (int i = 0; i < entry_count; ++i) {
                        Fragment frag = fm.findFragmentByTag(fm.getBackStackEntryAt(i).getName());
                        if (frag instanceof DetailFragment) {
                            df = (DetailFragment) frag;
                            df.currentUsbFile = null;
                        }
                    }

                    Iterator<FilePOJO> iterator1 = MainActivity.RECENT.iterator();
                    while (iterator1.hasNext()) {
                        if (iterator1.next().getFileObjectType() == FileObjectType.USB_TYPE) {
                            iterator1.remove();
                        }
                    }
                    Global.REMOVE_USB_URI_PERMISSIONS();
                }
            }
            if (recentDialogListener != null) {
                recentDialogListener.onMediaAttachedAndRemoved();
            }
        }
    }

    private class LocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DetailFragment df = (DetailFragment) fm.findFragmentById(R.id.detail_fragment);
            Bundle bundle = intent.getExtras();
            switch (intent.getAction()) {
                case Global.LOCAL_BROADCAST_OTHER_ACTIVITY_DELETE_FILE_ACTION:
                    if (df != null) {
                        df.local_activity_delete = true;
                    }
                    break;
                case Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION:
                    if (df != null) {
                        df.modification_observed = true;
                    }
                    break;
                case Global.LOCAL_BROADCAST_REFRESH_STORAGE_DIR_ACTION:
                    storageRecyclerAdapter.notifyDataSetChanged();
                    break;
                case Global.LOCAL_BROADCAST_POP_UP_NETWORK_FILE_TYPE_FRAGMENT:
                    if (bundle != null) {
                        FileObjectType fileObjectType = (FileObjectType) bundle.getSerializable("fileObjectType");
                        if (df != null && fileObjectType != null && fileObjectType == df.fileObjectType) {
                            onbackpressed(false);
                        }
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

                        if (df != null && df.fileObjectType == sourceFileObjectType) {
                            String tag = df.getTag();
                            if (Global.IS_CHILD_FILE(tag, parent_source_folder)) {
                                df.clearSelectionAndNotifyDataSetChanged();
                            }
                        }
                    }
                    break;
                case Global.LOCAL_BROADCAST_CLEAR_CACHE_REFRESH_ACTION:
                    if (bundle != null) {
                        String file_path = bundle.getString("file_path");
                        FileObjectType sourceFileObjectType = (FileObjectType) bundle.getSerializable("sourceFileObjectType");
                        if (df != null) {
                            df.adapter.clear_cache_and_refresh(file_path, sourceFileObjectType);
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

                        if (df != null) {
                            String tag = df.getTag();
                            if (tag.equals(dest_folder) && df.fileObjectType == destFileObjectType) {
                                df.clearSelectionAndNotifyDataSetChanged();
                                int idx = df.filePOJO_list.indexOf(filePOJO);
                                if (df.llm != null) {
                                    df.llm.scrollToPositionWithOffset(idx, 0);
                                } else if (df.glm != null) {
                                    df.glm.scrollToPositionWithOffset(idx, 0);
                                }
                            } else if (Global.IS_CHILD_FILE(tag, parent_dest_folder) && df.fileObjectType == destFileObjectType) {
                                df.clearSelectionAndNotifyDataSetChanged();
                            }

                            // in case of cut, to take care of instances of dest_folder is also parent of source folder, it is put in separate if block
                            if (Global.IS_CHILD_FILE(tag, parent_source_folder) && df.fileObjectType == sourceFileObjectType) {
                                df.clearSelectionAndNotifyDataSetChanged();
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
                        if (df != null) {
                            if (Global.AFTER_ARCHIVE_GOTO_DEST_FOLDER) {
                                DetailFragment.TO_BE_MOVED_TO_FILE_POJO = filePOJO;
                                if (df.detailFragmentListener != null) {
                                    if (destFileObjectType == FileObjectType.FILE_TYPE) {
                                        df.detailFragmentListener.createFragmentTransaction(dest_folder, FileObjectType.FILE_TYPE);
                                    } else if (destFileObjectType == FileObjectType.USB_TYPE && UsbFileRootSingleton.getInstance().isUsbFileRootSet()) {
                                        df.detailFragmentListener.createFragmentTransaction(dest_folder, FileObjectType.USB_TYPE);
                                    } else {
                                        df.detailFragmentListener.createFragmentTransaction(dest_folder, destFileObjectType);
                                    }
                                }
                            } else {
                                String tag = df.getTag();
                                if (Global.IS_CHILD_FILE(tag, parent_dest_folder) && df.fileObjectType == destFileObjectType) {
                                    df.clearSelectionAndNotifyDataSetChanged();
                                    int idx = df.filePOJO_list.indexOf(filePOJO);
                                    if (df.llm != null) {
                                        df.llm.scrollToPositionWithOffset(idx, 0);
                                    } else if (df.glm != null) {
                                        df.glm.scrollToPositionWithOffset(idx, 0);
                                    }
                                }
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

                        if (df != null) {
                            String tag = df.getTag();
                            if (tag.equals(dest_folder) && df.fileObjectType == destFileObjectType) {
                                df.clearSelectionAndNotifyDataSetChanged();
                                int idx = df.filePOJO_list.indexOf(filePOJO);
                                if (df.llm != null) {
                                    df.llm.scrollToPositionWithOffset(idx, 0);
                                } else if (df.glm != null) {
                                    df.glm.scrollToPositionWithOffset(idx, 0);
                                }

                            } else if (Global.IS_CHILD_FILE(tag, parent_dest_folder) && df.fileObjectType == destFileObjectType) {
                                df.clearSelectionAndNotifyDataSetChanged();
                            }
                        }
                    }
                    break;
                case Global.LOCAL_BROADCAST_CONNECTED_TO_CLOUD_ACTION:
                    if (bundle != null) {
                        FileObjectType fileObjectType = (FileObjectType) bundle.getSerializable("fileObjectType");
                        createFragmentTransaction("/", fileObjectType);
                    }
                    break;
            }
        }
    }
}
