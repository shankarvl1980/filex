package svl.kadatha.filex.usb;

import me.jahnen.libaums.core.fs.UsbFile;

/**
 * Represents an exclusive write access to the usbFileRoot.
 * Using try-with-resources ensures the write lock is released automatically.
 */
public class WriteAccess implements AutoCloseable {
    private final MyStampedLock stampedLock;
    private final long stamp;
    private final UsbFile usbFile;

    public WriteAccess(MyStampedLock stampedLock, long stamp, UsbFile usbFile) {
        this.stampedLock = stampedLock;
        this.stamp = stamp;
        this.usbFile = usbFile;
    }

    public UsbFile getUsbFile() {
        return usbFile;
    }

    @Override
    public void close() {
        stampedLock.unlockWrite(stamp);
    }
}