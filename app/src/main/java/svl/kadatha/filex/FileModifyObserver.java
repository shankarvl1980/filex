package svl.kadatha.filex;

import android.os.Build;
import android.os.FileObserver;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;

public class FileModifyObserver extends FileObserver {

    private FileObserverListener fileObserverListener;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private FileModifyObserver(File file)
    {
        super(file, CREATE | MOVED_FROM | MOVED_TO  | DELETE | DELETE_SELF);
    }

    private FileModifyObserver(String file_path)
    {
        super(file_path, CREATE | MOVED_FROM | MOVED_TO | DELETE | DELETE_SELF);
    }

    public static FileModifyObserver getInstance(String file_path)
    {
        if(file_path==null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            return new FileModifyObserver(new File(file_path)) ;
        }
        else
        {
            return new FileModifyObserver(file_path) ;
        }
    }
    @Override
    public void onEvent(int i, @Nullable String s) {

        i&=CREATE|MOVED_FROM|MOVED_TO|DELETE|DELETE_SELF;
        switch(i)
        {
            case CREATE:
            case MOVED_FROM:
            case MOVED_TO:
            case DELETE:
            case DELETE_SELF:
                if(fileObserverListener!=null) fileObserverListener.onFileModified();
                break;
        }

    }

    interface FileObserverListener
    {
        void onFileModified();
    }

    public void setFileObserverListener(FileObserverListener listener)
    {
        fileObserverListener=listener;
    }

}
