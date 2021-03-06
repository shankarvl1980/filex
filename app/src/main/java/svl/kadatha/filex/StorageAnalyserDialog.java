package svl.kadatha.filex;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StorageAnalyserDialog extends Fragment implements StorageAnalyserActivity.DetailFragmentCommunicationListener, FileModifyObserver.FileObserverListener
{
    static private final SimpleDateFormat SDF=new SimpleDateFormat("dd-MM-yyyy");
    private RecyclerView recycler_view;
    private TextView folder_empty_textview;
    private Context context;
    public StorageAnalyserAdapter adapter;
    public List<FilePOJO> filePOJO_list,totalFilePOJO_list;
    public int totalFilePOJO_list_Size;
    private StorageAnalyserActivity storageAnalyserActivity;
    public String fileclickselected;
    public FileObjectType fileObjectType;
    public UsbFile currentUsbFile;
    private ProgressBarFragment pbf_polling;
    public TextView folder_selected_textview;
    private List<FilePOJO> filePOJOS=new ArrayList<>(), filePOJOS_filtered=new ArrayList<>();
    private FileModifyObserver fileModifyObserver;
    public boolean local_activity_delete,modification_observed,cache_cleared;
    public boolean filled_filePOJOs;
    private Uri tree_uri;
    private String tree_uri_path="";
    private final int request_code=5208;
    public int file_list_size;
    public SparseBooleanArray mselecteditems=new SparseBooleanArray();
    public SparseArray<String> mselecteditemsFilePath=new SparseArray<>();
    public boolean is_toolbar_visible=true;

    private StorageAnalyserDialog(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        AsyncTaskStatus asyncTaskStatus = AsyncTaskStatus.NOT_YET_STARTED;
        context=getContext();
        fileclickselected=getTag();
        if(fileclickselected==null)
        {
            fileclickselected=Global.GET_INTERNAL_STORAGE_PATH_STORAGE_DIR();
        }
        Bundle bundle=getArguments();
        if(bundle!=null)
        {
            fileObjectType=(FileObjectType)bundle.getSerializable("fileObjectType");
        }

        if(fileObjectType==FileObjectType.ROOT_TYPE)
        {
            if(fileclickselected.startsWith(Global.GET_INTERNAL_STORAGE_PATH_STORAGE_DIR()) || (Global.EXTERNAL_STORAGE_PATH!=null && !Global.EXTERNAL_STORAGE_PATH.equals("") && fileclickselected.startsWith(Global.EXTERNAL_STORAGE_PATH)))
            {
                fileObjectType=FileObjectType.FILE_TYPE;
            }

        }
        else if(fileObjectType==FileObjectType.FILE_TYPE)
        {
            if(new File(Global.GET_INTERNAL_STORAGE_PATH_STORAGE_DIR()).getParent().startsWith(fileclickselected))
            {
                fileObjectType=FileObjectType.ROOT_TYPE;
            }
        }


        if(fileObjectType==FileObjectType.USB_TYPE)
        {
            if(MainActivity.usbFileRoot!=null)
            {
                try {
                    currentUsbFile=MainActivity.usbFileRoot.search(fileclickselected);

                } catch (IOException e) {

                }
            }
        }

        if (!Global.HASHMAP_FILE_POJO.containsKey(fileObjectType+fileclickselected)) {

            pbf_polling=ProgressBarFragment.getInstance();
            pbf_polling.show(StorageAnalyserActivity.FM,""); // don't show when archive view to avoid double pbf
            new Thread(new Runnable() {

                @Override
                public void run() {
                    filled_filePOJOs=false;
                    filled_filePOJOs=FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,fileclickselected,currentUsbFile,false);
                }
            }).start();
        }
        else
        {
            filePOJOS=Global.HASHMAP_FILE_POJO.get(fileObjectType+fileclickselected);
            filePOJOS_filtered=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+fileclickselected);
            filled_filePOJOs=true;
        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        context=getContext();
        if(cache_cleared)
        {
            cache_cleared=false;
            local_activity_delete=false;
            modification_observed=false;
            pbf_polling=ProgressBarFragment.getInstance();
            pbf_polling.show(StorageAnalyserActivity.FM,""); // don't show when archive view to avoid double pbf
            new Thread(new Runnable() {

                @Override
                public void run() {
                    filled_filePOJOs=false;
                    filled_filePOJOs=FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,fileclickselected,currentUsbFile,false);
                }
            }).start();

        }
        View v=inflater.inflate(R.layout.fragment_file_selector,container,false);
        storageAnalyserActivity=(StorageAnalyserActivity)context;
        storageAnalyserActivity.addFragmentCommunicationListener(this);

        fileModifyObserver=FileModifyObserver.getInstance(fileclickselected);
        fileModifyObserver.setFileObserverListener(this);

        TextView current_folder_label=v.findViewById(R.id.file_selector_current_folder_label);
        //current_folder_label.setText(R.string.current_folder_colon);
        folder_selected_textview = v.findViewById(R.id.file_selector_folder_selected);
        folder_selected_textview.setVisibility(View.GONE);
        recycler_view=v.findViewById(R.id.file_selectorRecyclerView);
        folder_empty_textview=v.findViewById(R.id.file_selector_folder_empty);

        LinearLayoutManager llm = new LinearLayoutManager(context);
        recycler_view.setLayoutManager(llm);
        recycler_view.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            int scroll_distance=0;
            final int threshold=5;

            public void onScrolled(RecyclerView rv, int dx, int dy)
            {
                super.onScrolled(rv,dx,dy);
                if(scroll_distance>threshold && is_toolbar_visible)
                {
                    switch (storageAnalyserActivity.toolbar_shown) {
                        case "bottom":
                            storageAnalyserActivity.bottom_toolbar.animate().translationY(storageAnalyserActivity.bottom_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                            break;
                        case "actionmode":
                            storageAnalyserActivity.actionmode_toolbar.animate().translationY(storageAnalyserActivity.actionmode_toolbar.getHeight()).setInterpolator(new AccelerateInterpolator(1));
                            break;

                    }

                    is_toolbar_visible=false;
                    scroll_distance=0;
                }
                else if(scroll_distance<-threshold && !is_toolbar_visible)
                {
                    switch (storageAnalyserActivity.toolbar_shown) {
                        case "bottom":
                            storageAnalyserActivity.bottom_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                            break;
                        case "actionmode":
                            storageAnalyserActivity.actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
                            break;

                    }
                    is_toolbar_visible=true;
                    scroll_distance=0;
                }

                if((is_toolbar_visible && dy>0) || (!is_toolbar_visible && dy<0))
                {
                    scroll_distance+=dy;
                }
            }
        });

        after_filledFilePojos_procedure();
        return v;

    }


    public static StorageAnalyserDialog getInstance(FileObjectType fileObjectType)
    {
        StorageAnalyserDialog storageAnalyserDialog=new StorageAnalyserDialog();
        Bundle bundle=new Bundle();
        bundle.putSerializable("fileObjectType",fileObjectType);
        storageAnalyserDialog.setArguments(bundle);
        return storageAnalyserDialog;
    }


    @Override
    public void onResume() {
        super.onResume();
        if(local_activity_delete)
        {
            cache_cleared=false;
            modification_observed=false;
            local_activity_delete=false;
            after_filledFilePojos_procedure();
        }
        else if(modification_observed && ArchiveDeletePasteFileService1.SERVICE_COMPLETED && ArchiveDeletePasteFileService2.SERVICE_COMPLETED && ArchiveDeletePasteFileService3.SERVICE_COMPLETED)
        {
            storageAnalyserActivity.DeselectAllAndAdjustToolbars(this,fileclickselected);
            cache_cleared=false;
            modification_observed=false;
            local_activity_delete=false;
            pbf_polling=ProgressBarFragment.getInstance();
            pbf_polling.show(StorageAnalyserActivity.FM,""); // don't show when archive view to avoid double pbf
            new Thread(new Runnable() {

                @Override
                public void run() {
                    filled_filePOJOs=false;
                    filled_filePOJOs=FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,fileclickselected,currentUsbFile,false);
                }
            }).start();
            FilePOJOUtil.UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(fileclickselected,fileObjectType);
            after_filledFilePojos_procedure();
        }
    }

    private void after_filledFilePojos_procedure()
    {
        final Handler handler_inter=new Handler();
        handler_inter.post(new Runnable() {
            @Override
            public void run() {
                if(filled_filePOJOs)
                {
                    long storage_space=0L;
                    for(Map.Entry<String,SpacePOJO> entry:Global.SPACE_ARRAY.entrySet())
                    {
                        if(!entry.getKey().equals("/") && fileclickselected.startsWith(entry.getKey()))
                        {
                            storage_space=entry.getValue().getTotalSpace();
                            break;
                        }
                    }

                    Iterate.FILL_FILE_SIZE(filePOJOS,storage_space);
                    totalFilePOJO_list=filePOJOS;
                    filePOJO_list=filePOJOS;
                    totalFilePOJO_list_Size=totalFilePOJO_list.size();
                    file_list_size=filePOJO_list.size();
                    storageAnalyserActivity.current_dir.setText(new File(fileclickselected).getName());
                    storageAnalyserActivity.file_number.setText(mselecteditems.size()+"/"+file_list_size);
                    Collections.sort(filePOJO_list,FileComparator.FilePOJOComparate(Global.STORAGE_ANALYSER_SORT,true));
                    adapter=new StorageAnalyserAdapter();
                    set_adapter();
                    if(pbf_polling!=null && pbf_polling.getDialog()!=null)
                    {
                        pbf_polling.dismissAllowingStateLoss();
                    }

                    handler_inter.removeCallbacks(this);
                }
                else
                {
                    handler_inter.postDelayed(this,50);
                }
            }
        });


    }


    @Override
    public void onStop() {
        super.onStop();
        fileModifyObserver.startWatching();
        if(pbf_polling!=null && pbf_polling.getDialog()!=null)
        {
            pbf_polling.dismissAllowingStateLoss();
        }
    }


    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        fileModifyObserver.stopWatching();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        storageAnalyserActivity.removeFragmentCommunicationListener(this);
    }

    public void notifyDataSetChanged()
    {
        after_filledFilePojos_procedure();
    }

    @Override
    public void onFragmentCacheClear() {
        cache_cleared=true;
    }

    @Override
    public void onSettingUsbFileRootNull() {
        currentUsbFile=null;
    }


    @Override
    public void onFileModified() {
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_MODIFICATION_OBSERVED_ACTION,LocalBroadcastManager.getInstance(context),StorageAnalyserActivity.ACTIVITY_NAME);
    }



    private void set_adapter()
    {
        recycler_view.setAdapter(adapter);
        if(file_list_size==0)
        {
            recycler_view.setVisibility(View.GONE);
            folder_empty_textview.setVisibility(View.VISIBLE);
        }
        else
        {
            recycler_view.setVisibility(View.VISIBLE);
            folder_empty_textview.setVisibility(View.GONE);
        }
    }

    public class StorageAnalyserAdapter extends RecyclerView.Adapter<StorageAnalyserAdapter.ViewHolder>
    {
        StorageAnalyserDialog sad=(StorageAnalyserDialog) StorageAnalyserActivity.FM.findFragmentById(R.id.storage_analyser_container);
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup p1, int p2)
        {
            // TODO: Implement this method
            return new ViewHolder(new StorageAnalyserRecyclerViewLayout(context,false));
        }

        @Override
        public void onBindViewHolder(final StorageAnalyserAdapter.ViewHolder p1, int p2)
        {
            // TODO: Implement this method
            FilePOJO file=filePOJO_list.get(p2);
            boolean selected=mselecteditems.get(p2,false);
            p1.v.setData(file,selected);
            p1.v.setSelected(selected);
        }

        @Override
        public int getItemCount()
        {
            // TODO: Implement this method
            return filePOJO_list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener
        {
            final StorageAnalyserRecyclerViewLayout v;
            FileObjectType fileObjectType;
            int pos;
            ViewHolder(StorageAnalyserRecyclerViewLayout v)
            {
                super(v);
                this.v=v;
                v.setOnClickListener(this);
                v.setOnLongClickListener(this);
            }

            @Override
            public void onClick(View p1)
            {
                pos=getBindingAdapterPosition();
                int size=mselecteditems.size();
                if(size>0)
                {
                    longClickMethod(p1,size);
                }
                else
                {
                    int pos=getBindingAdapterPosition();
                    FilePOJO filePOJO=filePOJO_list.get(pos);
                    fileObjectType=filePOJO.getFileObjectType();
                    if(filePOJO.getIsDirectory())
                    {
                        storageAnalyserActivity.createFileSelectorFragmentTransaction(filePOJO);
                    }
                    else
                    {
                        file_open_intent_despatch(filePOJO.getPath(),filePOJO.getFileObjectType(),filePOJO.getName());
                    }
                    FileSelectorRecentDialog.ADD_FILE_POJO_TO_RECENT(filePOJO);
                }
            }

            @Override
            public boolean onLongClick(View view) {
                longClickMethod(view,mselecteditems.size());
                return true;
            }

            private void longClickMethod (View v, int size)
            {
                pos=getBindingAdapterPosition();
                if(mselecteditems.get(pos,false))
                {
                    mselecteditems.delete(pos);
                    mselecteditemsFilePath.delete(pos);
                    v.setSelected(false);
                    ((StorageAnalyserRecyclerViewLayout)v).set_selected(false);
                    --size;

                    if(size==1)
                    {
                        //    mainActivity.rename.setEnabled(true);
                        //  mainActivity.rename.setAlpha(Global.ENABLE_ALFA);

                        onLongClickAdjustToolbars();
                    }
                    else if(size>1)
                    {
                        //mainActivity.rename.setEnabled(false);
                        //mainActivity.rename.setAlpha(Global.DISABLE_ALFA);

                        onLongClickAdjustToolbars();
                    }

                    if(size==0)
                    {
                        storageAnalyserActivity.DeselectAllAndAdjustToolbars(sad,sad.fileclickselected);
                    }
                }
                else
                {
                    mselecteditems.put(pos,true);
                    mselecteditemsFilePath.put(pos,filePOJO_list.get(pos).getPath());
                    v.setSelected(true);
                    ((StorageAnalyserRecyclerViewLayout)v).set_selected(true);
                    ++size;

                    if(size==1)
                    {
                        //mainActivity.rename.setEnabled(true);
                        //mainActivity.rename.setAlpha(Global.ENABLE_ALFA);
                    }
                    else if(size>1)
                    {
                        //mainActivity.rename.setEnabled(false);
                        //mainActivity.rename.setAlpha(Global.DISABLE_ALFA);
                    }

                    if(size==file_list_size)
                    {
                        //mainActivity.all_select.setImageResource(R.drawable.deselect_icon);
                    }

                    onLongClickAdjustToolbars();
                }
                storageAnalyserActivity.file_number.setText(size+"/"+file_list_size);
            }
        }


    }

    public void selectAll()
    {
        mselecteditems=new SparseBooleanArray();
        mselecteditemsFilePath=new SparseArray<>();
        int size=filePOJO_list.size();

        for(int i=0;i<size;++i)
        {
            mselecteditems.put(i,true);
            mselecteditemsFilePath.put(i,filePOJO_list.get(i).getPath());
        }

        int s=mselecteditems.size();
        storageAnalyserActivity.file_number.setText(s+"/"+size);
        notifyDataSetChanged();

        onLongClickAdjustToolbars();
    }

    public void deselectAll()
    {
        storageAnalyserActivity.DeselectAllAndAdjustToolbars(this,fileclickselected);
    }


    private void onLongClickAdjustToolbars()
    {
        storageAnalyserActivity.bottom_toolbar.setVisibility(View.GONE);
        storageAnalyserActivity.actionmode_toolbar.setVisibility(View.VISIBLE);

        storageAnalyserActivity.toolbar_shown="actionmode";
        storageAnalyserActivity.actionmode_toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(1));
        is_toolbar_visible=true;
    }

    public void seekSAFPermission()
    {
        storageAnalyserActivity.clear_cache=false;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, request_code);
    }

    @Override
    public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData)
    {
        if (requestCode == this.request_code && resultCode== Activity.RESULT_OK)
        {
            Uri treeUri;
            treeUri = resultData.getData();
            Global.ON_REQUEST_URI_PERMISSION(context,treeUri);
        }
        else
        {
            print(getString(R.string.permission_not_granted));
        }

    }

    private void file_open_intent_despatch(final String file_path, final FileObjectType fileObjectType, String file_name)
    {
        int idx=file_name.lastIndexOf(".");
        String file_ext="";
        if(idx!=-1)
        {
            file_ext=file_name.substring(idx+1);
        }

        if(file_ext.equals("") || !Global.CHECK_APPS_FOR_RECOGNISED_FILE_EXT(context,file_ext))
        {
            FileTypeSelectDialog fileTypeSelectFragment=new FileTypeSelectDialog();
            fileTypeSelectFragment.setFileTypeSelectListener(new FileTypeSelectDialog.FileTypeSelectListener()
            {
                public void onSelectType(String mime_type)
                {

                    if(fileObjectType==FileObjectType.USB_TYPE)
                    {
                        if(check_availability_USB_SAF_permission(file_path,fileObjectType))
                        {
                            FileIntentDispatch.openUri(context,file_path,mime_type,false,false,fileObjectType,tree_uri,tree_uri_path);
                        }
                    }
                    else if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
                    {
                        FileIntentDispatch.openFile(context,file_path,mime_type,false,false,fileObjectType);
                    }

                }
            });
            fileTypeSelectFragment.show(StorageAnalyserActivity.FM,"");
        }
        else
        {
            if(fileObjectType==FileObjectType.USB_TYPE)
            {
                if(check_availability_USB_SAF_permission(file_path,fileObjectType))
                {
                    FileIntentDispatch.openUri(context,file_path,"", file_ext.matches("(?i)zip"),false,fileObjectType,tree_uri,tree_uri_path);
                }
            }
            else if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
            {
                FileIntentDispatch.openFile(context,file_path,"",file_ext.matches("(?i)zip"),false,fileObjectType);
            }
        }
    }

    public void clearSelectionAndNotifyDataSetChanged()
    {
        mselecteditems=new SparseBooleanArray();
        mselecteditemsFilePath=new SparseArray<>();
        if(adapter!=null)
        {
            adapter.notifyDataSetChanged();
            file_list_size=filePOJO_list.size();
            storageAnalyserActivity.file_number.setText(mselecteditems.size()+"/"+file_list_size);
            totalFilePOJO_list_Size=totalFilePOJO_list.size();

            if(file_list_size==0)
            {
                recycler_view.setVisibility(View.GONE);
                folder_empty_textview.setVisibility(View.VISIBLE);
            }
            else
            {
                recycler_view.setVisibility(View.VISIBLE);
                folder_empty_textview.setVisibility(View.GONE);
            }
        }

    }


    public void clear_cache_and_refresh()
    {
        mselecteditems=new SparseBooleanArray();
        mselecteditemsFilePath=new SparseArray<>();
        storageAnalyserActivity.clearCache();
        modification_observed=true;
        Global.WORKOUT_AVAILABLE_SPACE();
    }


    private boolean check_availability_USB_SAF_permission(String file_path,FileObjectType fileObjectType)
    {
        if(fileObjectType==FileObjectType.USB_TYPE && MainActivity.usbFileRoot==null)
        {
            return false;
        }
        UriPOJO uriPOJO=Global.CHECK_AVAILABILITY_URI_PERMISSION(file_path,fileObjectType);
        if(uriPOJO!=null)
        {
            tree_uri_path=uriPOJO.get_path();
            tree_uri=uriPOJO.get_uri();
        }

        if(tree_uri_path.equals(""))
        {
            SAFPermissionHelperDialog safpermissionhelper=new SAFPermissionHelperDialog(true);
            safpermissionhelper.set_safpermissionhelperlistener(new SAFPermissionHelperDialog.SafPermissionHelperListener()
            {
                public void onOKBtnClicked()
                {
                    seekSAFPermission();
                }

                public void onCancelBtnClicked()
                {

                }
            });
            safpermissionhelper.show(StorageAnalyserActivity.FM,"saf_permission_dialog");
            return false;
        }
        else
        {
            return true;
        }
    }



    private void print(String msg)
    {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }

}



