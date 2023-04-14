package svl.kadatha.filex;

import android.Manifest;
import android.app.Activity;
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
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import me.jahnen.libaums.core.fs.FileSystem;
import me.jahnen.libaums.core.fs.UsbFile;

public class ArchiveViewerActivity extends BaseActivity{

    public ImageButton back_button,parent_dir_image_button,all_select;
    TextView file_number_view;
    public static final String ACTIVITY_NAME="ARCHIVE_VIEWER_ACTIVITY";
    Toolbar bottom_toolbar;
    ConstraintLayout search_toolbar;

    TextView current_dir_textview;
    Context context=this;
    private int countBackPressed=0;

    public TinyDB tinyDB;
    private static final boolean[] alreadyNotificationWarned=new boolean[1];

    static File ZIP_FILE;



    public static PackageManager PM;
    public PackageManager pm;

    public FragmentManager fm;
    public static FragmentManager FM;

    private EditText search_view;
    public boolean search_toolbar_visible;
    private KeyBoardUtil keyBoardUtil;

    private LocalBroadcastManager localBroadcastManager;
    private OtherActivityBroadcastReceiver otherActivityBroadcastReceiver;

    private InputMethodManager imm;

    public boolean clear_cache;

    public FloatingActionButton floating_button_back;

    public ArchiveViewerViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager())
            {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                    activityResultLauncher_all_file_access_permission.launch(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activityResultLauncher_all_file_access_permission.launch(intent);
                }
            }
        }

        PermissionsUtil permissionUtil=new PermissionsUtil(context,ArchiveViewerActivity.this);
        permissionUtil.check_permission();
        tinyDB=new TinyDB(context);
        setContentView(R.layout.activity_archive_viewer);
        ConstraintLayout root_layout=findViewById(R.id.archive_root_layout);
        viewModel=new ViewModelProvider(this).get(ArchiveViewerViewModel.class);
        fm=getSupportFragmentManager();
        FM=fm;
        pm=getPackageManager();
        PM=pm;
        localBroadcastManager=LocalBroadcastManager.getInstance(context);


        otherActivityBroadcastReceiver= new OtherActivityBroadcastReceiver();
        IntentFilter localBroadcastIntentFilter=new IntentFilter();
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION);
        localBroadcastManager.registerReceiver(otherActivityBroadcastReceiver,localBroadcastIntentFilter);


        keyBoardUtil=new KeyBoardUtil(root_layout);


        imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        search_toolbar=findViewById(R.id.search_toolbar);


        search_view=search_toolbar.findViewById(R.id.search_bar_view);
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
                if(!search_toolbar_visible)
                {
                    return;
                }
                ArchiveViewFragment archiveViewFragment=(ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
                if(archiveViewFragment!=null && archiveViewFragment.adapter!=null)
                {
                    archiveViewFragment.adapter.getFilter().filter(s.toString());
                }
            }
        });

        file_number_view=findViewById(R.id.archive_detail_fragment_file_number);

        back_button = findViewById(R.id.archive_top_toolbar_back_button);
        back_button.setVisibility(View.GONE);
        parent_dir_image_button=findViewById(R.id.archive_top_toolbar_parent_dir_imagebutton);
        current_dir_textview=findViewById(R.id.archive_top_toolbar_current_dir_label);
        all_select=findViewById(R.id.archive_detail_fragment_all_select);

        TopToolbarClickListener topToolbarClickListener = new TopToolbarClickListener();
        back_button.setOnClickListener(topToolbarClickListener);
        parent_dir_image_button.setOnClickListener(topToolbarClickListener);
        current_dir_textview.setOnClickListener(topToolbarClickListener);
        all_select.setOnClickListener(topToolbarClickListener);


        floating_button_back = findViewById(R.id.archive_floating_action_button_back);
        floating_button_back.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View p1)
            {
                onbackpressed(false);
            }
        });



        EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(this,4,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
        int[] drawables ={R.drawable.search_icon,R.drawable.refresh_icon,R.drawable.extract_icon,R.drawable.no_icon};
        String [] titles=new String[]{getString(R.string.search),getString(R.string.refresh),getString(R.string.extract),getString(R.string.close)};
        tb_layout.setResourceImageDrawables(drawables,titles);
        bottom_toolbar=findViewById(R.id.archive_bottom_toolbar);
        bottom_toolbar.addView(tb_layout);
        Button search=bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        Button refresh=bottom_toolbar.findViewById(R.id.toolbar_btn_2);
        Button extract = bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        Button close=bottom_toolbar.findViewById(R.id.toolbar_btn_4);

        BottomToolbarClickListener bottomToolbarClickListener=new BottomToolbarClickListener();
        search.setOnClickListener(bottomToolbarClickListener);
        refresh.setOnClickListener(bottomToolbarClickListener);
        extract.setOnClickListener(bottomToolbarClickListener);
        close.setOnClickListener(bottomToolbarClickListener);


        Global.WARN_NOTIFICATIONS_DISABLED(context,NotifManager.CHANNEL_ID,alreadyNotificationWarned);

        viewModel.isExtractionCompleted.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                ArchiveViewFragment archiveViewFragment=(ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
                if(asyncTaskStatus==AsyncTaskStatus.STARTED)
                {
                    if(archiveViewFragment!=null)archiveViewFragment.progress_bar.setVisibility(View.VISIBLE);
                }
                else if (asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    if(archiveViewFragment!=null)archiveViewFragment.progress_bar.setVisibility(View.GONE);
                }

                if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    if(viewModel.zipFileExtracted)
                    {
                        createFragmentTransaction(Global.ARCHIVE_EXTRACT_DIR.getAbsolutePath(),FileObjectType.FILE_TYPE);
                    }
                    else
                    {
                        if(Global.ARCHIVE_EXTRACT_DIR.exists())
                        {
                            Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.ARCHIVE_EXTRACT_DIR);
                        }
                    }

                    viewModel.isExtractionCompleted.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });

        viewModel.isDeletionCompleted.observe(this, new Observer<AsyncTaskStatus>() {
            @Override
            public void onChanged(AsyncTaskStatus asyncTaskStatus) {
                ArchiveViewFragment archiveViewFragment=(ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
                if(archiveViewFragment==null)return;
                if(asyncTaskStatus==AsyncTaskStatus.STARTED)
                {
                    archiveViewFragment.progress_bar.setVisibility(View.VISIBLE);
                }
                else if (asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    archiveViewFragment.progress_bar.setVisibility(View.GONE);
                }

                if(asyncTaskStatus==AsyncTaskStatus.COMPLETED)
                {
                    viewModel.isDeletionCompleted.setValue(AsyncTaskStatus.NOT_YET_STARTED);
                }
            }
        });


        if(savedInstanceState==null)
        {
            //createFragmentTransaction(Global.INTERNAL_PRIMARY_STORAGE_PATH,FileObjectType.FILE_TYPE);
            Intent intent=getIntent();
            if(intent!=null)
            {
                onNewIntent(intent);
            }
        }

    }


    public void set_visibility_searchbar(boolean visible)
    {
        ArchiveViewFragment archiveViewFragment=(ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
        if(archiveViewFragment.progress_bar.getVisibility()==View.VISIBLE)
        {
            Global.print(context,getString(R.string.please_wait));
            return;
        }
        search_toolbar_visible=visible;
        if(search_toolbar_visible)
        {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
            search_toolbar.setVisibility(View.VISIBLE);
            search_view.requestFocus();
        }
        else
        {
            actionmode_finish(archiveViewFragment, archiveViewFragment.fileclickselected);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        // TODO: Implement this method
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        final List<String> permission_not_granted_list=new ArrayList<>();
        if(requestCode==PermissionsUtil.PERMISSIONS_REQUEST_CODE && grantResults.length>0)
        {
            for(int i=0;i<permissions.length;++i)
            {
                if(grantResults[i]!=PackageManager.PERMISSION_GRANTED)
                {
                    permission_not_granted_list.add(permissions[i]);
                }
                else if(permissions[i].equals(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                {
                    Global.STORAGE_DIR.clear();
                    clearCache();
                    Intent in=getIntent();
                    finish();
                    startActivity(in);
                    return;
                }
            }

        }

        if(grantResults.length==0 || !permission_not_granted_list.isEmpty())
        {
            for(String permission:permission_not_granted_list)
            {
                switch(permission)
                {
                    case android.Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if(shouldShowRequestPermissionRationale(permission))
                            {
                                showDialogOK(getString(R.string.read_and_write_permissions_are_must_for_the_app_to_work_please_grant_permissions),new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        switch (which)
                                        {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                new PermissionsUtil(context,ArchiveViewerActivity.this).check_permission();
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                Global.print(context,getString(R.string.permission_not_granted));
                                                finish();
                                                break;
                                        }
                                    }
                                });
                            }
                            else
                            {
                                Global.print(context,getString(R.string.seems_permissions_were_not_granted_goto_settings_grant_permissions_to_app));
                                finish();
                                break;
                            }
                        }
                        break;

                    case android.Manifest.permission.READ_PHONE_STATE:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if(shouldShowRequestPermissionRationale(permission))
                            {
                                showDialogOK(getString(R.string.permission_required_to_regulate_audio_play_when_phone_rings),new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        switch (which)
                                        {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                new PermissionsUtil(context,ArchiveViewerActivity.this).check_permission();
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                Global.print(context,getString(R.string.permission_not_granted));
                                                break;
                                        }
                                    }
                                });
                            }

                        }
                        break;

                    case Manifest.permission.POST_NOTIFICATIONS:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if(shouldShowRequestPermissionRationale(permission))
                            {
                                showDialogOK(getString(R.string.permission_rationale_for_notification),new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        switch (which)
                                        {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                new PermissionsUtil(context,ArchiveViewerActivity.this).check_permission();
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                Global.print(context,getString(R.string.permission_not_granted));
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

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener)
    {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), okListener)
                .setNegativeButton(getString(R.string.cancel), okListener)
                .create()
                .show();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        // TODO: Implement this method
        super.onNewIntent(intent);
        if(intent==null)
        {
            return;
        }

        String receivedAction=intent.getAction();
        Uri uri=intent.getData();
        if(receivedAction!=null && receivedAction.equals(Intent.ACTION_VIEW) &&  uri !=null)
        {
            String path=RealPathUtil.getRealPath(context,uri);
            if(path==null)
            {
                Global.print(context,getString(R.string.could_not_open_zipe_file));
                return;
            }
            ZIP_FILE=new File(path);
            ZipFile zipfile;
            try
            {
                zipfile=new ZipFile(ZIP_FILE);
            }
            catch(IOException e)
            {
                Global.print(context,getString(R.string.could_not_open_zipe_file));
                return;
            }

            viewModel.extractArchive(zipfile);
        }
    }


    @Override
    protected void onStart()
    {
        // TODO: Implement this method
        super.onStart();
        clear_cache=true;
        Global.WORKOUT_AVAILABLE_SPACE();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(search_toolbar_visible)
        {
            set_visibility_searchbar(false);
        }

        if(!isChangingConfigurations() && clear_cache)
        {
            clearCache();
        }
    }

    public void clearCache()
    {
        Global.CLEAR_CACHE();
    }

    public void clearCache(String file_path, FileObjectType fileObjectType)
    {
        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(file_path),fileObjectType); //no need of broad cast here, as the method includes broadcast
    }


    private final ActivityResultLauncher<Intent> activityResultLauncher_all_file_access_permission=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager())
                {
                    Global.STORAGE_DIR.clear();
                    clearCache();
                    Intent in=getIntent();
                    finish();
                    startActivity(in);
                }
                else
                {
                    showDialogOK(getString(R.string.read_and_write_permissions_are_must_for_the_app_to_work_please_grant_permissions),new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            switch (which)
                            {
                                case DialogInterface.BUTTON_POSITIVE:
                                    try {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                        intent.addCategory("android.intent.category.DEFAULT");
                                        intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                                        activityResultLauncher_all_file_access_permission.launch(intent);
                                    } catch (Exception e) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                        activityResultLauncher_all_file_access_permission.launch(intent);
                                    }
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    Global.print(context,getString(R.string.permission_not_granted));
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
    protected void onSaveInstanceState(Bundle outState)
    {
        // TODO: Implement this method
        super.onSaveInstanceState(outState);
        outState.putBoolean("clear_cache",clear_cache);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        // TODO: Implement this method
        super.onRestoreInstanceState(savedInstanceState);
        clear_cache=savedInstanceState.getBoolean("clear_cache");
    }

    public void createFragmentTransaction(String file_path,FileObjectType fileObjectType)
    {
        ArchiveViewFragment archiveViewFragment=(ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
        if(archiveViewFragment!=null)
        {
            actionmode_finish(archiveViewFragment,file_path); //string provided to actionmode_finish method is file_path (which is clicked, not the existing file_path) to be created of fragemnttransaction
        }

        fm.beginTransaction().replace(R.id.archive_detail_fragment,ArchiveViewFragment.getInstance(fileObjectType),file_path)
                .addToBackStack(file_path).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
    }

    @Override
    public void onBackPressed()
    {
        // TODO: Implement this method
        onbackpressed(true);
    }

    private void onbackpressed(boolean onBackPressed)
    {
        ArchiveViewFragment archiveViewFragment=(ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);

        if(keyBoardUtil.getKeyBoardVisibility())
        {

            imm.hideSoftInputFromWindow(search_view.getWindowToken(),0);
        }
        else if(archiveViewFragment.viewModel.mselecteditems.size()>0)
        {
            actionmode_finish(archiveViewFragment,archiveViewFragment.fileclickselected);

        }
        else if(search_toolbar_visible)
        {
            set_visibility_searchbar(false);
        }
        else
        {
            archive_exit();
            int entry_count;
            if((entry_count=fm.getBackStackEntryCount())>1)
            {
                bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                bottom_toolbar.setVisibility(View.VISIBLE);

                fm.popBackStack();
                int frag=2;
                archiveViewFragment=(ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
                String tag=archiveViewFragment.getTag();
                while(!new File(tag).exists())
                {
                    fm.popBackStack();
                    ++frag;
                    if(frag>entry_count) break;
                    archiveViewFragment=(ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
                    tag = archiveViewFragment.getTag();
                }

                parent_dir_image_button.setEnabled(false);
                parent_dir_image_button.setAlpha(Global.DISABLE_ALFA);
                countBackPressed=0;
				/*
				if((entry_count-frag)<1)
				{
					floating_button_back.setEnabled(false);
					floating_button_back.setAlpha(Global.DISABLE_ALFA);
				}

				 */
            }
            else
            {
				/*
				floating_button_back.setEnabled(false);
				floating_button_back.setAlpha(Global.DISABLE_ALFA);

				 */
                if(onBackPressed)
                {
                    countBackPressed++;
                    if(countBackPressed==1)
                    {
                        Global.print(context,getString(R.string.press_again_to_close_activity));
                    }
                    else
                    {
                        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.ARCHIVE_EXTRACT_DIR);
                        if(Global.WHETHER_TO_CLEAR_CACHE_TODAY)
                        {
                            Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(getCacheDir());
                            if(Global.SIZE_APK_ICON_LIST>800)
                            {
                                Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.APK_ICON_DIR);
                            }
                            tinyDB.putInt("cache_cleared_month",Global.CURRENT_MONTH);
                            Global.print(context,"cleared cache");
                        }
                        finish();
                    }
                }
                else
                {
                    Global.print(context,getString(R.string.click_close_button_to_exit));
                }

            }
        }
    }

    @Override
    protected void onPause()
    {
        // TODO: Implement this method
        super.onPause();
        imm.hideSoftInputFromWindow(search_view.getWindowToken(),0);
    }


    @Override
    protected void onDestroy()
    {
        // TODO: Implement this method
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(otherActivityBroadcastReceiver);
    }

    public void DeselectAllAndAdjustToolbars(ArchiveViewFragment archiveViewFragment,String detailfrag_tag)
    {
        if(archiveViewFragment!=null)
        {
            archiveViewFragment.clearSelectionAndNotifyDataSetChanged();
            archiveViewFragment.is_toolbar_visible=true;
            all_select.setImageResource(R.drawable.select_icon);
        }
    }

    public void actionmode_finish(ArchiveViewFragment archiveViewFragment, String archiveViewfrag_tag)
    {
        DeselectAllAndAdjustToolbars(archiveViewFragment,archiveViewfrag_tag);
        imm.hideSoftInputFromWindow(search_view.getWindowToken(),0);
        search_view.setText("");
        search_view.clearFocus();
        search_toolbar.setVisibility(View.GONE); //no need to call adapter.filter with null to refill filepjos as calling datasetchanged replenished archiveViewFragment.adapter.filepojo list
        search_toolbar_visible=false;
        if(archiveViewFragment.adapter!=null)
        {
            archiveViewFragment.adapter.getFilter().filter(null);
        }
    }

    public void archive_exit()
    {
        if(Global.ARCHIVE_EXTRACT_DIR.exists())
        {
            ArchiveViewFragment archiveViewFragment=(ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
            archiveViewFragment.progress_bar.setVisibility(View.VISIBLE);
            viewModel.deleteDirectory(Global.ARCHIVE_EXTRACT_DIR);
            FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(Global.ARCHIVE_EXTRACT_DIR.getAbsolutePath()),FileObjectType.FILE_TYPE);
        }
    }


    private class TopToolbarClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            // TODO: Implement this method
            ArchiveViewFragment archiveViewFragment=(ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
            int id = v.getId();
            if (id == R.id.archive_top_toolbar_back_button) {

            } else if (id == R.id.archive_top_toolbar_parent_dir_imagebutton) {

                File f = new File(archiveViewFragment.fileclickselected);
                String parent_file_path = f.getParent();
                if (parent_file_path==null) return;
                if (archiveViewFragment.fileObjectType == FileObjectType.FILE_TYPE) {

                    File parent_file=new File(parent_file_path);
                    if (parent_file != null && parent_file.list() != null) {
                        createFragmentTransaction(parent_file.getAbsolutePath(),FileObjectType.FILE_TYPE);
                    }
                }
            } else if (id == R.id.archive_top_toolbar_current_dir_label) {

            } else if (id == R.id.archive_detail_fragment_all_select) {
                if (archiveViewFragment.adapter == null || archiveViewFragment.progress_bar.getVisibility()==View.VISIBLE) {
                    Global.print(context,getString(R.string.please_wait));
                    return;
                }

                if (archiveViewFragment.viewModel.mselecteditems.size() < archiveViewFragment.filePOJO_list.size()) {
                    all_select.setImageResource(R.drawable.deselect_icon);
                    archiveViewFragment.adapter.selectAll();
                } else {
                    all_select.setImageResource(R.drawable.select_icon);
                    archiveViewFragment.adapter.deselectAll();
                }
            }

        }

    }

    private class BottomToolbarClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {

            ArchiveViewFragment archiveViewFragment=(ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
            int id = v.getId();
            if (id == R.id.toolbar_btn_1) {
                if(!search_toolbar_visible)
                {
                    set_visibility_searchbar(true);
                }
                else
                {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                }

            } else if (id == R.id.toolbar_btn_2) {
                if(archiveViewFragment.progress_bar.getVisibility()==View.VISIBLE)
                {
                    Global.print(context,getString(R.string.please_wait));
                    return;
                }
                fm.beginTransaction().detach(archiveViewFragment).commit();
                fm.beginTransaction().attach(archiveViewFragment).commit();
                Global.WORKOUT_AVAILABLE_SPACE();
            } else if (id == R.id.toolbar_btn_3) {
                if(archiveViewFragment.progress_bar.getVisibility()==View.VISIBLE)
                {
                    Global.print(context,getString(R.string.please_wait));
                    return;
                }
                if(search_toolbar_visible)
                {
                    set_visibility_searchbar(false);
                }

                if(archiveViewFragment.progress_bar.getVisibility()==View.VISIBLE)
                {
                    Global.print(context,getString(R.string.please_wait));
                    return;
                }
                Bundle bundle=new Bundle();
                ArrayList<String> files_selected_array=new ArrayList<>();
                ArrayList<String> zipentry_selected_array=new ArrayList<>();
                if(ZIP_FILE!=null)
                {
                    files_selected_array.add(ZIP_FILE.getAbsolutePath());
                    int size=archiveViewFragment.viewModel.mselecteditemsFilePath.size();
                    if(archiveViewFragment.viewModel.mselecteditemsFilePath.size()!=0)
                    {
                        List<File> file_list=new ArrayList<>();

                        for(int i=0;i<size;++i)
                        {
                            file_list.add(new File(archiveViewFragment.viewModel.mselecteditemsFilePath.valueAt(i)));
                        }
                        MainActivity.recursivefilepath(zipentry_selected_array,file_list);
                    }

                    ArchiveSetUpDialog unziparchiveDialog=ArchiveSetUpDialog.getInstance(files_selected_array,zipentry_selected_array,archiveViewFragment.fileObjectType,ArchiveSetUpDialog.ARCHIVE_ACTION_UNZIP);
                    unziparchiveDialog.show(fm,null);
                    archiveViewFragment.clearSelectionAndNotifyDataSetChanged();
                }
                else
                {
                    Global.print(context,getString(R.string.could_not_perform_action));
                    onbackpressed(false);
                }

            } else if (id == R.id.toolbar_btn_4) {
                if(search_toolbar_visible)
                {
                    set_visibility_searchbar(false);
                }
                Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.ARCHIVE_EXTRACT_DIR);
                if(Global.WHETHER_TO_CLEAR_CACHE_TODAY)
                {
                    Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(getCacheDir());
                    if(Global.SIZE_APK_ICON_LIST>800)
                    {
                        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.APK_ICON_DIR);
                    }
                    tinyDB.putInt("cache_cleared_month",Global.CURRENT_MONTH);
                    Global.print(context,"cleared cache");
                }
                finish();

            }
        }

    }

    private class OtherActivityBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArchiveViewFragment archiveViewFragment=(ArchiveViewFragment) fm.findFragmentById(R.id.archive_detail_fragment);
            String activity_name=intent.getStringExtra("activity_name");
            String file_path=intent.getStringExtra("file_path");
            FileObjectType fileObjectType= (FileObjectType) intent.getSerializableExtra("fileObjectType");
            switch (intent.getAction()) {

                case Global.LOCAL_BROADCAST_DELETE_FILE_ACTION:
                    if (archiveViewFragment != null) archiveViewFragment.local_activity_delete = true;
                    break;
                case Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION:
                    if (archiveViewFragment != null) archiveViewFragment.modification_observed = true;
                    break;

            }
        }
    }




    public static class ArchiveDetailRecyclerViewAdapter extends  RecyclerView.Adapter <ArchiveDetailRecyclerViewAdapter.ViewHolder> implements Filterable
    {

        private final Context context;
        private final ArchiveViewFragment archiveViewFragment;
        private final ArchiveViewerActivity archiveViewerActivity;

        private CardViewClickListener cardViewClickListener;
        private boolean show_file_path;

        ArchiveDetailRecyclerViewAdapter(Context context)
        {
            this.context=context;
            archiveViewerActivity=(ArchiveViewerActivity)context;
            archiveViewFragment=(ArchiveViewFragment) archiveViewerActivity.fm.findFragmentById(R.id.archive_detail_fragment);
            archiveViewerActivity.current_dir_textview.setText(archiveViewFragment.file_click_selected_name);
            archiveViewerActivity.file_number_view.setText(archiveViewFragment.viewModel.mselecteditems.size()+"/"+archiveViewFragment.file_list_size);
            if(archiveViewFragment.fileObjectType==FileObjectType.FILE_TYPE || archiveViewFragment.fileObjectType==FileObjectType.ROOT_TYPE)
            {
                File f=new File(archiveViewFragment.fileclickselected);
                File parent_file=f.getParentFile();
                if(parent_file!=null)
                {
                    
                }
                else
                {
                    archiveViewerActivity.current_dir_textview.setText(R.string.root_directory);
                    archiveViewerActivity.parent_dir_image_button.setEnabled(false);
                    archiveViewerActivity.parent_dir_image_button.setAlpha(Global.DISABLE_ALFA);
                }
            }
            
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, AdapterView.OnLongClickListener
        {
            final RecyclerViewLayout view;
            int pos;

            ViewHolder (RecyclerViewLayout view)
            {
                super(view);
                this.view=view;
                this.view.setOnClickListener(this);
                this.view.setOnLongClickListener(this);

            }

            @Override
            public void onClick(View p1)
            {
                pos=getBindingAdapterPosition();
                int size=archiveViewFragment.viewModel.mselecteditems.size();
                if(size>0)
                {
                    longClickMethod(p1,size);
                }
                else
                {
                    if(cardViewClickListener!=null)
                    {
                        FilePOJO filePOJO=archiveViewFragment.filePOJO_list.get(pos);
                        cardViewClickListener.onClick(filePOJO);
                    }
                }
            }

            private void longClickMethod (View v, int size)
            {
                pos=getBindingAdapterPosition();
                if(archiveViewFragment.viewModel.mselecteditems.get(pos,false))
                {
                    archiveViewFragment.viewModel.mselecteditems.delete(pos);
                    archiveViewFragment.viewModel.mselecteditemsFilePath.delete(pos);
                    v.setSelected(false);
                    ((RecyclerViewLayout)v).set_selected(false);
                    --size;

                    if(size==1)
                    {
                        if(cardViewClickListener!=null)
                        {
                            FilePOJO filePOJO=archiveViewFragment.filePOJO_list.get(pos);
                            cardViewClickListener.onLongClick(filePOJO);
                        }
                    }
                    else if(size>1)
                    {
                        if(cardViewClickListener!=null)
                        {
                            FilePOJO filePOJO=archiveViewFragment.filePOJO_list.get(pos);
                            cardViewClickListener.onLongClick(filePOJO);
                        }
                    }

                    if(size==0)
                    {
                        archiveViewerActivity.DeselectAllAndAdjustToolbars(archiveViewFragment,archiveViewFragment.fileclickselected);
                    }
                }
                else
                {
                    archiveViewFragment.viewModel.mselecteditems.put(pos,true);
                    archiveViewFragment.viewModel.mselecteditemsFilePath.put(pos,archiveViewFragment.filePOJO_list.get(pos).getPath());
                    v.setSelected(true);
                    ((RecyclerViewLayout)v).set_selected(true);
                    ++size;

                    

                    if(size==archiveViewFragment.file_list_size)
                    {
                        archiveViewerActivity.all_select.setImageResource(R.drawable.deselect_icon);
                    }

                    if(cardViewClickListener!=null)
                    {
                        FilePOJO filePOJO=archiveViewFragment.filePOJO_list.get(pos);
                        cardViewClickListener.onLongClick(filePOJO);
                    }
                }
                archiveViewerActivity.file_number_view.setText(size+"/"+archiveViewFragment.file_list_size);
            }

            @Override
            public boolean onLongClick(View p1)
            {
                longClickMethod(p1,archiveViewFragment.viewModel.mselecteditems.size());
                return true;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
        {
            if(Global.FILE_GRID_LAYOUT)
            {
                return new ViewHolder(new RecyclerViewLayoutGrid(context,show_file_path));
            }
            else{
                return new ViewHolder(new RecyclerViewLayoutList(context,show_file_path));
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder p1, int p2)
        {
            FilePOJO file=archiveViewFragment.filePOJO_list.get(p2);
            boolean selected=archiveViewFragment.viewModel.mselecteditems.get(p2,false);
            p1.view.setData(file,selected);
            p1.view.setSelected(selected);
        }

        @Override
        public Filter getFilter() {
            return file_name_filter;
        }

        private final Filter file_name_filter =new Filter () {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                return new Filter.FilterResults();
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults filterResults) {

                archiveViewFragment.filePOJO_list = new ArrayList<>();
                if ((constraint == null || constraint.length() == 0) && archiveViewFragment.viewModel.library_filter_path!=null) {
                    for (int i = 0; i < archiveViewFragment.totalFilePOJO_list_Size; ++i) {
                        FilePOJO filePOJO = archiveViewFragment.totalFilePOJO_list.get(i);
                        if (new File(filePOJO.getPath()).getParent().equals(archiveViewFragment.viewModel.library_filter_path)) {
                            archiveViewFragment.filePOJO_list.add(filePOJO);
                        }
                    }
                }
                else if((constraint == null || constraint.length() == 0) && archiveViewFragment.viewModel.library_filter_path==null)
                {
                    archiveViewFragment.filePOJO_list = archiveViewFragment.totalFilePOJO_list;
                }
                else if(archiveViewFragment.viewModel.library_filter_path==null){
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (int i = 0; i < archiveViewFragment.totalFilePOJO_list_Size; ++i) {
                        FilePOJO filePOJO = archiveViewFragment.totalFilePOJO_list.get(i);
                        if (filePOJO.getLowerName().contains(pattern)) {
                            archiveViewFragment.filePOJO_list.add(filePOJO);
                        }
                    }
                }
                else
                {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (int i = 0; i < archiveViewFragment.totalFilePOJO_list_Size; ++i) {
                        FilePOJO filePOJO = archiveViewFragment.totalFilePOJO_list.get(i);
                        if (filePOJO.getLowerName().contains(pattern) && new File(filePOJO.getPath()).getParent().equals(archiveViewFragment.viewModel.library_filter_path)) {
                            archiveViewFragment.filePOJO_list.add(filePOJO);
                        }
                    }
                }


                int t=archiveViewFragment.filePOJO_list.size();
                if(archiveViewFragment.viewModel.mselecteditems.size()>0)
                {
                    deselectAll();
                }
                else
                {
                    notifyDataSetChanged();
                }
                if(t>0)
                {
                    archiveViewFragment.recyclerView.setVisibility(View.VISIBLE);
                    archiveViewFragment.folder_empty.setVisibility(View.GONE);
                }
                archiveViewerActivity.file_number_view.setText(archiveViewFragment.viewModel.mselecteditems.size()+ "/" +t);
            }
        };

        @Override
        public int getItemCount()
        {
            return archiveViewFragment.filePOJO_list.size();
        }


        public void selectAll()
        {
            archiveViewFragment.viewModel.mselecteditems=new SparseBooleanArray();
            archiveViewFragment.viewModel.mselecteditemsFilePath=new SparseArray<>();
            int size=archiveViewFragment.filePOJO_list.size();

            for(int i=0;i<size;++i)
            {
                archiveViewFragment.viewModel.mselecteditems.put(i,true);
                archiveViewFragment.viewModel.mselecteditemsFilePath.put(i,archiveViewFragment.filePOJO_list.get(i).getPath());
            }

            int s=archiveViewFragment.viewModel.mselecteditems.size();

            archiveViewerActivity.file_number_view.setText(s+"/"+size);
            notifyDataSetChanged();
            archiveViewerActivity.bottom_toolbar.setVisibility(View.VISIBLE);
        }

        public void deselectAll()
        {
            archiveViewerActivity.DeselectAllAndAdjustToolbars(archiveViewFragment,archiveViewFragment.fileclickselected);
        }

        interface CardViewClickListener
        {
            void onClick(FilePOJO filePOJO);
            void onLongClick(FilePOJO filePOJO);
        }

        public void setCardViewClickListener(CardViewClickListener listener)
        {
            this.cardViewClickListener=listener;
        }

    }





}
