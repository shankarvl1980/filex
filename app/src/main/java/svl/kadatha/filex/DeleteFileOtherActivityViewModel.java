package svl.kadatha.filex;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import me.jahnen.libaums.core.fs.UsbFile;

public class DeleteFileOtherActivityViewModel extends AndroidViewModel {
    private final Application application;
    private boolean isCancelled;
    private Future<?> future1,future2,future3;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    private boolean isFromInternal;
    public List<FilePOJO> deleted_files;
    public List<String> deleted_file_name_list;
    public ArrayList<AudioPOJO> deleted_audio_files;
    public String source_folder;


    public DeleteFileOtherActivityViewModel(@NonNull Application application) {
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

    public synchronized void deleteFilePOJO(String source_folder,List<FilePOJO> src_file_list, FileObjectType fileObjectType,Uri tree_uri, String tree_uri_path)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        this.source_folder=source_folder;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {

                deleted_files=new ArrayList<>();
                deleted_file_name_list=new ArrayList<>();
                if(fileObjectType==FileObjectType.FILE_TYPE)
                {
                    isFromInternal=FileUtil.isFromInternal(fileObjectType,src_file_list.get(0).getPath());
                }
                deleteFromFolder(src_file_list,fileObjectType,tree_uri,tree_uri_path);
                if(deleted_files.size()>0)
                {
                    Global.print_background_thread(application,application.getString(R.string.deleted_file));
                }
                else
                {
                    Global.print_background_thread(application,application.getString(R.string.could_not_delete_file));
                }
                if(deleted_files.size()>0)
                {
                    FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_name_list,fileObjectType);
                    Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION, LocalBroadcastManager.getInstance(application),"");
                }
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }


    private boolean deleteFromFolder(List<FilePOJO> src_file_list, FileObjectType fileObjectType, Uri tree_uri, String tree_uri_path)
    {
        boolean success=false;
        int size=src_file_list.size();
        String current_file_name;
        if(fileObjectType==FileObjectType.FILE_TYPE)
        {
            if(isFromInternal)
            {
                for(int i=0;i<size;++i)
                {
                    if(isCancelled())
                    {
                        return false;
                    }
                    FilePOJO filePOJO=src_file_list.get(i);
                    File f=new File(filePOJO.getPath());
                    current_file_name=f.getName();
                    success=FileUtil.deleteNativeDirectory(f);
                    if(success)
                    {
                        deleted_files.add(filePOJO);
                        deleted_file_name_list.add(current_file_name);
                    }

                }

            }
            else
            {
                if(tree_uri==null || tree_uri_path== null) return false;
                for(int i=0;i<size;++i)
                {
                    if(isCancelled())
                    {
                        return false;
                    }
                    FilePOJO filePOJO=src_file_list.get(i);
                    File file=new File(filePOJO.getPath());
                    current_file_name=file.getName();
                    success=FileUtil.deleteSAFDirectory(application,file.getAbsolutePath(),tree_uri,tree_uri_path);
                    if(success)
                    {
                        deleted_files.add(filePOJO);
                        deleted_file_name_list.add(current_file_name);
                    }

                }

            }
        }
        else if(fileObjectType==FileObjectType.USB_TYPE)
        {
            for(int i=0;i<size;++i)
            {
                if(isCancelled())
                {
                    return false;
                }
                FilePOJO filePOJO=src_file_list.get(i);
                UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,filePOJO.getPath());
                current_file_name=f.getName();
                success=FileUtil.deleteUsbDirectory(f);
                if(success)
                {
                    deleted_files.add(filePOJO);
                    deleted_file_name_list.add(current_file_name);
                }

            }
        }

        return success;
    }


    public synchronized void deleteAudioPOJO(String source_folder,List<AudioPOJO> src_audio_file_list, FileObjectType fileObjectType,Uri tree_uri, String tree_uri_path)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        this.source_folder=source_folder;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future2=executorService.submit(new Runnable() {
            @Override
            public void run() {


                deleted_audio_files=new ArrayList<>();
                deleted_file_name_list=new ArrayList<>();
                if(fileObjectType==FileObjectType.FILE_TYPE)
                {
                    isFromInternal=FileUtil.isFromInternal(fileObjectType,src_audio_file_list.get(0).getData());
                }
                deleteAudioPOJOFromFolder(src_audio_file_list,fileObjectType,tree_uri,tree_uri_path);
                if(deleted_audio_files.size()>0)
                {
                    Global.print_background_thread(application,application.getString(R.string.deleted_audio_file));
                }
                else
                {
                    Global.print_background_thread(application,application.getString(R.string.could_not_delete_file));
                }
                if(deleted_audio_files.size()>0)
                {
                    FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder,deleted_file_name_list,fileObjectType);
                    Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION, LocalBroadcastManager.getInstance(application),AudioPlayerActivity.ACTIVITY_NAME);
                }
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    private boolean deleteAudioPOJOFromFolder(List<AudioPOJO> src_audio_file_list, FileObjectType fileObjectType, Uri tree_uri, String tree_uri_path)
    {
        boolean success=false;
        int size=src_audio_file_list.size();
        String current_file_name;
        if(fileObjectType==FileObjectType.FILE_TYPE)
        {
            if(isFromInternal)
            {
                for(int i=0;i<size;++i)
                {
                    if(isCancelled())
                    {
                        return false;
                    }
                    AudioPOJO audioPOJO=src_audio_file_list.get(i);
                    File f=new File(audioPOJO.getData());
                    current_file_name=f.getName();
                    success=FileUtil.deleteNativeDirectory(f);
                    if(success)
                    {
                        deleted_audio_files.add(audioPOJO);
                        deleted_file_name_list.add(current_file_name);

                    }
                }
            }
            else
            {
                if(tree_uri==null || tree_uri_path== null) return false;
                for(int i=0;i<size;++i)
                {
                    if(isCancelled())
                    {
                        return false;
                    }
                    AudioPOJO audioPOJO=src_audio_file_list.get(i);
                    File file=new File(audioPOJO.getData());
                    current_file_name=file.getName();
                    success=FileUtil.deleteSAFDirectory(application,file.getAbsolutePath(),tree_uri,tree_uri_path);
                    if(success)
                    {
                        deleted_audio_files.add(audioPOJO);
                        deleted_file_name_list.add(current_file_name);

                    }
                }

            }
        }
        else if(fileObjectType==FileObjectType.USB_TYPE)
        {
            for(int i=0;i<size;++i)
            {
                if(isCancelled())
                {
                    return false;
                }
                AudioPOJO audioPOJO=src_audio_file_list.get(i);
                UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,audioPOJO.getData());
                current_file_name=f.getName();
                success=FileUtil.deleteUsbDirectory(f);
                if(success)
                {
                    deleted_audio_files.add(audioPOJO);
                    deleted_file_name_list.add(current_file_name);

                }
            }
        }

        return success;
    }

}
