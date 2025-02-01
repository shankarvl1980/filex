package svl.kadatha.filex.usb;

import me.jahnen.libaums.core.fs.UsbFile;

/**
 * Represents a read access to the usbFileRoot.
 * <p>
 * - If `valid` is true, the data was likely consistent at the time of reading.
 * - If `valid` is false, a writer may have changed data while we were reading.
 * <p>
 * Note: There's no real lock to release for an optimistic read,
 * so `close()` does nothing special here.
 */
public class ReadAccess implements AutoCloseable {
    private final UsbFile usbFile;
    private final boolean valid;

    public ReadAccess(UsbFile usbFile, boolean valid) {
        this.usbFile = usbFile;
        this.valid = valid;
    }

    public UsbFile getUsbFile() {
        return usbFile;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public void close() {
        // No lock to release (optimistic read).
    }
}


//public class ReadAccess implements AutoCloseable {
//    private final ReadWriteLock lock;
//    private final UsbFile usbFile;
//
//    ReadAccess(ReadWriteLock lock, UsbFile usbFile) {
//        this.lock = lock;
//        this.usbFile = usbFile;
//    }
//
//    public UsbFile getUsbFile() {
//        return usbFile;
//    }
//
//    @Override
//    public void close() {
//        // Called automatically by try-with-resources
//        lock.readLock().unlock();
//    }
//}
//
