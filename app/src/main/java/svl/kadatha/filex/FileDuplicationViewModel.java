package svl.kadatha.filex;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FileDuplicationViewModel extends ViewModel {

    public MutableLiveData<Boolean> duplicationChecked=new MutableLiveData<>();
    private boolean isCancelled;
    private Future<?> future1,future2,future3;
    public List<String> duplicate_file_path_array=new ArrayList<>();
    public List<FilePOJO> filePOJOS;
    public ArrayList<String> not_to_be_replaced_files_path_array=new ArrayList<>();

    public String source_folder,dest_folder;
    public FileObjectType sourceFileObjectType,destFileObjectType;
    boolean cut;
    public ArrayList<String>files_selected_array;
    public ArrayList<String> overwritten_file_path_list=new ArrayList<>();


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


    public void checkForExistingFileWithSameName(String source_folder,FileObjectType sourceFileObjectType, String dest_folder,FileObjectType destFileObjectType,ArrayList<String>files_selected_array, boolean cut ,boolean findAllDuplicates)
    {
        if(Boolean.TRUE.equals(duplicationChecked.getValue()))return;
        this.source_folder=source_folder;
        this.sourceFileObjectType=sourceFileObjectType;
        this.dest_folder=dest_folder;
        this.destFileObjectType=destFileObjectType;
        this.cut=cut;
        this.files_selected_array=files_selected_array;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                if(filePOJOS==null)
                {
                    filePOJOS=Global.HASHMAP_FILE_POJO.get(destFileObjectType+dest_folder);
                }
                int filePojoSize=filePOJOS.size();
                int fileSelectedSize=files_selected_array.size();
                FilePOJO filePOJO;
                String file_path;
                boolean stop_loop = false;
                for(int i=0;i<filePojoSize;++i)
                {
                    filePOJO=filePOJOS.get(i);
                    for(int j=0;j<fileSelectedSize;++j)
                    {
                        file_path=files_selected_array.get(j);
                        if(filePOJO.getName().equals(new File(file_path).getName()))
                        {
                            duplicate_file_path_array.add(file_path);

                            if(!findAllDuplicates)
                            {
                                stop_loop=true;
                                break;
                            }

                        }

                    }
                    if(stop_loop)break;
                }

                duplicationChecked.postValue(true);
            }
        });

    }

}
