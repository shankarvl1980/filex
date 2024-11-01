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

import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;

public class DeleteFileOtherActivityViewModel extends AndroidViewModel {
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    private final Application application;
    public List<FilePOJO> deleted_files;
    public List<String> deleted_file_name_list;
    public List<String> deleted_file_path_list;
    public ArrayList<AudioPOJO> deleted_audio_files;
    public String source_folder;
    private boolean isCancelled;
    private Future<?> future1, future2, future3;


    public DeleteFileOtherActivityViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
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
        if (future3 != null) {
            future3.cancel(mayInterruptRunning);
        }

        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
    }


    public synchronized void deleteFilePOJO(String source_folder, List<FilePOJO> src_file_list, FileObjectType fileObjectType, Uri tree_uri, String tree_uri_path) {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        this.source_folder = source_folder;
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                deleted_files = new ArrayList<>();
                deleted_file_name_list = new ArrayList<>();
                deleted_file_path_list = new ArrayList<>();
                deleteFromFolder(src_file_list, fileObjectType, tree_uri, tree_uri_path);
                if (!deleted_files.isEmpty()) {
                    FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder, deleted_file_name_list, fileObjectType);
                    Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION, LocalBroadcastManager.getInstance(application), null);
                    Global.print_background_thread(application, application.getString(R.string.deleted_file));
                } else {
                    Global.print_background_thread(application, application.getString(R.string.could_not_delete_file));
                }

                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    private void deleteFromFolder(List<FilePOJO> src_file_list, FileObjectType fileObjectType, Uri tree_uri, String tree_uri_path) {
        boolean success;
        int size = src_file_list.size();
        String current_file_name;

        for (int i = 0; i < size; ++i) {
            if (isCancelled()) {
                return;
            }
            FilePOJO filePOJO = src_file_list.get(i);
            File f = new File(filePOJO.getPath());
            current_file_name = f.getName();
            FileModel fileModel = FileModelFactory.getFileModel(filePOJO.getPath(), fileObjectType, tree_uri, tree_uri_path);
            success = fileModel.delete();
            if (success) {
                deleted_files.add(filePOJO);
                deleted_file_name_list.add(current_file_name);
                deleted_file_path_list.add(filePOJO.getPath());
            }
        }
    }

    public synchronized void deleteAudioPOJO(String source_folder, List<AudioPOJO> src_audio_file_list, FileObjectType fileObjectType, Uri tree_uri, String tree_uri_path) {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        this.source_folder = source_folder;
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future2 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                deleted_audio_files = new ArrayList<>();
                deleted_file_name_list = new ArrayList<>();
                deleted_file_path_list = new ArrayList<>();

                deleteAudioPOJOFromFolder(src_audio_file_list, fileObjectType, tree_uri, tree_uri_path);
                if (!deleted_audio_files.isEmpty()) {
                    FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(source_folder, deleted_file_name_list, fileObjectType);
                    Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION, LocalBroadcastManager.getInstance(application), null);
                    Global.print_background_thread(application, application.getString(R.string.deleted_audio_file));
                } else {
                    Global.print_background_thread(application, application.getString(R.string.could_not_delete_file));
                }
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    private void deleteAudioPOJOFromFolder(List<AudioPOJO> src_audio_file_list, FileObjectType fileObjectType, Uri tree_uri, String tree_uri_path) {
        boolean success;
        int size = src_audio_file_list.size();
        String current_file_name;

        for (int i = 0; i < size; ++i) {
            if (isCancelled()) {
                return;
            }
            AudioPOJO audioPOJO = src_audio_file_list.get(i);
            FileModel fileModel = FileModelFactory.getFileModel(audioPOJO.getData(), fileObjectType, tree_uri, tree_uri_path);
            current_file_name = fileModel.getName();
            success = fileModel.delete();
            if (success) {
                deleted_audio_files.add(audioPOJO);
                deleted_file_name_list.add(current_file_name);
                deleted_file_path_list.add(audioPOJO.getData());
            }
        }
    }
}
