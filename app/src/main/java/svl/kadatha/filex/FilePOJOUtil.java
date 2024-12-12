package svl.kadatha.filex;

import android.os.Build;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;
import com.google.gson.Gson;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.jcraft.jsch.ChannelSftp;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import me.jahnen.libaums.core.fs.UsbFile;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import svl.kadatha.filex.audio.AudioPOJO;
import svl.kadatha.filex.cloud.CloudAccountViewModel;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;
import svl.kadatha.filex.filemodel.GoogleDriveFileModel;
import svl.kadatha.filex.network.FtpClientRepository;
import svl.kadatha.filex.network.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.network.SftpChannelRepository;
import svl.kadatha.filex.network.SmbClientRepository;
import svl.kadatha.filex.network.WebDavClientRepository;

public class FilePOJOUtil {
    private static final String TAG = "Ftp-FilePOJOUtil";

    public static void REMOVE_FROM_HASHMAP_FILE_POJO_ON_REMOVAL_SEARCH_LIBRARY(String filePOJOHashmapKeyPath, final List<String> deleted_files_path_list, FileObjectType fileObjectType) {
        final int size = deleted_files_path_list.size();
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOs = repositoryClass.hashmap_file_pojo.get(FileObjectType.SEARCH_LIBRARY_TYPE + filePOJOHashmapKeyPath);
        List<FilePOJO> filePOJOs_filtered = repositoryClass.hashmap_file_pojo_filtered.get(FileObjectType.SEARCH_LIBRARY_TYPE + filePOJOHashmapKeyPath);
        if (filePOJOs == null || filePOJOs_filtered == null) {
            return;
        }
        for (int i = 0; i < size; ++i) {
            String deleted_file_path = deleted_files_path_list.get(i);
            File f = new File(deleted_file_path);
            String deleted_file_name = f.getName();
            String parent_folder = f.getParent();

            remove_from_FilePOJO_comparing_file_path(deleted_file_path, filePOJOs);
            remove_from_FilePOJO_comparing_file_path(deleted_file_path, filePOJOs_filtered);

            REMOVE_FROM_HASHMAP_FILE_POJO(parent_folder, Collections.singletonList(deleted_file_name), fileObjectType);
            REMOVE_FROM_AUDIO_CACHE(fileObjectType, deleted_file_path);

        }
        repositoryClass.hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE + filePOJOHashmapKeyPath, filePOJOs);
        repositoryClass.hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE + filePOJOHashmapKeyPath, filePOJOs_filtered);
    }

    public static void REMOVE_FROM_HASHMAP_FILE_POJO(final String source_folder, final List<String> deleted_files_name_list, FileObjectType fileObjectType) {
        final int size = deleted_files_name_list.size();
        UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(source_folder, fileObjectType);
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOs = repositoryClass.hashmap_file_pojo.get(fileObjectType + source_folder);
        List<FilePOJO> filePOJOs_filtered = repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType + source_folder);
        if (filePOJOs == null || filePOJOs_filtered == null) {
            UsbFile currentUsbFile = null;
            if (fileObjectType == FileObjectType.USB_TYPE) {
                if (MainActivity.usbFileRoot != null) {
                    try {
                        currentUsbFile = MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(source_folder));

                    } catch (IOException e) {

                    }
                }
            }
            FILL_FILE_POJO(new ArrayList<>(), new ArrayList<>(), fileObjectType, source_folder, currentUsbFile, false);
            filePOJOs = repositoryClass.hashmap_file_pojo.get(fileObjectType + source_folder);
            filePOJOs_filtered = repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType + source_folder);
        }
        if (filePOJOs == null || filePOJOs_filtered == null) {
            return;
        }
        String name;
        for (int i = 0; i < size; ++i) {
            name = deleted_files_name_list.get(i);
            remove_from_FilePOJO(name, filePOJOs);
            remove_from_FilePOJO(name, filePOJOs_filtered);
            String file_to_be_removed = Global.CONCATENATE_PARENT_CHILD_PATH(source_folder, name);
            REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(Collections.singletonList(file_to_be_removed), fileObjectType);

            if (!REMOVE_FROM_LIBRARY_CACHE(fileObjectType, file_to_be_removed, "Download")) {
                if (!REMOVE_FROM_LIBRARY_CACHE(fileObjectType, file_to_be_removed, "Document")) {
                    if (!REMOVE_FROM_LIBRARY_CACHE(fileObjectType, file_to_be_removed, "Image")) {
                        if (!REMOVE_FROM_LIBRARY_CACHE(fileObjectType, file_to_be_removed, "Audio")) {
                            if (!REMOVE_FROM_LIBRARY_CACHE(fileObjectType, file_to_be_removed, "Video")) {
                                if (!REMOVE_FROM_LIBRARY_CACHE(fileObjectType, file_to_be_removed, "Archive")) {
                                    REMOVE_FROM_LIBRARY_CACHE(fileObjectType, file_to_be_removed, "APK");
                                }
                            }
                        }
                    }
                }
            }

            REMOVE_FROM_LIBRARY_CACHE(fileObjectType, file_to_be_removed, "Large Files");
            REMOVE_FROM_LIBRARY_CACHE(fileObjectType, file_to_be_removed, "Duplicate Files");
            REMOVE_FROM_AUDIO_CACHE(fileObjectType, file_to_be_removed);
        }
        repositoryClass.hashmap_file_pojo.put(fileObjectType + source_folder, filePOJOs);
        repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType + source_folder, filePOJOs_filtered);
    }


    private static FilePOJO remove_from_FilePOJO(String name, List<FilePOJO> list) {
        FilePOJO deleted_filePOJO = null;
        Iterator<FilePOJO> iterator = list.iterator();
        while (iterator.hasNext()) {
            FilePOJO filePOJO = iterator.next();
            if (filePOJO.getName().equals(name)) {
                deleted_filePOJO = filePOJO;
                iterator.remove();
                return deleted_filePOJO;
            }
        }
        return deleted_filePOJO;
    }

    private static void remove_from_FilePOJO_comparing_file_path(String file_path, List<FilePOJO> list) {
        Iterator<FilePOJO> iterator = list.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getPath().equals(file_path)) {
                iterator.remove();
                break;
            }
        }
    }

    private static boolean REMOVE_FROM_LIBRARY_CACHE(FileObjectType fileObjectType, String file_path, String media_category) {
        if (fileObjectType != FileObjectType.FILE_TYPE) {
            return false;
        }
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOs = repositoryClass.hashmap_file_pojo.get(FileObjectType.SEARCH_LIBRARY_TYPE + media_category);
        List<FilePOJO> filePOJOs_filtered = repositoryClass.hashmap_file_pojo_filtered.get(FileObjectType.SEARCH_LIBRARY_TYPE + media_category);
        if (filePOJOs != null) {
            Iterator<FilePOJO> iterator = filePOJOs.iterator();
            while (iterator.hasNext()) {
                FilePOJO filePOJO = iterator.next();
                if (file_path.equals(filePOJO.getPath())) {
                    if (filePOJOs_filtered != null) {
                        filePOJOs_filtered.remove(filePOJO);
                    }
                    iterator.remove();
                    return true;
                }
            }
        }
        return false;
    }

    private static void REMOVE_FROM_AUDIO_CACHE(FileObjectType fileObjectType, String file_path_to_be_removed) {
        if (fileObjectType != FileObjectType.FILE_TYPE) {
            return;
        }
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        List<AudioPOJO> audioPOJOS = repositoryClass.audio_pojo_hashmap.get("audio");
        if (audioPOJOS != null) {
            Iterator<AudioPOJO> iterator = audioPOJOS.iterator();
            while (iterator.hasNext()) {
                AudioPOJO audioPOJO = iterator.next();
                if (audioPOJO.getData().equals(file_path_to_be_removed)) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

    public static FilePOJO ADD_TO_HASHMAP_FILE_POJO(final String dest_folder, final List<String> added_file_name_list, FileObjectType fileObjectType, List<String> overwritten_file_path_list) {
        FilePOJO filePOJO = null;
        int size;
        UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(dest_folder, fileObjectType);
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOs = repositoryClass.hashmap_file_pojo.get(fileObjectType + dest_folder);
        List<FilePOJO> filePOJOs_filtered = repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType + dest_folder);
        if (filePOJOs == null) {
            UsbFile currentUsbFile = null;
            if (fileObjectType == FileObjectType.USB_TYPE) {
                if (MainActivity.usbFileRoot != null) {
                    try {
                        currentUsbFile = MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(dest_folder));
                    } catch (IOException e) {

                    }
                }
            }
            FILL_FILE_POJO(new ArrayList<>(), new ArrayList<>(), fileObjectType, dest_folder, currentUsbFile, false);
            filePOJOs = repositoryClass.hashmap_file_pojo.get(fileObjectType + dest_folder);
            filePOJOs_filtered = repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType + dest_folder);
            if (filePOJOs == null) {
                return null;
            }
        } else {
            if (overwritten_file_path_list != null) // while creating new file, overwritten_file_path_list is null
            {
                size = overwritten_file_path_list.size();
                for (int i = 0; i < size; ++i) {
                    String overwritten_file_path = overwritten_file_path_list.get(i);
                    String overwritten_file_name = new File(overwritten_file_path).getName();
                    remove_from_FilePOJO(overwritten_file_name, filePOJOs);
                    remove_from_FilePOJO(overwritten_file_name, filePOJOs_filtered);
                }
            }

            size = added_file_name_list.size();
            String file_path;
            for (int i = 0; i < size; ++i) {
                file_path = Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder, added_file_name_list.get(i));
                filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(fileObjectType, file_path);
                if (filePOJO != null) {
                    filePOJOs.add(filePOJO);
                    if (filePOJO.getAlfa() == Global.ENABLE_ALFA) {
                        filePOJOs_filtered.add(filePOJO);
                    }
                }
            }
            repositoryClass.hashmap_file_pojo.put(fileObjectType + dest_folder, filePOJOs);
            repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType + dest_folder, filePOJOs_filtered);
        }

        REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(overwritten_file_path_list, fileObjectType);
        return filePOJO;
    }

    public static FilePOJO ADD_TO_HASHMAP_FILE_POJO_ON_ADD_SEARCH_LIBRARY(String filePOJOHashmapKeyPath, final List<String> added_file_path_list, FileObjectType fileObjectType, List<String> overwritten_file_path_list) {
        FilePOJO filePOJO = null;
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOs = repositoryClass.hashmap_file_pojo.get(FileObjectType.SEARCH_LIBRARY_TYPE + filePOJOHashmapKeyPath);
        List<FilePOJO> filePOJOs_filtered = repositoryClass.hashmap_file_pojo_filtered.get(FileObjectType.SEARCH_LIBRARY_TYPE + filePOJOHashmapKeyPath);
        if (filePOJOs == null || filePOJOs_filtered == null) {
            return null;
        }

        int size;
        if (overwritten_file_path_list != null) // while creating new file, overwritten_file_path_list is null
        {
            size = overwritten_file_path_list.size();
            for (int i = 0; i < size; ++i) {
                String overwritten_file_path = overwritten_file_path_list.get(i);
                remove_from_FilePOJO_comparing_file_path(overwritten_file_path, filePOJOs);
                remove_from_FilePOJO_comparing_file_path(overwritten_file_path, filePOJOs_filtered);
            }
        }

        size = added_file_path_list.size();
        for (int i = 0; i < size; ++i) {
            String file_path = added_file_path_list.get(i);
            File f = new File(file_path);
            String name = f.getName();
            String parent_file_path = f.getParent();
            List<String> file_name_list = Collections.singletonList(name);
            FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(parent_file_path, file_name_list, fileObjectType);
            filePOJO = ADD_TO_HASHMAP_FILE_POJO(parent_file_path, file_name_list, fileObjectType, overwritten_file_path_list); //single file is added, the last file pojo returned is the only filepojo
            if (filePOJO == null) {
                filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(fileObjectType, file_path);
            }
            if (filePOJO != null) {
                filePOJOs.add(filePOJO);
                if (filePOJO.getAlfa() == Global.ENABLE_ALFA) {
                    filePOJOs_filtered.add(filePOJO);
                }
            }
        }
        repositoryClass.hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE + filePOJOHashmapKeyPath, filePOJOs);
        repositoryClass.hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE + filePOJOHashmapKeyPath, filePOJOs_filtered);

        REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(overwritten_file_path_list, fileObjectType);
        return filePOJO;
    }


    public static void UPDATE_PARENT_FOLDER_HASHMAP_FILE_POJO(String dest_folder, FileObjectType fileObjectType) {
        String parent_path_to_dest_folder = new File(dest_folder).getParent();
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOs = repositoryClass.hashmap_file_pojo.get(fileObjectType + parent_path_to_dest_folder);
        List<FilePOJO> filePOJOs_filtered = repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType + parent_path_to_dest_folder);
        if (filePOJOs == null || filePOJOs_filtered == null) {
            return;
        }
        String name = new File(dest_folder).getName();
        FilePOJO removed_filePOJO = remove_from_FilePOJO(name, filePOJOs);
        remove_from_FilePOJO(name, filePOJOs_filtered);
        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(fileObjectType, dest_folder);
        if (filePOJO == null) {
            filePOJO = removed_filePOJO;
        }
        if (filePOJO != null) {
            filePOJOs.add(filePOJO);
            if (filePOJO.getAlfa() == Global.ENABLE_ALFA) {
                filePOJOs_filtered.add(filePOJO);
            }
        }
        repositoryClass.hashmap_file_pojo.put(fileObjectType + parent_path_to_dest_folder, filePOJOs);
        repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType + parent_path_to_dest_folder, filePOJOs_filtered);
    }

    public static void REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL(List<String> file_path_list, FileObjectType fileObjectType) {
        if (file_path_list == null) {
            return;
        }
        int size = file_path_list.size();
        for (int i = 0; i < size; ++i) {
            String file_path = file_path_list.get(i);
            REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL__(file_path, fileObjectType);
        }
        if (size > 0) {
            SET_PARENT_HASHMAP_FILE_POJO_SIZE_NULL(file_path_list.get(0), fileObjectType);
        }
    }

    private static void REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL__(String file_path, FileObjectType fileObjectType) {
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        Iterator<Map.Entry<String, List<FilePOJO>>> iterator = repositoryClass.hashmap_file_pojo.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<FilePOJO>> entry = iterator.next();
            if (Global.IS_CHILD_FILE(entry.getKey(), fileObjectType + file_path)) {
                iterator.remove();
            }
        }

        iterator = repositoryClass.hashmap_file_pojo_filtered.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<FilePOJO>> entry = iterator.next();
            if (Global.IS_CHILD_FILE(entry.getKey(), fileObjectType + file_path)) {
                iterator.remove();
            }
        }
        repositoryClass.hashmap_internal_directory_size.remove(file_path);
        repositoryClass.hashmap_external_directory_size.remove(file_path);
    }

    public static void SET_PARENT_HASHMAP_FILE_POJO_SIZE_NULL(String file_path, FileObjectType fileObjectType) {
        String parent_file_path = new File(file_path).getParent();
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        if (parent_file_path != null) {
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

    public static void SET_HASHMAP_FILE_POJO_SIZE_NULL(String file_path, FileObjectType fileObjectType) {
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
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
                                      String fileclickselected, UsbFile usbFile, boolean archive_view) {
        filePOJOS.clear();
        filePOJOS_filtered.clear();
        File file = new File(fileclickselected);
        if (fileObjectType == FileObjectType.ROOT_TYPE) {
            if (RootUtils.canRunRootCommands()) {
                String[] child_file_paths_array = RootUtils.listFilesInDirectory(fileclickselected);
                int size = child_file_paths_array.length;
                for (int i = 0; i < size; ++i) {
                    String child_file_path = child_file_paths_array[i];
                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO_ROOT(child_file_path, true, fileObjectType);
                    if (!filePOJO.getName().startsWith(".")) {
                        filePOJOS_filtered.add(filePOJO);
                    }
                    filePOJOS.add(filePOJO);
                }
            }
        } else if (fileObjectType == FileObjectType.FILE_TYPE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(fileclickselected))) {
                    if (archive_view) {
                        for (Path path : directoryStream) {
                            FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO_ZIP(path, true, fileObjectType);
                            if (!filePOJO.getName().startsWith(".")) {
                                filePOJOS_filtered.add(filePOJO);
                            }
                            filePOJOS.add(filePOJO);
                        }
                    } else {
                        for (Path path : directoryStream) {
                            FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(path, true, fileObjectType);
                            if (!filePOJO.getName().startsWith(".")) {
                                filePOJOS_filtered.add(filePOJO);
                            }
                            filePOJOS.add(filePOJO);
                        }
                    }
                } catch (IOException e) {

                }
            } else {
                if (archive_view) {
                    file_type_fill_filePOJO_zip(file, fileObjectType, filePOJOS, filePOJOS_filtered);
                } else {
                    file_type_fill_filePOJO(file, fileObjectType, filePOJOS, filePOJOS_filtered);
                }
            }
        } else if (fileObjectType == FileObjectType.FTP_TYPE) {
            FTPFile[] file_array;
            FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
            FTPClient ftpClient = null;
            try {
                ftpClient = ftpClientRepository.getFtpClient();
                file_array = ftpClient.listFiles(fileclickselected);
                int size = file_array.length;
                for (int i = 0; i < size; ++i) {
                    FTPFile f = file_array[i];
                    String name = f.getName();
                    String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, false, fileObjectType, path, ftpClient);
                    filePOJOS_filtered.add(filePOJO);
                    filePOJOS.add(filePOJO);
                }
            } catch (Exception e) {
                return;
            } finally {
                if (ftpClientRepository != null && ftpClient != null) {
                    ftpClientRepository.releaseFtpClient(ftpClient);
                }
            }
        } else if (fileObjectType == FileObjectType.SFTP_TYPE) {
            SftpChannelRepository sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            ChannelSftp channelSftp = null;
            try {
                channelSftp = sftpChannelRepository.getSftpChannel();
                Vector<ChannelSftp.LsEntry> lsEntries = channelSftp.ls(fileclickselected);
                for (ChannelSftp.LsEntry lsEntry : lsEntries) {
                    String name = lsEntry.getFilename();
                    String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(lsEntry, false, fileObjectType, path, channelSftp);
                    filePOJOS_filtered.add(filePOJO);
                    filePOJOS.add(filePOJO);
                }
            } catch (Exception e) {
                return;
            } finally {
                if (sftpChannelRepository != null && channelSftp != null) {
                    sftpChannelRepository.releaseChannel(channelSftp);
                }
            }
        } else if (fileObjectType == FileObjectType.WEBDAV_TYPE) {
            WebDavClientRepository webDavClientRepository;
            Sardine sardine;
            try {
                webDavClientRepository = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
                sardine = webDavClientRepository.getSardine();
                String basePath = webDavClientRepository.getBasePath(sardine);
                String fullPath = basePath + (fileclickselected.startsWith("/") ? fileclickselected : "/" + fileclickselected);
                String url = webDavClientRepository.buildUrl(fullPath);
                List<DavResource> resources = sardine.list(url);
                if (!resources.isEmpty()) {
                    resources.remove(0);// Safely remove the first element
                }
                for (DavResource resource : resources) {
                    String name = resource.getName();
                    String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);

                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(resource, false, fileObjectType, path, sardine);
                    filePOJOS_filtered.add(filePOJO);
                    filePOJOS.add(filePOJO);
                }
            } catch (IOException e) {
                return;
            }
        } else if (fileObjectType == FileObjectType.SMB_TYPE) {
            SmbClientRepository smbClientRepository = null;
            Session session = null;
            String shareName;
            try {
                smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
                session = smbClientRepository.getSession();
                shareName = smbClientRepository.getShareName();
                try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                    // Adjust fileclickselected to remove leading "/" if necessary
                    String adjustedPath = fileclickselected.startsWith("/") ? fileclickselected.substring(1) : fileclickselected;

                    // List files in the directory
                    List<FileIdBothDirectoryInformation> fileList = share.list(adjustedPath);

                    for (FileIdBothDirectoryInformation info : fileList) {
                        String name = info.getFileName();
                        // Exclude "." and ".."
                        if (name.equals(".") || name.equals("..")) {
                            continue;
                        }
                        String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);

                        // Create SmbFileInfo
                        MakeFilePOJOUtil.SmbFileInfo smbFileInfo = new MakeFilePOJOUtil.SmbFileInfo(name, path, info.getFileAttributes(), info.getEndOfFile(), info.getCreationTime().toEpochMillis(), info.getLastAccessTime().toEpochMillis(), info.getLastWriteTime().toEpochMillis(), info.getChangeTime().toEpochMillis());

                        // Create FilePOJO
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(smbFileInfo, false, fileObjectType);
                        filePOJOS_filtered.add(filePOJO);
                        filePOJOS.add(filePOJO);
                    }
                }
            } catch (Exception e) {
                return;
            } finally {
                if (smbClientRepository != null && session != null) {
                    smbClientRepository.releaseSession(session);
                }
            }
        } else if (fileObjectType == FileObjectType.GOOGLE_DRIVE_TYPE) {
            try {
                // Obtain OAuth token and possibly a helper class for Google Drive operations
                String oauthToken = CloudAccountViewModel.GOOGLE_DRIVE_ACCESS_TOKEN;

                // Determine the parent folder ID. If fileclickselected represents root, use 'root'.
                String parentId = getFileIdByPath(fileclickselected,oauthToken);
                // If fileclickselected is always root or a known folder ID, adjust accordingly.
                // If you have a method: listFilesInFolder(String folderId, String oauthToken)
                List<GoogleDriveFileModel.GoogleDriveFileMetadata> driveFiles = GoogleDriveFileModel.listFilesInFolder(parentId, oauthToken);

                for (GoogleDriveFileModel.GoogleDriveFileMetadata meta : driveFiles) {
                    String name = meta.name;
                    // Construct a path. If fileclickselected is "/", path might be "/filename"
                    String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                    // Make a FilePOJO from the Google Drive metadata
                    FilePOJO filePOJO = MakeCloudFilePOJOUtil.MAKE_FilePOJO_FromDriveAPI(path, false, fileObjectType, oauthToken);
                    if (!filePOJO.getName().startsWith(".")) {
                        filePOJOS_filtered.add(filePOJO);
                    }
                    filePOJOS.add(filePOJO);
                }
            } catch (Exception e) {
                return;
            }
        } else if (fileObjectType == FileObjectType.DROP_BOX_TYPE) {
            try {
                String accessToken = CloudAccountViewModel.DROP_BOX_ACCESS_TOKEN;
                // Get Dropbox client
                DbxClientV2 dbxClient = new DbxClientV2(new com.dropbox.core.DbxRequestConfig("YourAppName"), accessToken);

                // For Dropbox, if fileclickselected is "/", we can try listing "" (empty string) to represent root
                String dropboxPath = fileclickselected.equals("/") ? "" : fileclickselected;
                List<Metadata> entries = dbxClient.files().listFolder(dropboxPath).getEntries();

                for (Metadata meta : entries) {
                    String name = meta.getName();
                    String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                    // Convert the Dropbox Metadata to FilePOJO
                    FilePOJO filePOJO = MakeCloudFilePOJOUtil.MAKE_FilePOJO(meta, false, fileObjectType, path, dbxClient);
                    if (!filePOJO.getName().startsWith(".")) {
                        filePOJOS_filtered.add(filePOJO);
                    }
                    filePOJOS.add(filePOJO);
                }
            } catch (Exception e) {
                return;
            }
        } else {
            FileModel fileModel = FileModelFactory.getFileModel(fileclickselected, fileObjectType, null, null);
            FileModel[] fileModels = fileModel.list();
            int size = fileModels.length;
            for (int i = 0; i < size; ++i) {
                FileModel f = fileModels[i];
                String name = f.getName();
                String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                FileModel childFileModel = FileModelFactory.getFileModel(path, fileObjectType, null, null);
                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(childFileModel, false, fileObjectType);
                filePOJOS_filtered.add(filePOJO);
                filePOJOS.add(filePOJO);
            }
        }

        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        repositoryClass.hashmap_file_pojo.put(fileObjectType + fileclickselected, filePOJOS);
        repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType + fileclickselected, filePOJOS_filtered);
    }

    public static void FILL_FILE_POJO1(List<FilePOJO> filePOJOS, List<FilePOJO> filePOJOS_filtered, FileObjectType fileObjectType,
                                       String fileclickselected, UsbFile usbFile, boolean archive_view) {
        filePOJOS.clear();
        filePOJOS_filtered.clear();
        File file = new File(fileclickselected);

        if (fileObjectType == FileObjectType.ROOT_TYPE) {
            if (RootUtils.canRunRootCommands()) {
                String[] child_file_paths_array = RootUtils.listFilesInDirectory(fileclickselected);
                int size = child_file_paths_array.length;
                for (int i = 0; i < size; ++i) {
                    String child_file_path = child_file_paths_array[i];
                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO_ROOT(child_file_path, true, fileObjectType);
                    if (!filePOJO.getName().startsWith(".")) {
                        filePOJOS_filtered.add(filePOJO);
                    }
                    filePOJOS.add(filePOJO);
                }
            }
        } else if (fileObjectType == FileObjectType.FILE_TYPE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(fileclickselected))) {
                    if (archive_view) {
                        for (Path path : directoryStream) {
                            FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO_ZIP(path, true, fileObjectType);
                            if (!filePOJO.getName().startsWith(".")) {
                                filePOJOS_filtered.add(filePOJO);
                            }
                            filePOJOS.add(filePOJO);

                        }
                    } else {
                        for (Path path : directoryStream) {
                            FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(path, true, fileObjectType);
                            if (!filePOJO.getName().startsWith(".")) {
                                filePOJOS_filtered.add(filePOJO);
                            }
                            filePOJOS.add(filePOJO);
                        }
                    }
                } catch (IOException e) {

                }

            } else {
                if (archive_view) {
                    file_type_fill_filePOJO_zip(file, fileObjectType, filePOJOS, filePOJOS_filtered);
                } else {
                    file_type_fill_filePOJO(file, fileObjectType, filePOJOS, filePOJOS_filtered);
                }
            }
        } else if (fileObjectType == FileObjectType.USB_TYPE) {
            if (MainActivity.usbFileRoot == null) {
                return;
            } else {
                UsbFile[] file_array;
                try {
                    if (usbFile == null) {
                        usbFile = MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(fileclickselected));
                    }
                    file_array = usbFile.listFiles();
                    int size = file_array.length;
                    for (int i = 0; i < size; ++i) {
                        UsbFile f = file_array[i];
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, true);
                        filePOJOS_filtered.add(filePOJO);
                        filePOJOS.add(filePOJO);
                    }

                } catch (IOException e) {
                    MainActivity.usbFileRoot = null;
                    return;
                }
            }
        } else if (fileObjectType == FileObjectType.FTP_TYPE) {
            FTPFile[] file_array;
            FtpClientRepository ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
            FTPClient ftpClient = null;
            try {
                ftpClient = ftpClientRepository.getFtpClient();
                file_array = ftpClient.listFiles(fileclickselected);
                int size = file_array.length;
                for (int i = 0; i < size; ++i) {
                    FTPFile f = file_array[i];
                    String name = f.getName();
                    String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, false, fileObjectType, path, ftpClient);
                    filePOJOS_filtered.add(filePOJO);
                    filePOJOS.add(filePOJO);
                }
            } catch (Exception e) {
                return;
            } finally {
                if (ftpClientRepository != null && ftpClient != null) {
                    ftpClientRepository.releaseFtpClient(ftpClient);
                }
            }
        } else if (fileObjectType == FileObjectType.SFTP_TYPE) {
            SftpChannelRepository sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
            ChannelSftp channelSftp = null;
            try {
                channelSftp = sftpChannelRepository.getSftpChannel();
                Vector<ChannelSftp.LsEntry> lsEntries = channelSftp.ls(fileclickselected);
                for (ChannelSftp.LsEntry lsEntry : lsEntries) {
                    String name = lsEntry.getFilename();
                    String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(lsEntry, false, fileObjectType, path, channelSftp);
                    filePOJOS_filtered.add(filePOJO);
                    filePOJOS.add(filePOJO);
                }
            } catch (Exception e) {
                return;
            } finally {
                if (sftpChannelRepository != null && channelSftp != null) {
                    sftpChannelRepository.releaseChannel(channelSftp);
                }
            }
        } else if (fileObjectType == FileObjectType.WEBDAV_TYPE) {
            WebDavClientRepository webDavClientRepository = null;
            Sardine sardine;
            try {
                webDavClientRepository = WebDavClientRepository.getInstance(NetworkAccountDetailsViewModel.WEBDAV_NETWORK_ACCOUNT_POJO);
                sardine = webDavClientRepository.getSardine();
                String basePath = webDavClientRepository.getBasePath(sardine);
                String fullPath = basePath + (fileclickselected.startsWith("/") ? fileclickselected : "/" + fileclickselected);
                String url = webDavClientRepository.buildUrl(fullPath);
                List<DavResource> resources = sardine.list(url);
                if (!resources.isEmpty()) {
                    resources.remove(0);// Safely remove the first element
                }
                for (DavResource resource : resources) {
                    String name = resource.getName();
                    String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);

                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(resource, false, fileObjectType, path, sardine);
                    filePOJOS_filtered.add(filePOJO);
                    filePOJOS.add(filePOJO);
                }
            } catch (IOException e) {
                return;
            }
        } else if (fileObjectType == FileObjectType.SMB_TYPE) {
            SmbClientRepository smbClientRepository = null;
            Session session = null;
            String shareName = null;
            try {
                smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
                session = smbClientRepository.getSession();
                shareName = smbClientRepository.getShareName();
                try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                    // Adjust fileclickselected to remove leading "/" if necessary
                    String adjustedPath = fileclickselected.startsWith("/") ? fileclickselected.substring(1) : fileclickselected;

                    // List files in the directory
                    List<FileIdBothDirectoryInformation> fileList = share.list(adjustedPath);

                    for (FileIdBothDirectoryInformation info : fileList) {
                        String name = info.getFileName();
                        // Exclude "." and ".."
                        if (name.equals(".") || name.equals("..")) {
                            continue;
                        }
                        String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);

                        // Create SmbFileInfo
                        MakeFilePOJOUtil.SmbFileInfo smbFileInfo = new MakeFilePOJOUtil.SmbFileInfo(name, path, info.getFileAttributes(), info.getEndOfFile(), info.getCreationTime().toEpochMillis(), info.getLastAccessTime().toEpochMillis(), info.getLastWriteTime().toEpochMillis(), info.getChangeTime().toEpochMillis());

                        // Create FilePOJO
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(smbFileInfo, false, fileObjectType);

                        filePOJOS_filtered.add(filePOJO);
                        filePOJOS.add(filePOJO);
                    }
                }
            } catch (Exception e) {
                return;
            } finally {
                if (smbClientRepository != null && session != null) {
                    smbClientRepository.releaseSession(session);
                }
            }
        }

        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        repositoryClass.hashmap_file_pojo.put(fileObjectType + fileclickselected, filePOJOS);
        repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType + fileclickselected, filePOJOS_filtered);
    }


    private static void file_type_fill_filePOJO(File file, FileObjectType fileObjectType, List<FilePOJO> filePOJOS, List<FilePOJO> filePOJOS_filtered) {
        File[] file_array;
        if ((file_array = file.listFiles()) != null) {
            int size = file_array.length;
            for (int i = 0; i < size; ++i) {
                File f = file_array[i];
                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, true, fileObjectType);
                if (!filePOJO.getName().startsWith(".")) {
                    filePOJOS_filtered.add(filePOJO);
                }
                filePOJOS.add(filePOJO);
            }
        }
    }

    private static void file_type_fill_filePOJO_zip(File file, FileObjectType fileObjectType, List<FilePOJO> filePOJOS, List<FilePOJO> filePOJOS_filtered) {
        File[] file_array;
        if ((file_array = file.listFiles()) != null) {
            int size = file_array.length;
            for (int i = 0; i < size; ++i) {
                File f = file_array[i];
                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO_ZIP(f, true, fileObjectType);
                if (!filePOJO.getName().startsWith(".")) {
                    filePOJOS_filtered.add(filePOJO);
                }
                filePOJOS.add(filePOJO);
            }
        }
    }


    public static String getFileIdByPath(String file_path, String oauthToken) throws IOException {
        // Create the OkHttpClient and Gson instances here
        OkHttpClient httpClient = new OkHttpClient();
        Gson gson = new Gson();

        // Normalize path
        if (!file_path.startsWith("/")) {
            file_path = "/" + file_path;
        }

        // If the path is just root "/"
        if (file_path.equals("/")) {
            return "root";
        }

        String[] parts = file_path.split("/");
        String currentFolderId = "root"; // Start from root

        for (int i = 1; i < parts.length; i++) {
            String name = parts[i].trim();
            if (name.isEmpty()) {
                // Skip empty components (e.g. if path ends with "/")
                continue;
            }

            // Escape single quotes in the name
            String escapedName = name.replace("'", "\\'");

            // Search for a child with the given name under the current folder
            String query = "name = '" + escapedName + "' and '" + currentFolderId + "' in parents and trashed = false";
            HttpUrl url = HttpUrl.parse("https://www.googleapis.com/drive/v3/files")
                    .newBuilder()
                    .addQueryParameter("q", query)
                    .addQueryParameter("fields", "files(id, name)")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + oauthToken)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to retrieve file ID: " + response.code() + " - " + response.message());
                }

                String responseBody = response.body().string();
                GoogleDriveFileModel.DriveFilesListResponse filesListResponse = gson.fromJson(responseBody, GoogleDriveFileModel.DriveFilesListResponse.class);

                if (filesListResponse.files != null && !filesListResponse.files.isEmpty()) {
                    // Take the first matching file/folder
                    currentFolderId = filesListResponse.files.get(0).id;
                } else {
                    // Item not found
                    return null;
                }
            }
        }

        return currentFolderId;
    }


}
