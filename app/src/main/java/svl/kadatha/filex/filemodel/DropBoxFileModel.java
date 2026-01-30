package svl.kadatha.filex.filemodel;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.RelocationResult;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.UploadSessionFinishErrorException;
import com.dropbox.core.v2.files.UploadSessionStartResult;
import com.dropbox.core.v2.files.WriteMode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DropBoxFileModel implements FileModel, StreamUploadFileModel {

    // Dropbox requires chunk size multiple of 4MB for session append (recommendation).
    // 8MB is fine. You can tune.
    private static final int CHUNK_SIZE = 8 * 1024 * 1024;

    private final String accessToken;
    private final DbxClientV2 dbxClient;
    private final String path; // app-level path: "/" for root, else "/Folder/File"
    private Metadata metadata;

    public DropBoxFileModel(String accessToken, String path) throws DbxException {
        this(accessToken, newDbxClient(accessToken), normalizePath(path));
    }

    // internal ctor to reuse a single client
    private DropBoxFileModel(String accessToken, DbxClientV2 client, String path) throws DbxException {
        this.accessToken = accessToken;
        this.dbxClient = client;
        this.path = normalizePath(path);
        this.metadata = fetchMetadataOrRoot(this.path);
    }

    private DropBoxFileModel(String accessToken, DbxClientV2 client, String path, Metadata meta) {
        this.accessToken = accessToken;
        this.dbxClient = client;
        this.path = normalizePath(path);
        this.metadata = meta; // already have it from list
    }

    private static DbxClientV2 newDbxClient(String accessToken) {
        DbxRequestConfig cfg = DbxRequestConfig.newBuilder("FileX").build();
        return new DbxClientV2(cfg, accessToken);
    }

    private static String normalizePath(String p) {
        if (p == null || p.trim().isEmpty()) return "/";
        p = p.trim();
        if (!p.startsWith("/")) p = "/" + p;
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }

    // Dropbox API expects "" for root in many endpoints
    private static String toDropboxArgPath(String normalizedPath) {
        return "/".equals(normalizedPath) ? "" : normalizedPath;
    }

    /**
     * Reads up to max bytes. Returns number of bytes read, or -1 if EOF immediately.
     * This prevents "tiny reads" from PipedInputStream etc. causing too many small chunks.
     */
    private static int readFullyUpTo(InputStream in, byte[] buf, int max) throws IOException {
        int total = 0;
        while (total < max) {
            int n = in.read(buf, total, max - total);
            if (n == -1) {
                return (total == 0) ? -1 : total;
            }
            total += n;

            // If we got "some" bytes, allow more reads to fill the chunk,
            // but don't spin forever on slow streams: break once we got a decent amount.
            if (total >= 256 * 1024) { // 256KB threshold
                break;
            }
        }
        return total;
    }

    private Metadata fetchMetadataOrRoot(String normalizedPath) throws DbxException {
        if ("/".equals(normalizedPath)) {
            return null; // root is virtual; treat via path checks
        }
        return dbxClient.files().getMetadata(normalizedPath);
    }

    @Override
    public String getName() {
        if ("/".equals(path)) return "/";
        return (metadata != null) ? metadata.getName() : new java.io.File(path).getName();
    }

    @Override
    public String getParentName() {
        String parentPath = getParentPath();
        if (parentPath == null) return null;
        if ("/".equals(parentPath)) return "/";
        try {
            Metadata parent = dbxClient.files().getMetadata(parentPath);
            return parent.getName();
        } catch (DbxException e) {
            return null;
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getParentPath() {
        if ("/".equals(path)) return null;
        int idx = path.lastIndexOf('/');
        if (idx == 0) return "/";
        if (idx > 0) return path.substring(0, idx);
        return null;
    }

    @Override
    public boolean isDirectory() {
        return ("/".equals(path)) || (metadata instanceof FolderMetadata);
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        if ("/".equals(path)) return false;

        String parentPath = getParentPath();
        String newPath = (parentPath == null || "/".equals(parentPath))
                ? "/" + new_name
                : parentPath + "/" + new_name;

        try {
            RelocationResult result = dbxClient.files().moveV2(path, newPath);
            metadata = result.getMetadata();
            return true;
        } catch (DbxException e) {
            return false;
        }
    }

    @Override
    public boolean delete() {
        if ("/".equals(path)) return false;
        try {
            dbxClient.files().deleteV2(path);
            return true;
        } catch (DbxException e) {
            return false;
        }
    }

    @Override
    public InputStream getInputStream() {
        if (isDirectory() || "/".equals(path)) return null;
        try {
            String p = (metadata instanceof FileMetadata)
                    ? metadata.getPathLower()
                    : path;
            return dbxClient.files().download(p).getInputStream();
        } catch (DbxException e) {
            return null;
        }
    }

    /**
     * 🚫 Do not use OutputStream abstraction for Dropbox uploads.
     * Use putChildFromStream() instead (upload sessions).
     */
    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        throw new UnsupportedOperationException("DropBoxFileModel: use putChildFromStream()");
    }

    // -----------------------------
    // StreamUploadFileModel
    // -----------------------------
    @Override
    public boolean putChildFromStream(String childName,
                                      InputStream in,
                                      long contentLengthOrMinus1,
                                      long[] bytesRead) {
        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = 0;
        if (!isDirectory()) return false;

        final String destPath = "/".equals(path) ? ("/" + childName) : (path + "/" + childName);

        try (InputStream input = in) {

            byte[] buf = new byte[CHUNK_SIZE];

            // ---- 1) Read first chunk ----
            int firstRead = readFullyUpTo(input, buf, buf.length);
            if (firstRead <= 0) {
                // empty file
                return uploadEmptyFile(destPath);
            }

            UploadSessionStartResult start = dbxClient.files()
                    .uploadSessionStart()
                    .uploadAndFinish(new ByteArrayInputStream(buf, 0, firstRead));

            String sessionId = start.getSessionId();
            long uploaded = firstRead;

            if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = uploaded;

            // ---- 2) We keep ONE "pending" chunk in memory for finish() ----
            byte[] pending = new byte[CHUNK_SIZE];
            int pendingLen = readFullyUpTo(input, pending, pending.length);

            if (pendingLen <= 0) {
                CommitInfo commit = CommitInfo.newBuilder(destPath)
                        .withMode(WriteMode.OVERWRITE)
                        .build();

                UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);

                dbxClient.files()
                        .uploadSessionFinish(cursor, commit)
                        .uploadAndFinish(new ByteArrayInputStream(new byte[0]));

                try {
                    this.metadata = dbxClient.files().getMetadata(destPath);
                } catch (Exception ignored) {
                }
                return (contentLengthOrMinus1 <= 0) || (uploaded == contentLengthOrMinus1);
            }

            // ---- 3) Append full chunks, but always keep last chunk for finish() ----
            while (true) {
                byte[] next = new byte[CHUNK_SIZE];
                int nextLen = readFullyUpTo(input, next, next.length);

                if (nextLen <= 0) {
                    // pending is the final chunk => FINISH with it
                    CommitInfo commit = CommitInfo.newBuilder(destPath)
                            .withMode(WriteMode.OVERWRITE)
                            .build();

                    UploadSessionCursor finishCursor = new UploadSessionCursor(sessionId, uploaded);

                    dbxClient.files()
                            .uploadSessionFinish(finishCursor, commit)
                            .uploadAndFinish(new ByteArrayInputStream(pending, 0, pendingLen));

                    uploaded += pendingLen;
                    if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = uploaded;

                    try {
                        this.metadata = dbxClient.files().getMetadata(destPath);
                    } catch (Exception ignored) {
                    }

                    if (contentLengthOrMinus1 > 0 && uploaded != contentLengthOrMinus1) {
                        return false;
                    }
                    return true;
                }

                // append the pending chunk (middle chunk)
                UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);

                dbxClient.files()
                        .uploadSessionAppendV2(cursor)
                        .uploadAndFinish(new ByteArrayInputStream(pending, 0, pendingLen));

                uploaded += pendingLen;
                if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = uploaded;

                // shift pending <- next
                pending = next;
                pendingLen = nextLen;
            }

        } catch (UploadSessionFinishErrorException e) {
            return false;
        } catch (NetworkIOException e) {
            return false;
        } catch (DbxException | IOException e) {
            return false;
        }
    }

    private boolean uploadEmptyFile(String destPath) {
        try (InputStream empty = new ByteArrayInputStream(new byte[0])) {
            dbxClient.files()
                    .uploadBuilder(destPath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(empty);
            return true;
        } catch (DbxException | IOException e) {
            return false;
        }
    }

    @Override
    public FileModel[] list() {
        if (!isDirectory()) return new FileModel[0];

        try {
            String argPath = toDropboxArgPath(path);

            List<FileModel> fileModels = new ArrayList<>();

            ListFolderResult result = dbxClient.files().listFolder(argPath);

            while (true) {
                for (Metadata m : result.getEntries()) {
                    String childPath = m.getPathLower(); // canonical for API ops
                    fileModels.add(new DropBoxFileModel(accessToken, dbxClient, childPath, m));
                }

                if (!result.getHasMore()) {
                    break; // ✅ clean exit
                }

                // fetch next page
                result = dbxClient.files().listFolderContinue(result.getCursor());
            }

            return fileModels.toArray(new FileModel[0]);

        } catch (DbxException e) {
            return new FileModel[0];
        }
    }


    @Override
    public boolean createFile(String name) {
        if (!isDirectory()) return false;

        String filePath = "/".equals(path) ? ("/" + name) : (path + "/" + name);
        return uploadEmptyFile(filePath);
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        if (!isDirectory()) return false;

        String dirPath = "/".equals(path) ? ("/" + dir_name) : (path + "/" + dir_name);
        try {
            dbxClient.files().createFolderV2(dirPath);
            return true;
        } catch (CreateFolderErrorException e) {
            if (e.errorValue.isPath() && e.errorValue.getPathValue().isConflict()) {
                return true;
            }
            return false;
        } catch (DbxException e) {
            return false;
        }
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        if (!isDirectory()) return false;

        String[] dirs = extended_path.split("/");
        String currentPath = path;

        for (String dir : dirs) {
            if (dir.isEmpty()) continue;

            currentPath = "/".equals(currentPath) ? ("/" + dir) : (currentPath + "/" + dir);

            try {
                dbxClient.files().createFolderV2(currentPath);
            } catch (CreateFolderErrorException e) {
                if (e.errorValue.isPath() && e.errorValue.getPathValue().isConflict()) {
                    continue;
                }
                return false;
            } catch (DbxException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public long getLength() {
        if (isDirectory() || "/".equals(path)) return 0;
        return ((FileMetadata) metadata).getSize();
    }

    @Override
    public boolean exists() {
        if ("/".equals(path)) return true;
        try {
            dbxClient.files().getMetadata(path);
            return true;
        } catch (GetMetadataErrorException e) {
            if (e.errorValue.isPath() && e.errorValue.getPathValue().isNotFound()) return false;
            return false;
        } catch (DbxException e) {
            return false;
        }
    }

    @Override
    public long lastModified() {
        if (isDirectory() || "/".equals(path)) return 0;
        return ((FileMetadata) metadata).getServerModified().getTime();
    }

    @Override
    public boolean isHidden() {
        return getName().startsWith(".");
    }
}
