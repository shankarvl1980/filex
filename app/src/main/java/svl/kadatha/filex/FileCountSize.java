package svl.kadatha.filex;

import android.content.Context;
import android.net.Uri;

import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;

import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import me.jahnen.libaums.core.fs.UsbFile;
import svl.kadatha.filex.asynctasks.CopyToAsyncTask;
import timber.log.Timber;

public class FileCountSize {
    private static final String TAG = "Ftp-FileCountSize";
    final Context context;
    final MutableLiveData<String> mutable_size_of_files_to_be_archived_copied = new MutableLiveData<>();
    final MutableLiveData<Integer> mutable_total_no_of_files = new MutableLiveData<>();
    List<String> files_selected_array;
    boolean include_folder;
    FileObjectType sourceFileObjectType;
    int total_no_of_files;
    long total_size_of_files;
    String source_folder;
    private boolean isCancelled;
    private List<Uri> data_list;
    private Future<?> future1, future2, future3, future4;

    FileCountSize(Context context, List<String> files_selected_array, FileObjectType sourceFileObjectType) {
        this.context = context;
        this.files_selected_array = files_selected_array;
        this.include_folder = true;
        this.sourceFileObjectType = sourceFileObjectType;
    }

    FileCountSize(Context context, List<Uri> data_list) {
        this.context = context;
        this.data_list = data_list;
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
        isCancelled = true;
    }

    private boolean isCancelled() {
        return isCancelled;
    }

    public void fileCountDatalist() {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future1 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                long uri_size = 0;
                for (Uri data : data_list) {
                    uri_size += CopyToAsyncTask.getLengthUri(context, data);
                }

                total_no_of_files += data_list.size();
                total_size_of_files += uri_size;
                mutable_total_no_of_files.postValue(total_no_of_files);
                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
            }
        });
    }

    public void fileCount() {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        future2 = executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (context == null) {
                    mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
                    return;
                }
                source_folder = new File(files_selected_array.get(0)).getParent();
                int size = files_selected_array.size();
                if (sourceFileObjectType == FileObjectType.FILE_TYPE || sourceFileObjectType == FileObjectType.SEARCH_LIBRARY_TYPE || sourceFileObjectType == FileObjectType.ROOT_TYPE) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        try {
                            final int[] count = new int[1];
                            final long[] s = new long[1];
                            new NioFileIterator(files_selected_array, count, s, mutable_total_no_of_files, mutable_size_of_files_to_be_archived_copied);
                            total_no_of_files += count[0];
                            total_size_of_files += s[0];
                            mutable_total_no_of_files.postValue(total_no_of_files);
                            mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));

                        } catch (IOException e) {

                        }
                    } else {
                        File[] f_array = new File[size];
                        for (int i = 0; i < size; ++i) {
                            File f = new File(files_selected_array.get(i));
                            f_array[i] = f;
                        }
                        populate(f_array, include_folder);
                    }
                } else if (sourceFileObjectType == FileObjectType.USB_TYPE) {
                    UsbFile[] f_array = new UsbFile[size];
                    for (int i = 0; i < size; ++i) {
                        UsbFile f = FileUtil.getUsbFile(MainActivity.usbFileRoot, files_selected_array.get(i));
                        f_array[i] = f;
                    }
                    populate(f_array, include_folder);
                } else if (sourceFileObjectType == FileObjectType.FTP_TYPE) {
                    Timber.tag(TAG).d("Starting file count for FTP files");
                    FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
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
                } else if (sourceFileObjectType == FileObjectType.SFTP_TYPE) {
                    ChannelSftp.LsEntry[] ls_entries = new ChannelSftp.LsEntry[size];
                    SftpChannelRepository sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
                    ChannelSftp channelSftp = null;
                    try {
                        channelSftp = sftpChannelRepository.getSftpChannel();
                        for (int i = 0; i < size; ++i) {
                            String filePath = files_selected_array.get(i);
                            ChannelSftp.LsEntry entry = FileUtil.getSftpEntry(channelSftp, filePath);
                            if (entry != null) {
                                ls_entries[i] = entry;
                            } else {
                                Timber.tag("ViewModelFileCount").w("Skipping invalid path: %s", filePath);
                                // Optionally, handle invalid paths as needed
                            }
                        }
                        populate(ls_entries, include_folder, source_folder);
                    } catch (Exception e) {
                        Timber.tag("ViewModelFileCount").e("Exception during SFTP processing: %s", e.getMessage());
                        throw new RuntimeException(e);
                    } finally {
                        if (sftpChannelRepository != null && channelSftp != null) {
                            sftpChannelRepository.releaseChannel(channelSftp);
                        }
                    }
                } else if (sourceFileObjectType == FileObjectType.WEBDAV_TYPE) {
                    Timber.tag(TAG).d("Starting file count for WebDAV files");

                    Sardine sardine = null;
                    String url;
                    try {
                        WebDavClientRepository webDavClientRepository = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
                        sardine = webDavClientRepository.getSardine();
                        Timber.tag(TAG).d("WebDAV client acquired");
                        List<DavResource> resources = new ArrayList<>();
                        for (int i = 0; i < size; ++i) {
                            Timber.tag(TAG).d("Getting WebDAV resource info for: %s", files_selected_array.get(i));
                            String fullPath = webDavClientRepository.getBasePath(sardine) + files_selected_array.get(i);
                            url = webDavClientRepository.buildUrl(fullPath);
                            List<DavResource> resourceList = sardine.list(url);
                            if (!resourceList.isEmpty()) {
                                resources.add(resourceList.get(0));
                            }
                        }
                        Timber.tag(TAG).d("Starting populate method for WebDAV resources");
                        populate(resources.toArray(new DavResource[0]), include_folder, source_folder, sardine, webDavClientRepository);
                    } catch (IOException e) {
                        Timber.tag(TAG).e("Error during WebDAV file count: %s", e.getMessage());
                        throw new RuntimeException(e);
                    }
                } else if (sourceFileObjectType == FileObjectType.SMB_TYPE) {
                    Timber.tag(TAG).d("Starting file count for SMB files");
                    SmbClientRepository smbClientRepository = null;
                    Session session = null;
                    String shareName = null;
                    try {
                        smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
                        session = smbClientRepository.getSession();
                        shareName = smbClientRepository.getShareName();
                        try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                            for (String filePath : files_selected_array) {
                                if (isCancelled()) {
                                    Timber.tag(TAG).d("Operation cancelled during SMB file count.");
                                    return;
                                }

                                Timber.tag(TAG).d("Processing SMB path: %s", filePath);

                                String adjustedPath;
                                if (filePath.equals("/") || filePath.isEmpty()) {
                                    adjustedPath = "";
                                } else {
                                    adjustedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
                                }

                                // Determine if the path is a directory or a file
                                boolean isDirectory = share.folderExists(adjustedPath);
                                boolean isFile = share.fileExists(adjustedPath);

                                if (isDirectory) {
                                    Timber.tag(TAG).d("Path is a directory. Starting traversal.");
                                    // Start traversing the directory
                                    populate(share, adjustedPath, include_folder);
                                } else if (isFile) {
                                    Timber.tag(TAG).d("Path is a file. Processing single file.");
                                    // Path is a file; get its size and count as one
                                    FileAllInformation fileInfo = share.getFileInformation(adjustedPath);

                                    // Optionally, exclude hidden/system files
                                    Set<FileAttributes> attributes = MakeFilePOJOUtil.parseAttributes(fileInfo.getBasicInformation().getFileAttributes());
                                    if (attributes.contains(FileAttributes.FILE_ATTRIBUTE_HIDDEN) ||
                                            attributes.contains(FileAttributes.FILE_ATTRIBUTE_SYSTEM)) {
                                        Timber.tag(TAG).d("Skipping hidden/system file: %s", filePath);
                                    } else {
                                        total_no_of_files += 1;
                                        total_size_of_files += fileInfo.getStandardInformation().getEndOfFile();

                                        // Update LiveData
                                        mutable_total_no_of_files.postValue(total_no_of_files);
                                        mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));

                                        Timber.tag(TAG).d("Processed file - Total files: %d, Total size: %d", total_no_of_files, total_size_of_files);
                                    }
                                } else {
                                    // Path does not exist or is inaccessible
                                    Timber.tag(TAG).w("SMB path does not exist or is inaccessible: %s", filePath);
                                }

                                Timber.tag(TAG).d("Completed processing SMB path: %s", filePath);
                            }
                        }
                    } catch (Exception e) {
                        Timber.tag(TAG).e("Error during SMB file count: %s", e.getMessage());
                    } finally {
                        if (smbClientRepository != null && session != null) {
                            smbClientRepository.releaseSession(session);
                            Timber.tag(TAG).d("SMB session released");
                        }
                    }
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
            mutable_total_no_of_files.postValue(total_no_of_files);
            mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
            Timber.tag(TAG).d("Current totals - Files: %d, Size: %d", total_no_of_files, total_size_of_files);
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
            mutable_total_no_of_files.postValue(total_no_of_files);
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
                FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
                FTPClient ftpClient = null;
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
                } finally {
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
            mutable_total_no_of_files.postValue(total_no_of_files);
            mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
        }
        Timber.tag(TAG).d("FTP file count completed. Total files: %d, Total size: %d", total_no_of_files, total_size_of_files);
    }

    private void populate(ChannelSftp.LsEntry[] source_list_entries, boolean include_folder, String initialPath) {
        Timber.tag("ViewModelFileCount").d("Starting populateSFTP method with " + source_list_entries.length + " entries");

        Stack<Pair<ChannelSftp.LsEntry, String>> stack = new Stack<>();
        for (ChannelSftp.LsEntry entry : source_list_entries) {
            stack.push(new Pair<>(entry, initialPath));
        }
        Timber.tag("ViewModelFileCount").d("Initial stack size: " + stack.size());

        SftpChannelRepository sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
        ChannelSftp channelSftp = null;
        try {
            channelSftp = sftpChannelRepository.getSftpChannel();
            if (channelSftp == null || !channelSftp.isConnected()) {
                Timber.tag("ViewModelFileCount").e("SFTP channel is null or not connected.");
                return;
            }

            while (!stack.isEmpty()) {
                if (isCancelled()) { // Implement this method based on your cancellation logic
                    Timber.tag("ViewModelFileCount").d("Operation cancelled");
                    return;
                }

                Pair<ChannelSftp.LsEntry, String> pair = stack.pop();
                ChannelSftp.LsEntry entry = pair.first;
                String path = pair.second;

                if (entry == null) {
                    Timber.tag("ViewModelFileCount").w("Null LsEntry encountered, skipping");
                    continue;
                }

                String entryName = entry.getFilename();
                if (".".equals(entryName) || "..".equals(entryName)) {
                    Timber.tag("ViewModelFileCount").d("Skipping special directory: " + entryName);
                    continue;
                }

                Timber.tag("ViewModelFileCount").d("Processing entry: " + entryName + " at path: " + path);

                int no_of_files = 0;
                long size_of_files = 0L;

                if (entry.getAttrs().isDir()) {
                    Timber.tag("ViewModelFileCount").d("Processing directory: " + entryName);
                    String newPath = combinePaths(path, entryName);
                    Timber.tag("ViewModelFileCount").d("New folder path: " + newPath);

                    try {
                        Vector<ChannelSftp.LsEntry> subEntries = channelSftp.ls(newPath);
                        Timber.tag("ViewModelFileCount").d("Subdirectory " + entryName + " contains " + subEntries.size() + " entries");
                        for (ChannelSftp.LsEntry subEntry : subEntries) {
                            stack.push(new Pair<>(subEntry, newPath));
                        }
                    } catch (SftpException e) {
                        Timber.tag("ViewModelFileCount").e("Error listing SFTP directory contents for path: %s, Error: %s", newPath, e.getMessage());
                        continue;
                    }

                    if (include_folder) {
                        no_of_files++;
                        Timber.tag("ViewModelFileCount").d("Including folder in count");
                    }
                } else {
                    no_of_files++;
                    size_of_files += entry.getAttrs().getSize();
                    Timber.tag("ViewModelFileCount").d("File: " + entryName + ", Size: " + entry.getAttrs().getSize());
                }

                total_no_of_files += no_of_files;
                total_size_of_files += size_of_files;
                Timber.tag(TAG).d("Current totals - Files: %d, Size: %d", total_no_of_files, total_size_of_files);
                mutable_total_no_of_files.postValue(total_no_of_files);
                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
            }

        } catch (Exception e) {
            Timber.tag("ViewModelFileCount").e("Exception during SFTP populate: %s", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (sftpChannelRepository != null && channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
            }
        }

        Timber.tag("ViewModelFileCount").d("populateSFTP method completed");
    }

    private void populate(DavResource[] source_list_resources, boolean include_folder, String initialPath, Sardine sardine, WebDavClientRepository webDavClientRepository) {
        Timber.tag(TAG).d("Starting populateWebDAV method. Initial path: %s", initialPath);
        Stack<Pair<DavResource, String>> stack = new Stack<>();
        for (DavResource resource : source_list_resources) {
            stack.push(new Pair<>(resource, initialPath));
        }

        while (!stack.isEmpty()) {
            if (isCancelled()) {
                Timber.tag(TAG).d("WebDAV file count cancelled");
                return;
            }

            Pair<DavResource, String> pair = stack.pop();
            DavResource resource = pair.first;
            String path = pair.second;

            if (resource == null) {
                Timber.tag(TAG).w("Null DavResource encountered. Skipping.");
                continue;
            }

            int no_of_files = 0;
            long size_of_files = 0L;

            if (resource.isDirectory()) {
                Timber.tag(TAG).d("Processing WebDAV directory: %s", resource.getName());
                try {
                    String name = resource.getName();
                    String newPath = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
                    String fullPath = webDavClientRepository.getBasePath(sardine) + newPath;
                    String url = webDavClientRepository.buildUrl(fullPath);
                    Timber.tag(TAG).d("Listing resources in WebDAV directory: %s", fullPath);
                    List<DavResource> subResources = sardine.list(url);
                    if (!subResources.isEmpty()) {
                        subResources.remove(0);
                    }
                    Timber.tag(TAG).d("Found %d resources in WebDAV directory: %s", subResources.size(), fullPath);
                    for (DavResource subResource : subResources) {
                        stack.push(new Pair<>(subResource, newPath));
                    }
                } catch (IOException e) {
                    Timber.tag(TAG).e("Error processing WebDAV directory: %s", e.getMessage());
                }
                if (include_folder) {
                    no_of_files++;
                }
            } else {
                Timber.tag(TAG).d("Processing WebDAV file: %s, Size: %d", resource.getName(), resource.getContentLength());
                no_of_files++;
                size_of_files += resource.getContentLength();
            }

            total_no_of_files += no_of_files;
            total_size_of_files += size_of_files;
            Timber.tag(TAG).d("Current totals - Files: %d, Size: %d", total_no_of_files, total_size_of_files);
            mutable_total_no_of_files.postValue(total_no_of_files);
            mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
        }
        Timber.tag(TAG).d("WebDAV file count completed. Total files: %d, Total size: %d", total_no_of_files, total_size_of_files);
    }


    private void populate(DiskShare share, String startPath, boolean include_folder) {
        Stack<String> stack = new Stack<>();
        stack.push(startPath);

        while (!stack.isEmpty()) {
            if (isCancelled()) {
                Timber.tag(TAG).d("SMB file count cancelled.");
                return;
            }

            String currentPath = stack.pop();
            Timber.tag(TAG).d("Traversing SMB directory: %s", currentPath);

            List<FileIdBothDirectoryInformation> fileList;
            try {
                fileList = share.list(currentPath);
            } catch (SMBApiException e) {
                Timber.tag(TAG).e("Error listing SMB directory: %s, Error: %s", currentPath, e.getMessage());
                continue; // Skip this directory and continue with others
            }

            for (FileIdBothDirectoryInformation info : fileList) {
                String name = info.getFileName();

                // Skip special entries "." and ".."
                if (name.equals(".") || name.equals("..")) {
                    continue;
                }

                // Retrieve file attributes
                Set<FileAttributes> attributes = MakeFilePOJOUtil.parseAttributes(info.getFileAttributes());

                // Optionally skip hidden and system files
                if (attributes.contains(FileAttributes.FILE_ATTRIBUTE_HIDDEN) ||
                        attributes.contains(FileAttributes.FILE_ATTRIBUTE_SYSTEM)) {
                    Timber.tag(TAG).d("Skipping hidden/system entry: %s", name);
                    continue;
                }

                String fullPath = Global.CONCATENATE_PARENT_CHILD_PATH(currentPath, name);
                Timber.tag(TAG).d("Processing SMB entry: %s", fullPath);

                if (attributes.contains(FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
                    // Entry is a directory
                    if (include_folder) {
                        total_no_of_files += 1;
                    }
                    // Push the directory path onto the stack for traversal
                    stack.push(fullPath);
                    Timber.tag(TAG).d("Added directory to stack: %s", fullPath);
                } else {
                    // Entry is a file
                    total_no_of_files += 1;
                    total_size_of_files += info.getEndOfFile();
                    Timber.tag(TAG).d("Added file - Name: %s, Size: %d", name, info.getEndOfFile());
                }

                // Update LiveData after each entry
                mutable_total_no_of_files.postValue(total_no_of_files);
                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
            }
        }

        Timber.tag(TAG).d("Completed traversal of SMB directory structure.");
    }


    private String combinePaths(String dir, String file) {
        if (!dir.endsWith("/")) {
            dir += "/";
        }
        return dir + file;
    }
}
