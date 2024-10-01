package svl.kadatha.filex.filemodel;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import svl.kadatha.filex.RootUtils;

public class RootFileModel implements FileModel {

    private final String path;
    private final String name;

    public RootFileModel(String path) {
        this.path = path;
        this.name = extractName(path);
    }

    private String extractName(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        } else {
            return path;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getParentName() {
        String parentPath = getParentPath();
        if (parentPath == null) return null;
        int lastSlash = parentPath.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < parentPath.length() - 1) {
            return parentPath.substring(lastSlash + 1);
        } else {
            return parentPath;
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getParentPath() {
        if (path == null) return null;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash > 0) {
            return path.substring(0, lastSlash);
        } else {
            return "/";
        }
    }

    @Override
    public boolean isDirectory() {
        String command = "[ -d '" + path + "' ] && echo 'true' || echo 'false'";
        String output = RootUtils.executeCommand(command);
        return output != null && output.trim().equals("true");
    }

    @Override
    public boolean rename(String new_name, boolean overwrite) {
        String parentPath = getParentPath();
        String newPath = parentPath + "/" + new_name;
        String command;
        if (overwrite) {
            command = "mv -f '" + path + "' '" + newPath + "'";
        } else {
            command = "[ ! -e '" + newPath + "' ] && mv '" + path + "' '" + newPath + "'";
        }
        return RootUtils.executeCommandBoolean(command);
    }

    @Override
    public boolean delete() {
        if (isSafeToDelete(path)) {
            String command = "rm -rf '" + path + "'";
            return RootUtils.executeCommandBoolean(command);
        } else {
            // Log a warning and avoid deletion
            System.err.println("Unsafe delete operation prevented for path: " + path);
            return false;
        }
    }

    private boolean isSafeToDelete(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        // Normalize the path to remove redundant slashes
        String normalizedPath = path.replaceAll("/+", "/");
        // Disallow root directory deletion
        if (normalizedPath.equals("/")) {
            return false;
        }
        // Disallow deletion of "." or ".."
        String name = getName();
        if (".".equals(name) || "..".equals(name)) {
            return false;
        }
        // Additional checks can be added here if necessary
        return true;
    }


    @Override
    public InputStream getInputStream() {
        try {
            return new RootFileInputStream(path);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public OutputStream getChildOutputStream(String child_name, long source_length) {
        String childPath = path + "/" + child_name;
        try {
            return new RootFileOutputStream(childPath);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public FileModel[] list() {
        if (!isDirectory()) {
            return null;
        }
        String command = "ls -a '" + path + "'";
        String output = RootUtils.executeCommand(command);
        if (output == null) return null;
        String[] names = output.split("\n");
        ArrayList<FileModel> fileModels = new ArrayList<>();
        for (String fileName : names) {
            if (fileName.equals(".") || fileName.equals("..")) {
                continue;
            }
            String filePath = path + "/" + fileName;
            fileModels.add(new RootFileModel(filePath));
        }
        return fileModels.toArray(new FileModel[0]);
    }

    @Override
    public boolean createFile(String name) {
        String filePath = path + "/" + name;
        String command = "touch '" + filePath + "'";
        return RootUtils.executeCommandBoolean(command);
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        String dirPath = path + "/" + dir_name;
        String command = "[ ! -d '" + dirPath + "' ] && mkdir '" + dirPath + "'";
        return RootUtils.executeCommandBoolean(command);
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        String dirPath = path + "/" + extended_path;
        String command = "mkdir -p '" + dirPath + "'";
        return RootUtils.executeCommandBoolean(command);
    }

    @Override
    public long getLength() {
        String command = "stat -c '%s' '" + path + "'";
        String output = RootUtils.executeCommand(command);
        if (output == null) return 0;
        try {
            return Long.parseLong(output.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean exists() {
        String command = "[ -e '" + path + "' ] && echo 'true' || echo 'false'";
        String output = RootUtils.executeCommand(command);
        return output != null && output.trim().equals("true");
    }

    @Override
    public long lastModified() {
        String command = "stat -c '%Y' '" + path + "'";
        String output = RootUtils.executeCommand(command);
        if (output == null) return 0;
        try {
            return Long.parseLong(output.trim()) * 1000L; // Convert seconds to milliseconds
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean isHidden() {
        return getName().startsWith(".");
    }


    public static class RootFileInputStream extends InputStream {
        private final Process process;
        private final InputStream processInputStream;

        public RootFileInputStream(String path) throws IOException {
            // Start the 'su' process and execute 'cat' to read the file
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", "cat '" + path + "'"});
            processInputStream = process.getInputStream();
        }

        @Override
        public int read() throws IOException {
            return processInputStream.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return processInputStream.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return processInputStream.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            processInputStream.close();
            process.destroy();
        }
    }


    public static class RootFileOutputStream extends OutputStream {
        private final Process process;
        private final OutputStream processOutputStream;

        public RootFileOutputStream(String path) throws IOException {
            // Start the 'su' process and execute 'sh -c "cat > 'path'"' to write to the file
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", "sh -c 'cat > \"" + path + "\"'"});
            processOutputStream = process.getOutputStream();
        }

        @Override
        public void write(int b) throws IOException {
            processOutputStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            processOutputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            processOutputStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            processOutputStream.flush();
        }

        @Override
        public void close() throws IOException {
            processOutputStream.flush();
            processOutputStream.close();
            process.destroy();
        }
    }
}
