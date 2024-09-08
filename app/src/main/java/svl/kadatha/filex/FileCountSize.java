package svl.kadatha.filex;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import me.jahnen.libaums.core.fs.UsbFile;
import timber.log.Timber;

public class FileCountSize {
    Context context;
    List<String> files_selected_array;
    Uri target_uri;
    String target_uri_path;
    boolean include_folder;
    FileObjectType sourceFileObjectType;
    int total_no_of_files;
    long total_size_of_files;
    final MutableLiveData<String> mutable_size_of_files_to_be_archived_copied=new MutableLiveData<>();
    String source_folder;
    private boolean isCancelled;
    private Future<?> future1,future2,future3, future4;
    private static final String TAG = "Ftp-FileCountSize";

    FileCountSize(Context context,List<String> files_selected_array, Uri source_uri,String source_uri_path, FileObjectType sourceFileObjectType)
    {
        this.context=context;
        this.files_selected_array=files_selected_array;
        this.include_folder= true;
        this.target_uri= source_uri;
        this.target_uri_path= source_uri_path;
        this.sourceFileObjectType=sourceFileObjectType;

    }
    FileCountSize(int total_no_of_files, long total_size_of_files)
    {
        this.total_no_of_files=total_no_of_files;
        this.total_size_of_files=total_size_of_files;

    }

    public void cancel(boolean mayInterruptRunning){
        if(future1!=null) future1.cancel(mayInterruptRunning);
        if(future2!=null) future2.cancel(mayInterruptRunning);
        if(future3!=null) future3.cancel(mayInterruptRunning);
        if(future4!=null) future4.cancel(mayInterruptRunning);
        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }

    public void fileCount()
    {
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                if(context==null)
                {
                    mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
                    return;
                }
                source_folder=new File(files_selected_array.get(0)).getParent();
                int size=files_selected_array.size();
                if(sourceFileObjectType==FileObjectType.FILE_TYPE || sourceFileObjectType== FileObjectType.SEARCH_LIBRARY_TYPE || sourceFileObjectType==FileObjectType.ROOT_TYPE)
                {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                    {
                        try {
                            final int[] count = new int[1];
                            final long[] s = new long[1];
                            new NioFileIterator(files_selected_array,count, s,new MutableLiveData<>(),mutable_size_of_files_to_be_archived_copied);
                            total_no_of_files+=count[0];
                            total_size_of_files+=s[0];

                            mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));

                        } catch (IOException e) {

                        }
                    }
                    else
                    {
                        File[] f_array=new File[size];
                        for(int i=0;i<size;++i)
                        {
                            File f=new File(files_selected_array.get(i));
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
                        UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,files_selected_array.get(i));
                        f_array[i]=f;
                    }
                    populate(f_array,include_folder);
                }
                else if (sourceFileObjectType == FileObjectType.FTP_TYPE) {
                    Timber.tag(TAG).d("Starting file count for FTP files");
                    FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
                    FTPClient ftpClient = null;
                    try {
                        ftpClient = ftpClientRepository.getFtpClient();
                        Timber.tag(TAG).d("FTP client acquired");
                        FTPFile[] f_array = new FTPFile[size];
                        for (int i = 0; i < size; ++i) {
                            Timber.tag(TAG).d("Getting FTP file info for: %s", files_selected_array.get(i));
                            FTPFile f = FileUtil.getFtpFile(ftpClient, files_selected_array.get(i));
                            f_array[i] = f;
                        }
                        Timber.tag(TAG).d("Starting populate method for FTP files");
                        populate(f_array, include_folder, source_folder);
                    } catch (IOException e) {
                        Timber.tag(TAG).e("Error during FTP file count: %s", e.getMessage());
                        throw new RuntimeException(e);
                    } finally {
                        if (ftpClientRepository != null && ftpClient != null) {
                            ftpClientRepository.releaseFtpClient(ftpClient);
                            Timber.tag(TAG).d("FTP client released");
                        }
                    }
                }
                else
                {
                    populate(files_selected_array,include_folder);
                }
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

            total_no_of_files += no_of_files;
            total_size_of_files += size_of_files;

            mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
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
                    // Handle exception
                }
                if (include_folder) {
                    no_of_files++;
                }
            } else {
                no_of_files++;
                size_of_files += f.getLength();
            }

            total_no_of_files += no_of_files;
            total_size_of_files += size_of_files;
            mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
        }
    }

    private void populate(FTPFile[] source_list_files, boolean include_folder, String initialPath) {
        Timber.tag(TAG).d("Starting populate method for FTP files. Initial path: %s", initialPath);
        Stack<Pair<FTPFile, String>> stack = new Stack<>();
        for (FTPFile f : source_list_files) {
            stack.push(new Pair<>(f, initialPath));
        }

        while (!stack.isEmpty()) {
            if (isCancelled()) {
                Timber.tag(TAG).d("FTP file count cancelled");
                return;
            }

            Pair<FTPFile, String> pair = stack.pop();
            FTPFile f = pair.first;
            String path = pair.second;

            if (f == null) {
                Timber.tag(TAG).w("Null FTPFile encountered. Skipping.");
                continue;
            }

            int no_of_files = 0;
            long size_of_files = 0L;

            if (f.isDirectory()) {
                Timber.tag(TAG).d("Processing FTP directory: %s", f.getName());
                FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
                FTPClient ftpClient=null;
                try {
                    String name = f.getName();
                    String newPath = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
                    ftpClient = ftpClientRepository.getFtpClient();
                    Timber.tag(TAG).d("Listing files in FTP directory: %s", newPath);
                    FTPFile[] subFiles = ftpClient.listFiles(newPath);
                    Timber.tag(TAG).d("Found %d files in FTP directory: %s", subFiles.length, newPath);
                    for (FTPFile subFile : subFiles) {
                        stack.push(new Pair<>(subFile, newPath));
                    }
                } catch (Exception e) {
                    Timber.tag(TAG).e("Error processing FTP directory: %s", e.getMessage());
                }
                finally {
                    if (ftpClientRepository != null && ftpClient != null) {
                        ftpClientRepository.releaseFtpClient(ftpClient);
                        Timber.tag(TAG).d("FTP client released");
                    }
                }
                if (include_folder) {
                    no_of_files++;
                }
            } else {
                Timber.tag(TAG).d("Processing FTP file: %s, Size: %d", f.getName(), f.getSize());
                no_of_files++;
                size_of_files += f.getSize();
            }

            total_no_of_files += no_of_files;
            total_size_of_files += size_of_files;
            Timber.tag(TAG).d("Current totals - Files: %d, Size: %d", total_no_of_files, total_size_of_files);
            mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
        }
        Timber.tag(TAG).d("FTP file count completed. Total files: %d, Total size: %d", total_no_of_files, total_size_of_files);
    }
    private void populate(List<String> source_list_files, boolean include_folder) {
        Stack<String> stack = new Stack<>();
        for (String filePath : source_list_files) {
            stack.push(filePath);
        }

        while (!stack.isEmpty()) {
            if (isCancelled()) {
                return;
            }

            String parent_file_path = stack.pop();
            int no_of_files = 0;
            long size_of_files = 0L;

            Uri uri = FileUtil.getDocumentUri(parent_file_path, target_uri, target_uri_path);
            if (FileUtil.isDirectoryUri(context, uri)) {
                Uri children_uri = DocumentsContract.buildChildDocumentsUriUsingTree(target_uri, FileUtil.getDocumentID(parent_file_path, target_uri, target_uri_path));
                Cursor cursor = context.getContentResolver().query(children_uri, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        String displayName = cursor.getString(0);
                        stack.push(Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path, displayName));
                    }
                    cursor.close();
                }

                if (include_folder) {
                    no_of_files++;
                }
            } else {
                no_of_files++;
                size_of_files += FileUtil.getSizeUri(context, uri);
            }

            total_no_of_files += no_of_files;
            total_size_of_files += size_of_files;
            mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
        }
    }

}
