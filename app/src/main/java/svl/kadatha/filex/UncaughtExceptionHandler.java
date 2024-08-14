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
        RepositoryClass repositoryClass=RepositoryClass.getRepositoryClass();
        repositoryClass.storage_dir.clear();
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
            else if(appCompatActivity instanceof ArchiveViewActivity)
            {
                ((ArchiveViewActivity)appCompatActivity).clearCache();
            }
            else if(appCompatActivity instanceof AppManagerActivity)
            {
                ((AppManagerActivity)appCompatActivity).clear_cache=false;
            }
            else if(appCompatActivity instanceof InstaCropperActivity)
            {
                ((InstaCropperActivity)context).clear_cache=false;
            }
            appCompatActivity.finish();
            appCompatActivity.startActivity(intent);
        }

        System.exit(0);
    }
}
