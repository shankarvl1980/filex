package svl.kadatha.filex;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.concurrent.ExecutorService;

import svl.kadatha.filex.appmanager.AppManagerListViewModel;
import svl.kadatha.filex.filemodel.FileModel;
import svl.kadatha.filex.filemodel.FileModelFactory;
import svl.kadatha.filex.instacrop.InstaCropperActivity;

public class ArchiveDeletePasteServiceUtil {

    static Class getEmptyService(Context context) {
        Class emptyService = null;

        if (ArchiveDeletePasteFileService1.SERVICE_COMPLETED) {
            emptyService = ArchiveDeletePasteProgressActivity1.class;
        } else if (ArchiveDeletePasteFileService2.SERVICE_COMPLETED) {
            emptyService = ArchiveDeletePasteProgressActivity2.class;
        } else if (ArchiveDeletePasteFileService3.SERVICE_COMPLETED) {
            emptyService = ArchiveDeletePasteProgressActivity3.class;
        }

        if (emptyService != null) {
            AppCompatActivity appCompatActivity = (AppCompatActivity) context;
            if (appCompatActivity instanceof MainActivity) {
                ((MainActivity) context).clear_cache = false;
            } else if (appCompatActivity instanceof StorageAnalyserActivity) {
                ((StorageAnalyserActivity) context).clear_cache = false;
            } else if (appCompatActivity instanceof CopyToActivity) {
                ((CopyToActivity) context).clear_cache = false;
            } else if (appCompatActivity instanceof ArchiveViewActivity) {
                ((ArchiveViewActivity) context).clear_cache = false;
            } else if (appCompatActivity instanceof InstaCropperActivity) {
                ((InstaCropperActivity) context).clear_cache = false;
            }
        }
        return emptyService;
    }


    private static boolean NO_OPERATION_ON_FILE_OBJECT_TYPE(FileObjectType fileObjectType) {
        boolean noOperation = true;

        //checking asynctasks of copying, deleting and archiving are going on
        if (noOperation && ArchiveDeletePasteFileService1.SOURCE_FILE_OBJECT != null) {
            noOperation = ArchiveDeletePasteFileService1.SOURCE_FILE_OBJECT != fileObjectType;
        }

        if (noOperation && ArchiveDeletePasteFileService1.DEST_FILE_OBJECT != null) {
            noOperation = ArchiveDeletePasteFileService1.DEST_FILE_OBJECT != fileObjectType;
        }

        if (noOperation && ArchiveDeletePasteFileService2.SOURCE_FILE_OBJECT != null) {
            noOperation = ArchiveDeletePasteFileService2.SOURCE_FILE_OBJECT != fileObjectType;
        }

        if (noOperation && ArchiveDeletePasteFileService2.DEST_FILE_OBJECT != null) {
            noOperation = ArchiveDeletePasteFileService2.DEST_FILE_OBJECT != fileObjectType;
        }

        if (noOperation && ArchiveDeletePasteFileService3.SOURCE_FILE_OBJECT != null) {
            noOperation = ArchiveDeletePasteFileService3.SOURCE_FILE_OBJECT != fileObjectType;
        }

        if (noOperation && ArchiveDeletePasteFileService3.DEST_FILE_OBJECT != null) {
            noOperation = ArchiveDeletePasteFileService3.DEST_FILE_OBJECT != fileObjectType;
        }

        //checking whether creating or renaming file is going on
        if (noOperation && CreateRenameViewModel.FILE_OBJECT_TYPE != null) {
            noOperation = CreateRenameViewModel.FILE_OBJECT_TYPE != fileObjectType;
        }

        //check whether app is being backed up
        if (noOperation && AppManagerListViewModel.FILE_OBJECT_TYPE != null) {
            noOperation = AppManagerListViewModel.FILE_OBJECT_TYPE != fileObjectType;
        }

        //check whether usb file being copied to cache
        if (noOperation && Global.USB_CACHED_FILE_OBJECT != null) {
            noOperation = Global.USB_CACHED_FILE_OBJECT != fileObjectType;
        }

        return noOperation;
    }

    public static boolean WHETHER_TO_START_SERVICE_ON_USB(FileObjectType sourceFileObjectType, FileObjectType destFileObjectType) {
        boolean noOperation = true;
        if (noOperation && sourceFileObjectType != null) {
            if (sourceFileObjectType == FileObjectType.USB_TYPE) {
                noOperation = ArchiveDeletePasteServiceUtil.NO_OPERATION_ON_FILE_OBJECT_TYPE(FileObjectType.USB_TYPE);
            }
        }

        if (noOperation && destFileObjectType != null) {
            if (destFileObjectType == FileObjectType.USB_TYPE) {
                noOperation = ArchiveDeletePasteServiceUtil.NO_OPERATION_ON_FILE_OBJECT_TYPE(FileObjectType.USB_TYPE);
            }
        }

        return noOperation;
    }

    public static void CLEAR_CACHE_AND_REFRESH(String file_path, FileObjectType sourceFileObjectType) {
        Bundle bundle = new Bundle();
        bundle.putString("file_path", file_path);
        bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_CLEAR_CACHE_REFRESH_ACTION, LocalBroadcastManager.getInstance(App.getAppContext()), bundle);
    }

    public static void NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(String source_folder, FileObjectType sourceFileObjectType) {
        Bundle bundle = new Bundle();
        bundle.putString("source_folder", source_folder);
        bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_DELETE_FILE_ACTION, LocalBroadcastManager.getInstance(App.getAppContext()), bundle);
    }

    public static void NOTIFY_ALL_DIALOG_FRAGMENTS_ON_CUT_COPY(String dest_folder, String source_folder, FileObjectType destFileObjectType, FileObjectType sourceFileObjectType, FilePOJO filePOJO) {
        Bundle bundle = new Bundle();
        bundle.putString("dest_folder", dest_folder);
        bundle.putSerializable("destFileObjectType", destFileObjectType);
        bundle.putString("source_folder", source_folder);
        bundle.putSerializable("sourceFileObjectType", sourceFileObjectType);
        bundle.putParcelable("filePOJO", filePOJO);
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_CUT_COPY_FILE_ACTION, LocalBroadcastManager.getInstance(App.getAppContext()), bundle);
    }


    public static void NOTIFY_ALL_DIALOG_FRAGMENTS_ON_ARCHIVE_UNARCHIVE_COMPLETE(String dest_folder, FileObjectType destFileObjectType, FilePOJO filePOJO) {
        Bundle bundle = new Bundle();
        bundle.putString("dest_folder", dest_folder);
        bundle.putSerializable("destFileObjectType", destFileObjectType);
        bundle.putParcelable("filePOJO", filePOJO);
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_ARCHIVE_UNARCHIVE_FILE_ACTION, LocalBroadcastManager.getInstance(App.getAppContext()), bundle);
    }

    public static void NOTIFY_ALL_DIALOG_FRAGMENTS_ON_COPY_TO(String dest_folder, FileObjectType destFileObjectType, FilePOJO filePOJO) {
        Bundle bundle = new Bundle();
        bundle.putString("dest_folder", dest_folder);
        bundle.putSerializable("destFileObjectType", destFileObjectType);
        bundle.putParcelable("filePOJO", filePOJO);
        Global.LOCAL_BROADCAST(Global.LOCAL_BROADCAST_COPY_TO_FILE_ACTION, LocalBroadcastManager.getInstance(App.getAppContext()), bundle);
    }


    public static String ON_DELETE_ASYNC_TASK_COMPLETE(Context context, boolean result, int counter_no_files, String source_folder, FileObjectType sourceFileObjectType,
                                                       boolean cancelled, boolean storage_analyser_delete) {
        String notification_content;
        if (cancelled) {
            CLEAR_CACHE_AND_REFRESH(source_folder, sourceFileObjectType);
            return null;
        }

        if (result) {
            notification_content = sourceFileObjectType.equals(FileObjectType.SEARCH_LIBRARY_TYPE) ? context.getString(R.string.deleted_selected_files) : context.getString(R.string.deleted_selected_files) + " " + source_folder;
        } else {
            notification_content = sourceFileObjectType.equals(FileObjectType.SEARCH_LIBRARY_TYPE) ? context.getString(R.string.could_not_delete_selected_files) : context.getString(R.string.could_not_delete_selected_files) + " " + source_folder;
        }

        if (counter_no_files > 0) {
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(source_folder, sourceFileObjectType);
            Global.WORKOUT_AVAILABLE_SPACE();
        }
        return notification_content;
    }


    public static String ON_CUT_COPY_ASYNC_TASK_COMPLETE(Context context, boolean result, int counter_no_files, String source_folder, String dest_folder,
                                                         FileObjectType sourceFileObjectType, FileObjectType destFileObjectType, FilePOJO filePOJO,
                                                         boolean cut, boolean cancelled) {
        String notification_content;

        if (cancelled) {
            CLEAR_CACHE_AND_REFRESH(dest_folder, destFileObjectType);  //for dest_folder
            if (cut) {
                CLEAR_CACHE_AND_REFRESH(source_folder, sourceFileObjectType);  // for source_folder
            }
            return null;
        }

        if (result) {
            notification_content = (cut ? context.getString(R.string.moved_selected_files) + " " + dest_folder : context.getString(R.string.copied_selected_files) + " " + dest_folder);
        } else {
            notification_content = (cut ? context.getString(R.string.could_not_move_selected_files) + " " + dest_folder : context.getString(R.string.could_not_copy_selected_files) + " " + dest_folder);
        }

        if (counter_no_files > 0) {
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_CUT_COPY(dest_folder, source_folder, destFileObjectType, sourceFileObjectType, filePOJO);
            Global.WORKOUT_AVAILABLE_SPACE();
        }
        return notification_content;
    }

    public static void ON_ARCHIVE_ASYNC_TASK_CANCEL(Context context, String dest_folder, String zip_file_name, FileObjectType destFileObjectType, Uri tree_uri, String tree_uri_path) {
        File f = new File(dest_folder, zip_file_name);
        String file_path = Global.CONCATENATE_PARENT_CHILD_PATH(dest_folder, zip_file_name);
        ExecutorService executorService = MyExecutorService.getExecutorService();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FileModel fileModel = FileModelFactory.getFileModel(file_path, destFileObjectType, tree_uri, tree_uri_path);
                fileModel.delete();
                Global.WORKOUT_AVAILABLE_SPACE();
            }
        });
        NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(dest_folder, destFileObjectType);
    }

    public static String ON_ARCHIVE_ASYNC_TASK_COMPLETE(Context context, boolean result, FilePOJO filePOJO, String dest_folder,
                                                        String zip_file_name, FileObjectType destFileObjectType) {
        String notification_content;
        if (result) {
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_ARCHIVE_UNARCHIVE_COMPLETE(dest_folder, destFileObjectType, filePOJO);
            notification_content = context.getString(R.string.created) + " '" + zip_file_name + "' " + context.getString(R.string.at) + " " + dest_folder;
            Global.WORKOUT_AVAILABLE_SPACE();
        } else {
            notification_content = context.getString(R.string.could_not_create) + " '" + zip_file_name + "'";
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(dest_folder, destFileObjectType);
        }
        return notification_content;
    }


    public static String ON_UNARCHIVE_ASYNC_TASK_COMPLETE(Context context, boolean result, int counter_no_files, FilePOJO filePOJO, String dest_folder,
                                                          FileObjectType destFileObjectType,
                                                          String zip_file_path, boolean cancelled) {
        String notification_content;
        if (cancelled) {
            CLEAR_CACHE_AND_REFRESH(dest_folder, destFileObjectType);
            return null;
        }

        if (result) {
            notification_content = context.getString(R.string.unzipped) + " '" + new File(zip_file_path).getName() + "' " + context.getString(R.string.at) + " " + dest_folder;
        } else {
            notification_content = context.getString(R.string.could_not_extract) + " '" + new File(zip_file_path).getName() + "'";
        }

        if (counter_no_files > 0) {
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_ARCHIVE_UNARCHIVE_COMPLETE(dest_folder, destFileObjectType, filePOJO);
            Global.WORKOUT_AVAILABLE_SPACE();
        } else {
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(dest_folder, destFileObjectType);
        }
        return notification_content;
    }

    public static String ON_COPY_TO_ASYNC_TASK_COMPLETE(Context context, boolean result, String dest_folder,
                                                        String file_name, FileObjectType destFileObjectType, FilePOJO filePOJO) {
        String notification_content;
        if (result) {
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_COPY_TO(dest_folder, destFileObjectType, filePOJO);
            notification_content = context.getString(R.string.created) + " '" + file_name + "' " + context.getString(R.string.at) + " " + dest_folder;
            Global.WORKOUT_AVAILABLE_SPACE();
        } else {
            notification_content = context.getString(R.string.could_not_create) + " '" + file_name + "'";
            NOTIFY_ALL_DIALOG_FRAGMENTS_ON_DELETE(dest_folder, destFileObjectType);
        }
        return notification_content;
    }
}