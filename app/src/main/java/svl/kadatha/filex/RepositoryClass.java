package svl.kadatha.filex;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class RepositoryClass {

    final MutableLiveData<Integer> download_mutable_count = new MutableLiveData<>();
    final MutableLiveData<Integer> document_mutable_count = new MutableLiveData<>();
    final MutableLiveData<Integer> image_mutable_count = new MutableLiveData<>();
    final MutableLiveData<Integer> audio_mutable_count = new MutableLiveData<>();
    final MutableLiveData<Integer> video_mutable_count = new MutableLiveData<>();
    final MutableLiveData<Integer> archive_mutable_count = new MutableLiveData<>();
    final MutableLiveData<Integer> apk_mutable_count = new MutableLiveData<>();
    final MutableLiveData<Integer> large_mutable_count = new MutableLiveData<>();
    final MutableLiveData<Integer> duplicate_mutable_count = new MutableLiveData<>();
    final HashMap<String, List<FilePOJO>> hashmap_file_pojo_filtered = new HashMap<>();
    final HashMap<String, List<FilePOJO>> hashmap_file_pojo = new HashMap<>();
    final HashMap<String, List<AppManagerListFragment.AppPOJO>> app_pojo_hashmap = new HashMap<>();
    final HashMap<String, List<AudioPOJO>> audio_pojo_hashmap = new HashMap<>();
    final HashMap<String, List<AlbumPOJO>> album_pojo_hashmap = new HashMap<>();
    final List<String> internal_storage_path_list = new ArrayList<>();
    final List<String> external_storage_path_list = new ArrayList<>();
    private final Object download_lock = new Object();
    private final Object document_lock = new Object();
    private final Object image_lock = new Object();
    private final Object audio_lock = new Object();
    private final Object video_lock = new Object();
    private final Object archive_lock = new Object();
    private final Object apk_lock = new Object();
    private final Object app_lock = new Object();
    private final Object audio_pojo_lock = new Object();
    private final Object album_pojo_lock = new Object();
    private final Object large_file_lock = new Object();
    private final Object duplicate_file_lock = new Object();
    int download_count, document_count, image_count, audio_count, video_count, archive_count, apk_count, large_count, duplicate_count;
    ArrayList<FilePOJO> storage_dir = new ArrayList<>();
    ConcurrentHashMap<String, FilePOJOViewModel.FileStoragePOJO> hashmap_internal_directory_size = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, FilePOJOViewModel.FileStoragePOJO> hashmap_external_directory_size = new ConcurrentHashMap<>();


    private RepositoryClass() {
    }

    public static RepositoryClass getRepositoryClass() {
        return RepositoryClassHolder.repositoryClass;
    }

    public void getDownLoadList(boolean isCancelled) {
        synchronized (download_lock) {
            String media_category = "Download";
            if (hashmap_file_pojo.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE + media_category)) {
                return;
            }

            List<FilePOJO> filePOJOS = new ArrayList<>();
            List<FilePOJO> filePOJOS_filtered = new ArrayList<>();
            search_file(media_category, filePOJOS, filePOJOS_filtered, isCancelled, download_count = 0, download_mutable_count);
            hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS);
            hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS_filtered);
        }
    }

    public void getDocumentList(boolean isCancelled) {
        synchronized (document_lock) {
            String media_category = "Document";
            if (hashmap_file_pojo.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE + media_category)) {
                return;
            }

            List<FilePOJO> filePOJOS = new ArrayList<>();
            List<FilePOJO> filePOJOS_filtered = new ArrayList<>();
            search_file(media_category, filePOJOS, filePOJOS_filtered, isCancelled, document_count = 0, document_mutable_count);
            hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS);
            hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS_filtered);
        }
    }

    public void getImageList(boolean isCancelled) {
        synchronized (image_lock) {
            String media_category = "Image";
            if (hashmap_file_pojo.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE + media_category)) {
                return;
            }
            List<FilePOJO> filePOJOS = new ArrayList<>();
            List<FilePOJO> filePOJOS_filtered = new ArrayList<>();
            search_file(media_category, filePOJOS, filePOJOS_filtered, isCancelled, image_count = 0, image_mutable_count);
            hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS);
            hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS_filtered);
        }
    }

    public void getAudioList(boolean isCancelled) {
        synchronized (audio_lock) {
            String media_category = "Audio";
            if (hashmap_file_pojo.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE + media_category)) {
                return;
            }
            List<FilePOJO> filePOJOS = new ArrayList<>();
            List<FilePOJO> filePOJOS_filtered = new ArrayList<>();
            search_file(media_category, filePOJOS, filePOJOS_filtered, isCancelled, audio_count = 0, audio_mutable_count);
            hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS);
            hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS_filtered);
        }
    }

    public void getVideoList(boolean isCancelled) {
        synchronized (video_lock) {
            String media_category = "Video";
            if (hashmap_file_pojo.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE + media_category)) {
                return;
            }
            List<FilePOJO> filePOJOS = new ArrayList<>();
            List<FilePOJO> filePOJOS_filtered = new ArrayList<>();
            search_file(media_category, filePOJOS, filePOJOS_filtered, isCancelled, video_count = 0, video_mutable_count);
            hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS);
            hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS_filtered);
        }
    }

    public void getArchiveList(boolean isCancelled) {
        synchronized (archive_lock) {
            String media_category = "Archive";
            if (hashmap_file_pojo.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE + media_category)) {
                return;
            }
            List<FilePOJO> filePOJOS = new ArrayList<>();
            List<FilePOJO> filePOJOS_filtered = new ArrayList<>();
            search_file(media_category, filePOJOS, filePOJOS_filtered, isCancelled, archive_count = 0, archive_mutable_count);
            hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS);
            hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS_filtered);
        }

    }

    public void getApkList(boolean isCancelled) {
        synchronized (apk_lock) {
            String media_category = "APK";
            if (hashmap_file_pojo.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE + media_category)) {
                return;
            }
            List<FilePOJO> filePOJOS = new ArrayList<>();
            List<FilePOJO> filePOJOS_filtered = new ArrayList<>();
            search_file(media_category, filePOJOS, filePOJOS_filtered, isCancelled, apk_count = 0, apk_mutable_count);
            hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS);
            hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS_filtered);
        }
    }

    public void getLargeFileList(boolean isCancelled) {
        synchronized (large_file_lock) {
            String media_category = "Large Files";
            if (hashmap_file_pojo.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE + media_category)) {
                return;
            }
            List<FilePOJO> filePOJOS = new ArrayList<>();
            List<FilePOJO> filePOJOS_filtered = new ArrayList<>();
            search_file(media_category, filePOJOS, filePOJOS_filtered, isCancelled, large_count = 0, large_mutable_count);
            hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS);
            hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS_filtered);
        }
    }

    public void getDuplicateFileList(boolean isCancelled) {
        synchronized (duplicate_file_lock) {
            String media_category = "Duplicate Files";
            if (hashmap_file_pojo.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE + media_category)) {
                return;
            }
            List<FilePOJO> filePOJOS = new ArrayList<>();
            List<FilePOJO> filePOJOS_filtered = new ArrayList<>();
            search_file(media_category, filePOJOS, filePOJOS_filtered, isCancelled, duplicate_count = 0, duplicate_mutable_count);
            hashmap_file_pojo.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS);
            hashmap_file_pojo_filtered.put(FileObjectType.SEARCH_LIBRARY_TYPE + media_category, filePOJOS_filtered);
        }
    }

    private void search_file(String media_category, List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered, Boolean isCancelled, int count, MutableLiveData<Integer> mutable_file_count) {
        if (media_category == null) return;
        Context context = App.getAppContext();
        Cursor cursor = null;
        switch (media_category) {
            case "Download":
                search_download(f_pojos, f_pojos_filtered, isCancelled, count, mutable_file_count);
                break;
            case "Document":
                cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"), new String[]{MediaStore.Files.FileColumns.DATA},
                        "(" +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + ")",
                        new String[]{"%.doc", "%.docx", "%.txt", "%.pdf", "%.html", "%.htm", "%.rtf", "%.ppt", "%.pptx", "%.xls", "%.xlsx"}, null);
                break;
            case "Archive":
                cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"), new String[]{MediaStore.Files.FileColumns.DATA},
                        "(" +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + " OR " +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + ")",
                        new String[]{"%.tar", "%.gzip", "%.gz", "%.zip", "%.rar", "%.jar", "%.7z"}, null);
                break;
            case "APK":
                cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"), new String[]{MediaStore.Files.FileColumns.DATA},
                        "(" +
                                MediaStore.Files.FileColumns.DATA + " LIKE ?" + ")",
                        new String[]{"%.apk"}, null);
                break;
            case "Large Files":
                cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"), new String[]{MediaStore.Files.FileColumns.DATA},
                        "(" +
                                MediaStore.Files.FileColumns.SIZE + " >= ?" + ")",
                        new String[]{"20971520"}, null);

                break;
            case "Duplicate Files":
                cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"), new String[]{MediaStore.Files.FileColumns.DATA},
                        "(" + MediaStore.Files.FileColumns.MIME_TYPE + " NOT LIKE ?" + ")" + " AND " +
                                "(" + MediaStore.Files.FileColumns.SIZE + " >= ?" + ")"
                        , new String[]{DocumentsContract.Document.MIME_TYPE_DIR, "524288"}, null);
                break;
            case "Image":
                cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                break;
            case "Audio":
                cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media.DATA}, null, null, null);
                break;
            case "Video":
                cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Video.Media.DATA}, null, null, null);
                break;
            default:
                break;
        }


        if (cursor != null && cursor.getCount() > 0) {
            boolean extract_icon = media_category.equals("APK");
            Set<String> parent_directory = new LinkedHashSet<>();

            if (media_category.equals("Duplicate Files")) {
                MessageDigest digest;
                try {
                    digest = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException e) {
                    cursor.close();
                    return;
                }
                Map<String, List<String>> duplicate_file_name_hashmap = new HashMap<>();
                while (cursor.moveToNext()) {
                    if (isCancelled) {
                        cursor.close();
                        return;
                    }
                    DuplicateFiles.fillDuplicateFilesByName(cursor.getString(0), duplicate_file_name_hashmap);
                }

                List<String> duplicate_file_path_list = new ArrayList<>();
                Set<Map.Entry<String, List<String>>> entry_set = duplicate_file_name_hashmap.entrySet();
                Iterator<Map.Entry<String, List<String>>> iterator = entry_set.iterator();
                while (iterator.hasNext()) {

                    List<String> values = iterator.next().getValue();
                    if (values.size() < 2) {
                        iterator.remove();
                    } else {
                        duplicate_file_path_list.addAll(values);
                    }
                }

                TreeMap<String, List<String>> fileMap = DuplicateFiles.fillDuplicateFiles(duplicate_file_path_list, digest);
                for (Map.Entry<String, List<String>> e : fileMap.entrySet()) {
                    List<String> v = e.getValue();

                    String checksum = e.getKey();

                    if (v.size() > 1) {
                        for (String file_path : v) {
                            File f = new File(file_path);
                            if (f.exists()) {
                                FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, extract_icon, FileObjectType.FILE_TYPE);
                                filePOJO.setChecksum(checksum);
                                filePOJO.setWhetherExternal(!filePOJO.getPath().startsWith(Global.INTERNAL_PRIMARY_STORAGE_PATH));
                                f_pojos.add(filePOJO);
                                f_pojos_filtered.add(filePOJO);
                                parent_directory.add(f.getParent());
                                count++;
                                mutable_file_count.postValue(count);
                            }
                        }
                    }

                }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                while (cursor.moveToNext()) {
                    if (isCancelled) {
                        cursor.close();
                        return;
                    }

                    String data = cursor.getString(0);
                    Path path = Paths.get(data);
                    if (Files.exists(path)) {
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(path, extract_icon, FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        parent_directory.add(path.getParent().toString());
                        count++;
                        mutable_file_count.postValue(count);
                    }
                }
            } else {
                while (cursor.moveToNext()) {
                    if (isCancelled) {
                        cursor.close();
                        return;
                    }

                    String data = cursor.getString(0);
                    File f = new File(data);
                    if (f.exists()) {
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(f, extract_icon, FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        parent_directory.add(f.getParent());
                        count++;
                        mutable_file_count.postValue(count);
                    }
                }
            }

            List<LibraryAlbumSelectDialog.LibraryDirPOJO> libraryDirPOJOS = new ArrayList<>();
            for (String path : parent_directory) {
                String name = new File(path).getName();
                boolean fromSDCard = !Global.IS_CHILD_FILE(path, Global.INTERNAL_PRIMARY_STORAGE_PATH);
                libraryDirPOJOS.add(new LibraryAlbumSelectDialog.LibraryDirPOJO(path, name, fromSDCard));
            }
            Global.LIBRARY_FILTER_HASHMAP.put(media_category, libraryDirPOJOS);
            cursor.close();
        }
    }

    private void search_download(List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered, Boolean isCancelled, int count, MutableLiveData<Integer> mutable_file_count) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Path path = Paths.get("/storage/emulated/0/Download");
            if (Files.exists(path)) {
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
                    for (Path p : directoryStream) {
                        if (isCancelled) {
                            return;
                        }
                        FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(p, true, FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        count++;
                        mutable_file_count.postValue(count);
                    }
                } catch (IOException e) {

                }
            }
        } else {
            File f = new File("/storage/emulated/0/Download");
            if (f.exists()) {
                File[] file_list = f.listFiles();
                int size = file_list.length;
                for (int i = 0; i < size; ++i) {
                    if (isCancelled) {
                        return;
                    }
                    File file = file_list[i];
                    FilePOJO filePOJO = MakeFilePOJOUtil.MAKE_FilePOJO(file, true, FileObjectType.FILE_TYPE);
                    f_pojos.add(filePOJO);
                    f_pojos_filtered.add(filePOJO);
                    count++;
                    mutable_file_count.postValue(count);
                }
            }
        }

    }

    public void populateAppsList() {
        synchronized (app_lock) {
            if (app_pojo_hashmap.containsKey("system")) return;
            Context context = App.getAppContext();
            List<AppManagerListFragment.AppPOJO> userAppPOJOList = new ArrayList<>();
            List<AppManagerListFragment.AppPOJO> systemAppPOJOList = new ArrayList<>();

            int flags = PackageManager.GET_META_DATA |
                    PackageManager.GET_SHARED_LIBRARY_FILES |
                    PackageManager.GET_UNINSTALLED_PACKAGES;
            final PackageManager packageManager = context.getPackageManager();
            List<PackageInfo> packageInfos = packageManager.getInstalledPackages(flags);
            for (PackageInfo packageInfo : packageInfos) {
                if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                    String name = (String) packageInfo.applicationInfo.loadLabel(packageManager);
                    String package_name = packageInfo.packageName;
                    String version = packageInfo.versionName;
                    String publicsourcedir = packageInfo.applicationInfo.publicSourceDir;
                    if (publicsourcedir == null) {
                        continue;
                    }
                    File file = new File(publicsourcedir);
                    String path = file.getAbsolutePath();
                    long size = file.length();
                    long date = file.lastModified();
                    AppManagerListFragment.extract_icon(package_name + ".png", packageManager, packageInfo);
                    systemAppPOJOList.add(new AppManagerListFragment.AppPOJO(name, package_name, path, size, date, version));

                } else if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1) {
                    String name = (String) packageInfo.applicationInfo.loadLabel(packageManager);
                    String package_name = packageInfo.packageName;
                    String version = packageInfo.versionName;
                    String publicsourcedir = packageInfo.applicationInfo.publicSourceDir;
                    if (publicsourcedir == null) {
                        continue;
                    }
                    File file = new File(publicsourcedir);
                    String path = file.getAbsolutePath();
                    long size = file.length();
                    long date = file.lastModified();
                    AppManagerListFragment.extract_icon(package_name + ".png", packageManager, packageInfo);
                    userAppPOJOList.add(new AppManagerListFragment.AppPOJO(name, package_name, path, size, date, version));
                }
            }
            app_pojo_hashmap.put("user", userAppPOJOList);
            app_pojo_hashmap.put("system", systemAppPOJOList);
        }

    }

    public void getAudioPOJOList(boolean isCancelled) {
        synchronized (audio_pojo_lock) {
            if (audio_pojo_hashmap.containsKey("audio")) return;
            Context context = App.getAppContext();
            List<AudioPOJO> audio_list = new ArrayList<>();
            AudioPlayerActivity.EXISTING_AUDIOS_ID = new HashMap<>();
            Cursor audio_cursor;
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null, null, null, null);
            } catch (SecurityException e) {
            }

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    if (isCancelled) break;
                    String album_id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                    String album_path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                    String where = MediaStore.Audio.Media.ALBUM_ID + "=" + album_id;
                    audio_cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, where, null, null);
                    if (audio_cursor != null && audio_cursor.getCount() > 0) {
                        while (audio_cursor.moveToNext()) {
                            if (isCancelled) break;
                            int id = audio_cursor.getInt(audio_cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                            String data = audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                            String title = audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                            String album = audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                            String artist = audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                            String duration = audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                            if (new File(data).exists()) {
                                audio_list.add(new AudioPOJO(id, data, title, album_id, album, artist, duration, FileObjectType.FILE_TYPE));
                                AudioPlayerActivity.EXISTING_AUDIOS_ID.put(id, album_id);
                            }
                        }

                        audio_cursor.close();
                    }

                }
                audio_pojo_hashmap.put("audio", audio_list);
                cursor.close();
            }

        }

    }

    public void getAlbumList(boolean isCancelled) {
        synchronized (album_pojo_lock) {
            if (album_pojo_hashmap.containsKey("album")) return;
            Context context = App.getAppContext();
            List<AlbumPOJO> album_list = new ArrayList<>();
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null, null, null, null);
            } catch (SecurityException e) {
            }

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    if (isCancelled) break;
                    String id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                    String album_name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                    String no_of_songs = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
                    String album_path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                    album_list.add(new AlbumPOJO(id, album_name, artist, no_of_songs, album_path));
                }
                album_pojo_hashmap.put("album", album_list);
                cursor.close();
            }
        }
    }

    private static final class RepositoryClassHolder {
        static final RepositoryClass repositoryClass = new RepositoryClass();
    }
}
