package svl.kadatha.filex;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import me.jahnen.libaums.core.fs.UsbFile;

public class AppManagerListViewModel extends AndroidViewModel {

    private Future<?> future1,future2, future3;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public List<AppManagerListFragment.AppPOJO> systemAppPOJOList, userAppPOJOList;
    private final Application application;
    public final MutableLiveData<AsyncTaskStatus> isBackedUp=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    private boolean isCancelled;
    private FileObjectType destFileObjectType;

    public AppManagerListViewModel(@NonNull Application application) {
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

        isCancelled=true;
    }

    private boolean isCancelled()
    {
        return isCancelled;
    }

//    public void populateApps()
//    {
//        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
//        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
//        ExecutorService executorService=MyExecutorService.getExecutorService();
//        future1=executorService.submit(new Runnable() {
//            @Override
//            public void run() {
//                int flags = PackageManager.GET_META_DATA |
//                        PackageManager.GET_SHARED_LIBRARY_FILES |
//                        PackageManager.GET_UNINSTALLED_PACKAGES;
//
//                systemAppPOJOList=new ArrayList<>();
//                userAppPOJOList=new ArrayList<>();
//                final PackageManager packageManager = application.getPackageManager();
//                List<PackageInfo> packageInfos=packageManager.getInstalledPackages(flags);
//                for (PackageInfo packageInfo : packageInfos)
//                {
//                    if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1)
//                    {
//                        String name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
//                        String package_name=packageInfo.packageName;
//                        String version=packageInfo.versionName;
//                        String publicsourcedir=packageInfo.applicationInfo.publicSourceDir;
//                        if(publicsourcedir==null)
//                        {
//                            continue;
//                        }
//                        File file = new File(publicsourcedir);
//                        String path=file.getAbsolutePath();
//                        long size=file.length();
//                        long date=file.lastModified();
//                        AppManagerListFragment.extract_icon(package_name+".png",packageManager,packageInfo);
//                        systemAppPOJOList.add(new AppManagerListFragment.AppPOJO(name, package_name, path, size, date,version));
//
//                    }
//                    else if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1)
//                    {
//                        String name= (String) packageInfo.applicationInfo.loadLabel(packageManager);
//                        String package_name=packageInfo.packageName;
//                        String version=packageInfo.versionName;
//                        String publicsourcedir=packageInfo.applicationInfo.publicSourceDir;
//                        if(publicsourcedir==null)
//                        {
//                            continue;
//                        }
//                        File file = new File(publicsourcedir);
//                        String path=file.getAbsolutePath();
//                        long size=file.length();
//                        long date=file.lastModified();
//                        AppManagerListFragment.extract_icon(package_name+".png",packageManager,packageInfo);
//                        userAppPOJOList.add(new AppManagerListFragment.AppPOJO(name, package_name, path, size, date,version));
//                    }
//
//                }
//                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
//            }
//
//        });
//
//    }

    public synchronized void populateApps()
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                systemAppPOJOList=new ArrayList<>();
                userAppPOJOList=new ArrayList<>();
                RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass(application);
                repositoryClass.populateAppsList(userAppPOJOList,systemAppPOJOList);
                userAppPOJOList=Global.APP_POJO_HASHMAP.get("user");
                systemAppPOJOList=Global.APP_POJO_HASHMAP.get("system");
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });

    }

    public void back_up(List<String> files_selected_array, String dest_folder, FileObjectType destFileObjectType,List<String> new_name_list,Uri tree_uri,String tree_uri_path)
    {
        if(isBackedUp.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        isBackedUp.setValue(AsyncTaskStatus.STARTED);
        this.destFileObjectType=destFileObjectType;
        List<String>dest_file_names=new ArrayList<>();

        ExecutorService executorService=MyExecutorService.getExecutorService();
        future2=executorService.submit(new Runnable() {
            @Override
            public void run() {

                List<FilePOJO> destFilePOJOs=Global.HASHMAP_FILE_POJO.get(destFileObjectType+dest_folder);

                if(destFilePOJOs==null)
                {
                    UsbFile currentUsbFile=null;
                    if(destFileObjectType==FileObjectType.USB_TYPE)
                    {
                        if(MainActivity.usbFileRoot!=null)
                        {
                            try {
                                currentUsbFile=MainActivity.usbFileRoot.search(Global.GET_TRUNCATED_FILE_PATH_USB(dest_folder));

                            } catch (IOException e) {

                            }
                        }
                    }
                    FilePOJOUtil.FILL_FILEPOJO(new ArrayList<FilePOJO>(),new ArrayList<FilePOJO>(),destFileObjectType,dest_folder,currentUsbFile,false);
                    destFilePOJOs=Global.HASHMAP_FILE_POJO.get(destFileObjectType+dest_folder);
                }


                for(FilePOJO filePOJO:destFilePOJOs)
                {
                    dest_file_names.add(filePOJO.getName());
                }

                List<String> overwritten_copied_file_name_list;
                boolean copy_result = false;
                final boolean cut=false;
                String current_file_name;
                boolean isWritable=FileUtil.isWritable(destFileObjectType,dest_folder);
                final List<String> copied_files_name=new ArrayList<>();  //declared here instead of at Asynctask class to keep track of copied files in case replacement


                if(destFileObjectType==FileObjectType.ROOT_TYPE)
                {
                    if(destFileObjectType==FileObjectType.USB_TYPE)
                    {
                        isBackedUp.postValue(AsyncTaskStatus.COMPLETED);
                        return;
                    }

                }
                List<File> src_file_list=new ArrayList<>();

                for(String s: files_selected_array)
                {
                    File file=new File(s);
                    src_file_list.add(file);
                }

                int count = 0;
                overwritten_copied_file_name_list=new ArrayList<>(dest_file_names);
                for (File file : src_file_list) {
                    if (isCancelled() || file == null) {
                        isBackedUp.postValue(AsyncTaskStatus.COMPLETED);
                        return;
                    }

                    current_file_name = new_name_list.get(count);
                    String dest_file_path = Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder,current_file_name);

                    if (isWritable) {
                        copy_result = Copy_File_File(file, dest_file_path, cut);
                    } else {
                        if (destFileObjectType == FileObjectType.FILE_TYPE) {
                            copy_result = Copy_File_SAFFile(application, file, dest_folder, current_file_name, tree_uri, tree_uri_path, cut);
                        } else if (destFileObjectType == FileObjectType.USB_TYPE) {
                            copy_result = Copy_File_UsbFile(file, dest_folder, current_file_name, cut);
                        } else if (destFileObjectType == FileObjectType.FTP_TYPE) {
                            copy_result = Copy_File_FtpFile(file, dest_folder, current_file_name, cut);
                        }

                    }
                    String f_p = file.getAbsolutePath();
                    if (copy_result) {
                        copied_files_name.add(current_file_name);

                    }

                    files_selected_array.remove(f_p);
                    ++count;
                }

                if(copied_files_name.size()>0)
                {
                    List<String> overwritten_copied_file_path_list=new ArrayList<>();
                    overwritten_copied_file_name_list.retainAll(copied_files_name);
                    for(String name:overwritten_copied_file_name_list)
                    {
                        overwritten_copied_file_path_list.add(Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder,name));
                    }

                    FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(dest_folder,copied_files_name,destFileObjectType,overwritten_copied_file_path_list);
                    Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION, LocalBroadcastManager.getInstance(application),AppManagerActivity.ACTIVITY_NAME);
                    copied_files_name.clear();
                }

                isBackedUp.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }



    @SuppressWarnings("null")
    private boolean Copy_File_File(File source, String dest_file_path,boolean cut)
    {
        boolean success=false;
        File destination=new File(dest_file_path);
        if (source.isDirectory())
        {
            if(isCancelled())
            {
                return false;
            }
            if(!destination.exists())// || !destination.isDirectory())
            {
                if(!(success=FileUtil.mkdirsNative(destination)))
                {
                    return false;
                }
            }
            else {
                if(destination.isDirectory()) success=true;   //make success true as destination dir exists to execute cut directory
            }

            String[] files_name_array = source.list();
            if(files_name_array==null)
            {

                return true;
            }

            int size=files_name_array.length;
            for (int i=0;i<size;++i)
            {
                String inner_file_name=files_name_array[i];
                if(isCancelled())
                {
                    return false;
                }
                File srcFile = new File(source, inner_file_name);
                String inner_dest_file_path=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,inner_file_name);
                success=Copy_File_File(srcFile,inner_dest_file_path,cut);
            }

        }
        else
        {
            if(isCancelled())
            {
                return false;
            }
            success=FileUtil.copy_File_File(source,destination,cut);
        }

        return success;
    }


    @SuppressWarnings("null")
    private boolean Copy_File_SAFFile(Context context, File source, String dest_file_path, String name, Uri uri, String uri_path, boolean cut)
    {
        boolean success=false;

        if (source.isDirectory())
        {
            if(isCancelled())
            {
                return false;
            }


            if(destFileObjectType==FileObjectType.FILE_TYPE)
            {
                File destination=new File(dest_file_path,name);
                if(!destination.exists())// || !destination.isDirectory())
                {
                    if(!(success=FileUtil.mkdirSAF(context,dest_file_path,name,uri,uri_path)))
                    {
                        return false;
                    }
                }
                else {
                    if(destination.isDirectory()) success=true;
                }

            }

            /*
				//for other SAF
				else
				{
					Uri dest_uri=FileUtil.getDocumentUri(context,Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name),uri,uri_path);
					if(!FileUtil.exists(context,dest_uri) || !FileUtil.isDirectory(context,dest_uri))
					{
						if(!(success=FileUtil.mkdirSAF(context,dest_file_path,name,uri,uri_path)))
						{
							return false;
						}
					}

				}

				 */


            String[] files_name_list = source.list();
            if(files_name_list==null)
            {
                return true;
            }
            int size=files_name_list.length;
            for (int i=0;i<size;++i)
            {
                String inner_file_name=files_name_list[i];
                if(isCancelled())
                {
                    return false;
                }
                File srcFile = new File(source, inner_file_name);
                String inner_dest_file=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
                success=Copy_File_SAFFile(context,srcFile,inner_dest_file,inner_file_name,uri,uri_path,cut);
            }

        }
        else
        {
            if(isCancelled())
            {
                return false;
            }
            success=FileUtil.copy_File_SAFFile(context,source,dest_file_path,name,uri,uri_path,cut);
        }

        return success;
    }

    private boolean Copy_File_UsbFile(File source, String dest_file_path,String name,boolean cut)
    {
        boolean success=false;

        if (source.isDirectory())
        {
            if(isCancelled())
            {
                return false;
            }

            String file_path=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
            UsbFile dest_usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot, file_path);
            if(dest_usbFile==null) // || !dest_usbFile.isDirectory())
            {
                dest_usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot, dest_file_path);
                if(!(success=FileUtil.mkdirUsb(dest_usbFile,name)))
                {
                    return false;
                }
            }
            else {
                if(dest_usbFile.isDirectory()) success=true;
            }


            String[] files_name_list = source.list();
            if(files_name_list==null)
            {
                return true;
            }
            int size=files_name_list.length;
            for (int i=0;i<size;++i)
            {
                String inner_file_name=files_name_list[i];
                if(isCancelled())
                {
                    return false;
                }
                File srcFile = new File(source, inner_file_name);
                success=Copy_File_UsbFile(srcFile, file_path,inner_file_name,cut);
            }

        }
        else
        {
            if(isCancelled())
            {
                return false;
            }
            success=FileUtil.copy_File_UsbFile(source,dest_file_path,name,cut);
        }

        return success;
    }

    private boolean Copy_File_FtpFile(File source, String dest_file_path,String name,boolean cut)
    {
        boolean success=false;

        if (source.isDirectory())
        {
            if(isCancelled())
            {
                return false;
            }

            String file_path=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
            FTPFile dest_ftpFile=FileUtil.getFTPFile(file_path);//MainActivity.FTP_CLIENT.mlistFile(file_path);
            if(dest_ftpFile==null) // || !dest_usbFile.isDirectory())
            {
                if(!(success=FileUtil.mkdirFtp(file_path)))
                {
                    return false;
                }
            }
            else {
                if(dest_ftpFile.isDirectory()) success=true;
            }


            String[] files_name_list = source.list();
            if(files_name_list==null)
            {
                return true;
            }
            int size=files_name_list.length;
            for (int i=0;i<size;++i)
            {
                String inner_file_name=files_name_list[i];
                if(isCancelled())
                {
                    return false;
                }
                File srcFile = new File(source, inner_file_name);
                success=Copy_File_FtpFile(srcFile, file_path,inner_file_name,cut);
            }
        }
        else
        {
            if(isCancelled())
            {
                return false;
            }
            success=FileUtil.copy_File_FtpFile(source,dest_file_path,name,cut);
        }

        return success;
    }


    @SuppressWarnings("null")

    private boolean Copy_UsbFile_UsbFile(UsbFile src_usbfile, String dest_file_path, String name,boolean cut)
    {
        boolean success=false;
        if (src_usbfile.isDirectory())
        {
            if(isCancelled())
            {
                return false;
            }

            String file_path=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
            UsbFile dest_usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot, file_path);
            if(dest_usbFile==null)// || !dest_usbFile.isDirectory())
            {
                dest_usbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot, dest_file_path);
                if(!(success=FileUtil.mkdirUsb(dest_usbFile,name)))
                {
                    return false;
                }
            }
            else {
                if(dest_usbFile.isDirectory()) success=true;
            }


            UsbFile[] inner_source_list;
            try {
                inner_source_list = src_usbfile.listFiles();
            } catch (IOException e) {
                return false;
            }


            int size=inner_source_list.length;
            for (int i=0;i<size;++i)
            {
                UsbFile inner_usbfile=inner_source_list[i];
                if(isCancelled())
                {
                    return false;
                }
                success=Copy_UsbFile_UsbFile(inner_usbfile, file_path, inner_usbfile.getName(),cut);
            }
        }
        else
        {
            if(isCancelled())
            {
                return false;
            }

            success=FileUtil.copy_UsbFile_UsbFile(src_usbfile,dest_file_path,name,cut);
        }
        return success;
    }



    @SuppressWarnings("null")
    private boolean Copy_UsbFile_File(UsbFile src_usbfile, String parent_file_path, String name,boolean cut)
    {
        boolean success=false;
        File destination=new File(parent_file_path,name);
        if (src_usbfile.isDirectory())
        {
            if(isCancelled())
            {
                return false;
            }
            if(!destination.exists()) // || !destination.isDirectory())
            {
                if(!(success=FileUtil.mkdirsNative(destination)))
                {
                    return false;
                }
            }
            else {
                if(destination.isDirectory()) success=true;
            }


            UsbFile[] inner_source_list;
            try {
                inner_source_list = src_usbfile.listFiles();
            } catch (IOException e) {
                return false;
            }


            int size=inner_source_list.length;
            for (int i=0;i<size;++i)
            {
                UsbFile inner_usbfile=inner_source_list[i];
                if(isCancelled())
                {
                    return false;
                }

                String inner_dest_file=Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,name);
                success=Copy_UsbFile_File(inner_usbfile,inner_dest_file, inner_usbfile.getName(),cut);
            }

        }
        else
        {
            if(isCancelled())
            {
                return false;
            }
            success=FileUtil.copy_UsbFile_File(src_usbfile,destination,cut);
        }
        return success;
    }


    @SuppressWarnings("null")
    private boolean Copy_UsbFile_SAFFile(Context context, UsbFile source, String dest_file_path,String name,Uri uri,String uri_path,boolean cut)
    {
        boolean success=false;

        if (source.isDirectory())
        {
            if(isCancelled())
            {
                return false;
            }

            if(destFileObjectType==FileObjectType.FILE_TYPE)
            {
                File destination=new File(dest_file_path,name);
                if(!destination.exists()) // || !destination.isDirectory())
                {
                    if(!(success=FileUtil.mkdirSAF(context,dest_file_path,name,uri,uri_path)))
                    {
                        return false;
                    }
                }
                else {
                    if(destination.isDirectory()) success=true;
                }

            }
				/*
				//for other SAF
				else
				{
					Uri dest_uri=FileUtil.getDocumentUri(context,Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name),uri,uri_path);
					if(!FileUtil.exists(context,dest_uri) || !FileUtil.isDirectory(context,dest_uri))
					{
						if(!(success=FileUtil.mkdirSAF(context,dest_file_path,name,uri,uri_path)))
						{
							return false;
						}
					}

				}

				 */


            UsbFile[] files_name_list;
            try {
                files_name_list = source.listFiles();
            } catch (IOException e) {
                return  false;
            }
            int size=files_name_list.length;
            for (int i=0;i<size;++i)
            {
                UsbFile inner_usbfile=files_name_list[i];
                if(isCancelled())
                {
                    return false;
                }

                String inner_dest_file=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
                success=Copy_UsbFile_SAFFile(context,inner_usbfile,inner_dest_file,inner_usbfile.getName(),uri,uri_path,cut);
            }

        }
        else
        {
            if(isCancelled())
            {
                return false;
            }

            success=FileUtil.copy_UsbFile_SAFFile(context,source,dest_file_path,name,uri,uri_path,cut);
        }

        return success;
    }


    @SuppressWarnings("null")
    private boolean Copy_FtpFile_File(String src_file_path, String parent_file_path, String name,boolean cut)
    {
        boolean success=false;
        File destination=new File(parent_file_path,name);
        FTPFile src_ftpfile=FileUtil.getFTPFile(src_file_path);//MainActivity.FTP_CLIENT.mlistFile(src_file_path);
        if (src_ftpfile.isDirectory())
        {
            if(isCancelled())
            {
                return false;
            }
            if(!destination.exists()) // || !destination.isDirectory())
            {
                if(!(success=FileUtil.mkdirsNative(destination)))
                {
                    return false;
                }
            }
            else {
                if(destination.isDirectory()) success=true;
            }


            String[] inner_source_list;
            try {
                inner_source_list = MainActivity.FTP_CLIENT.listNames(src_file_path);
            } catch (IOException e) {
                return false;
            }


            int size=inner_source_list.length;
            for (int i=0;i<size;++i)
            {
                String inner_ftpfile_name=inner_source_list[i];
                if(isCancelled())
                {
                    return false;
                }

                String inner_dest_file=Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,name);
                String inner_source_file=Global.CONCATENATE_PARENT_CHILD_PATH(src_file_path,inner_ftpfile_name);
                success=Copy_FtpFile_File(inner_source_file,inner_dest_file, inner_ftpfile_name,cut);
            }

        }
        else
        {
            if(isCancelled())
            {
                return false;
            }

            success=FileUtil.copy_FtpFile_File(src_file_path,destination,cut);
        }

        return success;
    }


    @SuppressWarnings("null")
    private boolean Copy_FtpFile_SAFFile(Context context, String src_file_path, String dest_file_path,String name,Uri uri,String uri_path,boolean cut)
    {
        boolean success=false;
        FTPFile src_ftpfile=FileUtil.getFTPFile(src_file_path);//MainActivity.FTP_CLIENT.mlistFile(src_file_path);
        if (src_ftpfile.isDirectory())
        {
            if(isCancelled())
            {
                return false;
            }

            if(destFileObjectType==FileObjectType.FILE_TYPE)
            {
                File destination=new File(dest_file_path,name);
                if(!destination.exists()) // || !destination.isDirectory())
                {
                    if(!(success=FileUtil.mkdirSAF(context,dest_file_path,name,uri,uri_path)))
                    {
                        return false;
                    }
                }
                else {
                    if(destination.isDirectory()) success=true;
                }

            }

            String[] inner_source_list;
            try {
                inner_source_list = MainActivity.FTP_CLIENT.listNames(src_file_path);
            } catch (IOException e) {
                return false;
            }


            int size=inner_source_list.length;
            for (int i=0;i<size;++i)
            {
                String inner_ftpfile_name=inner_source_list[i];
                if(isCancelled())
                {
                    return false;
                }

                String inner_dest_file=Global.CONCATENATE_PARENT_CHILD_PATH(dest_file_path,name);
                String inner_source_file=Global.CONCATENATE_PARENT_CHILD_PATH(src_file_path,inner_ftpfile_name);
                success=Copy_FtpFile_SAFFile(context,inner_source_file,inner_dest_file,inner_ftpfile_name,uri,uri_path,cut);
            }

        }
        else
        {
            if(isCancelled())
            {
                return false;
            }
            success=FileUtil.copy_FtpFile_SAFFile(context,src_file_path,dest_file_path,name,uri,uri_path,cut);
        }

        return success;
    }

}
