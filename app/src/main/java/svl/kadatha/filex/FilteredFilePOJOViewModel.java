package svl.kadatha.filex;

import android.app.Application;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FilteredFilePOJOViewModel extends AndroidViewModel {
    private final Application application;
    private boolean isCancelled;
    private Future<?> future1,future2,future3,future4, future5,future6;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> hasWallPaperSet=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> isPdfBitmapFetched=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final MutableLiveData<AsyncTaskStatus> isRotated = new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public final List<FilePOJO> album_file_pojo_list=new ArrayList<>();
    public final IndexedLinkedHashMap<FilePOJO,Integer> video_list=new IndexedLinkedHashMap<>();
    public int total_images;
    public IndexedLinkedHashMap<Integer,Boolean> mselecteditems=new IndexedLinkedHashMap<>();
    public int file_selected_idx=0;
    public PdfRenderer pdfRenderer;
    public double size_per_page_MB;
    public int total_pages;
    public Bitmap bitmap;
    public boolean out_of_memory_exception_thrown;

    public int image_selected_idx=0,previously_selected_image_idx=0,pdf_current_position;
    public String source_folder;
    public FilePOJO currently_shown_file;
    public boolean firststart;

    public FileObjectType fileObjectType;
    public boolean fromThirdPartyApp;
    public String file_path;

    public boolean video_refreshed;


    public FilteredFilePOJOViewModel(@NonNull Application application) {
        super(application);
        this.application=application;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
        Global.DELETE_DIRECTORY_ASYNCHRONOUSLY(Global.TEMP_ROTATE_CACHE_DIR);
    }

    public void cancel(boolean mayInterruptRunning){
        if(future1!=null) future1.cancel(mayInterruptRunning);
        if(future2!=null) future2.cancel(mayInterruptRunning);
        if(future3!=null) future3.cancel(mayInterruptRunning);
        if(future4!=null) future4.cancel(mayInterruptRunning);
        if(future5!=null) future5.cancel(mayInterruptRunning);
        if(future6!=null) future6.cancel(mayInterruptRunning);
        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }


    public synchronized void getAlbumFromCurrentFolder(String regex, boolean whetherVideo )
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        firststart=true;
        source_folder=new File(file_path).getParent();
        ExecutorService executorService=MyExecutorService.getExecutorService();
        String finalSource_folder = source_folder;
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {

                if(fileObjectType ==FileObjectType.USB_TYPE)
                {
                    if(MainActivity.usbFileRoot!=null)
                    {
                        File cache_file=Global.COPY_TO_USB_CACHE(file_path);
                        currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(cache_file,false,FileObjectType.FILE_TYPE);
                    }
                }
                else if(fileObjectType==FileObjectType.ROOT_TYPE)
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,FileObjectType.FILE_TYPE);
                }
                else if(fileObjectType==FileObjectType.FTP_TYPE)
                {
                    File cache_file=new File(Global.FTP_CACHE_DIR,file_path);//Global.COPY_TO_FTP_CACHE(file_path);
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(cache_file,false,FileObjectType.FILE_TYPE);
                }
                else
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,FileObjectType.FILE_TYPE);
                }


                List<FilePOJO> filePOJOS=new ArrayList<>(), filePOJOS_filtered=new ArrayList<>();
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
                if (!repositoryClass.hashmap_file_pojo.containsKey(fileObjectType+ finalSource_folder))
                {
                    FilePOJOUtil.FILL_FILE_POJO(filePOJOS,filePOJOS_filtered,fileObjectType, finalSource_folder,null,false);
                }
                else
                {
                    if(MainActivity.SHOW_HIDDEN_FILE)
                    {
                        filePOJOS=repositoryClass.hashmap_file_pojo.get(fileObjectType+ finalSource_folder) ;
                    }
                    else
                    {
                        filePOJOS=repositoryClass.hashmap_file_pojo_filtered.get(fileObjectType+ finalSource_folder);
                    }
                }

                // limiting to the selected only, in case of file selected from usb storage by adding condition below
                if(fromThirdPartyApp || fileObjectType==FileObjectType.USB_TYPE || fileObjectType==FileObjectType.FTP_TYPE)
                {
                    if(whetherVideo)
                    {
                        video_list.put(currently_shown_file,0);
                    }
                    else
                    {
                        album_file_pojo_list.add(currently_shown_file);
                    }

                }
                else
                {
                    if(Global.SORT==null)
                    {
                        Global.GET_PREFERENCES(new TinyDB(application));
                    }
                    Collections.sort(filePOJOS,FileComparator.FilePOJOComparate(Global.SORT,false));
                    int size=filePOJOS.size();
                    int count=0;
                    for(int i=0; i<size;++i)
                    {
                        FilePOJO filePOJO=filePOJOS.get(i);
                        if(!filePOJO.getIsDirectory())
                        {
                            String file_ext;
                            int idx=filePOJO.getName().lastIndexOf(".");
                            if(idx!=-1)
                            {
                                file_ext=filePOJO.getName().substring(idx+1);
                                if(file_ext.matches(regex))
                                {
                                    if(whetherVideo)
                                    {
                                        video_list.put(filePOJO,0);
                                    }
                                    else
                                    {
                                        album_file_pojo_list.add(filePOJO);
                                    }

                                    if(filePOJO.getName().equals(currently_shown_file.getName())) file_selected_idx=count;
                                    count++;

                                }
                                else if(filePOJO.getName().equals(currently_shown_file.getName()))
                                {
                                    if(whetherVideo)
                                    {
                                        video_list.put(currently_shown_file,0);
                                    }
                                    else
                                    {
                                        album_file_pojo_list.add(currently_shown_file);
                                    }
                                    file_selected_idx=count;
                                    count++;
                                }

                            }
                            else if(filePOJO.getName().equals(currently_shown_file.getName()))
                            {
                                if(whetherVideo)
                                {
                                    video_list.put(currently_shown_file,0);
                                }
                                else
                                {
                                    album_file_pojo_list.add(currently_shown_file);
                                }
                                file_selected_idx=count;
                                count++;
                            }

                        }
                    }

                }
                if(whetherVideo)
                {
                    total_images=video_list.size();
                }
                else
                {
                    total_images=album_file_pojo_list.size();
                }

                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    public void setWallPaper(ActivityResult result,File temporaryDir)
    {
        if(hasWallPaperSet.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        hasWallPaperSet.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future2=executorService.submit(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {

                Uri uri=result.getData().getData();
                String file_name=result.getData().getStringExtra(InstaCropperActivity.EXTRA_FILE_NAME);
                File f=new File(temporaryDir,file_name);
                WallpaperManager wm= WallpaperManager.getInstance(application);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (wm.isWallpaperSupported() && wm.isSetWallpaperAllowed()) {
                            set_wallpaper(wm, uri);
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (wm.isWallpaperSupported()) {
                            set_wallpaper(wm, uri);
                        }
                    } else {
                        set_wallpaper(wm, uri);
                    }
                } finally {
                    if (f.exists()) {
                        f.delete();
                    }
                }

                hasWallPaperSet.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

    private void set_wallpaper(WallpaperManager wm, Uri uri) {
        try {
            wm.setStream(application.getContentResolver().openInputStream(uri));
            Global.print_background_thread(application, application.getString(R.string.set_wallpaper));
        } catch (IOException e) {
            // Handle exception
        }
    }

    public void initializePdfRenderer(FileObjectType fileObjectType,String file_path,Uri data,boolean fromThirdPartyApp)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        this.file_path=file_path;
        source_folder=new File(file_path).getParent();
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future3=executorService.submit(new Runnable() {
            @Override
            public void run() {

                if(fileObjectType ==FileObjectType.USB_TYPE)
                {
                    if(MainActivity.usbFileRoot!=null)
                    {
                        File cache_file=Global.COPY_TO_USB_CACHE(file_path);
                        currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(cache_file,false,FileObjectType.FILE_TYPE);
                    }
                }
                else if(fileObjectType==FileObjectType.ROOT_TYPE)
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,FileObjectType.FILE_TYPE);
                }
                else if(fileObjectType==FileObjectType.FTP_TYPE)
                {
                    File cache_file=new File(Global.FTP_CACHE_DIR,file_path);//Global.COPY_TO_FTP_CACHE(file_path);
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(cache_file,false,FileObjectType.FILE_TYPE);
                }
                else
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,FileObjectType.FILE_TYPE);
                }

                try {

                    long file_size;
                    if(fromThirdPartyApp)
                    {
                        pdfRenderer = new PdfRenderer(application.getContentResolver().openFileDescriptor(data,"r"));
                    }
                    else if(fileObjectType==FileObjectType.FILE_TYPE)
                    {
                        pdfRenderer = new PdfRenderer(ParcelFileDescriptor.open(new File(currently_shown_file.getPath()), ParcelFileDescriptor.MODE_READ_ONLY));
                    }
                    else if(fileObjectType==FileObjectType.USB_TYPE)
                    {
                        pdfRenderer = new PdfRenderer(ParcelFileDescriptor.open(new File(currently_shown_file.getPath()), ParcelFileDescriptor.MODE_READ_ONLY));
                    }
                    else if(fileObjectType==FileObjectType.ROOT_TYPE)
                    {
                        pdfRenderer = new PdfRenderer(ParcelFileDescriptor.open(new File(currently_shown_file.getPath()), ParcelFileDescriptor.MODE_READ_ONLY));
                    }
                    else if(fileObjectType==FileObjectType.FTP_TYPE)
                    {
                        pdfRenderer = new PdfRenderer(ParcelFileDescriptor.open(new File(currently_shown_file.getPath()), ParcelFileDescriptor.MODE_READ_ONLY));
                    }


                    file_size=currently_shown_file.getSizeLong();
                    if(file_size==0)
                    {
                        file_size=Global.GET_URI_FILE_SIZE(data,application);
                    }

                    total_pages = pdfRenderer.getPageCount();

                    if(file_size!=0)
                    {
                        size_per_page_MB=(double)file_size/total_pages/1024/1024;
                    }


                }
                catch (SecurityException e)
                {
                    Global.print_background_thread(application,application.getString(R.string.security_exception_thrown)+" - "+application.getString(R.string.may_be_password_protected));
                }
                catch (IOException e) {
                    Global.print_background_thread(application,application.getString(R.string.file_not_in_PDF_format_or_corrupted));
                }

                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });

    }


    public void rotate(Uri tree_uri, String tree_uri_path) {
    if (isRotated.getValue() != AsyncTaskStatus.NOT_YET_STARTED) return;
    isRotated.setValue(AsyncTaskStatus.STARTED);
    ExecutorService executorService = MyExecutorService.getExecutorService();
    future5 = executorService.submit(new Runnable() {
        @Override
        public void run() {
            double size= (double) currently_shown_file.getSizeLong() /1024 / 1024;
            if(size*5<(Global.AVAILABLE_MEMORY_MB()- PdfViewFragment.SAFE_MEMORY_BUFFER)){
                String file_path = currently_shown_file.getPath();
                String name=currently_shown_file.getName();
                long bytes_read=0;
                File file = new File(file_path);
                String parent_path = file.getParent();

                try {
                    Bitmap originalBitmap = BitmapFactory.decodeFile(file_path);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0,
                            originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);


                    File tempFile = new File(Global.TEMP_ROTATE_CACHE_DIR, name);
                    FileOutputStream out = new FileOutputStream(tempFile);
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();

                    if (tempFile.exists() && tempFile.length() > 0) {
                        if(FileUtil.isWritable(currently_shown_file.getFileObjectType(),file_path)){
                            FileUtil.copy_File_File(tempFile, file,true, bytes_read);
                        }
                        else{
                            FileUtil.copy_File_SAFFile(App.getAppContext(),tempFile,parent_path,name,tree_uri,tree_uri_path,true,bytes_read);
                        }

                    }

                    originalBitmap.recycle();
                    rotatedBitmap.recycle();
                } catch (Exception e) {

                }
            }
            isRotated.postValue(AsyncTaskStatus.COMPLETED);
        }
    });
}


    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
