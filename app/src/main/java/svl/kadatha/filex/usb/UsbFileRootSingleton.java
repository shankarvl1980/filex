package svl.kadatha.filex.usb;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import me.jahnen.libaums.core.fs.UsbFile;
import timber.log.Timber;

/**
 * A singleton class for handling a single UsbFile instance (usbFileRoot),
 * allowing concurrent read access but exclusive write access.
 *
 * - Call acquireUsbFileRootForRead() when you just need to read properties (size, modifiedTime, etc.).
 * - Call acquireUsbFileRootForWrite() for major operations (create, rename, delete, copy).
 */
public class UsbFileRootSingleton {

    private static volatile UsbFileRootSingleton sInstance;
    private static final Object LOCK = new Object();

    /**
     * A read/write lock:
     * - readLock() allows multiple concurrent readers,
     * - writeLock() requires exclusive access (no readers, no writers).
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    private UsbFile usbFileRoot;

    private UsbFileRootSingleton() {
        // Private to prevent direct instantiation
    }

    public static UsbFileRootSingleton getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = new UsbFileRootSingleton();
                }
            }
        }
        return sInstance;
    }

    /**
     * Set the underlying UsbFile root (requires a write lock).
     */
    public void setUsbFileRoot(UsbFile root) {
        readWriteLock.writeLock().lock();
        try {
            Timber.d("Setting usbFileRoot in singleton...");
            this.usbFileRoot = root;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * Check if the usbFileRoot is currently set (non-null).
     * This acquires the read lock for thread-safety.
     */
    public boolean isUsbFileRootSet() {
        readWriteLock.readLock().lock();
        try {
            return usbFileRoot != null;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public ReadAccess acquireUsbFileRootForRead() {
        readWriteLock.readLock().lock();
        if (usbFileRoot == null) {
            Timber.w("acquireUsbFileRootForRead: usbFileRoot is null.");
        }
        // Return a new AutoCloseable that unlocks in close().
        return new ReadAccess(readWriteLock, usbFileRoot);
    }

    public WriteAccess acquireUsbFileRootForWrite() {
        readWriteLock.writeLock().lock();
        if (usbFileRoot == null) {
            Timber.w("acquireUsbFileRootForWrite: usbFileRoot is null.");
        }
        return new WriteAccess(readWriteLock, usbFileRoot);
    }

    /**
     * Clear out the reference entirely (e.g., on USB detach).
     * Since we're modifying the reference, we must use the write lock.
     */
    public void clearUsbFileRoot() {
        readWriteLock.writeLock().lock();
        try {
            usbFileRoot = null;
            Timber.d("clearUsbFileRoot: usbFileRoot is cleared from singleton.");
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}
