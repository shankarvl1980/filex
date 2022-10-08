package svl.kadatha.filex;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;

import me.jahnen.libaums.core.fs.UsbFile;
import me.jahnen.libaums.core.fs.UsbFileOutputStream;

public class ArchiveDeletePasteServiceUtil {

    static Class getEmptyService(Context context)
    {
        Class emptyService=null;

        if(ArchiveDeletePasteFileService1.SERVICE_COMPLETED)
        {
            emptyService=ArchiveDeletePasteProgressActivity1.class;
        }
        else if(ArchiveDeletePasteFileService2.SERVICE_COMPLETED)
        {
            emptyService=ArchiveDeletePasteProgressActivity2.class;
        }
        else if(ArchiveDeletePasteFileService3.SERVICE_COMPLETED)

        {
            emptyService=ArchiveDeletePasteProgressActivity3.class;
        }
        if(emptyService!=null)
        {
            AppCompatActivity appCompatActivity=(AppCompatActivity)context;
            if(appCompatActivity instanceof MainActivity)
            {
                ((MainActivity)context).clear_cache=false;

            }
            else if(appCompatActivity instanceof StorageAnalyserActivity)
            {
                ((StorageAnalyserActivity)context).clear_cache=false;
            }
        }

        return emptyService;
    }

    public static void CLEAR_CACHE_AND_REFRESH(String file_path, FileObjectType fileObjectType)
    {
        DetailFragment df = null;
        FileSelectorDialog fileSelectorDialog = null;
        StorageAnalyserDialog storageAnalyserDialog = null;

        if(MainActivity.FM!=null) df=(DetailFragment) MainActivity.FM.findFragmentById(R.id.detail_fragment);
        if(FileSelectorActivity.FM!=null) fileSelectorDialog=(FileSelectorDialog)FileSelectorActivity.FM.findFragmentById(R.id.file_selector_container);
        if(StorageAnalyserActivity.FM!=null) storageAnalyserDialog=(StorageAnalyserDialog)StorageAnalyserActivity.FM.findFragmentById(R.id.storage_analyser_container);


        if(df!=null) df.adapter.clear_cache_and_refresh(file_path,fileObjectType);
        if(fileSelectorDialog!=null) fileSelectorDialog.clear_cache_and_refresh(file_path,fileObjectType);
        if(storageAnalyserDialog!=null) storageAnalyserDialog.clear_cache_and_refresh(file_path,fileObjectType);
    }

    public static void NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(String source_folder,FileObjectType fileObjectType)
    {
        DetailFragment df = null;
        FileSelectorDialog fileSelectorDialog = null;
        StorageAnalyserDialog storageAnalyserDialog = null;

        if(MainActivity.FM!=null) df=(DetailFragment) MainActivity.FM.findFragmentById(R.id.detail_fragment);
        if(FileSelectorActivity.FM!=null) fileSelectorDialog=(FileSelectorDialog)FileSelectorActivity.FM.findFragmentById(R.id.file_selector_container);
        if(StorageAnalyserActivity.FM!=null) storageAnalyserDialog=(StorageAnalyserDialog)StorageAnalyserActivity.FM.findFragmentById(R.id.storage_analyser_container);


        String parent_source_folder=new File(source_folder).getParent();
        if(parent_source_folder==null) parent_source_folder=source_folder;

        if(df!=null && df.fileObjectType==fileObjectType)
        {
            String tag=df.getTag();
            if(Global.IS_CHILD_FILE(tag,parent_source_folder))
            {
                df.clearSelectionAndNotifyDataSetChanged();
            }
        }

        if(fileSelectorDialog!=null && fileSelectorDialog.fileObjectType==fileObjectType)
        {
            String tag=fileSelectorDialog.getTag();
            if(Global.IS_CHILD_FILE(tag,parent_source_folder))
            {
                fileSelectorDialog.clearSelectionAndNotifyDataSetChanged();
            }

        }

        if(storageAnalyserDialog!=null && storageAnalyserDialog.fileObjectType==fileObjectType)
        {
            String tag=storageAnalyserDialog.getTag();
            if(Global.IS_CHILD_FILE(tag,parent_source_folder))
            {
                storageAnalyserDialog.clearSelectionAndNotifyDataSetChanged();
            }
            if(df!=null)df.local_activity_delete=true; //to avoid modification observed which causes re-populate of filepojos
        }

    }

    public static void NOTIFY_ALL_DIALOG_FRAGMENTS_ON_CUT_COPY(String dest_folder,String source_folder, FileObjectType destFileObjectType, FileObjectType sourceFileObjectType,FilePOJO filePOJO)
    {
        DetailFragment df = null;
        FileSelectorDialog fileSelectorDialog = null;
        StorageAnalyserDialog storageAnalyserDialog = null;

        if(MainActivity.FM!=null) df=(DetailFragment) MainActivity.FM.findFragmentById(R.id.detail_fragment);
        if(FileSelectorActivity.FM!=null) fileSelectorDialog=(FileSelectorDialog)FileSelectorActivity.FM.findFragmentById(R.id.file_selector_container);
        if(StorageAnalyserActivity.FM!=null) storageAnalyserDialog=(StorageAnalyserDialog)StorageAnalyserActivity.FM.findFragmentById(R.id.storage_analyser_container);


        String parent_dest_folder= new File(dest_folder).getParent();
        if(parent_dest_folder==null) parent_dest_folder=dest_folder;

        String parent_source_folder= new File(source_folder).getParent();
        if(parent_source_folder==null) parent_source_folder=source_folder;

        if(df!=null)
        {
            String tag=df.getTag();
            if(tag.equals(dest_folder) && df.fileObjectType==destFileObjectType)
            {
                Collections.sort(df.filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));
                df.clearSelectionAndNotifyDataSetChanged();
                int idx=df.filePOJO_list.indexOf(filePOJO);
                if(df.llm!=null)
                {
                    df.llm.scrollToPositionWithOffset(idx,0);
                }
                else if(df.glm!=null)
                {
                    df.glm.scrollToPositionWithOffset(idx,0);
                }

            }
            else if (Global.IS_CHILD_FILE(tag,parent_dest_folder) && df.fileObjectType==destFileObjectType)
            {
                df.clearSelectionAndNotifyDataSetChanged();
            }

            // in case of cut, to take care of instances of destfolder is also parent of source folder, it is put in separate if block
            if(Global.IS_CHILD_FILE(tag,parent_source_folder) && df.fileObjectType==sourceFileObjectType)
            {
                Collections.sort(df.filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));
                df.clearSelectionAndNotifyDataSetChanged();
            }
        }

        if(fileSelectorDialog!=null )
        {
            String tag=fileSelectorDialog.getTag();

            if (Global.IS_CHILD_FILE(tag,parent_dest_folder) && fileSelectorDialog.fileObjectType==destFileObjectType)
            {
                fileSelectorDialog.clearSelectionAndNotifyDataSetChanged();
            }

            // in case of cut, to take care of instances of destfolder is also parent of source folder, it is put in separate if block
            if(Global.IS_CHILD_FILE(tag,parent_source_folder) && fileSelectorDialog.fileObjectType==sourceFileObjectType)
            {
                Collections.sort(fileSelectorDialog.filePOJO_list,FileComparator.FilePOJOComparate(FileSelectorActivity.SORT,false));
                fileSelectorDialog.clearSelectionAndNotifyDataSetChanged();
            }
        }

        if(storageAnalyserDialog!=null)
        {
            String tag=storageAnalyserDialog.getTag();

            if (Global.IS_CHILD_FILE(tag,parent_dest_folder) && storageAnalyserDialog.fileObjectType==destFileObjectType)
            {
                storageAnalyserDialog.clearSelectionAndNotifyDataSetChanged();
            }

            // in case of cut, to take care of instances of destfolder is also parent of source folder, it is put in separate if block
            if(Global.IS_CHILD_FILE(tag,parent_source_folder) && storageAnalyserDialog.fileObjectType==sourceFileObjectType)
            {
                Collections.sort(storageAnalyserDialog.filePOJO_list,FileComparator.FilePOJOComparate(Global.STORAGE_ANALYSER_SORT,true));
                storageAnalyserDialog.clearSelectionAndNotifyDataSetChanged();
            }
        }

    }


    public static void NOTIFY_ALL_DIALOG_FRAGMENTS_ON_ARCHIVE_UNARCHIVE_COMPLETE(String dest_folder, FileObjectType destFileObjectType,FilePOJO filePOJO)
    {
        DetailFragment df = null;
        FileSelectorDialog fileSelectorDialog = null;
        StorageAnalyserDialog storageAnalyserDialog = null;

        if(MainActivity.FM!=null) df=(DetailFragment) MainActivity.FM.findFragmentById(R.id.detail_fragment);
        if(FileSelectorActivity.FM!=null) fileSelectorDialog=(FileSelectorDialog)FileSelectorActivity.FM.findFragmentById(R.id.file_selector_container);
        if(StorageAnalyserActivity.FM!=null) storageAnalyserDialog=(StorageAnalyserDialog)StorageAnalyserActivity.FM.findFragmentById(R.id.storage_analyser_container);

        String parent_dest_folder= new File(dest_folder).getParent();
        if(parent_dest_folder==null) parent_dest_folder=dest_folder;
        if(df!=null)
        {
            if(Global.AFTER_ARCHIVE_GOTO_DEST_FOLDER)
            {
                Collections.sort(df.filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));
                DetailFragment.TO_BE_MOVED_TO_FILE_POJO=filePOJO;

                if(destFileObjectType== FileObjectType.FILE_TYPE)
                {
                    df.mainActivity.createFragmentTransaction(dest_folder,FileObjectType.FILE_TYPE);
                }
                else if(destFileObjectType== FileObjectType.USB_TYPE && MainActivity.usbFileRoot!=null)
                {
                    df.mainActivity.createFragmentTransaction(dest_folder,FileObjectType.USB_TYPE);
                }
            }
            else
            {

                String tag=df.getTag();

                if(Global.IS_CHILD_FILE(tag,parent_dest_folder)  && df.fileObjectType==destFileObjectType)
                {
                    Collections.sort(df.filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));
                    df.clearSelectionAndNotifyDataSetChanged();
                    int idx=df.filePOJO_list.indexOf(filePOJO);
                    if(df.llm!=null)
                    {
                        df.llm.scrollToPositionWithOffset(idx,0);
                    }
                    else if(df.glm!=null)
                    {
                        df.glm.scrollToPositionWithOffset(idx,0);
                    }

                }
            }
        }

        if(fileSelectorDialog!=null && fileSelectorDialog.fileObjectType==destFileObjectType)
        {
            String tag=fileSelectorDialog.getTag();

            if(Global.IS_CHILD_FILE(tag,parent_dest_folder))
            {
                Collections.sort(fileSelectorDialog.filePOJO_list,FileComparator.FilePOJOComparate(FileSelectorActivity.SORT,false));
                fileSelectorDialog.clearSelectionAndNotifyDataSetChanged();
            }
        }

        if(storageAnalyserDialog!=null && storageAnalyserDialog.fileObjectType==destFileObjectType)
        {
            String tag=storageAnalyserDialog.getTag();

            if(Global.IS_CHILD_FILE(tag,parent_dest_folder))
            {
                Collections.sort(storageAnalyserDialog.filePOJO_list,FileComparator.FilePOJOComparate(Global.STORAGE_ANALYSER_SORT,true));
                fileSelectorDialog.clearSelectionAndNotifyDataSetChanged();
            }
        }
    }

    public static String ON_DELETE_ASYNCTASK_COMPLETE(Context context,int counter_no_files, String source_folder, FileObjectType sourceFileObjectType,
                                                      List<String> deleted_file_names, List<String> deleted_files_path_list, boolean cancelled,boolean storage_analyser_delete)
    {
        String notification_content;
        if(cancelled)
        {
            CLEAR_CACHE_AND_REFRESH(source_folder,sourceFileObjectType);
            return null;
        }
        if(counter_no_files>0)
        {
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(source_folder,sourceFileObjectType);
            notification_content=sourceFileObjectType.equals(FileObjectType.SEARCH_LIBRARY_TYPE) ? context.getString(R.string.deleted_selected_files) : context.getString(R.string.deleted_selected_files)+" "+source_folder;
            Global.WORKOUT_AVAILABLE_SPACE();
        }
        else
        {
            notification_content=sourceFileObjectType.equals(FileObjectType.SEARCH_LIBRARY_TYPE) ? context.getString(R.string.could_not_delete_selected_files) : context.getString(R.string.could_not_delete_selected_files)+" "+source_folder;
        }

        return notification_content;
    }




    public static String ON_CUT_COPY_ASYNCTASK_COMPLETE(Context context, int counter_no_files, String source_folder, String dest_folder,
                                                        FileObjectType sourceFileObjectType, FileObjectType destFileObjectType, FilePOJO filePOJO,
                                                        boolean cut, boolean cancelled)
    {
        String notification_content;

        if(cancelled)
        {
            CLEAR_CACHE_AND_REFRESH(dest_folder,destFileObjectType);  //for dest_folder
            if (cut) CLEAR_CACHE_AND_REFRESH(source_folder,sourceFileObjectType);  // for source_folder
            return null;
        }
        if(counter_no_files>0)
        {

            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_CUT_COPY(dest_folder,source_folder,destFileObjectType,sourceFileObjectType,filePOJO);
            notification_content=(cut ? context.getString(R.string.moved_selected_files)+" "+dest_folder : context.getString(R.string.copied_selected_files)+" "+dest_folder);
            Global.WORKOUT_AVAILABLE_SPACE();
        }
        else
        {
            notification_content=(cut ? context.getString(R.string.could_not_move_selected_files)+" "+dest_folder : context.getString(R.string.could_not_copy_selected_files)+" "+dest_folder);
        }
        return notification_content;
    }

    public static void ON_ARCHIVE_ASYNCTASK_CANCEL(Context context, String dest_folder, String zip_file_name, FileObjectType destFileObjectType, Uri tree_uri, String tree_uri_path, UsbFile zipUsbFile)
    {
        File f=new File(dest_folder,zip_file_name);
        if(destFileObjectType==FileObjectType.FILE_TYPE)
        {
            if(f.exists())
            {
                if (FileUtil.isWritable(destFileObjectType,f.getAbsolutePath()))
                {
                    FileUtil.deleteNativeDirectory(f);
                }
                else
                {
                    if (Global.IS_CHILD_FILE(dest_folder,tree_uri_path))
                    {
                        FileUtil.deleteSAFDirectory(context,f.getAbsolutePath(),tree_uri,tree_uri_path);
                    }

                }
            }
        }
        else if(destFileObjectType==FileObjectType.USB_TYPE)
        {
            if(zipUsbFile!=null)
            {
                FileUtil.deleteUsbDirectory(zipUsbFile);
            }
        }
        else
        {
            if(FileUtil.exists(context,f.getAbsolutePath(),tree_uri,tree_uri_path))
            {
                FileUtil.deleteSAFDirectory(context,f.getAbsolutePath(),tree_uri,tree_uri_path);
            }
        }

        NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(dest_folder,destFileObjectType);
        Global.WORKOUT_AVAILABLE_SPACE();
    }

    public static String ON_ARCHIVE_ASYNCTASK_COMPLETE(Context context,boolean result,String dest_folder,
                                                       String zip_file_name,FileObjectType destFileObjectType)
    {
        String notification_content;
        if(result)
        {
            FilePOJO filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder, Collections.singletonList(zip_file_name),destFileObjectType, Collections.singletonList(Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder,zip_file_name)));
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_ARCHIVE_UNARCHIVE_COMPLETE(dest_folder,destFileObjectType,filePOJO);
            notification_content=context.getString(R.string.created)+" '"+zip_file_name+"' "+context.getString(R.string.at)+" "+dest_folder;
            Global.WORKOUT_AVAILABLE_SPACE();
        }
        else
        {
            notification_content=context.getString(R.string.could_not_create)+" '"+zip_file_name+"'";
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(dest_folder,destFileObjectType);
        }
        return notification_content;
    }

    public static String ON_UNARCHIVE_ASYNCTASK_COMPLETE(Context context,int counter_no_files, String dest_folder,
                                                         List<String> written_file_name_list, FileObjectType destFileObjectType,
                                                         List<String> written_file_path_list, String zip_file_path, boolean cancelled)
    {
        String notification_content;
        if(cancelled)
        {
            CLEAR_CACHE_AND_REFRESH(dest_folder,destFileObjectType);
            return null;
        }
        if (counter_no_files>0)
        {

            FilePOJO filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder,written_file_name_list,destFileObjectType,written_file_path_list);
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_ARCHIVE_UNARCHIVE_COMPLETE(dest_folder,destFileObjectType,filePOJO);
            notification_content=context.getString(R.string.unzipped)+" '"+new File(zip_file_path).getName()+"' "+context.getString(R.string.at)+" "+dest_folder;
            Global.WORKOUT_AVAILABLE_SPACE();

        }
        else
        {
            notification_content=context.getString(R.string.could_not_extract)+" '"+new File(zip_file_path).getName()+"'";
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(dest_folder,destFileObjectType);
        }
        return notification_content;

    }

    public static String UNARCHIVE(Context context, String zip_dest_path, ZipEntry zipEntry, boolean isWritable,
                   FileObjectType destFileObjectType, Uri uri, String uri_path, InputStream zipInputStream

    ) throws IOException {
        String zip_entry_name=zipEntry.getName();
        String dest_file_path=Global.CONCATENATE_PARENT_CHILD_PATH(zip_dest_path,zip_entry_name);
        File dest_file=new File(dest_file_path);

        if(zipEntry.isDirectory())
        {
            if(destFileObjectType==FileObjectType.FILE_TYPE)
            {
                if(isWritable)
                {
                    FileUtil.mkdirsNative(dest_file);
                }
                else
                {

                    FileUtil.mkdirsSAFFile(context,zip_dest_path,zip_entry_name,uri,uri_path);
                }
            }
            else if(destFileObjectType== FileObjectType.USB_TYPE)
            {
                FileUtil.mkdirsUsb(zip_dest_path,zip_entry_name);
            }
            else
            {
                FileUtil.mkdirsSAFD(context,dest_file_path,null,uri,uri_path);
            }

        }
        else if(!zipEntry.isDirectory())
        {
            File parent_dest_file=dest_file.getParentFile();
            String parent_dest_file_path=parent_dest_file.getAbsolutePath();


            boolean parent_dir_exists;
            if(destFileObjectType==FileObjectType.FILE_TYPE)
            {
                parent_dir_exists=parent_dest_file.exists();
            }
            else if(destFileObjectType==FileObjectType.USB_TYPE)
            {
                UsbFile usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,parent_dest_file_path);
                parent_dir_exists= (usbFile != null);

            }
            else
            {
                parent_dir_exists=FileUtil.exists(context,parent_dest_file_path,uri,uri_path);
            }
            if(!parent_dir_exists)
            {

                if(destFileObjectType==FileObjectType.FILE_TYPE)
                {
                    if(isWritable)
                    {
                        FileUtil.mkdirsNative(parent_dest_file);
                    }
                    else
                    {
                        String zip_entry_parent=new File(zip_entry_name).getParent();
                        FileUtil.mkdirsSAFFile(context,zip_dest_path,zip_entry_parent,uri,uri_path);

                    }
                }
                else
                {
                    String zip_entry_parent=new File(zip_entry_name).getParent();
                    if(destFileObjectType==FileObjectType.USB_TYPE)
                    {
                        FileUtil.mkdirsUsb(zip_dest_path,zip_entry_parent);
                    }
                    else
                    {
                        FileUtil.mkdirsSAFD(context,parent_dest_file_path,null,uri,uri_path);
                    }
                }

            }

            OutputStream outStream=null;

            if(destFileObjectType==FileObjectType.FILE_TYPE)
            {
                if(isWritable)
                {
                    outStream=new FileOutputStream(dest_file);
                }
                else
                {
                    Uri zipuri = FileUtil.createDocumentUri(context,parent_dest_file_path,dest_file.getName(),false,uri,uri_path);
                    if (zipuri != null)
                    {
                        outStream = context.getContentResolver().openOutputStream(zipuri);
                    }
                }
            }
            else if(destFileObjectType==FileObjectType.USB_TYPE)
            {
                UsbFile usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,parent_dest_file_path);
                if(usbFile!=null)
                {
                    String name=dest_file.getName();
                    UsbFile targetUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,Global.CONCATENATE_PARENT_CHILD_PATH(parent_dest_file_path,name));
                    if(targetUsbFile!=null) FileUtil.deleteUsbFile(targetUsbFile);
                    UsbFile childUsbFile=usbFile.createFile(name);
                    if(!childUsbFile.isDirectory()) outStream=new UsbFileOutputStream(childUsbFile);
                }
            }

            if(outStream!=null)
            {
                BufferedOutputStream bufferedOutStream=new BufferedOutputStream(outStream);
                byte[] b=new byte[FileUtil.BUFFER_SIZE];
                int bytesread;
                while((bytesread=zipInputStream.read(b))!=-1)
                {
                    bufferedOutStream.write(b,0,bytesread);
                }
                bufferedOutStream.close();

            }

        }

        return  zip_entry_name;
    }



    public static class FileCountSize
    {
        final Context context;
        final List<String> files_selected_array;
        final Uri target_uri;
        final String target_uri_path;
        final boolean include_folder;
        final FileObjectType sourceFileObjectType;
        int total_no_of_files;
        long total_size_of_files;
        final MutableLiveData<String> mutable_size_of_files_to_be_archived_copied=new MutableLiveData<>();
        String source_folder;
        private boolean isCancelled;
        private Future<?> future1,future2,future3, future4;


        FileCountSize(Context context,List<String> files_selected_array, Uri source_uri,String source_uri_path, FileObjectType sourceFileObjectType)
        {
            this.context=context;
            this.files_selected_array=files_selected_array;
            this.include_folder= true;
            this.target_uri= source_uri;
            this.target_uri_path= source_uri_path;
            this.sourceFileObjectType=sourceFileObjectType;

        }


        public void cancel(boolean mayInterruptRunning){
            if(future1!=null) future1.cancel(mayInterruptRunning);
            if(future2!=null) future2.cancel(mayInterruptRunning);
            if(future3!=null) future3.cancel(mayInterruptRunning);
            if(future4!=null) future4.cancel(mayInterruptRunning);
            isCancelled=true;
        }

        private boolean isCancelled()
        {
            return isCancelled;
        }

        public void fileCount()
        {
            ExecutorService executorService=MyExecutorService.getExecutorService();
            future1=executorService.submit(new Runnable() {
                @Override
                public void run() {
                    source_folder=new File(files_selected_array.get(0)).getParent();

                    int size=files_selected_array.size();

                    if(sourceFileObjectType==FileObjectType.FILE_TYPE || sourceFileObjectType== FileObjectType.SEARCH_LIBRARY_TYPE || sourceFileObjectType==FileObjectType.ROOT_TYPE)
                    {
                        File[] f_array=new File[size];
                        for(int i=0;i<size;++i)
                        {
                            File f=new File(files_selected_array.get(i));
                            f_array[i]=f;
                        }
                        populate(f_array,include_folder);
                    }
                    else if(sourceFileObjectType== FileObjectType.USB_TYPE)
                    {
                        UsbFile[] f_array=new UsbFile[size];
                        for(int i=0;i<size;++i)
                        {
                            UsbFile f=FileUtil.getUsbFile(MainActivity.usbFileRoot,files_selected_array.get(i));
                            f_array[i]=f;
                        }
                        populate(f_array,include_folder);
                    }
                    else if(sourceFileObjectType==FileObjectType.FTP_TYPE)
                    {
                        FTPFile[] f_array=new FTPFile[size];
                        for(int i=0;i<size;++i)
                        {

                            FTPFile f = FileUtil.getFTPFile(files_selected_array.get(i));//MainActivity.FTP_CLIENT.mlistFile(files_selected_array.get(i));
                            f_array[i]=f;
                        }
                        populate(f_array,include_folder,source_folder);
                    }
                    else
                    {
                        populate(files_selected_array,include_folder);
                    }


                }
            });
        }


        private void populate(File[] source_list_files,boolean include_folder)
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
                total_no_of_files+=no_of_files;
                total_size_of_files+=size_of_files;

                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
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
                total_no_of_files+=no_of_files;
                total_size_of_files+=size_of_files;
                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
            }
        }

        private void populate(FTPFile[] source_list_files, boolean include_folder,String path)
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
                if(f==null)continue;
                if(f.isDirectory())
                {
                    try {
                        String name=f.getName();
                        path=Global.CONCATENATE_PARENT_CHILD_PATH(path,name);
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
                total_no_of_files+=no_of_files;
                total_size_of_files+=size_of_files;
                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
            }
        }

        private void populate(List<String> source_list_files,boolean include_folder)
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
                Uri uri=FileUtil.getDocumentUri(parent_file_path,target_uri,target_uri_path);
                if(FileUtil.isDirectory(context,uri))
                {
                    Uri children_uri= DocumentsContract.buildChildDocumentsUriUsingTree(target_uri,FileUtil.getDocumentID(parent_file_path,target_uri,target_uri_path));
                    Cursor cursor=context.getContentResolver().query(children_uri,new String[] {DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null);
                    if(cursor!=null && cursor.getCount()>0)
                    {
                        List<String>inner_source_list_files=new ArrayList<>();
                        while(cursor.moveToNext())
                        {
                            String displayName=cursor.getString(0);
                            inner_source_list_files.add(Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,displayName));
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
                total_no_of_files+=no_of_files;
                total_size_of_files+=size_of_files;
                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));

            }
        }

    }

}