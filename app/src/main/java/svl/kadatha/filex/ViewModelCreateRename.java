package svl.kadatha.filex;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

import me.jahnen.libaums.core.fs.UsbFile;

public class ViewModelCreateRename extends AndroidViewModel {

    private final Application application;
    public FilePOJO filePOJO;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public boolean file_created;
    public boolean fileNameChanged;

    public ViewModelCreateRename(@NonNull Application application) {
        super(application);
        this.application=application;
    }

    public void createFile(File file, FileObjectType fileObjectType, boolean isWritable, int file_type, String parent_folder, String tree_uri_path, Uri tree_uri)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        final String new_file_path=file.getAbsolutePath();
        final String new_name=file.getName();
        ExecutorService executorService=MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if(file_type==0)
                {
                    if(isWritable)
                    {
                        file_created=FileUtil.createNativeNewFile(file);
                    }
                    else if(fileObjectType== FileObjectType.FILE_TYPE)
                    {
                        file_created=FileUtil.createSAFNewFile(application,parent_folder,new_name,tree_uri,tree_uri_path);
                    }
                    else if(fileObjectType== FileObjectType.USB_TYPE)
                    {
                        UsbFile parentUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,parent_folder);
                        file_created=FileUtil.createUsbFile(parentUsbFile,new_name);

                    }
                    else if(fileObjectType==FileObjectType.ROOT_TYPE)
                    {
                        //file_created=RootUtils.EXECUTE(Arrays.asList(">",new_file_path));
                        if(Global.SET_OTHER_FILE_PERMISSION("rwx",parent_folder))
                        {
                            file_created=FileUtil.createNativeNewFile(file);
                        }
                    }
                    else if(fileObjectType==FileObjectType.FTP_TYPE)
                    {
                        if(Global.CHECK_FTP_SERVER_CONNECTED())
                        {
                            InputStream bin = new ByteArrayInputStream(new byte[0]);
                            try {
                                file_created=MainActivity.FTP_CLIENT.storeFile(new_file_path, bin);
                            } catch (IOException e) {
                                file_created=false;
                            }
                        }
                        else
                        {
                            file_created=false;
                        }

                    }
                }
                else if(file_type==1)
                {
                    if(isWritable)
                    {
                        file_created=FileUtil.mkdirNative(file);
                    }
                    else if(fileObjectType== FileObjectType.FILE_TYPE)
                    {
                        file_created=FileUtil.mkdirSAF(application,parent_folder,new_name,tree_uri,tree_uri_path);
                    }
                    else if(fileObjectType== FileObjectType.USB_TYPE)
                    {
                        UsbFile parentUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,parent_folder);
                        file_created=FileUtil.mkdirUsb(parentUsbFile,new_name);
                    }
                    else if(fileObjectType==FileObjectType.ROOT_TYPE)
                    {
                        //file_created=RootUtils.EXECUTE(Arrays.asList("mkdir","-p",new_file_path));
                        if(Global.SET_OTHER_FILE_PERMISSION("rwx",parent_folder))
                        {
                            file_created=FileUtil.mkdirNative(file);
                        }
                    }
                    else if(fileObjectType==FileObjectType.FTP_TYPE)
                    {
                        if(Global.CHECK_FTP_SERVER_CONNECTED())
                        {
                            try {
                                file_created=MainActivity.FTP_CLIENT.makeDirectory(new_file_path);
                            } catch (IOException e) {
                            }
                        }
                        else
                        {
                            file_created=false;
                        }
                    }
                }
                if(file_created)
                {
                    filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(parent_folder, Collections.singletonList(new_name),fileObjectType,null);
                }
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }


    public void renameFile(String parent_file_path, String existing_file_path, String existing_name,String new_file_path, String new_name, boolean isWritable, FileObjectType fileObjectType, boolean isDirectory, boolean overwriting,String tree_uri_path, Uri tree_uri, String filePOJOHashmapKeyPath, FileObjectType dfFileObjectType)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        File existing_file=new File(parent_file_path,existing_name);
        File new_file=new File(new_file_path);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if(fileObjectType==FileObjectType.FILE_TYPE)
                {
                    if(isWritable)
                    {
                        fileNameChanged=FileUtil.renameNativeFile(existing_file,new_file);

                    }
                    else
                    {
                        if(overwriting) //to overwrite file name
                        {
                            boolean isDir=new File(new_file_path).isDirectory();
                            if(!isDir && !isDirectory)
                            {
                                if(FileUtil.deleteSAFDirectory(application,new_file_path,tree_uri,tree_uri_path))
                                {
                                    fileNameChanged=FileUtil.renameSAFFile(application,Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,existing_name),new_name,tree_uri,tree_uri_path);
                                }
                            }

                        }
                        else
                        {
                            fileNameChanged=FileUtil.renameSAFFile(application,Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,existing_name),new_name,tree_uri,tree_uri_path);
                        }
                    }
                }
                else if(fileObjectType== FileObjectType.USB_TYPE)
                {
                    UsbFile existingUsbFile=FileUtil.getUsbFile(MainActivity.usbFileRoot,existing_file.getAbsolutePath());
                    fileNameChanged=FileUtil.renameUsbFile(existingUsbFile,new_name);

                }
                else if (fileObjectType==FileObjectType.ROOT_TYPE)
                {
                    if(RootUtils.CAN_RUN_ROOT_COMMANDS())
                    {
                        //fileNameChanged=RootUtils.EXECUTE(Arrays.asList("mv",existing_file.getAbsolutePath(),new_file_path));
                        if(Global.SET_OTHER_FILE_PERMISSION("rwx",existing_file_path))
                        {
                            fileNameChanged=FileUtil.renameNativeFile(existing_file,new_file);
                        }
                    }
                    else
                    {
                        //print(getString(R.string.root_access_not_avaialable));
                        fileNameChanged=false;
                    }

                }
                else if(fileObjectType==FileObjectType.FTP_TYPE)
                {
                    if(Global.CHECK_FTP_SERVER_CONNECTED())
                    {
                        try {
                            fileNameChanged=MainActivity.FTP_CLIENT.rename(existing_file.getAbsolutePath(),new_file_path);
                        } catch (IOException e) {
                        }
                    }
                    else
                    {
                        Global.print_background_thread(application,application.getString(R.string.ftp_server_is_not_connected));
                    }
                }
                if(fileNameChanged)
                {
                    if(dfFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
                    {
                        if(overwriting)
                        {
                            FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO_ON_REMOVAL_SEARCH_LIBRARY(filePOJOHashmapKeyPath, Collections.singletonList(new_file_path),fileObjectType);
                        }
                        filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO_ON_ADD_SEARCH_LIBRARY(filePOJOHashmapKeyPath,Collections.singletonList(new_file_path),fileObjectType, Collections.singletonList(existing_file_path));
                    }
                    else
                    {
                        if(overwriting)
                        {
                            FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(filePOJOHashmapKeyPath, Collections.singletonList(new_name),fileObjectType);
                        }

                        filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(filePOJOHashmapKeyPath, Collections.singletonList(new_name),fileObjectType, Collections.singletonList(existing_file_path));
                    }
                }
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

}
