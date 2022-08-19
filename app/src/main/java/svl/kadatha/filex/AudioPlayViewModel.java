package svl.kadatha.filex;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class AudioPlayViewModel extends ViewModel {

    private boolean isCancelled;
    private Future<?> future1,future2;
    public final MutableLiveData<Boolean> isFinished=new MutableLiveData<>();

    public boolean fromArchiveView;
    public FileObjectType fileObjectType;
    public boolean fromThirdPartyApp;
    public String file_path;



    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning){
        if(future1!=null) future1.cancel(mayInterruptRunning);
        if(future2!=null) future2.cancel(mayInterruptRunning);
        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }

    public synchronized void albumPolling(String source_folder, FileObjectType fileObjectType, boolean fromThirdPartyApp, boolean fromArchiveView)
    {
        if(Boolean.TRUE.equals(isFinished.getValue()))return;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
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

                AudioPlayerService.AUDIO_QUEUED_ARRAY=new ArrayList<>();
                AudioPlayerService.CURRENT_PLAY_NUMBER=0;

                // limiting to the selected only, in case of file selected from usb storage by adding condition below
                if(fromArchiveView || fromThirdPartyApp || fileObjectType==FileObjectType.USB_TYPE)
                {
                    AudioPlayerService.AUDIO_QUEUED_ARRAY.add(AudioPlayerActivity.AUDIO_FILE);
                }
                else
                {
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
                                if(file_ext.matches(Global.AUDIO_REGEX))
                                {

                                    AudioPOJO audio=new AudioPOJO(0,filePOJO.getPath(),filePOJO.getName(),null,null,"0",fileObjectType);
                                    AudioPlayerService.AUDIO_QUEUED_ARRAY.add(audio);

                                    if(AudioPlayerActivity.AUDIO_FILE.getTitle().equals(filePOJO.getName()))AudioPlayerService.CURRENT_PLAY_NUMBER=count;
                                    count++;

                                }
                                else if(filePOJO.getName().equals(AudioPlayerActivity.AUDIO_FILE.getTitle()))
                                {

                                    AudioPlayerService.AUDIO_QUEUED_ARRAY.add(AudioPlayerActivity.AUDIO_FILE);
                                    AudioPlayerService.CURRENT_PLAY_NUMBER=count;
                                    count++;
                                }

                            }
                            else if(filePOJO.getName().equals(AudioPlayerActivity.AUDIO_FILE.getTitle()))
                            {

                                AudioPlayerService.AUDIO_QUEUED_ARRAY.add(AudioPlayerActivity.AUDIO_FILE);
                                AudioPlayerService.CURRENT_PLAY_NUMBER=count;
                                count++;
                            }

                        }
                    }

                }

                isFinished.postValue(true);
            }
        });


    }




}
