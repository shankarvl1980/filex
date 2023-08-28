package svl.kadatha.filex.filemodel;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import me.jahnen.libaums.core.fs.UsbFile;
import me.jahnen.libaums.core.fs.UsbFileStreamFactory;
import svl.kadatha.filex.FileUtil;
import svl.kadatha.filex.Global;
import svl.kadatha.filex.MainActivity;

public class UsbFileModel implements FileModel{
    private final UsbFile usbFile;
    private final String path;

    UsbFileModel(String path){
        usbFile= FileUtil.getUsbFile(MainActivity.usbFileRoot,path);
        this.path=path;
    }

    @Override
    public String getName() {
        if(usbFile!=null)
        {
            return new File(path).getName();
        }
        else {
            return null;
        }
    }

    @Override
    public String getParentName() {

        if(usbFile!=null)
        {
            File parentFile=new File(path).getParentFile();
            if(parentFile!=null)
            {
                return parentFile.getName();
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    @Override
    public String getPath() {

        if(usbFile!=null)
        {
            return path;
        }
        else{
            return null;
        }

    }

    @Override
    public String getParentPath() {

        if(usbFile!=null){
            return  new File(path).getParent();
        }
        else{
            return null;
        }
    }

    @Override
    public boolean isDirectory() {
        if(usbFile!=null)
        {
            return usbFile.isDirectory();
        }
        return false;
    }

    @Override
    public boolean rename(String new_name,boolean overwrite) {
        return FileUtil.renameUsbFile(usbFile,new_name);
    }

    @Override
    public boolean delete() {
        return FileUtil.deleteUsbDirectory(usbFile);
    }

    @Override
    public InputStream getInputStream() {
        if(usbFile!=null)
        {
            return UsbFileStreamFactory.createBufferedInputStream(usbFile,MainActivity.usbCurrentFs);
        }
        return null;
    }

    @Override
    public OutputStream getChildOutputStream(String child_name,long source_length) {
        UsbFile parentUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,path);
        if (parentUsbFile != null)
        {
            UsbFile targetUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot, Global.CONCATENATE_PARENT_CHILD_PATH(path,child_name));
            if(targetUsbFile!=null && targetUsbFile.getLength()==0)FileUtil.deleteUsbFile(targetUsbFile);
            try {
                targetUsbFile = parentUsbFile.createFile(child_name);
                if(source_length >0) targetUsbFile.setLength(source_length); // causes problem
                return UsbFileStreamFactory.createBufferedOutputStream(targetUsbFile,MainActivity.usbCurrentFs);

            } catch (Exception e) {
                return null;
            }


        }
        return null;
    }

    @Override
    public FileModel[] list() {

        if(usbFile!=null)
        {
            if(!usbFile.isDirectory()) return null;
            try {
                UsbFile [] usbFiles=usbFile.listFiles();
                int size= usbFiles != null ? usbFiles.length : 0;
                FileModel[] fileModels=new FileModel[size];
                for(int i=0;i<size;++i)
                {
                    fileModels[i]=new UsbFileModel(usbFiles[i].getAbsolutePath());
                }
                return fileModels;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean createFile(String name) {
        return FileUtil.createUsbFile(usbFile,name);
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        UsbFile childUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,Global.CONCATENATE_PARENT_CHILD_PATH(path,dir_name));
        if(childUsbFile==null)
        {
            return FileUtil.mkdirUsb(usbFile,dir_name);
        }
        else {
            return childUsbFile.isDirectory();
        }

    }

    @Override
    public boolean makeDirsRecursively(String extended_path) {
        return FileUtil.mkdirsUsb(path, extended_path);
    }

    @Override
    public long getLength() {
        if(usbFile!=null){
            return usbFile.getLength();
        }
        else{
            return 0;
        }
    }

    @Override
    public boolean exists() {
        if(usbFile!=null)
        {
            UsbFile dest_usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot, path);
            return dest_usbFile != null;
        }
        else {
            return false;
        }

    }
}
