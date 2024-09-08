package svl.kadatha.filex;

import android.os.Build;

import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import me.jahnen.libaums.core.fs.UsbFile;
import timber.log.Timber;

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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
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
                    FtpClientRepository ftpClientRepository=FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
                    FTPClient ftpClient = null;
                    try {
                        ftpClient=ftpClientRepository.getFtpClient();
                        //if(Global.CHECK_OTHER_FTP_SERVER_CONNECTED(FtpClientRepository_old.getInstance().ftpClientForCount))
                        {
                            for(int i=0;i<size;++i)
                            {
                                FTPFile f = FileUtil.getFtpFile(ftpClient,source_list_files.get(i));
                                f_array[i]=f;
                            }
                            populate(f_array,include_folder,source_folder);
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    finally {
                        if (ftpClientRepository != null && ftpClient != null) {
                            ftpClientRepository.releaseFtpClient(ftpClient);
                        }
                    }

                }
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }


    private void populate(File[] source_list_files, boolean include_folder) {
        Stack<File> stack = new Stack<>();
        for (File f : source_list_files) {
            stack.push(f);
        }

        while (!stack.isEmpty()) {
            File f = stack.pop();
            if (isCancelled()) {
                return;
            }

            int no_of_files = 0;
            long size_of_files = 0L;

            if (f.isDirectory()) {
                File[] subFiles = f.listFiles();
                if (subFiles != null) {
                    for (File subFile : subFiles) {
                        stack.push(subFile);
                    }
                }
                if (include_folder) {
                    no_of_files++;
                }
            } else {
                no_of_files++;
                size_of_files += f.length();
            }

            cumulative_no_of_files += no_of_files;
            total_no_of_files.postValue(cumulative_no_of_files);
            total_size_of_files += size_of_files;
            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
        }
    }

    private void populate(UsbFile[] source_list_files, boolean include_folder) {
        Stack<UsbFile> stack = new Stack<>();
        for (UsbFile f : source_list_files) {
            stack.push(f);
        }

        while (!stack.isEmpty()) {
            if (isCancelled()) {
                return;
            }
            UsbFile f = stack.pop();
            int no_of_files = 0;
            long size_of_files = 0L;

            if (f.isDirectory()) {
                try {
                    UsbFile[] subFiles = f.listFiles();
                    for (UsbFile subFile : subFiles) {
                        stack.push(subFile);
                    }
                } catch (IOException e) {
                    // Handle exception as needed
                }
                if (include_folder) {
                    no_of_files++;
                }
            } else {
                no_of_files++;
                size_of_files += f.getLength();
            }

            cumulative_no_of_files += no_of_files;
            total_no_of_files.postValue(cumulative_no_of_files);
            total_size_of_files += size_of_files;
            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
        }
    }

    private void populate(FTPFile[] source_list_files, boolean include_folder, String initialPath) {
        Timber.tag("ViewModelFileCount").d("Starting populate method with " + source_list_files.length + " files");

        Stack<Pair<FTPFile, String>> stack = new Stack<>();
        for (FTPFile f : source_list_files) {
            stack.push(new Pair<>(f, initialPath));
        }
        Timber.tag("ViewModelFileCount").d( "Initial stack size: " + stack.size());
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
        FTPClient ftpClient = null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();
            while (!stack.isEmpty()) {
                if (isCancelled()) {
                    Timber.tag("ViewModelFileCount").d( "Operation cancelled");
                    return;
                }

                Pair<FTPFile, String> pair = stack.pop();
                FTPFile f = pair.first;
                String path = pair.second;

                if (f == null) {
                    Timber.tag("ViewModelFileCount").w("Null FTPFile encountered, skipping");
                    continue;
                }

                Timber.tag("ViewModelFileCount").d("Processing file: " + f.getName() + " at path: " + path);

                int no_of_files = 0;
                long size_of_files = 0L;

                if (f.isDirectory()) {
                    Timber.tag("ViewModelFileCount").d( "Processing directory: " + f.getName());
                    String name = f.getName();
                    String newPath = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
                    Timber.tag("ViewModelFileCount").d( "new folder path: " + newPath);

                    FTPFile[] subFiles = ftpClient.listFiles(newPath);
                    Timber.tag("ViewModelFileCount").d( "Subdirectory " + name + " contains " + subFiles.length + " files");
                    for (FTPFile subFile : subFiles) {
                        stack.push(new Pair<>(subFile, newPath));
                    }


                    if (include_folder) {
                        no_of_files++;
                        Timber.tag("ViewModelFileCount").d( "Including folder in count");
                    }
                } else {
                    no_of_files++;
                    size_of_files += f.getSize();
                    Timber.tag("ViewModelFileCount").d( "File: " + f.getName() + ", Size: " + f.getSize());
                }

                cumulative_no_of_files += no_of_files;
                total_no_of_files.postValue(cumulative_no_of_files);
                total_size_of_files += size_of_files;
                size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files));

                Timber.tag("ViewModelFileCount").d( "Cumulative files: " + cumulative_no_of_files + ", Total size: " + total_size_of_files);
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
        }

        Timber.tag("ViewModelFileCount").d( "Populate method completed");
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
