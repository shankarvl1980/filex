package svl.kadatha.filex;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import me.jahnen.libaums.core.fs.UsbFile;

public class FileEditorViewModel extends AndroidViewModel {

    private Future<?> future1,future2,future3;
    public File file;
    public String source_folder;
    public boolean isWritable,isFileBig;
    public boolean fromArchiveView,fromThirdPartyApp;
    public Uri data;
    public FileObjectType fileObjectType;
    public final MutableLiveData<AsyncTaskStatus> isReadingFinished=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> initializedSetUp=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public int eol,altered_eol;
    public LinkedHashMap<Integer, Long> page_pointer_hashmap=new LinkedHashMap<>();
    public int current_page=0;
    public long current_page_end_point=0L;
    public boolean file_start,file_end;
    public File temporary_file_for_save;
    public String file_path;
    public boolean file_format_supported=true;
    public boolean updated=true,to_be_closed_after_save;
    public String action_after_save="";
    public FilePOJO currently_shown_file;
    public TextViewUndoRedoBatch textViewUndoRedo;
    private BufferedReader bufferedReader;
    private long file_pointer;
    public boolean fileRead;
    public StringBuilder stringBuilder;
    private boolean isCancelled;
    public boolean edit_mode;

    public FileEditorViewModel(@NonNull Application application) {
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
        isCancelled = true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }

    public synchronized void openFile(FileInputStream fileInputStream, long f_pointer)
    {
        if(isReadingFinished.getValue()!=AsyncTaskStatus.NOT_YET_STARTED) return;
        isReadingFinished.setValue(AsyncTaskStatus.STARTED);
        fileRead=false;
        this.file_pointer=f_pointer;
        stringBuilder=new StringBuilder();
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                textViewUndoRedo.clearHistory();
                file_start= file_pointer == 0L;
                try
                {

                    FileChannel fc=fileInputStream.getChannel();
                    fc.position(file_pointer);

                    ByteBuffer buf=ByteBuffer.allocate(FileEditorActivity.BUFFER_SIZE);
                    int bytes_read;
                    if(file_pointer!=0L)
                    {
                        boolean to_break=false;
                        while((bytes_read=fc.read(buf))!=-1)
                        {
                            buf.flip();
                            for(int i=0;i<bytes_read;++i)
                            {
                                char m=(char)buf.get(i);
                                char n=0;
                                if(i+1<bytes_read)
                                {
                                    n=(char)buf.get(i+1);
                                }

                                file_pointer++;
                                if(m==10)
                                {
                                    to_break=true;
                                    eol=altered_eol=FileEditorActivity.EOL_N;
                                    break;
                                }
                                else if(m==13)
                                {
                                    if(n==10)
                                    {
                                        file_pointer++;
                                        eol=altered_eol=FileEditorActivity.EOL_RN;

                                    }
                                    else
                                    {
                                        eol=altered_eol=FileEditorActivity.EOL_R;
                                    }
                                    to_break=true;
                                    break;
                                }
                            }

                            if(to_break)
                            {
                                break;
                            }
                        }
                        page_pointer_hashmap.put(current_page,file_pointer);
                    }

                    buf.clear();
                    fc.position(file_pointer);
                    bufferedReader=new BufferedReader(Channels.newReader(fc,"UTF-8"));
                    String line;
                    int count=0;
                    long br=0,total_bytes_read=0;
                    int eol_len=(eol==FileEditorActivity.EOL_RN) ? 2 : 1;
                    int max_lines_to_display = 500;
                    while((line=bufferedReader.readLine())!=null)
                    {
                        br+=line.getBytes().length+eol_len;
                        stringBuilder.append(line).append("\n");
                        count++;
                        if(count>= max_lines_to_display)
                        {
                            file_end=false;
                            total_bytes_read=file_pointer+br;

                            break;
                        }
                    }
                    
                    if(count< max_lines_to_display)
                    {
                        file_end=true;
                        total_bytes_read=file.length();
                    }

                    current_page++;
                    current_page_end_point=total_bytes_read;
                    page_pointer_hashmap.put(current_page,current_page_end_point);
                    fileRead=true;

                } catch(IOException e)
                {
                   fileRead=false;
                } finally
                {
                    try
                    {
                        fileInputStream.close();
                        if(bufferedReader!=null)
                        {
                            bufferedReader.close();
                        }

                    }
                    catch(IOException e){}
                }

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
                        File cache_file=new File(Global.USB_CACHE_DIR,file_path);
                        if(!cache_file.exists())
                        {
                            File parent_file=cache_file.getParentFile();
                            if(parent_file!=null)
                            {
                                FileUtil.mkdirsNative(parent_file);
                                FileUtil.createNativeNewFile(cache_file);
                                UsbFile targetUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,file_path);
                                if(targetUsbFile!=null)
                                {
                                    FileUtil.copy_UsbFile_File(targetUsbFile,cache_file,false,new long[]{1});
                                }
                            }

                        }

                        currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(cache_file,false,false,FileObjectType.FILE_TYPE);
                    }
                }
                else if(fileObjectType==FileObjectType.ROOT_TYPE)
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,false,FileObjectType.FILE_TYPE);
                }
                else
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,false,FileObjectType.FILE_TYPE);
                }
                initializedSetUp.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

}
