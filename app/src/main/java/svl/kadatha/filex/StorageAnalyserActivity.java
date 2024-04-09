package svl.kadatha.filex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class StorageAnalyserActivity extends  BaseActivity implements MediaMountReceiver.MediaMountListener,DetailFragmentListener, DeleteFileAlertDialog.OKButtonClickListener
{
    private Context context;
    private LocalBroadcastManager localBroadcastManager;
    private MediaMountReceiver mediaMountReceiver;
    public static FragmentManager FM;
    public FragmentManager fm;
    public PackageManager pm;
    public TextView current_dir, file_number;
    static LinkedList<FilePOJO> RECENTS=new LinkedList<>();
    public RecentDialogListener recentDialogListener;
    private OtherActivityBroadcastReceiver otherActivityBroadcastReceiver;
    public boolean clear_cache;
    public List<FilePOJO> storage_filePOJO_list;
    public Toolbar bottom_toolbar,actionmode_toolbar;
    public String toolbar_shown="bottom";
    private ImageButton all_select;
    public FloatingActionButton floatingActionButton;
    public static final String ACTIVITY_NAME="STORAGE_ANALYSER_ACTIVITY";
    private int countBackPressed=0;
    private Group search_toolbar;
    public EditText search_edittext;
    public boolean search_toolbar_visible;
    private KeyBoardUtil keyBoardUtil;
    private InputMethodManager imm;
    private RepositoryClass repositoryClass;
    private StorageAnalyserActivityViewModel viewModel;
    private PopupWindow listPopWindow;
    final ArrayList<ListPopupWindowPOJO> list_popupwindowpojos=new ArrayList<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        repositoryClass=RepositoryClass.getRepositoryClass();
        viewModel=new ViewModelProvider(this).get(StorageAnalyserActivityViewModel.class); //required to clear hashmap internal and external storage details on final finish of activity
        mediaMountReceiver=new MediaMountReceiver();
        mediaMountReceiver.addMediaMountListener(this);
        IntentFilter intentFilter=new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addDataScheme("file");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(mediaMountReceiver, intentFilter,RECEIVER_NOT_EXPORTED);
        }else {
            context.registerReceiver(mediaMountReceiver, intentFilter);
        }

        localBroadcastManager= LocalBroadcastManager.getInstance(context);
        otherActivityBroadcastReceiver= new OtherActivityBroadcastReceiver();
        IntentFilter localBroadcastIntentFilter=new IntentFilter();
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION);
        localBroadcastManager.registerReceiver(otherActivityBroadcastReceiver,localBroadcastIntentFilter);

        fm=getSupportFragmentManager();
        FM=fm;
        pm=getPackageManager();
        setContentView(R.layout.activity_storage_analyser);
        ConstraintLayout root_layout=findViewById(R.id.storage_analyser_root_layout);
        ImageButton back_btn=findViewById(R.id.storage_analyser_back_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        current_dir=findViewById(R.id.storage_analyser_current_dir_tv);
        file_number=findViewById(R.id.storage_analyser_file_number); //initiate here before adding fragment
        Intent intent=getIntent();
        if(intent!=null)
        {
            if(savedInstanceState==null)
            {
                createFragmentTransaction(Global.GET_INTERNAL_STORAGE_FILEPOJO_STORAGE_DIR().getPath(),Global.GET_INTERNAL_STORAGE_FILEPOJO_STORAGE_DIR().getFileObjectType());
            }
        }

        current_dir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileSelectorRecentDialog fileSelectorRecentDialog = new FileSelectorRecentDialog();
                fileSelectorRecentDialog.show(fm, "storage_analyser_recent_file_dialog");
            }
        });

        ImageButton overflow_img_btn=findViewById(R.id.storage_analyser_overflow);
        overflow_img_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listPopWindow.showAsDropDown(v,0,Global.LIST_POPUP_WINDOW_DROP_DOWN_OFFSET);
            }
        });
        list_popupwindowpojos.add(new ListPopupWindowPOJO(R.drawable.clean_icon,getString(R.string.clean_storage),1));
        listPopWindow=new PopupWindow(context);
        ListView listView=new ListView(context);
        listView.setAdapter(new ListPopupWindowPOJO.PopupWindowAdapter(context,list_popupwindowpojos));
        listPopWindow.setContentView(listView);
        listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popupwindow_width));
        listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopWindow.setFocusable(true);
        listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.list_popup_background));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> adapterview, View v, int p1,long p2)
            {
                if (p1 == 0) {
                    StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                    actionModeFinish(storageAnalyserFragment, storageAnalyserFragment.fileclickselected);
                    CleanStorageDialog cleanMemoryDialog = new CleanStorageDialog();
                    cleanMemoryDialog.show(fm, "clean_storage_dialog");
                }
                listPopWindow.dismiss();
            }

        });


        all_select=findViewById(R.id.storage_analyser_all_select);
        all_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StorageAnalyserFragment storageAnalyserFragment =(StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if (storageAnalyserFragment.adapter == null || storageAnalyserFragment.progress_bar.getVisibility()==View.VISIBLE) {
                    Global.print(context,getString(R.string.please_wait));
                    return;
                }

                if (storageAnalyserFragment.viewModel.mselecteditems.size() < storageAnalyserFragment.filePOJO_list.size()) {
                    all_select.setImageResource(R.drawable.deselect_icon);
                    storageAnalyserFragment.selectAll();
                } else {
                    all_select.setImageResource(R.drawable.select_icon);
                    storageAnalyserFragment.deselectAll();
                }
            }
        });

        int imageview_dimension;
        if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==0) imageview_dimension = Global.IMAGEVIEW_DIMENSION_SMALL_LIST;
        else if(Global.RECYCLER_VIEW_FONT_SIZE_FACTOR==2) imageview_dimension=Global.IMAGEVIEW_DIMENSION_LARGE_LIST;
        else imageview_dimension=Global.IMAGEVIEW_DIMENSION_MEDIUM_LIST;


        LinearLayout size_description_layout=findViewById(R.id.storage_analyser_size_description);
        ConstraintLayout.LayoutParams layoutParams= (ConstraintLayout.LayoutParams) size_description_layout.getLayoutParams();
        layoutParams.setMargins(imageview_dimension+Global.TEN_DP,0,0,0);

        keyBoardUtil=new KeyBoardUtil(root_layout);

        imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        search_toolbar=findViewById(R.id.storage_analyser_search_toolbar);
        search_edittext=findViewById(R.id.storage_analyser_search_view_edit_text);
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
                if(!search_toolbar_visible)
                {
                    return;
                }
                StorageAnalyserFragment storageAnalyserFragment =(StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if(storageAnalyserFragment !=null && storageAnalyserFragment.adapter!=null)
                {
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

        EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(this,4,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
        int[] bottom_drawables ={R.drawable.search_icon,R.drawable.refresh_icon,R.drawable.sort_icon,R.drawable.no_icon};
        String [] titles=new String[]{getString(R.string.search),getString(R.string.refresh),getString(R.string.sort),getString(R.string.close)};
        tb_layout.setResourceImageDrawables(bottom_drawables,titles);

        bottom_toolbar=findViewById(R.id.storage_analyser_bottom_toolbar);
        bottom_toolbar.addView(tb_layout);


        Button search_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        search_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StorageAnalyserFragment storageAnalyserFragment=(StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if(storageAnalyserFragment.progress_bar.getVisibility()==View.VISIBLE)
                {
                    Global.print(context,getString(R.string.please_wait));
                    return;
                }
                if(!search_toolbar_visible)
                {
                    setSearchBarVisibility(true);
                }
                else
                {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                }
            }
        });


        Button refresh_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        refresh_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StorageAnalyserFragment storageAnalyserFragment =(StorageAnalyserFragment)fm.findFragmentById(R.id.storage_analyser_container);
                if(storageAnalyserFragment.progress_bar.getVisibility()==View.VISIBLE)
                {
                    Global.print(context,getString(R.string.please_wait));
                    return;
                }
                fm.beginTransaction().detach(storageAnalyserFragment).commit();
                fm.beginTransaction().attach(storageAnalyserFragment).commit();
            }
        });

        Button sort_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        sort_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StorageAnalyserSortDialog storageAnalyserSortDialog=new StorageAnalyserSortDialog();
                storageAnalyserSortDialog.show(fm,"storage_analyser_sort_dialog");
            }
        });

        Button exit_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_4);
        exit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(search_toolbar_visible)
                {
                    setSearchBarVisibility(false);
                }
                finish();
            }
        });

        tb_layout =new EquallyDistributedButtonsWithTextLayout(this,2,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
        int[] actionmode_drawables ={R.drawable.delete_icon,R.drawable.properties_icon};
        titles=new String[]{getString(R.string.delete),getString(R.string.properties)};
        tb_layout.setResourceImageDrawables(actionmode_drawables,titles);

        actionmode_toolbar=findViewById(R.id.storage_analyser_actionmode_toolbar);
        actionmode_toolbar.addView(tb_layout);
        Button delete_btn=actionmode_toolbar.findViewById(R.id.toolbar_btn_1);
        delete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if(storageAnalyserFragment.viewModel.mselecteditems.size()==0)
                {
                    Global.print(context,getString(R.string.could_not_perform_action));
                    DeselectAllAndAdjustToolbars(storageAnalyserFragment, storageAnalyserFragment.fileclickselected);
                    return;
                }
                final ArrayList<String> files_selected_array=new ArrayList<>();
                int size = storageAnalyserFragment.viewModel.mselecteditems.size();
                for (int i = 0; i < size; ++i) {
                    files_selected_array.add(storageAnalyserFragment.viewModel.mselecteditems.getValueAtIndex(i));
                }

                DeleteFileAlertDialog deleteFileAlertDialog = DeleteFileAlertDialog.getInstance(files_selected_array, storageAnalyserFragment.fileObjectType, storageAnalyserFragment.fileclickselected,true);
                deleteFileAlertDialog.show(fm, "delete_dialog");
                setSearchBarVisibility(false);

            }
        });

        Button properties_btn=actionmode_toolbar.findViewById(R.id.toolbar_btn_2);
        properties_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if(storageAnalyserFragment.viewModel.mselecteditems.size()==0)
                {
                    Global.print(context,getString(R.string.could_not_perform_action));
                    DeselectAllAndAdjustToolbars(storageAnalyserFragment, storageAnalyserFragment.fileclickselected);
                    return;
                }
                final ArrayList<String> files_selected_array=new ArrayList<>();
                int size = storageAnalyserFragment.viewModel.mselecteditems.size();
                for (int i = 0; i < size; ++i) {
                    files_selected_array.add(storageAnalyserFragment.viewModel.mselecteditems.getValueAtIndex(i));
                }
                PropertiesDialog propertiesDialog=PropertiesDialog.getInstance(files_selected_array,storageAnalyserFragment.fileObjectType);
                propertiesDialog.show(fm,"properties_dialog");
            }
        });

        storage_filePOJO_list=getFilePOJO_list();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onbackpressed(true);
            }
        });
    }

     @Override
    protected void onStart()
    {
        // TODO: Implement this method
        super.onStart();
        clear_cache=true;
    }



    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("clear_cache",clear_cache);
        outState.putString("toolbar_shown",toolbar_shown);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        clear_cache=savedInstanceState.getBoolean("clear_cache");
        toolbar_shown=savedInstanceState.getString("toolbar_shown");
        switch (toolbar_shown)
        {
            case "bottom":
                bottom_toolbar.setVisibility(View.VISIBLE);
                actionmode_toolbar.setVisibility(View.GONE);
                break;
            case "actionmode":
                actionmode_toolbar.setVisibility(View.VISIBLE);
                bottom_toolbar.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isFinishing() && !isChangingConfigurations() && clear_cache)
        {
            clearCache();
        }
    }

    public void setSearchBarVisibility(boolean visible)
    {
        StorageAnalyserFragment storageAnalyserFragment=(StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
        if(storageAnalyserFragment.progress_bar.getVisibility()==View.VISIBLE && visible)
        {
            Global.print(context,getString(R.string.please_wait));
            return;
        }

        search_toolbar_visible=visible;
        if(search_toolbar_visible)
        {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
            search_toolbar.setVisibility(View.VISIBLE);
            search_edittext.requestFocus();
        }
        else
        {
            imm.hideSoftInputFromWindow(search_edittext.getWindowToken(),0);
            search_toolbar.setVisibility(View.GONE);
            search_edittext.setText("");
            search_edittext.clearFocus();
            DeselectAllAndAdjustToolbars(storageAnalyserFragment,storageAnalyserFragment.getTag());
            if(storageAnalyserFragment.adapter!=null)storageAnalyserFragment.adapter.getFilter().filter(null);
        }
    }


    public void clearCache()
    {
        Global.CLEAR_CACHE();
    }

    @Override
    public void onScrollRecyclerView(boolean showToolBar) {

        if(showToolBar)
        {
            switch (toolbar_shown) {
                case "bottom":
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    break;
                case "actionmode":
                    actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    break;

            }

        }
        else {
            switch (toolbar_shown) {
                case "bottom":
                    bottom_toolbar.animate().translationY(bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    break;
                case "actionmode":
                    actionmode_toolbar.animate().translationY(actionmode_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                    break;
            }

        }


    }

    @Override
    public void actionModeFinish(Fragment fragment, String fileclickeselected) {
        if(fragment instanceof StorageAnalyserFragment)DeselectAllAndAdjustToolbars((StorageAnalyserFragment) fragment,fileclickeselected);
    }

    @Override
    public void onLongClickItem(int size) {

        if(size>=1)
        {
            bottom_toolbar.setVisibility(View.GONE);
            actionmode_toolbar.setVisibility(View.VISIBLE);
            toolbar_shown="actionmode";
            actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
        }
        else {
            StorageAnalyserFragment storageAnalyserFragment=(StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
            if(storageAnalyserFragment!=null)
            {
                if(size==storageAnalyserFragment.file_list_size)
                {
                    //mainActivity.all_select.setImageResource(R.drawable.deselect_icon);
                }
                DeselectAllAndAdjustToolbars(storageAnalyserFragment,storageAnalyserFragment.fileclickselected);
            }

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

    public void clearCache(String file_path, FileObjectType fileObjectType)
    {
        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(file_path),fileObjectType); //no need of broad cast here, as the method includes broadcast
    }

    @Override
    public void setCurrentDirText(String current_dir_name) {
        current_dir.setText(current_dir_name);
    }

    @Override
    public void enableParentDirImageButton(boolean enable) {

    }

    @Override
    public void rescanLargeDuplicateFilesLibrary(String type)
    {
        StorageAnalyserFragment storageAnalyserFragment=(StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
        if(storageAnalyserFragment==null)return;
        if(storageAnalyserFragment.fileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
        {
            if(storageAnalyserFragment.progress_bar.getVisibility()==View.VISIBLE)
            {
                Global.print(context, getString(R.string.please_wait));
            }
            else
            {
                actionModeFinish(storageAnalyserFragment,storageAnalyserFragment.fileclickselected);
                if(type.equals("large")){
                    rescan_large_files_library();
                }
                else if(type.equals("duplicate"))
                {
                    rescan_duplicate_files_library();
                }
            }
        }
        else
        {
            if(type.equals("large")){
                rescan_large_files_library();
            }
            else if(type.equals("duplicate"))
            {
                rescan_duplicate_files_library();
            }
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
                if (storageAnalyserFragment == null) return;
                if (storageAnalyserFragment.fileclickselected.equals("Large Files")) {
                    fm.beginTransaction().detach(storageAnalyserFragment).commit();
                    fm.beginTransaction().attach(storageAnalyserFragment).commit();

                }

            }
        });
    }


        public void rescan_duplicate_files_library()
        {
            Global.print(context,getString(R.string.scanning_started));
            ExecutorService executorService=MyExecutorService.getExecutorService();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Iterator<Map.Entry<String, List<FilePOJO>>> iterator=repositoryClass.hashmap_file_pojo.entrySet().iterator();
                    while(iterator.hasNext())
                    {
                        Map.Entry<String,List<FilePOJO>> entry=iterator.next();
                        if(entry.getKey().equals(FileObjectType.SEARCH_LIBRARY_TYPE+"Duplicate Files"))
                        {
                            iterator.remove();
                            break;
                        }

                    }
                    //get methods kept below instead of in if block above to avoid likely concurrent modification exception

                    viewModel.getDuplicateFileList(false);
                    StorageAnalyserFragment storageAnalyserFragment=(StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                    if(storageAnalyserFragment==null)return;
                    if(storageAnalyserFragment.fileclickselected.equals("Duplicate Files"))
                    {
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
        localBroadcastManager.unregisterReceiver(otherActivityBroadcastReceiver);
        context.unregisterReceiver(mediaMountReceiver);
        listPopWindow.dismiss(); // to avoid memory leak on orientation change
    }

    private void onbackpressed(boolean onBackPressed)
    {
        StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
        if(keyBoardUtil.getKeyBoardVisibility())
        {
            imm.hideSoftInputFromWindow(search_edittext.getWindowToken(),0);
        }
        else if(storageAnalyserFragment.viewModel.mselecteditems.size()>0)
        {
            DeselectAllAndAdjustToolbars(storageAnalyserFragment, storageAnalyserFragment.fileclickselected);
        }
        else if(search_toolbar_visible)
        {
            setSearchBarVisibility(false);
        }
        else
        {
            switch (toolbar_shown)
            {
                case "bottom":
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    bottom_toolbar.setVisibility(View.VISIBLE);
                    storageAnalyserFragment.is_toolbar_visible=true;
                    break;
                case "actionmode":
                    actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    actionmode_toolbar.setVisibility(View.VISIBLE);
                    storageAnalyserFragment.is_toolbar_visible=true;
                    break;
            }

            if(fm.getBackStackEntryCount()>1)
            {
                fm.popBackStack();
                int frag=2, entry_count=fm.getBackStackEntryCount();
                storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentByTag(fm.getBackStackEntryAt(entry_count-frag).getName());
                String tag= storageAnalyserFragment.getTag();

                while(tag !=null && !new File(tag).exists() && !tag.equals("Large Files") && !tag.equals("Duplicate Files") && storageAnalyserFragment.currentUsbFile == null)
                {
                    fm.popBackStack();
                    ++frag;
                    if(frag>entry_count) break;
                    storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentByTag(fm.getBackStackEntryAt(entry_count-frag).getName());
                    tag = storageAnalyserFragment.getTag();
                }
                countBackPressed=0;

            }
            else
            {
                if(onBackPressed)
                {
                    countBackPressed++;
                    if(countBackPressed==1)
                    {
                        Global.print(context,getString(R.string.press_again_to_close_activity));
                    }
                    else
                    {
                        finish();
                    }
                }
                else
                {
                    Global.print(context,context.getString(R.string.click_close_button_to_exit));
                }
            }
        }
    }

    public List<FilePOJO> getFilePOJO_list()
    {
        List<FilePOJO> filePOJOS = new ArrayList<>();
        for(FilePOJO filePOJO:repositoryClass.storage_dir)
        {
            if(filePOJO.getFileObjectType()==FileObjectType.FILE_TYPE)
            {
                filePOJOS.add(filePOJO);
            }
        }
        return filePOJOS;
    }

    public void createFragmentTransaction(String file_path, FileObjectType fileObjectType)
    {
        String fragment_tag;
        String existingFilePOJOkey="";
        StorageAnalyserFragment storageAnalyserFragment =(StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
        if(storageAnalyserFragment !=null)
        {
            fragment_tag= storageAnalyserFragment.getTag();
            existingFilePOJOkey= storageAnalyserFragment.fileObjectType+fragment_tag;
            DeselectAllAndAdjustToolbars(storageAnalyserFragment, storageAnalyserFragment.getTag());
        }

        if(!(fileObjectType+file_path).equals(existingFilePOJOkey))
        {
            fm.beginTransaction().replace(R.id.storage_analyser_container, StorageAnalyserFragment.getInstance(fileObjectType),file_path).addToBackStack(file_path)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commitAllowingStateLoss();
        }

    }

    public void DeselectAllAndAdjustToolbars(StorageAnalyserFragment sad, String sad_tag)
    {
        bottom_toolbar.setVisibility(View.VISIBLE);
        actionmode_toolbar.setVisibility(View.GONE);
        toolbar_shown="bottom";
        viewModel.tool_bar_shown="bottom";
        bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
        if(sad!=null)
        {
            sad.clearSelectionAndNotifyDataSetChanged();
            sad.is_toolbar_visible=true;
            all_select.setImageResource(R.drawable.select_icon);
        }
    }

    @Override
    public void deleteDialogOKButtonClick() {
        final StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
        DeselectAllAndAdjustToolbars(storageAnalyserFragment, storageAnalyserFragment.fileclickselected);
    }

    private class OtherActivityBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            StorageAnalyserFragment storageAnalyserFragment = (StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
            String activity_name=intent.getStringExtra("activity_name");
            String file_path=intent.getStringExtra("file_path");
            FileObjectType fileObjectType= (FileObjectType) intent.getSerializableExtra("fileObjectType");
            switch (intent.getAction()) {
                case Global.LOCAL_BROADCAST_DELETE_FILE_ACTION:
                    if (storageAnalyserFragment != null) storageAnalyserFragment.local_activity_delete = true;
                    break;
                case Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION:
                    if (storageAnalyserFragment != null) storageAnalyserFragment.modification_observed = true;
                    break;
            }
        }
    }

    @Override
    public void onMediaMount(String action) {
        switch (action) {
            case "android.intent.action.MEDIA_MOUNTED":
                repositoryClass.storage_dir.clear();
                repositoryClass.storage_dir.addAll(new ArrayList<>(StorageUtil.getSdCardPaths(context, true)));
                Global.WORKOUT_AVAILABLE_SPACE();
                storage_filePOJO_list=getFilePOJO_list();
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
                storage_filePOJO_list=getFilePOJO_list();
                if (recentDialogListener != null) {
                    recentDialogListener.onMediaAttachedAndRemoved();
                }
                FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(repositoryClass.external_storage_path_list, FileObjectType.FILE_TYPE);
                StorageAnalyserFragment storageAnalyserFragment =(StorageAnalyserFragment) fm.findFragmentById(R.id.storage_analyser_container);
                if(storageAnalyserFragment !=null) storageAnalyserFragment.clearSelectionAndNotifyDataSetChanged();

                break;
        }
    }


    interface RecentDialogListener
    {
        void onMediaAttachedAndRemoved();
    }



}
