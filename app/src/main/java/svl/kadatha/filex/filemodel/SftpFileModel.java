package svl.kadatha.filex.filemodel;


import androidx.annotation.NonNull;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import svl.kadatha.filex.Global;
import svl.kadatha.filex.network.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.network.SftpChannelRepository;
import timber.log.Timber;

public class SftpFileModel implements FileModel {

    private static final String TAG = "SftpFileModel";
    private final String path;

    public SftpFileModel(String path) {
        this.path = path;
        Timber.tag(TAG).d("SftpFileModel created for path: %s", path);
    }

    private static boolean mkdirSftp(String dirPath) {
        Timber.tag(TAG).d("Attempting to create SFTP directory: %s", dirPath);
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp = null;
        try {
            sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            channelSftp = sftpChannelRepository.getSftpChannel();

            try {
                channelSftp.stat(dirPath);
                Timber.tag(TAG).d("SFTP directory already exists: %s", dirPath);
                return true;
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    channelSftp.mkdir(dirPath);
                    Timber.tag(TAG).d("SFTP directory creation successful: %s", dirPath);
                    return true;
                } else {
                    throw e;
                }
            }
        } catch (SftpException | JSchException e) {
            Timber.tag(TAG).e("Error creating SFTP directory: %s", e.getMessage());
            return false;
        } finally {
            if (sftpChannelRepository != null && channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
                Timber.tag(TAG).d("SFTP channel released");
            }
        }
    }

    private static boolean mkdirsSftp(String parentPath, @NonNull String extendedPath) {
        Timber.tag(TAG).d("Attempting to create multiple SFTP directories: %s in %s", extendedPath, parentPath);
        boolean success;
        String[] pathSegments = extendedPath.split("/");
        String currentPath = parentPath;
        for (String segment : pathSegments) {
            if (!segment.isEmpty()) {
                currentPath = Global.CONCATENATE_PARENT_CHILD_PATH(currentPath, segment);
                success = mkdirSftp(currentPath);
                if (!success) {
                    Timber.tag(TAG).w("Failed to create SFTP directory: %s", currentPath);
                    return false;
                }
            }
        }
        Timber.tag(TAG).d("Successfully created multiple SFTP directories");
        return true;
    }

    private static boolean isDirectory(String filePath) {
        Timber.tag(TAG).d("Checking if SFTP path is directory: %s", filePath);
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp = null;
        try {
            sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            channelSftp = sftpChannelRepository.getSftpChannel();
            SftpATTRS attrs = channelSftp.lstat(filePath);
            boolean isDirectory = attrs.isDir();
            Timber.tag(TAG).d("SFTP path is directory result: %b for path: %s", isDirectory, filePath);
            return isDirectory;
        } catch (SftpException | JSchException e) {
            Timber.tag(TAG).e("Error checking if SFTP path is directory: %s", e.getMessage());
            return false;
        } finally {
            if (sftpChannelRepository != null && channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
            }
        }
    }

    @Override
    public String getName() {
        String name = new File(path).getName();
        Timber.tag(TAG).d("getName() returned: %s", name);
        return name;
    }

    @Override
    public String getParentName() {
        File parentFile = new File(path).getParentFile();
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
        String parentPath = new File(path).getParent();
        Timber.tag(TAG).d("getParentPath() returned: %s", parentPath);
        return parentPath;
    }

    @Override
    public boolean isDirectory() {
        boolean isDir = isDirectory(path);
        Timber.tag(TAG).d("isDirectory() returned: %b for path: %s", isDir, path);
        return isDir;
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        String newFilePath = Global.CONCATENATE_PARENT_CHILD_PATH(getParentPath(), new_name);
        Timber.tag(TAG).d("Attempting to rename from %s to %s", path, newFilePath);
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp = null;
        try {
            sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            channelSftp = sftpChannelRepository.getSftpChannel();
            channelSftp.rename(path, newFilePath);
            Timber.tag(TAG).d("Rename operation successful");
            return true;
        } catch (SftpException | JSchException e) {
            Timber.tag(TAG).e("Rename operation failed: %s", e.getMessage());
            return false;
        } finally {
            if (sftpChannelRepository != null && channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
                Timber.tag(TAG).d("SFTP channel released");
            }
        }
    }

    @Override
    public boolean delete() {
        Timber.tag(TAG).d("Attempting to delete SFTP directory or file: %s", path);
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp = null;
        boolean success = true;
        try {
            sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            channelSftp = sftpChannelRepository.getSftpChannel();
            deleteRecursive(channelSftp, path);
            Timber.tag(TAG).d("SFTP deletion result: %b for path: %s", success, path);
            return success;
        } catch (SftpException | JSchException e) {
            Timber.tag(TAG).e("Error deleting SFTP directory or file: %s", e.getMessage());
            success = false;
            return false;
        } finally {
            if (sftpChannelRepository != null && channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
                Timber.tag(TAG).d("SFTP channel released");
            }
        }
    }

    private void deleteRecursive(ChannelSftp channelSftp, String path) throws SftpException {
        SftpATTRS attrs = channelSftp.lstat(path);
        if (attrs.isDir()) {
            Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(path);
            if (entries != null) {
                for (ChannelSftp.LsEntry entry : entries) {
                    String entryName = entry.getFilename();
                    if (".".equals(entryName) || "..".equals(entryName)) {
                        continue;
                    }
                    String childPath = path + "/" + entryName;
                    deleteRecursive(channelSftp, childPath);
                }
            }
            channelSftp.rmdir(path);
            Timber.tag(TAG).d("Removed directory: %s", path);
        } else {
            channelSftp.rm(path);
            Timber.tag(TAG).d("Removed file: %s", path);
        }
    }

    @Override
    public InputStream getInputStream() {
        Timber.tag(TAG).d("Attempting to get InputStream for path: %s", path);
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp = null;
        try {
            sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            channelSftp = sftpChannelRepository.getSftpChannel();
            InputStream inputStream = channelSftp.get(path);
            if (inputStream == null) {
                throw new IOException("Failed to retrieve file stream");
            }
            Timber.tag(TAG).d("Successfully retrieved InputStream for path: %s", path);
            return new SFTPInputStreamWrapper(inputStream, channelSftp, sftpChannelRepository);
        } catch (Exception e) {
            Timber.tag(TAG).e("Failed to get InputStream: %s", e.getMessage());
            if (channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
            }
            return null;
        }
    }

    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        String filePath = Global.CONCATENATE_PARENT_CHILD_PATH(path, child_name);
        Timber.tag(TAG).d("Attempting to get OutputStream for path: %s", filePath);
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp = null;
        try {
            sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            channelSftp = sftpChannelRepository.getSftpChannel();
            OutputStream outputStream = channelSftp.put(filePath);
            Timber.tag(TAG).d("Successfully retrieved OutputStream for path: %s", filePath);
            return new SFTPOutputStreamWrapper(outputStream, channelSftp, sftpChannelRepository);
        } catch (Exception e) {
            Timber.tag(TAG).e("Failed to get OutputStream: %s", e.getMessage());
            if (channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
            }
            return null;
        }
    }

    @Override
    public FileModel[] list() {
        Timber.tag(TAG).d("Attempting to list files for path: %s", path);
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp = null;
        try {
            sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            channelSftp = sftpChannelRepository.getSftpChannel();
            Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(path);
            if (entries == null || entries.isEmpty()) {
                Timber.tag(TAG).w("No files listed or directory is empty for path: %s", path);
                return new FileModel[0];
            }

            List<FileModel> fileModels = new ArrayList<>();
            for (ChannelSftp.LsEntry entry : entries) {
                String fileName = entry.getFilename();
                if (!fileName.equals(".") && !fileName.equals("..")) {
                    String fullPath = path + "/" + fileName;
                    fileModels.add(new SftpFileModel(fullPath));
                    Timber.tag(TAG).d("Listed file: %s", fullPath);
                }
            }

            Timber.tag(TAG).d("Successfully listed %d files", fileModels.size());
            return fileModels.toArray(new FileModel[0]);
        } catch (SftpException | JSchException e) {
            Timber.tag(TAG).e("Failed to list files: %s", e.getMessage());
            return new FileModel[0];
        } finally {
            if (sftpChannelRepository != null && channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
                Timber.tag(TAG).d("SFTP channel released");
            }
        }
    }

    @Override
    public boolean createFile(String name) {
        String filePath = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
        Timber.tag(TAG).d("Attempting to create file: %s", filePath);
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp = null;
        try {
            sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            channelSftp = sftpChannelRepository.getSftpChannel();
            OutputStream outputStream = channelSftp.put(filePath);
            outputStream.close();
            Timber.tag(TAG).d("File creation successful for path: %s", filePath);
            return true;
        } catch (Exception e) {
            Timber.tag(TAG).e("Failed to create file: %s", e.getMessage());
            return false;
        } finally {
            if (sftpChannelRepository != null && channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
                Timber.tag(TAG).d("SFTP channel released");
            }
        }
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        String dirPath = Global.CONCATENATE_PARENT_CHILD_PATH(path, dir_name);
        boolean created = mkdirSftp(dirPath);
        Timber.tag(TAG).d("makeDirIfNotExists() returned: %b for path: %s", created, dirPath);
        return created;
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        boolean created = mkdirsSftp(path, extended_path);
        Timber.tag(TAG).d("makeDirsRecursively() returned: %b for path: %s", created, Global.CONCATENATE_PARENT_CHILD_PATH(path, extended_path));
        return created;
    }

    @Override
    public long getLength() {
        Timber.tag(TAG).d("Getting length for SFTP path: %s", path);
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp = null;
        try {
            sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            channelSftp = sftpChannelRepository.getSftpChannel();
            SftpATTRS attrs = channelSftp.lstat(path);
            return attrs.getSize();
        } catch (SftpException | JSchException e) {
            Timber.tag(TAG).e("Failed to get length: %s", e.getMessage());
            return 0;
        } finally {
            if (sftpChannelRepository != null && channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
                Timber.tag(TAG).d("SFTP channel released");
            }
        }
    }

    @Override
    public boolean exists() {
        Timber.tag(TAG).d("Checking if SFTP file exists: %s", path);
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp = null;
        try {
            sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            channelSftp = sftpChannelRepository.getSftpChannel();
            channelSftp.lstat(path);
            Timber.tag(TAG).d("SFTP file exists: %s", path);
            return true;
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                Timber.tag(TAG).d("SFTP file does not exist: %s", path);
                return false;
            } else {
                Timber.tag(TAG).e("Error checking if SFTP file exists: %s", e.getMessage());
                return false;
            }
        } catch (JSchException e) {
            Timber.tag(TAG).e("Error getting SFTP channel: %s", e.getMessage());
            return false;
        } finally {
            if (sftpChannelRepository != null && channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
                Timber.tag(TAG).d("SFTP channel released");
            }
        }
    }

    @Override
    public long lastModified() {
        Timber.tag(TAG).d("Getting last modified time for SFTP path: %s", path);
        SftpChannelRepository sftpChannelRepository = null;
        ChannelSftp channelSftp = null;
        try {
            sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            channelSftp = sftpChannelRepository.getSftpChannel();
            SftpATTRS attrs = channelSftp.lstat(path);
            return ((long) attrs.getMTime()) * 1000;
        } catch (SftpException | JSchException e) {
            Timber.tag(TAG).e("Failed to get last modified time: %s", e.getMessage());
            return 0;
        } finally {
            if (sftpChannelRepository != null && channelSftp != null) {
                sftpChannelRepository.releaseChannel(channelSftp);
                Timber.tag(TAG).d("SFTP channel released");
            }
        }
    }

    @Override
    public boolean isHidden() {
        return getName().startsWith(".");
    }

    public static class SFTPInputStreamWrapper extends InputStream {
        private final InputStream wrappedStream;
        private final ChannelSftp channelSftp;
        private final SftpChannelRepository repository;

        public SFTPInputStreamWrapper(InputStream wrappedStream, ChannelSftp channelSftp, SftpChannelRepository repository) {
            this.wrappedStream = wrappedStream;
            this.channelSftp = channelSftp;
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
        public void close() throws IOException {
            try {
                wrappedStream.close();
            } finally {
                repository.releaseChannel(channelSftp);
                Timber.tag("SFTPInputStreamWrapper").d("SFTP channel released");
            }
        }
    }

    public static class SFTPOutputStreamWrapper extends OutputStream {
        private final OutputStream wrappedStream;
        private final ChannelSftp channelSftp;
        private final SftpChannelRepository repository;

        public SFTPOutputStreamWrapper(OutputStream wrappedStream, ChannelSftp channelSftp, SftpChannelRepository repository) {
            this.wrappedStream = wrappedStream;
            this.channelSftp = channelSftp;
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
            } finally {
                repository.releaseChannel(channelSftp);
                Timber.tag("SFTPOutputStreamWrapper").d("SFTP channel released");
            }
        }
    }
}
