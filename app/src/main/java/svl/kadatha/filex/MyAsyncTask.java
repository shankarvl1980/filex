package svl.kadatha.filex;

import android.os.Handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


public abstract class MyAsyncTask {
    private boolean isCancelled;
    private Future<?> future1,future2,future3;
    private boolean success;
    private final Handler handler;

    MyAsyncTask(Handler handler)
    {
        this.handler=handler;
    }

    protected void onPreExecute()
    {

    }

    protected void onCancelled(Boolean result)
    {

    }


    protected void onPostExecute(boolean result)
    {

    }



    public void cancel(boolean mayInterruptRunning){
        isCancelled=true;
        if(future1!=null) future1.cancel(mayInterruptRunning);
        if(future2!=null) future2.cancel(mayInterruptRunning);
        if(future3!=null) future3.cancel(mayInterruptRunning);
        onCancelled(success);
    }

    public boolean isCancelled()
    {
        return isCancelled;
    }

    protected abstract Boolean doInBackground();

    public void execute()
    {
        onPreExecute();
        ExecutorService executorService=MyExecutorService.getExecutorService();
        future1=executorService.submit(new Runnable() {
            @Override
            public void run() {
                success=doInBackground();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onPostExecute(success);
                    }
                });
            }
        });
    }

}
