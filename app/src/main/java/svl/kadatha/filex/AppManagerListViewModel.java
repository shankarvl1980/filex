package svl.kadatha.filex;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import me.jahnen.libaums.core.fs.UsbFile;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;

public class AppManagerListViewModel extends AndroidViewModel {

    private Future<?> future1,future2, future3;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public List<AppManagerListFragment.AppPOJO> systemAppPOJOList, userAppPOJOList;
    private final Application application;
    public final MutableLiveData<AsyncTaskStatus> isBackedUp=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    private boolean isCancelled;
    private FileObjectType destFileObjectType;
    private final long[] bytes_read=new long[1];
    public List<FilePOJO> destFilePOJOs;

    public AppManagerListViewModel(@NonNull Application application) {
        super(application);
        this.application=application;
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning){
        if(future1!=null) future1.cancel(mayInterruptRunning);
        if(future2!=null) future2.cancel(mayInterruptRunning);
        if(future3!=null) future3.cancel(mayInterruptRunning);

        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }

    public synchronized void populateApps()
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                systemAppPOJOList=new ArrayList<>();
                userAppPOJOList=new ArrayList<>();
                if(!repositoryClass.app_pojo_hashmap.containsKey("system"))
                {
                    repositoryClass.populateAppsList();
                }

                userAppPOJOList=repositoryClass.app_pojo_hashmap.get("user");
                systemAppPOJOList=repositoryClass.app_pojo_hashmap.get("system");
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });

    }

    public void back_up(List<String> files_selected_array, String dest_folder, FileObjectType destFileObjectType,List<String> new_name_list,Uri tree_uri,String tree_uri_path)
    {
        if(!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(null,destFileObjectType))
        {
            Global.print(application,application.getString(R.string.wait_till_completion_on_going_operation_on_usb));
            return;
        }

        if(!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_FTP(null,destFileObjectType))
        {
            Global.print(application,application.getString(R.string.wait_till_current_service_on_ftp_finishes));
            return;
        }

        if(isBackedUp.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        isBackedUp.setValue(AsyncTaskStatus.STARTED);
        this.destFileObjectType=destFileObjectType;
        List<String>dest_file_names=new ArrayList<>();

        ExecutorService executorService=MyExecutorService.getExecutorService();
        future2=executorService.submit(new Runnable() {
            @Override
            public void run() {

                if(destFilePOJOs==null)
                {
                    UsbFile currentUsbFile=null;
                    if(destFileObjectType==FileObjectType.USB_TYPE)
                    {
                        if(MainActivity.usbFileRoot!=null)
                        {
                            try {
                                currentUsbFile=MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(dest_folder));

                            } catch (IOException e) {

                            }
                        }
                    }
                    FilePOJOUtil.FILL_FILE_POJO(new ArrayList<>(), new ArrayList<>(),destFileObjectType,dest_folder,currentUsbFile,false);
                    RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                    destFilePOJOs=repositoryClass.hashmap_file_pojo.get(destFileObjectType+dest_folder);
                }


                if(destFilePOJOs!=null)
                {
                    for(FilePOJO filePOJO:destFilePOJOs)
                    {
                        dest_file_names.add(filePOJO.getName());
                    }
                }


                List<String> overwritten_copied_file_name_list;
                boolean copy_result = false;
                final boolean cut=false;
                String current_file_name;
                boolean isWritable=FileUtil.isWritable(destFileObjectType,dest_folder);
                final List<String> copied_files_name=new ArrayList<>();  //declared here instead of at Asynctask class to keep track of copied files in case replacement


                if(destFileObjectType==FileObjectType.ROOT_TYPE)
                {
                    isBackedUp.postValue(AsyncTaskStatus.COMPLETED);
                    return;
                }
                List<File> src_file_list=new ArrayList<>();

                for(String s: files_selected_array)
                {
                    File file=new File(s);
                    src_file_list.add(file);
                }

                int count = 0;
                overwritten_copied_file_name_list=new ArrayList<>(dest_file_names);
                for (File file : src_file_list) {
                    if (isCancelled() || file == null) {
                        isBackedUp.postValue(AsyncTaskStatus.COMPLETED);
                        return;
                    }

                    current_file_name = new_name_list.get(count);
                    String dest_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder,current_file_name);

                    if (isWritable) {
                        copy_result = Copy_File_File(file, dest_file_path, cut,bytes_read);
                    } else {
                        copy_result=Copy_File_FileModel(file,dest_folder,current_file_name,tree_uri,tree_uri_path,bytes_read);

                    }
                    String f_p = file.getAbsolutePath();
                    if (copy_result) {
                        copied_files_name.add(current_file_name);

                    }

                    files_selected_array.remove(f_p);
                    ++count;
                }

                if(!copied_files_name.isEmpty())
                {
                    List<String> overwritten_copied_file_path_list=new ArrayList<>();
                    overwritten_copied_file_name_list.retainAll(copied_files_name);
                    for(String name:overwritten_copied_file_name_list)
                    {
                        overwritten_copied_file_path_list.add(Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder,name));
                    }

                    FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder,copied_files_name,destFileObjectType,overwritten_copied_file_path_list);
                    Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION, LocalBroadcastManager.getInstance(application),AppManagerActivity.ACTIVITY_NAME);
                    copied_files_name.clear();
                }

                isBackedUp.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    private boolean Copy_File_FileModel(File source, String dest_file_path, String name,Uri uri, String uri_path,long[] bytes_read)
    {
        boolean success;

        if(isCancelled())
        {
            return false;
        }

        FileModel destFileModel= FileModelFactory.getFileModel(dest_file_path,destFileObjectType,uri,uri_path);
        success=FileUtil.copy_File_FileModel(source,destFileModel,name,false,bytes_read);
        return success;
    }



    @SuppressWarnings("null")
    private boolean Copy_File_File(File source, String dest_file_path, boolean cut, long[] bytes_read)
    {
        boolean success=false;
        File destination=new File(dest_file_path);
        if (source.isDirectory())
        {
            if(isCancelled())
            {
                return false;
            }
            if(!destination.exists())// || !destination.isDirectoryUri())
            {
                if(!(success=FileUtil.mkdirsNative(destination)))
                {
                    return false;
                }
            }
            else {
                if(destination.isDirectory()) success=true;   //make success true as destination dir existsUri to execute cut directory
            }

            String[] files_name_array = source.list();
            if(files_name_array==null)
            {

                return true;
            }

            int size=files_name_array.length;
            for (int i=0;i<size;++i)
            {
                String inner_file_name=files_name_array[i];
                if(isCancelled())
                {
                    return false;
                }
                File srcFile = new File(source, inner_file_name);
                String inner_dest_file_path=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,inner_file_name);
                success=Copy_File_File(srcFile,inner_dest_file_path,cut,bytes_read);
            }

        }
        else
        {
            if(isCancelled())
            {
                return false;
            }
            success=FileUtil.copy_File_File(source,destination,cut,bytes_read);
        }

        return success;
    }

}
