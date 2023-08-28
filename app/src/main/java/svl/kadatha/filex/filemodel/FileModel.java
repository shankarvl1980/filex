package svl.kadatha.filex.filemodel;

import java.io.InputStream;
import java.io.OutputStream;

public interface FileModel {

    String getName();

    String getParentName();

    String getPath();

    String getParentPath();

    boolean isDirectory();

    boolean rename(String new_name,boolean overwrite);

    boolean delete();

    InputStream getInputStream();

    OutputStream getChildOutputStream(String child_name,long source_length);

    FileModel[] list();

    boolean createFile(String name);
    boolean makeDirIfNotExists (String dir_name);

    boolean makeDirsRecursively(String extended_path);

    long getLength();

    boolean exists();

}
