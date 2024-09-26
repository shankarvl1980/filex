package svl.kadatha.filex.filemodel;

import androidx.annotation.NonNull;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import svl.kadatha.filex.FtpClientRepository;
import svl.kadatha.filex.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.Global;
import timber.log.Timber;

public class FtpFileModel implements FileModel {

    private static final String TAG = "Ftp-FtpFileModel";
    private final String path;

    FtpFileModel(String path) {
        this.path = path;
        Timber.tag(TAG).d("FtpFileModel created for path: %s", path);
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
        String new_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(getParentPath(), new_name);
        Timber.tag(TAG).d("Attempting to rename from %s to %s", path, new_file_path);
        FtpClientRepository ftpClientRepository = null;
        FTPClient ftpClient = null;
        try {
            ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
            ftpClient = ftpClientRepository.getFtpClient();
            boolean renamed = ftpClient.rename(path, new_file_path);
            Timber.tag(TAG).d("Rename operation result: %b", renamed);
            return renamed;
        } catch (IOException e) {
            Timber.tag(TAG).e("Rename operation failed: %s", e.getMessage());
            return false;
        }
        finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
                Timber.tag(TAG).d("FTP client released");
            } 
        }
        
    }

    @Override
    public boolean delete() {
        Timber.tag(TAG).d("Attempting to delete FTP directory: %s", path);
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient = null;
        boolean success = true;

        try {
            ftpClient = ftpClientRepository.getFtpClient();
            if (ftpClient == null) {
                throw new IllegalStateException("Failed to obtain FTP client");
            }

            Stack<String> stack = new Stack<>();
            stack.push(path);

            while (!stack.isEmpty() && success) {
                String currentPath = stack.pop();
                String baseName = new File(currentPath).getName();
                if (".".equals(baseName) || "..".equals(baseName)) {
                    Timber.tag(TAG).w("Skipping deletion of special directory: %s", currentPath);
                    continue;
                }

                if (isDirectory(currentPath)) {
                    String[] list = ftpClient.listNames(currentPath);
                    if (list != null && list.length > 0) {
                        for (String item : list) {
                            String itemBaseName = new File(item).getName();
                            if (!".".equals(itemBaseName) && !"..".equals(itemBaseName)) {
                                stack.push(item);
                            }
                        }
                    } else {
                        success = ftpClient.removeDirectory(currentPath);
                    }
                } else {
                    success = ftpClient.deleteFile(currentPath);
                }

                if (!success) {
                    Timber.tag(TAG).e("Failed to delete: %s", currentPath);
                }
            }

            // If the original path was a directory and all contents were successfully deleted, delete the directory itself
            if (success && isDirectory(path)) {
                success = ftpClient.removeDirectory(path);
            }

            Timber.tag(TAG).d("FTP directory deletion result: %b for path: %s", success, path);
        } catch (IOException e) {
            Timber.tag(TAG).e("Error deleting FTP directory: %s", e.getMessage());
            success = false;
        } catch (IllegalStateException e) {
            Timber.tag(TAG).e("Failed to obtain FTP client: %s", e.getMessage());
            success = false;
        } finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
                Timber.tag(TAG).d("FTP client released");
            }
        }
        return success;
    }

    @Override
    public InputStream getInputStream() {
        Timber.tag(TAG).d("Attempting to get InputStream for path: %s", path);
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient = null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();
            InputStream inputStream = ftpClient.retrieveFileStream(path);
            if (inputStream == null) {
                throw new IOException("Failed to retrieve file stream");
            }
            Timber.tag(TAG).d("Successfully retrieved InputStream for path: %s", path);
            return new FTPInputStreamWrapper(inputStream, ftpClient, ftpClientRepository);
        } catch (Exception e) {
            Timber.tag(TAG).e("Failed to get InputStream: %s", e.getMessage());
            if (ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
            return null;
        }
    }


    public OutputStream getChildOutputStream(String child_name, long source_length) {
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, child_name);
        Timber.tag(TAG).d("Attempting to get OutputStream for path: %s", file_path);
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient = null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();
            OutputStream outputStream = ftpClient.storeFileStream(file_path);
            Timber.tag(TAG).d("Successfully retrieved OutputStream for path: %s", file_path);
            return new FTPOutputStreamWrapper(outputStream, ftpClient, ftpClientRepository);
        } catch (IOException e) {
            Timber.tag(TAG).e("Failed to get OutputStream: %s", e.getMessage());
            if (ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
            return null;
        }
    }

    @Override
    public FileModel[] list() {
        Timber.tag(TAG).d("Attempting to list files for path: %s", path);
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient = null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();
            if (ftpClient == null) {
                throw new IllegalStateException("Failed to obtain FTP client");
            }

            String[] fullPaths = ftpClient.listNames(path);
            if (fullPaths == null || fullPaths.length == 0) {
                Timber.tag(TAG).w("No files listed or directory is empty for path: %s", path);
                return new FileModel[0];
            }

            List<FileModel> fileModels = new ArrayList<>();
            for (String fileName : fullPaths) {
                String baseName = new File(fileName).getName();
                if (!baseName.equals(".") && !baseName.equals("..")) {
                    fileModels.add(new FtpFileModel(fileName));
                    Timber.tag(TAG).d("Listed file: %s", fileName);
                }
            }
            Timber.tag(TAG).d("Successfully listed %d files", fileModels.size());
            return fileModels.toArray(new FileModel[0]);
        } catch (IOException e) {
            Timber.tag(TAG).e("Failed to list files: %s", e.getMessage());
            return new FileModel[0];
        } catch (IllegalStateException e) {
            Timber.tag(TAG).e("Failed to obtain FTP client: %s", e.getMessage());
            return new FileModel[0];
        } finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
                Timber.tag(TAG).d("FTP client released");
            }
        }
    }

    @Override
    public boolean createFile(String name) {
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
        Timber.tag(TAG).d("Attempting to create file: %s", file_path);
        InputStream bin = new ByteArrayInputStream(new byte[0]);
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient=null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();
            boolean created = ftpClient.storeFile(file_path, bin);
            Timber.tag(TAG).d("File creation result: %b for path: %s", created, file_path);
            return created;
        } catch (IOException e) {
            Timber.tag(TAG).e("Failed to create file: %s", e.getMessage());
            return false;
        }
        finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
                Timber.tag(TAG).d("FTP client released");
            }
        }
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        String dir_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, dir_name);
        boolean created = mkdirFtp(dir_path);
        Timber.tag(TAG).d("makeDirIfNotExists() returned: %b for path: %s", created, dir_path);
        return created;
    }


    @Override
    public boolean makeDirsRecursively(String extended_path) {
        boolean created = mkdirsFTP(path, extended_path);
        Timber.tag(TAG).d("makeDirsRecursively() returned: %b for path: %s", created, Global.CONCATENATE_PARENT_CHILD_PATH(path, extended_path));
        return created;
    }


    @Override
    public long getLength() {
        Timber.tag(TAG).d("getLength() called, but always returns 0 for FTP files");
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient=null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();
            FTPFile[] files = ftpClient.listFiles(path);

            if (files.length == 1) {
                FTPFile file = files[0];
                return file.getSize();
            }
        } catch (IOException e) {
            return 0;
        }
        finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
                Timber.tag(TAG).d("FTP client released");
            }
        }

        return 0;
    }

    @Override
    public boolean exists() {
        Timber.tag(TAG).d("Checking if FTP file exists: %s", path);
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient=null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();

            String parentDir = new File(path).getParent();
            String fileName = new File(path).getName();

            FTPFileFilter filter = ftpFile -> ftpFile.getName().equals(fileName);

            FTPFile[] files = ftpClient.listFiles(parentDir, filter);

            boolean exists = files.length > 0;

            Timber.tag(TAG).d("FTP file exists result: %b for path: %s", exists, path);
            return exists;
        } catch (IOException e) {
            Timber.tag(TAG).e("Error checking if FTP file exists: %s", e.getMessage());
            return false;
        }
        finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
                Timber.tag(TAG).d("FTP client released");
            }
        }
    }

    @Override
    public long lastModified() {
        return 0;
    }

    @Override
    public boolean isHidden() {
        return path.startsWith(".");
    }


    public static class FTPOutputStreamWrapper extends OutputStream {
        private final OutputStream wrappedStream;
        private final FTPClient ftpClient;
        private final FtpClientRepository repository;
        private boolean isTransferCompleted = false;

        public FTPOutputStreamWrapper(OutputStream wrappedStream, FTPClient ftpClient, FtpClientRepository repository) {
            this.wrappedStream = wrappedStream;
            this.ftpClient = ftpClient;
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

        public void completePendingCommand() throws IOException {
            if (!isTransferCompleted) {
                wrappedStream.close();
                if (!ftpClient.completePendingCommand()) {
                    throw new IOException("FTP file transfer failed");
                }
                isTransferCompleted = true;
            }
        }

        @Override
        public void close() throws IOException {
            try {
                if (!isTransferCompleted) {
                    completePendingCommand();
                }
            } finally {
                repository.releaseFtpClient(ftpClient);
            }
        }
    }

    public static class FTPInputStreamWrapper extends InputStream {
        private final InputStream wrappedStream;
        private final FTPClient ftpClient;
        private final FtpClientRepository repository;
        private boolean isTransferCompleted = false;

        public FTPInputStreamWrapper(InputStream wrappedStream, FTPClient ftpClient, FtpClientRepository repository) {
            this.wrappedStream = wrappedStream;
            this.ftpClient = ftpClient;
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
                if (!isTransferCompleted) {
                    if (!ftpClient.completePendingCommand()) {
                        throw new IOException("FTP file transfer failed");
                    }
                    isTransferCompleted = true;
                }
            } finally {
                repository.releaseFtpClient(ftpClient);
                Timber.tag("FTPInputStreamWrapper").d("FTP client released");
            }
        }
    }

    private static boolean mkdirFtp(String file_path) {
        Timber.tag(TAG).d("Attempting to create FTP directory: %s", file_path);
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient=null;
        try {

            boolean dirExists = isDirectory(file_path);
            if (dirExists) {
                Timber.tag(TAG).d("FTP directory already exists: %s", file_path);
                return true;
            } else {
                ftpClient = ftpClientRepository.getFtpClient();
                boolean success = ftpClient.makeDirectory(file_path);
                Timber.tag(TAG).d("FTP directory creation result: %b for path: %s", success, file_path);
                return success;
            }
        } catch (IOException e) {
            Timber.tag(TAG).e("Error creating FTP directory: %s", e.getMessage());
            return false;
        }
        finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
                Timber.tag(TAG).d("FTP client released");
            }
        }
    }

    private static boolean mkdirsFTP(String parentPath, @NonNull String extendedPath) {
        Timber.tag(TAG).d("Attempting to create multiple FTP directories: %s in %s", extendedPath, parentPath);
        String[] pathSegments = extendedPath.split("/");
        String currentPath = parentPath;
        for (String segment : pathSegments) {
            if (!segment.isEmpty()) {
                currentPath = Global.CONCATENATE_PARENT_CHILD_PATH(currentPath, segment);
                if (!mkdirFtp(currentPath)) {
                    Timber.tag(TAG).w("Failed to create FTP directory: %s", currentPath);
                    return false;
                }
            }
        }
        Timber.tag(TAG).d("Successfully created multiple FTP directories");
        return true;
    }

    private static boolean isDirectory(String filePath) {
        Timber.tag(TAG).d("Checking if FTP path is directory: %s", filePath);
        FtpClientRepository ftpClientRepository = null;
        FTPClient ftpClient = null;
        try {
            ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
            ftpClient = ftpClientRepository.getFtpClient();
            boolean isDirectory = ftpClient.changeWorkingDirectory(filePath);
            Timber.tag(TAG).d("FTP path is directory result: %b for path: %s", isDirectory, filePath);
            return isDirectory;
        } catch (IOException e) {
            Timber.tag(TAG).e("Error checking if FTP path is directory: %s", e.getMessage());
            return false;
        } finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
        }
    }
}