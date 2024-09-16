package svl.kadatha.filex.filemodel;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import svl.kadatha.filex.App;
import svl.kadatha.filex.FileUtil;
import svl.kadatha.filex.Global;

@SuppressWarnings("IOStreamConstructor")
public class JavaFileModel implements FileModel {

    private final File file;
    private final String path;
    private final boolean writeable;
    private final Uri uri;
    private final String uri_path;

    JavaFileModel (@NonNull String path, boolean writeable,Uri uri, String uri_path)
    {
        file=new File(path);
        this.path=path;
        this.writeable=writeable;
        this.uri=uri;
        this.uri_path=uri_path;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getParentName() {
        File parentFile=file.getParentFile();
        if(parentFile!=null)
        {
            return parentFile.getName();
        }
        else{
            return null;
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getParentPath() {
        return file.getParent();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean rename(String new_name,boolean overwrite) {

        if(writeable)
        {
            File targetFile=new File(getParentPath(),new_name);
            if (file.renameTo(targetFile)) return true;

            if (targetFile.exists()) return false;

            return false;
        }
        else {
            if(overwrite)
            {
                String new_file_path=Global.CONCATENATE_PARENT_CHILD_PATH(getParentPath(), new_name);
                boolean isDir=new File(new_file_path).isDirectory();
                if(!isDir && !file.isDirectory())
                {
                    if(FileUtil.deleteSAFDirectory(App.getAppContext(),new_file_path,uri,uri_path))
                    {
                        return renameSAFFile(App.getAppContext(),path,new_name,uri,uri_path);
                    }
                }

            }
            else
            {
                return renameSAFFile(App.getAppContext(),path,new_name,uri,uri_path);
            }
        }
        return false;
    }


    @Override
    public boolean delete() {
       if(writeable)
       {
           return FileUtil.deleteNativeDirectory(file);
       }
       else {
           return FileUtil.deleteSAFDirectory(App.getAppContext(),path,uri,uri_path);
       }

    }

    @Override
    public InputStream getInputStream() {
        try {
            return new FileInputStream(file);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public OutputStream getChildOutputStream(String child_name,long source_length) {
        try {
            if(writeable)
            {
                return new FileOutputStream(new File(path,child_name));
            }
            else {
                Uri mUri = FileUtil.createDocumentUri(App.getAppContext(),path,child_name,false,uri,uri_path);
                if (mUri != null)
                {
                    try {
                        return App.getAppContext().getContentResolver().openOutputStream(mUri);
                    } catch (Exception e) {
                        return null;
                    }

                }
                else
                {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public FileModel[] list() {
        if(!file.isDirectory())
        {
            return null;
        }

        File [] files=file.listFiles();
        int size= files != null ? files.length : 0;
        FileModel[] fileModels = new FileModel[size];
        for(int i=0;i<size;++i)
        {
            fileModels[i]=new JavaFileModel(files[i].getAbsolutePath(),writeable,uri,uri_path);
        }
        return fileModels;
    }

    @Override
    public boolean createFile(String name) {
        if(writeable)
        {
            File new_file=new File(path,name);
            if (new_file.exists())
            {
                return false;
            }

            try
            {
                if (new_file.createNewFile())
                {
                    return true;
                }
            }
            catch(IOException e)
            {
                return false;
            }
            return false;
        }
        else
        {
            return createSAFNewFile(App.getAppContext(),path,name,uri,uri_path);
        }
    }


    private static boolean createSAFNewFile(Context context, String target_file_path, String name, Uri tree_uri, String tree_uri_path)
    {
        Uri uri = FileUtil.createDocumentUri(context, target_file_path,name,false,tree_uri,tree_uri_path);
        return uri != null;
    }

    @Override
    public boolean makeDirIfNotExists(String dir_name) {
        File dir=new File(getPath(),dir_name);
        if(dir.exists())// && dir.isDirectory())
        {
            return true;
        }
        else {
            if(writeable)
            {
                if (dir.exists())
                {
                    return true;
                }

                return dir.mkdir();
            }
            else {
                return mkdirSAF(App.getAppContext(),path,dir_name,uri,uri_path);
            }
        }
    }


    @Override
    public boolean makeDirsRecursively(String extended_path) {
        if(writeable)
        {
            File f=new File(path,extended_path);
            return FileUtil.mkdirsNative(f);
        }
        else {
            return mkdirsSAFFile(App.getAppContext(),path,extended_path,uri,uri_path);
        }
    }

    @Override
    public long getLength() {
        return file.length();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public boolean isHidden() {
        return file.isHidden();
    }

    private static boolean renameSAFFile(Context context, String target_file_path, String new_name, Uri tree_uri, String tree_uri_path)
    {
        Uri uri = FileUtil.getDocumentUri(target_file_path,tree_uri,tree_uri_path);
        try {
            uri= DocumentsContract.renameDocument(context.getContentResolver(),uri,new_name);
        } catch (FileNotFoundException e) {

        }
        return uri!=null;
    }

    private static boolean mkdirSAF(Context context, String target_file_path, String name, Uri tree_uri, String tree_uri_path)
    {
        Uri uri=FileUtil.createDocumentUri(context,target_file_path,name, true,tree_uri,tree_uri_path);
        return uri!=null;

    }

    private static boolean mkdirsSAFFile(Context context, String parent_file_path, @NonNull String path, Uri tree_uri, String tree_uri_path)
    {
        boolean success=true;
        String [] file_path_substring=path.split("/");
        int size=file_path_substring.length;
        for (int i=0; i<size;++i)
        {
            String path_string=file_path_substring[i];
            if(!path_string.isEmpty())
            {
                if(!new File(parent_file_path,path_string).exists())
                {
                    success=mkdirSAF(context,parent_file_path,path_string,tree_uri,tree_uri_path);

                }
                parent_file_path+=File.separator+path_string;
                if(!success)
                {
                    return false;
                }
            }

        }
        return success;
    }

}
