//package svl.kadatha.filex.filemodel;
//
//import androidx.annotation.NonNull;
//
//import com.microsoft.graph.core.ClientException;
//import com.microsoft.graph.models.DriveItem;
//import com.microsoft.graph.models.Folder;
//import com.microsoft.graph.requests.DriveItemCollectionPage;
//import com.microsoft.graph.requests.GraphServiceClient;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//import okhttp3.HttpUrl;
//import timber.log.Timber;
//
///**
// * OneDrive FileModel + StreamUploadFileModel.
// *
// * Key rule: Do NOT use OutputStream abstraction for OneDrive large uploads.
// * Use putChildFromStream() (upload session + chunked PUT).
// */
//public class OneDriveFileModel implements FileModel, StreamUploadFileModel {
//
//    private static final String TAG = "OneDriveFileModel";
//
//    // Your cap
//    private static final long MAX_FILE_SIZE = 2L * 1024L * 1024L * 1024L; // 2GB
//
//    // Graph recommends chunk size multiple of 320 KiB.
//    // 10 MiB is a solid default on mobile.
//    private static final int CHUNK_SIZE = 10 * 1024 * 1024;
//
//    private static final MediaType OCTET = MediaType.parse("application/octet-stream");
//
//    private final GraphServiceClient<?> graphClient;
//    private final OkHttpClient okHttp;
//
//    /**
//     * Internal normalized path without leading "/", and root = "".
//     * Example: "" (root), "Folder/Sub/file.txt"
//     */
//    private final String path;
//    private DriveItem driveItem;
//
//    public OneDriveFileModel(GraphServiceClient<?> graphClient, String path) throws ClientException {
//        this.graphClient = graphClient;
//        this.okHttp = new OkHttpClient.Builder()
//                .retryOnConnectionFailure(true)
//                .build();
//
//        this.path = normalizePath(path);
//        this.driveItem = getDriveItem(this.path);
//    }
//
//    private static String normalizePath(String rawPath) {
//        if (rawPath == null || rawPath.trim().isEmpty() || "/".equals(rawPath.trim())) {
//            return ""; // root
//        }
//        rawPath = rawPath.trim();
//        if (rawPath.startsWith("/")) rawPath = rawPath.substring(1);
//        // avoid trailing slash for non-root
//        if (rawPath.endsWith("/")) rawPath = rawPath.substring(0, rawPath.length() - 1);
//        return rawPath;
//    }
//
//    private DriveItem getDriveItem(String relativePathNoLeadingSlash) throws ClientException {
//        // root item: itemWithPath("") is OK in most SDK versions; otherwise use root().buildRequest().get()
//        if (relativePathNoLeadingSlash == null || relativePathNoLeadingSlash.isEmpty()) {
//            return graphClient.me()
//                    .drive()
//                    .root()
//                    .buildRequest()
//                    .get();
//        }
//
//        return graphClient.me()
//                .drive()
//                .root()
//                .itemWithPath(relativePathNoLeadingSlash)
//                .buildRequest()
//                .get();
//    }
//
//    // Return app-level path with leading "/" (your existing convention)
//    @Override
//    public String getPath() {
//        return "/" + path;
//    }
//
//    @Override
//    public String getName() {
//        if (path.isEmpty()) return "/";
//        return driveItem != null ? driveItem.name : lastSegment(path);
//    }
//
//    private static String lastSegment(String p) {
//        int idx = p.lastIndexOf('/');
//        return (idx >= 0) ? p.substring(idx + 1) : p;
//    }
//
//    @Override
//    public String getParentPath() {
//        if (path.isEmpty()) return null; // root has no parent
//        int lastSlash = path.lastIndexOf('/');
//        if (lastSlash == -1) return ""; // parent is root (normalized)
//        return path.substring(0, lastSlash);
//    }
//
//    @Override
//    public String getParentName() {
//        String parent = getParentPath();
//        if (parent == null) return null;
//        if (parent.isEmpty()) return "/";
//        try {
//            DriveItem p = getDriveItem(parent);
//            return p != null ? p.name : null;
//        } catch (ClientException e) {
//            return null;
//        }
//    }
//
//    @Override
//    public boolean isDirectory() {
//        return driveItem != null && driveItem.folder != null;
//    }
//
//    @Override
//    public boolean rename(String new_name, boolean overwrite) {
//        if (path.isEmpty()) return false; // don't rename root
//
//        DriveItem updatedItem = new DriveItem();
//        updatedItem.name = new_name;
//
//        try {
//            driveItem = graphClient.me().drive().items(driveItem.id)
//                    .buildRequest()
//                    .patch(updatedItem);
//            return true;
//        } catch (ClientException e) {
//            return false;
//        }
//    }
//
//    @Override
//    public boolean delete() {
//        if (path.isEmpty()) return false; // don't delete root
//        try {
//            graphClient.me().drive().items(driveItem.id)
//                    .buildRequest()
//                    .delete();
//            return true;
//        } catch (ClientException e) {
//            return false;
//        }
//    }
//
//    @Override
//    public InputStream getInputStream() {
//        if (isDirectory()) return null;
//        try {
//            return graphClient.me().drive().items(driveItem.id)
//                    .content()
//                    .buildRequest()
//                    .get();
//        } catch (ClientException e) {
//            return null;
//        }
//    }
//
//    /**
//     * 🚫 Do not use OutputStream abstraction for OneDrive uploads.
//     * Use putChildFromStream() (upload session + chunks).
//     */
//    @Override
//    public OutputStream getChildOutputStream(String child_name, long source_length) {
//        throw new UnsupportedOperationException("OneDriveFileModel: use putChildFromStream()");
//    }
//
//    @Override
//    public FileModel[] list() {
//        if (!isDirectory()) return new FileModel[0];
//
//        try {
//            DriveItemCollectionPage children = graphClient.me()
//                    .drive()
//                    .items(driveItem.id)
//                    .children()
//                    .buildRequest()
//                    .get();
//
//            List<FileModel> out = new ArrayList<>();
//            for (DriveItem item : children.getCurrentPage()) {
//                String childPath = path.isEmpty() ? item.name : (path + "/" + item.name);
//                out.add(new OneDriveFileModel(graphClient, "/" + childPath));
//            }
//            return out.toArray(new FileModel[0]);
//        } catch (ClientException e) {
//            return new FileModel[0];
//        }
//    }
//
//    @Override
//    public boolean createFile(String name) {
//        if (!isDirectory()) return false;
//
//        // Create empty file: upload session with 0 is allowed, but simplest is small PUT of empty bytes.
//        String childPath = path.isEmpty() ? name : (path + "/" + name);
//        byte[] empty = new byte[0];
//
//        try {
//            graphClient.me().drive().root().itemWithPath(childPath)
//                    .content()
//                    .buildRequest()
//                    .put(empty);
//            return true;
//        } catch (ClientException e) {
//            return false;
//        }
//    }
//
//    @Override
//    public boolean makeDirIfNotExists(String dir_name) {
//        if (!isDirectory()) return false;
//
//        String dirPath = path.isEmpty() ? dir_name : (path + "/" + dir_name);
//
//        // Try create under current folder by ID (more reliable than itemWithPath(parent).children()).
//        DriveItem newFolder = new DriveItem();
//        newFolder.name = dir_name;
//        newFolder.folder = new Folder();
//
//        try {
//            graphClient.me().drive().items(driveItem.id)
//                    .children()
//                    .buildRequest()
//                    .post(newFolder);
//            return true;
//        } catch (ClientException e) {
//            // If it already exists, metadata fetch should succeed
//            try {
//                getDriveItem(dirPath);
//                return true;
//            } catch (ClientException ignored) {
//                return false;
//            }
//        }
//    }
//
//    @Override
//    public boolean makeDirsRecursively(String extended_path) {
//        if (!isDirectory()) return false;
//
//        String[] parts = extended_path.split("/");
//        String current = path; // normalized
//
//        for (String dir : parts) {
//            if (dir == null || dir.isEmpty()) continue;
//
//            String next = current.isEmpty() ? dir : (current + "/" + dir);
//
//            try {
//                // Create folder under "current"
//                DriveItem parentItem = current.isEmpty() ? getDriveItem("") : getDriveItem(current);
//
//                DriveItem folder = new DriveItem();
//                folder.name = dir;
//                folder.folder = new Folder();
//
//                try {
//                    graphClient.me().drive().items(parentItem.id)
//                            .children()
//                            .buildRequest()
//                            .post(folder);
//                } catch (ClientException ce) {
//                    // If exists, allow
//                    getDriveItem(next);
//                }
//
//            } catch (ClientException ex) {
//                return false;
//            }
//
//            current = next;
//        }
//
//        return true;
//    }
//
//    @Override
//    public long getLength() {
//        if (isDirectory()) return 0;
//        return (driveItem != null && driveItem.size != null) ? driveItem.size : 0;
//    }
//
//    @Override
//    public boolean exists() {
//        try {
//            getDriveItem(path);
//            return true;
//        } catch (ClientException e) {
//            return false;
//        }
//    }
//
//    @Override
//    public long lastModified() {
//        if (driveItem != null && driveItem.lastModifiedDateTime != null) {
//            long epochSecond = driveItem.lastModifiedDateTime.toEpochSecond();
//            int nano = driveItem.lastModifiedDateTime.getNano();
//            return epochSecond * 1000L + nano / 1_000_000L;
//        }
//        return 0;
//    }
//
//    @Override
//    public boolean isHidden() {
//        String n = getName();
//        return n != null && n.startsWith(".");
//    }
//
//    // ---------------------------------------------------------------------
//    // StreamUploadFileModel
//    // ---------------------------------------------------------------------
//
//    /**
//     * OneDrive large-file upload requires a known total size to set Content-Range.
//     * So contentLengthOrMinus1 MUST be > 0 for large uploads.
//     */
//    @Override
//    public boolean putChildFromStream(@NonNull String childName,
//                                      @NonNull InputStream in,
//                                      long contentLengthOrMinus1,
//                                      long[] bytesRead) {
//
//        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = 0;
//
//        if (!isDirectory()) return false;
//
//        if (contentLengthOrMinus1 <= 0) {
//            // You can’t do correct Content-Range without total size.
//            // Decide size earlier; do not guess.
//            Timber.tag(TAG).e("putChildFromStream requires known content length for OneDrive.");
//            return false;
//        }
//
//        if (contentLengthOrMinus1 > MAX_FILE_SIZE) {
//            Timber.tag(TAG).e("File too large: %d bytes (max %d).", contentLengthOrMinus1, MAX_FILE_SIZE);
//            return false;
//        }
//
//        final String childPath = path.isEmpty() ? childName : (path + "/" + childName);
//
//        try (InputStream input = in) {
//
//            // 1) Create upload session (Graph SDK call)
//            UploadSession session = createUploadSession(childPath);
//            if (session == null || session.uploadUrl == null || session.uploadUrl.trim().isEmpty()) {
//                Timber.tag(TAG).e("createUploadSession failed (no uploadUrl).");
//                return false;
//            }
//
//            // 2) Upload chunks to uploadUrl
//            long uploaded = 0;
//            byte[] buf = new byte[CHUNK_SIZE];
//
//            while (uploaded < contentLengthOrMinus1) {
//                int toRead = (int) Math.min((long) buf.length, contentLengthOrMinus1 - uploaded);
//                int n = readFullyUpTo(input, buf, toRead);
//                if (n <= 0) {
//                    Timber.tag(TAG).e("Unexpected EOF: uploaded=%d expected=%d", uploaded, contentLengthOrMinus1);
//                    return false;
//                }
//
//                long start = uploaded;
//                long end = uploaded + n - 1;
//
//                RequestBody body = RequestBody.create(copyOf(buf, n), OCTET);
//
//                Request req = new Request.Builder()
//                        .url(session.uploadUrl)
//                        .put(body)
//                        .header("Content-Length", String.valueOf(n))
//                        .header("Content-Range", "bytes " + start + "-" + end + "/" + contentLengthOrMinus1)
//                        .build();
//
//                try (Response res = okHttp.newCall(req).execute()) {
//                    int code = res.code();
//
//                    // 202 Accepted = continue uploading
//                    // 201 Created / 200 OK = completed
//                    if (code == 202 || code == 200 || code == 201) {
//                        uploaded += n;
//                        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = uploaded;
//                    } else {
//                        Timber.tag(TAG).e("Chunk upload failed: %d %s", code, res.message());
//                        return false;
//                    }
//                }
//            }
//
//            // 3) Refresh metadata best-effort
//            try { this.driveItem = getDriveItem(childPath); } catch (Exception ignored) {}
//
//            return uploaded == contentLengthOrMinus1;
//
//        } catch (IOException e) {
//            Timber.tag(TAG).e(e, "putChildFromStream failed");
//            return false;
//        }
//    }
//
//    private UploadSession createUploadSession(String childPathNoLeadingSlash) {
//        // We use a customRequest because SDK method availability differs by version.
//        // Endpoint:
//        // /me/drive/root:/path/to/file:/createUploadSession
//        // Body: { "item": { "@microsoft.graph.conflictBehavior": "replace", "name": "file.ext" } }
//
//        try {
//            String safePath = childPathNoLeadingSlash;
//            // Build encoded URL path safely
//            // We only need to ensure spaces etc are encoded in the URL.
//            // Graph SDK customRequest accepts relative URL.
//            String url = "/me/drive/root:/" + encodeGraphPath(safePath) + ":/createUploadSession";
//
//            Map<String, Object> item = new HashMap<>();
//            item.put("@microsoft.graph.conflictBehavior", "replace");
//            // name field is optional here because path already includes it, but it’s fine:
//            item.put("name", lastSegment(safePath));
//
//            Map<String, Object> body = new HashMap<>();
//            body.put("item", item);
//
//            return graphClient.customRequest(url, UploadSession.class)
//                    .buildRequest()
//                    .post(body);
//
//        } catch (Exception e) {
//            Timber.tag(TAG).e(e, "createUploadSession error");
//            return null;
//        }
//    }
//
//    /**
//     * Encode each path segment for Graph "root:/...:/..." addressing.
//     */
//    private static String encodeGraphPath(String p) {
//        if (p == null || p.isEmpty()) return "";
//        String[] parts = p.split("/");
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < parts.length; i++) {
//            if (parts[i].isEmpty()) continue;
//            if (sb.length() > 0) sb.append("/");
//            // Use HttpUrl to encode segment safely
//            sb.append(HttpUrl.parse("https://x/").newBuilder().addPathSegment(parts[i]).build().encodedPath().substring(1));
//        }
//        return sb.toString();
//    }
//
//    /**
//     * Read up to max bytes. Returns bytes read, or -1 if EOF immediately.
//     * This avoids tiny reads causing too many small uploads.
//     */
//    private static int readFullyUpTo(InputStream in, byte[] buf, int max) throws IOException {
//        int total = 0;
//        while (total < max) {
//            int n = in.read(buf, total, max - total);
//            if (n == -1) return (total == 0) ? -1 : total;
//            total += n;
//            // Don’t block forever trying to fill; one successful read is enough to progress.
//            break;
//        }
//        return total;
//    }
//
//    private static byte[] copyOf(byte[] src, int len) {
//        byte[] out = new byte[len];
//        System.arraycopy(src, 0, out, 0, len);
//        return out;
//    }
//
//    /**
//     * Minimal model for upload session response.
//     * Graph returns at least: uploadUrl, expirationDateTime.
//     */
//    public static class UploadSession {
//        public String uploadUrl;
//        public String expirationDateTime;
//        // Optional: nextExpectedRanges etc (not required for sequential upload)
//        public List<String> nextExpectedRanges;
//    }
//}
