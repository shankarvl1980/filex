package svl.kadatha.filex;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.MediaStore;

import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RepositoryClass {

    int download_count,document_count,image_count,audio_count,video_count,archive_count,apk_count;
    final MutableLiveData<Integer> download_mutable_count=new MutableLiveData<>();
    final MutableLiveData<Integer> document_mutable_count=new MutableLiveData<>();
    final MutableLiveData<Integer> image_mutable_count=new MutableLiveData<>();
    final MutableLiveData<Integer> audio_mutable_count=new MutableLiveData<>();
    final MutableLiveData<Integer> video_mutable_count=new MutableLiveData<>();
    final MutableLiveData<Integer> archive_mutable_count=new MutableLiveData<>();
    final MutableLiveData<Integer> apk_mutable_count=new MutableLiveData<>();

    private final Object download_lock=new Object();
    private final Object document_lock=new Object();
    private final Object image_lock=new Object();
    private final Object audio_lock=new Object();
    private final Object video_lock=new Object();
    private final Object archive_lock=new Object();
    private final Object apk_lock=new Object();
    private final Object app_lock=new Object();
    private final Object audio_pojo_lock=new Object();
    private final Object album_pojo_lock=new Object();


    private RepositoryClass(){}

    private static final class RepositoryClassHolder {
        static final RepositoryClass repositoryClass = new RepositoryClass();
    }

    public static RepositoryClass getRepositoryClass()
    {
        return RepositoryClassHolder.repositoryClass;
    }


    public void getDownLoadList(Context context,boolean isCancelled)
    {
        synchronized (download_lock)
        {
            String media_category="Download";
            if(Global.HASHMAP_FILE_POJO.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE+media_category))
            {
                return;
            }
            List<FilePOJO>filePOJOS=new ArrayList<>();
            List<FilePOJO>filePOJOS_filtered=new ArrayList<>();
            search_file(context,media_category,filePOJOS,filePOJOS_filtered,isCancelled,download_count=0,download_mutable_count);
            Global.HASHMAP_FILE_POJO.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS);
            Global.HASHMAP_FILE_POJO_FILTERED.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS_filtered);
        }

    }


    public void getDocumentList(Context context,boolean isCancelled)
    {
        synchronized (document_lock)
        {
            String media_category="Document";
            if(Global.HASHMAP_FILE_POJO.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE+media_category))
            {
                return;
            }
            List<FilePOJO>filePOJOS=new ArrayList<>();
            List<FilePOJO>filePOJOS_filtered=new ArrayList<>();
            search_file(context,media_category,filePOJOS,filePOJOS_filtered,isCancelled,document_count=0,document_mutable_count);
            Global.HASHMAP_FILE_POJO.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS);
            Global.HASHMAP_FILE_POJO_FILTERED.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS_filtered);
        }
    }

    public void getImageList(Context context,boolean isCancelled)
    {
        synchronized (image_lock)
        {
            String media_category="Image";
            if(Global.HASHMAP_FILE_POJO.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE+media_category))
            {
                return;
            }
            List<FilePOJO>filePOJOS=new ArrayList<>();
            List<FilePOJO>filePOJOS_filtered=new ArrayList<>();
            search_file(context,media_category,filePOJOS,filePOJOS_filtered,isCancelled,image_count=0,image_mutable_count);
            Global.HASHMAP_FILE_POJO.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS);
            Global.HASHMAP_FILE_POJO_FILTERED.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS_filtered);
        }
    }

    public void getAudioList(Context context,boolean isCancelled)
    {
        synchronized (audio_lock)
        {
            String media_category="Audio";
            if(Global.HASHMAP_FILE_POJO.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE+media_category))
            {
                return;
            }
            List<FilePOJO>filePOJOS=new ArrayList<>();
            List<FilePOJO>filePOJOS_filtered=new ArrayList<>();
            search_file(context,media_category,filePOJOS,filePOJOS_filtered,isCancelled,audio_count=0,audio_mutable_count);
            Global.HASHMAP_FILE_POJO.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS);
            Global.HASHMAP_FILE_POJO_FILTERED.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS_filtered);
        }
    }

    public void getVideoList(Context context,boolean isCancelled)
    {
        synchronized (video_lock)
        {
            String media_category="Video";
            if(Global.HASHMAP_FILE_POJO.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE+media_category))
            {
                return;
            }
            List<FilePOJO>filePOJOS=new ArrayList<>();
            List<FilePOJO>filePOJOS_filtered=new ArrayList<>();
            search_file(context,media_category,filePOJOS,filePOJOS_filtered,isCancelled,video_count=0,video_mutable_count);
            Global.HASHMAP_FILE_POJO.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS);
            Global.HASHMAP_FILE_POJO_FILTERED.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS_filtered);
        }
    }

    public void getArchiveList(Context context,boolean isCancelled)
    {
        synchronized (archive_lock)
        {
            String media_category="Archive";
            if(Global.HASHMAP_FILE_POJO.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE+media_category))
            {
                return;
            }
            List<FilePOJO>filePOJOS=new ArrayList<>();
            List<FilePOJO>filePOJOS_filtered=new ArrayList<>();
            search_file(context,media_category,filePOJOS,filePOJOS_filtered,isCancelled,archive_count=0,archive_mutable_count);
            Global.HASHMAP_FILE_POJO.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS);
            Global.HASHMAP_FILE_POJO_FILTERED.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS_filtered);
        }

    }

    public void getApkList(Context context,boolean isCancelled)
    {
        synchronized (apk_lock)
        {
            String media_category="APK";
            if(Global.HASHMAP_FILE_POJO.containsKey(FileObjectType.SEARCH_LIBRARY_TYPE+media_category))
            {
                return;
            }
            List<FilePOJO>filePOJOS=new ArrayList<>();
            List<FilePOJO>filePOJOS_filtered=new ArrayList<>();
            search_file(context,media_category,filePOJOS,filePOJOS_filtered,isCancelled,apk_count=0,apk_mutable_count);
            Global.HASHMAP_FILE_POJO.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS);
            Global.HASHMAP_FILE_POJO_FILTERED.put(FileObjectType.SEARCH_LIBRARY_TYPE+media_category,filePOJOS_filtered);
        }
    }


    private void search_file(Context context, String media_category, List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered, Boolean isCancelled, int count, MutableLiveData<Integer> mutable_file_count)
    {
        if(media_category==null)return;
        Cursor cursor=null;
        switch (media_category)
        {
            case "Download":
                search_download(f_pojos,f_pojos_filtered,isCancelled,count,mutable_file_count);
                break;
            case "Document":
                cursor=context.getContentResolver().query(MediaStore.Files.getContentUri("external"),new String[]{MediaStore.Files.FileColumns.DATA},
                                "("+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+")",
                        new String[]{"%.doc","%.docx","%.txt","%.pdf","%.html","%.htm","%.rtf","%.ppt","%.pptx","%.xls","%.xlsx"},null);
                break;
            case "Archive":
                cursor=context.getContentResolver().query(MediaStore.Files.getContentUri("external"),new String[]{MediaStore.Files.FileColumns.DATA},
                                 "("+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+")",
                        new String[]{"%.tar","%.gzip","%.gz","%.zip","%.rar","%.jar"},null);
                break;
            case "APK":
                cursor=context.getContentResolver().query(MediaStore.Files.getContentUri("external"),new String[]{MediaStore.Files.FileColumns.DATA},
                                 "("+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+")",
                        new String[]{"%.apk"},null);
                break;
            case "Image":
                cursor=context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Images.Media.DATA},null,null,null);
                break;
            case "Audio":
                cursor=context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Media.DATA},null,null,null);
                break;
            case "Video":
                cursor=context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Video.Media.DATA},null,null,null);
                break;
            default:
                break;
        }


        if(cursor!=null && cursor.getCount()>0)
        {
            boolean extract_icon= media_category.equals("APK");
            Set<String> parent_directory=new LinkedHashSet<>();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            {
                while(cursor.moveToNext())
                {
                    if(isCancelled)
                    {
                        return;
                    }

                    String data=cursor.getString(0);
                    Path path= Paths.get(data);
                    if(Files.exists(path))
                    {
                        FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(path,extract_icon,false,FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        parent_directory.add(path.getParent().toString());
                        count++;
                        mutable_file_count.postValue(count);
                    }
                }
            }
            else
            {
                while(cursor.moveToNext())
                {
                    if(isCancelled)
                    {
                        return;
                    }

                    String data=cursor.getString(0);
                    File f=new File(data);
                    if(f.exists())
                    {
                        FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(f,extract_icon,false,FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        parent_directory.add(f.getParent());
                        count++;
                        mutable_file_count.postValue(count);
                    }
                }
            }

            Global.LIBRARY_FILTER_HASHMAP.put(media_category,parent_directory);
            cursor.close();
        }
    }

    private void search_download(List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered,Boolean isCancelled,int count, MutableLiveData<Integer>mutable_file_count)
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            Path path=Paths.get("/storage/emulated/0/Download");
            if(Files.exists(path))
            {
                try(DirectoryStream<Path> directoryStream=Files.newDirectoryStream(path))
                {
                    for(Path p : directoryStream)
                    {
                        if(isCancelled)
                        {
                            return;
                        }
                        FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(p,true,false,FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        count++;
                        mutable_file_count.postValue(count);
                    }
                }
                catch (IOException e)
                {

                }
            }
        }
        else
        {
            File f=new File("/storage/emulated/0/Download");
            if(f.exists())
            {
                File[] file_list=f.listFiles();
                int size=file_list.length;
                for(int i=0;i<size;++i)
                {
                    if(isCancelled)
                    {
                        return;
                    }
                    File file=file_list[i];
                    FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(file,true,false,FileObjectType.FILE_TYPE);
                    f_pojos.add(filePOJO);
                    f_pojos_filtered.add(filePOJO);
                    count++;
                    mutable_file_count.postValue(count);
                }
            }
        }

    }


    public void populateAppsList(Context context)
    {
        synchronized (app_lock)
        {
            if(Global.APP_POJO_HASHMAP.containsKey("system")) return;

            List<AppManagerListFragment.AppPOJO> userAppPOJOList=new ArrayList<>();
            List<AppManagerListFragment.AppPOJO> systemAppPOJOList=new ArrayList<>();

            int flags = PackageManager.GET_META_DATA |
                    PackageManager.GET_SHARED_LIBRARY_FILES |
                    PackageManager.GET_UNINSTALLED_PACKAGES;
            final PackageManager packageManager = context.getPackageManager();
            List<PackageInfo> packageInfos=packageManager.getInstalledPackages(flags);
            for (PackageInfo packageInfo : packageInfos)
            {
                if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
                {
                    String name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
                    String package_name=packageInfo.packageName;
                    String version=packageInfo.versionName;
                    String publicsourcedir=packageInfo.applicationInfo.publicSourceDir;
                    if(publicsourcedir==null)
                    {
                        continue;
                    }
                    File file = new File(publicsourcedir);
                    String path=file.getAbsolutePath();
                    long size=file.length();
                    long date=file.lastModified();
                    AppManagerListFragment.extract_icon(package_name+".png",packageManager,packageInfo);
                    systemAppPOJOList.add(new AppManagerListFragment.AppPOJO(name, package_name, path, size, date,version));

                }
                else if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1)
                {
                    String name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
                    String package_name=packageInfo.packageName;
                    String version=packageInfo.versionName;
                    String publicsourcedir=packageInfo.applicationInfo.publicSourceDir;
                    if(publicsourcedir==null)
                    {
                        continue;
                    }
                    File file = new File(publicsourcedir);
                    String path=file.getAbsolutePath();
                    long size=file.length();
                    long date=file.lastModified();
                    AppManagerListFragment.extract_icon(package_name+".png",packageManager,packageInfo);
                    userAppPOJOList.add(new AppManagerListFragment.AppPOJO(name, package_name, path, size, date,version));
                }
            }
            Global.APP_POJO_HASHMAP.put("user",userAppPOJOList);
            Global.APP_POJO_HASHMAP.put("system",systemAppPOJOList);
        }

    }

    public void getAudioPOJOList(Context context, boolean isCancelled)
    {
        synchronized (audio_pojo_lock)
        {
            if(Global.AUDIO_POJO_HASHMAP.containsKey("audio")) return;
            List<AudioPOJO> audio_list=new ArrayList<>();
            AudioPlayerActivity.EXISTING_AUDIOS_ID=new ArrayList<>();
            Cursor audio_cursor;
            Cursor cursor = null;
            try
            {
                cursor=context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,null,null,null,null);
            }
            catch(SecurityException e){}

            if(cursor!=null && cursor.getCount()>0)
            {
                while(cursor.moveToNext())
                {
                    if(isCancelled)break;
                    String album_id=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                    String album_path=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                    String where=MediaStore.Audio.Media.ALBUM_ID+"="+album_id;
                    audio_cursor=context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,where,null,null);
                    if(audio_cursor!=null && audio_cursor.getCount()>0)
                    {
                        while(audio_cursor.moveToNext())
                        {
                            if(isCancelled)break;
                            int id=audio_cursor.getInt(audio_cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                            String data=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                            String title=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                            String album=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                            String artist=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                            String duration=audio_cursor.getString(audio_cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                            if(new File(data).exists())
                            {
                                audio_list.add(new AudioPOJO(id,data,title,album,artist,duration,FileObjectType.FILE_TYPE));
                                AudioPlayerActivity.EXISTING_AUDIOS_ID.add(id);
                            }
                        }

                        audio_cursor.close();
                    }

                }
                Global.AUDIO_POJO_HASHMAP.put("audio",audio_list);
                cursor.close();
            }

        }

    }

    public void getAlbumList(Context context, boolean isCancelled)
    {
        synchronized (album_pojo_lock)
        {
            if(Global.ALBUM_POJO_HASHMAP.containsKey("album"))return;
            List<AlbumPOJO>album_list=new ArrayList<>();
            Cursor cursor = null;
            try
            {
                cursor=context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,null,null,null,null);
            }
            catch (SecurityException e){}

            if(cursor!=null && cursor.getCount()>0)
            {
                while(cursor.moveToNext())
                {
                    if(isCancelled)break;
                    String id=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                    String album_name=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                    String artist=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                    String no_of_songs=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
                    String album_path=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                    album_list.add(new AlbumPOJO(id,album_name,artist,no_of_songs,album_path));
                }
                Global.ALBUM_POJO_HASHMAP.put("album",album_list);
                cursor.close();
            }
        }

    }

}
