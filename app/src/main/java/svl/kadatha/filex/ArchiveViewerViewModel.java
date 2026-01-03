package svl.kadatha.filex;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class ArchiveViewerViewModel extends AndroidViewModel {

    public final MutableLiveData<AsyncTaskStatus> isExtractionCompleted = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> isArchiveEntriesPopulated = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public boolean zipFileExtracted;
    public FileObjectType fileObjectType;
    ArrayList<String> zip_entries_array;
    String base_path = "";
    private boolean isCancelled;
    private Future<?> future1, future2, future3, future4, future5, future6, future7, future8, future9;

    public ArchiveViewerViewModel(@NonNull Application application) {
        super(application);
    }

    private static void populate_file_paths(List<String> file_path_array, List<File> file_array) {
        Stack<File> stack = new Stack<>();
        for (File f : file_array) {
            stack.push(f);
        }

        while (!stack.isEmpty()) {
            File currentFile = stack.pop();
            if (currentFile.isDirectory()) {
                File[] innerFiles = currentFile.listFiles();
                if (innerFiles == null || innerFiles.length == 0) {
                    file_path_array.add(currentFile.getAbsolutePath() + File.separator);
                } else {
                    for (File innerFile : innerFiles) {
                        stack.push(innerFile);
                    }
                }
            } else {
                file_path_array.add(currentFile.getAbsolutePath());
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.ARCHIVE_EXTRACT_DIR);
        FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(Global.ARCHIVE_EXTRACT_DIR.getAbsolutePath()), FileObjectType.FILE_TYPE);
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
        if (future4 != null) {
            future4.cancel(mayInterruptRunning);
        }
        if (future5 != null) {
            future5.cancel(mayInterruptRunning);
        }
        if (future6 != null) {
            future6.cancel(mayInterruptRunning);
        }
        if (future7 != null) {
            future7.cancel(mayInterruptRunning);
        }
        if (future8 != null) {
            future8.cancel(mayInterruptRunning);
        }
        if (future9 != null) {
            future9.cancel(mayInterruptRunning);
        }
        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
    }

    public synchronized void extractArchive(ZipFile zipfile) {
        if (isExtractionCompleted.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        isExtractionCompleted.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                zipFileExtracted = false;
                FileUtil.deleteNativeDirectory(Global.ARCHIVE_EXTRACT_DIR);
                FilePOJOUtil.REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(Global.ARCHIVE_EXTRACT_DIR.getAbsolutePath()), FileObjectType.FILE_TYPE);

                Enumeration<? extends ZipEntry> zip_entries = zipfile.entries();
                while (zip_entries.hasMoreElements()) {
                    ZipEntry zipentry = zip_entries.nextElement();
                    File f = new File(Global.ARCHIVE_EXTRACT_DIR, zipentry.getName());
                    if (zipentry.isDirectory() && !f.exists()) {
                        zipFileExtracted = f.mkdirs();
                    } else if (!zipentry.isDirectory()) {
                        if (!f.getParentFile().exists()) {
                            zipFileExtracted = f.getParentFile().mkdirs();
                        }
                        try {
                            zipFileExtracted = f.createNewFile();
                        } catch (IOException e) {
                            zipFileExtracted = false;
                        }
                    }
                }
                isExtractionCompleted.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void populateArchiveEntries(ArchiveViewFragment archiveViewFragment) {
        if (isArchiveEntriesPopulated.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        isArchiveEntriesPopulated.setValue(AsyncTaskStatus.STARTED);
        zip_entries_array = new ArrayList<>();
        base_path = "";
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future2 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                fileObjectType = archiveViewFragment.fileObjectType;
                int size = archiveViewFragment.viewModel.mselecteditems.size();
                if (size != 0) {
                    List<File> file_list = new ArrayList<>();
                    for (int i = 0; i < size; ++i) {
                        file_list.add(new File(archiveViewFragment.viewModel.mselecteditems.getValueAtIndex(i)));
                    }
                    populate_file_paths(zip_entries_array, file_list);
                    base_path = archiveViewFragment.fileclickselected.substring(Global.ARCHIVE_CACHE_DIR_LENGTH + 1);
                }
                isArchiveEntriesPopulated.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
}