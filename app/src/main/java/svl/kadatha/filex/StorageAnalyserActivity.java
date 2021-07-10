package svl.kadatha.filex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class StorageAnalyserActivity extends  BaseActivity implements MediaMountReceiver.MediaMountListener, DeleteFileAlertDialog.OKButtonClickListener
{
    private Context context;
    private LocalBroadcastManager localBroadcastManager;
    private MediaMountReceiver mediaMountReceiver;
    public static FragmentManager FM;
    public static PackageManager PM;
    public TextView current_dir, file_number;
    private static final List<DetailFragmentCommunicationListener> DETAIL_FRAGMENT_COMMUNICATION_LISTENERS=new ArrayList<>();
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


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        mediaMountReceiver=new MediaMountReceiver();
        mediaMountReceiver.setMediaMountListener(this);
        IntentFilter intentFilter=new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addDataScheme("file");
        context.registerReceiver(mediaMountReceiver, intentFilter);

        localBroadcastManager= LocalBroadcastManager.getInstance(context);
        otherActivityBroadcastReceiver= new OtherActivityBroadcastReceiver();
        IntentFilter localBroadcastIntentFilter=new IntentFilter();
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION);
        localBroadcastIntentFilter.addAction(Global.LOCAL_BROADCAST_FILE_POJO_CACHE_CLEARED_ACTION);
        localBroadcastManager.registerReceiver(otherActivityBroadcastReceiver,localBroadcastIntentFilter);

        TinyDB tinyDB = new TinyDB(context);
        FM=getSupportFragmentManager();
        PM=getPackageManager();
        setContentView(R.layout.activity_storage_analyser);
        ImageButton back_btn=findViewById(R.id.storage_analyser_back_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        current_dir=findViewById(R.id.storage_analyser_current_dir_tv);
        file_number=findViewById(R.id.storage_analyser_file_number); //initiate here before adding fragment
        Intent intent=getIntent();
        if(intent!=null)
        {
            if(savedInstanceState==null)
            {
                createFileSelectorFragmentTransaction(Global.GET_INTERNAL_STORAGE_FILEPOJO_STORAGE_DIR());
            }
        }

        current_dir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileSelectorRecentDialog fileSelectorRecentDialog = new FileSelectorRecentDialog(FileSelectorRecentDialog.STORAGE_ANALYSER);
                fileSelectorRecentDialog.show(FM, "storage_analyser_recent_file_dialog");


            }
        });

        all_select=findViewById(R.id.storage_analyser_all_select);
        all_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StorageAnalyserDialog storageAnalyserDialog=(StorageAnalyserDialog) FM.findFragmentById(R.id.storage_analyser_container);
                if (storageAnalyserDialog.adapter == null) {
                    return;
                }

                if (storageAnalyserDialog.mselecteditems.size() < storageAnalyserDialog.filePOJO_list.size()) {
                    all_select.setImageResource(R.drawable.deselect_icon);
                    storageAnalyserDialog.selectAll();
                } else {
                    all_select.setImageResource(R.drawable.select_icon);
                    storageAnalyserDialog.deselectAll();
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

        floatingActionButton = findViewById(R.id.storage_analyser_floating_action_button_back);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onbackpressed(false);
            }
        });

        EquallyDistributedButtonsWithTextLayout tb_layout =new EquallyDistributedButtonsWithTextLayout(this,3,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
        int[] bottom_drawables ={R.drawable.refresh_icon,R.drawable.sort_icon,R.drawable.exit_icon};
        String [] titles=new String[]{getString(R.string.refresh),getString(R.string.sort),getString(R.string.exit)};
        tb_layout.setResourceImageDrawables(bottom_drawables,titles);

        bottom_toolbar=findViewById(R.id.storage_analyser_bottom_toolbar);
        bottom_toolbar.addView(tb_layout);
        Button refresh_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_1);
        refresh_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StorageAnalyserDialog storageAnalyserDialog=(StorageAnalyserDialog)FM.findFragmentById(R.id.storage_analyser_container);
                FM.beginTransaction().detach(storageAnalyserDialog).commit();
                FM.beginTransaction().attach(storageAnalyserDialog).commit();
            }
        });

        Button sort_btn=findViewById(R.id.toolbar_btn_2);
        sort_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StorageAnalyserSortDialog storageAnalyserSortDialog=new StorageAnalyserSortDialog();
                storageAnalyserSortDialog.show(FM,"storage_analyser_sort_dialog");
            }
        });

        Button exit_btn=bottom_toolbar.findViewById(R.id.toolbar_btn_3);
        exit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        tb_layout =new EquallyDistributedButtonsWithTextLayout(this,1,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT);
        int[] actionmode_drawables ={R.drawable.delete_icon};
        titles=new String[]{getString(R.string.delete)};
        tb_layout.setResourceImageDrawables(actionmode_drawables,titles);

        actionmode_toolbar=findViewById(R.id.storage_analyser_actionmode_toolbar);
        actionmode_toolbar.addView(tb_layout);
        Button delete_btn=actionmode_toolbar.findViewById(R.id.toolbar_btn_1);
        delete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final StorageAnalyserDialog storageAnalyserDialog= (StorageAnalyserDialog) FM.findFragmentById(R.id.storage_analyser_container);
                final ArrayList<String> files_selected_array=new ArrayList<>();
                int size = storageAnalyserDialog.mselecteditemsFilePath.size();
                for (int i = 0; i < size; ++i) {
                    int key = storageAnalyserDialog.mselecteditemsFilePath.keyAt(i);
                    files_selected_array.add(storageAnalyserDialog.mselecteditemsFilePath.get(key));
                }

                DeleteFileAlertDialog deleteFileAlertDialog = DeleteFileAlertDialog.getInstance(files_selected_array,storageAnalyserDialog.fileObjectType,storageAnalyserDialog.fileclickselected,true);
                deleteFileAlertDialog.show(FM, "delete_dialog");

            }
        });


        storage_filePOJO_list=getFilePOJO_list();
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

    public void clearCache()
    {
        Global.HASHMAP_FILE_POJO.clear();
        Global.HASHMAP_FILE_POJO_FILTERED.clear();
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_FILE_POJO_CACHE_CLEARED_ACTION,localBroadcastManager,ACTIVITY_NAME);
    }

    public void clearCache(String file_path, FileObjectType fileObjectType)
    {
        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(file_path),fileObjectType); //no need of broad cast here, as the method includes broadcast
    }

    public void broadcast_file_pojo_cache_removal(String file_path,FileObjectType fileObjectType)
    {
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_FILE_POJO_CACHE_CLEARED_ACTION,localBroadcastManager,ACTIVITY_NAME,file_path,fileObjectType);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(otherActivityBroadcastReceiver);
        context.unregisterReceiver(mediaMountReceiver);
    }

    @Override
    public void onBackPressed() {
        onbackpressed(true);
    }

    private void onbackpressed(boolean onBackPressed)
    {
        StorageAnalyserDialog storageAnalyserDialog= (StorageAnalyserDialog) FM.findFragmentById(R.id.storage_analyser_container);
        if(storageAnalyserDialog.mselecteditems.size()>0)
        {
            DeselectAllAndAdjustToolbars(storageAnalyserDialog,storageAnalyserDialog.fileclickselected);
        }
        else
        {
            switch (toolbar_shown)
            {

                case "bottom":
                    bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    bottom_toolbar.setVisibility(View.VISIBLE);
                    break;
                case "actionmode":
                    actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                    actionmode_toolbar.setVisibility(View.VISIBLE);
                    break;
            }


            if(FM.getBackStackEntryCount()>1)
            {
                FM.popBackStack();
                int frag=2, entry_count=FM.getBackStackEntryCount();
                storageAnalyserDialog= (StorageAnalyserDialog) FM.findFragmentByTag(FM.getBackStackEntryAt(entry_count-frag).getName());
                String tag=storageAnalyserDialog.getTag();

                while(tag !=null && !new File(tag).exists() && storageAnalyserDialog.currentUsbFile == null)
                {
                    FM.popBackStack();
                    ++frag;
                    if(frag>entry_count) break;
                    storageAnalyserDialog= (StorageAnalyserDialog) FM.findFragmentByTag(FM.getBackStackEntryAt(entry_count-frag).getName());
                    tag = storageAnalyserDialog.getTag();
                }

                /*
                if((entry_count-frag)<1)
                {

                }

                 */

            }
            else
            {
                /*


                 */

                if(onBackPressed)
                {
                    finish();
                }

            }

        }


    }


    private List<FilePOJO> getFilePOJO_list()
    {
        List<FilePOJO> filePOJOS = new ArrayList<>();
        for(FilePOJO filePOJO:Global.STORAGE_DIR)
        {
            if(filePOJO.getFileObjectType()==FileObjectType.FILE_TYPE)
            {
                filePOJOS.add(filePOJO);
            }
        }
        return filePOJOS;
    }

    public void createFileSelectorFragmentTransaction(FilePOJO filePOJO)
    {
        String fragment_tag;
        String existingFilePOJOkey="";
        StorageAnalyserDialog storageAnalyserDialog=(StorageAnalyserDialog) FM.findFragmentById(R.id.storage_analyser_container);
        if(storageAnalyserDialog!=null)
        {
            fragment_tag=storageAnalyserDialog.getTag();
            existingFilePOJOkey=storageAnalyserDialog.fileObjectType+fragment_tag;
            DeselectAllAndAdjustToolbars(storageAnalyserDialog,storageAnalyserDialog.getTag());
        }
        FileObjectType fileObjectType=filePOJO.getFileObjectType();
        String file_path=filePOJO.getPath();
        if(!(fileObjectType+file_path).equals(existingFilePOJOkey))
        {
            FM.beginTransaction().replace(R.id.storage_analyser_container,StorageAnalyserDialog.getInstance(fileObjectType),file_path).addToBackStack(file_path)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
        }

    }

    public void DeselectAllAndAdjustToolbars(StorageAnalyserDialog sad,String sad_tag)
    {


        bottom_toolbar.setVisibility(View.VISIBLE);
        actionmode_toolbar.setVisibility(View.GONE);
        toolbar_shown="bottom";
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
        final StorageAnalyserDialog storageAnalyserDialog= (StorageAnalyserDialog) FM.findFragmentById(R.id.storage_analyser_container);
        DeselectAllAndAdjustToolbars(storageAnalyserDialog,storageAnalyserDialog.fileclickselected);
    }

    private class OtherActivityBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            StorageAnalyserDialog storageAnalyserDialog = (StorageAnalyserDialog) FM.findFragmentById(R.id.storage_analyser_container);
            String activity_name=intent.getStringExtra("activity_name");
            String file_path=intent.getStringExtra("file_path");
            FileObjectType fileObjectType= (FileObjectType) intent.getSerializableExtra("fileObjectType");
            switch (intent.getAction()) {
                case Global.LOCAL_BROADCAST_DELETE_FILE_ACTION:
                    if (storageAnalyserDialog != null) storageAnalyserDialog.local_activity_delete = true;
                    break;
                case Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION:
                    if (storageAnalyserDialog != null) storageAnalyserDialog.modification_observed = true;
                    break;
                case Global.LOCAL_BROADCAST_FILE_POJO_CACHE_CLEARED_ACTION:
                    int size = DETAIL_FRAGMENT_COMMUNICATION_LISTENERS.size();
                    for(int i=0;i<size;++i)
                    {
                        DetailFragmentCommunicationListener listener=DETAIL_FRAGMENT_COMMUNICATION_LISTENERS.get(i);
                        if(listener!=null)
                        {
                            listener.onFragmentCacheClear(file_path,fileObjectType);
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onMediaMount(String action) {
        switch (action) {
            case "android.intent.action.MEDIA_MOUNTED":
                Global.STORAGE_DIR.clear();
                Global.STORAGE_DIR.addAll(new ArrayList<>(StorageUtil.getSdCardPaths(context, true)));
                Global.WORKOUT_AVAILABLE_SPACE();
                storage_filePOJO_list=getFilePOJO_list();

                if (recentDialogListener != null) {
                    recentDialogListener.onMediaAttachedAndRemoved();
                }

                break;
            case "android.intent.action.MEDIA_EJECT":
            case "android.intent.action.MEDIA_REMOVED":
            case "android.intent.action.MEDIA_BAD_REMOVAL":
                Global.STORAGE_DIR.clear();
                Global.STORAGE_DIR.addAll(new ArrayList<>(StorageUtil.getSdCardPaths(context, true)));
                Global.WORKOUT_AVAILABLE_SPACE();
                storage_filePOJO_list=getFilePOJO_list();
                if (recentDialogListener != null) {
                    recentDialogListener.onMediaAttachedAndRemoved();
                }
                FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(Global.EXTERNAL_STORAGE_PATH), FileObjectType.FILE_TYPE);
                StorageAnalyserDialog storageAnalyserDialog=(StorageAnalyserDialog) FM.findFragmentById(R.id.storage_analyser_container);
                if(storageAnalyserDialog!=null) storageAnalyserDialog.clearSelectionAndNotifyDataSetChanged();

                break;
        }
    }



    interface DetailFragmentCommunicationListener
    {
        void onFragmentCacheClear(String file_path, FileObjectType fileObjectType);
        void onSettingUsbFileRootNull();
    }

    public void addFragmentCommunicationListener(DetailFragmentCommunicationListener listener)
    {
        DETAIL_FRAGMENT_COMMUNICATION_LISTENERS.add(listener);
    }

    public void removeFragmentCommunicationListener(DetailFragmentCommunicationListener listener)
    {
        DETAIL_FRAGMENT_COMMUNICATION_LISTENERS.remove(listener);
    }

    interface RecentDialogListener
    {
        void onMediaAttachedAndRemoved();
    }


    private void print(String msg)
    {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}
