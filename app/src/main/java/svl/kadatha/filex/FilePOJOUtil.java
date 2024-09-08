package svl.kadatha.filex;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import me.jahnen.libaums.core.fs.UsbFile;
import timber.log.Timber;

public class FilePOJOUtil {

    static final SimpleDateFormat SDF_FTP=new SimpleDateFormat("yyyyMMddHHmmss");
    private static final String TAG = "Ftp-FilePOJOUtil";
    static FilePOJO MAKE_FilePOJO(File f, boolean extracticon, FileObjectType fileObjectType)
    {
        String name=f.getName();
        String path=f.getAbsolutePath();
        boolean isDirectory=f.isDirectory();
        long dateLong=f.lastModified();
        String date=Global.SDF.format(dateLong);
        long sizeLong=0L;
        String si;

        String file_ext="";
        int overlay_visible= View.INVISIBLE;
        float alfa=Global.ENABLE_ALFA;
        String package_name = null;
        int type=R.drawable.folder_icon;

        if(!isDirectory)
        {
            type=R.drawable.unknown_file_icon;
            int idx=name.lastIndexOf(".");
            if(idx!=-1)
            {
                file_ext=name.substring(idx+1);
                type=GET_FILE_TYPE(isDirectory,file_ext);
                if(type==-2)
                {
                    overlay_visible=View.VISIBLE;
                }
                else if(extracticon && type==0)
                {
                    package_name=EXTRACT_ICON(MainActivity.PM,path,file_ext);
                }
            }

            sizeLong=f.length();
            si=FileUtil.humanReadableByteCount(sizeLong);
        }
        else
        {
            String sub_file_count=null;
            String [] file_list;
            if((file_list=f.list(Global.File_NAME_FILTER))!=null)
            {
                sub_file_count="("+file_list.length+")";
            }
            si=sub_file_count;
        }

        if(f.isHidden())
        {
            alfa=Global.DISABLE_ALFA;
        }

        return new FilePOJO(fileObjectType,name,package_name,path,isDirectory,dateLong,date,sizeLong,si,type,file_ext,alfa,overlay_visible,0,0L,null,0,null,null);
    }

    static FilePOJO MAKE_FilePOJO_ZIP(File f, boolean extracticon, FileObjectType fileObjectType)
    {
        String name=f.getName();
        String path=f.getAbsolutePath();
        boolean isDirectory=f.isDirectory();
        long dateLong=f.lastModified();
        String date=Global.SDF.format(dateLong);
        long sizeLong=0L;
        String si;

        String file_ext="";
        int overlay_visible= View.INVISIBLE;
        float alfa=Global.ENABLE_ALFA;
        String package_name = null;
        int type=R.drawable.folder_icon;

        if(!isDirectory)
        {
            type=R.drawable.unknown_file_icon;
            int idx=name.lastIndexOf(".");
            if(idx!=-1)
            {
                file_ext=name.substring(idx+1);
                type=GET_FILE_TYPE(isDirectory,file_ext);
                if(type==-2)
                {
                    overlay_visible=View.VISIBLE;
                }
                else if(extracticon && type==0)
                {
                    package_name=EXTRACT_ICON(MainActivity.PM,path,file_ext);
                }
            }
            try(ZipFile zipFile = new ZipFile(ArchiveViewActivity.ZIP_FILE))
            {
                ZipEntry zipEntry = zipFile.getEntry(path.substring(Global.ARCHIVE_CACHE_DIR_LENGTH + 1));
                if(zipEntry!=null) sizeLong = zipEntry.getSize();
            }
            catch (IOException e) {

            }
            si=FileUtil.humanReadableByteCount(sizeLong);
        }
        else
        {
            String sub_file_count=null;
            String [] file_list;
            if((file_list=f.list(Global.File_NAME_FILTER))!=null)
            {
                sub_file_count="("+file_list.length+")";
            }
            si=sub_file_count;
        }

        if(f.isHidden())
        {
            alfa=Global.DISABLE_ALFA;
        }

        return new FilePOJO(fileObjectType,name,package_name,path,isDirectory,dateLong,date,sizeLong,si,type,file_ext,alfa,overlay_visible,0,0L,null,0,null,null);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    static FilePOJO MAKE_FilePOJO(Path p, boolean extracticon, FileObjectType fileObjectType)
    {
        String name=p.getFileName().toString();
        String path=p.toAbsolutePath().toString();
        boolean isDirectory;
        long dateLong=0;
        long sizeLong=0L;
        try {
            BasicFileAttributes basicFileAttributes=Files.readAttributes(p,BasicFileAttributes.class);
            isDirectory=basicFileAttributes.isDirectory();
            dateLong=basicFileAttributes.lastModifiedTime().toMillis();
            if(!isDirectory) sizeLong=basicFileAttributes.size();
        } catch (IOException e) {
            isDirectory=Files.isDirectory(p);
            try {
                dateLong = Files.getLastModifiedTime(p).toMillis();
            } catch (IOException ioe) {

            }
        }

        String date=Global.SDF.format(dateLong);

        String si;

        String file_ext="";
        int overlay_visible= View.INVISIBLE;
        float alfa=Global.ENABLE_ALFA;
        String package_name = null;
        int type=R.drawable.folder_icon;

        if(!isDirectory)
        {
            type=R.drawable.unknown_file_icon;
            int idx=name.lastIndexOf(".");
            if(idx!=-1)
            {
                file_ext=name.substring(idx+1);
                type=GET_FILE_TYPE(isDirectory,file_ext);
                if(type==-2)
                {
                    overlay_visible=View.VISIBLE;
                }
                else if(extracticon && type==0)
                {
                    package_name=EXTRACT_ICON(MainActivity.PM,path,file_ext);
                }
            }

            si=FileUtil.humanReadableByteCount(sizeLong);
        }
        else
        {
            String sub_file_count=null;
            try(DirectoryStream<Path> directoryStream=Files.newDirectoryStream(Paths.get(path),Global.GET_NIO_FILE_NAME_FILTER()))
            {
                int count = 0;
                for(Path pa : directoryStream)
                {
                    ++count;
                }
                sub_file_count="("+count+")";
            } catch (IOException e) {

            }
            si=sub_file_count;
        }

        if(p.startsWith("."))
        {
            alfa=Global.DISABLE_ALFA;
        }

        return new FilePOJO(fileObjectType,name,package_name,path,isDirectory,dateLong,date,sizeLong,si,type,file_ext,alfa,overlay_visible,0,0L,null,0,null,null);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    static FilePOJO MAKE_FilePOJO_ZIP(Path p, boolean extracticon, FileObjectType fileObjectType)
    {
        String name=p.getFileName().toString();
        String path=p.toAbsolutePath().toString();
        boolean isDirectory;
        long dateLong=0;
        long sizeLong=0L;
        try {
            BasicFileAttributes basicFileAttributes=Files.readAttributes(p,BasicFileAttributes.class);
            isDirectory=basicFileAttributes.isDirectory();
            dateLong=basicFileAttributes.lastModifiedTime().toMillis();
            if(!isDirectory) sizeLong=basicFileAttributes.size();
        } catch (IOException e) {
            isDirectory=Files.isDirectory(p);
            try {
                dateLong = Files.getLastModifiedTime(p).toMillis();
            } catch (IOException ioe) {

            }
        }

        String date=Global.SDF.format(dateLong);

        String si;

        String file_ext="";
        int overlay_visible= View.INVISIBLE;
        float alfa=Global.ENABLE_ALFA;
        String package_name = null;
        int type=R.drawable.folder_icon;

        if(!isDirectory)
        {
            type=R.drawable.unknown_file_icon;
            int idx=name.lastIndexOf(".");
            if(idx!=-1)
            {
                file_ext=name.substring(idx+1);
                type=GET_FILE_TYPE(isDirectory,file_ext);
                if(type==-2)
                {
                    overlay_visible=View.VISIBLE;
                }
                else if(extracticon && type==0)
                {
                    package_name=EXTRACT_ICON(MainActivity.PM,path,file_ext);
                }
            }
            try(ZipFile zipFile = new ZipFile(ArchiveViewActivity.ZIP_FILE))
            {
                ZipEntry zipEntry = zipFile.getEntry(path.substring(Global.ARCHIVE_CACHE_DIR_LENGTH + 1));
                if(zipEntry!=null) sizeLong = zipEntry.getSize();
            }
            catch (IOException e){}
            si=FileUtil.humanReadableByteCount(sizeLong);
        }
        else
        {

            String sub_file_count=null;
            try(DirectoryStream<Path> directoryStream=Files.newDirectoryStream(Paths.get(path),Global.GET_NIO_FILE_NAME_FILTER()))
            {
                int count = 0;
                for(Path pa : directoryStream)
                {
                    ++count;
                }
                sub_file_count="("+count+")";
            } catch (IOException e) {

            }
            si=sub_file_count;
        }

        if(p.startsWith("."))
        {
            alfa=Global.DISABLE_ALFA;
        }

        return new FilePOJO(fileObjectType,name,package_name,path,isDirectory,dateLong,date,sizeLong,si,type,file_ext,alfa,overlay_visible,0,0L,null,0,null,null);
    }

    static FilePOJO MAKE_FilePOJO(UsbFile f, boolean extract_icon)
    {
        String name=f.getName();
        String path=f.getAbsolutePath();
        boolean isDirectory=f.isDirectory();

        long dateLong=0L;
        String date="date";
        try
        {
            dateLong=f.lastModified();
            date=Global.SDF.format(dateLong);
        }
        catch (Exception e)
        {

        }

        long sizeLong=0L;
        String si;
        String file_ext="";
        int overlay_visible=View.INVISIBLE;
        float alfa=Global.ENABLE_ALFA;
        String package_name=null;
        int type=R.drawable.folder_icon;

        if(!isDirectory)
        {
            type=R.drawable.unknown_file_icon;
            int idx=name.lastIndexOf(".");
            if(idx!=-1)
            {
                file_ext=name.substring(idx+1);
                type=GET_FILE_TYPE(isDirectory,file_ext);
                if(type==-2)
                {
                    overlay_visible=View.VISIBLE;
                }
                else if(extract_icon && type==0)
                {
                    package_name=EXTRACT_ICON(MainActivity.PM,path,file_ext);
                }
            }
            sizeLong=f.getLength();
            si=FileUtil.humanReadableByteCount(sizeLong);
        }
        else
        {
            String sub_file_count=null;
            String [] file_list;
            try {
                file_list=f.list();
                sub_file_count="("+file_list.length+")";

            } catch (IOException e) {
                MainActivity.usbFileRoot=null;
            }
            si=sub_file_count;
        }

        return new FilePOJO(FileObjectType.USB_TYPE,name,package_name,path,isDirectory,dateLong,date,sizeLong,si,type,file_ext,alfa,overlay_visible,0,0L,null,0,null,null);
    }

    static FilePOJO MAKE_FilePOJO(FTPFile f, boolean extracticon, FileObjectType fileObjectType, String file_path, FTPClient ftpClient) {
        Timber.tag(TAG).d("Creating FilePOJO for FTP file: %s", file_path);
        String name = f.getName();
        String path = file_path;
        boolean isDirectory = f.isDirectory();
        long dateLong = 0L;
        String date = "";
        try {
            String str = ftpClient.getModificationTime(file_path);
            if (str != null) {
                if (str.contains(" ")) {
                    str = str.substring(str.indexOf(" "));
                }
                Date d = SDF_FTP.parse(str);
                date = Global.SDF.format(d);
            }
        } catch (Exception e) {
            Timber.tag(TAG).e("Error getting modification time for FTP file: %s", e.getMessage());
        }

        long sizeLong = 0L;
        String si = "";

        String file_ext = "";
        int overlay_visible = View.INVISIBLE;
        float alfa = Global.ENABLE_ALFA;
        String package_name = null;
        int type = R.drawable.folder_icon;

        if (!isDirectory) {
            type = R.drawable.unknown_file_icon;
            int idx = name.lastIndexOf(".");
            if (idx != -1) {
                file_ext = name.substring(idx + 1);
                type = GET_FILE_TYPE(isDirectory, file_ext);
                if (type == -2) {
                    overlay_visible = View.VISIBLE;
                } else if (extracticon && type == 0) {
                    package_name = EXTRACT_ICON(MainActivity.PM, path, file_ext);
                }
            }

            sizeLong = f.getSize();
            si = FileUtil.humanReadableByteCount(sizeLong);
        } else {
            String sub_file_count = null;
            String[] file_list;
            try {
                if ((file_list = ftpClient.listNames(file_path)) != null) {
                    sub_file_count = "(" + file_list.length + ")";
                }
                si = sub_file_count;
            } catch (IOException e) {
                Timber.tag(TAG).e("Error listing FTP directory contents: %s", e.getMessage());
            }
        }

        FilePOJO filePOJO = new FilePOJO(fileObjectType, name, package_name, path, isDirectory, dateLong, date, sizeLong, si, type, file_ext, alfa, overlay_visible, 0, 0L, null, 0, null, null);
        Timber.tag(TAG).d("Created FilePOJO for FTP file: %s, isDirectory: %b, size: %d", name, isDirectory, sizeLong);
        return filePOJO;
    }

    static FilePOJO MAKE_FilePOJO_ROOT(String file_path)
    {
        String[] command_line_long = {"ls", "-ld", file_path};
        try {
            Process process_long = Runtime.getRuntime().exec(command_line_long);
            //java.lang.Process process_long = Runtime.getRuntime().exec("ls -ld "+file_path);
            BufferedReader bf_long = new BufferedReader(new InputStreamReader(process_long.getInputStream()));
            String line_long=bf_long.readLine(); //consume first line as not required
            if(line_long != null) {

                return PARSE_MAKE_FilePOJO_ROOT(line_long,new File(file_path).getParent());

            }
            process_long.waitFor();
        }
        catch(Exception e){return  null;}
        return null;
    }

    static FilePOJO PARSE_MAKE_FilePOJO_ROOT(String line, String parent_file_path)
    {

        String [] split_line=line.split("\\s+");
        int split_count=split_line.length;
        String permission=split_line[0];
        String name;
        String path="";
        boolean isDirectory=false;
        long dateLong=0L;
        String date;
        long sizeLong = 0;

        if(permission.startsWith("l"))
        {
            name=split_line[split_count-3];

            if(split_count<=8)
            {
                try {
                    sizeLong=Long.parseLong(split_line[2]);
                }
                catch (NumberFormatException nfe)
                {
                    sizeLong=0L;
                }
                if(sizeLong==4096) isDirectory=true;
                date=split_line[3];
            }
            else
            {
                try {
                    sizeLong=Long.parseLong(split_line[3]);
                }
                catch (NumberFormatException nfe)
                {
                    sizeLong=0L;
                }
                if(sizeLong==4096) isDirectory=true;
                date=split_line[4];
            }
        }
        else
        {
            name=split_line[split_count-1];
            isDirectory= permission.startsWith("d");

            if(split_count<=6)
            {
                try {
                    sizeLong=Long.parseLong(split_line[2]);
                }
                catch (NumberFormatException nfe)
                {
                    sizeLong=0L;
                }
                date=split_line[3];
            }
            else
            {
                try {
                    sizeLong=Long.parseLong(split_line[3]);
                }
                catch (NumberFormatException nfe)
                {
                    sizeLong=0L;
                }
                date=split_line[4];
            }

        }

        path=Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,name);

        String si=FileUtil.humanReadableByteCount(sizeLong);

        String file_ext="";
        int overlay_visible= View.INVISIBLE;
        float alfa=Global.ENABLE_ALFA;
        int type=R.drawable.folder_icon;

        if(!isDirectory)
        {
            type=R.drawable.unknown_file_icon;
            int idx=name.lastIndexOf(".");
            if(idx!=-1)
            {
                file_ext=name.substring(idx+1);
                type=GET_FILE_TYPE(isDirectory,file_ext);
                if(type==-2)
                {
                    overlay_visible=View.VISIBLE;
                }
            }

        }
        else
        {
            String[] command_line = {"ls", "-l", path};
            try {
                Process process = Runtime.getRuntime().exec(command_line);
                BufferedReader bf_long = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int count=0;
                while(bf_long.readLine()!=null)
                {
                    ++count;
                }

                si="("+count+")";
                process.waitFor();
            }
            catch(Exception e){}

        }

        if(name.startsWith("."))
        {
            alfa=Global.DISABLE_ALFA;
        }


        return new FilePOJO(FileObjectType.ROOT_TYPE,name,null,path,isDirectory,dateLong,date,sizeLong,si,type,file_ext,alfa,overlay_visible,0,0L,null,0,null,null);
    }



    static FilePOJO MAKE_FilePOJO(FileObjectType fileObjectType, String file_path)
    {
        FilePOJO filePOJO=null;
        if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
        {
            File f=new File(file_path);
            filePOJO=MAKE_FilePOJO(f,true,fileObjectType);
        }
        else if(fileObjectType==FileObjectType.USB_TYPE)
        {
            if(MainActivity.usbFileRoot==null)
            {
                return null;
            }
            try {
                UsbFile f = MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(file_path));
                filePOJO=MAKE_FilePOJO(f,true);
            }
            catch (IOException e) {
                return  null;}
        }
        /*
        else if(fileObjectType==FileObjectType.ROOT_TYPE)
        {
            filePOJO=MAKE_FilePOJO_ROOT(file_path);
        }

         */
        else if(fileObjectType==FileObjectType.FTP_TYPE)
        {
            if(file_path.equals(File.separator))
            {
                filePOJO=new FilePOJO(fileObjectType,File.separator,null,File.separator,true,0L,null,0L,null,R.drawable.folder_icon,null,Global.ENABLE_ALFA,View.INVISIBLE,0,0L,null,0,null,null);
            }
            else
            {
                FtpClientRepository ftpClientRepository=FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
                FTPClient ftpClient= null;
                try {
                    ftpClient = ftpClientRepository.getFtpClient();
                    FTPFile f=FileUtil.getFtpFile(ftpClient,file_path);
                    if(f!=null)
                    {
                        filePOJO=MAKE_FilePOJO(f,false, fileObjectType,file_path,ftpClient);
                    }

                } catch (IOException e) {

                }
                finally {
                    if (ftpClientRepository != null && ftpClient != null) {
                        ftpClientRepository.releaseFtpClient(ftpClient);
                    }
                }

            }

        }

        return filePOJO;
    }

    static String EXTRACT_ICON(PackageManager packageManager, String file_path, String file_ext)
    {
        if(packageManager==null) return null;
        if(file_ext.matches(Global.APK_REGEX))
        {
            PackageInfo PI = packageManager.getPackageArchiveInfo(file_path, 0);
            if(PI==null) return null;
            PI.applicationInfo.publicSourceDir = file_path;
            String package_name=PI.packageName;
            String file_with_package_name=package_name+".png";
            AppManagerListFragment.extract_icon(file_with_package_name,packageManager,PI);
            return package_name;
        }
        else
        {
            return null;
        }
    }


    static int GET_FILE_TYPE(boolean isDirectory, String file_ext)
    {
        if(isDirectory)
        {
            return R.drawable.folder_icon;
        }
        else if(file_ext.matches(Global.AUDIO_REGEX))
        {
            return R.drawable.audio_file_icon;
        }
        else if(file_ext.matches(Global.PDF_REGEX))
        {
            return R.drawable.pdf_file_icon;
        }
        else if(file_ext.matches(Global.APK_REGEX))
        {
            return 0;
        }
        else if(file_ext.matches(Global.ZIP_REGEX) || file_ext.matches(Global.UNIX_ARCHIVE_REGEX))
        {
            return R.drawable.archive_file_icon;
        }
        else if(file_ext.matches(Global.IMAGE_REGEX))
        {
            return -1;
        }
        else if(file_ext.matches(Global.VIDEO_REGEX))
        {
            return -2;
        }
        else if(file_ext.matches(Global.TEXT_REGEX) || file_ext.matches( Global.RTF_REGEX))
        {
            return R.drawable.text_file_icon;
        }
        else if(file_ext.matches(Global.DOC_REGEX))
        {
            return R.drawable.word_file_icon;
        }
        else if(file_ext.matches(Global.XLS_REGEX))
        {
            return R.drawable.xls_file_icon;
        }
        else if(file_ext.matches(Global.PPT_REGEX))
        {
            return R.drawable.ppt_file_icon;
        }
        else
        {
            return R.drawable.unknown_file_icon;
        }
    }

    public static void REMOVE_FROM_HASHMAP_FILE_POJO_ON_REMOVAL_SEARCH_LIBRARY(String filePOJOHashmapKeyPath,final List<String> deleted_files_path_list, FileObjectType fileObjectType)
    {
        final int size=deleted_files_path_list.size();
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOs=repositoryClass.hashmap_file_pojo.get(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath);
        List<FilePOJO> filePOJOs_filtered=repositoryClass.hashmap_file_pojo_filtered.get(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath);
        if(filePOJOs==null || filePOJOs_filtered==null)
        {
            return;
        }
        for(int i=0;i<size;++i)
        {
            String deleted_file_path=deleted_files_path_list.get(i);
            File f=new File(deleted_file_path);
            String deleted_file_name=f.getName();
            String parent_folder=f.getParent();

            remove_from_FilePOJO_comparing_file_path(deleted_file_path,filePOJOs);
            remove_from_FilePOJO_comparing_file_path(deleted_file_path,filePOJOs_filtered);

            REMOVE_FROM_HASHMAP_FILE_POJO(parent_folder, Collections.singletonList(deleted_file_name),fileObjectType);
            REMOVE_FROM_AUDIO_CACHE(fileObjectType,deleted_file_path);

        }
        repositoryClass.hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath,filePOJOs);
        repositoryClass.hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath,filePOJOs_filtered);

    }

    public static void REMOVE_FROM_HASHMAP_FILE_POJO(final String source_folder, final List<String> deleted_files_name_list,FileObjectType fileObjectType)
    {
        final int size=deleted_files_name_list.size();
        UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(source_folder,fileObjectType);
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOs=repositoryClass.hashmap_file_pojo.get(fileObjectType+source_folder);
        List<FilePOJO> filePOJOs_filtered=repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType+source_folder);
        if(filePOJOs==null || filePOJOs_filtered==null)
        {
            UsbFile currentUsbFile=null;
            if(fileObjectType==FileObjectType.USB_TYPE)
            {
                if(MainActivity.usbFileRoot!=null)
                {
                    try {
                        currentUsbFile=MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(source_folder));

                    } catch (IOException e) {

                    }
                }
            }
            FILL_FILE_POJO(new ArrayList<>(), new ArrayList<>(),fileObjectType,source_folder,currentUsbFile,false);
            filePOJOs=repositoryClass.hashmap_file_pojo.get(fileObjectType+source_folder);
            filePOJOs_filtered=repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType+source_folder);
        }
        if(filePOJOs==null || filePOJOs_filtered==null) return;
        String name;
        for(int i=0;i<size;++i)
        {
            name=deleted_files_name_list.get(i);
            remove_from_FilePOJO(name,filePOJOs);
            remove_from_FilePOJO(name,filePOJOs_filtered);
            String file_to_be_removed=Global.CONCATENATE_PARENT_CHILD_PATH(source_folder,name);
            REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(file_to_be_removed),fileObjectType);

            if(!REMOVE_FROM_LIBRARY_CACHE(fileObjectType,file_to_be_removed,"Download"))
            {
                if(!REMOVE_FROM_LIBRARY_CACHE(fileObjectType,file_to_be_removed,"Document"))
                {
                    if(!REMOVE_FROM_LIBRARY_CACHE(fileObjectType,file_to_be_removed,"Image"))
                    {
                        if(!REMOVE_FROM_LIBRARY_CACHE(fileObjectType,file_to_be_removed,"Audio"))
                        {
                            if(!REMOVE_FROM_LIBRARY_CACHE(fileObjectType,file_to_be_removed,"Video"))
                            {
                                if(!REMOVE_FROM_LIBRARY_CACHE(fileObjectType,file_to_be_removed,"Archive"))
                                {
                                    REMOVE_FROM_LIBRARY_CACHE(fileObjectType,file_to_be_removed, "APK");
                                }
                            }
                        }
                    }
                }
            }

            REMOVE_FROM_LIBRARY_CACHE(fileObjectType,file_to_be_removed, "Large Files");
            REMOVE_FROM_LIBRARY_CACHE(fileObjectType,file_to_be_removed,"Duplicate Files");
            REMOVE_FROM_AUDIO_CACHE(fileObjectType,file_to_be_removed);
        }

        repositoryClass.hashmap_file_pojo.put(fileObjectType+source_folder,filePOJOs);
        repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType+source_folder,filePOJOs_filtered);
    }


    private static FilePOJO remove_from_FilePOJO(String name, List<FilePOJO>list)
    {
        FilePOJO deleted_filePOJO = null;
        Iterator<FilePOJO> iterator=list.iterator();
        while(iterator.hasNext())
        {
            FilePOJO filePOJO=iterator.next();
            if(filePOJO.getName().equals(name))
            {
                deleted_filePOJO=filePOJO;
                iterator.remove();
                return deleted_filePOJO;
            }
        }

        return deleted_filePOJO;
    }

    private static void remove_from_FilePOJO_comparing_file_path(String file_path, List<FilePOJO>list)
    {
        Iterator<FilePOJO> iterator=list.iterator();
        while(iterator.hasNext())
        {
            if(iterator.next().getPath().equals(file_path))
            {
                iterator.remove();
                break;
            }
        }
    }

    private static boolean REMOVE_FROM_LIBRARY_CACHE(FileObjectType fileObjectType,String file_path, String media_category)
    {
        if(fileObjectType!=FileObjectType.FILE_TYPE)return false;
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOs=repositoryClass.hashmap_file_pojo.get(FileObjectType.SEARCH_LIBRARY_TYPE+media_category);
        List<FilePOJO> filePOJOs_filtered=repositoryClass.hashmap_file_pojo_filtered.get(FileObjectType.SEARCH_LIBRARY_TYPE+media_category);
        if(filePOJOs!=null)
        {
            Iterator<FilePOJO> iterator=filePOJOs.iterator();
            while (iterator.hasNext())
            {
                FilePOJO filePOJO= iterator.next();
                if(file_path.equals(filePOJO.getPath()))
                {
                    if(filePOJOs_filtered!=null)filePOJOs_filtered.remove(filePOJO);
                    iterator.remove();
                    return true;
                }
            }
        }
        return false;
    }

    private static void REMOVE_FROM_AUDIO_CACHE(FileObjectType fileObjectType,String file_path_to_be_removed)
    {
        if(fileObjectType!=FileObjectType.FILE_TYPE)return;
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        List<AudioPOJO> audioPOJOS=repositoryClass.audio_pojo_hashmap.get("audio");
        if(audioPOJOS!=null)
        {
            Iterator<AudioPOJO> iterator= audioPOJOS.iterator();
            while (iterator.hasNext())
            {
                AudioPOJO audioPOJO=iterator.next();
                if(audioPOJO.getData().equals(file_path_to_be_removed))
                {
                    iterator.remove();
                    break;
                }
            }
        }

    }

    public static FilePOJO ADD_TO_HASHMAP_FILE_POJO(final String dest_folder, final List<String> added_file_name_list, FileObjectType fileObjectType, List<String> overwritten_file_path_list)
    {
        FilePOJO filePOJO = null;
        int size;
        UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(dest_folder,fileObjectType);
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOs=repositoryClass.hashmap_file_pojo.get(fileObjectType+dest_folder);
        List<FilePOJO> filePOJOs_filtered=repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType+dest_folder);
        if(filePOJOs==null)
        {
            UsbFile currentUsbFile=null;
            if(fileObjectType==FileObjectType.USB_TYPE)
            {
                if(MainActivity.usbFileRoot!=null)
                {
                    try {
                        currentUsbFile=MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(dest_folder));
                    } catch (IOException e) {

                    }
                }
            }
            FILL_FILE_POJO(new ArrayList<>(), new ArrayList<>(),fileObjectType,dest_folder,currentUsbFile,false);
            filePOJOs=repositoryClass.hashmap_file_pojo.get(fileObjectType+dest_folder);
            filePOJOs_filtered=repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType+dest_folder);
            if(filePOJOs==null)return null;
        }
        else
        {
            if(overwritten_file_path_list!=null) // while creating new file, overwritten_file_path_list is null
            {
                size=overwritten_file_path_list.size();
                for(int i=0;i<size;++i)
                {
                    String overwritten_file_path=overwritten_file_path_list.get(i);
                    String overwritten_file_name=new File(overwritten_file_path).getName();
                    remove_from_FilePOJO(overwritten_file_name,filePOJOs);
                    remove_from_FilePOJO(overwritten_file_name,filePOJOs_filtered);
                }
            }


            size=added_file_name_list.size();
            String file_path;
            for(int i=0;i<size;++i)
            {
                file_path=Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder,added_file_name_list.get(i));
                filePOJO=MAKE_FilePOJO(fileObjectType,file_path);
                if(filePOJO!=null)
                {
                    filePOJOs.add(filePOJO);
                    if(filePOJO.getAlfa()==Global.ENABLE_ALFA)
                    {
                        filePOJOs_filtered.add(filePOJO);
                    }
                }
            }
            repositoryClass.hashmap_file_pojo.put(fileObjectType+dest_folder,filePOJOs);
            repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType+dest_folder,filePOJOs_filtered);

        }

        REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(overwritten_file_path_list, fileObjectType);
        return filePOJO;

    }

    public static FilePOJO ADD_TO_HASHMAP_FILE_POJO_ON_ADD_SEARCH_LIBRARY(String filePOJOHashmapKeyPath, final List<String> added_file_path_list, FileObjectType fileObjectType, List<String> overwritten_file_path_list)
    {
        FilePOJO filePOJO = null;
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOs=repositoryClass.hashmap_file_pojo.get(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath);
        List<FilePOJO> filePOJOs_filtered=repositoryClass.hashmap_file_pojo_filtered.get(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath);
        if(filePOJOs==null || filePOJOs_filtered==null)
        {
            return null;
        }

        int size;
        if(overwritten_file_path_list!=null) // while creating new file, overwritten_file_path_list is null
        {
            size=overwritten_file_path_list.size();
            for(int i=0;i<size;++i)
            {
                String overwritten_file_path=overwritten_file_path_list.get(i);
                remove_from_FilePOJO_comparing_file_path(overwritten_file_path,filePOJOs);
                remove_from_FilePOJO_comparing_file_path(overwritten_file_path,filePOJOs_filtered);
            }
        }


        size=added_file_path_list.size();
        for(int i=0;i<size;++i)
        {
            String file_path=added_file_path_list.get(i);
            File f=new File(file_path);
            String name=f.getName();
            String parent_file_path=f.getParent();
            List<String> file_name_list=Collections.singletonList(name);
            FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(parent_file_path,file_name_list,fileObjectType);
            filePOJO=ADD_TO_HASHMAP_FILE_POJO(parent_file_path, file_name_list,fileObjectType,overwritten_file_path_list); //single file is added, the last file pojo returned is the only filepojo
            if(filePOJO==null)
            {
                filePOJO=MAKE_FilePOJO(fileObjectType,file_path);
            }
            if(filePOJO!=null)
            {
                filePOJOs.add(filePOJO);
                if(filePOJO.getAlfa()==Global.ENABLE_ALFA)
                {
                    filePOJOs_filtered.add(filePOJO);
                }
            }
        }
        repositoryClass.hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath,filePOJOs);
        repositoryClass.hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath,filePOJOs_filtered);

        REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(overwritten_file_path_list, fileObjectType);
        return filePOJO;
    }


    public static void UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(String dest_folder, FileObjectType fileObjectType)
    {
        String parent_path_to_dest_folder=new File(dest_folder).getParent();
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOs=repositoryClass.hashmap_file_pojo.get(fileObjectType+parent_path_to_dest_folder);
        List<FilePOJO> filePOJOs_filtered=repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType+parent_path_to_dest_folder);
        if(filePOJOs==null ||filePOJOs_filtered==null)
        {
            return;
        }
        String name=new File(dest_folder).getName();
        FilePOJO removed_filePOJO=remove_from_FilePOJO(name,filePOJOs);
        remove_from_FilePOJO(name,filePOJOs_filtered);
        FilePOJO filePOJO =MAKE_FilePOJO(fileObjectType,dest_folder);
        if(filePOJO==null)filePOJO=removed_filePOJO;
        if(filePOJO!=null)
        {
            filePOJOs.add(filePOJO);
            if(filePOJO.getAlfa()==Global.ENABLE_ALFA)
            {
                filePOJOs_filtered.add(filePOJO);
            }

        }
        repositoryClass.hashmap_file_pojo.put(fileObjectType+parent_path_to_dest_folder,filePOJOs);
        repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType+parent_path_to_dest_folder,filePOJOs_filtered);
    }


    public static void  REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(List<String> file_path_list, FileObjectType fileObjectType)
    {
        if(file_path_list==null) return;
        int size=file_path_list.size();
        for(int i=0;i<size;++i)
        {
            String file_path=file_path_list.get(i);
            REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL__(file_path,fileObjectType);
        }
        if(size>0) SET_PARENT_HASHMAP_FILE_POJO_SIZE_NULL(file_path_list.get(0),fileObjectType);
    }

    private static void  REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL__(String file_path, FileObjectType fileObjectType)
    {
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        Iterator<Map.Entry<String, List<FilePOJO>>> iterator=repositoryClass.hashmap_file_pojo.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry<String, List<FilePOJO>> entry= iterator.next();
            if(Global.IS_CHILD_FILE(entry.getKey(),fileObjectType+file_path))
            {
                iterator.remove();
            }
        }

        iterator=repositoryClass.hashmap_file_pojo_filtered.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry<String, List<FilePOJO>> entry= iterator.next();
            if(Global.IS_CHILD_FILE(entry.getKey(),fileObjectType+file_path))
            {
                iterator.remove();
            }
        }

        repositoryClass.hashmap_internal_directory_size.remove(file_path);
        repositoryClass.hashmap_external_directory_size.remove(file_path);

    }

    public static void SET_PARENT_HASHMAP_FILE_POJO_SIZE_NULL(String file_path,FileObjectType fileObjectType)
    {
        String parent_file_path=new File(file_path).getParent();
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        if(parent_file_path!=null)
        {
            for (Map.Entry<String, List<FilePOJO>> entry : repositoryClass.hashmap_file_pojo.entrySet()) {
                if (Global.IS_CHILD_FILE(fileObjectType + file_path, entry.getKey())) {
                    List<FilePOJO> filePOJOS = entry.getValue();
                    if (filePOJOS != null) {
                        for (FilePOJO filePOJO : filePOJOS) {
                            filePOJO.setTotalFiles(0);
                            filePOJO.setTotalSizeLong(0L);
                            filePOJO.setTotalSize(null);
                            filePOJO.setTotalSizePercentageDouble(0);
                            filePOJO.setTotalSizePercentage(null);
                        }
                    }
                }

            }
        }

    }

    public static void SET_HASHMAP_FILE_POJO_SIZE_NULL(String file_path,FileObjectType fileObjectType)
    {
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        for (Map.Entry<String, List<FilePOJO>> entry : repositoryClass.hashmap_file_pojo.entrySet()) {
            if ((fileObjectType + file_path).equals(entry.getKey())) {
                List<FilePOJO> filePOJOS = entry.getValue();
                if (filePOJOS != null) {
                    for (FilePOJO filePOJO : filePOJOS) {
                        filePOJO.setTotalFiles(0);
                        filePOJO.setTotalSizeLong(0L);
                        filePOJO.setTotalSize(null);
                        filePOJO.setTotalSizePercentageDouble(0);
                        filePOJO.setTotalSizePercentage(null);
                    }
                }
                break;
            }

        }

    }


    public static void FILL_FILE_POJO(List<FilePOJO> filePOJOS, List<FilePOJO> filePOJOS_filtered, FileObjectType fileObjectType,
                                      String fileclickselected, UsbFile usbFile , boolean archive_view)
    {
        filePOJOS.clear(); filePOJOS_filtered.clear();
        String other_permission_string = null;
        File file=new File(fileclickselected);
/*
        if(fileObjectType==FileObjectType.ROOT_TYPE)
        {
            String modified_other_permission_string=Global.GET_OTHER_FILE_PERMISSION(fileclickselected);
            Timber.tag("Shankar").d(","file_path - "+fileclickselected+"   existing other permission string - "+modified_other_permission_string+" permission to read - "+file.canRead());
            Global.SET_OTHER_FILE_PERMISSION("rwx",fileclickselected);
            SecurityManager securityManager=new SecurityManager();
            securityManager.checkRead(fileclickselected);

            file.setExecutable(true,false);
            file.setReadable(true,false);
            Timber.tag("Shankar").d("," owner of file - "+file.canRead());

        }

 */
        if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                try(DirectoryStream<Path> directoryStream= Files.newDirectoryStream(Paths.get(fileclickselected)))
                {
                    if(archive_view)
                    {
                        for(Path path : directoryStream)
                        {
                            FilePOJO filePOJO =MAKE_FilePOJO_ZIP(path,true,fileObjectType);
                            if(!filePOJO.getName().startsWith("."))
                            {
                                filePOJOS_filtered.add(filePOJO);
                            }
                            filePOJOS.add(filePOJO);

                        }
                    }
                    else{
                        for(Path path : directoryStream)
                        {
                            FilePOJO filePOJO =MAKE_FilePOJO(path,true,fileObjectType);
                            if(!filePOJO.getName().startsWith("."))
                            {
                                filePOJOS_filtered.add(filePOJO);
                            }
                            filePOJOS.add(filePOJO);

                        }
                    }

                } catch (IOException e) {

                }

            }
            else
            {
                if(archive_view)
                {
                    file_type_fill_filePOJO_zip(file,fileObjectType,filePOJOS,filePOJOS_filtered);
                }
                else {
                    file_type_fill_filePOJO(file, fileObjectType,filePOJOS,filePOJOS_filtered);
                }

            }

        }
        else if(fileObjectType==FileObjectType.USB_TYPE)
        {
            if(MainActivity.usbFileRoot==null)
            {
                return;
            }
            else
            {
                UsbFile[] file_array;
                try {
                    if(usbFile==null)
                    {
                        usbFile=MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(fileclickselected));
                    }
                    file_array=usbFile.listFiles();
                    int size=file_array.length;
                    for(int i=0;i<size;++i)
                    {
                        UsbFile f=file_array[i];
                        FilePOJO filePOJO=MAKE_FilePOJO(f,true);
                        filePOJOS_filtered.add(filePOJO);
                        filePOJOS.add(filePOJO);

                    }

                } catch (IOException e) {
                    MainActivity.usbFileRoot=null;
                    return;
                }
            }
        }
        else if (fileObjectType == FileObjectType.FTP_TYPE) {
            Timber.tag(TAG).d("Filling FilePOJO for FTP directory: %s", fileclickselected);
            FTPFile[] file_array;
            FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(FtpDetailsViewModel.FTP_POJO);
            FTPClient ftpClient=null;
            try {
                ftpClient = ftpClientRepository.getFtpClient();
                ftpClient.pwd();
                file_array = ftpClient.listFiles(fileclickselected);

                Timber.tag(TAG).d("Retrieved %d files from FTP directory", file_array.length);
                int size = file_array.length;
                for (int i = 0; i < size; ++i) {
                    FTPFile f = file_array[i];
                    String name = f.getName();
                    String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                    Timber.tag(TAG).d("Processing FTP file: %s", path);
                    FilePOJO filePOJO = MAKE_FilePOJO(f, false, fileObjectType, path, ftpClient);
                    filePOJOS_filtered.add(filePOJO);
                    filePOJOS.add(filePOJO);
                }
                Timber.tag(TAG).d("Successfully filled FilePOJO for FTP directory: %s", fileclickselected);
            } catch (Exception e) {
                Timber.tag(TAG).e("Error filling FilePOJO for FTP directory: %s", e.getMessage());
                return;
            }
            finally {
                if (ftpClientRepository != null && ftpClient != null) {
                    ftpClientRepository.releaseFtpClient(ftpClient);
                    Timber.tag(TAG).d("FTP client released");
                }
            }
        }
        /*
        else if(fileObjectType==FileObjectType.ROOT_TYPE) {

            String[] command_line_long = {"ls", "-l", fileclickselected};
            try {
                //java.lang.Process process_long = Runtime.getRuntime().exec(command_line_long);
                java.lang.Process process_long = Runtime.getRuntime().exec("ls -l "+fileclickselected);
                BufferedReader bf_long = new BufferedReader(new InputStreamReader(process_long.getInputStream()));
                String line_long;

                //line_long=bf_long.readLine(); //consume first line as not required
                while ((line_long = bf_long.readLine()) != null) {
                    FilePOJO filePOJO =PARSE_MAKE_FilePOJO_ROOT(line_long,fileclickselected);
                    if(!filePOJO.getName().startsWith("."))
                    {
                        filePOJOS_filtered.add(filePOJO);
                    }

                    filePOJOS.add(filePOJO);
                }
                process_long.waitFor();
            }
            catch(Exception e){}
        }

         */
/*
        if(fileObjectType==FileObjectType.ROOT_TYPE)
        {
            other_permission_string=Global.GET_OTHER_FILE_PERMISSION(fileclickselected);
            Timber.tag("Shankar").d(","file_path - "+fileclickselected+"    modified other permission - "+other_permission_string+" whether existsUri - "+new File(fileclickselected).existsUri());
            MAKE_FilePOJO_ROOT(fileclickselected);
        }

        if(other_permission_string!=null)
        {
            Global.SET_OTHER_FILE_PERMISSION(other_permission_string,fileclickselected);
        }

 */

        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        repositoryClass.hashmap_file_pojo.put(fileObjectType+fileclickselected,filePOJOS);
        repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType+fileclickselected,filePOJOS_filtered);
    }


    private static void file_type_fill_filePOJO(File file, FileObjectType fileObjectType, List<FilePOJO> filePOJOS, List<FilePOJO> filePOJOS_filtered)
    {
        File[] file_array;
        if((file_array=file.listFiles())!=null)
        {
            int size=file_array.length;
            for(int i=0;i<size;++i)
            {
                File f=file_array[i];
                FilePOJO filePOJO =MAKE_FilePOJO(f,true,fileObjectType);
                if(!filePOJO.getName().startsWith("."))
                {
                    filePOJOS_filtered.add(filePOJO);
                }
                filePOJOS.add(filePOJO);
            }

        }
    }


    private static void file_type_fill_filePOJO_zip(File file, FileObjectType fileObjectType,List<FilePOJO> filePOJOS, List<FilePOJO> filePOJOS_filtered)
    {
        File[] file_array;
        if((file_array=file.listFiles())!=null)
        {
            int size=file_array.length;
            for(int i=0;i<size;++i)
            {
                File f=file_array[i];
                FilePOJO filePOJO =MAKE_FilePOJO_ZIP(f,true,fileObjectType);
                if(!filePOJO.getName().startsWith("."))
                {
                    filePOJOS_filtered.add(filePOJO);
                }
                filePOJOS.add(filePOJO);
            }

        }
    }


}
