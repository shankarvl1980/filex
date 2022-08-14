package svl.kadatha.filex;

import android.app.Application;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.SparseBooleanArray;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FilteredFilePOJOViewModel extends AndroidViewModel {
    private final Application application;
    private boolean isCancelled;
    private Future<?> future1,future2,future3,future4, future5;
    public MutableLiveData<Boolean> isFinished=new MutableLiveData<>();
    public MutableLiveData<Boolean> hasWallPaperSet=new MutableLiveData<>();
    public final List<FilePOJO> album_file_pojo_list=new ArrayList<>();
    public final IndexedLinkedHashMap<FilePOJO,Integer> video_list=new IndexedLinkedHashMap<>();
    public int total_images;
    public SparseBooleanArray selected_item_sparseboolean=new SparseBooleanArray();
    public int file_selected_idx=0;
    public PdfRenderer pdfRenderer;
    public double size_per_page_MB;
    public int total_pages;
    public Bitmap bitmap;
    public boolean out_of_memory_exception_thrown;
    public MutableLiveData<Boolean> isPdfBitmapFetched=new MutableLiveData<>();
    public int image_selected_idx=0,previously_selected_image_idx=0,pdf_current_position;
    public String source_folder,file_path;
    public FilePOJO currently_shown_file;
    public boolean firststart;


    public FilteredFilePOJOViewModel(@NonNull Application application) {
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
        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }


    public synchronized void getAlbumFromCurrentFolder(FileObjectType fileObjectType,String file_path,String regex, boolean fromArchiveView, boolean fromThirdPartyApp, boolean whetherVideo )
    {
        if(Boolean.TRUE.equals(isFinished.getValue())) return;
        firststart=true;
        this.file_path=file_path;
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
                        try {
                            currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(file_path)),false);
                        } catch (IOException e) {

                        }
                    }
                }
                else if(fileObjectType==FileObjectType.ROOT_TYPE)
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,false,FileObjectType.FILE_TYPE);
                }
                else
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,false,FileObjectType.FILE_TYPE);
                }


                List<FilePOJO> filePOJOS=new ArrayList<>(), filePOJOS_filtered=new ArrayList<>();
                if (!Global.HASHMAP_FILE_POJO.containsKey(fileObjectType+ finalSource_folder))
                {
                    FilePOJOUtil.FILL_FILEPOJO(filePOJOS,filePOJOS_filtered,fileObjectType, finalSource_folder,null,false);
                }
                else
                {
                    if(MainActivity.SHOW_HIDDEN_FILE)
                    {
                        filePOJOS=Global.HASHMAP_FILE_POJO.get(fileObjectType+ finalSource_folder) ;
                    }
                    else
                    {
                        filePOJOS=Global.HASHMAP_FILE_POJO_FILTERED.get(fileObjectType+ finalSource_folder);
                    }
                }

                // limiting to the selected only, in case of file selected from usb storage by adding condition below
                if(fromArchiveView || fromThirdPartyApp || fileObjectType==FileObjectType.USB_TYPE)
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

                isFinished.postValue(true);
            }
        });
    }

    public void setWallPaper(ActivityResult result,File temporaryDir)
    {
        if(Boolean.TRUE.equals(hasWallPaperSet.getValue())) return;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future2=executorService.submit(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {

                Uri uri=result.getData().getData();
                String file_name=result.getData().getStringExtra(InstaCropperActivity.EXTRA_FILE_NAME);
                File f=new File(temporaryDir,file_name);
                WallpaperManager wm= WallpaperManager.getInstance(application);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(wm.isWallpaperSupported() && wm.isSetWallpaperAllowed())
                        try
                        {
                            wm.setStream(application.getContentResolver().openInputStream(uri));
                            Global.print_background_thread(application,application.getString(R.string.set_as_wallpaper));
                        }
                        catch(IOException e){}
                        finally
                        {
                            if(f.exists())
                            {
                                f.delete();
                            }
                        }
                    else
                    {

                        if(f.exists())
                        {
                            f.delete();
                        }

                    }
                }
                hasWallPaperSet.postValue(true);
            }
        });
    }


    public void initializePdfRenderer(FileObjectType fileObjectType,String file_path,Uri data,boolean fromThirdPartyApp)
    {
        if(Boolean.TRUE.equals(isFinished.getValue())) return;
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
                        try {
                            currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(file_path)),false);
                        } catch (IOException e) {

                        }
                    }
                }
                else if(fileObjectType==FileObjectType.ROOT_TYPE)
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,false,FileObjectType.FILE_TYPE);
                }
                else
                {
                    currently_shown_file=FilePOJOUtil.MAKE_FilePOJO(new File(file_path),false,false,FileObjectType.FILE_TYPE);
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
                        pdfRenderer = new PdfRenderer(application.getContentResolver().openFileDescriptor(data,"r"));
                    }
                    else if(fileObjectType==FileObjectType.ROOT_TYPE)
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

                isFinished.postValue(true);
            }
        });

    }

    public synchronized void fetchBitmapFromPDF(int position)
    {
        if(Boolean.TRUE.equals(isPdfBitmapFetched.getValue())) return;
        ExecutorService executorService=Executors.newSingleThreadExecutor();//MyExecutorService.getExecutorService();
        future4=executorService.submit(new Runnable() {
            @Override
            public void run() {
                if(size_per_page_MB*10<(Global.AVAILABLE_MEMORY_MB()-PdfViewFragment_single_view.SAFE_MEMORY_BUFFER)) {
                    pdf_current_position=position;
                    try {
                        bitmap=getBitmap(pdfRenderer,position);
                    }
                    catch (SecurityException e)
                    {
                      Global.print_background_thread(application,application.getString(R.string.security_exception_thrown));

                    }
                    catch (OutOfMemoryError error)
                    {
                        Global.print_background_thread(application,application.getString(R.string.outofmemory_exception_thrown));
                        out_of_memory_exception_thrown=true;
                    }
                    catch (Exception e)
                    {
                        Global.print_background_thread(application,application.getString(R.string.exception_thrown));

                    }
                    isPdfBitmapFetched.postValue(true);
                }
            }
        });
    }

    private Bitmap getBitmap(PdfRenderer pdfRenderer, int i)
    {
        PdfRenderer.Page page= pdfRenderer.openPage(i);
        Bitmap bitmap=Bitmap.createBitmap(page.getWidth(),page.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap,0f,0f,null);
        page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        page.close();
        return bitmap;
    }

}
