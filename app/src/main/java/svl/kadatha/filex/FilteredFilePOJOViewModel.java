package svl.kadatha.filex;

import android.app.Application;
import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FilteredFilePOJOViewModel extends AndroidViewModel {
    private final Application application;
    private boolean isCancelled;
    private Future<?> future;
    public MutableLiveData<Boolean> isFinished=new MutableLiveData<>();
    public final List<FilePOJO> album_file_pojo_list=new ArrayList<>();
    public int total_images;
    public SparseBooleanArray selected_item_sparseboolean=new SparseBooleanArray();
    public int file_selected_idx=0;

    public FilteredFilePOJOViewModel(@NonNull Application application) {
        super(application);
        this.application=application;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if(future!=null)
        {
            future.cancel(true);
            isCancelled=true;
        }
    }

    public void cancel(boolean mayInterruptRunning){
        if(future!=null)
        {
            future.cancel(mayInterruptRunning);
            isCancelled=true;
            //to remove from hashmmap
        }
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }


    public void getAlbumFromCurrentFolder(FileObjectType fileObjectType,String source_folder, String regex,boolean fromArchiveView, boolean fromThirdPartyApp, FilePOJO currently_shown_file )
    {
        if(Boolean.TRUE.equals(isFinished.getValue())) return;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future=executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<FilePOJO> filePOJOS=new ArrayList<>(), filePOJOS_filtered=new ArrayList<>();
                if (!Global.HASHMAP_FILE_POJO.containsKey(fileObjectType+source_folder))
                {
                    FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType,source_folder,null,false);
                }
                else
                {
                    if(MainActivity.SHOW_HIDDEN_FILE)
                    {
                        filePOJOS=Global.HASHMAP_FILE_POJO.get(fileObjectType+source_folder) ;
                    }
                    else
                    {
                        filePOJOS=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+source_folder);
                    }
                }

                // limiting to the selected only, in case of file selected from usb storage by adding condition below
                if(fromArchiveView || fromThirdPartyApp || fileObjectType==FileObjectType.USB_TYPE)
                {
                    album_file_pojo_list.add(currently_shown_file);
                }
                else
                {
                    if(Global.SORT==null)
                    {
                        Global.GET_PREFERENCES(new TinyDB(application));
                    }
                    Collections.sort(filePOJOS,FileComparator.FilePOJOComparate(Global.SORT,false));
                    int size=filePOJOS.size();
                    int count=0;
                    for(int i=0; i<size;++i)
                    {
                        FilePOJO filePOJO=filePOJOS.get(i);
                        if(!filePOJO.getIsDirectory())
                        {
                            String file_ext;
                            int idx=filePOJO.getName().lastIndexOf(".");
                            if(idx!=-1)
                            {
                                file_ext=filePOJO.getName().substring(idx+1);
                                if(file_ext.matches(regex))
                                {

                                    album_file_pojo_list.add(filePOJO);
                                    if(filePOJO.getName().equals(currently_shown_file.getName())) file_selected_idx=count;
                                    count++;

                                }
                                else if(filePOJO.getName().equals(currently_shown_file.getName()))
                                {
                                    album_file_pojo_list.add(currently_shown_file);
                                    file_selected_idx=count;
                                    count++;
                                }

                            }
                            else if(filePOJO.getName().equals(currently_shown_file.getName()))
                            {
                                album_file_pojo_list.add(currently_shown_file);
                                file_selected_idx=count;
                                count++;
                            }

                        }
                    }

                }
                total_images=album_file_pojo_list.size();
                isFinished.postValue(true);
            }
        });


    }

}
