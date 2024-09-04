package svl.kadatha.filex.filemodel;

import org.apache.commons.net.ftp.FTPClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import svl.kadatha.filex.FileUtil;
import svl.kadatha.filex.FtpClientRepository;
import svl.kadatha.filex.FtpDetailsViewModel;
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
        boolean isDir = FileUtil.isFtpPathDirectory(path);
        Timber.tag(TAG).d("isDirectory() returned: %b for path: %s", isDir, path);
        return isDir;
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        String new_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(getParentPath(), new_name);
        Timber.tag(TAG).d("Attempting to rename from %s to %s", path, new_file_path);
        try {
            FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
            FTPClient ftpClient = ftpClientRepository.getFtpClient();
            boolean renamed = ftpClient.rename(path, new_file_path);
            ftpClientRepository.releaseFtpClient(ftpClient);
            Timber.tag(TAG).d("Rename operation result: %b", renamed);
            return renamed;
        } catch (IOException e) {
            Timber.tag(TAG).e("Rename operation failed: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete() {
        boolean deleted = FileUtil.deleteFtpDirectory(path);
        Timber.tag(TAG).d("delete() returned: %b for path: %s", deleted, path);
        return deleted;
    }

    @Override
    public InputStream getInputStream() {
        Timber.tag(TAG).d("Attempting to get InputStream for path: %s", path);
        try {
            FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
            FTPClient ftpClient = ftpClientRepository.getFtpClient();
            InputStream inputStream = ftpClient.retrieveFileStream(path);
            ftpClientRepository.releaseFtpClient(ftpClient);
            Timber.tag(TAG).d("Successfully retrieved InputStream for path: %s", path);
            return inputStream;
        } catch (Exception e) {
            Timber.tag(TAG).e("Failed to get InputStream: %s", e.getMessage());
            return null;
        }
    }

    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, child_name);
        Timber.tag(TAG).d("Attempting to get OutputStream for path: %s", file_path);
        try {
            FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
            FTPClient ftpClient = ftpClientRepository.getFtpClient();
            OutputStream outputStream = ftpClient.storeFileStream(file_path);
            ftpClientRepository.releaseFtpClient(ftpClient);
            Timber.tag(TAG).d("Successfully retrieved OutputStream for path: %s", file_path);
            return outputStream;
        } catch (Exception e) {
            Timber.tag(TAG).e("Failed to get OutputStream: %s", e.getMessage());
            return null;
        }
    }

    @Override
    public FileModel[] list() {
        Timber.tag(TAG).d("Attempting to list files for path: %s", path);
        try {
            FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
            FTPClient ftpClient = ftpClientRepository.getFtpClient();
            String[] inner_source_list = ftpClient.listNames(path);
            ftpClientRepository.releaseFtpClient(ftpClient);

            if (inner_source_list == null) {
                Timber.tag(TAG).w("No files listed or directory is empty for path: %s", path);
                return new FileModel[0];
            }

            int size = inner_source_list.length;
            FileModel[] fileModels = new FileModel[size];
            for (int i = 0; i < size; ++i) {
                Timber.tag(TAG).d("Listed file: %s", inner_source_list[i]);
                fileModels[i] = new FtpFileModel(inner_source_list[i]);
            }
            Timber.tag(TAG).d("Successfully listed %d files", size);
            return fileModels;
        } catch (IOException e) {
            Timber.tag(TAG).e("Failed to list files: %s", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean createFile(String name) {
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
        Timber.tag(TAG).d("Attempting to create file: %s", file_path);
        InputStream bin = new ByteArrayInputStream(new byte[0]);
        try {
            FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
            FTPClient ftpClient = ftpClientRepository.getFtpClient();
            boolean created = ftpClient.storeFile(file_path, bin);
            ftpClientRepository.releaseFtpClient(ftpClient);
            Timber.tag(TAG).d("File creation result: %b for path: %s", created, file_path);
            return created;
        } catch (IOException e) {
            Timber.tag(TAG).e("Failed to create file: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        String dir_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, dir_name);
        boolean created = FileUtil.mkdirFtp(dir_path);
        Timber.tag(TAG).d("makeDirIfNotExists() returned: %b for path: %s", created, dir_path);
        return created;
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        boolean created = FileUtil.mkdirsFTP(path, extended_path);
        Timber.tag(TAG).d("makeDirsRecursively() returned: %b for path: %s", created, Global.CONCATENATE_PARENT_CHILD_PATH(path, extended_path));
        return created;
    }

    @Override
    public long getLength() {
        Timber.tag(TAG).d("getLength() called, but always returns 0 for FTP files");
        return 0;
    }

    @Override
    public boolean exists() {
        boolean exists = FileUtil.isFtpFileExists(path);
        Timber.tag(TAG).d("exists() returned: %b for path: %s", exists, path);
        return exists;
    }
}