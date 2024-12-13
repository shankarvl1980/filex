//package svl.kadatha.filex.filemodel;
//
//import com.microsoft.graph.requests.GraphServiceClient;
//import com.microsoft.graph.models.DriveItem;
//import com.microsoft.graph.models.Folder;
//import com.microsoft.graph.models.ItemReference;
//import com.microsoft.graph.options.Option;
//import com.microsoft.graph.core.ClientException;
//import com.microsoft.graph.requests.DriveItemCollectionPage;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.io.PipedInputStream;
//import java.io.PipedOutputStream;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * A OneDrive implementation of the FileModel interface.
// * This class assumes you have already created a GraphServiceClient with a valid access token.
// */
//public class OneDriveFileModel implements FileModel {
//    private final GraphServiceClient<?> graphClient;
//    private final String path;      // e.g. "/Folder/File.txt" (root-relative)
//    private DriveItem driveItem;    // Metadata of the current item
//
//    public OneDriveFileModel(GraphServiceClient<?> graphClient, String path) throws ClientException {
//        this.graphClient = graphClient;
//        this.path = normalizePath(path);
//        this.driveItem = getDriveItem(this.path);
//    }
//
//    private String normalizePath(String rawPath) {
//        // Convert leading "/" to empty for OneDrive addressing
//        // OneDrive format: root:/[path] for items
//        if (rawPath.equals("/")) {
//            return ""; // root represents top-level
//        } else if (rawPath.startsWith("/")) {
//            return rawPath.substring(1);
//        }
//        return rawPath;
//    }
//
//    private DriveItem getDriveItem(String relativePath) throws ClientException {
//        // Fetch the DriveItem by path
//        return graphClient.me()
//                .drive()
//                .root()
//                .itemWithPath(relativePath)
//                .buildRequest()
//                .get();
//    }
//
//    @Override
//    public String getName() {
//        return driveItem.name;
//    }
//
//    @Override
//    public String getParentName() {
//        String parentPath = getParentPath();
//        if (parentPath == null) return null;
//        try {
//            DriveItem parent = getDriveItem(parentPath);
//            return parent.name;
//        } catch (ClientException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    @Override
//    public String getPath() {
//        return "/" + path; // returning as a root-based path
//    }
//
//    @Override
//    public String getParentPath() {
//        if (path.isEmpty()) {
//            return null; // Root has no parent
//        }
//        int lastSlash = path.lastIndexOf('/');
//        if (lastSlash == -1) {
//            return ""; // Parent is root
//        }
//        return path.substring(0, lastSlash);
//    }
//
//    @Override
//    public boolean isDirectory() {
//        return driveItem.folder != null;
//    }
//
//    @Override
//    public boolean rename(String new_name, boolean overwrite) {
//        // To rename, we patch the item with a new name
//        DriveItem updatedItem = new DriveItem();
//        updatedItem.name = new_name;
//        try {
//            driveItem = graphClient.me().drive().items(driveItem.id)
//                    .buildRequest()
//                    .patch(updatedItem);
//            return true;
//        } catch (ClientException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    @Override
//    public boolean delete() {
//        try {
//            graphClient.me().drive().items(driveItem.id)
//                    .buildRequest()
//                    .delete();
//            return true;
//        } catch (ClientException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    @Override
//    public InputStream getInputStream() {
//        if (isDirectory()) return null;
//        try {
//            // Download file content
//            return graphClient.me().drive().items(driveItem.id)
//                    .content()
//                    .buildRequest()
//                    .get();
//        } catch (ClientException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    @Override
//    public OutputStream getChildOutputStream(String child_name, long source_length) {
//        if (!isDirectory()) return null;
//
//        String childPath = path.isEmpty() ? child_name : (path + "/" + child_name);
//
//        PipedOutputStream outputStream = new PipedOutputStream();
//        try {
//            PipedInputStream inputStream = new PipedInputStream(outputStream);
//
//            new Thread(() -> {
//                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//                    byte[] buffer = new byte[8192];
//                    int len;
//                    while ((len = inputStream.read(buffer)) != -1) {
//                        baos.write(buffer, 0, len);
//                    }
//
//                    byte[] data = baos.toByteArray();
//
//                    // Upload the fully read data to OneDrive
//                    graphClient.me().drive().root().itemWithPath(childPath)
//                            .content()
//                            .buildRequest()
//                            .put(data);
//
//                } catch (IOException | ClientException e) {
//                    e.printStackTrace();
//                }
//            }).start();
//
//            return outputStream;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    @Override
//    public FileModel[] list() {
//        if (!isDirectory()) return new FileModel[0];
//        try {
//            DriveItemCollectionPage childrenPage = graphClient.me().drive().items(driveItem.id)
//                    .children()
//                    .buildRequest()
//                    .get();
//
//            List<FileModel> fileModels = new ArrayList<>();
//            for (DriveItem item : childrenPage.getCurrentPage()) {
//                String childPath = (path.isEmpty()) ? item.name : path + "/" + item.name;
//                fileModels.add(new OneDriveFileModel(graphClient, "/" + childPath));
//            }
//            return fileModels.toArray(new FileModel[0]);
//        } catch (ClientException e) {
//            e.printStackTrace();
//            return new FileModel[0];
//        }
//    }
//
//    @Override
//    public boolean createFile(String name) {
//        if (!isDirectory()) return false;
//
//        String filePath = path.isEmpty() ? name : path + "/" + name;
//        byte[] data = new byte[0]; // Empty file content
//
//        try {
//            graphClient.me().drive().root().itemWithPath(filePath)
//                    .content()
//                    .buildRequest()
//                    .put(data);
//            return true;
//        } catch (ClientException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//
//    @Override
//    public boolean makeDirIfNotExists(String dir_name) {
//        if (!isDirectory()) return false;
//        String dirPath = path.isEmpty() ? dir_name : path + "/" + dir_name;
//
//        DriveItem newFolder = new DriveItem();
//        newFolder.name = dir_name;
//        newFolder.folder = new Folder();
//        newFolder.name = dir_name;
//
//        try {
//            graphClient.me().drive().root().itemWithPath(path)
//                    .children()
//                    .buildRequest()
//                    .post(newFolder);
//            return true;
//        } catch (ClientException e) {
//            // If conflict, possibly folder exists. Check if it exists:
//            try {
//                getDriveItem(dirPath);
//                return true;
//            } catch (ClientException ce) {
//                ce.printStackTrace();
//                return false;
//            }
//        }
//    }
//
//    @Override
//    public boolean makeDirsRecursively(String extended_path) {
//        if (!isDirectory()) return false;
//        String[] dirs = extended_path.split("/");
//        String currentPath = path;
//        for (String dir : dirs) {
//            if (dir.isEmpty()) continue;
//            String nextPath = currentPath.isEmpty() ? dir : currentPath + "/" + dir;
//
//            // Try creating folder
//            DriveItem folder = new DriveItem();
//            folder.name = dir;
//            folder.folder = new Folder();
//            try {
//                graphClient.me().drive().root().itemWithPath(currentPath)
//                        .children()
//                        .buildRequest()
//                        .post(folder);
//            } catch (ClientException e) {
//                // Check if it already exists
//                try {
//                    getDriveItem(nextPath);
//                    // Exists, continue
//                } catch (ClientException ce) {
//                    ce.printStackTrace();
//                    return false;
//                }
//            }
//            currentPath = nextPath;
//        }
//        return true;
//    }
//
//    @Override
//    public long getLength() {
//        if (isDirectory()) return 0;
//        return driveItem.size == null ? 0 : driveItem.size;
//    }
//
//    @Override
//    public boolean exists() {
//        try {
//            getDriveItem(path);
//            return true;
//        } catch (ClientException e) {
//            // If not found, handle that
//            if (e.getMessage() != null && e.getMessage().contains("itemNotFound")) {
//                return false;
//            }
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    @Override
//    public long lastModified() {
//        if (driveItem.file != null && driveItem.lastModifiedDateTime != null) {
//            // Convert OffsetDateTime to epoch milliseconds
//            long epochSecond = driveItem.lastModifiedDateTime.toEpochSecond();
//            int nano = driveItem.lastModifiedDateTime.getNano();
//            return epochSecond * 1000 + nano / 1_000_000;
//        }
//        return 0;
//    }
//
//
//
//
//
//    @Override
//    public boolean isHidden() {
//        return getName().startsWith(".");
//    }
//}
