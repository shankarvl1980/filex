package svl.kadatha.filex;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FileSelectorActivity extends BaseActivity implements MediaMountReceiver.MediaMountListener
{
    private Context context;
    public static FragmentManager FM;
    public static final int FOLDER_SELECT_REQUEST_CODE=1564;
    public static final int MOVE_COPY_REQUEST_CODE=351;
    public static final int PICK_FILE_REQUEST_CODE=0;
    private final List<DetailFragmentCommunicationListener> detailFragmentCommunicationListeners=new ArrayList<>();
    public boolean clear_cache;
    private OtherActivityBroadcastReceiver otherActivityBroadcastReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private MediaMountReceiver mediaMountReceiver;
    private PopupWindow listPopWindow;
    public List<FilePOJO> storage_filePOJO_list;
    public TextView file_number;
    public static final String ACTION_SOUGHT="action_sought";
    public int action_sought_request_code;
    private Bundle bundle;
    static LinkedList<FilePOJO> RECENTS=new LinkedList<>();
    private RecentDialogListener recentDialogListener;
    public FloatingActionButton floatingActionButton;
    public static final String ACTIVITY_NAME="FILE_SELECTOR_ACTIVITY";

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
        setContentView(R.layout.activity_file_selector);
        Toolbar toolbar=findViewById(R.id.activity_file_selector_toolbar);
        file_number=findViewById(R.id.file_selector_file_number); //initiate here before adding fragment
        Intent intent=getIntent();
        if(intent!=null)
        {
            action_sought_request_code=intent.getIntExtra(ACTION_SOUGHT,PICK_FILE_REQUEST_CODE);
            bundle=intent.getBundleExtra("bundle");
            if(savedInstanceState==null)
            {
                createFileSelectorFragmentTransaction(Global.GET_INTERNAL_STORAGE_FILEPOJO_STORAGE_DIR());
            }
        }

        TextView heading = findViewById(R.id.file_selector_heading);
        if(action_sought_request_code==FOLDER_SELECT_REQUEST_CODE) heading.setText(getString(R.string.choose_folder));
        else heading.setText(getString(R.string.application_name));
        //ImageButton directory_btn = findViewById(R.id.file_selector_directory_btn);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileSelectorRecentDialog fileSelectorRecentDialog = new FileSelectorRecentDialog(FileSelectorRecentDialog.FILE_SELECTOR);
                recentDialogListener=fileSelectorRecentDialog;
                fileSelectorRecentDialog.show(FM, "file_selector_recent_file_dialog");
            }
        });

        storage_filePOJO_list=getFilePOJO_list();
        listPopWindow=new PopupWindow(context);
        ListView listView=new ListView(context);
        PopupWindowAdapater popupWindowAdapater = new PopupWindowAdapater(context, storage_filePOJO_list);
        listView.setAdapter(popupWindowAdapater);
        listPopWindow.setContentView(listView);
        listPopWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.list_popupwindow_width));
        listPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        listPopWindow.setFocusable(true);
        listPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.list_popup_background));


        floatingActionButton = findViewById(R.id.file_selector_floating_action_button_back);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onbackpressed(false);
            }
        });

        ViewGroup buttons_layout = findViewById(R.id.file_selector_button_layout);
        buttons_layout.addView(new EquallyDistributedDialogButtonsLayout(context,2,Global.SCREEN_WIDTH,Global.SCREEN_HEIGHT));
        Button okbutton = buttons_layout.findViewById(R.id.first_button);
        okbutton.setText(R.string.ok);
        Button cancelbutton = buttons_layout.findViewById(R.id.second_button);
        cancelbutton.setText(R.string.cancel);
        okbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {

                FileSelectorDialog fileSelectorDialog=(FileSelectorDialog) FM.findFragmentById(R.id.file_selector_container);
                switch (action_sought_request_code) {
                    case FOLDER_SELECT_REQUEST_CODE:
                        if (fileSelectorDialog.fileclickselected == null) {
                            setResult(Activity.RESULT_CANCELED);
                        } else {
                            Intent intent = new Intent();
                            intent.putExtra("folderclickselected", fileSelectorDialog.folder_selected_textview.getText().toString());
                            intent.putExtra("destFileObjectType", fileSelectorDialog.fileObjectType);
                            setResult(Activity.RESULT_OK, intent);

                        }
                        finish();
                        break;
                    case MOVE_COPY_REQUEST_CODE:
                        if (fileSelectorDialog.fileclickselected == null) {
                            setResult(Activity.RESULT_CANCELED);
                        } else {
                            bundle.putString("dest_folder",fileSelectorDialog.folder_selected_textview.getText().toString());
                            bundle.putSerializable("destFileObjectType",fileSelectorDialog.fileObjectType);
                            Intent intent = new Intent();
                            intent.putExtra("bundle", bundle);
                            setResult(Activity.RESULT_OK, intent);
                        }

                        finish();
                        break;
                }

            }
        });


        cancelbutton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                finish();
            }
        });

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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("clear_cache",clear_cache);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        clear_cache=savedInstanceState.getBoolean("clear_cache");
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

        if(FM.getBackStackEntryCount()>1)
        {
            FM.popBackStack();
            int frag=2, entry_count=FM.getBackStackEntryCount();
            FileSelectorDialog fileSelectorDialog= (FileSelectorDialog) FM.findFragmentByTag(FM.getBackStackEntryAt(entry_count-frag).getName());
            String tag=fileSelectorDialog.getTag();

            while(tag !=null && !new File(tag).exists() && fileSelectorDialog.currentUsbFile == null)
            {
                FM.popBackStack();
                ++frag;
                if(frag>entry_count) break;
                fileSelectorDialog= (FileSelectorDialog) FM.findFragmentByTag(FM.getBackStackEntryAt(entry_count-frag).getName());
                tag = fileSelectorDialog.getTag();
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
        FileSelectorDialog fileSelectorDialog=(FileSelectorDialog) FM.findFragmentById(R.id.file_selector_container);
        if(fileSelectorDialog!=null)
        {
            fragment_tag=fileSelectorDialog.getTag();
            existingFilePOJOkey=fileSelectorDialog.fileObjectType+fragment_tag;
        }
        FileObjectType fileObjectType=filePOJO.getFileObjectType();
        String file_path=filePOJO.getPath();
        if(!(fileObjectType+file_path).equals(existingFilePOJOkey))
        {
            FileSelectorDialog ff=FileSelectorDialog.getInstance(fileObjectType);
            FM.beginTransaction().replace(R.id.file_selector_container,ff,file_path).addToBackStack(file_path)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
        }

    }

    private class OtherActivityBroadcastReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent) {

            FileSelectorDialog fileSelectorDialog = (FileSelectorDialog) FM.findFragmentById(R.id.file_selector_container);
            String activity_name=intent.getStringExtra("activity_name");
            switch (intent.getAction()) {
                case Global.LOCAL_BROADCAST_DELETE_FILE_ACTION:
                    if (fileSelectorDialog != null) fileSelectorDialog.local_activity_delete = true;
                    break;
                case Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION:
                    if (fileSelectorDialog != null) fileSelectorDialog.modification_observed = true;
                    break;
                case Global.LOCAL_BROADCAST_FILE_POJO_CACHE_CLEARED_ACTION:
                    int size = detailFragmentCommunicationListeners.size();
                    for(int i=0;i<size;++i)
                    {
                        DetailFragmentCommunicationListener listener=detailFragmentCommunicationListeners.get(i);
                        if(listener!=null)
                        {
                            listener.onFragmentCacheClear();
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
                FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Global.EXTERNAL_STORAGE_PATH, FileObjectType.FILE_TYPE);
                FileSelectorDialog fileSelectorDialog=(FileSelectorDialog)FM.findFragmentById(R.id.file_selector_container);
                if(fileSelectorDialog!=null) fileSelectorDialog.clearSelectionAndNotifyDataSetChanged();
                break;
        }
    }

    public static class PopupWindowAdapater extends ArrayAdapter<FilePOJO>
    {
        final Context context;
        final List<FilePOJO> list;
        PopupWindowAdapater(Context context, List<FilePOJO> list)
        {
            super(context,R.layout.list_popupwindow_layout,list);
            this.context=context;
            this.list=list;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable final View convertView, @NonNull ViewGroup parent) {
            View v;
            PopupWindowAdapater.ViewHolder vh;
            if(convertView==null)
            {
                v= LayoutInflater.from(context).inflate(R.layout.list_popupwindow_layout,parent,false);
                vh=new PopupWindowAdapater.ViewHolder();
                vh.imageView=v.findViewById(R.id.list_popupwindow_layout_iv);
                vh.textView=v.findViewById(R.id.list_popupwindow_tv);
                v.setTag(vh);
            }
            else
            {
                v=convertView;
                vh= (PopupWindowAdapater.ViewHolder) convertView.getTag();
            }
            final FilePOJO filePOJO=list.get(position);
            FileObjectType fileObjectType=filePOJO.getFileObjectType();
            if(fileObjectType==FileObjectType.FILE_TYPE)
            {
                if(Global.INTERNAL_STORAGE_PATH.contains(filePOJO.getPath()))
                {
                    vh.imageView.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.device_icon));
                }
                else
                {
                    vh.imageView.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.sdcard_icon));
                }
                vh.textView.setText(filePOJO.getName());
            }
            else if(fileObjectType==FileObjectType.USB_TYPE)
            {
                vh.imageView.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.sdcard_icon));
                vh.textView.setText(DetailFragment.USB_FILE_PREFIX+filePOJO.getName());
            }
            else if(fileObjectType==FileObjectType.ROOT_TYPE)
            {
                vh.imageView.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.device_icon));
                vh.textView.setText(R.string.root_directory);
            }

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((FileSelectorActivity)context).createFileSelectorFragmentTransaction(filePOJO);
                    ((FileSelectorActivity)context).listPopWindow.dismiss();
                }
            });
            return v;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        public static class ViewHolder
        {
            ImageView imageView;
            TextView textView;
        }

    }


    interface DetailFragmentCommunicationListener
    {
        void onFragmentCacheClear();
        void onSettingUsbFileRootNull();
    }

    public void addFragmentCommunicationListener(DetailFragmentCommunicationListener listener)
    {
        detailFragmentCommunicationListeners.add(listener);
    }

    public void removeFragmentCommunicationListener(DetailFragmentCommunicationListener listener)
    {
        detailFragmentCommunicationListeners.remove(listener);
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
