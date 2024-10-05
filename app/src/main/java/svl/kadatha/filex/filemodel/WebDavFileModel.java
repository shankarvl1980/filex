package svl.kadatha.filex.filemodel;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import svl.kadatha.filex.Global;
import svl.kadatha.filex.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.WebDavClientRepository;
import timber.log.Timber;

public class WebDavFileModel implements FileModel {

    private static final String TAG = "WebDavFileModel";
    private final String path;

    /**
     * Constructor for WebDavFileModel.
     *
     * @param path The absolute path of the WebDAV resource.
     */
    public WebDavFileModel(String path) {
        this.path = path;
        Timber.tag(TAG).d("WebDavFileModel created for path: %s", path);
    }

    /**
     * Helper method to determine if the current path is a directory.
     *
     * @return True if it's a directory, false otherwise.
     */
    private boolean isDirectoryInternal() {
        Sardine sardine;
        try {
            sardine = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO).getSardine();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for isDirectory check");
            return false;
        }
        try {
            // Sardine's list method with depth=0 retrieves only the properties of the resource itself
            List<DavResource> resources = sardine.list(path, 0);
            if (resources.isEmpty()) {
                Timber.tag(TAG).d("No resources found at path: %s", path);
                return false;
            }
            DavResource resource = resources.get(0);
            boolean isDir = resource.isDirectory();
            Timber.tag(TAG).d("isDirectoryInternal() for path: %s returned: %b", path, isDir);
            return isDir;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Error checking if path is directory: %s", path);
            return false;
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
        boolean isDir = isDirectoryInternal();
        Timber.tag(TAG).d("isDirectory() returned: %b for path: %s", isDir, path);
        return isDir;
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        String new_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(getParentPath(), new_name);
        Timber.tag(TAG).d("Attempting to rename from %s to %s", path, new_file_path);
        Sardine sardine;
        try {
            sardine = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO).getSardine();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for rename operation");
            return false;
        }

        try {
            if (overwrite && new WebDavFileModel(new_file_path).exists()) {
                boolean deleted = new WebDavFileModel(new_file_path).delete();
                if (!deleted) {
                    Timber.tag(TAG).w("Failed to delete existing resource at path: %s", new_file_path);
                    return false;
                }
            }
            sardine.move(path, new_file_path);
            Timber.tag(TAG).d("Rename operation succeeded from %s to %s", path, new_file_path);
            return true;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Rename operation failed: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete() {
        Timber.tag(TAG).d("Attempting to delete WebDAV resource: %s", path);
        try {
            deleteRecursively(path);
            Timber.tag(TAG).d("WebDAV resource deletion succeeded for path: %s", path);
            return true;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Error deleting WebDAV resource: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Recursively deletes a WebDAV resource (file or directory).
     *
     * @param targetPath The path to delete.
     * @throws IOException If an I/O error occurs.
     */
    private void deleteRecursively(String targetPath) throws IOException {
        Sardine sardine;
        try {
            sardine = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO).getSardine();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for delete operation");
            throw e;
        }

        WebDavFileModel targetModel = new WebDavFileModel(targetPath);
        if (targetModel.isDirectory()) {
            List<DavResource> resources = sardine.list(targetPath);
            for (DavResource resource : resources) {
                String resourcePath = resource.getPath();
                // Skip the directory itself
                if (resourcePath.equals(targetPath) || resourcePath.equals(targetPath + "/")) {
                    continue;
                }
                // Recursively delete sub-resources
                WebDavFileModel childModel = new WebDavFileModel(resourcePath);
                childModel.delete();
            }
            // After deleting all sub-resources, delete the directory itself
            sardine.delete(targetPath);
            Timber.tag(TAG).d("Deleted WebDAV directory: %s", targetPath);
        } else {
            // It's a file, delete directly
            sardine.delete(targetPath);
            Timber.tag(TAG).d("Deleted WebDAV file: %s", targetPath);
        }
    }

    @Override
    public InputStream getInputStream() {
        Timber.tag(TAG).d("Attempting to get InputStream for path: %s", path);
        Sardine sardine;
        try {
            sardine = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO).getSardine();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for getInputStream");
            return null;
        }
        try {
            InputStream inputStream = sardine.get(path);
            Timber.tag(TAG).d("Successfully retrieved InputStream for path: %s", path);
            return inputStream;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get InputStream: %s", e.getMessage());
            return null;
        }
    }

    @Override
    public OutputStream getChildOutputStream(String childName, long sourceLength) {
        String filePath = Global.CONCATENATE_PARENT_CHILD_PATH(path, childName);
        Timber.tag(TAG).d("Attempting to get OutputStream for path: %s", filePath);
        Sardine sardine;

        try {
            sardine = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO).getSardine();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for getChildOutputStream");
            return null;
        }

        try {
            // Define a threshold for in-memory vs. disk-based buffering (e.g., 10MB)
            final long MEMORY_THRESHOLD = 10 * 1024 * 1024; // 10MB

//            if (sourceLength > MEMORY_THRESHOLD) {
//                // Use disk-based buffering with a temporary file
//                return new WebDavOutputStreamWrapper(sardine, filePath, false);
//            }
//            else {
            // Use in-memory buffering
            return new WebDavOutputStreamWrapper(sardine, filePath, true);
            //}
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to create OutputStream wrapper: %s", e.getMessage());
            return null;
        }
    }

    @Override
    public FileModel[] list() {
        Timber.tag(TAG).d("Attempting to list files for path: %s", path);
        Sardine sardine;
        try {
            sardine = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO).getSardine();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for list operation");
            return new FileModel[0];
        }
        try {
            List<DavResource> resources = sardine.list(path);
            if (resources.isEmpty()) {
                Timber.tag(TAG).w("No files listed or directory is empty for path: %s", path);
                return new FileModel[0];
            }

            List<FileModel> fileModels = new ArrayList<>();
            for (DavResource resource : resources) {
                String resourcePath = resource.getPath();
                // Skip the directory itself
                if (!resourcePath.equals(path) && !resourcePath.equals(path + "/")) {
                    fileModels.add(new WebDavFileModel(resourcePath));
                    Timber.tag(TAG).d("Listed file: %s", resourcePath);
                }
            }
            Timber.tag(TAG).d("Successfully listed %d files", fileModels.size());
            return fileModels.toArray(new FileModel[0]);
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to list files: %s", e.getMessage());
            return new FileModel[0];
        }
    }

    @Override
    public boolean createFile(String name) {
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, name);
        Timber.tag(TAG).d("Attempting to create file: %s", file_path);
        Sardine sardine;
        try {
            sardine = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO).getSardine();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for createFile");
            return false;
        }
        try {
            sardine.put(file_path, new byte[0]); // Upload empty content
            Timber.tag(TAG).d("File creation succeeded for path: %s", file_path);
            return true;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to create file: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        String dir_path = Global.CONCATENATE_PARENT_CHILD_PATH(path, dir_name);
        Timber.tag(TAG).d("Attempting to create directory if not exists: %s", dir_path);
        Sardine sardine;
        try {
            sardine = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO).getSardine();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for makeDirIfNotExists");
            return false;
        }
        try {
            sardine.createDirectory(dir_path);
            Timber.tag(TAG).d("Directory creation succeeded for path: %s", dir_path);
            return true;
        } catch (IOException e) {
            // If the directory already exists, Sardine might throw an exception
            // You can check the exception message or status code to confirm
            if (e.getMessage().contains("Method Not Allowed")) {
                Timber.tag(TAG).d("Directory already exists: %s", dir_path);
                return true;
            }
            Timber.tag(TAG).e(e, "Failed to create directory: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        String fullPath = Global.CONCATENATE_PARENT_CHILD_PATH(path, extended_path);
        Timber.tag(TAG).d("Attempting to create directories recursively: %s", fullPath);
        Sardine sardine;
        try {
            sardine = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO).getSardine();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for makeDirsRecursively");
            return false;
        }
        try {
            sardine.createDirectory(fullPath);
            Timber.tag(TAG).d("Directories created recursively for path: %s", fullPath);
            return true;
        } catch (IOException e) {
            // Similar handling as makeDirIfNotExists
            if (e.getMessage().contains("Method Not Allowed")) {
                Timber.tag(TAG).d("Directories already exist: %s", fullPath);
                return true;
            }
            Timber.tag(TAG).e(e, "Failed to create directories recursively: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public long getLength() {
        Timber.tag(TAG).d("getLength() called for path: %s", path);
        Sardine sardine;
        try {
            sardine = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO).getSardine();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for getLength");
            return 0;
        }
        try {
            List<DavResource> resources = sardine.list(path, 0); // Depth 0
            if (!resources.isEmpty()) {
                DavResource resource = resources.get(0);
                long length = resource.getContentLength();
                Timber.tag(TAG).d("getLength() returned: %d for path: %s", length, path);
                return length;
            }
            return 0;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get length: %s", e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean exists() {
        Timber.tag(TAG).d("Checking if WebDAV resource exists: %s", path);
        Sardine sardine;
        try {
            sardine = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO).getSardine();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for exists");
            return false;
        }
        try {
            List<DavResource> resources = sardine.list(path, 0); // Depth 0
            boolean exists = !resources.isEmpty();
            Timber.tag(TAG).d("exists() returned: %b for path: %s", exists, path);
            return exists;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Error checking if WebDAV resource exists: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public long lastModified() {
        Timber.tag(TAG).d("lastModified() called for path: %s", path);
        Sardine sardine;
        try {
            sardine = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO).getSardine();
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get Sardine client for lastModified");
            return 0;
        }
        try {
            List<DavResource> resources = sardine.list(path, 0); // Depth 0
            if (!resources.isEmpty()) {
                DavResource resource = resources.get(0);
                Date modifiedDate = resource.getModified(); // Correctly using Date
                if (modifiedDate != null) {
                    long epoch = modifiedDate.getTime(); // Directly getting epoch time
                    Timber.tag(TAG).d("lastModified() returned: %d for path: %s", epoch, path);
                    return epoch;
                } else {
                    Timber.tag(TAG).w("Modified date is null for path: %s", path);
                    return 0;
                }
            }
            Timber.tag(TAG).w("No resources found at path: %s", path);
            return 0;
        } catch (IOException e) {
            Timber.tag(TAG).e(e, "Failed to get last modified: %s", e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean isHidden() {
        boolean hidden = new File(path).isHidden();
        Timber.tag(TAG).d("isHidden() returned: %b for path: %s", hidden, path);
        return hidden;
    }

    // Inner class to handle OutputStream for WebDAV
    public class WebDavOutputStreamWrapper extends OutputStream {
        private final ByteArrayOutputStream memoryBuffer;
        private final File tempFile;
        private final OutputStream outputStream;
        private final Sardine sardine;
        private final String filePath;
        private final boolean inMemory;
        private boolean isClosed = false;

        /**
         * Constructor for WebDavOutputStreamWrapper.
         *
         * @param sardine  The Sardine client instance.
         * @param filePath The destination path on the WebDAV server.
         * @param inMemory Flag indicating whether to buffer data in memory.
         * @throws IOException If an I/O error occurs.
         */
        public WebDavOutputStreamWrapper(Sardine sardine, String filePath, boolean inMemory) throws IOException {
            this.sardine = sardine;
            this.filePath = filePath;
            this.inMemory = inMemory;
            if (inMemory) {
                this.memoryBuffer = new ByteArrayOutputStream();
                this.tempFile = null;
                this.outputStream = this.memoryBuffer;
            } else {
                this.memoryBuffer = null;
                this.tempFile = File.createTempFile("webdav_upload_", null);
                this.outputStream = new FileOutputStream(this.tempFile);
            }
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            outputStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }

        /**
         * Closes the stream and uploads the data to the WebDAV server.
         *
         * @throws IOException If an I/O error occurs during upload.
         */
        @Override
        public void close() throws IOException {
            if (!isClosed) {
                try {
                    outputStream.close();
                    if (inMemory) {
                        // Upload data from memory buffer
                        try (InputStream is = new ByteArrayInputStream(memoryBuffer.toByteArray())) {
                            sardine.put(filePath, is);
                            Timber.tag(TAG).d("Uploaded file via OutputStream: %s", filePath);
                        }
                    } else {
                        // Upload data from temporary file
                        try (InputStream is = new FileInputStream(tempFile)) {
                            sardine.put(filePath, is);
                            Timber.tag(TAG).d("Uploaded file via OutputStream: %s", filePath);
                        }
                    }
                } catch (IOException e) {
                    Timber.tag(TAG).e(e, "Failed to upload file via OutputStream: %s", filePath);
                    throw e;
                } finally {
                    isClosed = true;
                    // Clean up temporary file if used
                    if (tempFile != null && tempFile.exists()) {
                        if (!tempFile.delete()) {
                            Timber.tag(TAG).w("Failed to delete temporary file: %s", tempFile.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

}
