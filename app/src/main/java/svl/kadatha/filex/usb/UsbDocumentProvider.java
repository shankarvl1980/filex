package svl.kadatha.filex.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.util.LruCache;
import android.webkit.MimeTypeMap;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import me.jahnen.libaums.core.UsbMassStorageDevice;
import me.jahnen.libaums.core.fs.FileSystem;
import me.jahnen.libaums.core.fs.UsbFile;
import me.jahnen.libaums.core.fs.UsbFileInputStream;
import me.jahnen.libaums.core.fs.UsbFileOutputStream;
import me.jahnen.libaums.core.partition.Partition;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.ParcelFileDescriptorUtil;
import svl.kadatha.filex.R;
import svl.kadatha.filex.TinyDB;
import timber.log.Timber;


public class UsbDocumentProvider extends DocumentsProvider {

    public static final String DOCUMENTS_AUTHORITY = "svl.kadatha.filex.usbdocuments";
    public static final String USB_ATTACHED = "usbattached";
    /**
     * Action string to request the permission to communicate with an UsbDevice.
     */
    public static final String ACTION_USB_PERMISSION = "svl.kadatha.filex.USB_PERMISSION";
    public static final String USB_ATTACH_BROADCAST = "svl.kadatha.filex.USB";
    private static final String TAG = UsbDocumentProvider.class.getSimpleName();
    private static final String DIRECTORY_SEPARATOR = "/";
    private static final String ROOT_SEPARATOR = ":";
    /**
     * Default root projection: everything but Root.COLUMN_MIME_TYPES
     */
    private final static String[] DEFAULT_ROOT_PROJECTION = new String[]{
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES};
    /**
     * Default document projection: everything but Document.COLUMN_ICON and Document.COLUMN_SUMMARY
     */
    private final static String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED};
    public static ArrayList<UsbMassStorageDevice> USB_MASS_STORAGE_DEVICES;
    private static int CHECKED_TIMES;
    private final Map<String, UsbPartition> mRoots = new HashMap<>();
    private final LruCache<String, UsbFile> mFileCache = new LruCache<>(100);
    private LocalBroadcastManager localBroadcastManager;

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    private static String getMimeType(UsbFile file) {

        if (file.isDirectory()) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        } else {
            String extension = MimeTypeMap.getFileExtensionFromUrl(file.getName()).toLowerCase();
            if (!extension.isEmpty()) {
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                Timber.tag(TAG).d("mimeType: " + mimeType);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                return mimeType;
            }
        }
        return "application/octet-stream";
    }

    private static String getFileName(String mimeType, String displayName) {

        String extension = MimeTypeMap.getFileExtensionFromUrl(displayName).toLowerCase();
        if ((extension == null) ||
                !Objects.equals(mimeType, MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension))) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension != null) {
                displayName = displayName + "." + extension;
            }
        }
        return displayName;
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        assert context != null;

        Global.RECOGNISE_USB = new TinyDB(context).getBoolean("recognise_usb");
        USB_MASS_STORAGE_DEVICES = new ArrayList<>();
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        getDetail();

        BroadcastReceiver usbPermissionBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (CHECKED_TIMES < 2) {
                    onCreate();
                }
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    discoverDevice(device);
                }
            }
        };

        BroadcastReceiver usbAttachedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                discoverDevice(device);
            }
        };

        BroadcastReceiver usbDetachedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                CHECKED_TIMES = 0;
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                detachDevice(device);
            }
        };

        // ----------------------------------------------------
        // Registration for usbPermissionBroadcastReceiver
        // ----------------------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33+) - must specify exported or not
            context.registerReceiver(
                    usbPermissionBroadcastReceiver,
                    new IntentFilter(ACTION_USB_PERMISSION),
                    Context.RECEIVER_NOT_EXPORTED // or RECEIVER_EXPORTED if you truly need it exported
            );
        } else {
            // Older versions - two-argument method
            context.registerReceiver(usbPermissionBroadcastReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        }

        // ----------------------------------------------------
        // Registration for usbAttachedBroadcastReceiver
        // ----------------------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                    usbAttachedBroadcastReceiver,
                    new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED),
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            context.registerReceiver(
                    usbAttachedBroadcastReceiver,
                    new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            );
        }

        // ----------------------------------------------------
        // Registration for usbDetachedBroadcastReceiver
        // ----------------------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                    usbDetachedBroadcastReceiver,
                    new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED),
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            context.registerReceiver(
                    usbDetachedBroadcastReceiver,
                    new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
            );
        }

        CHECKED_TIMES++;

        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            discoverDevice(device);
        }

        return true;
    }


    private void getDetail() {

        Context context = getContext();
        assert context != null;
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            discoverDevice(device);
        }
    }

    private void discoverDevice(UsbDevice device) {
        if (!Global.RECOGNISE_USB) {
            return;
        }
        Timber.tag(TAG).d("discoverDevice() " + device.toString());
        Context context = getContext();
        assert context != null;
        if (!USB_MASS_STORAGE_DEVICES.isEmpty()) {
            return;
        }
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        for (UsbMassStorageDevice massStorageDevice : UsbMassStorageDevice.getMassStorageDevices(getContext())) {
            if (device.equals(massStorageDevice.getUsbDevice())) {
                if (usbManager.hasPermission(device)) {
                    addRoot(massStorageDevice);
                    USB_MASS_STORAGE_DEVICES.add(massStorageDevice);

                    Intent intent = new Intent();
                    intent.setAction(UsbDocumentProvider.USB_ATTACH_BROADCAST);
                    intent.putExtra(USB_ATTACHED, true);
                    localBroadcastManager.sendBroadcast(intent);

                } else {
                    int pending_intent_flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT;
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                            ACTION_USB_PERMISSION), pending_intent_flag);
                    usbManager.requestPermission(device, permissionIntent);
                }
            }
        }
    }

    private void detachDevice(UsbDevice usbDevice) {

        Timber.tag(TAG).d("detachDevice() " + usbDevice.toString());
        for (Map.Entry<String, UsbPartition> root : mRoots.entrySet()) {
            if (root.getValue().device.equals(usbDevice)) {
                Timber.tag(TAG).d("remove rootId " + root.getKey());
                mRoots.remove(root.getKey());
                mFileCache.evictAll();
                notifyRootsChanged();
                break;
            }
        }
        USB_MASS_STORAGE_DEVICES.clear();
        Intent intent = new Intent();
        intent.setAction(UsbDocumentProvider.USB_ATTACH_BROADCAST);
        intent.putExtra(USB_ATTACHED, false);
        localBroadcastManager.sendBroadcast(intent);
    }

    private void addRoot(UsbMassStorageDevice device) {
        Timber.tag(TAG).d("addRoot() " + device.toString());

        try {
            device.init();
            for (Partition partition : device.getPartitions()) {
                UsbPartition usbPartition = new UsbPartition();
                usbPartition.device = device.getUsbDevice();
                usbPartition.fileSystem = partition.getFileSystem();
                mRoots.put(Integer.toString(partition.hashCode()), usbPartition);

                Timber.tag(TAG).d("found root " + partition.hashCode());
            }
        } catch (IOException e) {
            Timber.tag(TAG).e("error setting up device");
        }

        notifyRootsChanged();
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        Timber.tag(TAG).d("queryRoots()");

        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));

        for (Map.Entry<String, UsbPartition> root : mRoots.entrySet()) {
            UsbPartition usbPartition = root.getValue();
            FileSystem fileSystem = usbPartition.fileSystem;
            UsbFile rootDirectory = fileSystem.getRootDirectory();
            String volumeLabel = fileSystem.getVolumeLabel();

            String title;
            UsbDevice usbDevice = usbPartition.device;
            title = usbDevice.getManufacturerName() + " " + usbDevice.getProductName();

            String documentId = getDocIdForFile(rootDirectory);

            Timber.tag(TAG).d("add root " + documentId);

            final MatrixCursor.RowBuilder row = result.newRow();
            // These columns are required
            row.add(DocumentsContract.Root.COLUMN_ROOT_ID, root.getKey());
            row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, documentId);
            row.add(DocumentsContract.Root.COLUMN_TITLE, title);
            row.add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_LOCAL_ONLY |
                    DocumentsContract.Root.FLAG_SUPPORTS_CREATE |
                    DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD);
            row.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.usb_icon);
            // These columns are optional
            row.add(DocumentsContract.Root.COLUMN_SUMMARY, volumeLabel);
            row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, fileSystem.getFreeSpace());
            // Root.COLUMN_MIME_TYPE is another optional column and useful if you have multiple roots with different
            // types of mime types (roots that don't match the requested mime type are automatically hidden)
        }

        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        Timber.tag(TAG).d("queryDocument() " + documentId);

        try {
            final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
            includeFile(result, getFileForDocId(documentId));
            return result;
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        Timber.tag(TAG).d("queryChildDocuments() " + parentDocumentId);

        try {
            final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
            UsbFile parent = getFileForDocId(parentDocumentId);
            for (UsbFile child : parent.listFiles()) {
                includeFile(result, child);
            }
            return result;
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        Timber.tag(TAG).d("openDocument() " + documentId);

        try {
            UsbFile file = getFileForDocId(documentId);
            //final int accessMode = ParcelFileDescriptor.parseMode(mode);
            final int accessMode;
            if ("w".equals(mode)) {
                accessMode = ParcelFileDescriptor.MODE_WRITE_ONLY;
            } else {
                accessMode = ParcelFileDescriptor.MODE_READ_ONLY;
            }
            if (accessMode == ParcelFileDescriptor.MODE_READ_ONLY) {
                Timber.tag(TAG).d("openDocument() piping to UsbFileInputStream");
                return ParcelFileDescriptorUtil.pipeFrom(new UsbFileInputStream(file));
            } else if (accessMode == ParcelFileDescriptor.MODE_WRITE_ONLY) {
                Timber.tag(TAG).d("openDocument() piping to UsbFileOutputStream");
                return ParcelFileDescriptorUtil.pipeTo(new UsbFileOutputStream(file));

            }

            Timber.tag(TAG).d("openDocument() return null");

            return null;

        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        return Global.IS_CHILD_FILE(documentId, parentDocumentId);
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName)
            throws FileNotFoundException {
        Timber.tag(TAG).d("createDocument() " + parentDocumentId);

        try {
            UsbFile parent = getFileForDocId(parentDocumentId);
            UsbFile child;
            if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                child = parent.createDirectory(displayName);
            } else {
                child = parent.createFile(getFileName(mimeType, displayName));
            }

            return getDocIdForFile(child);

        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public String renameDocument(String documentId, String displayName)
            throws FileNotFoundException {
        Timber.tag(TAG).d("renameDocument() " + documentId);

        try (UsbFile file = getFileForDocId(documentId)) {
            file.setName(getFileName(getMimeType(file), displayName));
            mFileCache.remove(documentId);
            return getDocIdForFile(file);

        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        Timber.tag(TAG).d("deleteDocument() " + documentId);

        try (UsbFile file = getFileForDocId(documentId)) {
            file.delete();
            mFileCache.remove(documentId);
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public String getDocumentType(String documentId) {
        Timber.tag(TAG).d("getDocumentType() " + documentId);

        try {
            return getMimeType(getFileForDocId(documentId));
        } catch (IOException e) {
            Timber.tag(TAG).e(e.getMessage());
        }

        return "application/octet-stream";
    }

    private void includeFile(final MatrixCursor result, final UsbFile file) throws FileNotFoundException {

        final MatrixCursor.RowBuilder row = result.newRow();

        // These columns are required
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, getDocIdForFile(file));
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.isRoot() ? "" : file.getName());
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, getMimeType(file));

        int flags = DocumentsContract.Document.FLAG_SUPPORTS_DELETE
                | DocumentsContract.Document.FLAG_SUPPORTS_WRITE
                | DocumentsContract.Document.FLAG_SUPPORTS_RENAME;

        if (file.isDirectory()) {
            flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
        }

        /*
        // We only show thumbnails for image files - expect a call to openDocumentThumbnail for each file that has
        // this flag set
        if (mimeType.startsWith("image/"))
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL;
            */
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags);
        // COLUMN_SIZE is required, but can be null
        row.add(DocumentsContract.Document.COLUMN_SIZE, file.isDirectory() ? 0 : file.getLength());
        // These columns are optional
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.isRoot() ? 0 : file.lastModified());
        // Document.COLUMN_ICON can be a resource id identifying a custom icon. The system provides default icons
        // based on mime type
        // Document.COLUMN_SUMMARY is optional additional information about the file
    }

    private void notifyRootsChanged() {
        getContext().getContentResolver().notifyChange(
                DocumentsContract.buildRootsUri(DOCUMENTS_AUTHORITY), null, false);
    }

    private String getDocIdForFile(UsbFile file) throws FileNotFoundException {

        if (file.isRoot()) {
            for (Map.Entry<String, UsbPartition> root : mRoots.entrySet()) {
                if (file.equals(root.getValue().fileSystem.getRootDirectory())) {
                    String documentId = root.getKey() + ROOT_SEPARATOR;
                    mFileCache.put(documentId, file);
                    return documentId;
                }
            }
            throw new FileNotFoundException("Missing root entry");
        }

        String documentId = getDocIdForFile(file.getParent()) + DIRECTORY_SEPARATOR + file.getName();
        mFileCache.put(documentId, file);
        return documentId;
    }

    private UsbFile getFileForDocId(String documentId) throws IOException {
        Timber.tag(TAG).d("getFileForDocId() " + documentId);
        Timber.tag(TAG).d("No cache entry for " + documentId);

        String[] path_segments = documentId.split(ROOT_SEPARATOR);
        if (path_segments.length == 1) {
            Timber.tag(TAG).d("path segments when length 1 " + path_segments[0]);
            try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
                return access.getUsbFile();
            }
        } else {
            String path = path_segments[1];
            try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
                UsbFile usbFileRoot = access.getUsbFile();
                return usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(path));
            }
        }
    }

    private static class UsbPartition {
        UsbDevice device;
        FileSystem fileSystem;
    }
}
