package svl.kadatha.filex;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.concurrent.Future;

@SuppressWarnings("ALL")
public class ViewModelFileCount extends ViewModel {
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public long total_size_of_files;
    MutableLiveData<Integer> total_no_of_files = new MutableLiveData<>();
    MutableLiveData<String> size_of_files_formatted = new MutableLiveData<>();
    private FileCountSize fileCountSize;
    private int cumulative_no_of_files;
    private Future<?> future;
    private boolean isCancelled;

    // Constructor where you instantiate and execute FileCountSize
    public ViewModelFileCount(Context context, List<String> files_selected_array, FileObjectType sourceFileObjectType) {
        // Instantiate FileCountSize within the ViewModel
        fileCountSize = new FileCountSize(context, files_selected_array, sourceFileObjectType);

        // Call the method in FileCountSize (which handles background execution)
        fileCountSize.fileCount();  // Assuming this method runs in a background thread

        // Optionally, you can observe and react to any changes in the file count/size
        fileCountSize.mutable_total_no_of_files.observeForever(totalFiles -> {
            total_no_of_files.postValue(totalFiles);
        });

        fileCountSize.mutable_size_of_files_to_be_archived_copied.observeForever(totalSize -> {
            size_of_files_formatted.postValue(totalSize);
        });
    }

    // Another constructor variant to instantiate FileCountSize with just a data list
    public ViewModelFileCount(Context context, List<Uri> data_list) {
        fileCountSize = new FileCountSize(context, data_list);

        // Call the method in FileCountSize (which handles background execution)
        fileCountSize.fileCountDatalist();  // Assuming this method runs in a background thread

        // Optionally, update LiveData as needed
        total_no_of_files.postValue(fileCountSize.total_no_of_files);
        size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(fileCountSize.total_size_of_files));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    private void cancel(boolean mayInterruptRunning) {
        if (future != null) future.cancel(mayInterruptRunning);
        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
    }


    public static class ViewModelFileCountFactory implements ViewModelProvider.Factory {
        private final Context context;
        private final List<String> filesSelectedArray;
        private final FileObjectType fileObjectType;

        // Constructor to accept the parameters for ViewModel
        public ViewModelFileCountFactory(Context context, List<String> filesSelectedArray, FileObjectType fileObjectType) {
            this.context = context;
            this.filesSelectedArray = filesSelectedArray;
            this.fileObjectType = fileObjectType;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ViewModelFileCount.class)) {
                // Create a new instance of ViewModelFileCount and pass the arguments
                return (T) new ViewModelFileCount(context, filesSelectedArray, fileObjectType);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
