package svl.kadatha.filex;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import me.jahnen.libaums.core.fs.UsbFile;

@SuppressWarnings("ALL")
public class ViewModelFileCount extends ViewModel {

    final MutableLiveData<Integer> total_no_of_files=new MutableLiveData<>();
    public long total_size_of_files;
    private int cumulative_no_of_files;
    final MutableLiveData<String> size_of_files_formatted=new MutableLiveData<>();
    private Future<?> future;
    private boolean isCancelled;
    public final MutableLiveData<AsyncTaskStatus>asyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    private void cancel(boolean mayInterruptRunning){
        if(future!=null) future.cancel(mayInterruptRunning);
        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }

    public synchronized void countFile(String source_folder, FileObjectType sourceFileObjectType, ArrayList<String> source_list_files , int size, boolean include_folder)
    {
       if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
       asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future=executorService.submit(new Runnable()
        {
            @Override
            public void run()
            {
                Global.SET_OTHER_FILE_PERMISSION("rwx",source_folder);
                if(sourceFileObjectType==FileObjectType.FILE_TYPE || sourceFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE || sourceFileObjectType==FileObjectType.ROOT_TYPE)
                {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                    {
                        try {
                            final int[] count = new int[1];
                            final long[] size = new long[1];
                            new NioFileIterator(source_list_files,count, size,total_no_of_files,size_of_files_formatted);
                            cumulative_no_of_files+=count[0];
                            total_size_of_files+=size[0];

                            total_no_of_files.postValue(cumulative_no_of_files);
                            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
                        } catch (IOException e) {

                        }
                    }
                    else
                    {
                        File[] f_array=new File[size];
                        for(int i=0;i<size;++i)
                        {
                            File f=new File(source_list_files.get(i));
                            f_array[i]=f;
                        }
                        populate(f_array,include_folder);
                    }
                }
                else if(sourceFileObjectType== FileObjectType.USB_TYPE)
                {
                    UsbFile[] f_array=new UsbFile[size];
                    for(int i=0;i<size;++i)
                    {
                        UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,source_list_files.get(i));
                        f_array[i]=f;
                    }
                    populate(f_array,include_folder);
                }
                else if(sourceFileObjectType==FileObjectType.FTP_TYPE)
                {
                    FTPFile[] f_array=new FTPFile[size];
                    if(Global.CHECK_OTHER_FTP_SERVER_CONNECTED(FtpClientRepository.getInstance().ftpClientForCount))
                    {
                        for(int i=0;i<size;++i)
                        {
                            FTPFile f = FileUtil.getFTPFileFromOtherFTPClient(FtpClientRepository.getInstance().ftpClientForCount,source_list_files.get(i));
                            f_array[i]=f;
                        }
                        populate(f_array,include_folder,source_folder);
                    }

                }
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }


    private void populate(File[] source_list_files, boolean include_folder)
    {
        int size=source_list_files.length;
        for(int i=0;i<size;++i)
        {
            File f=source_list_files[i];
            if(isCancelled())
            {
                return;
            }
            int no_of_files=0;
            long size_of_files=0L;
            if(f.isDirectory())
            {
                if(f.list()!=null)
                {
                    populate(f.listFiles(),include_folder);
                }
                if(include_folder)
                {
                    no_of_files++;
                }
            }
            else
            {
                no_of_files++;
                size_of_files+=f.length();
            }
            cumulative_no_of_files+=no_of_files;
            total_no_of_files.postValue(cumulative_no_of_files);
            total_size_of_files+=size_of_files;
            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
        }
    }

    private void populate(UsbFile[] source_list_files, boolean include_folder)
    {
        int size=source_list_files.length;
        for(int i=0;i<size;++i)
        {
            UsbFile f=source_list_files[i];
            if(isCancelled())
            {
                return;
            }
            int no_of_files=0;
            long size_of_files=0L;
            if(f.isDirectory())
            {
                try {
                    populate(f.listFiles(),include_folder);
                } catch (IOException e) {

                }
                if(include_folder)
                {
                    no_of_files++;
                }
            }
            else
            {
                no_of_files++;
                size_of_files+=f.getLength();
            }
            cumulative_no_of_files+=no_of_files;
            total_no_of_files.postValue(cumulative_no_of_files);
            total_size_of_files+=size_of_files;
            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
        }
    }

    private void populate(FTPFile[] source_list_files, boolean include_folder, String path)
    {
        int size=source_list_files.length;
        for(int i=0;i<size;++i)
        {
            FTPFile f=source_list_files[i];
            if(isCancelled())
            {
                return;
            }
            int no_of_files=0;
            long size_of_files=0L;
            if(f.isDirectory())
            {
                try {
                    String name=f.getName();
                    path=Global.CONCATENATE_PARENT_CHILD_PATH(path,name);
                    populate(FtpClientRepository.getInstance().ftpClientForCount.listFiles(path),include_folder,path);
                } catch (Exception e) {

                }
                if(include_folder)
                {
                    no_of_files++;
                }
            }
            else
            {
                no_of_files++;
                size_of_files+=f.getSize();
            }
            cumulative_no_of_files+=no_of_files;
            total_no_of_files.postValue(cumulative_no_of_files);
            total_size_of_files+=size_of_files;
            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
        }
    }

/*
    private void populate(List<String> source_list_files, boolean include_folder)
    {
        int size=source_list_files.size();
        for(int i=0;i<size;++i)
        {
            if(isCancelled())
            {
                return;
            }
            int no_of_files=0;
            long size_of_files=0L;
            String parent_file_path=source_list_files.get(i);
            Uri uri=FileUtil.getDocumentUri(parent_file_path,tree_uri,tree_uri_path);
            if(FileUtil.isDirectoryUri(context,uri))
            {
                Uri children_uri= DocumentsContract.buildChildDocumentsUriUsingTree(tree_uri,FileUtil.getDocumentID(parent_file_path,tree_uri,tree_uri_path));
                Cursor cursor=context.getContentResolver().query(children_uri,new String[] {DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null);
                if(cursor!=null && cursor.getCount()>0)
                {
                    List<String>inner_source_list_files=new ArrayList<>();
                    while(cursor.moveToNext())
                    {

                        String docID=cursor.getString(0);
                        String displayName=cursor.getString(1);
                        inner_source_list_files.add(Global.CONCATE(parent_file_path,displayName);

                    }
                    cursor.close();
                    populate(inner_source_list_files,include_folder);

                }

                if(include_folder)

                {
                    no_of_files++;
                }
            }
            else
            {
                no_of_files++;
                size_of_files+=FileUtil.getSize(context,uri);
            }
            cumulative_no_of_files+=no_of_files;
            total_no_of_files.postValue(cumulative_no_of_files);
            total_size_of_files+=size_of_files;
            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files,Global.BYTE_COUNT_BLOCK_1000));
            //publishProgress();
        }
    }

 */


}
