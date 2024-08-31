package svl.kadatha.filex;

import android.app.Application;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import timber.log.Timber;

public class FileEditorViewModel extends AndroidViewModel {

    private final Application application;
    private Future<?> future1,future2,future3,future4;
    public File file;
    public String source_folder;
    public boolean isWritable,isFileBig;
    public boolean fromThirdPartyApp;
    public Uri data;
    public FileObjectType fileObjectType;
    public final MutableLiveData<AsyncTaskStatus> isReadingFinished=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> initializedSetUp=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> saveContentInTempFile=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
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
        isCancelled = true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }



    private static final int CHUNK_SIZE = 1024 * 1024; // 1 MB chunks
    public static final int MAX_LINES_TO_DISPLAY = 200;

    public synchronized void openFile(FileInputStream fileInputStream, long filePointer, int pageNumber) {
        if (isReadingFinished.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
        isReadingFinished.setValue(AsyncTaskStatus.STARTED);

        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(() -> {
            try (FileChannel fc = fileInputStream.getChannel()) {
                fc.position(filePointer);
                ByteBuffer buffer = ByteBuffer.allocate(CHUNK_SIZE);
                StringBuilder chunk = new StringBuilder();
                int linesRead = 0;
                long totalBytesRead = filePointer;

                while (fc.read(buffer) != -1 && linesRead < MAX_LINES_TO_DISPLAY) {
                    buffer.flip();
                    while (buffer.hasRemaining() && linesRead < MAX_LINES_TO_DISPLAY) {
                        char c = (char) buffer.get();
                        chunk.append(c);
                        if (c == '\n') {
                            linesRead++;
                        }
                    }
                    totalBytesRead += buffer.position();
                    buffer.compact();
                }

                stringBuilder = chunk;
                fileRead = true;
                file_start = (filePointer == 0);
                file_end = (linesRead < MAX_LINES_TO_DISPLAY);


                current_page = pageNumber;
                current_page_end_point = totalBytesRead;
                page_pointer_hashmap.put(current_page, new PagePointer(filePointer, current_page_end_point));

                Iterator<Map.Entry<Integer, PagePointer>> iterator = page_pointer_hashmap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, PagePointer> entry = iterator.next();
                    if (entry.getKey() > current_page) {
                        iterator.remove();
                    }
                }

            } catch (IOException e) {
                Timber.e(e, "Error reading file");
                stringBuilder = new StringBuilder();
                fileRead = false;
            } finally {
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
                    if(MainActivity.usbFileRoot!=null)
                    {
                        File cache_file=Global.COPY_TO_USB_CACHE(file_path);
                        currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(cache_file,false,FileObjectType.FILE_TYPE);
                    }
                }
                else if(fileObjectType==FileObjectType.ROOT_TYPE)
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,FileObjectType.FILE_TYPE);
                }
                else if(fileObjectType==FileObjectType.FTP_TYPE)
                {
                    File cache_file=new File(Global.FTP_CACHE_DIR,file_path);//Global.COPY_TO_FTP_CACHE(file_path);
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(cache_file,false,FileObjectType.FILE_TYPE);
                }
                else
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,FileObjectType.FILE_TYPE);
                }
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




    public static class PagePointer implements Parcelable {
        long startPoint;
        long endPoint;

        PagePointer(long startPoint, long endPoint) {
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
    }

}
