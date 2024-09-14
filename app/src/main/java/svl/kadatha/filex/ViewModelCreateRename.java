package svl.kadatha.filex;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;

public class ViewModelCreateRename extends AndroidViewModel {

    public FilePOJO filePOJO;
    public final MutableLiveData<AsyncTaskStatus> asyncTaskStatus=new MutableLiveData<>(AsyncTaskStatus.NOT_YET_STARTED);
    public boolean file_created;
    public boolean fileNameChanged;
    public List<FilePOJO> destFilePOJOs;
    public static FileObjectType FILE_OBJECT_TYPE;

    public ViewModelCreateRename(@NonNull Application application) {
        super(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        FILE_OBJECT_TYPE=null;
    }

    public synchronized void createFile(File file, FileObjectType fileObjectType, int file_type, String parent_folder, String tree_uri_path, Uri tree_uri)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        FILE_OBJECT_TYPE=fileObjectType;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        final String new_name=file.getName();
        ExecutorService executorService=MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                file_created=false;
                if(file_type==0)
                {
                    if(fileObjectType==FileObjectType.ROOT_TYPE)
                    {
                        //file_created=RootUtils.EXECUTE(Arrays.asList(">",new_file_path));
                        if(Global.SET_OTHER_FILE_PERMISSION("rwx",parent_folder))
                        {
                            file_created=FileUtil.createNativeNewFile(file);
                        }
                    }
                    else {
                        FileModel fileModel= FileModelFactory.getFileModel(parent_folder,fileObjectType,tree_uri,tree_uri_path);
                        file_created=fileModel.createFile(new_name);
                    }

                }
                else if(file_type==1)
                {
                    if(fileObjectType==FileObjectType.ROOT_TYPE)
                    {
                        //file_created=RootUtils.EXECUTE(Arrays.asList("mkdir","-p",new_file_path));
                        if(Global.SET_OTHER_FILE_PERMISSION("rwx",parent_folder))
                        {
                            file_created=FileUtil.mkdirNative(file);
                        }
                    }
                    else {
                        FileModel fileModel= FileModelFactory.getFileModel(parent_folder,fileObjectType,tree_uri,tree_uri_path);
                        file_created=fileModel.makeDirIfNotExists(new_name);
                    }
                }
                if(file_created)
                {
                    filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(parent_folder, Collections.singletonList(new_name),fileObjectType,null);
                }
                FILE_OBJECT_TYPE=null;
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }


    public synchronized void renameFile(String parent_file_path, String existing_file_path, String existing_name,String new_file_path, String new_name, boolean isWritable, FileObjectType fileObjectType, boolean isDirectory, boolean overwrite,String tree_uri_path, Uri tree_uri, String filePOJOHashmapKeyPath, FileObjectType dfFileObjectType)
    {
        if(asyncTaskStatus.getValue()!=AsyncTaskStatus.NOT_YET_STARTED)return;
        FILE_OBJECT_TYPE=fileObjectType;
        asyncTaskStatus.setValue(AsyncTaskStatus.STARTED);
        ExecutorService executorService=MyExecutorService.getExecutorService();
        File existing_file=new File(parent_file_path,existing_name);
        File new_file=new File(new_file_path);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                fileNameChanged=false;
                if (fileObjectType==FileObjectType.ROOT_TYPE)
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
                else {
                        FileModel fileModel=FileModelFactory.getFileModel(Global.CONCATENATE_PARENT_CHILD_PATH(parent_file_path,existing_name),fileObjectType,tree_uri,tree_uri_path);
                        fileNameChanged=fileModel.rename(new_name,overwrite);
                }


                if(fileNameChanged)
                {
                    if(dfFileObjectType==FileObjectType.SEARCH_LIBRARY_TYPE)
                    {
                        if(overwrite)
                        {
                            FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO_ON_REMOVAL_SEARCH_LIBRARY(filePOJOHashmapKeyPath, Collections.singletonList(new_file_path),fileObjectType);
                        }
                        filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO_ON_ADD_SEARCH_LIBRARY(filePOJOHashmapKeyPath,Collections.singletonList(new_file_path),fileObjectType, Collections.singletonList(existing_file_path));
                    }
                    else
                    {
                        if(overwrite)
                        {
                            FilePOJOUtil.REMOVE_FROM_HASHMAP_FILE_POJO(filePOJOHashmapKeyPath, Collections.singletonList(new_name),fileObjectType);
                        }

                        filePOJO=FilePOJOUtil.ADD_TO_HASHMAP_FILE_POJO(filePOJOHashmapKeyPath, Collections.singletonList(new_name),fileObjectType, Collections.singletonList(existing_file_path));
                    }
                }
                FILE_OBJECT_TYPE=null;
                asyncTaskStatus.postValue(AsyncTaskStatus.COMPLETED);
            }
        });
    }

}
