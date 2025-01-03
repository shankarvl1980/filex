package svl.kadatha.filex;

import android.app.Application;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.os.EnvironmentCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import me.jahnen.libaums.core.fs.UsbFile;

public class FilePOJOViewModel extends AndroidViewModel {
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    final List<FilePOJO> path = new ArrayList<>();
    private final Application application;
    public List<FilePOJO> filePOJOS, filePOJOS_filtered;
    public IndexedLinkedHashMap<Integer, String> mselecteditems = new IndexedLinkedHashMap<>();
    public String file_type = "f";
    public String library_filter_path;
    public boolean library_time_desc = false, library_size_desc = false;
    int count;
    int dir_count;
    private boolean isCancelled = false;
    private Future<?> future1, future2, future3, future4, future5, future6, future7, future8, future9, future10, future11;
    private String what_to_find = null;
    private String media_category = null;

    public FilePOJOViewModel(@NonNull Application application) {
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
        if (future10 != null) {
            future10.cancel(mayInterruptRunning);
        }
        if (future11 != null) {
            future11.cancel(mayInterruptRunning);
        }
        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
    }


    public synchronized void populateFilePOJO(FileObjectType fileObjectType, String fileclickselected, UsbFile currentUsbFile, boolean archive_view, boolean fill_file_size_also) {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();

        future1 = executorService.submit(() -> {
            filePOJOS = new ArrayList<>();
            filePOJOS_filtered = new ArrayList<>();
            FilePOJOUtil.FILL_FILE_POJO(filePOJOS, filePOJOS_filtered, fileObjectType, fileclickselected, currentUsbFile, archive_view);

            if (fill_file_size_also) {
                long storage_space = 0L;
                String key = fileObjectType + fileclickselected;

                // Determine storage space
                for (Map.Entry<String, SpacePOJO> entry : Global.SPACE_ARRAY.entrySet()) {
                    if (Global.IS_CHILD_FILE(key, entry.getKey())) {
                        storage_space = entry.getValue().getTotalSpace();
                        break;
                    }
                }

                final long final_storage_space = storage_space;

                dir_count = filePOJOS.size();
                try {
                    // Process file sizes using the helper method
                    processFileSizes(final_storage_space, fileclickselected, 0, dir_count);
                } catch (InterruptedException | ExecutionException e) {
                }
            }
            asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
        });
    }


    public synchronized void fill_filePOJOs_size(FileObjectType fileObjectType, String fileclickselected, UsbFile currentUsbFile) {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();

        future2 = executorService.submit(() -> {
            long storage_space = 0L;
            String key = fileObjectType + fileclickselected;

            // Determine storage space
            for (Map.Entry<String, SpacePOJO> entry : Global.SPACE_ARRAY.entrySet()) {
                if (Global.IS_CHILD_FILE(key, entry.getKey())) {
                    storage_space = entry.getValue().getTotalSpace();
                    break;
                }
            }

            final long final_storage_space = storage_space;

            // Count the number of directories (assuming all are directories)
            int numDirs = filePOJOS.size();

            try {
                // Process file sizes using the helper method
                processFileSizes(final_storage_space, fileclickselected, 0, numDirs);
            } catch (InterruptedException | ExecutionException e) {
                // Optionally, set the asyncTaskStatus to FAILED
                // asyncTaskStatus.postValue(AsyncTaskStatus.FAILED);
                return;
            } finally {
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    private void processFileSizes(long storageSpace, String fileClickSelected, int start, int end) throws InterruptedException, ExecutionException {
        // Count the number of directories
        int numDirs = 0;
        for (int i = start; i < end; i++) {
            if (filePOJOS.get(i).getIsDirectory()) {
                numDirs++;
            } else {
                break; // Assuming sorted: directories first
            }
        }

        // Calculate the midpoint to split directories
        int halfDirs = numDirs / 2;

        // Define index ranges for the two threads
        int endThread1 = start + halfDirs;

        int startThread2 = start + halfDirs;

        // Create two separate tasks for the threads
        Runnable task1 = () -> {
            fill_file_size(storageSpace, fileClickSelected, start, endThread1);
        };

        Runnable task2 = () -> {
            fill_file_size(storageSpace, fileClickSelected, startThread2, end);
        };

        // Submit both tasks to the executor service
        Future<?> futureTask1 = MyExecutorService.getExecutorService().submit(task1);
        Future<?> futureTask2 = MyExecutorService.getExecutorService().submit(task2);

        // Wait for both tasks to complete
        futureTask1.get();
        futureTask2.get();
    }

    private void fill_file_size(long volume_storage_size, String fileclickselected, int start, int end) {
        if (filePOJOS == null) {
            return;
        }
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        int[] total_no_of_files = new int[1];
        long[] total_size_of_files = new long[1];
        ConcurrentHashMap<String, FileStoragePOJO> hashMap_directory_size;
        if (Global.IS_CHILD_FILE(fileclickselected, Global.INTERNAL_PRIMARY_STORAGE_PATH)) {
            hashMap_directory_size = repositoryClass.hashmap_internal_directory_size;
        } else {
            hashMap_directory_size = repositoryClass.hashmap_external_directory_size;
        }

        for (int i = start; i < end; ++i) {
            if (isCancelled()) {
                return;
            }
            FilePOJO filePOJO = filePOJOS.get(i);
            total_no_of_files[0] = 0;
            total_size_of_files[0] = 0;
            FileStoragePOJO fileStoragePOJO = hashMap_directory_size.get(filePOJO.getPath());
            if (fileStoragePOJO == null) {
                if (filePOJO.getIsDirectory()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        try {
                            new NioFileIteratorForSize(filePOJO.getPath(), hashMap_directory_size);
                        } catch (IOException e) {
                        }
                    } else {
                        get_size_non_nio(new File(filePOJO.getPath()), total_no_of_files, total_size_of_files, true, hashMap_directory_size);
                    }
                }
            }

            if (filePOJO.getIsDirectory()) {
                Iterator<Map.Entry<String, FileStoragePOJO>> iterator = hashMap_directory_size.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, FileStoragePOJO> entry = iterator.next();
                    if (entry.getKey().startsWith(filePOJO.getPath())) {
                        total_no_of_files[0] += entry.getValue().number_of_files;
                        total_size_of_files[0] += entry.getValue().total_size;
                    }
                }

                filePOJO.setTotalFiles(total_no_of_files[0]);
                filePOJO.setTotalSizeLong(total_size_of_files[0]);
                filePOJO.setTotalSize(FileUtil.humanReadableByteCount(total_size_of_files[0]));
                double percentage = 0;
                if (volume_storage_size != 0) {
                    percentage = total_size_of_files[0] * 100.0 / volume_storage_size;
                }
                filePOJO.setTotalSizePercentageDouble(percentage);
                filePOJO.setTotalSizePercentage(String.format("%.2f", percentage) + "%");
            } else {
                double percentage = 0;
                if (volume_storage_size != 0) {
                    percentage = filePOJO.getSizeLong() * 100.0 / volume_storage_size;
                }
                filePOJO.setTotalSizePercentageDouble(percentage);
                filePOJO.setTotalSizePercentage(String.format("%.2f", percentage) + "%");
            }
        }
    }

    private void get_size_non_nio(File f, int[] total_no_of_files, long[] total_size_of_files, boolean include_folder, ConcurrentHashMap<String, FileStoragePOJO> storage_hashmap) {
        Stack<File> stack = new Stack<>();
        stack.push(f);

        while (!stack.isEmpty() && !isCancelled()) {
            File current = stack.pop();
            int no_of_files = 0;
            long size_of_files = 0L;

            if (current.isDirectory()) {
                File[] files_array = current.listFiles();
                if (files_array != null && files_array.length != 0) {
                    for (File file : files_array) {
                        stack.push(file);
                    }
                    if (include_folder) {
                        ++no_of_files;
                    }
                    storage_hashmap.put(current.getAbsolutePath(), new FileStoragePOJO(total_no_of_files[0] + 1, total_size_of_files[0]));
                }
            } else {
                ++no_of_files;
                size_of_files += current.length();
            }

            total_no_of_files[0] += no_of_files;
            total_size_of_files[0] += size_of_files;
        }
    }

    public synchronized void getLibraryList(String media_category) {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future3 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                filePOJOS = new ArrayList<>();
                filePOJOS_filtered = new ArrayList<>();
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                switch (media_category) {
                    case "Download":
                        repositoryClass.getDownLoadList(isCancelled);
                        break;
                    case "Document":
                        repositoryClass.getDocumentList(isCancelled);
                        break;
                    case "Image":
                        repositoryClass.getImageList(isCancelled);
                        break;
                    case "Audio":
                        repositoryClass.getAudioList(isCancelled);
                        break;
                    case "Video":
                        repositoryClass.getVideoList(isCancelled);
                        break;
                    case "Archive":
                        repositoryClass.getArchiveList(isCancelled);
                        break;
                    case "APK":
                        repositoryClass.getApkList(isCancelled);
                        break;
                    case "Large Files":
                        repositoryClass.getLargeFileList(isCancelled);
                        break;
                    case "Duplicate Files":
                        repositoryClass.getDuplicateFileList(isCancelled);
                        break;
                }
                filePOJOS = repositoryClass.hashmap_file_pojo.get(FileObjectType.SEARCH_LIBRARY_TYPE + media_category);
                filePOJOS_filtered = repositoryClass.hashmap_file_pojo_filtered.get(FileObjectType.SEARCH_LIBRARY_TYPE + media_category);
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void populateLibrarySearchFilePOJO(FileObjectType fileObjectType, Set<FilePOJO> search_in_dir, String library_or_search, String fileclickselected, String search_file_name, String search_file_type, boolean search_whole_word, boolean search_case_sensitive, boolean search_regex, long search_lower_limit_size, long search_upper_limit_size) {
        if (asyncTaskStatus.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            return;
        }
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        count = 0;
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future4 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (filePOJOS != null) {
                    Iterator<FilePOJO> iterator = filePOJOS.iterator();
                    while (iterator.hasNext()) {
                        if (isCancelled()) {
                            break;
                        }
                        FilePOJO filePOJO = iterator.next();
                        if (!new File(filePOJO.getPath()).exists()) {
                            iterator.remove();
                        }
                    }

                    filePOJOS_filtered = filePOJOS;
                    asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                    return;
                }

                filePOJOS = new ArrayList<>();
                filePOJOS_filtered = new ArrayList<>();
                if (library_or_search.equals(DetailFragment.SEARCH_RESULT)) {
                    for (FilePOJO f : search_in_dir) {
                        if (f.getFileObjectType() == FileObjectType.FILE_TYPE && Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(new File(f.getPath())))) {
                            path.add(f);
                        }
                    }
                    if (search_regex) {
                        what_to_find = search_file_name;
                    } else if (search_whole_word) {
                        what_to_find = "\\Q" + search_file_name + "\\E";
                        if (!search_case_sensitive) {
                            what_to_find = "(?i)\\Q" + search_file_name + "\\E";
                        }
                    } else {
                        what_to_find = ".*(\\Q" + search_file_name + "\\E).*";
                        if (!search_case_sensitive) {
                            what_to_find = ".*((?i)\\Q" + search_file_name + "\\E).*";
                        }
                    }
                    file_type = search_file_type;
                } else {
                    RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                    for (FilePOJO f : repositoryClass.storage_dir) {
                        if (f.getFileObjectType() == FileObjectType.FILE_TYPE && Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(new File(f.getPath())))) {
                            path.add(f);
                        }
                    }
                    if (application.getString(R.string.document).equals(library_or_search)) {
                        what_to_find = ".*((?i)\\.doc|\\.docx|\\.txt|\\.pdf|\\.html|\\.htm|\\.rtf|\\.ppt|\\.pptx|\\.xls|\\.xlsx)$";
                        media_category = "Document";
                    } else if (application.getString(R.string.image).equals(library_or_search)) {
                        what_to_find = ".*((?i)\\.png|\\.jpg|\\.jpeg|\\.gif|\\.tif|\\.svg|\\.webp|\\.avif)$";
                        media_category = "Image";
                    } else if (application.getString(R.string.audio).equals(library_or_search)) {
                        what_to_find = ".*((?i)\\.mp3|\\.ogg|\\.wav|\\.aac|\\.wma|\\.opus|\\.m4r|\\.m4a|\\.awb)$";
                        media_category = "Audio";
                    } else if (application.getString(R.string.video).equals(library_or_search)) {
                        what_to_find = ".*((?i)\\.3gp|\\.mp4|\\.avi|\\.mov|\\.flv|\\.wmv|\\.webm)$";
                        media_category = "Video";
                    } else if (application.getString(R.string.archive).equals(library_or_search)) {
                        what_to_find = ".*((?i)\\.zip|\\.rar|\\.tar|\\.gz|\\.gzip|\\.jar|\\.7z)$";
                        media_category = "Archive";
                    } else if (application.getString(R.string.apk).equals(library_or_search)) {
                        what_to_find = ".*(?i)\\.apk$";
                        media_category = "APK";
                    } else if (application.getString(R.string.download).equals(library_or_search)) {
                        what_to_find = ".*";
                        media_category = "Download";
                    }
                }

                if (Global.DETAILED_SEARCH_LIBRARY) {
                    if (media_category != null && media_category.equals("Download")) {
                        search_download(filePOJOS, filePOJOS_filtered);
                    } else {
                        start_distributed_search(search_lower_limit_size, search_upper_limit_size);
                    }
                } else {
                    if (media_category != null && media_category.equals("Download")) {
                        search_download(filePOJOS, filePOJOS_filtered);
                    } else if (library_or_search.equals(DetailFragment.SEARCH_RESULT)) {
                        start_distributed_search(search_lower_limit_size, search_upper_limit_size);
                    } else {
                        search_file(filePOJOS, filePOJOS_filtered);
                    }
                }
                RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
                repositoryClass.hashmap_file_pojo.put(fileObjectType + fileclickselected, filePOJOS);
                repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType + fileclickselected, filePOJOS_filtered);
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    private void start_distributed_search(long search_lower_limit_size, long search_upper_limit_size) {
        // Collect first-level subdirectories and files from the given paths
        List<String> directories = new ArrayList<>();
        List<String> files = new ArrayList<>();
        for (FilePOJO f : path) {
            collectFirstLevelDirectoriesAndFiles(f.getPath(), directories, files);
        }

        // Split directories into two halves
        int dirSize = directories.size();
        int midIndex = dirSize / 2;

        // First half of directories
        List<String> list1 = new ArrayList<>(directories.subList(0, midIndex));

        // Second half of directories plus all files
        List<String> list2 = new ArrayList<>(directories.subList(midIndex, dirSize));
        list2.addAll(files);

        // Create two background threads
        List<FilePOJO> filePOJOS1 = new ArrayList<>();
        List<FilePOJO> filePOJOS_filtered1 = new ArrayList<>();
        List<FilePOJO> filePOJOS2 = new ArrayList<>();
        List<FilePOJO> filePOJOS_filtered2 = new ArrayList<>();

        ExecutorService executor1 = MyExecutorService.getExecutorService();
        ExecutorService executor2 = MyExecutorService.getExecutorService();

        future5 = executor1.submit(() -> {
            for (String path : list1) {
                if (isCancelled()) {
                    return;
                }
                if (search_upper_limit_size == 0L && search_lower_limit_size == 0L) {
                    search_file(what_to_find, file_type, path, filePOJOS1, filePOJOS_filtered1);
                } else {
                    search_file(what_to_find, file_type, path, filePOJOS1, filePOJOS_filtered1, search_lower_limit_size, search_upper_limit_size);
                }
            }
        });

        future6 = executor2.submit(() -> {
            for (String path : list2) {
                if (isCancelled()) {
                    return;
                }
                if (search_upper_limit_size == 0L && search_lower_limit_size == 0L) {
                    search_file(what_to_find, file_type, path, filePOJOS2, filePOJOS_filtered2);
                } else {
                    search_file(what_to_find, file_type, path, filePOJOS2, filePOJOS_filtered2, search_lower_limit_size, search_upper_limit_size);
                }
            }
        });

        // Wait for both threads to complete
        try {
            future5.get();
            future6.get();
        } catch (InterruptedException | ExecutionException e) {
            // Handle exceptions
        }

        // Combine results
        filePOJOS.addAll(filePOJOS1);
        filePOJOS.addAll(filePOJOS2);

        filePOJOS_filtered.addAll(filePOJOS_filtered1);
        filePOJOS_filtered.addAll(filePOJOS_filtered2);
    }

    private void collectFirstLevelDirectoriesAndFiles(String dirPath, List<String> directories, List<String> files) {
        File dir = new File(dirPath);
        if (dir.isDirectory()) {
            File[] list = dir.listFiles();
            if (list != null) {
                for (File f : list) {
                    if (f.isDirectory()) {
                        directories.add(f.getAbsolutePath());
                    } else if (f.isFile()) {
                        files.add(f.getAbsolutePath());
                    }
                }
            }
        } else if (dir.isFile()) {
            files.add(dir.getAbsolutePath());
        }
    }


    private void search_file(List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered) {
        if (media_category == null) {
            return;
        }
        Cursor cursor = null;
        switch (media_category) {
            case "Download":
                search_download(f_pojos, f_pojos_filtered);
                break;
            case "Document":
                cursor = application.getContentResolver().query(MediaStore.Files.getContentUri("external"), new String[]{MediaStore.Files.FileColumns.DATA},
                        "(" +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + ")",

                        new String[]{"%.doc", "%.docx", "%.txt", "%.pdf", "%.html", "%.htm", "%.rtf", "%.ppt", "%.pptx", "%.xls", "%.xlsx"}, null);
                break;
            case "Archive":
                cursor = application.getContentResolver().query(MediaStore.Files.getContentUri("external"), new String[]{MediaStore.Files.FileColumns.DATA},
                        "(" +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + ")",
                        new String[]{"%.tar", "%.gzip", "%.gz", "%.zip", "%.rar", "%.jar", "%.7z"}, null);
                break;
            case "APK":
                cursor = application.getContentResolver().query(MediaStore.Files.getContentUri("external"), new String[]{MediaStore.Files.FileColumns.DATA},
                        "(" +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + ")",
                        new String[]{"%.apk"}, null);
                break;
            case "Image":
                cursor = application.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                break;
            case "Audio":
                cursor = application.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media.DATA}, null, null, null);
                break;
            case "Video":
                cursor = application.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Video.Media.DATA}, null, null, null);
                break;
            default:
                break;
        }


        if (cursor != null && cursor.getCount() > 0) {
            boolean extract_icon = media_category != null && media_category.equals("APK");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                while (cursor.moveToNext()) {
                    if (isCancelled) {
                        return;
                    }

                    String data = cursor.getString(0);
                    Path path = Paths.get(data);
                    {
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(path, extract_icon, FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        count++;
                    }
                }
            } else {
                while (cursor.moveToNext()) {
                    if (isCancelled()) {
                        return;
                    }
                    String data = cursor.getString(0);
                    File f = new File(data);
                    {
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, extract_icon, FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        count++;
                    }
                }
            }
            cursor.close();
        }
    }

    private void search_download(List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Path path = Paths.get("/storage/emulated/0/Download");
            if (Files.exists(path)) {
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
                    for (Path p : directoryStream) {
                        if (isCancelled) {
                            return;
                        }
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(p, true, FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        count++;
                    }
                } catch (IOException e) {

                }
            }
        } else {
            File f = new File("/storage/emulated/0/Download");
            if (f.exists()) {
                File[] file_list = f.listFiles();
                int size = file_list.length;
                for (int i = 0; i < size; ++i) {
                    if (isCancelled()) {
                        return;
                    }
                    File file = file_list[i];
                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(file, true, FileObjectType.FILE_TYPE);
                    f_pojos.add(filePOJO);
                    f_pojos_filtered.add(filePOJO);
                    count++;
                }
            }
        }
    }

    private void search_file(String search_name, String file_type, String search_dir, List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered) throws PatternSyntaxException {
        boolean extract_icon = media_category != null && media_category.equals("APK");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Path searchPath = Paths.get(search_dir);
            try {
                if (Files.isRegularFile(searchPath)) {
                    // search_dir is a file
                    BasicFileAttributes basicFileAttributes = Files.readAttributes(searchPath, BasicFileAttributes.class);
                    if ((file_type.equals("f") || file_type.equals("fd")) &&
                            Pattern.matches(search_name, searchPath.getFileName().toString())) {
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(searchPath, extract_icon, FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        count++;
                    }
                } else {
                    // search_dir is a directory
                    Files.walkFileTree(searchPath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
                            if ((file_type.equals("d") || file_type.equals("fd")) &&
                                    Pattern.matches(search_name, path.getFileName().toString())) {
                                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(path, extract_icon, FileObjectType.FILE_TYPE);
                                f_pojos.add(filePOJO);
                                f_pojos_filtered.add(filePOJO);
                                count++;
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
                            if ((file_type.equals("f") || file_type.equals("fd")) &&
                                    Pattern.matches(search_name, path.getFileName().toString())) {
                                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(path, extract_icon, FileObjectType.FILE_TYPE);
                                f_pojos.add(filePOJO);
                                f_pojos_filtered.add(filePOJO);
                                count++;
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path path, IOException e) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            } catch (PatternSyntaxException e) {
                Global.print_background_thread(application, e.getMessage());
            } catch (IOException e) {
                // Handle or log the IOException if necessary
            }
        } else {
            File file = new File(search_dir);
            if (file.isFile()) {
                // search_dir is a file
                if (isCancelled()) {
                    return;
                }
                try {
                    if ((file_type.equals("f") || file_type.equals("fd")) &&
                            Pattern.matches(search_name, file.getName())) {
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(file, extract_icon, FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        count++;
                    }
                } catch (PatternSyntaxException e) {
                    Global.print_background_thread(application, e.getMessage());
                }
            } else if (file.isDirectory()) {
                // search_dir is a directory
                File[] list = file.listFiles();
                if (list == null) {
                    return;
                }
                int size = list.length;
                for (int i = 0; i < size; ++i) {
                    File f = list[i];
                    if (isCancelled()) {
                        return;
                    }
                    try {
                        if (f.isDirectory()) {
                            if ((file_type.equals("d") || file_type.equals("fd")) &&
                                    Pattern.matches(search_name, f.getName())) {
                                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, extract_icon, FileObjectType.FILE_TYPE);
                                f_pojos.add(filePOJO);
                                f_pojos_filtered.add(filePOJO);
                                count++;
                            }
                            search_file(search_name, file_type, f.getPath(), f_pojos, f_pojos_filtered);
                        } else {
                            if ((file_type.equals("f") || file_type.equals("fd")) &&
                                    Pattern.matches(search_name, f.getName())) {
                                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, extract_icon, FileObjectType.FILE_TYPE);
                                f_pojos.add(filePOJO);
                                f_pojos_filtered.add(filePOJO);
                                count++;
                            }
                        }
                    } catch (PatternSyntaxException e) {
                        Global.print_background_thread(application, e.getMessage());
                    }
                }
            } else {
                // search_dir does not exist or is not accessible
            }
        }
    }

    private void search_file(String search_name, String file_type, String search_dir,
                             List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered,
                             long lower_limit_size, long upper_limit_size) throws PatternSyntaxException {
        boolean extract_icon = media_category != null && media_category.equals("APK");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Path searchPath = Paths.get(search_dir);
            try {
                if (Files.isRegularFile(searchPath)) {
                    // search_dir is a file
                    BasicFileAttributes basicFileAttributes = Files.readAttributes(searchPath, BasicFileAttributes.class);
                    long length = basicFileAttributes.size();
                    if ((file_type.equals("f") || file_type.equals("fd")) &&
                            Pattern.matches(search_name, searchPath.getFileName().toString()) &&
                            ((lower_limit_size == 0 || length >= lower_limit_size) &&
                                    (upper_limit_size == 0 || length <= upper_limit_size))) {
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(searchPath, extract_icon, FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        count++;
                    }
                } else if (Files.isDirectory(searchPath)) {
                    // search_dir is a directory
                    Files.walkFileTree(searchPath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
                            if ((file_type.equals("d") || file_type.equals("fd")) &&
                                    Pattern.matches(search_name, path.getFileName().toString())) {
                                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(path, extract_icon, FileObjectType.FILE_TYPE);
                                f_pojos.add(filePOJO);
                                f_pojos_filtered.add(filePOJO);
                                count++;
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
                            long length = basicFileAttributes.size();
                            if ((file_type.equals("f") || file_type.equals("fd")) &&
                                    Pattern.matches(search_name, path.getFileName().toString()) &&
                                    ((lower_limit_size == 0 || length >= lower_limit_size) &&
                                            (upper_limit_size == 0 || length <= upper_limit_size))) {
                                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(path, extract_icon, FileObjectType.FILE_TYPE);
                                f_pojos.add(filePOJO);
                                f_pojos_filtered.add(filePOJO);
                                count++;
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path path, IOException e) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else {
                    // search_dir does not exist or is not accessible
                }
            } catch (PatternSyntaxException e) {
                Global.print_background_thread(application, e.getMessage());
            } catch (IOException e) {
                // Handle or log the IOException if necessary
            }
        } else {
            File file = new File(search_dir);
            if (file.isFile()) {
                // search_dir is a file
                if (isCancelled()) {
                    return;
                }
                try {
                    long length = file.length();
                    if ((file_type.equals("f") || file_type.equals("fd")) &&
                            Pattern.matches(search_name, file.getName()) &&
                            ((lower_limit_size == 0 || length >= lower_limit_size) &&
                                    (upper_limit_size == 0 || length <= upper_limit_size))) {
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(file, extract_icon, FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        count++;
                    }
                } catch (PatternSyntaxException e) {
                    Global.print_background_thread(application, e.getMessage());
                }
            } else if (file.isDirectory()) {
                // search_dir is a directory
                File[] list = file.listFiles();
                if (list == null) {
                    return;
                }
                int size = list.length;
                for (int i = 0; i < size; ++i) {
                    File f = list[i];
                    if (isCancelled()) {
                        return;
                    }
                    try {
                        if (f.isDirectory()) {
                            if ((file_type.equals("d") || file_type.equals("fd")) &&
                                    Pattern.matches(search_name, f.getName())) {
                                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, extract_icon, FileObjectType.FILE_TYPE);
                                f_pojos.add(filePOJO);
                                f_pojos_filtered.add(filePOJO);
                                count++;
                            }
                            search_file(search_name, file_type, f.getPath(), f_pojos, f_pojos_filtered, lower_limit_size, upper_limit_size);
                        } else {
                            long length = f.length();
                            if ((file_type.equals("f") || file_type.equals("fd")) &&
                                    Pattern.matches(search_name, f.getName()) &&
                                    ((lower_limit_size == 0 || length >= lower_limit_size) &&
                                            (upper_limit_size == 0 || length <= upper_limit_size))) {
                                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, extract_icon, FileObjectType.FILE_TYPE);
                                f_pojos.add(filePOJO);
                                f_pojos_filtered.add(filePOJO);
                                count++;
                            }
                        }
                    } catch (PatternSyntaxException e) {
                        Global.print_background_thread(application, e.getMessage());
                    }
                }
            } else {
                // search_dir does not exist or is not accessible
            }
        }
    }


    static class FileStoragePOJO {
        final int number_of_files;
        final long total_size;

        FileStoragePOJO(int number_of_files, long total_size) {
            this.number_of_files = number_of_files;
            this.total_size = total_size;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static class NioFileIteratorForSize extends SimpleFileVisitor<Path> {
        private final int[] count = new int[1];
        private final long[] size = new long[1];
        private final ConcurrentHashMap<String, FilePOJOViewModel.FileStoragePOJO> storagePOJOHashMap;

        public NioFileIteratorForSize(String file_path, ConcurrentHashMap<String, FilePOJOViewModel.FileStoragePOJO> storagePOJOHashMap) throws IOException {
            this.storagePOJOHashMap = storagePOJOHashMap;
            Files.walkFileTree(Paths.get(file_path), this);
        }


        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
            ++count[0];
            size[0] += attributes.size();
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
            count[0] = 0;
            size[0] = 0;

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            ++count[0];
            String path = dir.toAbsolutePath().toString();
            storagePOJOHashMap.put(path, new FilePOJOViewModel.FileStoragePOJO(count[0], size[0]));
            count[0] = 0;
            size[0] = 0;
            return FileVisitResult.CONTINUE;
        }

        public int getCount() {
            return count[0];
        }

        public long getSize() {
            return size[0];
        }
    }
}
