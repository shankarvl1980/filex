package svl.kadatha.filex.texteditor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import svl.kadatha.filex.MyExecutorService;
import svl.kadatha.filex.NotifManager;
import svl.kadatha.filex.R;

public class TextSaveService3 extends Service {
    static boolean SERVICE_COMPLETED = true;
    public LinkedHashMap<Integer, TextEditorViewModel.PagePointer> pagePointerHashmap;
    private FileSaveServiceBinder binder = new FileSaveServiceBinder();
    private FileSaveServiceCompletionListener fileSaveServiceCompletionListener;
    private Context context;
    private NotifManager nm;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        SERVICE_COMPLETED = false;
        context = this;
        nm = new NotifManager(context);
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getBundleExtra("bundle");
        if (bundle != null) {
            filesave(bundle);
            int notification_id = 982;
            File file = new File(bundle.getString("file_path"));
            startForeground(notification_id, nm.build(getString(R.string.being_updated) + "-" + "'" + file.getName() + "'", notification_id));
        } else {
            SERVICE_COMPLETED = true;
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (binder == null) {
            binder = new FileSaveServiceBinder();
        }
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        SERVICE_COMPLETED = true;
    }

    private void filesave(final Bundle bundle) {
        ExecutorService executorService = MyExecutorService.getExecutorService();
        Future future = executorService.submit(new Runnable() {
            @Override
            public void run() {
                final TextSaveHelper.SaveResult saveResult = TextSaveHelper.saveFile(context, bundle);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (fileSaveServiceCompletionListener != null) {
                            fileSaveServiceCompletionListener.onServiceCompletion(saveResult);
                        }
                        stopForeground(true);
                        stopSelf();
                        SERVICE_COMPLETED = true;
                    }
                });
            }
        });
    }

    public void setFileSaveServiceCompletionListener(FileSaveServiceCompletionListener listener) {
        fileSaveServiceCompletionListener = listener;
    }

    interface FileSaveServiceCompletionListener {
        void onServiceCompletion(TextSaveHelper.SaveResult result);
    }

    class FileSaveServiceBinder extends Binder {
        public TextSaveService3 getService() {
            return TextSaveService3.this;
        }
    }
}