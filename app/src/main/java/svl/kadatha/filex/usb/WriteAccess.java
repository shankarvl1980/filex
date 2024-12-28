package svl.kadatha.filex.usb;

import java.util.concurrent.locks.ReadWriteLock;

import me.jahnen.libaums.core.fs.UsbFile;

public class WriteAccess implements AutoCloseable {
    private final ReadWriteLock lock;
    private final UsbFile usbFile;

    WriteAccess(ReadWriteLock lock, UsbFile usbFile) {
        this.lock = lock;
        this.usbFile = usbFile;
    }

    public UsbFile getUsbFile() {
        return usbFile;
    }

    @Override
    public void close() {
        lock.writeLock().unlock();
    }
}

