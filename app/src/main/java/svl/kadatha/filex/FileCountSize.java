package svl.kadatha.filex;





import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMBApiException;
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
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import svl.kadatha.filex.asynctasks.CopyToAsyncTask;
import svl.kadatha.filex.cloud.CloudAuthActivityViewModel;
import svl.kadatha.filex.network.FtpClientRepository;
import svl.kadatha.filex.network.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.network.SftpChannelRepository;
import svl.kadatha.filex.network.SmbClientRepository;
import svl.kadatha.filex.network.WebDavClientRepository;
import svl.kadatha.filex.usb.ReadAccess;
import svl.kadatha.filex.usb.UsbFileRootSingleton;
import timber.log.Timber;

public class FileCountSize {
    private static final String TAG = "FileCountSize";
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
    private static final OkHttpClient CLOUD_HTTP = Global.HTTP;
    private static final Gson CLOUD_GSON = Global.GSON;

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

    public void fileCountDataList() {
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
                    try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
                        UsbFile usbFileRoot = access.getUsbFile();
                        for (int i = 0; i < size; ++i) {
                            UsbFile f = FileUtil.getUsbFile(usbFileRoot, files_selected_array.get(i));
                            f_array[i] = f;
                        }
                        populate(f_array, include_folder);
                    }
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

                    Sardine sardine;
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
                    SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
                    SmbClientRepository.ShareHandle h = null;
                    try {
                        h = smbClientRepository.acquireShare();
                        DiskShare share = h.share;
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
                    } catch (Exception e) {
                        Timber.tag(TAG).e("Error during SMB file count: %s", e.getMessage());
                    } finally {
                        if (smbClientRepository != null) {
                            smbClientRepository.releaseShare(h);
                            Timber.tag(TAG).d("SMB session released");
                        }
                    }
                } else if (sourceFileObjectType == FileObjectType.GOOGLE_DRIVE_TYPE) {

                    final String oauthToken = CloudAuthActivityViewModel.GOOGLE_DRIVE_ACCESS_TOKEN;
                    if (oauthToken == null || oauthToken.trim().isEmpty()) {
                        Timber.tag(TAG).e("GOOGLE_DRIVE_TYPE: oauthToken is null/empty");
                        return;
                    }

                    final int CAP = 100;
                    final int capPlusOne = CAP + 1;

                    for (String sel : files_selected_array) {
                        if (isCancelled()) return;

                        // ---- 1) Try fastest path: POJO -> cloudId + mime + size ----
                        FilePOJO pojo = FilePOJOUtil.GET_FILE_POJO(sel, FileObjectType.GOOGLE_DRIVE_TYPE);

                        String driveId = null;
                        String mimeHint = null;
                        Long sizeHint = null;

                        if (pojo != null) {
                            driveId = safeTrim(pojo.getCloudId());
                            mimeHint = safeTrim(pojo.getDriveMimeType());

                            long s = pojo.getSizeLong(); // long primitive
                            if (s > 0) sizeHint = s;     // 0 => unknown/not set
                        }

                        // ---- 2) If no driveId, fallback: resolve by path (your existing mechanism) ----
                        if (driveId == null || driveId.isEmpty()) {
                            String path = normalizeGoogleDrivePath(sel);
                            if (path == null) continue;

                            driveId = driveResolveIdByPath(path, oauthToken);
                            Timber.tag("DRIVE_PATH").d("SEL=%s  PATH=%s  RESOLVED_ID=%s", sel, path, driveId);

                            if (driveId == null || driveId.isEmpty()) continue;
                        }

                        // ---- 3) Decide folder/file with minimal API calls ----
                        boolean isFolder = DriveMime.FOLDER.equals(mimeHint);

                        // If mime unknown, fetch meta once to know folder/file + maybe size
                        DriveFileMeta meta = null;
                        if (mimeHint == null || mimeHint.isEmpty()) {
                            meta = driveGetMeta(driveId, oauthToken);
                            if (meta == null) continue;
                            mimeHint = meta.mimeType;
                            isFolder = DriveMime.FOLDER.equals(mimeHint);
                        }

                        if (isFolder) {
                            // Count folder itself if include_folder
                            if (include_folder) {
                                total_no_of_files += 1;
                                if (total_no_of_files >= capPlusOne) {
                                    postDriveCapped(capPlusOne);
                                    return;
                                }
                                postDriveProgress();
                            }

                            // Traverse descendants (folder sizes come from children)
                            drivePopulateFolderTreeCapped(driveId, oauthToken, include_folder, capPlusOne);

                            if (total_no_of_files >= capPlusOne) return;

                        } else {
                            // ---- Single file ----
                            // Prefer POJO size; else meta.size/quotaBytesUsed
                            if (sizeHint == null) {
                                if (meta == null) meta = driveGetMeta(driveId, oauthToken);
                                sizeHint = bestDriveSize(meta);
                            }

                            total_no_of_files += 1;
                            total_size_of_files += safeSize(sizeHint);

                            if (total_no_of_files >= capPlusOne) {
                                postDriveCapped(capPlusOne);
                                return;
                            }

                            postDriveProgress();
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

    // ---- Drive constants ----
    private static final class DriveMime {
        static final String FOLDER = "application/vnd.google-apps.folder";
    }

    private static String normalizeGoogleDrivePath(String path) {
        if (path == null) return null;

        path = path.trim();
        if (path.isEmpty()) return null;

        // Always forward slashes
        path = path.replace('\\', '/');

        // Ensure leading slash
        if (!path.startsWith("/")) path = "/" + path;

        // Strip UI label "My Drive"
        if (path.equals("/My Drive")) return "/";
        if (path.startsWith("/My Drive/")) {
            path = path.substring("/My Drive".length()); // keeps leading '/'
            if (path.isEmpty()) path = "/";
        }

        // Collapse multiple slashes
        while (path.contains("//")) path = path.replace("//", "/");

        // Trim trailing slash (except root)
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);

        return path;
    }

    private static String safeTrim(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private static Long bestDriveSize(DriveFileMeta m) {
        if (m == null) return null;
        if (m.size != null && m.size >= 0) return m.size;
        if (m.quotaBytesUsed != null && m.quotaBytesUsed >= 0) return m.quotaBytesUsed;
        return null;
    }

    private void postDriveProgress() {
        mutable_total_no_of_files.postValue(total_no_of_files);
        mutable_size_of_files_to_be_archived_copied.postValue(
                FileUtil.humanReadableByteCount(total_size_of_files)
        );
    }

    private void postDriveCapped(int capPlusOne) {
        mutable_total_no_of_files.postValue(capPlusOne);
        mutable_size_of_files_to_be_archived_copied.postValue(
                FileUtil.humanReadableByteCount(total_size_of_files) + "+"
        );
    }

    private static long safeSize(Long size) {
        return (size == null || size < 0) ? 0L : size;
    }







    private String driveResolveIdByPath(String path, String oauthToken) {
        if (path == null) return null;

        // If caller accidentally passed an ID, keep it (IDs do NOT contain '/')
        // Example: "1AbCDefGhIJkLmNoPqR"
        if (!path.contains("/")) return path;

        path = normalizeGoogleDrivePath(path);
        if (path == null) return null;

        // Root folder
        if ("/".equals(path)) return "root";

        // "/Apks/Child" -> ["", "Apks", "Child"]
        String[] parts = path.split("/");
        String parentId = "root";

        for (String name : parts) {
            if (name == null || name.isEmpty()) continue;

            String nextId = driveFindChildIdByName(parentId, name, oauthToken);
            if (nextId == null) {
                Timber.tag("DRIVE_PATH").e("Not found under parentId=%s: name=%s (path=%s)", parentId, name, path);
                return null;
            }
            parentId = nextId;
        }
        return parentId;
    }

    private String driveFindChildIdByName(String parentId, String name, String oauthToken) {
        // WARNING: if duplicates exist (same name under same parent), this returns the first match.
        String q = "name = " + quoteDrive(name)
                + " and '" + parentId + "' in parents"
                + " and trashed = false";

        HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                .newBuilder()
                .addQueryParameter("q", q)
                .addQueryParameter("pageSize", "1")
                .addQueryParameter("fields", "files(id,mimeType)")
                .build();

        Request req = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + oauthToken)
                .get()
                .build();

        try (Response resp = CLOUD_HTTP.newCall(req).execute()) {

            Timber.tag("DRIVE_PATH").d("FIND_CHILD URL: %s", url);
            Timber.tag("DRIVE_PATH").d("HTTP CODE: %d", resp.code());

            if (!resp.isSuccessful()) {
                String err = resp.body() != null ? resp.body().string() : "null";
                Timber.tag("DRIVE_PATH").e("ERROR BODY: %s", err);
                return null;
            }

            if (resp.body() == null) return null;

            DriveListResponse r = CLOUD_GSON.fromJson(resp.body().charStream(), DriveListResponse.class);
            if (r == null || r.files == null || r.files.isEmpty()) return null;

            DriveFileMeta f = r.files.get(0);
            return (f != null) ? f.id : null;

        } catch (Exception e) {
            Timber.tag("DRIVE_PATH").e("EXCEPTION: %s", Log.getStackTraceString(e));
            return null;
        }
    }

    private static String quoteDrive(String s) {
        if (s == null) return "''";
        // Escape backslash and single quote for Drive q
        String out = s.replace("\\", "\\\\").replace("'", "\\'");
        return "'" + out + "'";
    }

    // Minimal meta for one file
    private static final class DriveFileMeta {
        String id;
        String mimeType;
        Long size;
        Long quotaBytesUsed;
    }


    // Response for list children
    private static final class DriveListResponse {
        String nextPageToken;
        List<DriveFileMeta> files;
    }

    /**
     * Traverse a folder tree (DFS) and add counts + sizes into total_no_of_files / total_size_of_files.
     * Uses Drive files.list with pagination. Stops early when count reaches capPlusOne.
     */
    private void drivePopulateFolderTreeCapped(String rootFolderId,
                                               String oauthToken,
                                               boolean includeFolder,
                                               int capPlusOne) {

        Stack<String> folderStack = new Stack<>();
        folderStack.push(rootFolderId);

        while (!folderStack.isEmpty()) {
            if (isCancelled()) return;

            String currentFolderId = folderStack.pop();
            String pageToken = null;

            while (true) {
                if (isCancelled()) return;

                DriveListResponse res = driveListChildren(currentFolderId, oauthToken, pageToken);
                if (res == null || res.files == null) return;

                for (DriveFileMeta child : res.files) {
                    if (isCancelled()) return;
                    if (child == null) continue;

                    boolean isFolder = DriveMime.FOLDER.equals(child.mimeType);

                    if (isFolder) {
                        if (includeFolder) {
                            total_no_of_files += 1;
                            if (total_no_of_files >= capPlusOne) {
                                // Signal capped to UI:
                                mutable_total_no_of_files.postValue(capPlusOne);
                                mutable_size_of_files_to_be_archived_copied.postValue(
                                        FileUtil.humanReadableByteCount(total_size_of_files) + "+"
                                );
                                return;
                            }
                        }
                        folderStack.push(child.id);
                    } else {
                        total_no_of_files += 1;
                        total_size_of_files += safeSize(child.size);

                        if (total_no_of_files >= capPlusOne) {
                            mutable_total_no_of_files.postValue(capPlusOne);
                            mutable_size_of_files_to_be_archived_copied.postValue(
                                    FileUtil.humanReadableByteCount(total_size_of_files) + "+"
                            );
                            return;
                        }
                    }

                    // Update after each item (same pattern you used elsewhere)
                    mutable_total_no_of_files.postValue(total_no_of_files);
                    mutable_size_of_files_to_be_archived_copied.postValue(
                            FileUtil.humanReadableByteCount(total_size_of_files)
                    );
                }

                pageToken = res.nextPageToken;
                if (pageToken == null || pageToken.isEmpty()) break;
            }
        }
    }

    private DriveFileMeta driveGetMeta(String fileId, String oauthToken) {
        if (fileId == null) return null;
        fileId = fileId.trim();

        // Drive file IDs never contain '/'
        if (fileId.isEmpty() || fileId.contains("/")) {
            Timber.tag("DRIVE_META").e("Invalid Drive fileId passed: %s", fileId);
            return null;
        }

        HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files/" + fileId)
                .newBuilder()
                .addQueryParameter("fields", "id,mimeType,size,quotaBytesUsed")
                .build();

        // ✅ CREATE REQUEST
        Request req = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + oauthToken)
                .get()
                .build();

        try (Response resp = CLOUD_HTTP.newCall(req).execute()) {

            Timber.tag("DRIVE_META").d("URL: %s", url);
            Timber.tag("DRIVE_META").d("HTTP CODE: %d", resp.code());

            if (!resp.isSuccessful()) {
                String err = resp.body() != null ? resp.body().string() : "null";
                Timber.tag("DRIVE_META").e("ERROR BODY: %s", err);
                return null;
            }

            if (resp.body() == null) {
                Timber.tag("DRIVE_META").e("Body is null");
                return null;
            }

            DriveFileMeta meta = CLOUD_GSON.fromJson(resp.body().charStream(), DriveFileMeta.class);
            Timber.tag("DRIVE_META").d("META: id=%s mime=%s size=%s",
                    meta.id, meta.mimeType, meta.size);

            return meta;

        } catch (Exception e) {
            Timber.tag("DRIVE_META").e("EXCEPTION: %s", Log.getStackTraceString(e));
            return null;
        }
    }



    private DriveListResponse driveListChildren(String folderId, String oauthToken, String pageToken) {
        // files.list with:
        // q="'<folderId>' in parents and trashed=false"
        // fields="nextPageToken,files(id,mimeType,size)"
        HttpUrl.Builder b = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                .newBuilder()
                .addQueryParameter("q", "'" + folderId + "' in parents and trashed=false")
                .addQueryParameter("pageSize", "1000")
                .addQueryParameter("fields", "nextPageToken,files(id,mimeType,size)");

        if (pageToken != null && !pageToken.isEmpty()) {
            b.addQueryParameter("pageToken", pageToken);
        }

        Request req = new Request.Builder()
                .url(b.build())
                .header("Authorization", "Bearer " + oauthToken)
                .get()
                .build();

        try (Response resp = CLOUD_HTTP.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) return null;
            return CLOUD_GSON.fromJson(resp.body().charStream(), DriveListResponse.class);
        } catch (IOException e) {
            Timber.tag(TAG).e("Drive list error: %s", e.getMessage());
            return null;
        }
    }

}
