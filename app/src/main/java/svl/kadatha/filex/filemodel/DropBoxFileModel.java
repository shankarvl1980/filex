package svl.kadatha.filex.filemodel;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DropBoxFileModel implements FileModel {
    private final String accessToken;
    private final DbxClientV2 dbxClient;
    private final String path; // Dropbox path, e.g., "/Folder/File.txt"
    private Metadata metadata;

    public DropBoxFileModel(String accessToken, String path) throws DbxException {
        this.accessToken = accessToken;
        this.dbxClient = new DbxClientV2(new com.dropbox.core.DbxRequestConfig("YourAppName"), accessToken);
        this.path = path;
        this.metadata = dbxClient.files().getMetadata(path);
    }

    @Override
    public String getName() {
        return metadata.getName();
    }

    @Override
    public String getParentName() {
        String parentPath = getParentPath();
        if (parentPath != null && !parentPath.equals("/")) {
            try {
                Metadata parentMetadata = dbxClient.files().getMetadata(parentPath);
                return parentMetadata.getName();
            } catch (DbxException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getParentPath() {
        if (path.equals("/")) {
            return null;
        }
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex == 0) {
            return "/";
        } else if (lastSlashIndex > 0) {
            return path.substring(0, lastSlashIndex);
        } else {
            return null;
        }
    }

    @Override
    public boolean isDirectory() {
        return metadata instanceof FolderMetadata;
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        String parentPath = getParentPath();
        String newPath = parentPath + "/" + new_name;
        try {
            RelocationResult result = dbxClient.files().moveV2(path, newPath);
            metadata = result.getMetadata();
            return true;
        } catch (DbxException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete() {
        try {
            dbxClient.files().deleteV2(path);
            return true;
        } catch (DbxException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public InputStream getInputStream() {
        if (isDirectory()) {
            return null;
        }
        try {
            FileMetadata fileMetadata = (FileMetadata) metadata;
            return dbxClient.files().download(fileMetadata.getPathLower()).getInputStream();
        } catch (DbxException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        if (!isDirectory()) {
            return null;
        }
        String childPath = path.equals("/") ? "/" + child_name : path + "/" + child_name;
        PipedOutputStream outputStream = new PipedOutputStream();
        try {
            PipedInputStream inputStream = new PipedInputStream(outputStream);
            new Thread(() -> {
                try {
                    dbxClient.files().uploadBuilder(childPath)
                            .withMode(WriteMode.ADD)
                            .uploadAndFinish(inputStream);
                } catch (DbxException | IOException e) {
                    e.printStackTrace();
                }
            }).start();
            return outputStream;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public FileModel[] list() {
        if (!isDirectory()) {
            return new FileModel[0];
        }
        try {
            ListFolderResult result = dbxClient.files().listFolder(path.equals("/") ? "" : path);
            List<FileModel> fileModels = new ArrayList<>();
            for (Metadata metadata : result.getEntries()) {
                fileModels.add(new DropBoxFileModel(accessToken, metadata.getPathLower()));
            }
            return fileModels.toArray(new FileModel[0]);
        } catch (DbxException e) {
            e.printStackTrace();
            return new FileModel[0];
        }
    }

    @Override
    public boolean createFile(String name) {
        if (!isDirectory()) {
            return false;
        }
        String filePath = path.equals("/") ? "/" + name : path + "/" + name;
        try (InputStream in = new ByteArrayInputStream(new byte[0])) {
            dbxClient.files().uploadBuilder(filePath)
                    .withMode(WriteMode.ADD)
                    .uploadAndFinish(in);
            return true;
        } catch (DbxException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        if (!isDirectory()) {
            return false;
        }
        String dirPath = path.equals("/") ? "/" + dir_name : path + "/" + dir_name;
        try {
            dbxClient.files().createFolderV2(dirPath);
            return true;
        } catch (CreateFolderErrorException e) {
            if (e.errorValue.isPath() && e.errorValue.getPathValue().isConflict()) {
                // Folder already exists
                return true;
            } else {
                e.printStackTrace();
                return false;
            }
        } catch (DbxException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        if (!isDirectory()) {
            return false;
        }
        String[] dirs = extended_path.split("/");
        String currentPath = path;
        for (String dir : dirs) {
            if (dir.isEmpty()) continue;
            currentPath = currentPath.equals("/") ? "/" + dir : currentPath + "/" + dir;
            try {
                dbxClient.files().createFolderV2(currentPath);
            } catch (CreateFolderErrorException e) {
                if (e.errorValue.isPath() && e.errorValue.getPathValue().isConflict()) {
                    // Folder already exists
                    continue;
                } else {
                    e.printStackTrace();
                    return false;
                }
            } catch (DbxException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public long getLength() {
        if (isDirectory()) {
            return 0;
        }
        FileMetadata fileMetadata = (FileMetadata) metadata;
        return fileMetadata.getSize();
    }

    @Override
    public boolean exists() {
        try {
            dbxClient.files().getMetadata(path);
            return true;
        } catch (GetMetadataErrorException e) {
            if (e.errorValue.isPath() && e.errorValue.getPathValue().isNotFound()) {
                return false;
            } else {
                e.printStackTrace();
                return false;
            }
        } catch (DbxException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public long lastModified() {
        if (isDirectory()) {
            return 0;
        }
        FileMetadata fileMetadata = (FileMetadata) metadata;
        return fileMetadata.getServerModified().getTime();
    }

    @Override
    public boolean isHidden() {
        return getName().startsWith(".");
    }
}

