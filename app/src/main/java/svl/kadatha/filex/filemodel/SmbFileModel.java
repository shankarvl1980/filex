package svl.kadatha.filex.filemodel;

import androidx.annotation.NonNull;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Stack;

import svl.kadatha.filex.Global;
import svl.kadatha.filex.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.SmbClientRepository;
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
        Session session = null;
        String shareName = smbClientRepository.getShareName();
        try {
            session = smbClientRepository.getSession();
            try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                if (share.folderExists(filePath)) {
                    Timber.tag(TAG).d("SMB directory already exists: %s", filePath);
                    return true;
                } else {
                    share.mkdir(filePath);
                    Timber.tag(TAG).d("SMB directory created: %s", filePath);
                    return true;
                }
            }
        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Error creating SMB directory: %s", e.getMessage());
            return false;
        } finally {
            if (session != null) {
                smbClientRepository.releaseSession(session);
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
        SmbClientRepository smbClientRepository = null;
        Session session = null;
        String shareName = null;
        try {
            smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
            session = smbClientRepository.getSession();
            shareName = smbClientRepository.getShareName();
            try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                boolean exists = share.folderExists(filePath);
                Timber.tag(TAG).d("SMB path is directory result: %b for path: %s", exists, filePath);
                return exists;
            }
        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Error checking if SMB path is directory: %s", e.getMessage());
            return false;
        } finally {
            if (smbClientRepository != null && session != null) {
                smbClientRepository.releaseSession(session);
            }
        }
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        String new_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(getParentPath(), new_name);
        Timber.tag(TAG).d("Attempting to rename from '%s' to '%s'", path, new_file_path);
        String old_file_path=path;
        String sanitizedPath = old_file_path.startsWith("/") ? old_file_path.substring(1) : old_file_path;
        String sanitizedNewFilePath = new_file_path.startsWith("/") ? new_file_path.substring(1) : new_file_path;
        Timber.tag(TAG).d("after sanitization,attempting to rename from '%s' to '%s'", sanitizedPath, sanitizedNewFilePath);
        SmbClientRepository smbClientRepository = null;
        Session session = null;
        String shareName;
        try {
            smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
            session = smbClientRepository.getSession();
            shareName = smbClientRepository.getShareName();
            try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
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
            }
        } catch (SMBApiException e) {
            Timber.tag(TAG).e(e, "SMBApiException during rename operation");
            Timber.tag(TAG).e("Error Code: %s", e.getStatus());
            return false;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "IOException during rename operation");
            return false;
        } finally {
            if (smbClientRepository != null && session != null) {
                smbClientRepository.releaseSession(session);
                Timber.tag(TAG).d("SMB session released");
            }
        }
    }


    @Override
    public boolean delete() {
        Timber.tag(TAG).d("Attempting to delete SMB directory: %s", path);
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        Session session = null;
        boolean success = true;
        String shareName = smbClientRepository.getShareName();

        try {
            session = smbClientRepository.getSession();
            try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
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
            }
        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Error deleting SMB directory: %s", e.getMessage());
            success = false;
        } finally {
            if (session != null) {
                smbClientRepository.releaseSession(session);
                Timber.tag(TAG).d("SMB session released");
            }
        }
        return success;
    }

    @Override
    public InputStream getInputStream() {
        Timber.tag(TAG).d("Attempting to get InputStream for path: %s", path);
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        Session session = null;
        String shareName = smbClientRepository.getShareName();
        try {
            session = smbClientRepository.getSession();
            DiskShare share = (DiskShare) session.connectShare(shareName);
            File file = share.openFile(
                    path,
                    EnumSet.of(AccessMask.GENERIC_READ),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                    SMB2CreateDisposition.FILE_OPEN,
                    null
            );
            InputStream inputStream = file.getInputStream();
            Timber.tag(TAG).d("Successfully retrieved InputStream for path: %s", path);
            return new SMBInputStreamWrapper(inputStream, file, share, session, smbClientRepository);
        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Failed to get InputStream: %s", e.getMessage());
            return null;
        } finally {
            if (session != null) {
                smbClientRepository.releaseSession(session);
            }
        }
    }

    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, child_name);
        Timber.tag(TAG).d("Attempting to get OutputStream for path: %s", file_path);
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        Session session = null;
        String shareName = smbClientRepository.getShareName();
        try {
            session = smbClientRepository.getSession();
            DiskShare share = (DiskShare) session.connectShare(shareName);
            File file = share.openFile(
                    file_path,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_WRITE),
                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    null
            );
            OutputStream outputStream = file.getOutputStream();
            Timber.tag(TAG).d("Successfully retrieved OutputStream for path: %s", file_path);
            return new SMBOutputStreamWrapper(outputStream, file, share, session, smbClientRepository);
        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Failed to get OutputStream: %s", e.getMessage());
            return null;
        } finally {
            if (session != null) {
                smbClientRepository.releaseSession(session);
            }
        }
    }

    @Override
    public FileModel[] list() {
        Timber.tag(TAG).d("Attempting to list files for path: %s", path);
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        Session session = null;
        String shareName = smbClientRepository.getShareName();
        try {
            session = smbClientRepository.getSession();
            DiskShare share = (DiskShare) session.connectShare(shareName);
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
            if (session != null) {
                smbClientRepository.releaseSession(session);
                Timber.tag(TAG).d("SMB session released");
            }
        }
    }

    @Override
    public boolean createFile(String name) {
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
        Timber.tag(TAG).d("Attempting to create file: %s", file_path);
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        Session session = null;
        String shareName = smbClientRepository.getShareName();
        try {
            session = smbClientRepository.getSession();
            DiskShare share = (DiskShare) session.connectShare(shareName);
            if (share.fileExists(file_path)) {
                Timber.tag(TAG).w("File already exists: %s", file_path);
                return false;
            }
            share.openFile(
                    file_path,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_WRITE),
                    SMB2CreateDisposition.FILE_CREATE,
                    null
            ).close();
            Timber.tag(TAG).d("File creation successful: %s", file_path);
            return true;
        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Failed to create file: %s", e.getMessage());
            return false;
        } finally {
            if (session != null) {
                smbClientRepository.releaseSession(session);
                Timber.tag(TAG).d("SMB session released");
            }
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
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        Session session = null;
        String shareName = smbClientRepository.getShareName();
        try {
            session = smbClientRepository.getSession();
            DiskShare share = (DiskShare) session.connectShare(shareName);
            if (share.fileExists(path)) {
                FileAllInformation fileInfo = share.getFileInformation(path);
                long size = fileInfo.getStandardInformation().getEndOfFile();
                Timber.tag(TAG).d("File size: %d bytes", size);
                return size;
            }
        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Failed to get file size: %s", e.getMessage());
        } finally {
            if (session != null) {
                smbClientRepository.releaseSession(session);
                Timber.tag(TAG).d("SMB session released");
            }
        }
        return 0;
    }

    @Override
    public boolean exists() {
        Timber.tag(TAG).d("Checking if SMB file exists: %s", path);
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        Session session = null;
        String shareName = smbClientRepository.getShareName();
        try {
            session = smbClientRepository.getSession();
            DiskShare share = (DiskShare) session.connectShare(shareName);
            boolean exists = share.fileExists(path) || share.folderExists(path);
            Timber.tag(TAG).d("SMB file exists result: %b for path: %s", exists, path);
            return exists;
        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Error checking if SMB file exists: %s", e.getMessage());
            return false;
        } finally {
            if (session != null) {
                smbClientRepository.releaseSession(session);
                Timber.tag(TAG).d("SMB session released");
            }
        }
    }

    @Override
    public long lastModified() {
        Timber.tag(TAG).d("lastModified() called for SMB file: %s", path);
        SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
        Session session = null;
        String shareName = smbClientRepository.getShareName();
        try {
            session = smbClientRepository.getSession();
            DiskShare share = (DiskShare) session.connectShare(shareName);
            if (share.fileExists(path) || share.folderExists(path)) {
                FileAllInformation fileInfo = share.getFileInformation(path);
                long lastModified = fileInfo.getBasicInformation().getChangeTime().toEpochMillis();
                Timber.tag(TAG).d("File last modified time: %d", lastModified);
                return lastModified;
            }
        } catch (IOException | SMBRuntimeException e) {
            Timber.tag(TAG).e("Failed to get last modified time: %s", e.getMessage());
        } finally {
            if (session != null) {
                smbClientRepository.releaseSession(session);
                Timber.tag(TAG).d("SMB session released");
            }
        }
        return 0;
    }

    @Override
    public boolean isHidden() {
        Timber.tag(TAG).d("Checking if SMB file is hidden: %s", path);
        return getName().startsWith(".");
    }

    // Inner classes for InputStream and OutputStream wrappers
    public static class SMBInputStreamWrapper extends InputStream {
        private final InputStream wrappedStream;
        private final File smbFile;
        private final DiskShare share;
        private final Session session;
        private final SmbClientRepository repository;

        public SMBInputStreamWrapper(InputStream wrappedStream, File smbFile, DiskShare share, Session session, SmbClientRepository repository) {
            this.wrappedStream = wrappedStream;
            this.smbFile = smbFile;
            this.share = share;
            this.session = session;
            this.repository = repository;
        }

        @Override
        public int read() throws IOException {
            return wrappedStream.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return wrappedStream.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return wrappedStream.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return wrappedStream.skip(n);
        }

        @Override
        public int available() throws IOException {
            return wrappedStream.available();
        }

        @Override
        public void close() throws IOException {
            try {
                wrappedStream.close();
                smbFile.close();
                share.close();
            } finally {
                repository.releaseSession(session);
                Timber.tag("SMBInputStreamWrapper").d("SMB session released");
            }
        }
    }

    public static class SMBOutputStreamWrapper extends OutputStream {
        private final OutputStream wrappedStream;
        private final File smbFile;
        private final DiskShare share;
        private final Session session;
        private final SmbClientRepository repository;

        public SMBOutputStreamWrapper(OutputStream wrappedStream, File smbFile, DiskShare share, Session session, SmbClientRepository repository) {
            this.wrappedStream = wrappedStream;
            this.smbFile = smbFile;
            this.share = share;
            this.session = session;
            this.repository = repository;
        }

        @Override
        public void write(int b) throws IOException {
            wrappedStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            wrappedStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            wrappedStream.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            try {
                wrappedStream.close();
                smbFile.close();
                share.close();
            } finally {
                repository.releaseSession(session);
                Timber.tag("SMBOutputStreamWrapper").d("SMB session released");
            }
        }
    }
}
