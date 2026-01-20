package svl.kadatha.filex.filemodel;

import androidx.annotation.NonNull;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Stack;

import svl.kadatha.filex.Global;
import svl.kadatha.filex.network.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.network.SmbClientRepository;
import timber.log.Timber;

public class SmbFileModel implements FileModel {

    private static final String TAG = "Smb-SmbFileModel";
    private final String path;

    public SmbFileModel(String path) {
        this.path = path;
        Timber.tag(TAG).d("SmbFileModel created for path: %s", path);
    }

    private static boolean mkdirSmb(String filePath) {
        Timber.tag(TAG).d("Attempting to create SMB directory: %s", filePath);
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        SmbClientRepository.ShareHandle h = null;
        try {
            h = smbClientRepository.acquireShare();
            DiskShare share = h.share;
            if (share.folderExists(filePath)) {
                Timber.tag(TAG).d("SMB directory already exists: %s", filePath);
                return true;
            } else {
                share.mkdir(filePath);
                Timber.tag(TAG).d("SMB directory created: %s", filePath);
                return true;
            }

        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Error creating SMB directory: %s", e.getMessage());
            return false;
        } finally {
            if (smbClientRepository != null) {
                smbClientRepository.releaseShare(h);
                Timber.tag(TAG).d("SMB session released");
            }

        }
    }

    private static boolean mkdirsSmb(String parentPath, @NonNull String extendedPath) {
        Timber.tag(TAG).d("Attempting to create multiple SMB directories: %s in %s", extendedPath, parentPath);
        String[] pathSegments = extendedPath.split("/");
        String currentPath = parentPath;
        for (String segment : pathSegments) {
            if (!segment.isEmpty()) {
                currentPath = Global.CONCATENATE_PARENT_CHILD_PATH(currentPath, segment);
                if (!mkdirSmb(currentPath)) {
                    Timber.tag(TAG).w("Failed to create SMB directory: %s", currentPath);
                    return false;
                }
            }
        }
        Timber.tag(TAG).d("Successfully created multiple SMB directories");
        return true;
    }

    /**
     * share-relative mkdirs helper (no leading slash)
     */
    private static void mkdirsOnShare(DiskShare share, String shareRelativeDir) {
        if (shareRelativeDir == null || shareRelativeDir.isEmpty()) return;

        String[] parts = shareRelativeDir.split("/");
        String cur = "";
        for (String part : parts) {
            if (part == null || part.isEmpty()) continue;
            cur = cur.isEmpty() ? part : (cur + "/" + part);
            try {
                if (!share.folderExists(cur)) {
                    share.mkdir(cur);
                }
            } catch (Exception ignored) {
                // If it already exists or races with another thread, ignore.
            }
        }
    }

    @Override
    public String getName() {
        String name = new java.io.File(path).getName();
        Timber.tag(TAG).d("getName() returned: %s", name);
        return name;
    }

    @Override
    public String getParentName() {
        java.io.File parentFile = new java.io.File(path).getParentFile();
        String parentName = (parentFile != null) ? parentFile.getName() : null;
        Timber.tag(TAG).d("getParentName() returned: %s", parentName);
        return parentName;
    }

    @Override
    public String getPath() {
        Timber.tag(TAG).d("getPath() returned: %s", path);
        return path;
    }

    @Override
    public String getParentPath() {
        String parentPath = new java.io.File(path).getParent();
        Timber.tag(TAG).d("getParentPath() returned: %s", parentPath);
        return parentPath;
    }

    @Override
    public boolean isDirectory() {
        boolean isDir = isDirectory(path);
        Timber.tag(TAG).d("isDirectory() returned: %b for path: %s", isDir, path);
        return isDir;
    }

    private boolean isDirectory(String filePath) {
        Timber.tag(TAG).d("Checking if SMB path is directory: %s", filePath);
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        SmbClientRepository.ShareHandle h = null;
        try {
            h = smbClientRepository.acquireShare();
            DiskShare share = h.share;
            boolean exists = share.folderExists(filePath);
            Timber.tag(TAG).d("SMB path is directory result: %b for path: %s", exists, filePath);
            return exists;

        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Error checking if SMB path is directory: %s", e.getMessage());
            return false;
        } finally {
            if (smbClientRepository != null) {
                smbClientRepository.releaseShare(h);
            }
        }
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        String new_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(getParentPath(), new_name);
        Timber.tag(TAG).d("Attempting to rename from '%s' to '%s'", path, new_file_path);
        String old_file_path = path;
        String sanitizedPath = old_file_path.startsWith("/") ? old_file_path.substring(1) : old_file_path;
        String sanitizedNewFilePath = new_file_path.startsWith("/") ? new_file_path.substring(1) : new_file_path;
        Timber.tag(TAG).d("after sanitization,attempting to rename from '%s' to '%s'", sanitizedPath, sanitizedNewFilePath);
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        SmbClientRepository.ShareHandle h = null;
        try {
            h = smbClientRepository.acquireShare();
            DiskShare share = h.share;
            // Open the source file with DELETE access
            File smbFile = share.openFile(
                    sanitizedPath,
                    EnumSet.of(AccessMask.DELETE, AccessMask.FILE_WRITE_ATTRIBUTES),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN,
                    null
            );

            // Perform the rename operation
            smbFile.rename(sanitizedNewFilePath, true);
            smbFile.close();
            Timber.tag(TAG).d("Rename operation successful");
            return true;
        } catch (SMBApiException e) {
            Timber.tag(TAG).e(e, "SMBApiException during rename operation");
            Timber.tag(TAG).e("Error Code: %s", e.getStatus());
            return false;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "IOException during rename operation");
            return false;
        } finally {
            if (smbClientRepository != null) {
                smbClientRepository.releaseShare(h);
                Timber.tag(TAG).d("SMB session released");
            }
        }
    }

    @Override
    public boolean delete() {
        Timber.tag(TAG).d("Attempting to delete SMB directory: %s", path);
        boolean success = true;
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        SmbClientRepository.ShareHandle h = null;
        try {
            h = smbClientRepository.acquireShare();
            DiskShare share = h.share;
            Stack<String> stack = new Stack<>();
            stack.push(path);

            while (!stack.isEmpty() && success) {
                String currentPath = stack.pop();
                String baseName = new java.io.File(currentPath).getName();
                if (".".equals(baseName) || "..".equals(baseName)) {
                    Timber.tag(TAG).w("Skipping deletion of special directory: %s", currentPath);
                    continue;
                }

                if (share.folderExists(currentPath)) {
                    List<FileIdBothDirectoryInformation> list = share.list(currentPath);
                    if (list != null && !list.isEmpty()) {
                        for (FileIdBothDirectoryInformation item : list) {
                            String itemName = item.getFileName();
                            if (!".".equals(itemName) && !"..".equals(itemName)) {
                                String itemPath = Global.CONCATENATE_PARENT_CHILD_PATH(currentPath, itemName);
                                stack.push(itemPath);
                            }
                        }
                    } else {
                        share.rmdir(currentPath, false);
                    }
                } else if (share.fileExists(currentPath)) {
                    share.rm(currentPath);
                }

                if (!success) {
                    Timber.tag(TAG).e("Failed to delete: %s", currentPath);
                }
            }

            // If the original path was a directory and all contents were successfully deleted, delete the directory itself
            if (success && share.folderExists(path)) {
                share.rmdir(path, false);
            }

            Timber.tag(TAG).d("SMB directory deletion result: %b for path: %s", success, path);

        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Error deleting SMB directory: %s", e.getMessage());
            success = false;
        } finally {
            if (smbClientRepository != null) {
                smbClientRepository.releaseShare(h);
                Timber.tag(TAG).d("SMB session released");
            }
        }
        return success;
    }

    @Override
    public InputStream getInputStream() {
        Timber.tag(TAG).d("Attempting to get InputStream for path: %s", path);

        SmbClientRepository repo =
                SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);

        SmbClientRepository.ShareHandle h = null;
        try {
            h = repo.acquireShareForStreaming();
            DiskShare share = h.share;
            String p = SmbClientRepository.stripLeadingSlash(path);
            File smbFile = share.openFile(
                    p,
                    EnumSet.of(AccessMask.GENERIC_READ),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                    SMB2CreateDisposition.FILE_OPEN,
                    null
            );

            InputStream in = smbFile.getInputStream();
            Timber.tag(TAG).d("Successfully retrieved InputStream for path: %s", path);

            return new SMBInputStreamWrapper(in, smbFile, h);

        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Failed to get InputStream for path: %s", path);
            if (h != null) {
                try {
                    h.close();
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }

    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        String filePath = Global.CONCATENATE_PARENT_CHILD_PATH(path, child_name);
        Timber.tag(TAG).d("Attempting to get OutputStream for path: %s", filePath);

        SmbClientRepository repo =
                SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);

        SmbClientRepository.ShareHandle h = null;
        try {
            h = repo.acquireShareForStreaming();
            DiskShare share = h.share;
            String p = SmbClientRepository.stripLeadingSlash(filePath);

            // Ensure parent dirs exist if your pipeline expects it
            // (Optional – keep if you want)
            String parent = new java.io.File(p).getParent();
            if (parent != null && parent.length() > 0) {
                // if parent doesn't exist, mkdirs style
                // NOTE: folderExists uses share-relative paths
                if (!share.folderExists(parent)) {
                    // create recursively
                    mkdirsOnShare(share, parent);
                }
            }

            File smbFile = share.openFile(
                    p,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    null
            );

            OutputStream out = smbFile.getOutputStream();
            Timber.tag(TAG).d("Successfully retrieved OutputStream for path: %s", filePath);
            return new SMBOutputStreamWrapper(out, smbFile, h);
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Failed to get OutputStream for path: %s", filePath);
            if (h != null) {
                try {
                    h.close();
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }

    @Override
    public FileModel[] list() {
        Timber.tag(TAG).d("Attempting to list files for path: %s", path);
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        SmbClientRepository.ShareHandle h = null;
        try {
            h = smbClientRepository.acquireShare();
            DiskShare share = h.share;
            List<FileIdBothDirectoryInformation> fileList = share.list(path);
            if (fileList == null || fileList.isEmpty()) {
                Timber.tag(TAG).w("No files listed or directory is empty for path: %s", path);
                return new FileModel[0];
            }
            List<FileModel> fileModels = new ArrayList<>();
            for (FileIdBothDirectoryInformation info : fileList) {
                String fileName = info.getFileName();
                if (!fileName.equals(".") && !fileName.equals("..")) {
                    String filePath = Global.CONCATENATE_PARENT_CHILD_PATH(path, fileName);
                    fileModels.add(new SmbFileModel(filePath));
                    Timber.tag(TAG).d("Listed file: %s", filePath);
                }
            }
            Timber.tag(TAG).d("Successfully listed %d files", fileModels.size());
            return fileModels.toArray(new FileModel[0]);
        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Failed to list files: %s", e.getMessage());
            return new FileModel[0];
        } finally {
            if (smbClientRepository != null) {
                smbClientRepository.releaseShare(h);
                Timber.tag(TAG).d("SMB session released");
            }
        }
    }

    @Override
    public boolean createFile(String name) {
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
        Timber.tag(TAG).d("Attempting to create file: %s", file_path);
        SmbClientRepository repo = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        SmbClientRepository.ShareHandle h = null;
        try {
            h = repo.acquireShare();
            DiskShare share = h.share;
            String adjusted = file_path.startsWith("/") ? file_path.substring(1) : file_path;
            if (share.fileExists(adjusted)) {
                Timber.tag(TAG).w("File already exists: %s", file_path);
                return false;
            }

            share.openFile(
                    adjusted,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_WRITE),
                    SMB2CreateDisposition.FILE_CREATE,
                    null
            ).close();

            Timber.tag(TAG).d("File creation successful: %s", file_path);
            return true;
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Failed to create file: %s", file_path);
            return false;
        } finally {
            repo.releaseShare(h);
        }
    }


    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        String dir_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, dir_name);
        boolean created = mkdirSmb(dir_path);
        Timber.tag(TAG).d("makeDirIfNotExists() returned: %b for path: %s", created, dir_path);
        return created;
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        boolean created = mkdirsSmb(path, extended_path);
        Timber.tag(TAG).d("makeDirsRecursively() returned: %b for path: %s", created, Global.CONCATENATE_PARENT_CHILD_PATH(path, extended_path));
        return created;
    }

    @Override
    public long getLength() {
        Timber.tag(TAG).d("getLength() called for SMB file: %s", path);
        SmbClientRepository repo = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        SmbClientRepository.ShareHandle h = null;
        try {
            h = repo.acquireShare();
            DiskShare share = h.share;
            String adjusted = path.startsWith("/") ? path.substring(1) : path;
            if (share.fileExists(adjusted)) {
                FileAllInformation fileInfo = share.getFileInformation(adjusted);
                return fileInfo.getStandardInformation().getEndOfFile();
            }
            return 0;
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Failed to get file size: %s", path);
            return 0;
        } finally {
            repo.releaseShare(h);
        }
    }


    @Override
    public boolean exists() {
        Timber.tag(TAG).d("Checking if SMB file exists: %s", path);

        SmbClientRepository repo = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        SmbClientRepository.ShareHandle h = null;

        try {
            h = repo.acquireShare();
            DiskShare share = h.share;
            String adjusted = path.startsWith("/") ? path.substring(1) : path;
            return share.fileExists(adjusted) || share.folderExists(adjusted);
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Error checking if SMB file exists: %s", path);
            return false;
        } finally {
            repo.releaseShare(h);
        }
    }

    @Override
    public long lastModified() {
        Timber.tag(TAG).d("lastModified() called for SMB file: %s", path);

        SmbClientRepository repo = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        SmbClientRepository.ShareHandle h = null;

        try {
            h = repo.acquireShare();
            DiskShare share = h.share;

            String adjusted = path.startsWith("/") ? path.substring(1) : path;

            if (share.fileExists(adjusted) || share.folderExists(adjusted)) {
                FileAllInformation fileInfo = share.getFileInformation(adjusted);
                return fileInfo.getBasicInformation().getChangeTime().toEpochMillis();
            }
            return 0;
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Failed to get last modified time: %s", path);
            return 0;
        } finally {
            repo.releaseShare(h);
        }
    }


    @Override
    public boolean isHidden() {
        Timber.tag(TAG).d("Checking if SMB file is hidden: %s", path);
        return getName().startsWith(".");
    }

    // Inner classes for InputStream and OutputStream wrappers
    public static final class SMBInputStreamWrapper extends FilterInputStream {
        private final File smbFile;
        private final SmbClientRepository.ShareHandle handle;
        private boolean closed;

        public SMBInputStreamWrapper(InputStream in, File smbFile, SmbClientRepository.ShareHandle handle) {
            super(in);
            this.smbFile = smbFile;
            this.handle = handle;
        }

        @Override
        public void close() throws IOException {
            if (closed) return;
            closed = true;
            IOException first = null;
            try {
                super.close();
            } catch (IOException e) {
                first = e;
            }

            try {
                if (smbFile != null) smbFile.close();
            } catch (Exception ignored) {
            }
            try {
                if (handle != null) handle.close();
            } catch (Exception ignored) {
            }

            if (first != null) throw first;
        }
    }

    public static final class SMBOutputStreamWrapper extends FilterOutputStream {
        private final File smbFile;
        private final SmbClientRepository.ShareHandle handle;
        private boolean closed;

        public SMBOutputStreamWrapper(OutputStream out, File smbFile, SmbClientRepository.ShareHandle handle) {
            super(out);
            this.smbFile = smbFile;
            this.handle = handle;
        }

        @Override
        public void close() throws IOException {
            if (closed) return;
            closed = true;
            IOException first = null;
            try {
                // flush then close
                super.flush();
            } catch (IOException e) {
                first = e;
            }

            try {
                super.close();
            } catch (IOException e) {
                if (first == null) first = e;
            }

            try {
                if (smbFile != null) smbFile.close();
            } catch (Exception ignored) {
            }
            try {
                if (handle != null) handle.close();
            } catch (Exception ignored) {
            }

            if (first != null) throw first;
        }
    }
}
