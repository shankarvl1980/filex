package svl.kadatha.filex;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import me.jahnen.libaums.core.fs.UsbFile;

public class FilePOJOUtil {

    static FilePOJO MAKE_FilePOJO(File f, boolean extracticon, boolean archive_view,FileObjectType fileObjectType)
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

        if(!isDirectory)
        {
            int idx=name.lastIndexOf(".");
            if(idx!=-1)
            {
                file_ext=name.substring(idx+1);
                if(extracticon)
                {
                    package_name=EXTRACT_ICON(MainActivity.PM,path,file_ext);
                }

                if(file_ext.matches(Global.VIDEO_REGEX))
                {
                    overlay_visible=View.VISIBLE;
                }
            }
            if(archive_view) {
                try {
                    ZipFile zipFile = new ZipFile(MainActivity.ZIP_FILE);
                    ZipEntry zipEntry = zipFile.getEntry(path.substring(Global.ARCHIVE_CACHE_DIR_LENGTH + 1));
                    sizeLong = zipEntry.getSize();

                } catch (IOException e) {
                }
            }
            else
            {
                sizeLong=f.length();
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

        int type=GET_FILE_TYPE(isDirectory,file_ext);
        return new FilePOJO(fileObjectType,name,package_name,path,isDirectory,dateLong,date,sizeLong,si,type,file_ext,alfa,overlay_visible,0,0L,null,0,null);
    }

    static FilePOJO MAKE_FilePOJO(UsbFile f, boolean extracticon)
    {
        String name=f.getName();
        String path=f.getAbsolutePath();
        boolean isDirectory=f.isDirectory();
        long dateLong=0L;
        String date="date";
        long sizeLong=0L;
        String si;
        String file_ext="";
        int overlay_visible=View.INVISIBLE;
        float alfa=Global.ENABLE_ALFA;
        String package_name=null;

        if(!isDirectory)
        {

            int idx=name.lastIndexOf(".");
            if(idx!=-1)
            {
                file_ext=name.substring(idx+1);
                if(extracticon)
                {
                    package_name=EXTRACT_ICON(MainActivity.PM,path,file_ext);
                }
                if(file_ext.matches(Global.VIDEO_REGEX))
                {
                    overlay_visible=View.VISIBLE;
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

        int type=GET_FILE_TYPE(isDirectory,file_ext);
        return new FilePOJO(FileObjectType.USB_TYPE,name,package_name,path,isDirectory,dateLong,date,sizeLong,si,type,file_ext,alfa,overlay_visible,0,0L,null,0,null);
    }

    static FilePOJO MAKE_FilePOJO(FTPFile f, boolean extracticon, boolean archive_view, FileObjectType fileObjectType,String file_path)
    {
        String name=f.getName();
        String path=file_path;
        boolean isDirectory=f.isDirectory();
        long dateLong= 0L;
        String date="";
        try {
            date = MainActivity.FTP_CLIENT.getModificationTime(file_path);
        } catch (IOException e) {

        }

        long sizeLong=0L;
        String si="";

        String file_ext="";
        int overlay_visible= View.INVISIBLE;
        float alfa=Global.ENABLE_ALFA;
        String package_name = null;

        if(!isDirectory)
        {
            int idx=name.lastIndexOf(".");
            if(idx!=-1)
            {
                file_ext=name.substring(idx+1);
                if(extracticon)
                {
                    package_name=EXTRACT_ICON(MainActivity.PM,path,file_ext);
                }
                if(file_ext.matches(Global.VIDEO_REGEX))
                {
                    overlay_visible=View.VISIBLE;
                }
            }

            sizeLong=f.getSize();
            si=FileUtil.humanReadableByteCount(sizeLong);
        }
        else
        {

            String sub_file_count=null;
            String [] file_list;
            try
            {
                if((file_list=MainActivity.FTP_CLIENT.listNames(file_path))!=null)
                {
                    sub_file_count="("+file_list.length+")";

                }
                si=sub_file_count;
            }
            catch (IOException e)
            {

            }

        }
        int type=GET_FILE_TYPE(isDirectory,file_ext);
        return new FilePOJO(fileObjectType,name,package_name,path,isDirectory,dateLong,date,sizeLong,si,type,file_ext,alfa,overlay_visible,0,0L,null,0,null);
    }



    static FilePOJO MAKE_FilePOJO_ROOT(String file_path)
    {
        String[] command_line_long = {"ls", "-ld", file_path};
        try {
            java.lang.Process process_long = Runtime.getRuntime().exec(command_line_long);
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

        if(!isDirectory)
        {
            int idx=name.lastIndexOf(".");
            if(idx!=-1)
            {
                file_ext=name.substring(idx+1);
                if(file_ext.matches(Global.VIDEO_REGEX))
                {
                    overlay_visible=View.VISIBLE;
                }
            }

        }
        else
        {
            String[] command_line = {"ls", "-l", path};
            try {
                java.lang.Process process = Runtime.getRuntime().exec(command_line);
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

        int type=GET_FILE_TYPE(isDirectory,file_ext);
        return new FilePOJO(FileObjectType.ROOT_TYPE,name,null,path,isDirectory,dateLong,date,sizeLong,si,type,file_ext,alfa,overlay_visible,0,0L,null,0,null);
    }



    static FilePOJO MAKE_FilePOJO(FileObjectType fileObjectType, String file_path)
    {
        FilePOJO filePOJO=null;
        if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
        {
            File f=new File(file_path);
            filePOJO=MAKE_FilePOJO(f,true,false,fileObjectType);
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
                filePOJO=new FilePOJO(fileObjectType,File.separator,null,File.separator,true,0L,null,0L,null,R.drawable.folder_icon,null,Global.ENABLE_ALFA,View.INVISIBLE,0,0L,null,0,null);
            }
            else
            {
                FTPFile f=FileUtil.getFTPFile(file_path);//MainActivity.FTP_CLIENT.mlistFile(file_path);
                filePOJO=MAKE_FilePOJO(f,false,false,fileObjectType,file_path);
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
            if(!Global.APK_ICON_PACKAGE_NAME_LIST.contains(file_with_package_name))
            {
                Drawable APKicon = PI.applicationInfo.loadIcon(packageManager);
                Bitmap bitmap;
                if(APKicon instanceof BitmapDrawable)
                {
                    bitmap=((BitmapDrawable)APKicon).getBitmap();
                }
                else
                {
                    bitmap = Bitmap.createBitmap(APKicon.getIntrinsicWidth(),APKicon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    APKicon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    APKicon.draw(canvas);
                }

                File f=new File(Global.APK_ICON_DIR,file_with_package_name);
                FileOutputStream fileOutputStream=null;
                try {
                    fileOutputStream=new FileOutputStream(f);
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,fileOutputStream);
                    fileOutputStream.close();
                    Global.APK_ICON_PACKAGE_NAME_LIST.add(file_with_package_name);
                } catch (IOException e) {
                    if(fileOutputStream!=null)
                    {
                        try {
                            fileOutputStream.close();
                        } catch (IOException ioException) {

                        }
                    }
                }

            }

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
            return -1;
        }
        else if(file_ext.matches(Global.ZIP_REGEX) || file_ext.matches(Global.UNIX_ARCHIVE_REGEX))
        {
            return R.drawable.archive_file_icon;
        }
        else if(file_ext.matches(Global.IMAGE_REGEX))
        {
            return 0;
        }
        else if(file_ext.matches(Global.VIDEO_REGEX))
        {
            return 0;
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
        List<FilePOJO> filePOJOs=Global.HASHMAP_FILE_POJO.get(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath);
        List<FilePOJO> filePOJOs_filtered=Global.HASHMAP_FILE_POJO_FILTERED.get(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath);
        if(filePOJOs==null)
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
        }
        Global.HASHMAP_FILE_POJO.put(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath,filePOJOs);
        Global.HASHMAP_FILE_POJO_FILTERED.put(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath,filePOJOs_filtered);

    }

    public static void REMOVE_FROM_HASHMAP_FILE_POJO(final String source_folder, final List<String> deleted_files_name_list,FileObjectType fileObjectType)
    {
        final int size=deleted_files_name_list.size();
        UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(source_folder,fileObjectType);
        List<FilePOJO> filePOJOs=Global.HASHMAP_FILE_POJO.get(fileObjectType+source_folder);
        List<FilePOJO> filePOJOs_filtered=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+source_folder);
        if(filePOJOs==null)
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
            FilePOJOUtil.FILL_FILEPOJO(new ArrayList<FilePOJO>(),new ArrayList<FilePOJO>(),fileObjectType,source_folder,currentUsbFile,false);
            filePOJOs=Global.HASHMAP_FILE_POJO.get(fileObjectType+source_folder);
            filePOJOs_filtered=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+source_folder);
            if(filePOJOs==null)return;
        }

        String name;
        for(int i=0;i<size;++i)
        {
            name=deleted_files_name_list.get(i);
            remove_from_FilePOJO(name,filePOJOs);
            remove_from_FilePOJO(name,filePOJOs_filtered);
            String folder_to_be_removed=Global.CONCATENATE_PARENT_CHILD_PATH(source_folder,name);
            REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(folder_to_be_removed),fileObjectType);

        }

        Global.HASHMAP_FILE_POJO.put(fileObjectType+source_folder,filePOJOs);
        Global.HASHMAP_FILE_POJO_FILTERED.put(fileObjectType+source_folder,filePOJOs_filtered);

        Global.AUDIO_POJO_HASHMAP.clear();
        Global.ALBUM_POJO_HASHMAP.clear();
    }


    private static int remove_from_FilePOJO(String name, List<FilePOJO>list)
    {
        Iterator<FilePOJO> iterator=list.iterator();
        int i = 0;
        while(iterator.hasNext())
        {
            if(iterator.next().getName().equals(name))
            {
                iterator.remove();
                return i;
            }
            ++i;
        }
        return i;
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

    public static FilePOJO ADD_TO_HASHMAP_FILE_POJO(final String dest_folder, final List<String> added_file_name_list, FileObjectType fileObjectType, List<String> overwritten_file_path_list)
    {
        FilePOJO filePOJO = null;
        int size;
        UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(dest_folder,fileObjectType);
        List<FilePOJO> filePOJOs=Global.HASHMAP_FILE_POJO.get(fileObjectType+dest_folder);
        List<FilePOJO> filePOJOs_filtered=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+dest_folder);
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
            FilePOJOUtil.FILL_FILEPOJO(new ArrayList<FilePOJO>(),new ArrayList<FilePOJO>(),fileObjectType,dest_folder,currentUsbFile,false);
            filePOJOs=Global.HASHMAP_FILE_POJO.get(fileObjectType+dest_folder);
            filePOJOs_filtered=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+dest_folder);
            if(filePOJOs==null)return null;
        }

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
            if(filePOJO==null) break;
            filePOJOs.add(filePOJO);
            if(filePOJO.getAlfa()==Global.ENABLE_ALFA)
            {
                filePOJOs_filtered.add(filePOJO);
            }
        }
        Global.HASHMAP_FILE_POJO.put(fileObjectType+dest_folder,filePOJOs);
        Global.HASHMAP_FILE_POJO_FILTERED.put(fileObjectType+dest_folder,filePOJOs_filtered);

        REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(overwritten_file_path_list, fileObjectType);
        return filePOJO;
    }

    public static FilePOJO ADD_TO_HASHMAP_FILE_POJO_ON_ADD_SEARCH_LIBRARY(String filePOJOHashmapKeyPath, final List<String> added_file_path_list, FileObjectType fileObjectType, List<String> overwritten_file_path_list)
    {
        FilePOJO filePOJO = null;
        List<FilePOJO> filePOJOs=Global.HASHMAP_FILE_POJO.get(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath);
        List<FilePOJO> filePOJOs_filtered=Global.HASHMAP_FILE_POJO_FILTERED.get(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath);
        if(filePOJOs==null)
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
                file_path=Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,name);
                filePOJO=MAKE_FilePOJO(fileObjectType,file_path);
            }
            filePOJOs.add(filePOJO);
            if(filePOJO.getAlfa()==Global.ENABLE_ALFA)
            {
                filePOJOs_filtered.add(filePOJO);
            }

        }
        Global.HASHMAP_FILE_POJO.put(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath,filePOJOs);
        Global.HASHMAP_FILE_POJO_FILTERED.put(FileObjectType.SEARCH_LIBRARY_TYPE+filePOJOHashmapKeyPath,filePOJOs_filtered);

        REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(overwritten_file_path_list, fileObjectType);
        return filePOJO;
    }


    public static void UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(String dest_folder, FileObjectType fileObjectType)
    {
        String parent_path_to_dest_folder=new File(dest_folder).getParent();
        List<FilePOJO> filePOJOs=Global.HASHMAP_FILE_POJO.get(fileObjectType+parent_path_to_dest_folder);
        List<FilePOJO> filePOJOs_filtered=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+parent_path_to_dest_folder);
        if(filePOJOs==null)
        {
            return;
        }
        String name=new File(dest_folder).getName();
        int i=remove_from_FilePOJO(name,filePOJOs);
        int j=remove_from_FilePOJO(name,filePOJOs_filtered);
        FilePOJO filePOJO=MAKE_FilePOJO(fileObjectType,dest_folder);
        filePOJOs.add(i,filePOJO);
        if(filePOJO.getAlfa()==Global.ENABLE_ALFA)
        {
            filePOJOs_filtered.add(j,filePOJO);
        }

        Global.HASHMAP_FILE_POJO.put(fileObjectType+parent_path_to_dest_folder,filePOJOs);
        Global.HASHMAP_FILE_POJO_FILTERED.put(fileObjectType+parent_path_to_dest_folder,filePOJOs_filtered);

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
        Iterator iterator=Global.HASHMAP_FILE_POJO.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry entry=(Map.Entry)iterator.next();
            if(Global.IS_CHILD_FILE((String) entry.getKey(),fileObjectType+file_path))
            {
                ((List<FilePOJO>)entry.getValue()).clear();
                iterator.remove();
            }
        }

        iterator=Global.HASHMAP_FILE_POJO_FILTERED.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry entry=(Map.Entry)iterator.next();
            if(Global.IS_CHILD_FILE((String) entry.getKey(),fileObjectType+file_path))
            {
                ((List<FilePOJO>)entry.getValue()).clear();
                iterator.remove();
            }
        }

    }

    public static void SET_PARENT_HASHMAP_FILE_POJO_SIZE_NULL(String file_path,FileObjectType fileObjectType)
    {
        String parent_file_path=new File(file_path).getParent();
        if(parent_file_path!=null)
        {
            for (Map.Entry<String, List<FilePOJO>> entry : Global.HASHMAP_FILE_POJO.entrySet()) {
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

        for (Map.Entry<String, List<FilePOJO>> entry : Global.HASHMAP_FILE_POJO.entrySet()) {
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


    public static boolean FILL_FILEPOJO(List<FilePOJO> filePOJOS, List<FilePOJO> filePOJOS_filtered, FileObjectType fileObjectType,
                                              String fileclickselected,UsbFile usbFile ,boolean archive_view)
    {

        filePOJOS.clear(); filePOJOS_filtered.clear();
        String other_permission_string = null;
        File file=new File(fileclickselected);
/*
        if(fileObjectType==FileObjectType.ROOT_TYPE)
        {
            String modified_other_permission_string=Global.GET_OTHER_FILE_PERMISSION(fileclickselected);
            Log.d("shankar","file_path - "+fileclickselected+"   existing other permission string - "+modified_other_permission_string+" permission to read - "+file.canRead());
            Global.SET_OTHER_FILE_PERMISSION("rwx",fileclickselected);
            SecurityManager securityManager=new SecurityManager();
            securityManager.checkRead(fileclickselected);

            file.setExecutable(true,false);
            file.setReadable(true,false);
            Log.d("shankar"," owner of file - "+file.canRead());

        }

 */
        if(fileObjectType==FileObjectType.FILE_TYPE || fileObjectType==FileObjectType.ROOT_TYPE)
        {
            File[] file_array;
            if((file_array=file.listFiles())!=null)
            {

                int size=file_array.length;
                for(int i=0;i<size;++i)
                {
                    File f=file_array[i];
                    FilePOJO filePOJO =MAKE_FilePOJO(f,true,archive_view,fileObjectType);
                    if(!filePOJO.getName().startsWith("."))
                    {

                        filePOJOS_filtered.add(filePOJO);
                    }

                    filePOJOS.add(filePOJO);

                }

            }
        }
        else if(fileObjectType==FileObjectType.USB_TYPE)
        {
            if(MainActivity.usbFileRoot==null)
            {
                return true;
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
                    return true;
                }
            }
        }
        else if(fileObjectType==FileObjectType.FTP_TYPE)
        {
            if(!Global.CHECK_FTP_SERVER_CONNECTED())
            {
                return true;
            }
            else
            {
                FTPFile[] file_array;
                try {

                    file_array=MainActivity.FTP_CLIENT.listFiles(fileclickselected);
                    int size=file_array.length;
                    for(int i=0;i<size;++i)
                    {
                        FTPFile f=file_array[i];
                        String name=f.getName();
                        String path=Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected,name);
                        FilePOJO filePOJO=MAKE_FilePOJO(f,false,false,fileObjectType,path);
                        filePOJOS_filtered.add(filePOJO);
                        filePOJOS.add(filePOJO);

                    }

                } catch (IOException e) {
                    return true;
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
            Log.d("shankar","file_path - "+fileclickselected+"    modified other permission - "+other_permission_string+" whether exists - "+new File(fileclickselected).exists());
            MAKE_FilePOJO_ROOT(fileclickselected);
        }

        if(other_permission_string!=null)
        {
            Global.SET_OTHER_FILE_PERMISSION(other_permission_string,fileclickselected);
        }

 */

        Global.HASHMAP_FILE_POJO.put(fileObjectType+fileclickselected,filePOJOS);
        Global.HASHMAP_FILE_POJO_FILTERED.put(fileObjectType+fileclickselected,filePOJOS_filtered);
        return true;
    }

}
