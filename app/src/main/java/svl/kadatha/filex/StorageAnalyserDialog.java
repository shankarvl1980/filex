package svl.kadatha.filex;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.jahnen.libaums.core.fs.UsbFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class StorageAnalyserDialog extends Fragment implements StorageAnalyserActivity.DetailFragmentCommunicationListener, FileModifyObserver.FileObserverListener
{
    private RecyclerView recycler_view;
    private TextView folder_empty_textview;
    private Context context;
    public StorageAnalyserAdapter adapter;
    public List<FilePOJO> filePOJO_list,totalFilePOJO_list;
    public int totalFilePOJO_list_Size;
    public StorageAnalyserActivity storageAnalyserActivity;
    public String fileclickselected;
    public FileObjectType fileObjectType;
    public UsbFile currentUsbFile;
    //private CancelableProgressBarDialog cancelableProgressBarDialog;
    public TextView folder_selected_textview;
    //private List<FilePOJO> filePOJOS=new ArrayList<>(), filePOJOS_filtered=new ArrayList<>();
    private FileModifyObserver fileModifyObserver;
    public boolean local_activity_delete,modification_observed,cache_cleared;
    public boolean filled_filePOJOs;
    private Uri tree_uri;
    private String tree_uri_path="";
    private final int request_code=5208;
    public int file_list_size;
    //public SparseBooleanArray mselecteditems=new SparseBooleanArray();
    //public SparseArray<String> mselecteditemsFilePath=new SparseArray<>();
    public boolean is_toolbar_visible=true, filled_file_size;
    //private FillSizeAsyncTask fillSizeAsyncTask;
    private FilePOJO clicked_filepojo;
    private AsyncTaskStatus asynctask_status;
    //private AsyncTaskFilePopulate asyncTaskFilePopulate;
    private static final String FILE_TYPE_REQUEST_CODE="storage_analyser_file_type_request_code";
    private static final String CANCEL_PROGRESS_REQUEST_CODE="storage_anayliser_cancel_progress_request_code";
    public FrameLayout progress_bar;
    public FilePOJOViewModel viewModel;
    private final static String SAF_PERMISSION_REQUEST_CODE="storage_analyser_dialog_saf_permission_request_code";


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context=context;
        storageAnalyserActivity=(StorageAnalyserActivity)context;
        storageAnalyserActivity.addFragmentCommunicationListener(this);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        storageAnalyserActivity.removeFragmentCommunicationListener(this);
        storageAnalyserActivity=null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        asynctask_status = AsyncTaskStatus.NOT_YET_STARTED;

        fileclickselected=getTag();
        if(fileclickselected==null)
        {
            fileclickselected=Global.INTERNAL_PRIMARY_STORAGE_PATH;
        }

        Bundle bundle=getArguments();
        if(bundle!=null)
        {
            fileObjectType=(FileObjectType)bundle.getSerializable("fileObjectType");
        }

        if(fileObjectType==FileObjectType.ROOT_TYPE)
        {
            if(FileUtil.isFromInternal(FileObjectType.FILE_TYPE,fileclickselected) || (Global.EXTERNAL_STORAGE_PATH!=null && !Global.EXTERNAL_STORAGE_PATH.equals("") && Global.IS_CHILD_FILE(fileclickselected,Global.EXTERNAL_STORAGE_PATH)))
            {
                fileObjectType=FileObjectType.FILE_TYPE;
            }
        }
        else if(fileObjectType==FileObjectType.FILE_TYPE)
        {
            for(String path:Global.INTERNAL_STORAGE_PATH_LIST)
            {
                if(Global.IS_CHILD_FILE(new File(path).getParent(),fileclickselected))
                {
                    fileObjectType=FileObjectType.ROOT_TYPE;
                    break;
                }
            }
        }


        if(fileObjectType==FileObjectType.USB_TYPE)
        {
            if(MainActivity.usbFileRoot!=null)
            {
                try {
                    currentUsbFile=MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(fileclickselected));

                } catch (IOException e) {

                }
            }
        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        /*
        if(cache_cleared)
        {
            cache_cleared=false;
            local_activity_delete=false;
            modification_observed=false;
            if(asynctask_status!=AsyncTaskStatus.STARTED)
            {
                asyncTaskFilePopulate=new AsyncTaskFilePopulate();
                asyncTaskFilePopulate.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

        }

         */
        View v=inflater.inflate(R.layout.fragment_file_selector,container,false);
        fileModifyObserver=FileModifyObserver.getInstance(fileclickselected);
        fileModifyObserver.setFileObserverListener(this);

        folder_selected_textview = v.findViewById(R.id.file_selector_folder_selected);
        folder_selected_textview.setVisibility(View.GONE);
        recycler_view=v.findViewById(R.id.file_selectorRecyclerView);
        folder_empty_textview=v.findViewById(R.id.file_selector_folder_empty);
        progress_bar=v.findViewById(R.id.file_selector_progressbar);

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

        viewModel=new ViewModelProvider(this).get(FilePOJOViewModel.class);
        if (!Global.HASHMAP_FILE_POJO.containsKey(fileObjectType+fileclickselected)) {
            if(asynctask_status!=AsyncTaskStatus.STARTED)
            {
                //asyncTaskFilePopulate=new AsyncTaskFilePopulate();
                //asyncTaskFilePopulate.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                //cancelableProgressBarDialog=CancelableProgressBarDialog.getInstance(CANCEL_PROGRESS_REQUEST_CODE);
                //cancelableProgressBarDialog.set_title(getString(R.string.analysing));
                //cancelableProgressBarDialog.show(storageAnalyserActivity.fm, "");
                viewModel.populateFilePOJO(fileObjectType,fileclickselected,currentUsbFile,false,true);
            }

        }
        else
        {
            viewModel.filePOJOS=Global.HASHMAP_FILE_POJO.get(fileObjectType+fileclickselected);
            viewModel.filePOJOS_filtered=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+fileclickselected);
            if(!viewModel.filled_size)
            {
                viewModel.fill_file_size(fileObjectType,fileclickselected,currentUsbFile,false);
            }
            else
            {
                after_filledFilePojos_procedure();
            }
            filled_filePOJOs=true;

        }

        viewModel.isFinished.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean)
                {
                    after_filledFilePojos_procedure();
                }
            }
        });

        viewModel.mutable_file_count.observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                storageAnalyserActivity.file_number.setText(viewModel.mselecteditems.size()+"/"+integer);
            }
        });

        ((AppCompatActivity)context).getSupportFragmentManager().setFragmentResultListener(SAF_PERMISSION_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(SAF_PERMISSION_REQUEST_CODE))
                {
                    tree_uri=result.getParcelable("tree_uri");
                    tree_uri_path=result.getString("tree_uri_path");

                }

            }
        });

        /*
        storageAnalyserActivity.fm.setFragmentResultListener(CANCEL_PROGRESS_REQUEST_CODE, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if(requestKey.equals(CANCEL_PROGRESS_REQUEST_CODE))
                {
                    viewModel.cancel(true);
                    if(cancelableProgressBarDialog!=null && cancelableProgressBarDialog.getDialog()!=null)
                    {
                        cancelableProgressBarDialog.dismissAllowingStateLoss();
                    }
                    storageAnalyserActivity.onClickCancel();
                }
            }
        });

         */

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

        if(modification_observed)// && ArchiveDeletePasteFileService1.SERVICE_COMPLETED && ArchiveDeletePasteFileService2.SERVICE_COMPLETED && ArchiveDeletePasteFileService3.SERVICE_COMPLETED)
        {
            storageAnalyserActivity.DeselectAllAndAdjustToolbars(this,fileclickselected);
            cache_cleared=false;
            modification_observed=false;
            local_activity_delete=false;

            if(asynctask_status!=AsyncTaskStatus.STARTED)
            {
                //asyncTaskFilePopulate=new AsyncTaskFilePopulate();
                //asyncTaskFilePopulate.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                //cancelableProgressBarDialog=CancelableProgressBarDialog.getInstance(CANCEL_PROGRESS_REQUEST_CODE);
                //cancelableProgressBarDialog.set_title(getString(R.string.analysing));
                //cancelableProgressBarDialog.show(storageAnalyserActivity.fm, "");
                progress_bar.setVisibility(View.VISIBLE);
                viewModel.isFinished.setValue(false);
                viewModel.filled_size=false;
                viewModel.populateFilePOJO(fileObjectType,fileclickselected,currentUsbFile,false,true);
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    FilePOJOUtil.UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(fileclickselected,fileObjectType);
                }
            }).start();
        }
        else if(local_activity_delete)
        {
            cache_cleared=false;
            modification_observed=false;
            local_activity_delete=false;
            //after_filledFilePojos_procedure();
            totalFilePOJO_list=viewModel.filePOJOS;
            filePOJO_list=viewModel.filePOJOS;
            totalFilePOJO_list_Size=totalFilePOJO_list.size();
            file_list_size=filePOJO_list.size();
            if(storageAnalyserActivity!=null)
            {
                storageAnalyserActivity.current_dir.setText(new File(fileclickselected).getName());
                storageAnalyserActivity.file_number.setText(viewModel.mselecteditems.size()+"/"+file_list_size);
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void after_filledFilePojos_procedure()
    {
        totalFilePOJO_list=viewModel.filePOJOS;
        filePOJO_list=viewModel.filePOJOS;
        totalFilePOJO_list_Size=totalFilePOJO_list.size();
        file_list_size=filePOJO_list.size();
        if(storageAnalyserActivity!=null)
        {
            storageAnalyserActivity.current_dir.setText(new File(fileclickselected).getName());
            storageAnalyserActivity.file_number.setText(viewModel.mselecteditems.size()+"/"+file_list_size);
        }

        Collections.sort(filePOJO_list,FileComparator.FilePOJOComparate(Global.STORAGE_ANALYSER_SORT,true));
        adapter=new StorageAnalyserAdapter();
        set_adapter();
        progress_bar.setVisibility(View.GONE);
        /*
        if(cancelableProgressBarDialog!=null && cancelableProgressBarDialog.getDialog()!=null)
        {
            cancelableProgressBarDialog.dismissAllowingStateLoss();
        }

         */
    }

/*
    private void after_filledFilePojos_procedure()
    {
        final Handler handler_inter=new Handler();
        handler_inter.post(new Runnable() {
            @Override
            public void run() {
                if(filled_filePOJOs)
                {
                    filled_file_size=false;
                    long storage_space=0L;
                    String key=fileObjectType+fileclickselected;
                    for(Map.Entry<String,SpacePOJO> entry:Global.SPACE_ARRAY.entrySet())
                    {
                        if(Global.IS_CHILD_FILE(key,entry.getKey()))
                        {
                            storage_space=entry.getValue().getTotalSpace();
                            break;
                        }
                    }
                    final long final_storage_space = storage_space;
                    fillSizeAsyncTask=new FillSizeAsyncTask(filePOJOS,final_storage_space);
                    fillSizeAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    fill_size_filepojos();
                    handler_inter.removeCallbacks(this);
                }
                else
                {
                    handler_inter.postDelayed(this,50);
                }
            }
        });
    }

 */

    /*
    public void fill_size_filepojos()
    {
        final Handler handler=new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(filled_file_size)
                {
                    totalFilePOJO_list=filePOJOS;
                    filePOJO_list=filePOJOS;
                    totalFilePOJO_list_Size=totalFilePOJO_list.size();
                    file_list_size=filePOJO_list.size();
                    if(storageAnalyserActivity!=null)
                    {
                        storageAnalyserActivity.current_dir.setText(new File(fileclickselected).getName());
                        storageAnalyserActivity.file_number.setText(mselecteditems.size()+"/"+file_list_size);
                    }

                    Collections.sort(filePOJO_list,FileComparator.FilePOJOComparate(Global.STORAGE_ANALYSER_SORT,true));
                    adapter=new StorageAnalyserAdapter();
                    set_adapter();

                    if(cancelableProgressBarDialog!=null && cancelableProgressBarDialog.getDialog()!=null)
                    {
                        cancelableProgressBarDialog.dismissAllowingStateLoss();
                    }
                    handler.removeCallbacks(this);

                }
                else
                {
                    handler.postDelayed(this,50);
                }
            }
        });

    }

     */

    @Override
    public void onStop() {
        super.onStop();
        fileModifyObserver.startWatching();
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
    public void onFragmentCacheClear(String file_path, FileObjectType fileObjectType) {
        if(file_path==null || fileObjectType==null)
        {
            cache_cleared=true;
        }
        else if(Global.IS_CHILD_FILE(this.fileObjectType+fileclickselected,fileObjectType+file_path))
        {
            cache_cleared=true;
        }
        else if((this.fileObjectType+fileclickselected).equals(fileObjectType+new File(file_path).getParent()))
        {
            cache_cleared=true;
        }

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

    /*
    private class AsyncTaskFilePopulate extends svl.kadatha.filex.AsyncTask<Void,Void,Void>
    {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            asynctask_status=AsyncTaskStatus.STARTED;
            cancelableProgressBarDialog=CancelableProgressBarDialog.getInstance(CANCEL_PROGRESS_REQUEST_CODE);
            cancelableProgressBarDialog.set_title(getString(R.string.analysing));

            cancelableProgressBarDialog.setProgressBarCancelListener(new CancelableProgressBarDialog.ProgresBarFragmentCancelListener() {
                @Override
                public void on_cancel_progress() {
                    if(asyncTaskFilePopulate!=null) asyncTaskFilePopulate.cancel(true);
                    if(fillSizeAsyncTask!=null) fillSizeAsyncTask.cancel(true);
                    storageAnalyserActivity.onClickCancel();
                }
            });


            if(storageAnalyserActivity.fm==null)
            {
                context=getContext();
                storageAnalyserActivity=(StorageAnalyserActivity) context;
                storageAnalyserActivity.fm=storageAnalyserActivity.getSupportFragmentManager();
            }

            cancelableProgressBarDialog.show(storageAnalyserActivity.fm, "");
        }


        @Override
        protected Void doInBackground(Void... voids) {
            filled_filePOJOs=false;
            filled_filePOJOs=FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,fileclickselected,currentUsbFile,false);
            return null;
        }


        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            filled_filePOJOs=true;
        }


        private boolean FILL_FILEPOJO(List<FilePOJO> filePOJOS, List<FilePOJO> filePOJOS_filtered, FileObjectType fileObjectType,
                                            String fileclickselected,UsbFile usbFile ,boolean archive_view)
        {

            filePOJOS.clear(); filePOJOS_filtered.clear();
            String other_permission_string = null;
            File file=new File(fileclickselected);

            if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
            {
                File[] file_array;
                if((file_array=file.listFiles())!=null)
                {
                    int size=file_array.length;
                    for(int i=0;i<size;++i)
                    {
                        if(isCancelled())
                        {
                            return true;
                        }
                        File f=file_array[i];
                        FilePOJO filePOJO =FilePOJOUtil.MAKE_FilePOJO(f,true,archive_view,fileObjectType);
                        if(!filePOJO.getName().startsWith("."))
                        {

                            filePOJOS_filtered.add(filePOJO);
                        }

                        filePOJOS.add(filePOJO);

                    }

                }
            }
            else if(fileObjectType==FileObjectType.USB_TYPE)
            {
                if(MainActivity.usbFileRoot==null)
                {
                    return true;
                }
                else
                {
                    UsbFile[] file_array;
                    try {
                        if(usbFile==null)
                        {
                            usbFile=MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(fileclickselected));
                        }
                        file_array=usbFile.listFiles();
                        int size=file_array.length;
                        for(int i=0;i<size;++i)
                        {
                            if(isCancelled())
                            {
                                return true;
                            }
                            UsbFile f=file_array[i];
                            FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(f,true);
                            filePOJOS_filtered.add(filePOJO);
                            filePOJOS.add(filePOJO);

                        }

                    } catch (IOException e) {
                        MainActivity.usbFileRoot=null;
                        return true;
                    }
                }
            }
            else if(fileObjectType==FileObjectType.FTP_TYPE)
            {
                if(!Global.CHECK_FTP_SERVER_CONNECTED())
                {
                    return true;
                }
                else
                {
                    FTPFile[] file_array;
                    try {

                        file_array=MainActivity.FTP_CLIENT.listFiles(fileclickselected);
                        int size=file_array.length;
                        for(int i=0;i<size;++i)
                        {
                            if(isCancelled())
                            {
                                return true;
                            }
                            FTPFile f=file_array[i];
                            String name=f.getName();
                            String path=(fileclickselected.endsWith(File.separator) ? fileclickselected+name : fileclickselected+File.separator+name);
                            FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(f,false,false,fileObjectType,path);
                            filePOJOS_filtered.add(filePOJO);
                            filePOJOS.add(filePOJO);

                        }

                    } catch (IOException e) {
                        return true;
                    }
                }
            }

            Global.HASHMAP_FILE_POJO.put(fileObjectType+fileclickselected,filePOJOS);
            Global.HASHMAP_FILE_POJO_FILTERED.put(fileObjectType+fileclickselected,filePOJOS_filtered);
            return true;
        }
    }

     */


    public class StorageAnalyserAdapter extends RecyclerView.Adapter<StorageAnalyserAdapter.ViewHolder>
    {
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
            boolean selected=viewModel.mselecteditems.get(p2,false);
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
                int size=viewModel.mselecteditems.size();
                if(size>0)
                {
                    longClickMethod(p1,size);
                }
                else
                {
                    int pos=getBindingAdapterPosition();
                    FilePOJO filePOJO=filePOJO_list.get(pos);
                    clicked_filepojo=filePOJO;
                    fileObjectType=filePOJO.getFileObjectType();
                    if(filePOJO.getIsDirectory())
                    {
                        storageAnalyserActivity.createFileSelectorFragmentTransaction(filePOJO);
                    }
                    else
                    {
                        file_open_intent_despatch(filePOJO.getPath(),filePOJO.getFileObjectType(),filePOJO.getName());
                    }
                    FileSelectorRecentDialog.ADD_FILE_POJO_TO_RECENT(filePOJO,FileSelectorRecentDialog.STORAGE_ANALYSER);
                }
            }

            @Override
            public boolean onLongClick(View view) {
                longClickMethod(view,viewModel.mselecteditems.size());
                return true;
            }

            private void longClickMethod (View v, int size)
            {
                pos=getBindingAdapterPosition();
                if(viewModel.mselecteditems.get(pos,false))
                {
                    viewModel.mselecteditems.delete(pos);
                    viewModel.mselecteditemsFilePath.delete(pos);
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
                        final StorageAnalyserDialog sad=(StorageAnalyserDialog) storageAnalyserActivity.fm.findFragmentById(R.id.storage_analyser_container);
                        storageAnalyserActivity.DeselectAllAndAdjustToolbars(sad,sad.fileclickselected);
                    }
                }
                else
                {
                    viewModel.mselecteditems.put(pos,true);
                    viewModel.mselecteditemsFilePath.put(pos,filePOJO_list.get(pos).getPath());
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
        viewModel.mselecteditems=new SparseBooleanArray();
        viewModel.mselecteditemsFilePath=new SparseArray<>();
        int size=filePOJO_list.size();

        for(int i=0;i<size;++i)
        {
            viewModel.mselecteditems.put(i,true);
            viewModel.mselecteditemsFilePath.put(i,filePOJO_list.get(i).getPath());
        }

        int s=viewModel.mselecteditems.size();
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

    /*
    public void seekSAFPermission()
    {
        storageAnalyserActivity.clear_cache=false;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        activityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent>activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode()== Activity.RESULT_OK)
            {
                Uri treeUri;
                treeUri = result.getData().getData();
                Global.ON_REQUEST_URI_PERMISSION(context,treeUri);
            }
            else
            {
                Global.print(context,getString(R.string.permission_not_granted));
            }

        }
    });

     */

    private final ActivityResultLauncher<Intent> activityResultLauncher_unknown_package_install_permission=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode()== Activity.RESULT_OK)
            {
                if(clicked_filepojo!=null)file_open_intent_despatch(clicked_filepojo.getPath(),clicked_filepojo.getFileObjectType(),clicked_filepojo.getName());
                clicked_filepojo=null;
            }
            else
            {
                Global.print(context,getString(R.string.permission_not_granted));
            }
        }
    });


    private void file_open_intent_despatch(final String file_path, final FileObjectType fileObjectType, String file_name)
    {
        int idx=file_name.lastIndexOf(".");
        String file_ext="";
        if(idx!=-1)
        {
            file_ext=file_name.substring(idx+1);
        }

        if(file_ext.matches("(?i)zip"))
        {
            Global.print(context,getString(R.string.cannot_open_here));
            return;
        }


        if(file_ext.equals("") || !Global.CHECK_APPS_FOR_RECOGNISED_FILE_EXT(context,file_ext))
        {
            FileTypeSelectDialog fileTypeSelectFragment=FileTypeSelectDialog.getInstance(file_path,false,fileObjectType,tree_uri,tree_uri_path);
            /*
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

             */
            fileTypeSelectFragment.show(storageAnalyserActivity.fm, "");
        }
        else
        {
            if(file_ext.matches("(?i)apk"))
            {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!storageAnalyserActivity.getPackageManager().canRequestPackageInstalls()) {
                        Intent unknown_package_install_intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                        unknown_package_install_intent.setData(Uri.parse(String.format("package:%s", Global.FILEX_PACKAGE)));
                        activityResultLauncher_unknown_package_install_permission.launch(unknown_package_install_intent);
                        return;
                    }
                }
            }

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
        viewModel.mselecteditems=new SparseBooleanArray();
        viewModel.mselecteditemsFilePath=new SparseArray<>();
        if(adapter!=null)
        {
            adapter.notifyDataSetChanged();
            file_list_size=filePOJO_list.size();
            storageAnalyserActivity.file_number.setText(viewModel.mselecteditems.size()+"/"+file_list_size);
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


    public void clear_cache_and_refresh(String file_path, FileObjectType fileObjectType)
    {
        viewModel.mselecteditems=new SparseBooleanArray();
        viewModel.mselecteditemsFilePath=new SparseArray<>();
        storageAnalyserActivity.clearCache(file_path,fileObjectType);
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
            SAFPermissionHelperDialog safpermissionhelper=SAFPermissionHelperDialog.getInstance(SAF_PERMISSION_REQUEST_CODE,file_path,fileObjectType);
            /*
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

             */
            safpermissionhelper.show(storageAnalyserActivity.fm, "saf_permission_dialog");
            return false;
        }
        else
        {
            return true;
        }
    }

    /*
    private class FillSizeAsyncTask extends AsyncTask<Void,Void,Void>
    {

        private int NO_OF_FILES;
        private long SIZE_OF_FILES;

        final List<FilePOJO>filePOJOS;
        final long final_storage_space;

        FillSizeAsyncTask(List<FilePOJO>filePOJOS,long final_storage_space)
        {
            this.filePOJOS=filePOJOS;
            this.final_storage_space=final_storage_space;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if(cancelableProgressBarDialog==null)
            {
                cancelableProgressBarDialog=CancelableProgressBarDialog.getInstance(CANCEL_PROGRESS_REQUEST_CODE);
                cancelableProgressBarDialog.set_title(getString(R.string.analysing));

                cancelableProgressBarDialog.setProgressBarCancelListener(new CancelableProgressBarDialog.ProgresBarFragmentCancelListener() {
                    @Override
                    public void on_cancel_progress() {
                        if(asyncTaskFilePopulate!=null) asyncTaskFilePopulate.cancel(true);
                        if(fillSizeAsyncTask!=null) fillSizeAsyncTask.cancel(true);
                        storageAnalyserActivity.onClickCancel();
                    }
                });

                if(storageAnalyserActivity.fm==null)
                {
                    context=getContext();
                    storageAnalyserActivity=(StorageAnalyserActivity) context;
                    storageAnalyserActivity.fm=storageAnalyserActivity.getSupportFragmentManager();
                }

                cancelableProgressBarDialog.show(storageAnalyserActivity.fm, "");
                //Log.d("shankar", "cancelable progress bar shown again");
            }
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            filled_file_size=true;
            FilePOJOUtil.SET_HASHMAP_FILE_POJO_SIZE_NULL(fileclickselected,fileObjectType);
            asynctask_status=AsyncTaskStatus.COMPLETED;
            if(cancelableProgressBarDialog!=null)
            {
                cancelableProgressBarDialog.dismissAllowingStateLoss();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            filled_file_size=false;
            filled_file_size=fill_file_size(filePOJOS, final_storage_space);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            asynctask_status=AsyncTaskStatus.COMPLETED;
            if(cancelableProgressBarDialog!=null)
            {
                cancelableProgressBarDialog.dismissAllowingStateLoss();
            }

        }

        private void get_size(File f, boolean include_folder)
        {
            int no_of_files=0;
            long size_of_files=0L;
            if(isCancelled()) return;
            if(f.isDirectory())
            {

                File[] files_array=f.listFiles();
                if(files_array!=null && files_array.length!=0)
                {
                    for(File file:files_array)
                    {
                        get_size(file,include_folder);
                    }
                    if(include_folder)
                    {
                        no_of_files++;
                    }
                }

            }
            else
            {
                no_of_files++;
                size_of_files+=f.length();
            }

            NO_OF_FILES+=no_of_files;
            SIZE_OF_FILES+=size_of_files;
        }


        private boolean fill_file_size(List<FilePOJO> filePOJOS,long volume_storage_size)
        {
            if(filePOJOS==null) return true;
            int size=filePOJOS.size();
            for(int i=0;i<size;++i)
            {
                if(isCancelled()) return true;
                FilePOJO filePOJO=filePOJOS.get(i);
                NO_OF_FILES=0; SIZE_OF_FILES=0;
                if(filePOJO.getTotalSizePercentage()!=null) continue;
                if(filePOJO.getIsDirectory())
                {
                    get_size(new File(filePOJO.getPath()),true);
                    filePOJO.setTotalFiles(NO_OF_FILES);
                    filePOJO.setTotalSizeLong(SIZE_OF_FILES);
                    filePOJO.setTotalSize(FileUtil.humanReadableByteCount(SIZE_OF_FILES,Global.BYTE_COUNT_BLOCK_1000));
                    double percentage = SIZE_OF_FILES * 100.0/ volume_storage_size;
                    filePOJO.setTotalSizePercentageDouble(percentage);
                    filePOJO.setTotalSizePercentage(String.format("%.2f",percentage) +"%");
                }
                else
                {
                    double percentage = filePOJO.getSizeLong() * 100.0 / volume_storage_size;
                    filePOJO.setTotalSizePercentageDouble(percentage);
                    filePOJO.setTotalSizePercentage(String.format("%.2f",percentage)+"%");
                }

            }
            return true;
        }
    }

     */

}



