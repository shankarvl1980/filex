package svl.kadatha.filex;

import android.app.Application;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import timber.log.Timber;

public class FileEditorViewModel extends AndroidViewModel {

    private final Application application;
    private Future<?> future1,future2,future3,future4,future5;
    public File file;
    public String source_folder;
    public boolean isWritable,isFileBig;
    public boolean fromThirdPartyApp;
    public Uri data;
    public FileObjectType fileObjectType;
    public final MutableLiveData<AsyncTaskStatus> isReadingFinished=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> initializedSetUp=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> saveContentInTempFile=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> gotEOLofFile=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public int eol,altered_eol;
    public LinkedHashMap<Integer, PagePointer> page_pointer_hashmap=new LinkedHashMap<>();
    public int current_page=0;
    public long current_page_end_point=0L;
    public boolean file_start,file_end;
    public String file_path;
    public boolean file_format_supported=true;
    public boolean updated=true,to_be_closed_after_save;
    public String action_after_save="";
    public FilePOJO currently_shown_file;
    public TextViewUndoRedoBatch textViewUndoRedo;
    public boolean fileRead;
    public StringBuilder stringBuilder;
    private boolean isCancelled;
    public boolean edit_mode;
    public boolean is_eol_group_visible;
    public final static String temp_content_file_name="temp_content.txt";
    public boolean whether_temp_content_saved;

    public FileEditorViewModel(@NonNull Application application) {
        super(application);
        this.application=application;
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
        if(future4!=null) future4.cancel(mayInterruptRunning);
        if(future5!=null) future5.cancel(mayInterruptRunning);
        isCancelled = true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }


    private static final String TAG = "FileEditorViewModel";

    public static final int MAX_LINES_TO_DISPLAY = 200;
    private static final int MAX_LINE_LENGTH = 10000;

    public synchronized void openFile(FileInputStream fileInputStream, long filePointer, int pageNumber) {
        Timber.tag(TAG).d("Starting openFile: filePointer=%d, pageNumber=%d", filePointer, pageNumber);

        // Check if reading is already in progress
        if (isReadingFinished.getValue() != AsyncTaskStatus.NOT_YET_STARTED) {
            Timber.tag(TAG).w("openFile called while reading is already in progress");
            return;
        }

        // Set reading status to started
        isReadingFinished.setValue(AsyncTaskStatus.STARTED);

        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(() -> {
            try {
                // Skip to the correct byte position before creating the BufferedReader
                long skippedBytes = 0;
                while (skippedBytes < filePointer) {
                    long remainingToSkip = filePointer - skippedBytes;
                    long actualSkipped = fileInputStream.skip(remainingToSkip);
                    if (actualSkipped == 0) break;  // In case skipping fails
                    skippedBytes += actualSkipped;
                }
                Timber.tag(TAG).d("Skipped %d bytes to reach filePointer %d", skippedBytes, filePointer);

                // Now create the BufferedReader to start reading from the correct position
                BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
                StringBuilder chunk = new StringBuilder();
                int linesRead = 0;
                long totalBytesRead = 0;
                fileRead = true;
                file_end = false;
                int currentLineNumber = 1;
                long lastValidFilePointer = filePointer;  // Track the last valid file position

                String line;
                while ((line = reader.readLine()) != null && linesRead < MAX_LINES_TO_DISPLAY) {
                    // Check if line length exceeds the max allowed length
                    if (line.length() > MAX_LINE_LENGTH) {
                        Timber.tag(TAG).w("Line exceeds the maximum allowed length: %d", line.length());
                        fileRead = false;  // Abort reading
                        break;
                    }

                    // Append the line to the chunk
                    chunk.append(line).append(System.lineSeparator());
                    linesRead++;
                    long bytesRead = line.getBytes(StandardCharsets.UTF_8).length + System.lineSeparator().getBytes(StandardCharsets.UTF_8).length;
                    totalBytesRead += bytesRead;
                    lastValidFilePointer = filePointer + totalBytesRead;  // Update the last valid file pointer
                    Timber.tag(TAG).d("Processed line %d: %s", currentLineNumber, line);
                    currentLineNumber++;
                }

                // Check if we have more content to read
                if (line == null) {
                    file_end = true;
                }

                if (fileRead) {
                    // Successfully read the content
                    stringBuilder = chunk;
                    file_start = (filePointer == 0);

                    Timber.tag(TAG).d("Finished reading: totalBytesRead=%d, linesRead=%d, file_start=%b, file_end=%b, currentPosition=%d",
                            totalBytesRead, linesRead, file_start, file_end, lastValidFilePointer);

                    // Update current page information
                    current_page = pageNumber;
                    current_page_end_point = lastValidFilePointer;

                    page_pointer_hashmap.put(current_page, new PagePointer(filePointer, lastValidFilePointer));

                    // Remove future pages, as new content up to this page has been read
                    Iterator<Map.Entry<Integer, PagePointer>> iterator = page_pointer_hashmap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        if (iterator.next().getKey() > current_page) {
                            iterator.remove();
                        }
                    }

                    Timber.tag(TAG).d("Final page_pointer_hashmap: %s", page_pointer_hashmap);
                } else {
                    // Reading was aborted due to exceeding line length
                    Timber.tag(TAG).w("File reading aborted due to exceeding max line length or error");
                    stringBuilder = new StringBuilder();
                }

            } catch (IOException e) {
                // Handle IO exceptions during reading
                Timber.tag(TAG).e(e, "Error reading file: %s", e.getMessage());
                stringBuilder = new StringBuilder();
                fileRead = false;
            } finally {
                try {
                    // Ensure the FileInputStream is closed properly
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                } catch (IOException e) {
                    Timber.tag(TAG).e(e, "Error closing FileInputStream: %s", e.getMessage());
                }

                // Update the task status and log completion
                Timber.tag(TAG).d("openFile completed: fileRead=%b, current_page=%d, current_page_end_point=%d",
                        fileRead, current_page, current_page_end_point);
                isReadingFinished.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }



    public synchronized void setUpInitialization(FileObjectType fileObjectType,String file_path)
    {
        if (initializedSetUp.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
        initializedSetUp.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future2=executorService.submit(new Runnable() {
            @Override
            public void run() {
                file=new File(file_path);
                source_folder=file.getParent();
                isWritable=FileUtil.isWritable(fileObjectType,file_path);

                if(fileObjectType ==FileObjectType.USB_TYPE)
                {
                    File cache_file=Global.COPY_TO_USB_CACHE(file_path,fileObjectType);
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(cache_file,false,FileObjectType.FILE_TYPE);
                }
                else if(fileObjectType==FileObjectType.ROOT_TYPE)
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,FileObjectType.FILE_TYPE);
                }
                else if(fileObjectType==FileObjectType.FTP_TYPE)
                {
                    File cache_file=Global.COPY_TO_FTP_CACHE(file_path);
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(cache_file,false,FileObjectType.FILE_TYPE);
                }
                else
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,FileObjectType.FILE_TYPE);
                }

                if(fileObjectType==FileObjectType.USB_TYPE || fileObjectType==FileObjectType.FTP_TYPE)
                {
                    data = FileProvider.getUriForFile(application,Global.FILEX_PACKAGE+".provider",new File(currently_shown_file.getPath()));
                }
                determineEOL(data);
                initializedSetUp.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void saveContentInTempFile(String content){
        if (saveContentInTempFile.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
        saveContentInTempFile.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future3=executorService.submit(new Runnable() {
            @Override
            public void run() {
                File tempFile = new File(application.getExternalCacheDir(), temp_content_file_name);
                whether_temp_content_saved=false;
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(content);
                    whether_temp_content_saved=true;
                } catch (IOException e) {

                }
                saveContentInTempFile.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    private void determineEOL(Uri data) {
        eol = FileEditorActivity.EOL_N; // Default to \n
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(application.getContentResolver().openInputStream(data), StandardCharsets.UTF_8))) {
            int firstChar = reader.read();
            if (firstChar != -1) {
                if (firstChar == 13) { // \r
                    int secondChar = reader.read();
                    if (secondChar == 10) { // \n
                        eol = FileEditorActivity.EOL_RN;
                    } else {
                        eol = FileEditorActivity.EOL_R;
                    }
                } else if (firstChar != 10) { // Not \n
                    // Read until we find a line ending or EOF
                    int c;
                    while ((c = reader.read()) != -1) {
                        if (c == 10) { // \n
                            eol = FileEditorActivity.EOL_N;
                            break;
                        } else if (c == 13) { // \r
                            int nextChar = reader.read();
                            if (nextChar == 10) { // \n
                                eol = FileEditorActivity.EOL_RN;
                            } else {
                                eol = FileEditorActivity.EOL_R;
                            }
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            Timber.e(e, "Error determining EOL");
        }
    }



    public static class PagePointer implements Parcelable {
        private final long startPoint;
        private final long endPoint;

        public PagePointer(long startPoint, long endPoint) {
            this.startPoint = startPoint;
            this.endPoint = endPoint;
        }

        protected PagePointer(Parcel in) {
            startPoint = in.readLong();
            endPoint = in.readLong();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(startPoint);
            dest.writeLong(endPoint);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<PagePointer> CREATOR = new Creator<PagePointer>() {
            @Override
            public PagePointer createFromParcel(Parcel in) {
                return new PagePointer(in);
            }

            @Override
            public PagePointer[] newArray(int size) {
                return new PagePointer[size];
            }
        };

        public long getStartPoint() {
            return startPoint;
        }

        public long getEndPoint() {
            return endPoint;
        }

        @Override
        public String toString() {
            return "PagePointer{" +
                    "startPoint=" + startPoint +
                    ", endPoint=" + endPoint +
                    '}';
        }
    }

}
