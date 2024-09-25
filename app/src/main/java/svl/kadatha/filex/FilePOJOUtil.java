package svl.kadatha.filex;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
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
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import me.jahnen.libaums.core.fs.UsbFile;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;
import timber.log.Timber;

public class FilePOJOUtil {
    private static final String TAG = "Ftp-FilePOJOUtil";
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
                filePOJO=MakeFilePOJOUtil.MAKE_FilePOJO(fileObjectType,file_path);
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
                filePOJO=MakeFilePOJOUtil.MAKE_FilePOJO(fileObjectType,file_path);
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
        FilePOJO filePOJO =MakeFilePOJOUtil.MAKE_FilePOJO(fileObjectType,dest_folder);
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
        File file=new File(fileclickselected);
        if(fileObjectType==FileObjectType.ROOT_TYPE)
        {
            if(RootUtils.canRunRootCommands()){
                String[] child_file_paths_array=RootUtils.listFilesInDirectory(fileclickselected);
                int size = child_file_paths_array.length;
                for (int i = 0; i < size; ++i){
                    String child_file_path = child_file_paths_array[i];
                    FilePOJO filePOJO =MakeFilePOJOUtil.MAKE_FilePOJO_ROOT(child_file_path,true,fileObjectType);
                    if(!filePOJO.getName().startsWith("."))
                    {
                        filePOJOS_filtered.add(filePOJO);
                    }
                    filePOJOS.add(filePOJO);
                }
            }

        }
        else if(fileObjectType==FileObjectType.FILE_TYPE)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                try(DirectoryStream<Path> directoryStream= Files.newDirectoryStream(Paths.get(fileclickselected)))
                {
                    if(archive_view)
                    {
                        for(Path path : directoryStream)
                        {
                            FilePOJO filePOJO=MakeFilePOJOUtil.MAKE_FilePOJO_ZIP(path,true,fileObjectType);
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
                            FilePOJO filePOJO=MakeFilePOJOUtil.MAKE_FilePOJO(path,true,fileObjectType);
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
        else if (fileObjectType == FileObjectType.FTP_TYPE) {
            Timber.tag(TAG).d("Filling FilePOJO for FTP directory: %s", fileclickselected);
            FTPFile[] file_array;
            FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
            FTPClient ftpClient=null;
            try {
                ftpClient = ftpClientRepository.getFtpClient();
                file_array = ftpClient.listFiles(fileclickselected);
                Timber.tag(TAG).d("Retrieved %d files from FTP directory", file_array.length);
                int size = file_array.length;
                for (int i = 0; i < size; ++i) {
                    FTPFile f = file_array[i];
                    String name = f.getName();
                    String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                    Timber.tag(TAG).d("Processing FTP file: %s", path);
                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, false, fileObjectType, path, ftpClient);
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
        else if(fileObjectType==FileObjectType.SFTP_TYPE){
            SftpChannelRepository sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            ChannelSftp channelSftp = null;
            try {
                channelSftp=sftpChannelRepository.getSftpChannel();
                Vector<ChannelSftp.LsEntry> lsEntries=channelSftp.ls(fileclickselected);
                for(ChannelSftp.LsEntry lsEntry : lsEntries){
                    String name=lsEntry.getFilename();
                    String path=Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected,name);
                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(lsEntry, false, fileObjectType,path,channelSftp);
                    filePOJOS_filtered.add(filePOJO);
                    filePOJOS.add(filePOJO);
                }
            } catch (Exception e) {
                return;
            }
            finally {
                if(sftpChannelRepository!=null && channelSftp!=null){
                    sftpChannelRepository.releaseChannel(channelSftp);
                    Timber.tag(TAG).d("SFTP channel released");
                }
            }
        }
        else
        {
            FileModel fileModel= FileModelFactory.getFileModel(fileclickselected,fileObjectType,null,null);
            FileModel[] fileModels=fileModel.list();
            int size = fileModels.length;
            for (int i = 0; i < size; ++i) {
                FileModel f = fileModels[i];
                String name = f.getName();
                String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                FileModel childFileModel = FileModelFactory.getFileModel(path, fileObjectType, null, null);
                Timber.tag(TAG).d("Processing FileModel file: %s", path);
                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(childFileModel,false,fileObjectType);
                filePOJOS_filtered.add(filePOJO);
                filePOJOS.add(filePOJO);
            }
        }

        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        repositoryClass.hashmap_file_pojo.put(fileObjectType+fileclickselected,filePOJOS);
        repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType+fileclickselected,filePOJOS_filtered);
    }

    public static void FILL_FILE_POJO1(List<FilePOJO> filePOJOS, List<FilePOJO> filePOJOS_filtered, FileObjectType fileObjectType,
                                      String fileclickselected, UsbFile usbFile , boolean archive_view)
    {
        filePOJOS.clear(); filePOJOS_filtered.clear();
        File file=new File(fileclickselected);

        if(fileObjectType==FileObjectType.ROOT_TYPE)
        {
            if(RootUtils.canRunRootCommands()){
                String[] child_file_paths_array=RootUtils.listFilesInDirectory(fileclickselected);
                int size = child_file_paths_array.length;
                for (int i = 0; i < size; ++i){
                    String child_file_path = child_file_paths_array[i];
                    FilePOJO filePOJO =MakeFilePOJOUtil.MAKE_FilePOJO_ROOT(child_file_path,true,fileObjectType);
                    if(!filePOJO.getName().startsWith("."))
                    {
                        filePOJOS_filtered.add(filePOJO);
                    }
                    filePOJOS.add(filePOJO);
                }
            }
        }
        else if(fileObjectType==FileObjectType.FILE_TYPE)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                try(DirectoryStream<Path> directoryStream= Files.newDirectoryStream(Paths.get(fileclickselected)))
                {
                    if(archive_view)
                    {
                        for(Path path : directoryStream)
                        {
                            FilePOJO filePOJO =MakeFilePOJOUtil.MAKE_FilePOJO_ZIP(path,true,fileObjectType);
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
                            FilePOJO filePOJO =MakeFilePOJOUtil.MAKE_FilePOJO(path,true,fileObjectType);
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
                        FilePOJO filePOJO=MakeFilePOJOUtil.MAKE_FilePOJO(f,true);
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
            FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
            FTPClient ftpClient=null;
            try {
                ftpClient = ftpClientRepository.getFtpClient();
                file_array = ftpClient.listFiles(fileclickselected);
                Timber.tag(TAG).d("Retrieved %d files from FTP directory", file_array.length);
                int size = file_array.length;
                for (int i = 0; i < size; ++i) {
                    FTPFile f = file_array[i];
                    String name = f.getName();
                    String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                    Timber.tag(TAG).d("Processing FTP file: %s", path);
                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, false, fileObjectType, path, ftpClient);
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
        else if(fileObjectType==FileObjectType.SFTP_TYPE){
            SftpChannelRepository sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            ChannelSftp channelSftp = null;
            try {
                channelSftp=sftpChannelRepository.getSftpChannel();
                Vector<ChannelSftp.LsEntry> lsEntries=channelSftp.ls(fileclickselected);
                for(ChannelSftp.LsEntry lsEntry : lsEntries){
                    String name=lsEntry.getFilename();
                    String path=Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected,name);
                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(lsEntry, false, fileObjectType,path,channelSftp);
                    filePOJOS_filtered.add(filePOJO);
                    filePOJOS.add(filePOJO);
                }
            } catch (Exception e) {
                return;
            }
            finally {
                if(sftpChannelRepository!=null && channelSftp!=null){
                    sftpChannelRepository.releaseChannel(channelSftp);
                    Timber.tag(TAG).d("SFTP channel released");
                }
            }
        }

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
                FilePOJO filePOJO =MakeFilePOJOUtil.MAKE_FilePOJO(f,true,fileObjectType);
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
                FilePOJO filePOJO =MakeFilePOJOUtil.MAKE_FilePOJO_ZIP(f,true,fileObjectType);
                if(!filePOJO.getName().startsWith("."))
                {
                    filePOJOS_filtered.add(filePOJO);
                }
                filePOJOS.add(filePOJO);
            }
        }
    }
}
