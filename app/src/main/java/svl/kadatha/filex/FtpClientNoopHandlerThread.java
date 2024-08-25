package svl.kadatha.filex;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.IOException;

public class FtpClientNoopHandlerThread extends HandlerThread {

    public Handler handler;
    private final Handler noopHandler;
    public static final int START=1;
    public static final int STOP=0;
    private static FtpClientNoopHandlerThread ftpClientNoopHandlerThread;
    private final Runnable runnable;

    public static FtpClientNoopHandlerThread getInstance()
    {
        if(ftpClientNoopHandlerThread==null)
        {
            ftpClientNoopHandlerThread=new FtpClientNoopHandlerThread("ftpclientnoophandlerthread");
            ftpClientNoopHandlerThread.start();
            ftpClientNoopHandlerThread.onLooperPrepared();
        }
        return ftpClientNoopHandlerThread;
    }

    private FtpClientNoopHandlerThread(String name) {
        super(name);
        runnable=new Runnable() {
            @Override
            public void run() {
                //if(Global.CHECK_OTHER_FTP_SERVER_CONNECTED(FtpClientRepository_old.getInstance().ftpClientForNoop))
//                {
//                    try {
//                        FtpClientRepository_old.getInstance().ftpClientForNoop.sendNoOp();
//                    } catch (IOException e) {
//                        noopHandler.removeCallbacks(this);
//                    }
//                }
//                else {
//                    noopHandler.removeCallbacks(this);
//                }
            }
        };
        noopHandler=new Handler();
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        handler=new Handler(getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what)
                {
                    case START:
                        noopHandler.removeCallbacks(runnable);
                        noopHandler.postDelayed(runnable,5000);
                        break;
                    case STOP:
                        noopHandler.removeCallbacks(runnable);
                        quit();
                        break;
                    default:
                        noopHandler.removeCallbacks(runnable);
                        quit();
                        break;
                }

                return true;
            }
        });
    }



}
