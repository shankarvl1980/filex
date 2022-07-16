package svl.kadatha.filex;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.Future;

public class FilePOJOViewModel extends AndroidViewModel {
    private final Application application;
    private boolean alreadyRun,isCancelled;
    private Future<?> future;
    public MutableLiveData<Boolean> isFinished=new MutableLiveData<>();
    public List<FilePOJO> filePOJO_list;

    public FilePOJOViewModel(@NonNull Application application) {
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

    private void cancel(boolean mayInterruptRunning){
        if(future!=null)
        {
            future.cancel(mayInterruptRunning);
            isCancelled=true;
        }
    }



}
