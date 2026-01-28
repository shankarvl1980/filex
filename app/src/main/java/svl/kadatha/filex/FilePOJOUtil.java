package svl.kadatha.filex;

import android.os.Build;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.google.gson.Gson;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
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
import svl.kadatha.filex.cloud.CloudAuthActivityViewModel;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;
import svl.kadatha.filex.filemodel.GoogleDriveFileModel;
import svl.kadatha.filex.network.FtpClientRepository;
import svl.kadatha.filex.network.NetworkAccountDetailsViewModel;
import svl.kadatha.filex.network.SftpChannelRepository;
import svl.kadatha.filex.network.SmbClientRepository;
import svl.kadatha.filex.network.WebDavClientRepository;
import svl.kadatha.filex.usb.ReadAccess;
import svl.kadatha.filex.usb.UsbFileRootSingleton;
import timber.log.Timber;

public class FilePOJOUtil {
    private static final Object file_pojo_lock = new Object();
    private static final Object audio_pojo_lock = new Object();

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
                try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
                    UsbFile usbFileRoot = access.getUsbFile();
                    try {
                        currentUsbFile = usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(source_folder));

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
        synchronized (file_pojo_lock) {
            Iterator<FilePOJO> iterator = list.iterator();
            while (iterator.hasNext()) {
                FilePOJO filePOJO = iterator.next();
                if (filePOJO.getName().equals(name)) {
                    deleted_filePOJO = filePOJO;
                    iterator.remove();
                    return deleted_filePOJO;
                }
            }
        }
        return deleted_filePOJO;
    }

    private static void remove_from_FilePOJO_comparing_file_path(String file_path, List<FilePOJO> list) {
        synchronized (file_pojo_lock) {
            Iterator<FilePOJO> iterator = list.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getPath().equals(file_path)) {
                    iterator.remove();
                    break;
                }
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
        synchronized (file_pojo_lock) {
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
        }
        return false;
    }

    private static void REMOVE_FROM_AUDIO_CACHE(FileObjectType fileObjectType, String file_path_to_be_removed) {
        if (fileObjectType != FileObjectType.FILE_TYPE) {
            return;
        }
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        List<AudioPOJO> audioPOJOS = repositoryClass.audio_pojo_hashmap.get("audio");
        synchronized (audio_pojo_lock) {
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
                try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
                    UsbFile usbFileRoot = access.getUsbFile();
                    try {
                        currentUsbFile = usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(dest_folder));
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
                synchronized (file_pojo_lock) {
                    if (filePOJO != null) {
                        filePOJOs.add(filePOJO);
                        if (filePOJO.getAlfa() == Global.ENABLE_ALFA) {
                            filePOJOs_filtered.add(filePOJO);
                        }
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
            synchronized (file_pojo_lock) {
                if (filePOJO != null) {
                    filePOJOs.add(filePOJO);
                    if (filePOJO.getAlfa() == Global.ENABLE_ALFA) {
                        filePOJOs_filtered.add(filePOJO);
                    }
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
        synchronized (file_pojo_lock) {
            if (filePOJO != null) {
                filePOJOs.add(filePOJO);
                if (filePOJO.getAlfa() == Global.ENABLE_ALFA) {
                    filePOJOs_filtered.add(filePOJO);
                }
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
            Timber.tag(Global.TAG).d("parent method " + fileObjectType + file_path);
            REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL__(file_path, fileObjectType);
        }
        if (size > 0) {
            SET_PARENT_HASHMAP_FILE_POJO_SIZE_NULL(file_path_list.get(0), fileObjectType);
        }
    }

    private static void REMOVE_CHILD_HASHMAP_FILE_POJO_ON_REMOVAL__(String file_path, FileObjectType fileObjectType) {
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        Iterator<Map.Entry<String, List<FilePOJO>>> iterator = repositoryClass.hashmap_file_pojo.entrySet().iterator();
        synchronized (file_pojo_lock) {
            while (iterator.hasNext()) {
                Map.Entry<String, List<FilePOJO>> entry = iterator.next();
                if (Global.IS_CHILD_FILE(entry.getKey(), fileObjectType + file_path)) {
                    iterator.remove();
                }
            }
        }

        iterator = repositoryClass.hashmap_file_pojo_filtered.entrySet().iterator();
        synchronized (file_pojo_lock) {
            while (iterator.hasNext()) {
                Map.Entry<String, List<FilePOJO>> entry = iterator.next();
                Timber.tag(Global.TAG).d("sub method " + entry.getKey());
                if (Global.IS_CHILD_FILE(entry.getKey(), fileObjectType + file_path)) {
                    iterator.remove();
                }
            }
        }

        repositoryClass.hashmap_internal_directory_size.remove(file_path);
        repositoryClass.hashmap_external_directory_size.remove(file_path);
    }

    public static FilePOJO GET_FILE_POJO(String file_path, FileObjectType fileObjectType) {
        String parent_path = Global.getParentPath(file_path);
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        List<FilePOJO> filePOJOList = repositoryClass.hashmap_file_pojo.get(fileObjectType + parent_path);
        if (filePOJOList == null) return null;
        Iterator<FilePOJO> iterator = filePOJOList.iterator();
        FilePOJO filePOJO;
        while (iterator.hasNext()) {
            filePOJO = iterator.next();
            if (filePOJO.getPath().equals(file_path)) {
                return filePOJO;
            }
        }
        return null;
    }

    public static void SET_PARENT_HASHMAP_FILE_POJO_SIZE_NULL(String file_path, FileObjectType fileObjectType) {
        String parent_file_path = new File(file_path).getParent();
        RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
        if (parent_file_path != null) {
            synchronized (file_pojo_lock) {
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

        try {
            if (fileObjectType == FileObjectType.ROOT_TYPE) {
                try {
                    if (RootUtils.canRunRootCommands()) {
                        String[] child_file_paths_array = RootUtils.listFilesInDirectory(fileclickselected);
                        if (child_file_paths_array != null) {
                            int size = child_file_paths_array.length;
                            for (int i = 0; i < size; ++i) {
                                String child_file_path = child_file_paths_array[i];
                                try {
                                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO_ROOT(child_file_path, true, fileObjectType);
                                    if (!filePOJO.getName().startsWith(".")) {
                                        filePOJOS_filtered.add(filePOJO);
                                    }
                                    filePOJOS.add(filePOJO);
                                } catch (Exception itemEx) {
                                    Timber.tag(Global.TAG).w(itemEx, "ROOT item skipped: %s", child_file_path);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Timber.tag(Global.TAG).w(e, "ROOT_TYPE listing failed: %s", fileclickselected);
                }

            } else if (fileObjectType == FileObjectType.FILE_TYPE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(fileclickselected))) {
                        if (archive_view) {
                            for (Path path : directoryStream) {
                                try {
                                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO_ZIP(path, true, fileObjectType);
                                    if (!filePOJO.getName().startsWith(".")) {
                                        filePOJOS_filtered.add(filePOJO);
                                    }
                                    filePOJOS.add(filePOJO);
                                } catch (Exception itemEx) {
                                    Timber.tag(Global.TAG).w(itemEx, "FILE(ZIP) item skipped: %s", String.valueOf(path));
                                }
                            }
                        } else {
                            for (Path path : directoryStream) {
                                try {
                                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(path, true, fileObjectType);
                                    if (!filePOJO.getName().startsWith(".")) {
                                        filePOJOS_filtered.add(filePOJO);
                                    }
                                    filePOJOS.add(filePOJO);
                                } catch (Exception itemEx) {
                                    Timber.tag(Global.TAG).w(itemEx, "FILE item skipped: %s", String.valueOf(path));
                                }
                            }
                        }
                    } catch (IOException e) {
                        Timber.tag(Global.TAG).w(e, "FILE_TYPE listing failed: %s", fileclickselected);
                    }
                } else {
                    try {
                        if (archive_view) {
                            file_type_fill_filePOJO_zip(file, fileObjectType, filePOJOS, filePOJOS_filtered);
                        } else {
                            file_type_fill_filePOJO(file, fileObjectType, filePOJOS, filePOJOS_filtered);
                        }
                    } catch (Exception e) {
                        Timber.tag(Global.TAG).w(e, "Legacy FILE_TYPE listing failed: %s", fileclickselected);
                    }
                }

            } else if (fileObjectType == FileObjectType.USB_TYPE) {
                try {
                    if (UsbFileRootSingleton.getInstance().isUsbFileRootSet()) {
                        try (ReadAccess access = UsbFileRootSingleton.getInstance().acquireUsbFileRootForRead()) {
                            UsbFile usbFileRoot = access.getUsbFile();
                            try {
                                if (usbFile == null) {
                                    usbFile = usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(fileclickselected));
                                }
                                UsbFile[] file_array = usbFile != null ? usbFile.listFiles() : new UsbFile[0];
                                int size = file_array.length;
                                for (int i = 0; i < size; ++i) {
                                    UsbFile f = file_array[i];
                                    try {
                                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, true);
                                        filePOJOS_filtered.add(filePOJO);
                                        filePOJOS.add(filePOJO);
                                    } catch (Exception itemEx) {
                                        Timber.tag(Global.TAG).w(itemEx, "USB item skipped: %s", f.getName());
                                    }
                                }
                            } catch (IOException e) {
                                Timber.tag(Global.TAG).w(e, "USB_TYPE listFiles failed: %s", fileclickselected);
                            }
                        }
                    }
                } catch (Exception e) {
                    Timber.tag(Global.TAG).w(e, "USB_TYPE access failed: %s", fileclickselected);
                }
            } else if (fileObjectType == FileObjectType.FTP_TYPE) {
                FtpClientRepository ftpClientRepository = null;
                FTPClient ftpClient = null;
                try {
                    ftpClientRepository = FtpClientRepository.getInstance(NetworkAccountDetailsViewModel.FTP_NETWORK_ACCOUNT_POJO);
                    ftpClient = ftpClientRepository.getFtpClient();
                    FTPFile[] file_array = ftpClient.listFiles(fileclickselected);
                    int size = file_array.length;
                    for (int i = 0; i < size; ++i) {
                        FTPFile f = file_array[i];
                        try {
                            String name = f.getName();
                            if (".".equals(name) || "..".equals(name)) continue;
                            String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                            FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, false, fileObjectType, path, ftpClient);
                            filePOJOS_filtered.add(filePOJO);
                            filePOJOS.add(filePOJO);
                        } catch (Exception itemEx) {
                            Timber.tag(Global.TAG).w(itemEx, "FTP item skipped");
                        }
                    }
                } catch (Exception e) {
                    Timber.tag(Global.TAG).w(e, "FTP_TYPE listing failed: %s", fileclickselected);
                } finally {
                    if (ftpClientRepository != null && ftpClient != null) {
                        ftpClientRepository.releaseFtpClient(ftpClient);
                    }
                }
            } else if (fileObjectType == FileObjectType.SFTP_TYPE) {
                SftpChannelRepository sftpChannelRepository = null;
                ChannelSftp channelSftp = null;
                try {
                    sftpChannelRepository = SftpChannelRepository.getInstance(NetworkAccountDetailsViewModel.SFTP_NETWORK_ACCOUNT_POJO);
                    channelSftp = sftpChannelRepository.getSftpChannel();
                    Vector<ChannelSftp.LsEntry> lsEntries = channelSftp.ls(fileclickselected);
                    for (ChannelSftp.LsEntry lsEntry : lsEntries) {
                        try {
                            String name = lsEntry.getFilename();
                            if (".".equals(name) || "..".equals(name)) continue;
                            String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                            FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(lsEntry, false, fileObjectType, path, channelSftp);
                            filePOJOS_filtered.add(filePOJO);
                            filePOJOS.add(filePOJO);
                        } catch (Exception itemEx) {
                            Timber.tag(Global.TAG).w(itemEx, "SFTP item skipped");
                        }
                    }
                } catch (Exception e) {
                    Timber.tag(Global.TAG).w(e, "SFTP_TYPE listing failed: %s", fileclickselected);
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
                        // remove the directory itself if present
                        try {
                            resources.remove(0);
                        } catch (Exception ignore) {
                        }
                    }
                    for (DavResource resource : resources) {
                        try {
                            String name = resource.getName();
                            if (".".equals(name) || "..".equals(name)) continue;
                            String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                            FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(resource, false, fileObjectType, path, sardine);
                            filePOJOS_filtered.add(filePOJO);
                            filePOJOS.add(filePOJO);
                        } catch (Exception itemEx) {
                            Timber.tag(Global.TAG).w(itemEx, "WebDAV item skipped");
                        }
                    }
                } catch (IOException e) {
                    Timber.tag(Global.TAG).w(e, "WEBDAV_TYPE listing failed: %s", fileclickselected);
                }
            } else if (fileObjectType == FileObjectType.SMB_TYPE) {
                SmbClientRepository smbClientRepository = SmbClientRepository.getInstance(NetworkAccountDetailsViewModel.SMB_NETWORK_ACCOUNT_POJO);
                SmbClientRepository.ShareHandle h = null;
                try {
                    h = smbClientRepository.acquireShare();
                    DiskShare share = h.share;
                    String adjustedPath = fileclickselected.startsWith("/") ? fileclickselected.substring(1) : fileclickselected;
                    List<FileIdBothDirectoryInformation> fileList = share.list(adjustedPath);
                    for (FileIdBothDirectoryInformation info : fileList) {
                        try {
                            String name = info.getFileName();
                            if (".".equals(name) || "..".equals(name)) continue;
                            String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                            MakeFilePOJOUtil.SmbFileInfo smbFileInfo = new MakeFilePOJOUtil.SmbFileInfo(
                                    name, path, info.getFileAttributes(), info.getEndOfFile(),
                                    info.getCreationTime().toEpochMillis(), info.getLastAccessTime().toEpochMillis(),
                                    info.getLastWriteTime().toEpochMillis(), info.getChangeTime().toEpochMillis());
                            FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(smbFileInfo, false, fileObjectType);
                            filePOJOS_filtered.add(filePOJO);
                            filePOJOS.add(filePOJO);
                        } catch (Exception itemEx) {
                            Timber.tag(Global.TAG).w(itemEx, "SMB item skipped");
                        }
                    }

                } catch (Exception e) {
                    Timber.tag(Global.TAG).w(e, "SMB_TYPE listing failed: %s", fileclickselected);
                } finally {
                    if (smbClientRepository != null) {
                        smbClientRepository.releaseShare(h);
                    }
                }
            } else if (fileObjectType == FileObjectType.GOOGLE_DRIVE_TYPE) {
                try {
                    String oauthToken = CloudAuthActivityViewModel.GOOGLE_DRIVE_ACCESS_TOKEN;
                    String parentId = getFileIdByPath(fileclickselected, oauthToken);

                    List<GoogleDriveFileModel.GoogleDriveFileMetadata> driveFiles = GoogleDriveFileModel.listFilesInFolder(parentId, oauthToken);
                    for (GoogleDriveFileModel.GoogleDriveFileMetadata meta : driveFiles) {
                        try {
                            String name = meta.name;
                            String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                            FilePOJO filePOJO = MakeCloudFilePOJOUtil.MAKE_FilePOJO_FromDriveMeta(meta, path, false, fileObjectType);

                            if (filePOJO != null) {
                                filePOJOS_filtered.add(filePOJO);
                                filePOJOS.add(filePOJO);
                            }
                        } catch (Exception itemEx) {
                            Timber.tag(Global.TAG).w(itemEx, "GOOGLE_DRIVE item skipped");
                        }
                    }
                } catch (Exception e) {
                    Timber.tag(Global.TAG).w(e, "GOOGLE_DRIVE_TYPE listing failed: %s", fileclickselected);
                }
            } else if (fileObjectType == FileObjectType.DROP_BOX_TYPE) {
                try {
                    String accessToken = CloudAuthActivityViewModel.DROP_BOX_ACCESS_TOKEN;
                    DbxClientV2 dbxClient = new DbxClientV2(new DbxRequestConfig("YourAppName"), accessToken);

                    String dropboxPath = "/".equals(fileclickselected) ? "" : fileclickselected;

                    ListFolderResult result = dbxClient.files().listFolder(dropboxPath);

                    while (true) {
                        for (Metadata meta : result.getEntries()) {
                            try {
                                String name = meta.getName();
                                String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                                FilePOJO filePOJO = MakeCloudFilePOJOUtil.MAKE_FilePOJO(meta, false, fileObjectType, path);
                                if (filePOJO != null) {
                                    filePOJOS_filtered.add(filePOJO);
                                    filePOJOS.add(filePOJO);
                                }
                            } catch (Exception itemEx) {
                                Timber.tag(Global.TAG).w(itemEx, "DROPBOX item skipped");
                            }
                        }

                        if (!result.getHasMore()) break;
                        result = dbxClient.files().listFolderContinue(result.getCursor());
                    }
                } catch (Exception e) {
                    Timber.tag(Global.TAG).w(e, "DROP_BOX_TYPE listing failed: %s", fileclickselected);
                }
            } else if (fileObjectType == FileObjectType.YANDEX_TYPE) {
                try {
                    String accessToken = CloudAuthActivityViewModel.YANDEX_ACCESS_TOKEN;
                    OkHttpClient client = Global.HTTP;
                    Gson gson = Global.GSON;

                    String yPath = (fileclickselected == null || fileclickselected.trim().isEmpty()) ? "/" : fileclickselected;

                    HttpUrl url = HttpUrl.parse("https://cloud-api.yandex.net/v1/disk/resources")
                            .newBuilder()
                            .addQueryParameter("path", yPath)
                            .addQueryParameter("limit", "1000") // reduce risk of missing items
                            .addQueryParameter("fields",
                                    "_embedded.items.name,_embedded.items.type,_embedded.items.size,_embedded.items.modified,_embedded.items.path")
                            .build();

                    Request request = new Request.Builder()
                            .url(url)
                            .header("Authorization", "OAuth " + accessToken)
                            .get()
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            MakeCloudFilePOJOUtil.YandexResource dirRes =
                                    gson.fromJson(response.body().charStream(), MakeCloudFilePOJOUtil.YandexResource.class);

                            if (dirRes != null && dirRes._embedded != null && dirRes._embedded.items != null) {
                                for (MakeCloudFilePOJOUtil.YandexResource item : dirRes._embedded.items) {
                                    try {
                                        String name = item.name;
                                        String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                                        FilePOJO filePOJO = MakeCloudFilePOJOUtil.MAKE_FilePOJO(item, false, fileObjectType, path);

                                        if (filePOJO != null) {
                                            filePOJOS_filtered.add(filePOJO);
                                            filePOJOS.add(filePOJO);
                                        }
                                    } catch (Exception itemEx) {
                                        Timber.tag(Global.TAG).w(itemEx, "YANDEX item skipped");
                                    }
                                }
                            }
                        } else {
                            Timber.tag(Global.TAG).w("YANDEX_TYPE listing not successful: %s", fileclickselected);
                        }
                    }
                } catch (Exception e) {
                    Timber.tag(Global.TAG).w(e, "YANDEX_TYPE listing failed: %s", fileclickselected);
                }

//            } else if (fileObjectType == FileObjectType.ONE_DRIVE_TYPE) {
//                try {
//                    // Listing via your path-only OneDriveFileModel
//                    FileModel folderModel = new OneDriveFileModel(fileclickselected);
//                    FileModel[] children = folderModel.list();
//
//                    for (FileModel child : children) {
//                        try {
//                            // Build display path (you already do this pattern everywhere)
//                            String name = child.getName();
//                            String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
//
//                            // IMPORTANT: avoid re-resolving network by creating another model if possible.
//                            // We should build FilePOJO from metadata, but we only have FileModel here.
//                            // So: if your OneDriveFileModel exposes DriveItem, use that.
//                            // If not, fallback: MakeFilePOJOUtil.MAKE_FilePOJO(child,...)
//
//                            FilePOJO filePOJO;
//
//                            if (child instanceof OneDriveFileModel) {
//                                // Add a getter in OneDriveFileModel: DriveItem getDriveItemUnsafe()
//                                filePOJO = MakeCloudFilePOJOUtil.MAKE_FilePOJO_FromOneDriveModel(
//                                        (OneDriveFileModel) child,
//                                        path,
//                                        false,
//                                        fileObjectType
//                                );
//                            } else {
//                                filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(child, false, fileObjectType);
//                            }
//
//                            if (filePOJO != null) {
//                                if (!filePOJO.getName().startsWith("."))
//                                    filePOJOS_filtered.add(filePOJO);
//                                filePOJOS.add(filePOJO);
//                            }
//                        } catch (Exception itemEx) {
//                            Timber.tag(Global.TAG).w(itemEx, "ONEDRIVE item skipped");
//                        }
//                    }
//
//                } catch (Exception e) {
//                    Timber.tag(Global.TAG).w(e, "ONE_DRIVE_TYPE listing failed: %s", fileclickselected);
//                }
            } else {
                try {
                    FileModel fileModel = FileModelFactory.getFileModel(fileclickselected, fileObjectType, null, null);
                    FileModel[] fileModels = fileModel.list();
                    int size = fileModels.length;
                    for (int i = 0; i < size; ++i) {
                        FileModel f = fileModels[i];
                        try {
                            String name = f.getName();
                            String path = Global.CONCATENATE_PARENT_CHILD_PATH(fileclickselected, name);
                            FileModel childFileModel = FileModelFactory.getFileModel(path, fileObjectType, null, null);
                            FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(childFileModel, false, fileObjectType);
                            filePOJOS_filtered.add(filePOJO);
                            filePOJOS.add(filePOJO);
                        } catch (Exception itemEx) {
                            Timber.tag(Global.TAG).w(itemEx, "GEN item skipped");
                        }
                    }
                } catch (Exception e) {
                    Timber.tag(Global.TAG).w(e, "GENERIC listing failed: %s", fileclickselected);
                }
            }

        } finally {
            RepositoryClass repositoryClass = RepositoryClass.getRepositoryClass();
            repositoryClass.hashmap_file_pojo.put(fileObjectType + fileclickselected, new ArrayList<>(filePOJOS));
            repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType + fileclickselected, new ArrayList<>(filePOJOS_filtered));
        }
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
        OkHttpClient httpClient = Global.HTTP;
        Gson gson = Global.GSON;

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
