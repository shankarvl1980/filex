package svl.kadatha.filex;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import me.jahnen.libaums.core.fs.UsbFile;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;

public class AppManagerListViewModel extends AndroidViewModel {

    public static FileObjectType FILE_OBJECT_TYPE;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> isBackedUp = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    private final Application application;
    private final long[] bytes_read = new long[1];
    public List<AppManagerListFragment.AppPOJO> systemAppPOJOList, userAppPOJOList;
    public List<FilePOJO> destFilePOJOs;
    private Future<?> future1, future2, future3;
    private boolean isCancelled;

    public AppManagerListViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
        FILE_OBJECT_TYPE = null;
    }

    public void cancel(boolean mayInterruptRunning) {
        if (future1 != null) future1.cancel(mayInterruptRunning);
        if (future2 != null) future2.cancel(mayInterruptRunning);
        if (future3 != null) future3.cancel(mayInterruptRunning);
        isCancelled = true;
        FILE_OBJECT_TYPE = null;
    }

    private boolean isCancelled() {
        return isCancelled;
    }

    public synchronized void populateApps() {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                systemAppPOJOList = new ArrayList<>();
                userAppPOJOList = new ArrayList<>();
                if (!repositoryClass.app_pojo_hashmap.containsKey("system")) {
                    repositoryClass.populateAppsList();
                }

                userAppPOJOList = repositoryClass.app_pojo_hashmap.get("user");
                systemAppPOJOList = repositoryClass.app_pojo_hashmap.get("system");
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });

    }

    public void back_up(List<String> files_selected_array, String dest_folder, FileObjectType destFileObjectType, List<String> new_name_list, Uri tree_uri, String tree_uri_path) {
        if (!ArchiveDeletePasteServiceUtil.WHETHER_TO_START_SERVICE_ON_USB(null, destFileObjectType)) {
            Global.print(application, application.getString(R.string.wait_till_completion_on_going_operation_on_usb));
            return;
        }

        if (isBackedUp.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
        FILE_OBJECT_TYPE = destFileObjectType;
        isBackedUp.setValue(AsyncTaskStatus.STARTED);

        List<String> dest_file_names = new ArrayList<>();
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future2 = executorService.submit(new Runnable() {
            @Override
            public void run() {

                if (destFilePOJOs == null) {
                    UsbFile currentUsbFile = null;
                    if (destFileObjectType == FileObjectType.USB_TYPE) {
                        if (MainActivity.usbFileRoot != null) {
                            try {
                                currentUsbFile = MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(dest_folder));
                            } catch (IOException e) {

                            }
                        }
                    }
                    FilePOJOUtil.FILL_FILE_POJO(new ArrayList<>(), new ArrayList<>(), destFileObjectType, dest_folder, currentUsbFile, false);
                    RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                    destFilePOJOs = repositoryClass.hashmap_file_pojo.get(destFileObjectType + dest_folder);
                }

                if (destFilePOJOs != null) {
                    for (FilePOJO filePOJO : destFilePOJOs) {
                        dest_file_names.add(filePOJO.getName());
                    }
                }

                List<String> overwritten_copied_file_name_list;
                boolean copy_result;
                final boolean cut = false;
                String current_file_name;
                final List<String> copied_files_name = new ArrayList<>();  //declared here instead of at Asynctask class to keep track of copied files in case replacement

                if (destFileObjectType == FileObjectType.ROOT_TYPE) {
                    isBackedUp.postValue(AsyncTaskStatus.COMPLETED);
                    return;
                }

                List<File> src_file_list = new ArrayList<>();
                for (String s : files_selected_array) {
                    File file = new File(s);
                    src_file_list.add(file);
                }

                int count = 0;
                overwritten_copied_file_name_list = new ArrayList<>(dest_file_names);
                for (File file : src_file_list) {
                    if (isCancelled() || file == null) {
                        isBackedUp.postValue(AsyncTaskStatus.COMPLETED);
                        return;
                    }

                    current_file_name = new_name_list.get(count);

                    FileModel destFileModel = FileModelFactory.getFileModel(dest_folder, destFileObjectType, tree_uri, tree_uri_path);
                    copy_result = FileUtil.copy_File_FileModel(file, destFileModel, current_file_name, cut, bytes_read);

                    String f_p = file.getAbsolutePath();
                    if (copy_result) {
                        copied_files_name.add(current_file_name);
                    }

                    files_selected_array.remove(f_p);
                    ++count;
                }

                if (!copied_files_name.isEmpty()) {
                    List<String> overwritten_copied_file_path_list = new ArrayList<>();
                    overwritten_copied_file_name_list.retainAll(copied_files_name);
                    for (String name : overwritten_copied_file_name_list) {
                        overwritten_copied_file_path_list.add(Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder, name));
                    }

                    FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder, copied_files_name, destFileObjectType, overwritten_copied_file_path_list);
                    Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION, LocalBroadcastManager.getInstance(application), null);
                    copied_files_name.clear();
                }
                FILE_OBJECT_TYPE = null;
                isBackedUp.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

}
