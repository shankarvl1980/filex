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
                else if(sourceFileObjectType==FileObjectType.FTP_TYPE)
                {
                    //if(Global.CHECK_OTHER_FTP_SERVER_CONNECTED(FtpClientRepository_old.getInstance().ftpClientForProgress))
                    {
                        FtpClientRepository ftpClientRepository=FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
                        FTPClient ftpClient= null;
                        try {
                            ftpClient = ftpClientRepository.getFtpClient();
                            FTPFile[] f_array=new FTPFile[size];
                            for(int i=0;i<size;++i)
                            {

                                FTPFile f = FileUtil.getFTPFileFromOtherFTPClient(ftpClient,files_selected_array.get(i));
                                f_array[i]=f;

                            }
                            populate(f_array,include_folder,source_folder);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        finally {
                            if (ftpClientRepository != null && ftpClient != null) {
                                ftpClientRepository.releaseFtpClient(ftpClient);
                            }
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
        Stack<Pair<FTPFile, String>> stack = new Stack<>();
        for (FTPFile f : source_list_files) {
            stack.push(new Pair<>(f, initialPath));
        }

        while (!stack.isEmpty()) {
            if (isCancelled()) {
                return;
            }

            Pair<FTPFile, String> pair = stack.pop();
            FTPFile f = pair.first;
            String path = pair.second;

            if (f == null) continue;

            int no_of_files = 0;
            long size_of_files = 0L;

            if (f.isDirectory()) {
                try {
                    String name = f.getName();
                    String newPath = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
                    FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
                    FTPClient ftpClient = ftpClientRepository.getFtpClient();
                    FTPFile[] subFiles = ftpClient.listFiles(newPath);
                    for (FTPFile subFile : subFiles) {
                        stack.push(new Pair<>(subFile, newPath));
                    }
                    ftpClientRepository.releaseFtpClient(ftpClient);
                } catch (Exception e) {
                    // Handle exception
                }
                if (include_folder) {
                    no_of_files++;
                }
            } else {
                no_of_files++;
                size_of_files += f.getSize();
            }

            total_no_of_files += no_of_files;
            total_size_of_files += size_of_files;
            mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
        }
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


//        private void populate(File[] source_list_files,boolean include_folder)
//        {
//            int size=source_list_files.length;
//            for(int i=0;i<size;++i)
//            {
//                File f=source_list_files[i];
//                if(isCancelled())
//                {
//                    return;
//                }
//                int no_of_files=0;
//                long size_of_files=0L;
//                if(f.isDirectory())
//                {
//                    if(f.list()!=null)
//                    {
//                        populate(f.listFiles(),include_folder);
//                    }
//                    if(include_folder)
//                    {
//                        no_of_files++;
//                    }
//                }
//                else
//                {
//                    no_of_files++;
//                    size_of_files+=f.length();
//                }
//                total_no_of_files+=no_of_files;
//                total_size_of_files+=size_of_files;
//
//                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
//            }
//        }
//
//        private void populate(UsbFile[] source_list_files, boolean include_folder)
//        {
//            int size=source_list_files.length;
//            for(int i=0;i<size;++i)
//            {
//                UsbFile f=source_list_files[i];
//                if(isCancelled())
//                {
//                    return;
//                }
//                int no_of_files=0;
//                long size_of_files=0L;
//                if(f.isDirectory())
//                {
//                    try {
//                        populate(f.listFiles(),include_folder);
//                    } catch (IOException e) {
//
//                    }
//                    if(include_folder)
//                    {
//                        no_of_files++;
//                    }
//                }
//                else
//                {
//                    no_of_files++;
//                    size_of_files+=f.getLength();
//                }
//                total_no_of_files+=no_of_files;
//                total_size_of_files+=size_of_files;
//                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
//            }
//        }
//
//        private void populate(FTPFile[] source_list_files, boolean include_folder,String path)
//        {
//            int size=source_list_files.length;
//            for(int i=0;i<size;++i)
//            {
//                FTPFile f=source_list_files[i];
//                if(isCancelled())
//                {
//                    return;
//                }
//                int no_of_files=0;
//                long size_of_files=0L;
//                if(f==null)continue;
//                if(f.isDirectory())
//                {
//                    try {
//                        String name=f.getName();
//                        path=Global.CONCATENATE_PARENT_CHILD_PATH(path,name);
//                        FtpClientRepository ftpClientRepository=FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
//                        FTPClient ftpClient= ftpClientRepository.getFtpClient();
//                        populate(ftpClient.listFiles(path),include_folder,path);
//                        ftpClientRepository.releaseFtpClient(ftpClient);
//
//                    } catch (Exception e) {
//
//                    }
//                    if(include_folder)
//                    {
//                        no_of_files++;
//                    }
//                }
//                else
//                {
//                    no_of_files++;
//                    size_of_files+=f.getSize();
//                }
//
//                total_no_of_files+=no_of_files;
//                total_size_of_files+=size_of_files;
//                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
//            }
//        }
//
//        private void populate(List<String> source_list_files,boolean include_folder)
//        {
//            int size=source_list_files.size();
//            for(int i=0;i<size;++i)
//            {
//                if(isCancelled())
//                {
//                    return;
//                }
//                int no_of_files=0;
//                long size_of_files=0L;
//                String parent_file_path=source_list_files.get(i);
//                Uri uri=FileUtil.getDocumentUri(parent_file_path,target_uri,target_uri_path);
//                if(FileUtil.isDirectoryUri(context,uri))
//                {
//                    Uri children_uri= DocumentsContract.buildChildDocumentsUriUsingTree(target_uri,FileUtil.getDocumentID(parent_file_path,target_uri,target_uri_path));
//                    Cursor cursor=context.getContentResolver().query(children_uri,new String[] {DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null);
//                    if(cursor!=null && cursor.getCount()>0)
//                    {
//                        List<String>inner_source_list_files=new ArrayList<>();
//                        while(cursor.moveToNext())
//                        {
//                            String displayName=cursor.getString(0);
//                            inner_source_list_files.add(Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,displayName));
//                        }
//                        cursor.close();
//                        populate(inner_source_list_files,include_folder);
//                    }
//
//                    if(include_folder)
//                    {
//                        no_of_files++;
//                    }
//                }
//                else
//                {
//                    no_of_files++;
//                    size_of_files+=FileUtil.getSizeUri(context,uri);
//                }
//                total_no_of_files+=no_of_files;
//                total_size_of_files+=size_of_files;
//                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
//
//            }
//        }

}
