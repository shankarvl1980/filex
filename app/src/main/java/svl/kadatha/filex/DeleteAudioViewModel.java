package svl.kadatha.filex;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class DeleteAudioViewModel extends AndroidViewModel {
    private final Application application;
    private boolean isCancelled;
    private Future<?> future1,future2,future3;
    public MutableLiveData<Boolean> isFinished=new MutableLiveData<>();
    private boolean isFromInternal;
    public ArrayList<AudioPOJO> deleted_audio_files;
    public List<String> deleted_file_name_list;
    List<String> deleted_file_path_list;
    //public List<Integer> deleted_files_idx;
    public boolean success;

    public DeleteAudioViewModel(@NonNull Application application) {
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


    public synchronized void deleteAudioPOJO(boolean whetherFromAlbum, List<AudioPOJO> src_audio_file_list,Uri tree_uri, String tree_uri_path)
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                deleted_audio_files=new ArrayList<>();
                //deleted_files_idx=new ArrayList<>();
                deleted_file_path_list=new ArrayList<>();
                deleted_file_name_list=new ArrayList<>();
                success=false;
                if(whetherFromAlbum)
                {
                    isFromInternal=FileUtil.isFromInternal(FileObjectType.FILE_TYPE,src_audio_file_list.get(0).getData());
                    success=deleteFromFolder(src_audio_file_list,tree_uri,tree_uri_path);
                }
                else
                {
                    success=deleteFromLibrarySearch(src_audio_file_list,tree_uri,tree_uri_path);
                }


                if(deleted_audio_files.size()>0)
                {
                    if(whetherFromAlbum)
                    {
                        String parent_dir=new File(deleted_file_path_list.get(0)).getParent();
                        FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(parent_dir,deleted_file_name_list,FileObjectType.FILE_TYPE);
                    }
                    else
                    {
                        for(String file_path:deleted_file_path_list)
                        {
                            String parent_dir=new File(file_path).getParent();
                            String file_name=new File(file_path).getName();
                            FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(parent_dir, Collections.singletonList(file_name),FileObjectType.FILE_TYPE);
                        }
                    }

                    Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION, LocalBroadcastManager.getInstance(application),AudioPlayerActivity.ACTIVITY_NAME);
                }

                isFinished.postValue(true);
            }
        });
    }


    private boolean deleteFromFolder(List<AudioPOJO> src_audio_file_list,Uri tree_uri, String tree_uri_path)
    {
        boolean success=false;
        int size=src_audio_file_list.size();
        String current_file_name;
        if(isFromInternal)
        {

            for(int i=0;i<size;++i)
            {
                AudioPOJO audioPOJO=src_audio_file_list.get(i);
                String file_path=audioPOJO.getData();
                File f=new File(file_path);
                current_file_name=f.getName();
                success=deleteNativeDirectory(f);
                if(success)
                {
                    deleted_audio_files.add(audioPOJO);
                    deleted_file_path_list.add(file_path);
                    deleted_file_name_list.add(current_file_name);
                }
                //files_selected_for_delete.remove(file_path);
            }

        }
        else
        {
            // no need to check SAF permission as this dialog started only after obtaining SAF permission for all files
            for(int i=0;i<size;++i)
            {
                AudioPOJO audioPOJO=src_audio_file_list.get(i);
                String file_path=audioPOJO.getData();
                File file=new File(file_path);
                current_file_name=file.getName();
                success=deleteSAFDirectory(file,tree_uri,tree_uri_path);
                if(success)
                {
                    deleted_audio_files.add(audioPOJO);
                    deleted_file_path_list.add(file_path);
                    deleted_file_name_list.add(current_file_name);
                }
                //files_selected_for_delete.remove(file_path);
            }

        }
        return success;
    }

    private boolean deleteFromLibrarySearch(List<AudioPOJO> src_audio_file_list, Uri tree_uri, String tree_uri_path)
    {
        boolean success=false;
        int size=src_audio_file_list.size();
        String current_file_name;
        for(int i=0;i<size;++i)
        {
            AudioPOJO audioPOJO=src_audio_file_list.get(i);
            String file_path=audioPOJO.getData();
            File f=new File(file_path);
            if(FileUtil.isFromInternal(FileObjectType.FILE_TYPE,file_path))
            {
                current_file_name=f.getName();
                success=deleteNativeDirectory(f);
            }
            else
            {
                current_file_name=f.getName();
                success=deleteSAFDirectory(f,tree_uri,tree_uri_path);
            }
            if(success)
            {
                deleted_audio_files.add(audioPOJO);
                deleted_file_path_list.add(file_path);
                deleted_file_name_list.add(current_file_name);
            }
           // files_selected_for_delete.remove(file_path);
        }

        return success;
    }


    public boolean deleteNativeDirectory(final File folder)
    {
        boolean success=false;

        if (folder.isDirectory())            //Check if folder file is a real folder
        {
            if(isCancelled())
            {
                return false;
            }

            File[] list = folder.listFiles(); //Storing all file name within array
            int size=list.length;
            for (int i = 0; i < size; ++i)
            {
                if(isCancelled())
                {
                    return false;
                }

                File tmpF = list[i];
                success=deleteNativeDirectory(tmpF);

            }
        }
//        counter_no_files++;
//        counter_size_files+=folder.length();
//        size_of_files_format=FileUtil.humanReadableByteCount(counter_size_files,Global.BYTE_COUNT_BLOCK_1000);
//        publishProgress(folder);
        success=folder.delete();
        return success;
    }

    public boolean deleteSAFDirectory(final File folder, Uri tree_uri, String tree_uri_path)
    {
        boolean success=true;
        if (folder.isDirectory())            //Check if folder file is a real folder
        {
            if(isCancelled())
            {
                return false;
            }
            File[] list = folder.listFiles(); //Storing all file name within array
            int size=list.length;
            for (int i = 0; i < size; ++i)
            {
                if(isCancelled())
                {
                    return false;
                }
                File tmpF = list[i];
                success=deleteSAFDirectory(tmpF,tree_uri,tree_uri_path);

            }

        }

//        counter_no_files++;
//        counter_size_files+=folder.length();
//        size_of_files_format=FileUtil.humanReadableByteCount(counter_size_files,Global.BYTE_COUNT_BLOCK_1000);
//        publishProgress(folder);
        success=FileUtil.deleteSAFDirectory(application,folder.getAbsolutePath(),tree_uri,tree_uri_path);

        return success;
    }


}