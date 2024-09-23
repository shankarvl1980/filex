package svl.kadatha.filex;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.Future;

import me.jahnen.libaums.core.fs.UsbFile;
import timber.log.Timber;

@SuppressWarnings("ALL")
public class ViewModelFileCount extends ViewModel {
    private FileCountSize fileCountSize;
    private int cumulative_no_of_files;
    MutableLiveData<Integer> total_no_of_files=new MutableLiveData<>();
    public long total_size_of_files;
    MutableLiveData<String> size_of_files_formatted=new MutableLiveData<>();
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

    // Constructor where you instantiate and execute FileCountSize
    public ViewModelFileCount(Context context, List<String> files_selected_array, FileObjectType sourceFileObjectType) {
        // Instantiate FileCountSize within the ViewModel
        fileCountSize = new FileCountSize(context, files_selected_array, sourceFileObjectType);

        // Call the method in FileCountSize (which handles background execution)
        fileCountSize.fileCount();  // Assuming this method runs in a background thread

        // Optionally, you can observe and react to any changes in the file count/size
        fileCountSize.mutable_total_no_of_files.observeForever(totalFiles -> {
            total_no_of_files.postValue(totalFiles);
        });

        fileCountSize.mutable_size_of_files_to_be_archived_copied.observeForever(totalSize -> {
            size_of_files_formatted.postValue(totalSize);
        });
    }

    // Another constructor variant to instantiate FileCountSize with just a data list
    public ViewModelFileCount(Context context, List<Uri> data_list) {
        fileCountSize = new FileCountSize(context, data_list);

        // Call the method in FileCountSize (which handles background execution)
        fileCountSize.fileCountDatalist();  // Assuming this method runs in a background thread

        // Optionally, update LiveData as needed
        total_no_of_files.postValue(fileCountSize.total_no_of_files);
        size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(fileCountSize.total_size_of_files));
    }


//    public synchronized void countFile(String source_folder, FileObjectType sourceFileObjectType, ArrayList<String> source_list_files , int size, boolean include_folder)
//    {
//       if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
//       asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
//        ExecutorService executorService=MyExecutorService.getExecutorService();
//        future=executorService.submit(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                Global.SET_OTHER_FILE_PERMISSION("rwx",source_folder);
//                if(sourceFileObjectType==FileObjectType.FILE_TYPE || sourceFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE || sourceFileObjectType==FileObjectType.ROOT_TYPE)
//                {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//                    {
//                        try {
//                            final int[] count = new int[1];
//                            final long[] size = new long[1];
//                            new NioFileIterator(source_list_files,count, size,total_no_of_files,size_of_files_formatted);
//                            cumulative_no_of_files+=count[0];
//                            total_size_of_files+=size[0];
//
//                            total_no_of_files.postValue(cumulative_no_of_files);
//                            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
//                        } catch (IOException e) {
//
//                        }
//                    }
//                    else
//                    {
//                        File[] f_array=new File[size];
//                        for(int i=0;i<size;++i)
//                        {
//                            File f=new File(source_list_files.get(i));
//                            f_array[i]=f;
//                        }
//                        populate(f_array,include_folder);
//                    }
//                }
//                else if(sourceFileObjectType== FileObjectType.USB_TYPE)
//                {
//                    UsbFile[] f_array=new UsbFile[size];
//                    for(int i=0;i<size;++i)
//                    {
//                        UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,source_list_files.get(i));
//                        f_array[i]=f;
//                    }
//                    populate(f_array,include_folder);
//                }
//                else if(sourceFileObjectType==FileObjectType.FTP_TYPE)
//                {
//                    FTPFile[] f_array=new FTPFile[size];
//                    FtpClientRepository ftpClientRepository=FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
//                    FTPClient ftpClient = null;
//                    try {
//                        ftpClient=ftpClientRepository.getFtpClient();
//                        for(int i=0;i<size;++i)
//                        {
//                            FTPFile f = FileUtil.getFtpFile(ftpClient,source_list_files.get(i));
//                            f_array[i]=f;
//                        }
//                        populate(f_array,include_folder,source_folder);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                    finally {
//                        if (ftpClientRepository != null && ftpClient != null) {
//                            ftpClientRepository.releaseFtpClient(ftpClient);
//                        }
//                    }
//                }
//                else if(sourceFileObjectType == FileObjectType.SFTP_TYPE) {
//                    ChannelSftp.LsEntry[] ls_entries = new ChannelSftp.LsEntry[size];
//                    SftpChannelRepository sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
//                    ChannelSftp channelSftp = null;
//                    try {
//                        channelSftp = sftpChannelRepository.getSftpChannel();
//                        for(int i = 0; i < size; ++i) {
//                            String filePath = source_list_files.get(i);
//                            ChannelSftp.LsEntry entry = FileUtil.getSftpEntry(channelSftp, filePath);
//                            if(entry != null) {
//                                ls_entries[i] = entry;
//                            } else {
//                                Timber.tag("ViewModelFileCount").w("Skipping invalid path: %s", filePath);
//                                // Optionally, handle invalid paths as needed
//                            }
//                        }
//                        populateSFTP(ls_entries, include_folder, source_folder);
//                    } catch (Exception e) {
//                        Timber.tag("ViewModelFileCount").e("Exception during SFTP processing: %s", e.getMessage());
//                        throw new RuntimeException(e);
//                    }
//                    finally {
//                        if (sftpChannelRepository != null && channelSftp != null) {
//                            sftpChannelRepository.releaseChannel(channelSftp);
//                        }
//                    }
//                }
//                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
//            }
//        });
//    }




    public static class ViewModelFileCountFactory implements ViewModelProvider.Factory {
        private final Context context;
        private final List<String> filesSelectedArray;
        private final FileObjectType fileObjectType;

        // Constructor to accept the parameters for ViewModel
        public ViewModelFileCountFactory(Context context, List<String> filesSelectedArray, FileObjectType fileObjectType) {
            this.context = context;
            this.filesSelectedArray = filesSelectedArray;
            this.fileObjectType = fileObjectType;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ViewModelFileCount.class)) {
                // Create a new instance of ViewModelFileCount and pass the arguments
                return (T) new ViewModelFileCount(context, filesSelectedArray, fileObjectType);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }


//    private void populate(File[] source_list_files, boolean include_folder) {
//        Stack<File> stack = new Stack<>();
//        for (File f : source_list_files) {
//            stack.push(f);
//        }
//
//        while (!stack.isEmpty()) {
//            File f = stack.pop();
//            if (isCancelled()) {
//                return;
//            }
//
//            int no_of_files = 0;
//            long size_of_files = 0L;
//
//            if (f.isDirectory()) {
//                File[] subFiles = f.listFiles();
//                if (subFiles != null) {
//                    for (File subFile : subFiles) {
//                        stack.push(subFile);
//                    }
//                }
//                if (include_folder) {
//                    no_of_files++;
//                }
//            } else {
//                no_of_files++;
//                size_of_files += f.length();
//            }
//
//            cumulative_no_of_files += no_of_files;
//            total_no_of_files.postValue(cumulative_no_of_files);
//            total_size_of_files += size_of_files;
//            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
//        }
//    }
//
//    private void populate(UsbFile[] source_list_files, boolean include_folder) {
//        Stack<UsbFile> stack = new Stack<>();
//        for (UsbFile f : source_list_files) {
//            stack.push(f);
//        }
//
//        while (!stack.isEmpty()) {
//            if (isCancelled()) {
//                return;
//            }
//            UsbFile f = stack.pop();
//            int no_of_files = 0;
//            long size_of_files = 0L;
//
//            if (f.isDirectory()) {
//                try {
//                    UsbFile[] subFiles = f.listFiles();
//                    for (UsbFile subFile : subFiles) {
//                        stack.push(subFile);
//                    }
//                } catch (IOException e) {
//                    // Handle exception as needed
//                }
//                if (include_folder) {
//                    no_of_files++;
//                }
//            } else {
//                no_of_files++;
//                size_of_files += f.getLength();
//            }
//
//            cumulative_no_of_files += no_of_files;
//            total_no_of_files.postValue(cumulative_no_of_files);
//            total_size_of_files += size_of_files;
//            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
//        }
//    }
//
//    private void populate(FTPFile[] source_list_files, boolean include_folder, String initialPath) {
//        Timber.tag("ViewModelFileCount").d("Starting populate method with " + source_list_files.length + " files");
//
//        Stack<Pair<FTPFile, String>> stack = new Stack<>();
//        for (FTPFile f : source_list_files) {
//            stack.push(new Pair<>(f, initialPath));
//        }
//        Timber.tag("ViewModelFileCount").d( "Initial stack size: " + stack.size());
//        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
//        FTPClient ftpClient = null;
//        try {
//            ftpClient = ftpClientRepository.getFtpClient();
//            while (!stack.isEmpty()) {
//                if (isCancelled()) {
//                    Timber.tag("ViewModelFileCount").d( "Operation cancelled");
//                    return;
//                }
//
//                Pair<FTPFile, String> pair = stack.pop();
//                FTPFile f = pair.first;
//                String path = pair.second;
//
//                if (f == null) {
//                    Timber.tag("ViewModelFileCount").w("Null FTPFile encountered, skipping");
//                    continue;
//                }
//
//                Timber.tag("ViewModelFileCount").d("Processing file: " + f.getName() + " at path: " + path);
//
//                int no_of_files = 0;
//                long size_of_files = 0L;
//
//                if (f.isDirectory()) {
//                    Timber.tag("ViewModelFileCount").d( "Processing directory: " + f.getName());
//                    String name = f.getName();
//                    String newPath = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
//                    Timber.tag("ViewModelFileCount").d( "new folder path: " + newPath);
//
//                    FTPFile[] subFiles = ftpClient.listFiles(newPath);
//                    Timber.tag("ViewModelFileCount").d( "Subdirectory " + name + " contains " + subFiles.length + " files");
//                    for (FTPFile subFile : subFiles) {
//                        stack.push(new Pair<>(subFile, newPath));
//                    }
//
//
//                    if (include_folder) {
//                        no_of_files++;
//                        Timber.tag("ViewModelFileCount").d( "Including folder in count");
//                    }
//                } else {
//                    no_of_files++;
//                    size_of_files += f.getSize();
//                    Timber.tag("ViewModelFileCount").d( "File: " + f.getName() + ", Size: " + f.getSize());
//                }
//
//                cumulative_no_of_files += no_of_files;
//                total_no_of_files.postValue(cumulative_no_of_files);
//                total_size_of_files += size_of_files;
//                size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
//
//                Timber.tag("ViewModelFileCount").d( "Cumulative files: " + cumulative_no_of_files + ", Total size: " + total_size_of_files);
//            }
//
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        finally {
//            if (ftpClientRepository != null && ftpClient != null) {
//                ftpClientRepository.releaseFtpClient(ftpClient);
//            }
//        }
//
//        Timber.tag("ViewModelFileCount").d( "Populate method completed");
//    }
//
//    private void populateSFTP(ChannelSftp.LsEntry[] source_list_entries, boolean include_folder, String initialPath) {
//        Timber.tag("ViewModelFileCount").d("Starting populateSFTP method with " + source_list_entries.length + " entries");
//
//        Stack<Pair<ChannelSftp.LsEntry, String>> stack = new Stack<>();
//        for (ChannelSftp.LsEntry entry : source_list_entries) {
//            stack.push(new Pair<>(entry, initialPath));
//        }
//        Timber.tag("ViewModelFileCount").d("Initial stack size: " + stack.size());
//
//        SftpChannelRepository sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
//        ChannelSftp channelSftp = null;
//        try {
//            channelSftp = sftpChannelRepository.getSftpChannel();
//            if (channelSftp == null || !channelSftp.isConnected()) {
//                Timber.tag("ViewModelFileCount").e("SFTP channel is null or not connected.");
//                return;
//            }
//
//            while (!stack.isEmpty()) {
//                if (isCancelled()) { // Implement this method based on your cancellation logic
//                    Timber.tag("ViewModelFileCount").d("Operation cancelled");
//                    return;
//                }
//
//                Pair<ChannelSftp.LsEntry, String> pair = stack.pop();
//                ChannelSftp.LsEntry entry = pair.first;
//                String path = pair.second;
//
//                if (entry == null) {
//                    Timber.tag("ViewModelFileCount").w("Null LsEntry encountered, skipping");
//                    continue;
//                }
//
//                String entryName = entry.getFilename();
//                if (".".equals(entryName) || "..".equals(entryName)) {
//                    Timber.tag("ViewModelFileCount").d("Skipping special directory: " + entryName);
//                    continue;
//                }
//
//                Timber.tag("ViewModelFileCount").d("Processing entry: " + entryName + " at path: " + path);
//
//                int no_of_files = 0;
//                long size_of_files = 0L;
//
//                if (entry.getAttrs().isDir()) {
//                    Timber.tag("ViewModelFileCount").d("Processing directory: " + entryName);
//                    String newPath = combinePaths(path, entryName);
//                    Timber.tag("ViewModelFileCount").d("New folder path: " + newPath);
//
//                    try {
//                        @SuppressWarnings("unchecked")
//                        Vector<ChannelSftp.LsEntry> subEntries = channelSftp.ls(newPath);
//                        Timber.tag("ViewModelFileCount").d("Subdirectory " + entryName + " contains " + subEntries.size() + " entries");
//                        for (ChannelSftp.LsEntry subEntry : subEntries) {
//                            stack.push(new Pair<>(subEntry, newPath));
//                        }
//                    } catch (SftpException e) {
//                        Timber.tag("ViewModelFileCount").e("Error listing SFTP directory contents for path: %s, Error: %s", newPath, e.getMessage());
//                        continue;
//                    }
//
//                    if (include_folder) {
//                        no_of_files++;
//                        Timber.tag("ViewModelFileCount").d("Including folder in count");
//                    }
//                } else {
//                    no_of_files++;
//                    size_of_files += entry.getAttrs().getSize();
//                    Timber.tag("ViewModelFileCount").d("File: " + entryName + ", Size: " + entry.getAttrs().getSize());
//                }
//
//                // Update cumulative counts
//                cumulative_no_of_files += no_of_files;
//                total_no_of_files.postValue(cumulative_no_of_files);
//                total_size_of_files += size_of_files;
//                size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
//
//                Timber.tag("ViewModelFileCount").d("Cumulative files: " + cumulative_no_of_files + ", Total size: " + total_size_of_files);
//            }
//
//        } catch (Exception e) {
//            Timber.tag("ViewModelFileCount").e("Exception during SFTP populate: %s", e.getMessage());
//            throw new RuntimeException(e);
//        }
//        finally {
//            if (sftpChannelRepository != null && channelSftp != null) {
//                sftpChannelRepository.releaseChannel(channelSftp);
//            }
//        }
//
//        Timber.tag("ViewModelFileCount").d("populateSFTP method completed");
//    }
//
//
//    private String combinePaths(String dir, String file){
//        if(!dir.endsWith("/")){
//            dir += "/";
//        }
//        return dir + file;
//    }

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
