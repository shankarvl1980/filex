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

import svl.kadatha.filex.Global;
import svl.kadatha.filex.network.FtpClientRepository;
import svl.kadatha.filex.network.NetworkAccountDetailsViewModel;

public class FtpFileModel implements FileModel {

    private final String path;

    FtpFileModel(String path) {
        this.path = path;
    }

    private static boolean mkdirFtp(String file_path) {
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient = null;
        try {

            boolean dirExists = isDirectory(file_path);
            if (dirExists) {
                return true;
            } else {
                ftpClient = ftpClientRepository.getFtpClient();
                return ftpClient.makeDirectory(file_path);
            }
        } catch (IOException e) {
            return false;
        } finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
        }
    }

    private static boolean mkdirsFTP(String parentPath, @NonNull String extendedPath) {
        String[] pathSegments = extendedPath.split("/");
        String currentPath = parentPath;
        for (String segment : pathSegments) {
            if (!segment.isEmpty()) {
                currentPath = Global.CONCATENATE_PARENT_CHILD_PATH(currentPath, segment);
                if (!mkdirFtp(currentPath)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isDirectory(String filePath) {
        FtpClientRepository ftpClientRepository = null;
        FTPClient ftpClient = null;
        try {
            ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
            ftpClient = ftpClientRepository.getFtpClient();
            return ftpClient.changeWorkingDirectory(filePath);
        } catch (IOException e) {
            return false;
        } finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
        }
    }

    @Override
    public String getName() {
        return new File(path).getName();
    }

    @Override
    public String getParentName() {
        File parentFile = new File(path).getParentFile();
        return (parentFile != null) ? parentFile.getName() : null;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getParentPath() {
        return new File(path).getParent();
    }

    @Override
    public boolean isDirectory() {
        return isDirectory(path);
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        String new_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(getParentPath(), new_name);
        FtpClientRepository ftpClientRepository = null;
        FTPClient ftpClient = null;
        try {
            ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
            ftpClient = ftpClientRepository.getFtpClient();
            return ftpClient.rename(path, new_file_path);
        } catch (IOException e) {
            return false;
        } finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
        }

    }

    @Override
    public boolean delete() {
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
                }
            }

            // If the original path was a directory and all contents were successfully deleted, delete the directory itself
            if (success && isDirectory(path)) {
                success = ftpClient.removeDirectory(path);
            }

        } catch (IOException e) {
            success = false;
        } catch (IllegalStateException e) {
            success = false;
        } finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
        }
        return success;
    }

    @Override
    public InputStream getInputStream() {
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient = null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();
            InputStream inputStream = ftpClient.retrieveFileStream(path);
            if (inputStream == null) {
                throw new IOException("Failed to retrieve file stream");
            }
            return new FTPInputStreamWrapper(inputStream, ftpClient, ftpClientRepository);
        } catch (Exception e) {
            if (ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
            return null;
        }
    }

    public OutputStream getChildOutputStream(String child_name, long source_length) {
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, child_name);
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient = null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();
            OutputStream outputStream = ftpClient.storeFileStream(file_path);
            return new FTPOutputStreamWrapper(outputStream, ftpClient, ftpClientRepository);
        } catch (IOException e) {
            if (ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
            return null;
        }
    }

    @Override
    public FileModel[] list() {
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient = null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();
            if (ftpClient == null) {
                throw new IllegalStateException("Failed to obtain FTP client");
            }

            String[] fullPaths = ftpClient.listNames(path);
            if (fullPaths == null || fullPaths.length == 0) {
                return new FileModel[0];
            }

            List<FileModel> fileModels = new ArrayList<>();
            for (String fileName : fullPaths) {
                String baseName = new File(fileName).getName();
                if (!baseName.equals(".") && !baseName.equals("..")) {
                    fileModels.add(new FtpFileModel(fileName));
                }
            }
            return fileModels.toArray(new FileModel[0]);
        } catch (IOException e) {
            return new FileModel[0];
        } catch (IllegalStateException e) {
            return new FileModel[0];
        } finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
        }
    }

    @Override
    public boolean createFile(String name) {
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
        InputStream bin = new ByteArrayInputStream(new byte[0]);
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient = null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();
            return ftpClient.storeFile(file_path, bin);
        } catch (IOException e) {
            return false;
        } finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
        }
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        String dir_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, dir_name);
        return mkdirFtp(dir_path);
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        return mkdirsFTP(path, extended_path);
    }

    @Override
    public long getLength() {
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient = null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();
            FTPFile[] files = ftpClient.listFiles(path);

            if (files.length == 1) {
                FTPFile file = files[0];
                return file.getSize();
            }
        } catch (IOException e) {
            return 0;
        } finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
            }
        }

        return 0;
    }

    @Override
    public boolean exists() {
        FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
        FTPClient ftpClient = null;
        try {
            ftpClient = ftpClientRepository.getFtpClient();

            String parentDir = new File(path).getParent();
            String fileName = new File(path).getName();

            FTPFileFilter filter = ftpFile -> ftpFile.getName().equals(fileName);

            FTPFile[] files = ftpClient.listFiles(parentDir, filter);

            return files.length > 0;
        } catch (IOException e) {
            return false;
        } finally {
            if (ftpClientRepository != null && ftpClient != null) {
                ftpClientRepository.releaseFtpClient(ftpClient);
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
            }
        }
    }
}