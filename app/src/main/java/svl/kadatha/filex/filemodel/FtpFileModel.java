package svl.kadatha.filex.filemodel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import svl.kadatha.filex.FileUtil;
import svl.kadatha.filex.FtpClientRepository;
import svl.kadatha.filex.Global;

public class FtpFileModel implements FileModel {

    private final String path;

    FtpFileModel(String path)
    {
        this.path=path;
    }

    @Override
    public String getName() {
        return new File(path).getName();
    }

    @Override
    public String getParentName() {
        File parentFile=new File(path).getParentFile();
        if(parentFile!=null)
        {
            return parentFile.getName();
        }
        else {
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
        return FileUtil.isFtpPathDirectory(path);
    }

    @Override
    public boolean rename(String new_name,boolean overwrite) {
        String new_file_path=Global.CONCATENATE_PARENT_CHILD_PATH(getParentPath(),new_name);
        if(Global.CHECK_FTP_SERVER_CONNECTED())
        {
            try {
                return FtpClientRepository.getInstance().ftpClientMain.rename(path,new_file_path);
            } catch (IOException e) {
                return false;
            }
        }
        else {
            return false;
        }
    }

    @Override
    public boolean delete() {
        return FileUtil.deleteFtpDirectory(path);
    }

    @Override
    public InputStream getInputStream() {
        if(Global.CHECK_FTP_SERVER_CONNECTED())
        {
            try {
                return FtpClientRepository.getInstance().ftpClientMain.retrieveFileStream(path);
            } catch (Exception e) {

                return null;
            }
        }
        else {
            return null;
        }
    }

    @Override
    public OutputStream getChildOutputStream(String child_name,long source_length) {
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(path,child_name);
        if(Global.CHECK_FTP_SERVER_CONNECTED())
        {
            try {
                return FtpClientRepository.getInstance().ftpClientMain.storeFileStream(file_path);
            } catch (Exception e) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    @Override
    public FileModel[] list() {
        String[] inner_source_list;
        if(Global.CHECK_FTP_SERVER_CONNECTED())
        {
            try {
                inner_source_list = FtpClientRepository.getInstance().ftpClientMain.listNames(path);
                int size=inner_source_list.length;
                FileModel[] fileModels=new FileModel[size];
                for (int i=0;i<size;++i)
                {
                    String full_path=Global.CONCATENATE_PARENT_CHILD_PATH(path,inner_source_list[i]);
                    fileModels[i]=new FtpFileModel(full_path);
                }
                return fileModels;

            } catch (IOException e) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    @Override
    public boolean createFile(String name) {
        if(Global.CHECK_FTP_SERVER_CONNECTED())
        {
            InputStream bin = new ByteArrayInputStream(new byte[0]);
            try {
                return FtpClientRepository.getInstance().ftpClientMain.storeFile(Global.CONCATENATE_PARENT_CHILD_PATH(path,name), bin);
            } catch (IOException e) {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        return FileUtil.mkdirFtp(Global.CONCATENATE_PARENT_CHILD_PATH(path,dir_name));
    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        return FileUtil.mkdirsFTP(path, extended_path);
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public boolean exists() {
        return FileUtil.isFtpFileExists(path);
    }
}
