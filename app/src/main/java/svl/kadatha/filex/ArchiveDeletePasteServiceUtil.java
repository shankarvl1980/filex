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
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;

public class ArchiveDeletePasteServiceUtil {

    static Class getEmptyService(Context context)
    {
        Class emptyService=null;

        if(ArchiveDeletePasteFileService1.SERVICE_COMPLETED)
        {
            emptyService=ArchiveDeletePasteProgressActivity1.class;
        }
        else
            if(ArchiveDeletePasteFileService2.SERVICE_COMPLETED)
        {
            emptyService=ArchiveDeletePasteProgressActivity2.class;
        }
        else
            if(ArchiveDeletePasteFileService3.SERVICE_COMPLETED)

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
            else if(appCompatActivity instanceof CopyToActivity)
            {
                ((CopyToActivity)context).clear_cache=false;
            }
            else if(appCompatActivity instanceof ArchiveViewActivity)
            {
                ((ArchiveViewActivity)context).clear_cache=false;
            }
        }

        return emptyService;
    }


    private static boolean NO_OPERATION_ON_FILE_OBJECT_TYPE(FileObjectType fileObjectType)
    {
        boolean noOperation=true;
        if(noOperation && ArchiveDeletePasteFileService1.SOURCE_FILE_OBJECT!=null)
        {
            noOperation=ArchiveDeletePasteFileService1.SOURCE_FILE_OBJECT != fileObjectType;
        }

        if(noOperation && ArchiveDeletePasteFileService1.DEST_FILE_OBJECT!=null)
        {
            noOperation=ArchiveDeletePasteFileService1.DEST_FILE_OBJECT != fileObjectType;
        }

        if(noOperation && ArchiveDeletePasteFileService2.SOURCE_FILE_OBJECT!=null)
        {
            noOperation=ArchiveDeletePasteFileService2.SOURCE_FILE_OBJECT != fileObjectType;
        }

        if(noOperation && ArchiveDeletePasteFileService2.DEST_FILE_OBJECT!=null)
        {
            noOperation=ArchiveDeletePasteFileService2.DEST_FILE_OBJECT != fileObjectType;
        }

        if(noOperation && ArchiveDeletePasteFileService3.SOURCE_FILE_OBJECT!=null)
        {
            noOperation=ArchiveDeletePasteFileService3.SOURCE_FILE_OBJECT != fileObjectType;
        }

        if(noOperation && ArchiveDeletePasteFileService3.DEST_FILE_OBJECT!=null)
        {
            noOperation=ArchiveDeletePasteFileService3.DEST_FILE_OBJECT != fileObjectType;
        }

        return noOperation;
    }

    public static boolean WHETHER_TO_START_SERVICE_ON_USB(FileObjectType sourceFileObjectType, FileObjectType destFileObjectType)
    {
        boolean noOperation=true;
        if(noOperation && sourceFileObjectType!=null)
        {
            if(sourceFileObjectType==FileObjectType.USB_TYPE)
            {
                noOperation=ArchiveDeletePasteServiceUtil.NO_OPERATION_ON_FILE_OBJECT_TYPE(FileObjectType.USB_TYPE);
            }
        }

        if(noOperation && destFileObjectType!=null)
        {
            if(destFileObjectType==FileObjectType.USB_TYPE)
            {
                noOperation=ArchiveDeletePasteServiceUtil.NO_OPERATION_ON_FILE_OBJECT_TYPE(FileObjectType.USB_TYPE);
            }
        }

        return noOperation;
    }
    public static boolean WHETHER_TO_START_SERVICE_ON_FTP(FileObjectType sourceFileObjectType, FileObjectType destFileObjectType)
    {
        boolean noOperation=true;
        if(noOperation && sourceFileObjectType!=null)
        {
            if(sourceFileObjectType==FileObjectType.FTP_TYPE)
            {
                noOperation=ArchiveDeletePasteServiceUtil.NO_OPERATION_ON_FILE_OBJECT_TYPE(FileObjectType.FTP_TYPE);
            }
        }

        if(noOperation && destFileObjectType!=null)
        {
            if(destFileObjectType==FileObjectType.FTP_TYPE)
            {
                noOperation=ArchiveDeletePasteServiceUtil.NO_OPERATION_ON_FILE_OBJECT_TYPE(FileObjectType.FTP_TYPE);
            }
        }

        return noOperation;
    }

    public static void CLEAR_CACHE_AND_REFRESH(String file_path, FileObjectType fileObjectType)
    {
        DetailFragment df = null;
        FileSelectorFragment fileSelectorFragment = null;
        StorageAnalyserFragment storageAnalyserFragment = null;

        if(MainActivity.FM!=null) df=(DetailFragment) MainActivity.FM.findFragmentById(R.id.detail_fragment);
        if(FileSelectorActivity.FM!=null) fileSelectorFragment=(FileSelectorFragment)FileSelectorActivity.FM.findFragmentById(R.id.file_selector_container);
        if(StorageAnalyserActivity.FM!=null) storageAnalyserFragment =(StorageAnalyserFragment)StorageAnalyserActivity.FM.findFragmentById(R.id.storage_analyser_container);


        if(df!=null) df.adapter.clear_cache_and_refresh(file_path,fileObjectType);
        if(fileSelectorFragment!=null) fileSelectorFragment.clear_cache_and_refresh(file_path,fileObjectType);
        if(storageAnalyserFragment !=null) storageAnalyserFragment.clear_cache_and_refresh(file_path,fileObjectType);
    }

    public static void NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(String source_folder,FileObjectType fileObjectType)
    {
        DetailFragment df = null;
        FileSelectorFragment fileSelectorFragment = null;
        StorageAnalyserFragment storageAnalyserFragment = null;

        if(MainActivity.FM!=null) df=(DetailFragment) MainActivity.FM.findFragmentById(R.id.detail_fragment);
        if(FileSelectorActivity.FM!=null) fileSelectorFragment=(FileSelectorFragment)FileSelectorActivity.FM.findFragmentById(R.id.file_selector_container);
        if(StorageAnalyserActivity.FM!=null) storageAnalyserFragment =(StorageAnalyserFragment)StorageAnalyserActivity.FM.findFragmentById(R.id.storage_analyser_container);


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

        if(fileSelectorFragment!=null && fileSelectorFragment.fileObjectType==fileObjectType)
        {
            String tag=fileSelectorFragment.getTag();
            if(Global.IS_CHILD_FILE(tag,parent_source_folder))
            {
                fileSelectorFragment.clearSelectionAndNotifyDataSetChanged();
            }

        }

        if(storageAnalyserFragment !=null && storageAnalyserFragment.fileObjectType==fileObjectType)
        {
            String tag= storageAnalyserFragment.getTag();
            if(Global.IS_CHILD_FILE(tag,parent_source_folder))
            {
                storageAnalyserFragment.clearSelectionAndNotifyDataSetChanged();
            }
            if(df!=null)df.local_activity_delete=true; //to avoid modification observed which causes re-populate of filepojos
        }

    }

    public static void NOTIFY_ALL_DIALOG_FRAGMENTS_ON_CUT_COPY(String dest_folder,String source_folder, FileObjectType destFileObjectType, FileObjectType sourceFileObjectType,FilePOJO filePOJO)
    {
        DetailFragment df = null;
        FileSelectorFragment fileSelectorFragment = null;
        StorageAnalyserFragment storageAnalyserFragment = null;

        if(MainActivity.FM!=null) df=(DetailFragment) MainActivity.FM.findFragmentById(R.id.detail_fragment);
        if(FileSelectorActivity.FM!=null) fileSelectorFragment=(FileSelectorFragment)FileSelectorActivity.FM.findFragmentById(R.id.file_selector_container);
        if(StorageAnalyserActivity.FM!=null) storageAnalyserFragment =(StorageAnalyserFragment)StorageAnalyserActivity.FM.findFragmentById(R.id.storage_analyser_container);


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

        if(fileSelectorFragment!=null )
        {
            String tag=fileSelectorFragment.getTag();

            if (Global.IS_CHILD_FILE(tag,parent_dest_folder) && fileSelectorFragment.fileObjectType==destFileObjectType)
            {
                fileSelectorFragment.clearSelectionAndNotifyDataSetChanged();
            }

            // in case of cut, to take care of instances of destfolder is also parent of source folder, it is put in separate if block
            if(Global.IS_CHILD_FILE(tag,parent_source_folder) && fileSelectorFragment.fileObjectType==sourceFileObjectType)
            {
                Collections.sort(fileSelectorFragment.filePOJO_list,FileComparator.FilePOJOComparate(FileSelectorActivity.SORT,false));
                fileSelectorFragment.clearSelectionAndNotifyDataSetChanged();
            }
        }

        if(storageAnalyserFragment !=null)
        {
            String tag= storageAnalyserFragment.getTag();

            if (Global.IS_CHILD_FILE(tag,parent_dest_folder) && storageAnalyserFragment.fileObjectType==destFileObjectType)
            {
                storageAnalyserFragment.clearSelectionAndNotifyDataSetChanged();
            }

            // in case of cut, to take care of instances of destfolder is also parent of source folder, it is put in separate if block
            if(Global.IS_CHILD_FILE(tag,parent_source_folder) && storageAnalyserFragment.fileObjectType==sourceFileObjectType)
            {
                Collections.sort(storageAnalyserFragment.filePOJO_list,FileComparator.FilePOJOComparate(Global.STORAGE_ANALYSER_SORT,true));
                storageAnalyserFragment.clearSelectionAndNotifyDataSetChanged();
            }
        }
    }


    public static void NOTIFY_ALL_DIALOG_FRAGMENTS_ON_ARCHIVE_UNARCHIVE_COMPLETE(String dest_folder, FileObjectType destFileObjectType,FilePOJO filePOJO)
    {
        DetailFragment df = null;
        FileSelectorFragment fileSelectorFragment = null;
        StorageAnalyserFragment storageAnalyserFragment = null;

        if(MainActivity.FM!=null) df=(DetailFragment) MainActivity.FM.findFragmentById(R.id.detail_fragment);
        if(FileSelectorActivity.FM!=null) fileSelectorFragment=(FileSelectorFragment)FileSelectorActivity.FM.findFragmentById(R.id.file_selector_container);
        if(StorageAnalyserActivity.FM!=null) storageAnalyserFragment =(StorageAnalyserFragment)StorageAnalyserActivity.FM.findFragmentById(R.id.storage_analyser_container);

        String parent_dest_folder= new File(dest_folder).getParent();
        if(parent_dest_folder==null) parent_dest_folder=dest_folder;
        if(df!=null)
        {
            if(Global.AFTER_ARCHIVE_GOTO_DEST_FOLDER)
            {
                Collections.sort(df.filePOJO_list,FileComparator.FilePOJOComparate(Global.SORT,false));
                DetailFragment.TO_BE_MOVED_TO_FILE_POJO=filePOJO;

                if (df.detailFragmentListener != null) {

                    if(destFileObjectType== FileObjectType.FILE_TYPE)
                    {
                        df.detailFragmentListener.createFragmentTransaction(dest_folder,FileObjectType.FILE_TYPE);
                    }
                    else if(destFileObjectType== FileObjectType.USB_TYPE && MainActivity.usbFileRoot!=null)
                    {
                        df.detailFragmentListener.createFragmentTransaction(dest_folder,FileObjectType.USB_TYPE);
                    }
                    else if (destFileObjectType==FileObjectType.FTP_TYPE)
                    {
                        df.detailFragmentListener.createFragmentTransaction(dest_folder,FileObjectType.FTP_TYPE);
                    }
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

        if(fileSelectorFragment!=null && fileSelectorFragment.fileObjectType==destFileObjectType)
        {
            String tag=fileSelectorFragment.getTag();

            if(Global.IS_CHILD_FILE(tag,parent_dest_folder))
            {
                Collections.sort(fileSelectorFragment.filePOJO_list,FileComparator.FilePOJOComparate(FileSelectorActivity.SORT,false));
                fileSelectorFragment.clearSelectionAndNotifyDataSetChanged();
            }
        }

        if(storageAnalyserFragment !=null && storageAnalyserFragment.fileObjectType==destFileObjectType)
        {
            String tag= storageAnalyserFragment.getTag();

            if(Global.IS_CHILD_FILE(tag,parent_dest_folder))
            {
                Collections.sort(storageAnalyserFragment.filePOJO_list,FileComparator.FilePOJOComparate(Global.STORAGE_ANALYSER_SORT,true));
                fileSelectorFragment.clearSelectionAndNotifyDataSetChanged();
            }
        }
    }

    public static void NOTIFY_ALL_DIALOG_FRAGMENTS_ON_COPY_TO(String dest_folder, FileObjectType destFileObjectType,FilePOJO filePOJO)
    {
        DetailFragment df = null;
        FileSelectorFragment fileSelectorFragment = null;
        StorageAnalyserFragment storageAnalyserFragment = null;

        if(MainActivity.FM!=null) df=(DetailFragment) MainActivity.FM.findFragmentById(R.id.detail_fragment);
        if(FileSelectorActivity.FM!=null) fileSelectorFragment=(FileSelectorFragment)FileSelectorActivity.FM.findFragmentById(R.id.file_selector_container);
        if(StorageAnalyserActivity.FM!=null) storageAnalyserFragment =(StorageAnalyserFragment)StorageAnalyserActivity.FM.findFragmentById(R.id.storage_analyser_container);


        String parent_dest_folder= new File(dest_folder).getParent();
        if(parent_dest_folder==null) parent_dest_folder=dest_folder;


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
        }

        if(fileSelectorFragment!=null )
        {
            String tag=fileSelectorFragment.getTag();

            if (Global.IS_CHILD_FILE(tag,parent_dest_folder) && fileSelectorFragment.fileObjectType==destFileObjectType)
            {
                fileSelectorFragment.clearSelectionAndNotifyDataSetChanged();
            }
        }

        if(storageAnalyserFragment !=null)
        {
            String tag= storageAnalyserFragment.getTag();

            if (Global.IS_CHILD_FILE(tag,parent_dest_folder) && storageAnalyserFragment.fileObjectType==destFileObjectType)
            {
                storageAnalyserFragment.clearSelectionAndNotifyDataSetChanged();
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
        ExecutorService executorService=MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
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
                else if(destFileObjectType==FileObjectType.FTP_TYPE)
                {
                    //do not do any thing as it is on main thread
                }
                else
                {
                    if(FileUtil.existsUri(context,f.getAbsolutePath(),tree_uri,tree_uri_path))
                    {
                        FileUtil.deleteSAFDirectory(context,f.getAbsolutePath(),tree_uri,tree_uri_path);
                    }
                }


                Global.WORKOUT_AVAILABLE_SPACE();
            }
        });
        NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(dest_folder,destFileObjectType);
    }

    public static String ON_ARCHIVE_ASYNCTASK_COMPLETE(Context context,boolean result,FilePOJO filePOJO,String dest_folder,
                                                       String zip_file_name,FileObjectType destFileObjectType)
    {
        String notification_content;
        if(result)
        {
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


    public static String ON_UNARCHIVE_ASYNCTASK_COMPLETE(Context context, int counter_no_files,FilePOJO filePOJO, String dest_folder,
                                                         FileObjectType destFileObjectType,
                                                         String zip_file_path, boolean cancelled)
    {
        String notification_content;
        if(cancelled)
        {
            CLEAR_CACHE_AND_REFRESH(dest_folder,destFileObjectType);
            return null;
        }
        if (counter_no_files>0)
        {
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

    public static String ON_COPY_TO_ASYNCTASK_COMPLETE(Context context,boolean result,String dest_folder,
                                                       String file_name,FileObjectType destFileObjectType, FilePOJO filePOJO)
    {
        String notification_content;
        if(result)
        {
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_COPY_TO(dest_folder,destFileObjectType,filePOJO);
            notification_content=context.getString(R.string.created)+" '"+file_name+"' "+context.getString(R.string.at)+" "+dest_folder;
            Global.WORKOUT_AVAILABLE_SPACE();
        }
        else
        {
            notification_content=context.getString(R.string.could_not_create)+" '"+file_name+"'";
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(dest_folder,destFileObjectType);
        }
        return notification_content;
    }


    public static String UNARCHIVE(String zip_dest_path, ZipEntry zipEntry,FileObjectType destFileObjectType, Uri uri, String uri_path, InputStream zipInputStream

    ) throws IOException {
        String zip_entry_name=zipEntry.getName();
        String dest_file_path=Global.CONCATENATE_PARENT_CHILD_PATH(zip_dest_path,zip_entry_name);
        File dest_file=new File(dest_file_path);

        FileModel fileModel= FileModelFactory.getFileModel(zip_dest_path,destFileObjectType,uri,uri_path);

        if(zipEntry.isDirectory())
        {
            fileModel.makeDirsRecursively(zip_entry_name);
        }
        else if(!zipEntry.isDirectory())
        {
            File parent_dest_file=dest_file.getParentFile();
            String parent_dest_file_path=parent_dest_file.getAbsolutePath();

            FileModel zipEntryFileModel=FileModelFactory.getFileModel(parent_dest_file_path,destFileObjectType,uri,uri_path);
            boolean parent_dir_exists = zipEntryFileModel.exists();

            if(!parent_dir_exists)
            {
                String zip_entry_parent=new File(zip_entry_name).getParent();
                fileModel.makeDirsRecursively(zip_entry_parent);
            }

            OutputStream outStream;

            zipEntryFileModel=FileModelFactory.getFileModel(parent_dest_file_path,destFileObjectType,uri,uri_path);
            outStream=zipEntryFileModel.getChildOutputStream(dest_file.getName(),0);

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
        Context context;
        List<String> files_selected_array;
        Uri target_uri;
        String target_uri_path;
        boolean include_folder;
        FileObjectType sourceFileObjectType;
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
        FileCountSize(int total_no_of_files, long total_size_of_files)
        {
            this.total_no_of_files=total_no_of_files;
            this.total_size_of_files=total_size_of_files;

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
                    if(context==null)
                    {
                        mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));
                        return;
                    }
                    source_folder=new File(files_selected_array.get(0)).getParent();
                    int size=files_selected_array.size();
                    if(sourceFileObjectType==FileObjectType.FILE_TYPE || sourceFileObjectType== FileObjectType.SEARCH_LIBRARY_TYPE || sourceFileObjectType==FileObjectType.ROOT_TYPE)
                    {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                        {
                            try {
                                final int[] count = new int[1];
                                final long[] s = new long[1];
                                new NioFileIterator(files_selected_array,count, s,new MutableLiveData<>(),mutable_size_of_files_to_be_archived_copied);
                                total_no_of_files+=count[0];
                                total_size_of_files+=s[0];

                                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));

                            } catch (IOException e) {

                            }
                        }
                        else
                        {
                            File[] f_array=new File[size];
                            for(int i=0;i<size;++i)
                            {
                                File f=new File(files_selected_array.get(i));
                                f_array[i]=f;
                            }
                            populate(f_array,include_folder);
                        }

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
                        if(Global.CHECK_OTHER_FTP_SERVER_CONNECTED(FtpClientRepository.getInstance().ftpClientForProgress))
                        {
                            FTPFile[] f_array=new FTPFile[size];
                            for(int i=0;i<size;++i)
                            {
                                FTPFile f = FileUtil.getFTPFileFromOtherFTPClient(FtpClientRepository.getInstance().ftpClientForProgress,files_selected_array.get(i));
                                f_array[i]=f;
                            }
                            populate(f_array,include_folder,source_folder);
                        }

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
                        populate(FtpClientRepository.getInstance().ftpClientForProgress.listFiles(path),include_folder,path);
                    } catch (Exception e) {

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
                if(FileUtil.isDirectoryUri(context,uri))
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
                    size_of_files+=FileUtil.getSizeUri(context,uri);
                }
                total_no_of_files+=no_of_files;
                total_size_of_files+=size_of_files;
                mutable_size_of_files_to_be_archived_copied.postValue(FileUtil.humanReadableByteCount(total_size_of_files));

            }
        }

    }

}