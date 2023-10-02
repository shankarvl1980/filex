package svl.kadatha.filex;

import android.app.Application;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.os.EnvironmentCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import me.jahnen.libaums.core.fs.UsbFile;

public class FilePOJOViewModel extends AndroidViewModel {
    private final Application application;
    private boolean isCancelled=false;
    private Future<?> future1,future2,future3, future4, future5, future6, future7, future8,future9,future10,future11;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> copyFtpAsyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public List<FilePOJO> filePOJOS, filePOJOS_filtered;
    public IndexedLinkedHashMap<Integer,String> mselecteditems=new IndexedLinkedHashMap<>();
    private String what_to_find=null;
    private String media_category=null;
    final List<FilePOJO> path=new ArrayList<>();
    public String file_type="f";
    int count;
    int dir_count;int half_dir_count;

    public String library_filter_path;
    public boolean library_time_desc=false,library_size_desc=false;
    public String ftp_cached_file_path;
    public FileObjectType ftp_cached_file_fileObjectType;
    public boolean select_app_to_open_ftp;



    public FilePOJOViewModel(@NonNull Application application) {
        super(application);
        this.application=application;
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    public void cancel(boolean mayInterruptRunning){
        if(future1!=null) future1.cancel(mayInterruptRunning);
        if(future2!=null) future2.cancel(mayInterruptRunning);
        if(future3!=null) future3.cancel(mayInterruptRunning);
        if(future4!=null) future4.cancel(mayInterruptRunning);
        if(future5!=null) future5.cancel(mayInterruptRunning);
        if(future6!=null) future6.cancel(mayInterruptRunning);
        if(future7!=null) future7.cancel(mayInterruptRunning);
        if(future8!=null) future8.cancel(mayInterruptRunning);
        if(future9!=null) future9.cancel(mayInterruptRunning);
        if(future10!=null) future10.cancel(mayInterruptRunning);
        if(future11!=null) future11.cancel(mayInterruptRunning);
        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }

    public synchronized void populateFilePOJO(FileObjectType fileObjectType, String fileclickselected, UsbFile currentUsbFile, boolean archive_view, boolean fill_file_size_also)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED) return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                filePOJOS=new ArrayList<>(); filePOJOS_filtered=new ArrayList<>();
                FilePOJOUtil.FILL_FILE_POJO(filePOJOS,filePOJOS_filtered,fileObjectType,fileclickselected,currentUsbFile,archive_view);
                if(fill_file_size_also)
                {
                    long storage_space=0L;
                    String key=fileObjectType+fileclickselected;
                    for(Map.Entry<String,SpacePOJO> entry:Global.SPACE_ARRAY.entrySet())
                    {
                        if(Global.IS_CHILD_FILE(key,entry.getKey()))
                        {
                            storage_space=entry.getValue().getTotalSpace();
                            break;
                        }
                    }

                    final long final_storage_space = storage_space;
                    dir_count = filePOJOS.size();half_dir_count=filePOJOS.size()/2;
                    future4=executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            fill_file_size(final_storage_space, fileclickselected,0,dir_count);
                        }
                    });

//                    future5=executorService.submit(new Runnable() {
//                        @Override
//                        public void run() {
//                            fill_file_size(final_storage_space, fileclickselected,half_dir_count,dir_count);
//                        }
//                    });
                    try {
                        future4.get();
                        //future5.get();
                    } catch (ExecutionException | InterruptedException e) {}
                }
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public synchronized void fill_filePOJOs_size(FileObjectType fileObjectType, String fileclickselected, UsbFile currentUsbFile)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED) return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future2=executorService.submit(new Runnable() {
            @Override
            public void run() {
                long storage_space=0L;
                String key=fileObjectType+fileclickselected;
                for(Map.Entry<String,SpacePOJO> entry:Global.SPACE_ARRAY.entrySet())
                {
                    if(Global.IS_CHILD_FILE(key,entry.getKey()))
                    {
                        storage_space=entry.getValue().getTotalSpace();
                        break;
                    }
                }

                final long final_storage_space = storage_space;
                dir_count = filePOJOS.size();half_dir_count=filePOJOS.size()/2;
                future6=executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        fill_file_size(final_storage_space, fileclickselected,0,dir_count);
                    }
                });
//
//                future7=executorService.submit(new Runnable() {
//                    @Override
//                    public void run() {
//                        fill_file_size(final_storage_space, fileclickselected,half_dir_count,dir_count);
//                    }
//                });
                try {
                    future6.get();
                    //future7.get();
                } catch (ExecutionException | InterruptedException e) {}

                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    private void fill_file_size(long volume_storage_size, String fileclickselected ,int start, int end)
    {
        if(filePOJOS==null) return;
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        int[] total_no_of_files=new int[1];long[] total_size_of_files=new long[1];
        HashMap<String, FilePOJOViewModel.FileStoragePOJO> hashMap_directory_size;
        if(Global.IS_CHILD_FILE(fileclickselected,Global.INTERNAL_PRIMARY_STORAGE_PATH))
        {
            hashMap_directory_size=repositoryClass.hashmap_internal_directory_size;
        }
        else {
            hashMap_directory_size=repositoryClass.hashmap_external_directory_size;
        }

        for(int i=start;i<end;++i)
        {
            if(isCancelled()) return;
            FilePOJO filePOJO=filePOJOS.get(i);
            total_no_of_files[0]=0; total_size_of_files[0]=0;
            FileStoragePOJO fileStoragePOJO=hashMap_directory_size.get(filePOJO.getPath());
            if(fileStoragePOJO==null)
            {
                if(filePOJO.getIsDirectory())
                {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                    {
                        try {
                            new NioFileIteratorForSize(filePOJO.getPath(), hashMap_directory_size);
                        } catch (IOException e) {}
                    }
                    else
                    {
                        get_size_non_nio(new File(filePOJO.getPath()),total_no_of_files,total_size_of_files,true,hashMap_directory_size);
                    }
                }
            }

            if(filePOJO.getIsDirectory())
            {
                Iterator<Map.Entry<String, FileStoragePOJO>> iterator=hashMap_directory_size.entrySet().iterator();
                while(iterator.hasNext())
                {
                    Map.Entry<String, FileStoragePOJO> entry= iterator.next();
                    if(entry.getKey().startsWith(filePOJO.getPath()))
                    {
                        total_no_of_files[0]+=entry.getValue().number_of_files;
                        total_size_of_files[0]+=entry.getValue().total_size;
                    }
                }

                filePOJO.setTotalFiles(total_no_of_files[0]);
                filePOJO.setTotalSizeLong(total_size_of_files[0]);
                filePOJO.setTotalSize(FileUtil.humanReadableByteCount(total_size_of_files[0]));
                double percentage = total_size_of_files[0] * 100.0/ volume_storage_size;
                filePOJO.setTotalSizePercentageDouble(percentage);
                filePOJO.setTotalSizePercentage(String.format("%.2f",percentage) +"%");
            }
            else
            {
                double percentage = filePOJO.getSizeLong() * 100.0 / volume_storage_size;
                filePOJO.setTotalSizePercentageDouble(percentage);
                filePOJO.setTotalSizePercentage(String.format("%.2f",percentage)+"%");
            }
        }
    }

    private void get_size_non_nio(File f, int[]total_no_of_files, long[]total_size_of_files, boolean include_folder, HashMap<String, FileStoragePOJO> storage_hashmap)
    {
        int no_of_files=0;
        long size_of_files=0L;
        if(isCancelled()) return;
        if(f.isDirectory())
        {
            total_no_of_files[0]=0;total_size_of_files[0]=0;
            File[] files_array=f.listFiles();
            if(files_array!=null && files_array.length!=0)
            {
                for(File file:files_array)
                {
                    get_size_non_nio(file,total_no_of_files,total_size_of_files,include_folder,storage_hashmap);
                }
                if(include_folder)
                {
                    ++no_of_files;
                }
                storage_hashmap.put(f.getAbsolutePath(),new FileStoragePOJO(++total_no_of_files[0], total_size_of_files[0]));
                total_no_of_files[0]=0;total_size_of_files[0]=0;
            }
        }
        else
        {
            ++no_of_files;
            size_of_files+=f.length();

        }
        total_no_of_files[0]+=no_of_files;
        total_size_of_files[0]+=size_of_files;
    }

    public void copyFtpToDevice(String file_path, FileObjectType fileObjectType,boolean select_app)
    {
        if(copyFtpAsyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED) return;
        copyFtpAsyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ftp_cached_file_path=file_path;
        ftp_cached_file_fileObjectType=fileObjectType;
        select_app_to_open_ftp=select_app;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future10=executorService.submit(new Runnable() {
            @Override
            public void run() {
                Global.COPY_TO_FTP_CACHE(file_path);
                copyFtpAsyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }
    public synchronized void getLibraryList(String media_category)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED) return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future3=executorService.submit(new Runnable() {
            @Override
            public void run() {
                filePOJOS=new ArrayList<>(); filePOJOS_filtered=new ArrayList<>();
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                switch (media_category)
                {
                    case "Download":
                        repositoryClass.getDownLoadList(application,isCancelled);
                        break;
                    case "Document":
                        repositoryClass.getDocumentList(application,isCancelled);
                        break;
                    case "Image":
                        repositoryClass.getImageList(application, isCancelled);
                        break;
                    case "Audio":
                        repositoryClass.getAudioList(application,isCancelled);
                        break;
                    case "Video":
                        repositoryClass.getVideoList(application,isCancelled);
                        break;
                    case "Archive":
                        repositoryClass.getArchiveList(application,isCancelled);
                        break;
                    case "APK":
                        repositoryClass.getApkList(application,isCancelled);
                        break;
                    case "LargeFiles":
                        repositoryClass.getLargeFileList(application,isCancelled);
                }
                filePOJOS=repositoryClass.hashmap_file_pojo.get(FileObjectType.SEARCH_LIBRARY_TYPE+media_category);
                filePOJOS_filtered=repositoryClass.hashmap_file_pojo_filtered.get(FileObjectType.SEARCH_LIBRARY_TYPE+media_category);
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });

    }

    public synchronized void populateLibrarySearchFilePOJO(FileObjectType fileObjectType, Set<FilePOJO> search_in_dir,String library_or_search,String fileclickselected,String search_file_name,String search_file_type,boolean search_whole_word,boolean search_case_sensitive,boolean search_regex,long search_lower_limit_size,long search_upper_limit_size)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED) return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        count=0;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future11=executorService.submit(new Runnable() {
            @Override
            public void run() {
                if(filePOJOS!=null)
                {
                    Iterator<FilePOJO> iterator=filePOJOS.iterator();
                    while(iterator.hasNext())
                    {
                        if(isCancelled())
                        {
                            break;
                        }
                        FilePOJO filePOJO=iterator.next();
                        if(!new File(filePOJO.getPath()).exists())
                        {
                            iterator.remove();
                        }
                    }

                    filePOJOS_filtered=filePOJOS;
                    asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
                    return;
                }

                filePOJOS=new ArrayList<>(); filePOJOS_filtered=new ArrayList<>();
                if(library_or_search.equals(DetailFragment.SEARCH_RESULT))
                {
                    for(FilePOJO f : search_in_dir)
                    {
                        if(f.getFileObjectType()==FileObjectType.FILE_TYPE && Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(new File(f.getPath()))))
                        {
                            path.add(f);
                        }
                    }
                    if(search_regex)
                    {
                        what_to_find=search_file_name;
                    }
                    else if(search_whole_word)
                    {
                        what_to_find="\\Q"+search_file_name+"\\E";
                        if(!search_case_sensitive)
                        {
                            what_to_find="(?i)\\Q"+search_file_name+"\\E";
                        }
                    }
                    else
                    {
                        what_to_find=".*(\\Q"+search_file_name+"\\E).*";
                        if(!search_case_sensitive)
                        {
                            what_to_find=".*((?i)\\Q"+search_file_name+"\\E).*";
                        }
                    }
                    file_type=search_file_type;
                }
                else
                {
                    RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                    for(FilePOJO f : repositoryClass.storage_dir)
                    {
                        if(f.getFileObjectType()==FileObjectType.FILE_TYPE && Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(new File(f.getPath()))))
                        {
                            path.add(f);
                        }
                    }
                    if (application.getString(R.string.document).equals(library_or_search)) {
                        what_to_find = ".*((?i)\\.doc|\\.docx|\\.txt|\\.pdf|\\.html|\\.htm|\\.rtf|\\.ppt|\\.pptx|\\.xls|\\.xlsx)$";
                        media_category="Document";
                    } else if (application.getString(R.string.image).equals(library_or_search)) {
                        what_to_find = ".*((?i)\\.png|\\.jpg|\\.jpeg|\\.gif|\\.tif|\\.svg|\\.webp|\\.avif)$";
                        media_category="Image";
                    } else if (application.getString(R.string.audio).equals(library_or_search)) {
                        what_to_find = ".*((?i)\\.mp3|\\.ogg|\\.wav|\\.aac|\\.wma|\\.opus|\\.m4r|\\.m4a)$";
                        media_category="Audio";
                    } else if (application.getString(R.string.video).equals(library_or_search)) {
                        what_to_find = ".*((?i)\\.3gp|\\.mp4|\\.avi|\\.mov|\\.flv|\\.wmv|\\.webm)$";
                        media_category="Video";
                    } else if (application.getString(R.string.archive).equals(library_or_search)) {
                        what_to_find = ".*((?i)\\.zip|\\.rar|\\.tar|\\.gz|\\.gzip|\\.jar|\\.7z)$";
                        media_category="Archive";
                    } else if (application.getString(R.string.apk).equals(library_or_search)) {
                        what_to_find = ".*(?i)\\.apk$";
                        media_category="APK";
                    } else if(application.getString(R.string.download).equals(library_or_search)){
                        what_to_find=".*";
                        media_category="Download";
                    }
                }

                if(Global.DETAILED_SEARCH_LIBRARY)
                {
                    if(media_category!=null && media_category.equals("Download"))
                    {
                        search_download(filePOJOS,filePOJOS_filtered);
                    }
                    else
                    {
                        for(FilePOJO f : path)
                        {
                            if(search_upper_limit_size==0L && search_lower_limit_size==0L)
                            {
                                search_file(what_to_find,file_type,f.getPath(),filePOJOS,filePOJOS_filtered);
                            }
                            else
                            {
                                search_file(what_to_find,file_type,f.getPath(),filePOJOS,filePOJOS_filtered,search_lower_limit_size,search_upper_limit_size);
                            }

                        }
                    }
                }
                else
                {
                    if(media_category!=null && media_category.equals("Download"))
                    {
                        search_download(filePOJOS,filePOJOS_filtered);
                    }
                    else if(library_or_search.equals(DetailFragment.SEARCH_RESULT))
                    {
                        for(FilePOJO f : path)
                        {
                            if(search_upper_limit_size==0L && search_lower_limit_size==0L)
                            {
                                search_file(what_to_find,file_type,f.getPath(),filePOJOS,filePOJOS_filtered);
                            }
                            else
                            {
                                search_file(what_to_find,file_type,f.getPath(),filePOJOS,filePOJOS_filtered,search_lower_limit_size,search_upper_limit_size);
                            }
                        }
                    }
                    else
                    {
                        search_file(filePOJOS,filePOJOS_filtered);
                    }
                }
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                repositoryClass.hashmap_file_pojo.put(fileObjectType+fileclickselected,filePOJOS);
                repositoryClass.hashmap_file_pojo_filtered.put(fileObjectType+fileclickselected,filePOJOS_filtered);
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }


    private void search_file(List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered)
    {
        if(media_category==null)return;
        Cursor cursor=null;
        switch (media_category)
        {
            case "Download":
                search_download(f_pojos,f_pojos_filtered);
                break;
            case "Document":
                cursor=application.getContentResolver().query(MediaStore.Files.getContentUri("external"),new String[]{MediaStore.Files.FileColumns.DATA},
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
                cursor=application.getContentResolver().query(MediaStore.Files.getContentUri("external"),new String[]{MediaStore.Files.FileColumns.DATA},
                        "("+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+" OR "+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+")",
                        new String[]{"%.tar","%.gzip","%.gz","%.zip","%.rar","%.jar","%.7z"},null);
                break;
            case "APK":
                cursor=application.getContentResolver().query(MediaStore.Files.getContentUri("external"),new String[]{MediaStore.Files.FileColumns.DATA},
                        "("+
                                MediaStore.Files.FileColumns.DATA+" LIKE ?"+")",
                        new String[]{"%.apk"},null);
                break;
            case "Image":
                cursor=application.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Images.Media.DATA},null,null,null);
                break;
            case "Audio":
                cursor=application.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Media.DATA},null,null,null);
                break;
            case "Video":
                cursor=application.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Video.Media.DATA},null,null,null);
                break;
            default:
                break;
        }


        if(cursor!=null && cursor.getCount()>0)
        {
            boolean extract_icon= media_category != null && media_category.equals("APK");
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
                    //if(Files.exists(path))
                    {
                        FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(path,extract_icon,FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        count++;
                        //mutable_file_count.postValue(count);
                    }
                }
            }
            else
            {
                while(cursor.moveToNext())
                {
                    if(isCancelled())
                    {
                        return;
                    }
                    String data=cursor.getString(0);
                    File f=new File(data);
                    // if(f.exists())
                    {
                        FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(f,extract_icon,FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        count++;
                        //        mutable_file_count.postValue(count);
                    }
                }
            }

            cursor.close();
        }
    }

    private void search_download(List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered)
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
                        FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(p,true,FileObjectType.FILE_TYPE);
                        f_pojos.add(filePOJO);
                        f_pojos_filtered.add(filePOJO);
                        count++;
                        //mutable_file_count.postValue(count);
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
                    if(isCancelled())
                    {
                        return;
                    }
                    File file=file_list[i];
                    FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(file,true,FileObjectType.FILE_TYPE);
                    f_pojos.add(filePOJO);
                    f_pojos_filtered.add(filePOJO);
                    count++;
                    //  mutable_file_count.postValue(count);
                }
            }
        }

    }

    private void search_file(String search_name,String file_type, String search_dir, List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered) throws PatternSyntaxException
    {
        boolean extract_icon= media_category != null && media_category.equals("APK");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            try {
                Files.walkFileTree(Paths.get(search_dir), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
                        if((file_type.equals("d") || file_type.equals("fd")) && Pattern.matches(search_name,path.getFileName().toString()))
                        {
                            FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(path,extract_icon,FileObjectType.FILE_TYPE);
                            f_pojos.add(filePOJO);
                            f_pojos_filtered.add(filePOJO);
                            count++;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
                        if((file_type.equals("f")||file_type.equals("fd")) && Pattern.matches(search_name,path.getFileName().toString()))
                        {
                            FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(path,extract_icon,FileObjectType.FILE_TYPE);
                            f_pojos.add(filePOJO);
                            f_pojos_filtered.add(filePOJO);
                            count++;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path path, IOException e) {
                        return FileVisitResult.CONTINUE;
                    }

                });
            }
            catch(final PatternSyntaxException e)
            {
                Global.print_background_thread(application,e.getMessage());
            }
            catch (IOException e) {

            }
        }
        else
        {
            File[] list=new File(search_dir).listFiles();
            if(list==null) return;
            int size=list.length;
            for(int i=0;i<size;++i)
            {
                File f=list[i];
                if(isCancelled())
                {
                    return;
                }
                try
                {
                    if(f.isDirectory())
                    {
                        if((file_type.equals("d") || file_type.equals("fd")) && Pattern.matches(search_name,f.getName()))
                        {
                            FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(f,extract_icon,FileObjectType.FILE_TYPE);
                            f_pojos.add(filePOJO);
                            f_pojos_filtered.add(filePOJO);
                            count++;
                        }
                        search_file(search_name,file_type,f.getPath(),f_pojos,f_pojos_filtered);
                    }
                    else
                    {
                        if((file_type.equals("f")||file_type.equals("fd")) && Pattern.matches(search_name,f.getName()))
                        {
                            FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(f,extract_icon,FileObjectType.FILE_TYPE);
                            f_pojos.add(filePOJO);
                            f_pojos_filtered.add(filePOJO);
                            count++;
                        }
                    }
                    //mutable_file_count.postValue(count);
                }
                catch(final PatternSyntaxException e)
                {
                    Global.print_background_thread(application,e.getMessage());
                }

            }
        }

    }

    private void search_file(String search_name,String file_type, String search_dir, List<FilePOJO> f_pojos, List<FilePOJO> f_pojos_filtered, long lower_limit_size, long upper_limit_size) throws PatternSyntaxException
    {
        boolean extract_icon= media_category != null && media_category.equals("APK");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            try {
                Files.walkFileTree(Paths.get(search_dir), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
                        if((file_type.equals("d") || file_type.equals("fd")) && Pattern.matches(search_name,path.getFileName().toString()))
                        {
                            FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(path,extract_icon,FileObjectType.FILE_TYPE);
                            f_pojos.add(filePOJO);
                            f_pojos_filtered.add(filePOJO);
                            count++;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
                        long length=basicFileAttributes.size();
                        if((file_type.equals("f")||file_type.equals("fd")) && Pattern.matches(search_name,path.getFileName().toString()) && ((lower_limit_size == 0 || length >= lower_limit_size) && (upper_limit_size == 0 || length <= upper_limit_size)))
                        {
                            FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(path,extract_icon,FileObjectType.FILE_TYPE);
                            f_pojos.add(filePOJO);
                            f_pojos_filtered.add(filePOJO);
                            count++;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path path, IOException e) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            catch(final PatternSyntaxException e)
            {
                Global.print_background_thread(application,e.getMessage());
            }
            catch (IOException e) {

            }
        }
        else
        {
            File[] list=new File(search_dir).listFiles();
            if(list==null) return;
            int size=list.length;
            for(int i=0;i<size;++i)
            {
                File f=list[i];
                if(isCancelled())
                {
                    return;
                }
                try
                {
                    if(f.isDirectory())
                    {
                        if((file_type.equals("d") || file_type.equals("fd")) && Pattern.matches(search_name,f.getName()))
                        {
                            FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(f,extract_icon,FileObjectType.FILE_TYPE);
                            f_pojos.add(filePOJO);
                            f_pojos_filtered.add(filePOJO);
                            count++;
                        }
                        search_file(search_name,file_type,f.getPath(),f_pojos,f_pojos_filtered, lower_limit_size, upper_limit_size);
                    }
                    else
                    {
                        long length=f.length();
                        if((file_type.equals("f")||file_type.equals("fd")) && Pattern.matches(search_name,f.getName()) && ((lower_limit_size == 0 || length >= lower_limit_size) && (upper_limit_size == 0 || length <= upper_limit_size)))
                        {
                            FilePOJO filePOJO=FilePOJOUtil.MAKE_FilePOJO(f,extract_icon,FileObjectType.FILE_TYPE);
                            f_pojos.add(filePOJO);
                            f_pojos_filtered.add(filePOJO);
                            count++;
                        }
                    }
                    //mutable_file_count.postValue(count);
                }
                catch(final PatternSyntaxException e)
                {
                    Global.print_background_thread(application,e.getMessage());
                }
            }
        }

    }

    static class FileStoragePOJO
    {
        final int number_of_files;
        final long total_size;

        FileStoragePOJO(int number_of_files,long total_size)
        {
            this.number_of_files=number_of_files;
            this.total_size=total_size;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static class NioFileIteratorForSize extends SimpleFileVisitor<Path>
    {
        private final int[] count = new int[1];
        private final long[] size = new long[1];
        private final HashMap<String, FilePOJOViewModel.FileStoragePOJO>storagePOJOHashMap;
        public NioFileIteratorForSize(String file_path,HashMap<String, FilePOJOViewModel.FileStoragePOJO> storagePOJOHashMap) throws IOException
        {
            this.storagePOJOHashMap=storagePOJOHashMap;
            Files.walkFileTree(Paths.get(file_path), this);
        }


        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
            ++count[0];
            size[0]+=attributes.size();
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
            count[0]=0;
            size[0]=0;

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            ++count[0];
            String path=dir.toAbsolutePath().toString();
            storagePOJOHashMap.put(path,new FilePOJOViewModel.FileStoragePOJO(count[0],size[0]));
            count[0]=0;
            size[0]=0;
            return FileVisitResult.CONTINUE;
        }

        public int getCount() {
            return count[0];
        }

        public long getSize() {
            return size[0];
        }
    }

}
