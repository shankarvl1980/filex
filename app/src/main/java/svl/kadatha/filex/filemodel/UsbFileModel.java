package svl.kadatha.filex.filemodel;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

import me.jahnen.libaums.core.fs.UsbFile;
import me.jahnen.libaums.core.fs.UsbFileStreamFactory;
import svl.kadatha.filex.FileUtil;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.MainActivity;
import svl.kadatha.filex.usb.ReadAccess;
import svl.kadatha.filex.usb.UsbFileRootSingleton;
import svl.kadatha.filex.usb.WriteAccess;

public class UsbFileModel implements FileModel {
    //private final UsbFile usbFile;
    private final String path;

    UsbFileModel(String path) {
        //usbFile = FileUtil.getUsbFile(MainActivity.usbFileRoot, path);
        this.path = path;
    }

    private static boolean mkdirUsb(UsbFile parentUsbFile, String name) {
        if (parentUsbFile == null) {
            return false;
        }
        try {
            parentUsbFile.createDirectory(name);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean mkdirsUsb(String parent_file_path, @NonNull String path) {
        try (WriteAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForWrite()) {
            UsbFile usbFileRoot = access.getUsbFile();
            UsbFile parentUsbFile = FileUtil.getUsbFile(usbFileRoot, parent_file_path);
            if (parentUsbFile == null) {
                return false;
            }

            String[] pathSegments = path.split("/");
            for (String segment : pathSegments) {
                if (!segment.isEmpty()) {
                    UsbFile usbFile = FileUtil.getUsbFile(parentUsbFile, segment);
                    if (usbFile == null) {
                        if (!mkdirUsb(parentUsbFile, segment)) {
                            return false;
                        }
                        usbFile = FileUtil.getUsbFile(parentUsbFile, segment);
                        if (usbFile == null) {
                            return false;
                        }
                    }
                    parentUsbFile = usbFile;
                }
            }
            return true;
        }

    }

    private static boolean deleteUsbFile(UsbFile usbFile) {
        if (usbFile == null) {
            return false;
        }
        try {
//            if (!usbFile.isDirectory() && usbFile.getLength() == 0) {
//                boolean madeNonZero = make_UsbFile_non_zero_length(usbFile.getAbsolutePath());
//                if (madeNonZero) {
//                    usbFile.delete();
//                    return true;
//                }
//            } else
            {
                usbFile.delete();
                return true;
            }


        } catch (IOException e) {
            return false;
        }
        //return false;
    }

    public static boolean make_UsbFile_non_zero_length(@NonNull String target_file_path) {
        String string = "abcdefghijklmnopqrstuvwxyz";

        // Acquire the write lock
        try (WriteAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForWrite()) {
            UsbFile usbFileRoot = access.getUsbFile();
            if (usbFileRoot == null) {
                // Could be that USB is detached or never set
                return false;
            }

            // Find the target UsbFile
            UsbFile targetUsbFile = FileUtil.getUsbFile(usbFileRoot, target_file_path);
            if (targetUsbFile == null) {
                return false;
            }

            // Open, write, and close the stream all inside the locked scope
            try (OutputStream outStream = UsbFileStreamFactory.createBufferedOutputStream(targetUsbFile, MainActivity.usbCurrentFs)) {
                outStream.write(string.getBytes(StandardCharsets.UTF_8));
            }

        } catch (Exception e) {
            return false;
        }

        // If we reach here, the write and close succeeded
        return true;
    }


    @Override
    public String getName() {
        return new File(path).getName();
    }

    @Override
    public String getParentName() {
        File parentFile = new File(path).getParentFile();
        if (parentFile != null) {
            return parentFile.getName();
        } else {
            return null;
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getParentPath() {
        return new File(path).getParent();
    }

    @Override
    public boolean isDirectory() {
        try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
            UsbFile usbFileRoot = access.getUsbFile();
            UsbFile usbFile = FileUtil.getUsbFile(usbFileRoot, path);
            if (usbFile != null) {
                return usbFile.isDirectory();
            }
            return false;
        }
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        try (WriteAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForWrite()) {
            UsbFile usbFileRoot = access.getUsbFile();
            UsbFile usbFile = FileUtil.getUsbFile(usbFileRoot, path);
            if (usbFile == null) {
                return false;
            }
            try {
                usbFile.setName(new_name);
                return true;

            } catch (IOException e) {
                return false;
            }
        }
    }

    @Override
    public boolean delete() {
        try (WriteAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForWrite()) {
            UsbFile usbFileRoot = access.getUsbFile();
            UsbFile usbFile = FileUtil.getUsbFile(usbFileRoot, path);
            if (usbFile == null) {
                return false;
            }

            Stack<UsbFile> stack = new Stack<>();
            stack.push(usbFile);
            boolean success = true;

            while (!stack.isEmpty() && success) {
                UsbFile current = stack.pop();

                if (current.isDirectory()) {
                    UsbFile[] list;
                    try {
                        list = current.listFiles();
                    } catch (IOException e) {
                        System.err.println("Error listing files: " + e.getMessage());
                        success = false;
                        continue;
                    }

                    if (list != null && list.length > 0) {
                        // Push the current directory back onto the stack
                        stack.push(current);
                        // Push all children onto the stack
                        for (UsbFile child : list) {
                            stack.push(child);
                        }
                    } else {
                        // Empty directory, try to delete it
                        success = deleteUsbFile(current);
                        if (!success) {
                            System.err.println("Failed to delete directory: " + current.getName());
                        }
                    }
                } else {
                    // It's a file, try to delete it
                    success = deleteUsbFile(current);
                    if (!success) {
                        System.err.println("Failed to delete file: " + current.getName());
                    }
                }
            }

            // If the original folder still exists (it was not empty initially),
            // we need to delete it now
            if (success && usbFile.isDirectory()) {
                success = deleteUsbFile(usbFile);
                if (!success) {
                    System.err.println("Failed to delete root folder: " + usbFile.getName());
                }
            }
            return success;
        }
    }

    @Override
    public InputStream getInputStream() {
        ReadAccess readAccess = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead();
        UsbFile usbFileRoot = readAccess.getUsbFile();
        if (usbFileRoot == null) {
            // If null, release lock immediately
            readAccess.close();
            return null;
        }

        UsbFile targetUsbFile = FileUtil.getUsbFile(usbFileRoot, path);
        if (targetUsbFile == null) {
            readAccess.close();
            return null;
        }

        InputStream rawStream =
                UsbFileStreamFactory.createBufferedInputStream(targetUsbFile, MainActivity.usbCurrentFs);

        // Wrap in a locked stream that calls readAccess.close() in its close()
        return new LockedUsbInputStream(rawStream, readAccess);

    }


    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        WriteAccess writeAccess = UsbFileRootSingleton.getInstance().acquireUsbFileRootForWrite();
        UsbFile usbFileRoot = writeAccess.getUsbFile();
        if (usbFileRoot == null) {
            writeAccess.close();
            return null;
        }

        UsbFile parentUsbFile = FileUtil.getUsbFile(usbFileRoot, path);
        if (parentUsbFile == null) {
            writeAccess.close();
            return null;
        }

        try {
            UsbFile childUsbFile = parentUsbFile.createFile(child_name);
            if (source_length > 0) {
                childUsbFile.setLength(source_length);
            }
            OutputStream rawStream =
                    UsbFileStreamFactory.createBufferedOutputStream(childUsbFile, MainActivity.usbCurrentFs);

            return new LockedUsbOutputStream(rawStream, writeAccess);

        } catch (IOException e) {
            writeAccess.close();
            return null;
        }
    }


    @Override
    public FileModel[] list() {
        try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
            UsbFile usbFileRoot = access.getUsbFile();
            UsbFile usbFile = FileUtil.getUsbFile(usbFileRoot, path);
            if (usbFile != null) {
                if (!usbFile.isDirectory()) {
                    return null;
                }
                try {
                    UsbFile[] usbFiles = usbFile.listFiles();
                    int size = usbFiles != null ? usbFiles.length : 0;
                    FileModel[] fileModels = new FileModel[size];
                    for (int i = 0; i < size; ++i) {
                        fileModels[i] = new UsbFileModel(usbFiles[i].getAbsolutePath());
                    }
                    return fileModels;
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }
    }

    @Override
    public boolean createFile(String name) {
        try (WriteAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForWrite()) {
            UsbFile usbFileRoot = access.getUsbFile();
            UsbFile usbFile = FileUtil.getUsbFile(usbFileRoot, path);
            if (usbFile == null) {
                return false;
            }
            try {
                usbFile.createFile(name);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        try (WriteAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForWrite()) {
            UsbFile usbFileRoot = access.getUsbFile();
            UsbFile usbFile = FileUtil.getUsbFile(usbFileRoot, path);
            UsbFile childUsbFile = FileUtil.getUsbFile(usbFileRoot, Global.CONCATENATE_PARENT_CHILD_PATH(path, dir_name));
            if (childUsbFile == null) {
                return mkdirUsb(usbFile, dir_name);
            } else {
                return childUsbFile.isDirectory();
            }
        }
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        return mkdirsUsb(path, extended_path);
    }

    @Override
    public long getLength() {
        try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
            UsbFile usbFileRoot = access.getUsbFile();
            UsbFile usbFile = FileUtil.getUsbFile(usbFileRoot, path);
            if (usbFile != null) {
                return usbFile.getLength();
            } else {
                return 0;
            }
        }
    }

    @Override
    public boolean exists() {
        try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
            UsbFile usbFileRoot = access.getUsbFile();
            UsbFile usbFile = FileUtil.getUsbFile(usbFileRoot, path);
            if (usbFile != null) {
                UsbFile dest_usbFile = FileUtil.getUsbFile(usbFileRoot, path);
                return dest_usbFile != null;
            } else {
                return false;
            }
        }
    }

    @Override
    public long lastModified() {
        try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
            UsbFile usbFileRoot = access.getUsbFile();
            UsbFile usbFile = FileUtil.getUsbFile(usbFileRoot, path);
            if (usbFile != null) {
                return usbFile.lastModified();
            } else {
                return 0;
            }
        }
    }

    @Override
    public boolean isHidden() {
        return path.startsWith(".");
    }

    public class LockedUsbInputStream extends java.io.FilterInputStream {
        private final ReadAccess readAccess;

        public LockedUsbInputStream(InputStream in, ReadAccess readAccess) {
            super(in);
            this.readAccess = readAccess;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();  // closes the actual USB input stream
            } finally {
                // release the read lock
                readAccess.close();
            }
        }
    }

    public class LockedUsbOutputStream extends java.io.FilterOutputStream {
        private final WriteAccess writeAccess;

        public LockedUsbOutputStream(OutputStream out, WriteAccess writeAccess) {
            super(out);
            this.writeAccess = writeAccess;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close(); // flush and close the underlying USB output stream
            } finally {
                // release the write lock
                writeAccess.close();
            }
        }
    }

}
