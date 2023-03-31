package svl.kadatha.filex;
import android.os.Handler;
import android.os.Looper;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public abstract class AlternativeAsyncTask < Params, Progress, Result > {

    private final ExecutorService executor;
    private Handler handler;
    private Future<?>future;
    protected AlternativeAsyncTask() {

        executor = MyExecutorService.getExecutorService();

    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public Handler getHandler() {
        if (handler == null) {
            synchronized(AsyncTask.class) {
                handler = new Handler(Looper.getMainLooper());
            }
        }
        return handler;
    }

    protected void onPreExecute() {}

    protected abstract Result doInBackground(Params... params);

    protected void onPostExecute(Result result) {}

    protected void onCancelled(Result result) {}

    protected void onProgressUpdate(@NotNull Progress value) {}

    // used for push progress resport to UI
    public void publishProgress(@NotNull Progress value) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                onProgressUpdate(value);
            }
        });
    }

//    public void execute() {
//        execute(null);
//    }

    public void execute(Params params) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                onPreExecute();
                future=executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        Result result = doInBackground(params);
                        if(isCancelled())
                        {
                            getHandler().post(new Runnable() {
                                @Override
                                public void run() {
                                    onCancelled(result);
                                }
                            });

                        }
                        else {
                            getHandler().post(new Runnable() {
                                @Override
                                public void run() {
                                    onPostExecute(result);
                                }
                            });

                        }

                    }
                });
            }
        });
    }

    public void cancel(boolean interrupt) {
        if(future!=null)
        {
            future.cancel(interrupt);
        }

    }

    public boolean isCancelled() {
        return executor == null || executor.isTerminated() || executor.isShutdown() || future ==null || future.isCancelled();
    }
}