package svl.kadatha.filex;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

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
    public MutableLiveData<Boolean> isFinished=new MutableLiveData<>();
    private boolean isFromInternal;
    public List<FilePOJO> deleted_files;
    public List<String> deleted_file_name_list;
    private List<FilePOJO> src_file_list;
    private List<AudioPOJO> src_audio_file_list;
    public ArrayList<AudioPOJO> deleted_audio_files;
    private FileObjectType fileObjectType;
    private Uri tree_uri;
    private String tree_uri_path;



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

    public synchronized void deleteFilePOJO(List<FilePOJO> msrc_file_list, FileObjectType mfileObjectType,Uri mtree_uri, String mtree_uri_path)
    {
        src_file_list=msrc_file_list;
        fileObjectType=mfileObjectType;
        tree_uri=mtree_uri;
        tree_uri_path=mtree_uri_path;
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
                isFinished.postValue(true);
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


    public synchronized void deleteAudioPOJO(List<AudioPOJO> msrc_file_list, FileObjectType mfileObjectType,Uri mtree_uri, String mtree_uri_path)
    {
        src_audio_file_list=msrc_file_list;
        fileObjectType=mfileObjectType;
        tree_uri=mtree_uri;
        tree_uri_path=mtree_uri_path;
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
                isFinished.postValue(true);
            }
        });
    }

    private boolean deleteAudioPOJOFromFolder(List<AudioPOJO> src_file_list, FileObjectType fileObjectType, Uri tree_uri, String tree_uri_path)
    {
        boolean success=false;
        int iteration=0;
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
