package svl.kadatha.filex;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import me.jahnen.libaums.core.fs.UsbFile;

public class ViewModelFileCount extends ViewModel {

    MutableLiveData<Integer> total_no_of_files=new MutableLiveData<>();
    private long total_size_of_files;
    private int cumulative_no_of_files;
    MutableLiveData<String> size_of_files_formatted=new MutableLiveData<>();
    private Future<?> future;
    private boolean isCancelled;
    private boolean alreadyRun;
    @Override
    protected void onCleared() {
        super.onCleared();
        cancel(true);
    }

    private void cancel(boolean mayInterruptRunning){
        if(future!=null) future.cancel(mayInterruptRunning);
        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }

    public synchronized void count(String source_folder, FileObjectType sourceFileObjectType, ArrayList<String> source_list_files , int size, boolean include_folder)
    {
        if(alreadyRun) return;
        alreadyRun=true;
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future=executorService.submit(new Runnable() {
            @Override
            public void run() {

                Global.SET_OTHER_FILE_PERMISSION("rwx",source_folder);
                String file_path=source_list_files.get(0);
                if(sourceFileObjectType==FileObjectType.FILE_TYPE || sourceFileObjectType==FileObjectType.ROOT_TYPE)
                {
                    File[] f_array=new File[size];
                    for(int i=0;i<size;++i)
                    {
                        File f=new File(source_list_files.get(i));
                        f_array[i]=f;
                    }
                    populate(f_array,include_folder);

                }
                else if(sourceFileObjectType== FileObjectType.USB_TYPE)
                {
                    UsbFile[] f_array=new UsbFile[size];
                    for(int i=0;i<size;++i)
                    {
                        UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,source_list_files.get(i));
                        f_array[i]=f;
                    }
                    populate(f_array,include_folder);
                }
                else if(sourceFileObjectType== FileObjectType.SEARCH_LIBRARY_TYPE)
                {
                    File[] f_array=new File[size];
                    for(int i=0;i<size;++i)
                    {
                        File f=new File(source_list_files.get(i));
                        f_array[i]=f;
                    }
                    populate(f_array,include_folder);

                }
                else if(sourceFileObjectType==FileObjectType.FTP_TYPE)
                {
                    FTPFile[] f_array=new FTPFile[size];
                    for(int i=0;i<size;++i)
                    {

                        FTPFile f = FileUtil.getFTPFile(source_list_files.get(i));//MainActivity.FTP_CLIENT.mlistFile(source_list_files.get(i));
                        f_array[i]=f;

                    }
                    populate(f_array,include_folder,source_folder);

                }

            }
        });
    }


    private void populate(File[] source_list_files, boolean include_folder)
    {
        int size=source_list_files.length;
        for(int i=0;i<size;++i)
        {
            File f=source_list_files[i];
            if(isCancelled())
            {
                return;
            }
            int no_of_files=0;
            long size_of_files=0L;
            if(f.isDirectory())
            {
                if(f.list()!=null)
                {
                    populate(f.listFiles(),include_folder);
                }
                if(include_folder)
                {
                    no_of_files++;
                }
            }
            else
            {
                no_of_files++;
                size_of_files+=f.length();
            }
            cumulative_no_of_files+=no_of_files;
            total_no_of_files.postValue(cumulative_no_of_files);
            total_size_of_files+=size_of_files;
            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files,Global.BYTE_COUNT_BLOCK_1000));
        }
    }

    private void populate(UsbFile[] source_list_files, boolean include_folder)
    {
        int size=source_list_files.length;
        for(int i=0;i<size;++i)
        {
            UsbFile f=source_list_files[i];
            if(isCancelled())
            {
                return;
            }
            int no_of_files=0;
            long size_of_files=0L;
            if(f.isDirectory())
            {
                try {
                    populate(f.listFiles(),include_folder);
                } catch (IOException e) {

                }
                if(include_folder)
                {
                    no_of_files++;
                }
            }
            else
            {
                no_of_files++;
                size_of_files+=f.getLength();
            }
            cumulative_no_of_files+=no_of_files;
            total_no_of_files.postValue(cumulative_no_of_files);
            total_size_of_files+=size_of_files;
            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files,Global.BYTE_COUNT_BLOCK_1000));
        }
    }

    private void populate(FTPFile[] source_list_files, boolean include_folder, String path)
    {
        int size=source_list_files.length;
        for(int i=0;i<size;++i)
        {
            FTPFile f=source_list_files[i];
            if(isCancelled())
            {
                return;
            }
            int no_of_files=0;
            long size_of_files=0L;
            if(f.isDirectory())
            {
                try {
                    String name=f.getName();
                    path=(path.endsWith(File.separator)) ? path+name : path+File.separator+name;
                    populate(MainActivity.FTP_CLIENT.listFiles(path),include_folder,path);
                } catch (IOException e) {

                }
                if(include_folder)
                {
                    no_of_files++;
                }
            }
            else
            {
                no_of_files++;
                size_of_files+=f.getSize();
            }
            cumulative_no_of_files+=no_of_files;
            total_no_of_files.postValue(cumulative_no_of_files);
            total_size_of_files+=size_of_files;
            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files,Global.BYTE_COUNT_BLOCK_1000));
        }
    }

/*
    private void populate(List<String> source_list_files, boolean include_folder)
    {
        int size=source_list_files.size();
        for(int i=0;i<size;++i)
        {
            if(isCancelled())
            {
                return;
            }
            int no_of_files=0;
            long size_of_files=0L;
            String parent_file_path=source_list_files.get(i);
            Uri uri=FileUtil.getDocumentUri(parent_file_path,tree_uri,tree_uri_path);
            if(FileUtil.isDirectory(context,uri))
            {
                Uri children_uri= DocumentsContract.buildChildDocumentsUriUsingTree(tree_uri,FileUtil.getDocumentID(parent_file_path,tree_uri,tree_uri_path));
                Cursor cursor=context.getContentResolver().query(children_uri,new String[] {DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null);
                if(cursor!=null && cursor.getCount()>0)
                {
                    List<String>inner_source_list_files=new ArrayList<>();
                    while(cursor.moveToNext())
                    {

                        String docID=cursor.getString(0);
                        String displayName=cursor.getString(1);
                        inner_source_list_files.add(parent_file_path+File.separator+displayName);

                    }
                    cursor.close();
                    populate(inner_source_list_files,include_folder);

                }

                if(include_folder)

                {
                    no_of_files++;
                }
            }
            else
            {
                no_of_files++;
                size_of_files+=FileUtil.getSize(context,uri);
            }
            cumulative_no_of_files+=no_of_files;
            total_no_of_files.postValue(cumulative_no_of_files);
            total_size_of_files+=size_of_files;
            size_of_files_formatted.postValue(FileUtil.humanReadableByteCount(total_size_of_files,Global.BYTE_COUNT_BLOCK_1000));
            //publishProgress();
        }
    }

 */


}
