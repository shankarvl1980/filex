package svl.kadatha.filex.filemodel;

import android.net.Uri;

import java.util.List;

import svl.kadatha.filex.FileObjectType;
import svl.kadatha.filex.FileUtil;

public class FileModelFactory {

    public static FileModel getFileModel(String path, FileObjectType fileObjectType, Uri uri,String uri_path)
    {
        FileModel fileModel = null;
        switch (fileObjectType){
            case ROOT_TYPE:
                break;
            case FILE_TYPE:
            case SEARCH_LIBRARY_TYPE:
                boolean writeable= FileUtil.isFromInternal(fileObjectType,path);
                fileModel=new JavaFileModel(path,writeable,uri,uri_path);
                break;
            case USB_TYPE:
                fileModel=new UsbFileModel(path);
                break;
            case FTP_TYPE:
                fileModel=new FtpFileModel(path);
                break;
            case SFTP_TYPE:
                fileModel=new SftpFileModel(path);
        }
        return fileModel;
    }

    public static FileModel[] getFileModelArray(List<String> file_path_list, FileObjectType fileObjectType, Uri uri,String uri_path)
    {
        int size=file_path_list.size();
        FileModel[] fileModels = new FileModel[size];
        for(int i=0;i<size;++i){
            String file_path=file_path_list.get(i);
            fileModels[i]=getFileModel(file_path,fileObjectType,uri,uri_path);
        }
        return fileModels;
    }
}
