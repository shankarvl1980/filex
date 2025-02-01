package svl.kadatha.filex.usb;

import me.jahnen.libaums.core.fs.UsbFile;
import timber.log.Timber;

public class UsbFileRootSingleton {

    private static volatile UsbFileRootSingleton sInstance;
    private static final Object LOCK = new Object();

    private final MyStampedLock stampedLock = new MyStampedLock();
    private UsbFile usbFileRoot;

    private UsbFileRootSingleton() {
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

    // -------------------------------------------------
    // 1) Write Methods (Exclusive)
    // -------------------------------------------------

    public void setUsbFileRoot(UsbFile root) {
        long stamp = stampedLock.writeLock();  // one writer at a time
        try {
            Timber.d("setUsbFileRoot: Setting usbFileRoot...");
            this.usbFileRoot = root;
        } finally {
            stampedLock.unlockWrite(stamp);
        }
    }

    public void clearUsbFileRoot() {
        long stamp = stampedLock.writeLock();
        try {
            Timber.d("clearUsbFileRoot: Clearing usbFileRoot...");
            usbFileRoot = null;
        } finally {
            stampedLock.unlockWrite(stamp);
        }
    }

    /**
     * Acquire an exclusive write lock, e.g. for rename, delete, etc.
     * Use try-with-resources with WriteAccess.
     */

    public WriteAccess acquireUsbFileRootForWrite() {
        long stamp = stampedLock.writeLock();
        if (usbFileRoot == null) {
            Timber.w("acquireUsbFileRootForWrite: usbFileRoot is null. Set it first?");
        }
        return new WriteAccess(stampedLock, stamp, usbFileRoot);
    }

    // -------------------------------------------------
    // 2) Read Methods (Optimistic)
    // -------------------------------------------------

    /**
     * Acquire an optimistic read, which does NOT block a writer.
     */

    public ReadAccess acquireUsbFileRootForRead() {
        long stamp = stampedLock.tryOptimisticRead();
        UsbFile localRef = usbFileRoot;
        boolean valid = stampedLock.validate(stamp);

        if (!valid) {
            // The data might have changed while reading
            Timber.d("acquireUsbFileRootForRead: Optimistic stamp invalid; partial data possible.");
            return new ReadAccess(localRef, false);
        }
        // If valid, localRef should be consistent
        return new ReadAccess(localRef, true);
    }

    // -------------------------------------------------
    // 3) Non-blocking check if usbFileRoot is set
    // -------------------------------------------------

    /**
     * Check if usbFileRoot is non-null, without blocking a writer.
     * We do an optimistic read. If the stamp is invalid,
     * we can either do a quick fallback read or just return whatever we have.
     */

    public boolean isUsbFileRootSet() {
        long stamp = stampedLock.tryOptimisticRead();
        UsbFile ref = usbFileRoot;
        if (stampedLock.validate(stamp)) {
            // Stamp is valid => no concurrent writer intervened
            return ref != null;
        } else {
            // If invalid, a writer got the lock, so ref might be stale.
            // But to keep it purely non-blocking, let's just read again
            // without acquiring a real read lock:
            return usbFileRoot != null;
        }
    }
}

/// **
// * A singleton class for handling a single UsbFile instance (usbFileRoot),
// * allowing concurrent read access but exclusive write access.
// *
// * - Call acquireUsbFileRootForRead() when you just need to read properties (size, modifiedTime, etc.).
// * - Call acquireUsbFileRootForWrite() for major operations (create, rename, delete, copy).
// */
// public class UsbFileRootSingleton {
//
//    private static volatile UsbFileRootSingleton sInstance;
//    private static final Object LOCK = new Object();
//
//    /**
//     * A read/write lock:
//     * - readLock() allows multiple concurrent readers,
//     * - writeLock() requires exclusive access (no readers, no writers).
//     */
//    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
//
//    private UsbFile usbFileRoot;
//
//    private UsbFileRootSingleton() {
//        // Private to prevent direct instantiation
//    }
//
//    public static UsbFileRootSingleton getInstance() {
//        if (sInstance == null) {
//            synchronized (LOCK) {
//                if (sInstance == null) {
//                    sInstance = new UsbFileRootSingleton();
//                }
//            }
//        }
//        return sInstance;
//    }
//
//    /**
//     * Set the underlying UsbFile root (requires a write lock).
//     */
//    public void setUsbFileRoot(UsbFile root) {
//        readWriteLock.writeLock().lock();
//        try {
//            Timber.d("Setting usbFileRoot in singleton...");
//            this.usbFileRoot = root;
//        } finally {
//            readWriteLock.writeLock().unlock();
//        }
//    }
//
//    /**
//     * Check if the usbFileRoot is currently set (non-null).
//     * This acquires the read lock for thread-safety.
//     */
//    public boolean isUsbFileRootSet() {
//        readWriteLock.readLock().lock();
//        try {
//            return usbFileRoot != null;
//        } finally {
//            readWriteLock.readLock().unlock();
//        }
//    }
//
//    public ReadAccess acquireUsbFileRootForRead() {
//        readWriteLock.readLock().lock();
//        if (usbFileRoot == null) {
//            Timber.w("acquireUsbFileRootForRead: usbFileRoot is null.");
//        }
//        // Return a new AutoCloseable that unlocks in close().
//        return new ReadAccess(readWriteLock, usbFileRoot);
//    }
//
//    public WriteAccess acquireUsbFileRootForWrite() {
//        readWriteLock.writeLock().lock();
//        if (usbFileRoot == null) {
//            Timber.w("acquireUsbFileRootForWrite: usbFileRoot is null.");
//        }
//        return new WriteAccess(readWriteLock, usbFileRoot);
//    }
//
//    /**
//     * Clear out the reference entirely (e.g., on USB detach).
//     * Since we're modifying the reference, we must use the write lock.
//     */
//    public void clearUsbFileRoot() {
//        readWriteLock.writeLock().lock();
//        try {
//            usbFileRoot = null;
//            Timber.d("clearUsbFileRoot: usbFileRoot is cleared from singleton.");
//        } finally {
//            readWriteLock.writeLock().unlock();
//        }
//    }
//}
