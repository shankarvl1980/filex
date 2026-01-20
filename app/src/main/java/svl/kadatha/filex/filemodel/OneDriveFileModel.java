//package svl.kadatha.filex.filemodel;
//
//import androidx.annotation.NonNull;
//
//import com.microsoft.graph.core.ClientException;
//import com.microsoft.graph.http.IHttpRequest;
//import com.microsoft.graph.http.IHttpRequestBuilder;
//import com.microsoft.graph.models.DriveItem;
//import com.microsoft.graph.models.Folder;
//import com.microsoft.graph.requests.DriveItemCollectionPage;
//import com.microsoft.graph.requests.DriveItemCollectionRequest;
//import com.microsoft.graph.requests.GraphServiceClient;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//
//import okhttp3.HttpUrl;
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//import svl.kadatha.filex.FileObjectType;
//import svl.kadatha.filex.FilePOJO;
//import svl.kadatha.filex.FilePOJOUtil;
//import timber.log.Timber;
//
/// **
// * OneDriveFileModel (path-only public ctor)
// *
// * IMPORTANT:
// * Call OneDriveFileModel.init(graphClient) once after login,
// * OR call init(accessToken) to build a minimal Graph client.
// */
//public class OneDriveFileModel implements FileModel, StreamUploadFileModel {
//
//    private static final String TAG = "OneDriveFileModel";
//
//    private static final int CHUNK_SIZE = 10 * 1024 * 1024; // must be multiple of 320KiB
//    private static final MediaType OCTET = MediaType.parse("application/octet-stream");
//
//    // ✅ Shared singletons (like your DRIVE_HTTP/DRIVE_GSON style)
//    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
//            .retryOnConnectionFailure(true)
//            .build();
//
//    private static volatile GraphServiceClient<?> GRAPH; // must be set via init()
//    private DriveItem driveItem;
//    // -----------------------------
//    // Instance state
//    // -----------------------------
//
//    private final String displayPath; // "/" or "/Folder/Sub"
//    private final String itemId;
//    private DriveItem driveItem;
//
//    // -----------------------------
//    // Init hooks (call once after login)
//    // -----------------------------
//
//    public static void init(@NonNull GraphServiceClient<?> graphClient) {
//        GRAPH = graphClient;
//    }
//
//    /**
//     * Fallback: build a minimal Graph client using access token.
//     * Use this ONLY if you don't already have a GraphServiceClient instance.
//     */
//    public static void init(@NonNull final String accessToken) {
//        GRAPH = GraphServiceClient.builder()
//                .authenticationProvider(request -> {
//                    // This is where the SDK injects the Authorization header.
//                    request.addHeader("Authorization", "Bearer " + accessToken);
//                })
//                .buildClient();
//    }
//
//    private static GraphServiceClient<?> graph() {
//        GraphServiceClient<?> g = GRAPH;
//        if (g == null) {
//            throw new IllegalStateException("OneDriveFileModel not initialized. Call OneDriveFileModel.init(...) after login.");
//        }
//        return g;
//    }
//
//    // -----------------------------
//    // Public ctor: PATH ONLY
//    // -----------------------------
//
//    public OneDriveFileModel(String path) throws ClientException {
//        this.displayPath = normalizeDisplayPath(path);
//
//        // ✅ Fast path: resolve from your FilePOJO cache
//        FilePOJO pojo = null;
//        try {
//            pojo = FilePOJOUtil.GET_FILE_POJO(this.displayPath, FileObjectType.ONE_DRIVE_TYPE);
//        } catch (Exception ignored) {}
//
//        if (pojo != null && pojo.getCloudId() != null && !pojo.getCloudId().isEmpty()) {
//            this.itemId = pojo.getCloudId();
//            this.driveItem = buildDriveItemFromPojo(pojo); // minimal, no network
//            return;
//        }
//
//        // 🐢 Slow fallback: resolve by path
//        DriveItem resolved = resolveByPath(this.displayPath);
//        if (resolved == null || resolved.id == null || resolved.id.isEmpty()) {
//            throw new ClientException("OneDrive item not found: " + this.displayPath, null);
//        }
//
//        this.itemId = resolved.id;
//        this.driveItem = resolved;
//    }
//
//    // Internal ctor used by list(): reuse DriveItem metadata (no extra GET)
//    private OneDriveFileModel(String displayPath, DriveItem item) {
//        this.displayPath = normalizeDisplayPath(displayPath);
//        this.driveItem = item;
//        this.itemId = (item != null) ? item.id : null;
//    }
//
//    DriveItem getDriveItemUnsafe() {
//        return driveItem;
//    }
//    // -----------------------------
//    // Normalization helpers
//    // -----------------------------
//
//    private static String normalizeDisplayPath(String p) {
//        if (p == null || p.trim().isEmpty() || "/".equals(p.trim())) return "/";
//        p = p.trim();
//        if (!p.startsWith("/")) p = "/" + p;
//        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
//        return p;
//    }
//
//    private static String toGraphRelativePath(String displayPath) {
//        if (displayPath == null || displayPath.trim().isEmpty() || "/".equals(displayPath.trim())) return "";
//        String p = displayPath.trim();
//        if (p.startsWith("/")) p = p.substring(1);
//        if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
//        return p;
//    }
//
//    private static String lastSegmentOfDisplayPath(String displayPath) {
//        if (displayPath == null || displayPath.isEmpty() || "/".equals(displayPath)) return "/";
//        int idx = displayPath.lastIndexOf('/');
//        return (idx >= 0 && idx < displayPath.length() - 1) ? displayPath.substring(idx + 1) : displayPath;
//    }
//
//    private static String parentDisplayPath(String displayPath) {
//        if (displayPath == null || "/".equals(displayPath)) return null;
//        int idx = displayPath.lastIndexOf('/');
//        if (idx <= 0) return "/";
//        return displayPath.substring(0, idx);
//    }
//
//    private static DriveItem buildDriveItemFromPojo(FilePOJO pojo) {
//        DriveItem di = new DriveItem();
//        di.id = pojo.getCloudId();
//        di.name = pojo.getName();
//        if (pojo.getIsDirectory()) {
//            di.folder = new Folder();
//        } else {
//            long sz = pojo.getSizeLong();
//            if (sz > 0) di.size = sz;
//        }
//        return di;
//    }
//
//    // -----------------------------
//    // Graph resolution
//    // -----------------------------
//
//    private static DriveItem resolveByPath(String displayPath) throws ClientException {
//        String rel = toGraphRelativePath(displayPath);
//        if (rel.isEmpty()) {
//            return graph().me().drive().root().buildRequest().get();
//        }
//        return graph().me().drive().root().itemWithPath(rel).buildRequest().get();
//    }
//
//    private static DriveItem fetchById(String id) throws ClientException {
//        return graph().me().drive().items(id).buildRequest().get();
//    }
//
//    private void ensureDriveItemLoaded() {
//        if (driveItem != null && driveItem.id != null) return;
//        try {
//            if (itemId != null && !itemId.isEmpty()) {
//                driveItem = fetchById(itemId);
//            }
//        } catch (Exception ignored) {}
//    }
//
//    // -----------------------------
//    // FileModel API
//    // -----------------------------
//
//    @Override
//    public String getPath() {
//        return displayPath;
//    }
//
//    @Override
//    public String getName() {
//        ensureDriveItemLoaded();
//        if ("/".equals(displayPath)) return "/";
//        return (driveItem != null && driveItem.name != null) ? driveItem.name : lastSegmentOfDisplayPath(displayPath);
//    }
//
//    @Override
//    public String getParentPath() {
//        return parentDisplayPath(displayPath);
//    }
//
//    @Override
//    public String getParentName() {
//        String parent = getParentPath();
//        if (parent == null) return null;
//        if ("/".equals(parent)) return "/";
//        try {
//            DriveItem p = resolveByPath(parent);
//            return (p != null) ? p.name : null;
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    @Override
//    public boolean isDirectory() {
//        ensureDriveItemLoaded();
//        return "/".equals(displayPath) || (driveItem != null && driveItem.folder != null);
//    }
//
//    @Override
//    public boolean rename(String new_name, boolean overwrite) {
//        if ("/".equals(displayPath)) return false;
//        ensureDriveItemLoaded();
//        if (driveItem == null || driveItem.id == null) return false;
//
//        DriveItem updated = new DriveItem();
//        updated.name = new_name;
//
//        try {
//            driveItem = graph().me().drive().items(driveItem.id).buildRequest().patch(updated);
//            return true;
//        } catch (ClientException e) {
//            return false;
//        }
//    }
//
//    @Override
//    public boolean delete() {
//        if ("/".equals(displayPath)) return false;
//        ensureDriveItemLoaded();
//        if (driveItem == null || driveItem.id == null) return false;
//
//        try {
//            graph().me().drive().items(driveItem.id).buildRequest().delete();
//            return true;
//        } catch (ClientException e) {
//            return false;
//        }
//    }
//
//    @Override
//    public InputStream getInputStream() {
//        if (isDirectory()) return null;
//        ensureDriveItemLoaded();
//        if (driveItem == null || driveItem.id == null) return null;
//
//        try {
//            return graph().me().drive().items(driveItem.id).content().buildRequest().get();
//        } catch (ClientException e) {
//            return null;
//        }
//    }
//
//    @Override
//    public OutputStream getChildOutputStream(String child_name, long source_length) {
//        throw new UnsupportedOperationException("OneDriveFileModel: use putChildFromStream()");
//    }
//
//    @Override
//    public FileModel[] list() {
//        if (!isDirectory()) return new FileModel[0];
//        ensureDriveItemLoaded();
//        if (driveItem == null || driveItem.id == null) return new FileModel[0];
//
//        try {
//            DriveItemCollectionRequest req = graph().me()
//                    .drive()
//                    .items(driveItem.id)
//                    .children()
//                    .buildRequest();
//
//            List<FileModel> out = new ArrayList<>();
//
//            DriveItemCollectionPage page = req.get();
//            while (page != null) {
//                List<DriveItem> items = page.getCurrentPage();
//                if (items != null) {
//                    for (DriveItem child : items) {
//                        if (child == null || child.name == null) continue;
//
//                        String childDisplayPath = "/".equals(displayPath)
//                                ? ("/" + child.name)
//                                : (displayPath + "/" + child.name);
//
//                        out.add(new OneDriveFileModel(childDisplayPath, child)); // ✅ reuse metadata, no network
//                    }
//                }
//
//                if (page.getNextPage() == null) break;
//                page = page.getNextPage().buildRequest().get();
//            }
//
//            return out.toArray(new FileModel[0]);
//
//        } catch (ClientException e) {
//            return new FileModel[0];
//        }
//    }
//
//    @Override
//    public boolean createFile(String name) {
//        if (!isDirectory()) return false;
//
//        String childDisplayPath = "/".equals(displayPath) ? ("/" + name) : (displayPath + "/" + name);
//        String childRel = toGraphRelativePath(childDisplayPath);
//
//        try {
//            graph().me().drive().root().itemWithPath(childRel)
//                    .content()
//                    .buildRequest()
//                    .put(new byte[0]);
//            return true;
//        } catch (ClientException e) {
//            return false;
//        }
//    }
//
//    @Override
//    public boolean makeDirIfNotExists(String dir_name) {
//        if (!isDirectory()) return false;
//        ensureDriveItemLoaded();
//        if (driveItem == null || driveItem.id == null) return false;
//
//        DriveItem folder = new DriveItem();
//        folder.name = dir_name;
//        folder.folder = new Folder();
//
//        try {
//            graph().me().drive().items(driveItem.id)
//                    .children()
//                    .buildRequest()
//                    .post(folder);
//            return true;
//        } catch (ClientException e) {
//            // If it already exists, treat as success if it resolves
//            try {
//                String child = "/".equals(displayPath) ? ("/" + dir_name) : (displayPath + "/" + dir_name);
//                DriveItem existing = resolveByPath(child);
//                return existing != null && existing.folder != null;
//            } catch (Exception ignored) {
//                return false;
//            }
//        }
//    }
//
//    @Override
//    public boolean makeDirsRecursively(String extended_path) {
//        if (!isDirectory()) return false;
//
//        String base = displayPath;
//        String[] parts = extended_path.split("/");
//
//        for (String seg : parts) {
//            if (seg == null || seg.isEmpty()) continue;
//
//            String next = "/".equals(base) ? ("/" + seg) : (base + "/" + seg);
//
//            try {
//                DriveItem item = resolveByPath(next);
//                if (item == null || item.folder == null) {
//                    OneDriveFileModel parentModel = new OneDriveFileModel(base);
//                    if (!parentModel.makeDirIfNotExists(seg)) return false;
//                }
//            } catch (Exception e) {
//                try {
//                    OneDriveFileModel parentModel = new OneDriveFileModel(base);
//                    if (!parentModel.makeDirIfNotExists(seg)) return false;
//                } catch (Exception ex) {
//                    return false;
//                }
//            }
//
//            base = next;
//        }
//
//        return true;
//    }
//
//    @Override
//    public long getLength() {
//        if (isDirectory()) return 0;
//        ensureDriveItemLoaded();
//        return (driveItem != null && driveItem.size != null) ? driveItem.size : 0;
//    }
//
//    @Override
//    public boolean exists() {
//        try {
//            if (itemId != null && !itemId.isEmpty()) return true;
//            DriveItem i = resolveByPath(displayPath);
//            return i != null && i.id != null;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    // -----------------------------
//    // lastModified (minSdk21 safe)
//    // -----------------------------
//
//    private static final SimpleDateFormat RFC3339_Z =
//            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
//    private static final SimpleDateFormat RFC3339_Z_MS =
//            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
//
//    private static String normalizeRfc3339(String s) {
//        if (s == null) return null;
//        // "+05:30" -> "+0530"
//        if (s.length() >= 6) {
//            char c = s.charAt(s.length() - 6);
//            if (c == '+' || c == '-') {
//                return s.substring(0, s.length() - 3) + s.substring(s.length() - 2);
//            }
//        }
//        return s;
//    }
//
//    @Override
//    public long lastModified() {
//        ensureDriveItemLoaded();
//        if (driveItem == null || driveItem.lastModifiedDateTime == null) return 0;
//
//        String s;
//        try {
//            s = String.valueOf(driveItem.lastModifiedDateTime);
//        } catch (Exception e) {
//            return 0;
//        }
//
//        if (s == null || s.isEmpty()) return 0;
//
//        if (s.endsWith("Z")) s = s.substring(0, s.length() - 1) + "+0000";
//        s = normalizeRfc3339(s);
//
//        try {
//            if (s.contains(".")) return RFC3339_Z_MS.parse(s).getTime();
//            return RFC3339_Z.parse(s).getTime();
//        } catch (Exception e) {
//            return 0;
//        }
//    }
//
//    @Override
//    public boolean isHidden() {
//        String n = getName();
//        return n != null && n.startsWith(".");
//    }
//
//    // -----------------------------
//    // StreamUploadFileModel
//    // -----------------------------
//
//    @Override
//    public boolean putChildFromStream(@NonNull String childName,
//                                      @NonNull InputStream in,
//                                      long contentLengthOrMinus1,
//                                      long[] bytesRead) {
//
//        if (bytesRead != null && bytesRead.length > 0) bytesRead[0] = 0;
//        if (!isDirectory()) return false;
//
//        if (contentLengthOrMinus1 <= 0) {
//            Timber.tag(TAG).e("putChildFromStream requires known content length for OneDrive.");
//            return false;
//        }
//
//        String childDisplayPath = "/".equals(displayPath)
//                ? ("/" + childName)
//                : (displayPath + "/" + childName);
//
//        final String childRel = toGraphRelativePath(childDisplayPath);
//
//        try (InputStream input = in) {
//
//            UploadSession session = createUploadSession(childRel);
//            if (session == null || session.uploadUrl == null || session.uploadUrl.trim().isEmpty()) {
//                Timber.tag(TAG).e("createUploadSession failed (no uploadUrl).");
//                return false;
//            }
//
//            long uploaded = 0;
//            byte[] buf = new byte[CHUNK_SIZE];
//
//            while (uploaded < contentLengthOrMinus1) {
//                int toRead = (int) Math.min((long) buf.length, contentLengthOrMinus1 - uploaded);
//                int n = readExactly(input, buf, toRead);
//                if (n <= 0) return false;
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
//                try (Response res = HTTP.newCall(req).execute()) {
//                    int code = res.code();
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
//            return uploaded == contentLengthOrMinus1;
//
//        } catch (IOException e) {
//            Timber.tag(TAG).e(e, "putChildFromStream failed");
//            return false;
//        }
//    }
//
//    private UploadSession createUploadSession(String childRelPathNoLeadingSlash) {
//        try {
//            String url = "/me/drive/root:/" + encodeGraphPath(childRelPathNoLeadingSlash) + ":/createUploadSession";
//
//            HashMap<String, Object> item = new HashMap<>();
//            item.put("@microsoft.graph.conflictBehavior", "replace");
//
//            HashMap<String, Object> body = new HashMap<>();
//            body.put("item", item);
//
//            return graph().customRequest(url, UploadSession.class)
//                    .buildRequest()
//                    .post(body);
//
//        } catch (Exception e) {
//            Timber.tag(TAG).e(e, "createUploadSession error");
//            return null;
//        }
//    }
//
//    private static String encodeGraphPath(String p) {
//        if (p == null || p.isEmpty()) return "";
//        String[] parts = p.split("/");
//        StringBuilder sb = new StringBuilder();
//        for (String seg : parts) {
//            if (seg == null || seg.isEmpty()) continue;
//            if (sb.length() > 0) sb.append("/");
//            sb.append(HttpUrl.parse("https://x/").newBuilder()
//                    .addPathSegment(seg)
//                    .build()
//                    .encodedPath()
//                    .substring(1));
//        }
//        return sb.toString();
//    }
//
//    private static int readExactly(InputStream in, byte[] buf, int len) throws IOException {
//        int off = 0;
//        while (off < len) {
//            int n = in.read(buf, off, len - off);
//            if (n == -1) return -1;
//            off += n;
//        }
//        return off;
//    }
//
//    private static byte[] copyOf(byte[] src, int len) {
//        byte[] out = new byte[len];
//        System.arraycopy(src, 0, out, 0, len);
//        return out;
//    }
//
//    public static class UploadSession {
//        public String uploadUrl;
//        public String expirationDateTime;
//        public List<String> nextExpectedRanges;
//    }
//}
