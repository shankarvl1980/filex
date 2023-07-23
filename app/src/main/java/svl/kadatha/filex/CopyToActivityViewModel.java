package svl.kadatha.filex;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.List;
import java.util.concurrent.Future;

public class CopyToActivityViewModel extends AndroidViewModel {
    private boolean isCancelled;
    private Future<?> future1,future2,future3;
    public List<FilePOJO> destFilePOJOs;

    public CopyToActivityViewModel(@NonNull Application application) {
        super(application);
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

}
