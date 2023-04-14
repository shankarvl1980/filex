package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler{

    final Context context;

    UncaughtExceptionHandler(Context context)
    {
        this.context=context;
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        StringWriter strack_trace=new StringWriter();
        e.printStackTrace(new PrintWriter(strack_trace));

        Global.STORAGE_DIR.clear();
        AppCompatActivity appCompatActivity=(AppCompatActivity)context;
        if(appCompatActivity!=null)
        {
            Intent intent=appCompatActivity.getIntent();
            if(appCompatActivity instanceof MainActivity)
            {
                ((MainActivity)appCompatActivity).clearCache();
            }
            else if(appCompatActivity instanceof FileSelectorActivity)
            {
                ((FileSelectorActivity)appCompatActivity).clearCache();
            }
            else if(appCompatActivity instanceof StorageAnalyserActivity)
            {
                ((StorageAnalyserActivity)appCompatActivity).clearCache();
            }
            else if(appCompatActivity instanceof CopyToActivity)
            {
                ((CopyToActivity)appCompatActivity).clearCache();
            }
            else if(appCompatActivity instanceof ArchiveViewerActivity)
            {
                ((ArchiveViewerActivity)appCompatActivity).clearCache();
            }
            appCompatActivity.finish();
            appCompatActivity.startActivity(intent);
        }

        System.exit(0);
    }
}
