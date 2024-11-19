package svl.kadatha.filex;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FileSelectorActivityViewModel extends AndroidViewModel {
    public final MutableLiveData<AsyncTaskStatus> populateUriAsyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    ArrayList<Uri> uri_list;
    ClipData clipData;
    private boolean isCancelled;
    private Future<?> future1, future2;

    public FileSelectorActivityViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning) {
        if (future1 != null) {
            future1.cancel(mayInterruptRunning);
        }
        if (future2 != null) {
            future2.cancel(mayInterruptRunning);
        }

        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
    }

    public void populateUri(Context context, FileSelectorFragment fileSelectorFragment) {
        if (populateUriAsyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        populateUriAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                uri_list = new ArrayList<>();
                int size = fileSelectorFragment.viewModel.mselecteditems.size();
                for (int i = 0; i < size; ++i) {
                    String file_path = fileSelectorFragment.viewModel.mselecteditems.getValueAtIndex(i);
                    File file = new File(file_path);
                    uri_list.add(FileProvider.getUriForFile(context, Global.FILEX_PACKAGE + ".provider", file));
                }

                clipData = new ClipData(
                        new ClipDescription("Selected Files", new String[]{"*/*"}),
                        new ClipData.Item(uri_list.get(0))
                );

                for (int i = 1; i < size; i++) {
                    clipData.addItem(new ClipData.Item(uri_list.get(i)));
                }
                populateUriAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
}