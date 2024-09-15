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
        if(usbFile==null) return false;
        try {
            usbFile.setName(new_name);
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean delete() {
        if (usbFile == null) {
            return false;
        }

        Stack<UsbFile> stack = new Stack<>();
        stack.push(usbFile);
        boolean success = true;

        while (!stack.isEmpty() && success) {
            UsbFile current = stack.pop();

            if (current.isDirectory()) {
                UsbFile[] list = new UsbFile[0];
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
            if(targetUsbFile!=null && targetUsbFile.getLength()==0)deleteUsbFile(targetUsbFile);
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
        if(usbFile==null) return false;
        try {
            usbFile.createFile(name);
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        UsbFile childUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,Global.CONCATENATE_PARENT_CHILD_PATH(path,dir_name));
        if(childUsbFile==null)
        {
            return mkdirUsb(usbFile,dir_name);
        }
        else {
            return childUsbFile.isDirectory();
        }
    }


    @Override
    public boolean makeDirsRecursively(String extended_path) {
        return mkdirsUsb(path, extended_path);
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

    @Override
    public long lastModified() {
        if(usbFile!=null){
            return usbFile.lastModified();
        }
        else{
            return 0;
        }
    }

    @Override
    public boolean isHidden() {
        return path.startsWith(".");
    }

    private static boolean mkdirUsb(UsbFile parentUsbFile, String name)
    {
        if(parentUsbFile==null) return false;
        try {
            parentUsbFile.createDirectory(name);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean mkdirsUsb(String parent_file_path, @NonNull String path)
    {
        boolean success=true;
        UsbFile parentUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,parent_file_path);
        if(parentUsbFile==null)
        {
            return false;
        }
        String [] path_substring=path.split("/");
        int size=path_substring.length;
        for (int i=0; i<size;++i)
        {
            String path_string=path_substring[i];
            if(!path_string.equals(""))
            {
                UsbFile usbFile;
                if((usbFile=FileUtil.getUsbFile(parentUsbFile,path_string))==null)
                {
                    success=mkdirUsb(parentUsbFile,path_string);
                    parentUsbFile=FileUtil.getUsbFile(parentUsbFile,path_string);
                }
                else
                {
                    parentUsbFile=usbFile;
                }

                if(!success)
                {
                    return false;
                }
            }

        }
        return success;
    }

    private static boolean deleteUsbFile(UsbFile usbFile)
    {
        if(usbFile==null) return false;
        try {
            if(!usbFile.isDirectory() && usbFile.getLength()==0)
            {
                boolean madeNonZero=make_UsbFile_non_zero_length(usbFile.getAbsolutePath());
                if(madeNonZero)
                {
                    usbFile.delete();
                    return true;
                }
            }
            else
            {
                usbFile.delete();
                return true;
            }


        } catch (IOException e) {
            return false;
        }
        return false;
    }

    public static boolean make_UsbFile_non_zero_length(@NonNull String target_file_path)
    {
        String string="abcdefghijklmnopqrstuvwxyz";
        OutputStream outStream=null;
        try
        {
            UsbFile targetUsbFile=  FileUtil.getUsbFile(MainActivity.usbFileRoot,target_file_path);
            if (targetUsbFile != null)
            {
                outStream=UsbFileStreamFactory.createBufferedOutputStream(targetUsbFile,MainActivity.usbCurrentFs);
                outStream.write(string.getBytes(StandardCharsets.UTF_8));
            }
            else
            {
                return false;
            }
        }
        catch (Exception e)
        {
            return false;
        }
        finally
        {
            try
            {
                if(outStream!=null)outStream.close();
            }
            catch (Exception e)
            {
                // ignore exception
            }

        }
        return true;
    }

}
