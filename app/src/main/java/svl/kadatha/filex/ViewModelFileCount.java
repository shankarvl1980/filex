package svl.kadatha.filex;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.concurrent.Future;

@SuppressWarnings("ALL")
public class ViewModelFileCount extends ViewModel {
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public long total_size_of_files;
    MutableLiveData<Integer> total_no_of_files = new MutableLiveData<>();
    MutableLiveData<String> size_of_files_formatted = new MutableLiveData<>();
    private FileCountSize fileCountSize;
    private int cumulative_no_of_files;
    private Future<?> future;
    private boolean isCancelled;

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

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    private void cancel(boolean mayInterruptRunning) {
        if (future != null) future.cancel(mayInterruptRunning);
        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
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
}
