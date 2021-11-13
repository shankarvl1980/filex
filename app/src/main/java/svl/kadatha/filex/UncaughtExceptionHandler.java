package svl.kadatha.filex;

import android.content.Context;
import android.content.Intent;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler{

    Context context;

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
            appCompatActivity.finish();
            appCompatActivity.startActivity(intent);
        }

        System.exit(0);
    }
}
